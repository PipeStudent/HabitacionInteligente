package com.felipe.habitacioninteligente.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** Preferencias cifradas (AES-256) respaldadas por Android Keystore: sesion y credenciales del broker. */
object SecurePrefs {
    @Volatile private var prefs: SharedPreferences? = null

    private fun p(ctx: Context): SharedPreferences =
        prefs ?: synchronized(this) {
            prefs ?: run {
                val key = MasterKey.Builder(ctx.applicationContext)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
                EncryptedSharedPreferences.create(
                    ctx.applicationContext, "secure_prefs", key,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                ).also { prefs = it }
            }
        }

    // ---- Sesion ----
    fun saveSession(ctx: Context, user: String, role: String) =
        p(ctx).edit().putString("session_user", user).putString("session_role", role).apply()

    fun sessionUser(ctx: Context): String? = p(ctx).getString("session_user", null)
    fun sessionRole(ctx: Context): String = p(ctx).getString("session_role", Roles.VIEWER) ?: Roles.VIEWER
    fun clearSession(ctx: Context) =
        p(ctx).edit().remove("session_user").remove("session_role").apply()

    // ---- Configuracion MQTT ----
    data class MqttConfig(
        val host: String, val port: Int, val tls: Boolean,
        val user: String, val pass: String, val prefix: String
    )

    fun mqttConfig(ctx: Context): MqttConfig {
        val s = p(ctx)
        return MqttConfig(
            host = s.getString("mqtt_host", "broker.hivemq.com") ?: "broker.hivemq.com",
            port = s.getInt("mqtt_port", 8883),
            tls = s.getBoolean("mqtt_tls", true),
            user = s.getString("mqtt_user", "") ?: "",
            pass = s.getString("mqtt_pass", "") ?: "",
            prefix = s.getString("mqtt_prefix", "habitacion_felipe_01/") ?: "habitacion_felipe_01/"
        )
    }

    fun saveMqttConfig(ctx: Context, c: MqttConfig) = p(ctx).edit()
        .putString("mqtt_host", c.host).putInt("mqtt_port", c.port).putBoolean("mqtt_tls", c.tls)
        .putString("mqtt_user", c.user).putString("mqtt_pass", c.pass).putString("mqtt_prefix", c.prefix)
        .apply()
}

object Roles {
    const val ADMIN = "ADMIN"     // controla dispositivos y cambia configuracion
    const val VIEWER = "VIEWER"   // solo monitorea
}
