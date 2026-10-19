package com.felipe.habitacioninteligente.data

import com.felipe.habitacioninteligente.security.Roles
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

data class Reading(val ts: Long, val temp: Double, val hum: Double, val mov: Boolean)
data class EventLog(val ts: Long, val user: String, val detail: String)

/** Acceso a Firebase Authentication + Cloud Firestore (users, readings, events). */
object Repo {
    private val auth get() = FirebaseAuth.getInstance()
    private val fs get() = FirebaseFirestore.getInstance()

    val currentEmail: String? get() = auth.currentUser?.email
    fun isSignedIn() = auth.currentUser != null
    fun signOut() = auth.signOut()

    /** Crea la cuenta en Firebase Auth y su documento users/{uid} con rol VIEWER. */
    fun register(email: String, pass: String, cb: (Result<String>) -> Unit) {
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener { res ->
                val uid = res.user!!.uid
                fs.collection("users").document(uid)
                    .set(mapOf("email" to email, "role" to Roles.VIEWER))
                    .addOnSuccessListener { cb(Result.success(Roles.VIEWER)) }
                    .addOnFailureListener { cb(Result.failure(it)) }
            }
            .addOnFailureListener { cb(Result.failure(it)) }
    }

    fun login(email: String, pass: String, cb: (Result<String>) -> Unit) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener { fetchRole(cb) }
            .addOnFailureListener { cb(Result.failure(it)) }
    }

    /** Lee el rol (ADMIN/VIEWER) del usuario actual desde Firestore. */
    fun fetchRole(cb: (Result<String>) -> Unit) {
        val uid = auth.currentUser?.uid ?: return cb(Result.failure(IllegalStateException("Sin sesion")))
        fs.collection("users").document(uid).get()
            .addOnSuccessListener { cb(Result.success(it.getString("role") ?: Roles.VIEWER)) }
            .addOnFailureListener { cb(Result.failure(it)) }
    }

    fun saveReading(temp: Double, hum: Double, mov: Boolean) {
        fs.collection("readings").add(
            mapOf("ts" to System.currentTimeMillis(), "temp" to temp, "hum" to hum, "mov" to mov)
        )
    }

    fun lastReadings(limit: Long = 100, cb: (List<Reading>) -> Unit) {
        fs.collection("readings").orderBy("ts", Query.Direction.DESCENDING).limit(limit).get()
            .addOnSuccessListener { snap ->
                cb(snap.documents.map {
                    Reading(it.getLong("ts") ?: 0, it.getDouble("temp") ?: 0.0,
                        it.getDouble("hum") ?: 0.0, it.getBoolean("mov") ?: false)
                })
            }
            .addOnFailureListener { cb(emptyList()) }
    }

    fun logEvent(detail: String) {
        val email = currentEmail ?: return
        fs.collection("events").add(mapOf("ts" to System.currentTimeMillis(), "user" to email, "detail" to detail))
    }

    fun lastEvents(limit: Long = 100, cb: (List<EventLog>) -> Unit) {
        fs.collection("events").orderBy("ts", Query.Direction.DESCENDING).limit(limit).get()
            .addOnSuccessListener { snap ->
                cb(snap.documents.map {
                    EventLog(it.getLong("ts") ?: 0, it.getString("user") ?: "", it.getString("detail") ?: "")
                })
            }
            .addOnFailureListener { cb(emptyList()) }
    }
}
