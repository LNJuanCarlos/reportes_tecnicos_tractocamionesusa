package com.example.reportes.Activitys

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.reportes.Models.DatosGenerales
import com.example.reportes.Models.SeccionTecnicaFirestore
import com.example.reportes.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.util.UUID

class DatosGeneralesActivity : AppCompatActivity() {

    private lateinit var evaluacionId: String
    private lateinit var etCliente: EditText
    private lateinit var etMarca: EditText
    private lateinit var etPlaca: EditText
    private lateinit var etVin: EditText
    private lateinit var etKm: EditText
    private lateinit var etHr: EditText
    private lateinit var etmodelo:EditText
    private lateinit var etanio: EditText
    private lateinit var etcajaCambios: EditText
    private lateinit var etmotorModelo: EditText
    private lateinit var etmotorSerie: EditText

    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private var photoUri: Uri? = null
    private var tipoFotoActual: String = ""
    private lateinit var btnVerVin: Button
    private lateinit var imgPreviewVin: ImageView
    private var fotoVinLocalPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_datos_generales)

        findViewById<Button>(R.id.btnFotoPlaca).setOnClickListener {
            tomarFoto("placa")
        }

        findViewById<Button>(R.id.btnFotoFrontal).setOnClickListener {
            tomarFoto("frontal")
        }

        findViewById<Button>(R.id.btnFotoVin).setOnClickListener {
            tomarFoto("vin")
        }

        findViewById<Button>(R.id.btnFotoKm).setOnClickListener {
            tomarFoto("km")
        }

        btnVerVin = findViewById(R.id.btnVerVin)
        imgPreviewVin = findViewById(R.id.imgPreviewVin)

        btnVerVin.setOnClickListener {
            togglePreview(
                btnVerVin,
                imgPreviewVin,
                fotoVinLocalPath   // String que ya guardas al tomar la foto
            )
        }


        evaluacionId = intent.getStringExtra("EVALUACION_ID")
            ?: run {
                Toast.makeText(this, "Evaluación no encontrada", Toast.LENGTH_LONG).show()
                finish()
                return
            }



         etCliente = findViewById(R.id.etCliente)
         etMarca = findViewById(R.id.etMarca)
         etPlaca = findViewById(R.id.etPlaca)
         etVin = findViewById(R.id.etVin)
         etKm = findViewById(R.id.etKm)
         etHr = findViewById(R.id.etHr)
         etmodelo = findViewById(R.id.etModelo)
         etanio = findViewById(R.id.etanio)
         etcajaCambios = findViewById(R.id.etcaja)
         etmotorModelo = findViewById(R.id.etmotorModelo)
         etmotorSerie = findViewById(R.id.etMotorSerie)

         val btnGuardar = findViewById<Button>(R.id.btnGuardar)

        cargarDatosGenerales()

        btnGuardar.setOnClickListener {

            val datos = DatosGenerales(
                cliente = etCliente.text.toString(),
                marca = etMarca.text.toString(),
                placa = etPlaca.text.toString(),
                vin = etVin.text.toString(),
                km = etKm.text.toString(),
                hr = etHr.text.toString(),
                modelo = etmodelo.text.toString(),
                anio = etanio.text.toString(),
                cajaCambios = etcajaCambios.text.toString(),
                motorModelo = etmotorModelo.text.toString(),
                motorSerie = etmotorSerie.text.toString()
            )

            guardarDatosGenerales(datos)
        }

        takePictureLauncher =
            registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
                if (success && photoUri != null) {
                    subirFotoIdentificacion(photoUri!!, tipoFotoActual)
                }
            }

    }

    private fun togglePreview(
        btn: Button,
        imageView: ImageView,
        fotoPath: String?
    ) {
        if (imageView.visibility == View.GONE) {

            if (fotoPath.isNullOrEmpty()) {
                Toast.makeText(this, "No hay foto para mostrar", Toast.LENGTH_SHORT).show()
                return
            }

            imageView.visibility = View.VISIBLE
            btn.text = "X"

            Glide.with(this)
                .load(File(fotoPath))
                .centerCrop()
                .into(imageView)

        } else {
            imageView.visibility = View.GONE
            btn.text = "VER"
        }
    }

    private fun subirFotoIdentificacion(uri: Uri, tipo: String) {

        val storageRef = FirebaseStorage.getInstance().reference
        val ref = storageRef.child(
            "evaluaciones/$evaluacionId/identificacion/$tipo.jpg"
        )

        ref.putFile(uri)
            .continueWithTask { task ->
                if (!task.isSuccessful) throw task.exception!!
                ref.downloadUrl
            }
            .addOnSuccessListener { downloadUri ->

                val campo = when (tipo) {
                    "placa" -> "fotos.placaUrl"
                    "frontal" -> "fotos.frontalUrl"
                    "vin" -> "fotos.vinUrl"
                    "km" -> "fotos.kmUrl"
                    else -> return@addOnSuccessListener
                }

                FirebaseFirestore.getInstance()
                    .collection("evaluaciones")
                    .document(evaluacionId)
                    .collection("datosGenerales")
                    .document("info")
                    .update(campo, downloadUri.toString())

                Toast.makeText(this, "Foto $tipo guardada", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error subiendo foto", Toast.LENGTH_LONG).show()
            }
    }

    private fun tomarFoto(tipo: String) {
        tipoFotoActual = tipo

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                2001
            )
            return
        }

        val file = File.createTempFile(
            "foto_${tipo}_${System.currentTimeMillis()}",
            ".jpg",
            cacheDir
        )

        photoUri = FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            file
        )

        takePictureLauncher.launch(photoUri!!)
    }


    private fun cargarDatosGenerales() {

        FirebaseFirestore.getInstance()
            .collection("evaluaciones")
            .document(evaluacionId)
            .collection("datosGenerales")
            .document("info")
            .get()
            .addOnSuccessListener { doc ->

                if (doc.exists()) {
                    etCliente.setText(doc.getString("cliente"))
                    etMarca.setText(doc.getString("marca"))
                    etPlaca.setText(doc.getString("placa"))
                    etVin.setText(doc.getString("vin"))
                    etKm.setText(doc.getString("km"))
                    etHr.setText(doc.getString("hr"))
                    etmodelo.setText(doc.getString("modelo"))
                    etanio.setText(doc.getString("anio"))
                    etcajaCambios.setText(doc.getString("cajaCambios"))
                    etmotorModelo.setText(doc.getString("motorModelo"))
                    etmotorSerie.setText(doc.getString("motorSerie"))
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error cargando datos", Toast.LENGTH_SHORT).show()
            }
    }

    private fun guardarDatosGenerales(datos: DatosGenerales) {

        val db = FirebaseFirestore.getInstance()

        // 1 Guardar datos generales
        db.collection("evaluaciones")
            .document(evaluacionId)
            .collection("datosGenerales")
            .document("info")
            .set(datos)
            .addOnSuccessListener {

                // 2 Marcar sección como completada
                db.collection("evaluaciones")
                    .document(evaluacionId)
                    .collection("secciones")
                    .document("datos")
                    .set(mapOf("completada" to true))

                Toast.makeText(this, "Datos guardados", Toast.LENGTH_SHORT).show()
                finish() // vuelve al HOME
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al guardar", Toast.LENGTH_LONG).show()
            }
    }
}