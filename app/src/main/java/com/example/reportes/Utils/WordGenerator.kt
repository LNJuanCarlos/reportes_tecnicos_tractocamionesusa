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
        val doc = XWPFDocument()

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

                    val tituloDatos = doc.createParagraph()
                    tituloDatos.createRun().apply {
                        setText("DATOS GENERALES")
                        isBold = true
                    }

                    val table = doc.createTable(9, 2)

                    table.getRow(0).getCell(0).text = "CLIENTE"
                    table.getRow(0).getCell(1).text = datos.cliente

                    table.getRow(1).getCell(0).text = "MARCA"
                    table.getRow(1).getCell(1).text = datos.marca

                    table.getRow(2).getCell(0).text = "VIN"
                    table.getRow(2).getCell(1).text = datos.vin

                    table.getRow(3).getCell(0).text = "MODELO"
                    table.getRow(3).getCell(1).text = datos.modelo

                    table.getRow(4).getCell(0).text = "PLACA"
                    table.getRow(4).getCell(1).text = datos.placa

                    table.getRow(5).getCell(0).text = "AÑO"
                    table.getRow(5).getCell(1).text = datos.anio

                    table.getRow(6).getCell(0).text = "CAJA DE CAMBIOS"
                    table.getRow(6).getCell(1).text = datos.cajaCambios

                    table.getRow(7).getCell(0).text = "MOTOR-MODELO"
                    table.getRow(7).getCell(1).text = datos.motorModelo

                    table.getRow(8).getCell(0).text = "MOTOR-SERIE"
                    table.getRow(8).getCell(1).text = datos.motorSerie
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
