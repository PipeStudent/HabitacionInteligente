package com.felipe.habitacioninteligente.mqtt

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.felipe.habitacioninteligente.data.Repo
import com.felipe.habitacioninteligente.security.Roles
import com.felipe.habitacioninteligente.security.SecurePrefs
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import org.json.JSONObject
import java.util.UUID

/**
 * Comunicacion por WiFi con el ESP32 mediante MQTT (TLS).
 * Tanto esta app como la segunda instancia en otro celular se suscriben a los mismos topics,
 * por eso ambas ven el mismo estado (los topics "estado" y "status" son retenidos por el ESP32).
 */
object MqttManager {

    data class RoomState(
        var temp: Double? = null, var hum: Double? = null, var mov: Boolean = false,
        var luz: Boolean = false, var fan: Boolean = false, var deviceOnline: Boolean = false
    )

    interface Listener {
        fun onConnection(connected: Boolean, message: String)
        fun onState(state: RoomState)
    }

    val state = RoomState()
    private val main = Handler(Looper.getMainLooper())
    private val listeners = mutableSetOf<Listener>()
    private var client: Mqtt3AsyncClient? = null
    private var prefix = ""
    private var appCtx: Context? = null
    private var lastSavedAt = 0L
    var connected = false
        private set

    fun addListener(l: Listener) { listeners += l; l.onConnection(connected, if (connected) "Conectado" else "Desconectado"); l.onState(state) }
    fun removeListener(l: Listener) { listeners -= l }

    private fun notifyConn(ok: Boolean, msg: String) {
        connected = ok
        main.post { listeners.toList().forEach { it.onConnection(ok, msg) } }
    }

    private fun notifyState() = main.post { listeners.toList().forEach { it.onState(state) } }

    fun connect(ctx: Context) {
        if (connected || client != null) return
        appCtx = ctx.applicationContext
        val cfg = SecurePrefs.mqttConfig(ctx)
        prefix = cfg.prefix
        notifyConn(false, "Conectando a ${cfg.host}...")

        var b = MqttClient.builder().useMqttVersion3()
            .identifier("hab-app-" + UUID.randomUUID().toString().take(8))
            .serverHost(cfg.host).serverPort(cfg.port)
            .automaticReconnectWithDefaultConfig()
            .addConnectedListener { subscribeAll(); notifyConn(true, "Conectado a ${cfg.host}") }
            .addDisconnectedListener { notifyConn(false, "Desconectado (reintentando...)") }
        if (cfg.tls) b = b.sslWithDefaultConfig()   // TLS: cifrado en transito
        val c = b.buildAsync()
        client = c

        var cb = c.connectWith().keepAlive(30).cleanSession(true)
        if (cfg.user.isNotEmpty()) {
            cb = cb.simpleAuth().username(cfg.user).password(cfg.pass.toByteArray()).applySimpleAuth()
        }
        cb.send().whenComplete { _, t ->
            if (t != null) notifyConn(false, "Error: ${t.message}")
        }
    }

    private fun subscribeAll() {
        val c = client ?: return
        for (t in listOf("sensores", "estado", "status")) {
            c.subscribeWith().topicFilter(prefix + t).qos(MqttQos.AT_LEAST_ONCE)
                .callback { p -> handle(p.topic.toString().removePrefix(prefix), String(p.payloadAsBytes)) }
                .send()
        }
    }

    private fun handle(topic: String, payload: String) {
        try {
            when (topic) {
                "sensores" -> {
                    val j = JSONObject(payload)
                    state.temp = j.optDouble("temp"); state.hum = j.optDouble("hum")
                    state.mov = j.optInt("mov", 0) == 1
                    val now = System.currentTimeMillis()
                    if (now - lastSavedAt > 30_000 && !state.temp!!.isNaN()) {   // guarda historial cada 30 s
                        lastSavedAt = now
                        val c = appCtx
                        // Solo el ADMIN escribe el historial en Firestore (evita duplicados entre celulares)
                        if (c != null && SecurePrefs.sessionRole(c) == Roles.ADMIN)
                            Repo.saveReading(state.temp!!, state.hum ?: 0.0, state.mov)
                    }
                }
                "estado" -> {
                    val j = JSONObject(payload)
                    state.luz = j.optInt("luz", 0) == 1; state.fan = j.optInt("fan", 0) == 1
                }
                "status" -> state.deviceOnline = payload.trim() == "online"
            }
            notifyState()
        } catch (_: Exception) { /* payload invalido: se ignora */ }
    }

    /** Envia un comando al ESP32. device: "luz" | "ventilador". */
    fun sendCommand(device: String, on: Boolean) {
        client?.publishWith()?.topic(prefix + "cmd/" + device)
            ?.qos(MqttQos.AT_LEAST_ONCE)?.payload((if (on) "ON" else "OFF").toByteArray())?.send()
    }

    fun disconnect() {
        client?.disconnect()
        client = null
        connected = false
    }

    fun reconnect(ctx: Context) { disconnect(); connect(ctx) }
}
