package com.example.reportes.Activitys

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.reportes.R
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class DatosGeneralesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_datos_generales)

        val etCliente = findViewById<EditText>(R.id.etCliente)
        val etMarca = findViewById<EditText>(R.id.etMarca)
        val etVIN = findViewById<EditText>(R.id.etVIN)
        val btnContinuar = findViewById<Button>(R.id.btnContinuar)

        btnContinuar.setOnClickListener {

            //  1. Crear ID único de evaluación
            val evaluacionId = UUID.randomUUID().toString()

            //  2. Pasar a la siguiente pantalla
            val intent = Intent(this, SeccionTecnicaActivity::class.java)
            intent.putExtra("EVALUACION_ID", evaluacionId)
            intent.putExtra("SECCION", "Motor")

            //  3. Pasar datos generales
            intent.putExtra("CLIENTE", etCliente.text.toString())
            intent.putExtra("MARCA", etMarca.text.toString())
            intent.putExtra("VIN", etVIN.text.toString())

            val evaluacion = hashMapOf(
                "id" to evaluacionId,
                "cliente" to etCliente.text.toString(),
                "marca" to etMarca.text.toString(),
                "vin" to etVIN.text.toString(),
                "fecha" to System.currentTimeMillis()
            )

            FirebaseFirestore.getInstance()
                .collection("evaluaciones")
                .document(evaluacionId)
                .set(evaluacion)

            startActivity(intent)
        }
    }
}