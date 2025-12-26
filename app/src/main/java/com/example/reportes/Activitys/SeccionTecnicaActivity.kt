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
import com.example.reportes.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import androidx.activity.OnBackPressedCallback

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.reportes.Models.ItemSeccion
import com.example.reportes.Models.SeccionTecnicaFirestore

class SeccionTecnicaActivity : AppCompatActivity() {

    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private var currentPhotoIndex = -1
    private var photoUri: Uri? = null
    private lateinit var adapter: SeccionAdapter


    private lateinit var evaluacionId: String
    private lateinit var seccionId: String
    private lateinit var seccionTitulo: String
    private val items = mutableListOf<ItemSeccion>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seccion_tecnica)

        val rv = findViewById<RecyclerView>(R.id.rvSeccion)
        val btnAgregar = findViewById<Button>(R.id.btnAgregar)
        //val btnContinuar = findViewById<Button>(R.id.btnContinuar)

        findViewById<Button>(R.id.btnGuardarSeccion).setOnClickListener {
            guardarSeccion()
        }



        evaluacionId = intent.getStringExtra("EVALUACION_ID")
            ?: run {
                Toast.makeText(this, "Evaluación no encontrada", Toast.LENGTH_LONG).show()
                finish(); return
            }

        seccionId = intent.getStringExtra("SECCION_ID")
            ?: run {
                Toast.makeText(this, "Sección no válida", Toast.LENGTH_LONG).show()
                finish(); return
            }

        seccionTitulo = intent.getStringExtra("SECCION_TITULO") ?: "Sección"





        adapter = SeccionAdapter(
            items,
            onFotoClick = { pos -> tomarFoto(pos) },
            onEliminarClick = { pos ->
                items.removeAt(pos)
                adapter.notifyItemRemoved(pos)
            }
        )

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        btnAgregar.setOnClickListener {
            items.add(ItemSeccion(orden = items.size + 1))
            adapter.notifyItemInserted(items.size - 1)
        }

        takePictureLauncher =
            registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
                Log.d("CAMERA", "takePicture success = $success")
                if (success && photoUri != null) {
                    subirFotoFirebase(photoUri!!, currentPhotoIndex)
                }
            }

        cargarSeccion()

    }

    private fun cargarSeccion() {

        FirebaseFirestore.getInstance()
            .collection("evaluaciones")
            .document(evaluacionId)
            .collection("secciones")
            .document(seccionId)
            .get()
            .addOnSuccessListener { doc ->

                if (doc.exists()) {
                    val seccion = doc.toObject(SeccionTecnicaFirestore::class.java)

                    items.clear()
                    items.addAll(seccion?.items ?: emptyList())

                    adapter.notifyDataSetChanged()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error cargando sección", Toast.LENGTH_SHORT).show()
            }
    }


    private fun guardarSeccion() {

        val seccion = SeccionTecnicaFirestore(
            completada = true,
            items = items
        )

        FirebaseFirestore.getInstance()
            .collection("evaluaciones")
            .document(evaluacionId)
            .collection("secciones")
            .document(seccionId)
            .set(seccion)
            .addOnSuccessListener {
                Toast.makeText(this, "Sección guardada", Toast.LENGTH_SHORT).show()
                finish() // vuelve al HOME
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al guardar sección", Toast.LENGTH_LONG).show()
            }
    }


    private fun tomarFoto(pos: Int) {
        currentPhotoIndex = pos

        // 1️⃣ Permiso cámara
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                1001
            )
            return
        }

        // 2️⃣ Crear archivo temporal
        val file = File.createTempFile(
            "foto_${System.currentTimeMillis()}",
            ".jpg",
            cacheDir
        )

        // 🔥 ESTA ES LA RUTA LOCAL
        val currentPhotoPath = file.absolutePath

        // 🔥 GUARDAMOS LOCAL (PARA QUE SE VEA AL INSTANTE)
        items[pos].fotoLocal = currentPhotoPath
        adapter.notifyItemChanged(pos)

        // 3️⃣ Crear URI
        photoUri = FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            file
        )

        // 4️⃣ Lanzar cámara
        takePictureLauncher.launch(photoUri!!)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1001 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            // Reintentar tomar foto
            if (currentPhotoIndex != -1) {
                tomarFoto(currentPhotoIndex)
            }
        } else {
            Toast.makeText(
                this,
                "Permiso de cámara requerido",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun subirFotoFirebase(uri: Uri, pos: Int) {
        val storageRef = FirebaseStorage.getInstance().reference
        val ref = storageRef.child(
            "evaluaciones/$evaluacionId/$seccionId/foto_${System.currentTimeMillis()}.jpg"
        )

        ref.putFile(uri)
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    throw task.exception!!
                }
                ref.downloadUrl
            }
            .addOnSuccessListener { downloadUri ->

                if (pos in items.indices) {
                    items[pos].fotoUrl = downloadUri.toString()

                    Log.d("STORAGE", "URL GUARDADA = ${items[pos].fotoUrl}")

                    adapter.notifyItemChanged(pos)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error subiendo foto", Toast.LENGTH_SHORT).show()
            }
    }


}