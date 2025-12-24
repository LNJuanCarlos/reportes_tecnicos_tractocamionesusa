package com.example.reportes.Activitys

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.reportes.Adapters.SeccionAdapter
import com.example.reportes.Models.EvaluacionTecnica
import com.example.reportes.Models.SeccionItem
import com.example.reportes.Models.SeccionTecnica
import com.example.reportes.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import androidx.activity.OnBackPressedCallback

class SeccionTecnicaActivity : AppCompatActivity() {

    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private var currentPhotoIndex = -1
    private var photoUri: Uri? = null
    private lateinit var adapter: SeccionAdapter
    private val lista = mutableListOf<SeccionItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seccion_tecnica)

        val rv = findViewById<RecyclerView>(R.id.rvSeccion)
        val btnAgregar = findViewById<Button>(R.id.btnAgregar)
        val titulo = intent.getStringExtra("SECCION") ?: "Sección"


        val evaluacionId = intent.getStringExtra("EVALUACION_ID")!!
        val cliente = intent.getStringExtra("CLIENTE")

        findViewById<TextView>(R.id.tvTituloSeccion).text = titulo

        adapter = SeccionAdapter(
            lista,
            onFotoClick = { pos -> tomarFoto(pos) },
            onEliminarClick = { pos ->
                lista.removeAt(pos)
                adapter.notifyItemRemoved(pos)
            }
        )

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        btnAgregar.setOnClickListener {
            lista.add(SeccionItem(orden = lista.size + 1))
            adapter.notifyItemInserted(lista.size - 1)
        }

        takePictureLauncher =
            registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
                if (success && photoUri != null) {
                    subirFotoFirebase(photoUri!!, currentPhotoIndex)
                }
            }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    guardarEvaluacion()
                    finish()
                }
            }
        )

    }

    private fun tomarFoto(pos: Int) {
        currentPhotoIndex = pos

        val file = File.createTempFile(
            "foto_${System.currentTimeMillis()}",
            ".jpg",
            cacheDir
        )

        photoUri = FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            file
        )

        photoUri?.let {
            takePictureLauncher.launch(it)
        }
    }

    private fun subirFotoFirebase(uri: Uri, pos: Int) {
        val storageRef = FirebaseStorage.getInstance().reference
        val seccionNombre = intent.getStringExtra("SECCION") ?: "general"
        val ref = storageRef.child(
            "evaluaciones/$seccionNombre/foto_${System.currentTimeMillis()}.jpg"
        )

        ref.putFile(uri)
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    throw task.exception!!
                }
                ref.downloadUrl
            }
            .addOnSuccessListener { downloadUri ->
                if (pos in lista.indices) {
                    lista[pos].fotoUri = downloadUri.toString()
                    adapter.notifyItemChanged(pos)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error subiendo foto", Toast.LENGTH_SHORT).show()
            }
    }

    private fun guardarEvaluacion() {

        val evaluacionId = intent.getStringExtra("EVALUACION_ID")!!
        val seccionNombre = intent.getStringExtra("SECCION") ?: "Sección"

        val seccion = hashMapOf(
            "nombre" to seccionNombre,
            "items" to lista
        )

        FirebaseFirestore.getInstance()
            .collection("evaluaciones")
            .document(evaluacionId)
            .collection("secciones")
            .document(seccionNombre)
            .set(seccion)
            .addOnSuccessListener {
                Toast.makeText(this, "Sección guardada", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al guardar sección", Toast.LENGTH_SHORT).show()
            }
    }

}