package com.felipe.habitacioninteligente.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.felipe.habitacioninteligente.data.Repo
import com.felipe.habitacioninteligente.databinding.ActivityDashboardBinding
import com.felipe.habitacioninteligente.mqtt.MqttManager
import com.felipe.habitacioninteligente.security.Roles
import com.felipe.habitacioninteligente.security.SecurePrefs

class DashboardActivity : AppCompatActivity(), MqttManager.Listener {
    private lateinit var b: ActivityDashboardBinding
    private lateinit var user: String
    private lateinit var role: String
    private var updating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val u = SecurePrefs.sessionUser(this)
        if (u == null || !Repo.isSignedIn()) { startActivity(Intent(this, LoginActivity::class.java)); finish(); return }
        user = u; role = SecurePrefs.sessionRole(this)
        b = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(b.root)

        val isAdmin = role == Roles.ADMIN
        b.tvUser.text = "Usuario: $user  ·  Rol: $role"
        // Refresca el rol desde Firestore (por si un admin lo cambió)
        Repo.fetchRole { r -> r.onSuccess { nr -> if (nr != role) { SecurePrefs.saveSession(this, user, nr); recreate() } } }
        // Control de acceso: solo ADMIN controla dispositivos y accede a configuracion
        b.swLuz.isEnabled = isAdmin
        b.swFan.isEnabled = isAdmin
        b.btnSettings.isEnabled = isAdmin
        b.tvRoleNote.text = if (isAdmin) "" else "Modo solo lectura: tu rol no permite controlar dispositivos."

        b.swLuz.setOnCheckedChangeListener { _, on -> if (!updating) command("luz", on) }
        b.swFan.setOnCheckedChangeListener { _, on -> if (!updating) command("ventilador", on) }
        b.btnHistory.setOnClickListener { startActivity(Intent(this, HistoryActivity::class.java)) }
        b.btnSettings.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        b.btnLogout.setOnClickListener {
            Repo.logEvent("Cierre de sesión")
            Repo.signOut()
            MqttManager.disconnect()
            SecurePrefs.clearSession(this)
            startActivity(Intent(this, LoginActivity::class.java)); finish()
        }
    }

    private fun command(device: String, on: Boolean) {
        if (role != Roles.ADMIN) return
        MqttManager.sendCommand(device, on)
        Repo.logEvent("Comando $device = ${if (on) "ON" else "OFF"}")
    }

    override fun onStart() {
        super.onStart()
        if (!::b.isInitialized) return
        MqttManager.connect(this)
        MqttManager.addListener(this)
    }

    override fun onStop() {
        super.onStop()
        if (::b.isInitialized) MqttManager.removeListener(this)
    }

    override fun onConnection(connected: Boolean, message: String) {
        b.tvConn.text = (if (connected) "● " else "○ ") + message
    }

    override fun onState(state: MqttManager.RoomState) {
        updating = true
        b.tvTemp.text = state.temp?.takeIf { !it.isNaN() }?.let { "%.1f °C".format(it) } ?: "-- °C"
        b.tvHum.text = state.hum?.takeIf { !it.isNaN() }?.let { "%.0f %%".format(it) } ?: "-- %"
        b.tvMov.text = if (state.mov) "Movimiento detectado" else "Sin movimiento"
        b.tvDevice.text = if (state.deviceOnline) "ESP32: en línea" else "ESP32: sin conexión"
        b.swLuz.isChecked = state.luz
        b.swFan.isChecked = state.fan
        updating = false
    }
}
