package com.felipe.habitacioninteligente.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.felipe.habitacioninteligente.data.Repo
import com.felipe.habitacioninteligente.databinding.ActivityHistoryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val b = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(b.root)
        val fmt = SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault())

        fun show(lv: android.widget.ListView, items: List<String>, empty: String) {
            lv.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items.ifEmpty { listOf(empty) })
        }
        show(b.lvReadings, listOf("Cargando…"), "")
        show(b.lvEvents, listOf("Cargando…"), "")

        // Datos desde Cloud Firestore
        Repo.lastReadings { list ->
            show(b.lvReadings, list.map {
                "${fmt.format(Date(it.ts))}  ·  %.1f °C  ·  %.0f %%  ·  %s".format(it.temp, it.hum, if (it.mov) "mov" else "sin mov")
            }, "Sin lecturas aún")
        }
        Repo.lastEvents { list ->
            show(b.lvEvents, list.map { "${fmt.format(Date(it.ts))}  ·  ${it.user}: ${it.detail}" }, "Sin eventos aún")
        }
        b.btnBack.setOnClickListener { finish() }
    }
}
