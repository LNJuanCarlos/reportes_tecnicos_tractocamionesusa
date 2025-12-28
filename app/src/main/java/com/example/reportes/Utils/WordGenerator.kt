package com.example.reportes.Utils

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.reportes.Models.DatosGenerales
import com.example.reportes.Models.ItemSeccion
import com.example.reportes.Models.SeccionTecnicaFirestore
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import org.apache.poi.util.Units
import org.apache.poi.xwpf.usermodel.Document
import org.apache.poi.xwpf.usermodel.ParagraphAlignment
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.math.min

object WordGenerator {

    fun generarWordCompletoPruebaFoto(
        context: Context,
        evaluacionId: String,
        onFinish: (File) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        val storage = FirebaseStorage.getInstance()

        // Abrimos plantilla desde assets
        val assetManager = context.assets
        val inputStream = assetManager.open("plantilla_reporte_tecnico.docx")
        val templateFile = File(context.filesDir, "plantilla_temp.docx")
        inputStream.use { input ->
            templateFile.outputStream().use { output -> input.copyTo(output) }
        }
        val doc = XWPFDocument(FileInputStream(templateFile))

        // Obtener datos generales
        db.collection("evaluaciones")
            .document(evaluacionId)
            .collection("datosGenerales")
            .document("info")
            .get()
            .addOnSuccessListener { datosDoc ->
                val datos = datosDoc.toObject(DatosGenerales::class.java)
                if (datos != null) {
                    val datosMap = mapOf(
                        "{{cliente}}" to datos.cliente,
                        "{{marca}}" to datos.marca,
                        "{{vin}}" to datos.vin,
                        "{{modelo}}" to datos.modelo,
                        "{{placa}}" to datos.placa,
                        "{{anio}}" to datos.anio,
                        "{{caja_cambios}}" to datos.cajaCambios,
                        "{{motor_modelo}}" to datos.motorModelo,
                        "{{motor_serie}}" to datos.motorSerie
                    )

                    // Reemplazar texto en párrafos
                    for (p in doc.paragraphs) {
                        var textoCompleto = p.runs.joinToString("") { it.text() }

                        //  SALTAR si es placeholder de imagen
                        if (textoCompleto.contains("{{foto_")) continue

                        for ((placeholder, valor) in datosMap) {
                            textoCompleto = textoCompleto.replace(placeholder, valor)
                        }
                        for (run in p.runs) run.setText("", 0)
                        p.createRun().setText(textoCompleto)
                    }

                    // Reemplazar texto en tablas
                    for (table in doc.tables) {
                        for (row in table.rows) {
                            for (cell in row.tableCells) {
                                for (p in cell.paragraphs) {
                                    var textoCompleto = p.runs.joinToString("") { it.text() }

                                    //  SALTAR placeholders de imagen
                                    if (textoCompleto.contains("{{foto_")) continue

                                    for ((placeholder, valor) in datosMap) {
                                        textoCompleto = textoCompleto.replace(placeholder, valor)
                                    }
                                    for (run in p.runs) run.setText("", 0)
                                    p.createRun().setText(textoCompleto)
                                }
                            }
                        }
                    }

                    // ------------------------------
                    // FOTO DE PLACA {{foto_placa}}
                    // ------------------------------
                    val urlFoto = datos.placaFrontalUrl
                    if (!urlFoto.isNullOrEmpty()) {
                        val tempFile = File.createTempFile("foto_temp", ".jpg", context.cacheDir)
                        storage.getReferenceFromUrl(urlFoto).getFile(tempFile)
                            .addOnSuccessListener {
                                try {
                                    insertarFotoProporcionada(
                                        doc,
                                        "{{foto_placa}}",
                                        tempFile,
                                        280.0,
                                        180.0
                                    )
                                    tempFile.delete()

                                    // Guardar documento final
                                    // ------------------------------
                                    // FOTO VIN {{foto_vin}}
                                    // ------------------------------
                                    val vinFotoUrl = datos.vinFotoUrl

                                    if (!vinFotoUrl.isNullOrEmpty()) {
                                        val tempVinFile = File.createTempFile(
                                            "vin_temp",
                                            ".jpg",
                                            context.cacheDir
                                        )

                                        storage.getReferenceFromUrl(vinFotoUrl)
                                            .getFile(tempVinFile)
                                            .addOnSuccessListener {

                                                insertarFotoProporcionada(
                                                    doc,
                                                    "{{foto_vin}}",
                                                    tempVinFile,
                                                    280.0,
                                                    180.0
                                                )

                                                tempVinFile.delete()

                                                val file = File(
                                                    context.getExternalFilesDir(null),
                                                    "Reporte_${System.currentTimeMillis()}.docx"
                                                )
                                                FileOutputStream(file).use { doc.write(it) }
                                                doc.close()
                                                onFinish(file)
                                            }
                                            .addOnFailureListener {
                                                // aunque falle la foto, igual guardamos el Word
                                                val file = File(
                                                    context.getExternalFilesDir(null),
                                                    "Reporte_${System.currentTimeMillis()}.docx"
                                                )
                                                FileOutputStream(file).use { doc.write(it) }
                                                doc.close()
                                                onFinish(file)
                                            }
                                    } else {
                                        // no hay foto VIN, guardamos igual
                                        val file = File(
                                            context.getExternalFilesDir(null),
                                            "Reporte_${System.currentTimeMillis()}.docx"
                                        )
                                        FileOutputStream(file).use { doc.write(it) }
                                        doc.close()
                                        onFinish(file)
                                    }

                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(
                                        context,
                                        "Error insertando foto",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                            .addOnFailureListener {
                                it.printStackTrace()
                                Toast.makeText(context, "Error descargando foto", Toast.LENGTH_LONG)
                                    .show()
                            }
                    } else {
                        Toast.makeText(context, "No hay URL de foto para prueba", Toast.LENGTH_LONG)
                            .show()
                    }
                }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                Toast.makeText(context, "Error cargando datos generales", Toast.LENGTH_LONG).show()
            }
    }

    private fun insertarFotoProporcionada(
        doc: XWPFDocument,
        placeholder: String,
        file: File,
        maxAnchoEMU: Double,
        maxAltoEMU: Double
    ) {
        // Primero obtenemos dimensiones reales de la imagen
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(file.absolutePath, options)
        val anchoOriginal = options.outWidth
        val altoOriginal = options.outHeight

        // Calcular proporción para que no supere los máximos
        var anchoFinal = anchoOriginal.toDouble()
        var altoFinal = altoOriginal.toDouble()
        val ratio = min(maxAnchoEMU / anchoFinal, maxAltoEMU / altoFinal)
        anchoFinal *= ratio
        altoFinal *= ratio

        // ------------------
        // PÁRRAFOS
        // ------------------
        for (p in doc.paragraphs) {
            val textoCompleto = p.runs.joinToString("") { it.text() }
            if (textoCompleto.contains(placeholder)) {
                for (run in p.runs) run.setText("", 0)
                p.alignment = ParagraphAlignment.CENTER
                val runFoto = p.createRun()
                val fis = FileInputStream(file)
                runFoto.addPicture(
                    fis,
                    Document.PICTURE_TYPE_JPEG,
                    file.name,
                    Units.toEMU(anchoFinal),
                    Units.toEMU(altoFinal)
                )
                fis.close()
            }
        }

        // ------------------
        // TABLAS
        // ------------------
        for (table in doc.tables) {
            for (row in table.rows) {
                for (cell in row.tableCells) {
                    for (p in cell.paragraphs) {
                        val textoCompleto = p.runs.joinToString("") { it.text() }
                        if (textoCompleto.contains(placeholder)) {
                            for (run in p.runs) run.setText("", 0)
                            p.alignment = ParagraphAlignment.CENTER
                            val runFoto = p.createRun()
                            val fis = FileInputStream(file)
                            runFoto.addPicture(
                                fis,
                                Document.PICTURE_TYPE_JPEG,
                                file.name,
                                Units.toEMU(anchoFinal),
                                Units.toEMU(altoFinal)
                            )
                            fis.close()
                        }
                    }
                }
            }
        }
    }

    private fun insertarFotoDesdeSeccion(
        doc: XWPFDocument,
        placeholder: String,
        evaluacionId: String,
        seccionId: String,
        context: Context,
        db: FirebaseFirestore,
        storage: FirebaseStorage,
        onFinish: () -> Unit
    ) {
        db.collection("evaluaciones")
            .document(evaluacionId)
            .collection("secciones")
            .document(seccionId)
            .get()
            .addOnSuccessListener { docSeccion ->

                val items = docSeccion.get("items") as? List<Map<String, Any>> ?: emptyList()

                val itemConFoto = items.firstOrNull {
                    it["fotoUrl"]?.toString()?.isNotBlank() == true
                }

                val fotoUrl = itemConFoto?.get("fotoUrl")?.toString()

                if (!fotoUrl.isNullOrEmpty()) {

                    val tempFile = File.createTempFile("vin_temp", ".jpg", context.cacheDir)
                    storage.getReferenceFromUrl(fotoUrl)
                        .getFile(tempFile)
                        .addOnSuccessListener {

                            insertarFotoProporcionada(
                                doc,
                                placeholder,
                                tempFile,
                                220.0,
                                140.0
                            )

                            tempFile.delete()
                            onFinish()
                        }
                        .addOnFailureListener { onFinish() }

                } else {
                    onFinish()
                }
            }
            .addOnFailureListener { onFinish() }
    }


    fun abrirWord(context: Context, file: File) {

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        // 1 Intent principal (Word / Docs)
        val intentWord = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(
                uri,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        // 2 Intent fallback (WPS / genérico)
        val intentFallback = Intent(Intent.ACTION_VIEW).apply {
            data = uri
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(
                Intent.createChooser(intentWord, "Abrir reporte con")
            )
        } catch (e: Exception) {
            try {
                context.startActivity(
                    Intent.createChooser(intentFallback, "Abrir reporte con")
                )
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "No se encontró ninguna app para abrir el archivo",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

}
