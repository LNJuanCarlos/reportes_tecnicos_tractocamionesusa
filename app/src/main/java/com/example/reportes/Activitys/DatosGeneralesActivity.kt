package com.example.reportes.Activitys

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
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
import com.example.reportes.Utils.TipoFoto
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class DatosGeneralesActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_FOTO = 1001
    }
    private lateinit var evaluacionId: String
    private lateinit var etCliente: EditText
    private lateinit var etMarca: EditText
    private lateinit var etPlaca: EditText
    private lateinit var etVin: EditText
    private lateinit var eTFrontal: EditText
    private lateinit var etKm: EditText
    private lateinit var etHr: EditText
    private lateinit var etmodelo:EditText
    private lateinit var etanio: EditText
    private lateinit var etcajaCambios: EditText
    private lateinit var etmotorModelo: EditText
    private lateinit var etmotorSerie: EditText
    private lateinit var currentPhotoFile: File

    private lateinit var tipoFotoActual: TipoFoto
    private var fotoUri: Uri? = null

    private val fotoUrls = mutableMapOf<TipoFoto, String>()
    private val btnVerMap = mutableMapOf<TipoFoto, Button>()
    private val imgPreviewMap = mutableMapOf<TipoFoto, ImageView>()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_datos_generales)

        findViewById<Button>(R.id.btnFotoPlaca).setOnClickListener {
            tomarFoto(TipoFoto.PLACA)
        }

        findViewById<Button>(R.id.btnFotoFrontal).setOnClickListener {
            tomarFoto(TipoFoto.FRONTAL)
        }

        findViewById<Button>(R.id.btnFotoVin).setOnClickListener {
            tomarFoto(TipoFoto.VIN)
        }

        findViewById<Button>(R.id.btnFotoKm).setOnClickListener {
            tomarFoto(TipoFoto.KM)
        }

        findViewById<Button>(R.id.btnFotoCaja).setOnClickListener {
            tomarFoto(TipoFoto.CAJA_CAMBIOS)
        }

        findViewById<Button>(R.id.btnFotoMotor).setOnClickListener {
            tomarFoto(TipoFoto.MOTOR)
        }

        val btnVerVin = findViewById<Button>(R.id.btnVerVin)
        val imgPreviewVin = findViewById<ImageView>(R.id.imgPreviewVin)

        val btnVerPlaca = findViewById<Button>(R.id.btnVerPlaca)
        val imgPreviewPlaca = findViewById<ImageView>(R.id.imgPreviewPlaca)

        val btnVerFrontal = findViewById<Button>(R.id.btnVerFrontal)
        val imgPreviewFrontal = findViewById<ImageView>(R.id.imgPreviewFrontal)

        val btnVerKm = findViewById<Button>(R.id.btnVerKm)
        val imgPreviewKm = findViewById<ImageView>(R.id.imgPreviewKm)

        val btnVerCaja = findViewById<Button>(R.id.btnVerCaja)
        val imgPreviewCaja = findViewById<ImageView>(R.id.imgPreviewCaja)

        val btnVerMotor = findViewById<Button>(R.id.btnVerMotor)
        val imgPreviewMotor = findViewById<ImageView>(R.id.imgPreviewMotor)

        btnVerMap[TipoFoto.VIN] = btnVerVin
        imgPreviewMap[TipoFoto.VIN] = imgPreviewVin

        btnVerMap[TipoFoto.PLACA] = btnVerPlaca
        imgPreviewMap[TipoFoto.PLACA] = imgPreviewPlaca

        btnVerMap[TipoFoto.FRONTAL] = btnVerFrontal
        imgPreviewMap[TipoFoto.FRONTAL] = imgPreviewFrontal

        btnVerMap[TipoFoto.KM] = btnVerKm
        imgPreviewMap[TipoFoto.KM] = imgPreviewKm

        btnVerMap[TipoFoto.CAJA_CAMBIOS] = btnVerCaja
        imgPreviewMap[TipoFoto.CAJA_CAMBIOS] = imgPreviewCaja

        btnVerMap[TipoFoto.MOTOR] = btnVerMotor
        imgPreviewMap[TipoFoto.MOTOR] = imgPreviewMotor

        btnVerVin.isEnabled = false
        btnVerVin.alpha = 0.5f

        listOf(btnVerPlaca, btnVerFrontal, btnVerKm, btnVerCaja, btnVerMotor).forEach {
            it.isEnabled = false
            it.alpha = 0.5f
        }

        btnVerVin.setOnClickListener {togglePreview(TipoFoto.VIN) }
        btnVerPlaca.setOnClickListener { togglePreview(TipoFoto.PLACA) }
        btnVerFrontal.setOnClickListener { togglePreview(TipoFoto.FRONTAL) }
        btnVerKm.setOnClickListener { togglePreview(TipoFoto.KM) }
        btnVerCaja.setOnClickListener { togglePreview(TipoFoto.CAJA_CAMBIOS) }
        btnVerMotor.setOnClickListener { togglePreview(TipoFoto.MOTOR) }



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
         eTFrontal = findViewById(R.id.etFrontal)
         etHr = findViewById(R.id.etHr)
         etmodelo = findViewById(R.id.etModelo)
         etanio = findViewById(R.id.etanio)
         etcajaCambios = findViewById(R.id.etcaja)
         etmotorModelo = findViewById(R.id.etmotorModelo)
         etmotorSerie = findViewById(R.id.etMotorSerie)

         val btnGuardar = findViewById<Button>(R.id.btnGuardar)

        cargarDatosGenerales()
        cargarFotos()

        btnGuardar.setOnClickListener {

            val frontalTexto = eTFrontal.text.toString().trim()

            val datos = DatosGenerales(
                cliente = etCliente.text.toString(),
                marca = etMarca.text.toString(),
                placa = etPlaca.text.toString(),
                vin = etVin.text.toString(),
                frontal = if (frontalTexto.isEmpty())
                    "VISTA FRONTAL DE LA UNIDAD"
                else frontalTexto,
                km = etKm.text.toString(),
                hr = etHr.text.toString(),
                modelo = etmodelo.text.toString(),
                anio = etanio.text.toString(),
                cajaCambios = etcajaCambios.text.toString(),
                motorModelo = etmotorModelo.text.toString(),
                motorSerie = etmotorSerie.text.toString(),

                vinFotoUrl = fotoUrls[TipoFoto.VIN],
                placaFotoUrl = fotoUrls[TipoFoto.PLACA],
                frontalFotoUrl = fotoUrls[TipoFoto.FRONTAL],
                kmFotoUrl = fotoUrls[TipoFoto.KM],
                cajaFotoUrl = fotoUrls[TipoFoto.CAJA_CAMBIOS],
                motorFotoUrl = fotoUrls[TipoFoto.MOTOR]
            )

            guardarDatosGenerales(datos)
        }

    }

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                tomarFoto(tipoFotoActual)
            } else {
                Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_LONG).show()
            }
        }

    private fun tienePermisoCamara(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    private val fotoCampoFirestore = mapOf(
        TipoFoto.VIN to "vinFotoUrl",
        TipoFoto.PLACA to "placaFotoUrl",
        TipoFoto.FRONTAL to "frontalFotoUrl",
        TipoFoto.KM to "kmFotoUrl",
        TipoFoto.CAJA_CAMBIOS to "cajaFotoUrl",
        TipoFoto.MOTOR to "motorFotoUrl"
    )

    private fun cargarFotos() {
        FirebaseFirestore.getInstance()
            .collection("evaluaciones")
            .document(evaluacionId)
            .collection("datosGenerales")
            .document("info")
            .get()
            .addOnSuccessListener { doc ->

                fotoCampoFirestore.forEach { (tipo, campo) ->
                    val url = doc.getString(campo)
                    if (!url.isNullOrEmpty()) {
                        fotoUrls[tipo] = url
                        habilitarVer(tipo)
                    }
                }
            }
    }

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                subirFotoFirebase(tipoFotoActual, fotoUri!!)
            } else {
                Toast.makeText(this, "No se pudo tomar la foto", Toast.LENGTH_SHORT).show()
            }
        }


    private fun togglePreview(tipo: TipoFoto) {

        val btn = btnVerMap[tipo] ?: return
        val img = imgPreviewMap[tipo] ?: return
        val url = fotoUrls[tipo]

        if (img.visibility == View.VISIBLE) {
            img.visibility = View.GONE
            btn.text = "VER"
            return
        }

        if (url.isNullOrEmpty()) {
            Toast.makeText(this, "No hay foto para mostrar", Toast.LENGTH_SHORT).show()
            return
        }

        img.visibility = View.VISIBLE
        btn.text = "X"

        Glide.with(this)
            .load(url)
            .into(img)
    }

    private fun habilitarVer(tipo: TipoFoto) {
        btnVerMap[tipo]?.apply {
            isEnabled = true
            alpha = 1f
        }
    }
    private fun subirFotoFirebase(tipo: TipoFoto, uri: Uri) {

        val ref = FirebaseStorage.getInstance().reference.child(
            "evaluaciones/$evaluacionId/datosGenerales/${tipo.name.lowercase()}.jpg"
        )

        ref.putFile(uri)
            .continueWithTask { task ->
                if (!task.isSuccessful) throw task.exception!!
                ref.downloadUrl
            }
            .addOnSuccessListener { downloadUri ->

                fotoUrls[tipo] = downloadUri.toString()
                habilitarVer(tipo)

                Toast.makeText(
                    this,
                    "Foto ${tipo.name} cargada",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error subiendo ${tipo.name}", Toast.LENGTH_LONG).show()
            }
    }

    fun tomarFoto(tipo: TipoFoto) {
        tipoFotoActual = tipo

        if (!tienePermisoCamara()) {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
            return
        }

        val file = crearArchivoFoto(tipo)
        currentPhotoFile = file

        fotoUri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            file
        )

        takePictureLauncher.launch(fotoUri!!)
    }


    private fun crearArchivoFoto(tipo: TipoFoto): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        return File.createTempFile(
            "${tipo.name}_$timeStamp",
            ".jpg",
            storageDir
        )
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
                    eTFrontal.setText(doc.getString("frontal"))
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
            .set(datos, SetOptions.merge())
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