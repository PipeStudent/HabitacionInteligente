package com.felipe.habitacioninteligente.ui

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.util.Patterns
import androidx.appcompat.app.AppCompatActivity
import com.felipe.habitacioninteligente.data.Repo
import com.felipe.habitacioninteligente.databinding.ActivityLoginBinding
import com.felipe.habitacioninteligente.security.SecurePrefs

class LoginActivity : AppCompatActivity() {
    private lateinit var b: ActivityLoginBinding
    private var registerMode = false
    private var failures = 0
    private var lockedUntil = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Repo.isSignedIn() && SecurePrefs.sessionUser(this) != null) { goDashboard(); return }
        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.btnAction.setOnClickListener { submit() }
        b.btnToggle.setOnClickListener {
            registerMode = !registerMode
            b.tvTitle.text = if (registerMode) "Crear cuenta" else "Iniciar sesión"
            b.btnAction.text = if (registerMode) "Registrarme" else "Entrar"
            b.btnToggle.text = if (registerMode) "Ya tengo cuenta" else "Crear cuenta nueva"
            b.tvError.text = ""
        }
    }

    private fun submit() {
        val email = b.etUser.text.toString().trim()
        val pass = b.etPass.text.toString()
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { b.tvError.text = "Ingresa un correo válido"; return }

        if (registerMode) {
            if (pass.length < 8 || pass.none { it.isDigit() } || pass.none { it.isLetter() }) {
                b.tvError.text = "La contraseña debe tener 8+ caracteres, letras y números"; return
            }
            setBusy(true)
            Repo.register(email, pass) { r ->
                setBusy(false)
                r.onSuccess { role -> Repo.logEvent("Cuenta creada ($role)"); startSession(email, role) }
                r.onFailure { b.tvError.text = "No se pudo crear la cuenta: ${it.message}" }
            }
            return
        }

        // Bloqueo temporal tras 5 intentos fallidos
        val now = SystemClock.elapsedRealtime()
        if (now < lockedUntil) {
            b.tvError.text = "Demasiados intentos. Espera ${(lockedUntil - now) / 1000 + 1} s"; return
        }
        setBusy(true)
        Repo.login(email, pass) { r ->
            setBusy(false)
            r.onSuccess { role -> failures = 0; Repo.logEvent("Inicio de sesión"); startSession(email, role) }
            r.onFailure {
                failures++
                if (failures >= 5) { lockedUntil = SystemClock.elapsedRealtime() + 30_000; failures = 0 }
                b.tvError.text = "Correo o contraseña incorrectos"
            }
        }
    }

    private fun setBusy(busy: Boolean) { b.btnAction.isEnabled = !busy }

    private fun startSession(email: String, role: String) {
        SecurePrefs.saveSession(this, email, role)
        goDashboard()
    }

    private fun goDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }
}
