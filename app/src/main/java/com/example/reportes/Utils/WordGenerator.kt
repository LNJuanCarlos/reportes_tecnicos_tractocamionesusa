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
                        "{{motor_serie}}" to datos.motorSerie,
                        "{{km}}" to datos.km,
                        "{{horas}}" to datos.hr,
                        "{{frontal}}" to datos.frontal
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
                    // ------------------------------
                    val urlFoto = datos.placaFotoUrl

                    val fotosWord = mapOf(
                        "{{foto_placa}}" to datos.placaFotoUrl,
                        "{{foto_vin}}" to datos.vinFotoUrl,
                        "{{foto_frontal}}" to datos.frontalFotoUrl,
                        "{{foto_km}}" to datos.kmFotoUrl
                    )

                    val fotosIterator = fotosWord.entries.iterator()

                    fun procesarSiguienteFoto() {
                        if (!fotosIterator.hasNext()) {
                            val file = File(
                                context.getExternalFilesDir(null),
                                "Reporte_${System.currentTimeMillis()}.docx"
                            )
                            FileOutputStream(file).use { doc.write(it) }
                            doc.close()
                            onFinish(file)
                            return
                        }

                        val (placeholder, url) = fotosIterator.next()

                        insertarFotoSiExiste(
                            context,
                            storage,
                            doc,
                            placeholder,
                            url
                        ) {
                            procesarSiguienteFoto()
                        }
                    }

                    procesarSiguienteFoto()
                }
            }
    }


    private fun insertarFotoSiExiste(
        context: Context,
        storage: FirebaseStorage,
        doc: XWPFDocument,
        placeholder: String,
        url: String?,
        onFinish: () -> Unit
    ) {
        if (url.isNullOrEmpty()) {
            onFinish()
            return
        }

        val tempFile = File.createTempFile("img_temp", ".jpg", context.cacheDir)

        storage.getReferenceFromUrl(url)
            .getFile(tempFile)
            .addOnSuccessListener {
                insertarFotoProporcionada(
                    doc,
                    placeholder,
                    tempFile,
                    420.0,
                    180.0
                )
                tempFile.delete()
                onFinish()
            }
            .addOnFailureListener {
                tempFile.delete()
                onFinish()
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

    fun abrirWord(context: Context, file: File) {

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
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
