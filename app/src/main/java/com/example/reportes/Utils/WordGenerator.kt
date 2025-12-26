package com.example.reportes.Utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.reportes.Models.DatosGenerales
import com.example.reportes.Models.ItemSeccion
import com.example.reportes.Models.SeccionTecnicaFirestore
import com.google.firebase.firestore.FirebaseFirestore
import org.apache.poi.util.Units
import org.apache.poi.xwpf.usermodel.Document
import org.apache.poi.xwpf.usermodel.ParagraphAlignment
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object WordGenerator {

    fun generarWordCompleto(
        context: Context,
        evaluacionId: String,
        onFinish: (File) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()

        // ===============================
        // ABRIR PLANTILLA DESDE ASSETS
        // ===============================
        val assetManager = context.assets
        val inputStream = assetManager.open("plantilla_reporte_tecnico.docx")

        // Copiamos a un archivo temporal para poder modificarlo
        val templateFile = File(context.filesDir, "plantilla_temp.docx")
        inputStream.use { input ->
            templateFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        // Abrimos el documento con Apache POI
        val doc = XWPFDocument(FileInputStream(templateFile))

        // ===============================
        // TÍTULO
        // ===============================
        val title = doc.createParagraph()
        title.alignment = ParagraphAlignment.CENTER
        title.createRun().apply {
            setText("REPORTE TÉCNICO")
            isBold = true
            fontSize = 16
        }
        doc.createParagraph()

        // ===============================
        // DATOS GENERALES
        // ===============================
        db.collection("evaluaciones")
            .document(evaluacionId)
            .collection("datosGenerales")
            .document("info")
            .get()
            .addOnSuccessListener { datosDoc ->

                val datos = datosDoc.toObject(DatosGenerales::class.java)

                if (datos != null) {

                    // -------------------------------
                    // REEMPLAZAR PLACEHOLDERS
                    // -------------------------------
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

                    // Reemplazar en todos los párrafos
                    for (paragraph in doc.paragraphs) {
                        var texto = paragraph.text
                        for ((placeholder, valor) in datosMap) {
                            texto = texto.replace(placeholder, valor)
                        }
                        val runs = paragraph.runs
                        for (run in runs) run.setText("", 0)
                        paragraph.createRun().setText(texto)
                    }

                    // Reemplazar también dentro de tablas
                    for (table in doc.tables) {
                        for (row in table.rows) {
                            for (cell in row.tableCells) {
                                var texto = cell.text
                                for ((placeholder, valor) in datosMap) {
                                    texto = texto.replace(placeholder, valor)
                                }
                                cell.removeParagraph(0)
                                cell.setText(texto)
                            }
                        }
                    }

                    // -------------------------------
                    // AGREGAR FOTOS
                    // -------------------------------
                    // Suponiendo que `datos.fotos` es List<String> con paths locales o URLs descargadas
                    datos.fotos?.forEach { fotoPath ->
                        try {
                            val paragraph = doc.createParagraph()
                            paragraph.alignment = ParagraphAlignment.CENTER
                            val run = paragraph.createRun()
                            val imageFile = File(fotoPath)
                            if (imageFile.exists()) {
                                val fis = FileInputStream(imageFile)
                                run.addPicture(
                                    fis,
                                    Document.PICTURE_TYPE_JPEG,
                                    imageFile.name,
                                    Units.toEMU(400.0),
                                    Units.toEMU(300.0)
                                )
                                fis.close()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                doc.createParagraph()

                // ===============================
                // SECCIONES TÉCNICAS
                // ===============================
                db.collection("evaluaciones")
                    .document(evaluacionId)
                    .collection("secciones")
                    .get()
                    .addOnSuccessListener { seccionesSnapshot ->

                        for (seccion in seccionesSnapshot) {

                            val titulo = seccion.id.uppercase()

                            val p = doc.createParagraph()
                            p.createRun().apply {
                                setText("SECCIÓN: $titulo")
                                isBold = true
                            }

                            val data = seccion.toObject(SeccionTecnicaFirestore::class.java)

                            data.items.forEach { item ->
                                if (item.observacion.isNotBlank()) {
                                    val obs = doc.createParagraph()
                                    obs.createRun().setText("• ${item.observacion}")
                                }
                            }

                            doc.createParagraph()
                        }

                        // ===============================
                        // GUARDAR ARCHIVO
                        // ===============================
                        val file = File(
                            context.getExternalFilesDir(null),
                            "Reporte_${System.currentTimeMillis()}.docx"
                        )

                        FileOutputStream(file).use {
                            doc.write(it)
                        }

                        doc.close()
                        onFinish(file)
                    }
            }
    }


    fun generarWord(
        context: Context,
        datos: DatosGenerales
    ): File {

        val doc = XWPFDocument()

        // TÍTULO
        val title = doc.createParagraph()
        title.alignment = ParagraphAlignment.CENTER
        val run = title.createRun()
        run.setText("REPORTE TÉCNICO")
        run.isBold = true
        run.fontSize = 16

        doc.createParagraph()

        // TABLA
        val table = doc.createTable(6, 2)
        table.getRow(0).getCell(0).text = "Cliente"
        table.getRow(0).getCell(1).text = datos.cliente

        table.getRow(1).getCell(0).text = "Marca"
        table.getRow(1).getCell(1).text = datos.marca

        table.getRow(2).getCell(0).text = "Placa"
        table.getRow(2).getCell(1).text = datos.placa

        table.getRow(3).getCell(0).text = "VIN"
        table.getRow(3).getCell(1).text = datos.vin

        table.getRow(4).getCell(0).text = "KM"
        table.getRow(4).getCell(1).text = datos.km

        table.getRow(5).getCell(0).text = "HR"
        table.getRow(5).getCell(1).text = datos.hr

        val file = File(
            context.getExternalFilesDir(null),
            "Reporte_${System.currentTimeMillis()}.docx"
        )

        FileOutputStream(file).use {
            doc.write(it)
        }

        return file
    }

    fun generarWordDemo(context: Context) {

        val document = XWPFDocument()

        // =========================
        // TÍTULO
        // =========================
        val titulo = document.createParagraph()
        titulo.alignment = org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER
        val runTitulo = titulo.createRun()
        runTitulo.setBold(true)
        runTitulo.fontSize = 18
        runTitulo.setText("REPORTE TÉCNICO")

        document.createParagraph()

        // =========================
        // DATOS GENERALES
        // =========================
        val datos = document.createParagraph()
        val runDatos = datos.createRun()
        runDatos.setBold(true)
        runDatos.setText("DATOS GENERALES")

        val info = document.createParagraph()
        val runInfo = info.createRun()
        runInfo.setText(
            """
            Cliente: Juan Pérez
            Marca: Volvo
            Placa: ABC-123
            VIN: 123456789
            Km: 150000
            Hr: 3200
            """.trimIndent()
        )

        document.createParagraph()

        // =========================
        // SECCIÓN TÉCNICA
        // =========================
        val seccion = document.createParagraph()
        val runSeccion = seccion.createRun()
        runSeccion.setBold(true)
        runSeccion.setText("SECCIÓN: PLACA / FRONTAL")

        val item = document.createParagraph()
        item.createRun().setText(
            """
            Observación:
            Golpe en parachoques delantero.
            """.trimIndent()
        )

        // =========================
        // GUARDAR ARCHIVO
        // =========================
        val file = File(
            context.getExternalFilesDir(null),
            "reporte_demo.docx"
        )

        FileOutputStream(file).use {
            document.write(it)
        }

        document.close()

        abrirWord(context, file)
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
