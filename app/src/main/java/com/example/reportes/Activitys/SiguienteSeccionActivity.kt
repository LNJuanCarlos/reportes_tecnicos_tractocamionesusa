package com.example.reportes.Activitys

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.reportes.Adapters.SeccionHomeAdapter
import com.example.reportes.Models.SeccionHome
import com.example.reportes.R
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class SiguienteSeccionActivity : AppCompatActivity() {

    private lateinit var evaluacionId: String
    private lateinit var adapter: SeccionHomeAdapter

    private val secciones = mutableListOf(
        SeccionHome("datos", "Datos generales"),
        SeccionHome("placa", "Placa / Frontal"),
        SeccionHome("vin", "VIN / KM / HR"),
        SeccionHome("motor", "Motor"),
        SeccionHome("chasis", "Chasis"),
        SeccionHome("electrico", "Sistema eléctrico")
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_siguiente_seccion)

        evaluacionId = intent.getStringExtra("EVALUACION_ID")
            ?: UUID.randomUUID().toString()

        val rv = findViewById<RecyclerView>(R.id.rvSecciones)
        val btnFinalizar = findViewById<Button>(R.id.btnFinalizar)

        adapter = SeccionHomeAdapter(secciones) { seccion ->
            abrirSeccion(seccion)
        }

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        cargarEstadoSecciones(btnFinalizar)
    }

    private fun abrirSeccion(seccion: SeccionHome) {

        val intent = when (seccion.id) {
            "datos" -> Intent(this, DatosGeneralesActivity::class.java)
            else -> Intent(this, SeccionTecnicaActivity::class.java)
        }

        intent.putExtra("EVALUACION_ID", evaluacionId)
        intent.putExtra("SECCION_ID", seccion.id)
        intent.putExtra("SECCION_TITULO", seccion.titulo)

        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        val btnFinalizar = findViewById<Button>(R.id.btnFinalizar)
        cargarEstadoSecciones(btnFinalizar)
    }

    private fun cargarEstadoSecciones(btnFinalizar: Button) {

        val db = FirebaseFirestore.getInstance()

        db.collection("evaluaciones")
            .document(evaluacionId)
            .collection("secciones")
            .get()
            .addOnSuccessListener { result ->

                // Reset
                secciones.forEach { it.completada = false }

                for (doc in result) {
                    val id = doc.id
                    val completada = doc.getBoolean("completada") ?: false

                    secciones.find { it.id == id }?.completada = completada
                }

                adapter.notifyDataSetChanged()

                // Habilitar finalizar solo si todas están completas
                btnFinalizar.isEnabled = secciones.all { it.completada }
            }
    }
}