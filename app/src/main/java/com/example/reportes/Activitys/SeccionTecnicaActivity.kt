package com.example.reportes.Activitys

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.reportes.Adapters.SeccionAdapter
import com.example.reportes.Models.SeccionItem
import com.example.reportes.R

class SeccionTecnicaActivity : AppCompatActivity() {

    private lateinit var adapter: SeccionAdapter
    private val lista = mutableListOf<SeccionItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seccion_tecnica)

        val rv = findViewById<RecyclerView>(R.id.rvSeccion)
        val btnAgregar = findViewById<Button>(R.id.btnAgregar)
        val titulo = intent.getStringExtra("SECCION") ?: "Sección"

        findViewById<TextView>(R.id.tvTituloSeccion).text = titulo

        adapter = SeccionAdapter(
            lista,
            onFotoClick = { pos -> tomarFoto(pos) },
            onEliminarClick = { pos ->
                lista.removeAt(pos)
                adapter.notifyDataSetChanged()
            }
        )

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        btnAgregar.setOnClickListener {
            lista.add(SeccionItem(orden = lista.size + 1))
            adapter.notifyDataSetChanged()
        }
    }

    private fun tomarFoto(pos: Int) {
        // aquí conectamos cámara en el siguiente paso
        Toast.makeText(this, "Tomar foto posición $pos", Toast.LENGTH_SHORT).show()
    }
}