package com.felipe.habitacioninteligente.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.felipe.habitacioninteligente.databinding.ActivitySettingsBinding
import com.felipe.habitacioninteligente.mqtt.MqttManager
import com.felipe.habitacioninteligente.security.Roles
import com.felipe.habitacioninteligente.security.SecurePrefs

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Control de acceso: solo ADMIN
        if (SecurePrefs.sessionRole(this) != Roles.ADMIN) { finish(); return }
        val b = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(b.root)
        val c = SecurePrefs.mqttConfig(this)
        b.etHost.setText(c.host); b.etPort.setText(c.port.toString())
        b.etMqttUser.setText(c.user); b.etMqttPass.setText(c.pass)
        b.etPrefix.setText(c.prefix); b.swTls.isChecked = c.tls

        b.btnSave.setOnClickListener {
            val port = b.etPort.text.toString().toIntOrNull()
            var prefix = b.etPrefix.text.toString().trim()
            if (port == null || b.etHost.text.isBlank() || prefix.isEmpty()) {
                Toast.makeText(this, "Revisa host, puerto y prefijo", Toast.LENGTH_SHORT).show(); return@setOnClickListener
            }
            if (!prefix.endsWith("/")) prefix += "/"
            SecurePrefs.saveMqttConfig(this, SecurePrefs.MqttConfig(
                b.etHost.text.toString().trim(), port, b.swTls.isChecked,
                b.etMqttUser.text.toString().trim(), b.etMqttPass.text.toString(), prefix))
            MqttManager.reconnect(this)
            Toast.makeText(this, "Guardado. Reconectando…", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
