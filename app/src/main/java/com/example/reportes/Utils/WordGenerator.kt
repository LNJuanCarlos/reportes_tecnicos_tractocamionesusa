package com.example.reportes.Utils

import android.content.Context
import android.content.Intent
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
        inputStream.use { input -> templateFile.outputStream().use { output -> input.copyTo(output) } }
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
                        for ((placeholder, valor) in datosMap) {
                            textoCompleto = textoCompleto.replace(placeholder, valor)
                        }
                        // Limpiar runs y poner texto final
                        for (run in p.runs) run.setText("", 0)
                        p.createRun().setText(textoCompleto)
                    }

                    // Reemplazar texto en tablas
                    for (table in doc.tables) {
                        for (row in table.rows) {
                            for (cell in row.tableCells) {
                                for (p in cell.paragraphs) {
                                    var textoCompleto = p.runs.joinToString("") { it.text() }
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
                                    insertarFotoRobusta(doc, "{{foto_placa}}", tempFile, 230.0, 180.0)
                                    tempFile.delete()

                                    // Guardar documento final
                                    val file = File(
                                        context.getExternalFilesDir(null),
                                        "Reporte_${System.currentTimeMillis()}.docx"
                                    )
                                    FileOutputStream(file).use { doc.write(it) }
                                    doc.close()
                                    onFinish(file)

                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, "Error insertando foto", Toast.LENGTH_LONG).show()
                                }
                            }
                            .addOnFailureListener {
                                it.printStackTrace()
                                Toast.makeText(context, "Error descargando foto", Toast.LENGTH_LONG).show()
                            }
                    } else {
                        Toast.makeText(context, "No hay URL de foto para prueba", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                Toast.makeText(context, "Error cargando datos generales", Toast.LENGTH_LONG).show()
            }
    }

    private fun insertarFotoRobusta(
        doc: XWPFDocument,
        placeholder: String,
        file: File,
        anchoEMU: Double,
        altoEMU: Double
    ) {
        // Párrafos
        for (p in doc.paragraphs) {
            val textoCompleto = p.runs.joinToString("") { it.text() }
            if (textoCompleto.contains(placeholder)) {
                // Limpiar todos los runs
                for (run in p.runs) run.setText("", 0)
                // Centrar párrafo
                p.alignment = ParagraphAlignment.CENTER
                val runFoto = p.createRun()
                val fis = FileInputStream(file)
                runFoto.addPicture(
                    fis,
                    Document.PICTURE_TYPE_JPEG,
                    file.name,
                    Units.toEMU(anchoEMU),
                    Units.toEMU(altoEMU)
                )
                fis.close()
            }
        }

        // Tablas
        for (table in doc.tables) {
            for (row in table.rows) {
                for (cell in row.tableCells) {
                    for (p in cell.paragraphs) {
                        val textoCompleto = p.runs.joinToString("") { it.text() }
                        if (textoCompleto.contains(placeholder)) {
                            for (run in p.runs) run.setText("", 0)
                            // Centrar párrafo dentro de la celda
                            p.alignment = ParagraphAlignment.CENTER
                            val runFoto = p.createRun()
                            val fis = FileInputStream(file)
                            runFoto.addPicture(
                                fis,
                                Document.PICTURE_TYPE_JPEG,
                                file.name,
                                Units.toEMU(anchoEMU),
                                Units.toEMU(altoEMU)
                            )
                            fis.close()
                        }
                    }
                }
            }
        }
    }

    private fun agregarSeccionesYGuardar(
        doc: XWPFDocument,
        evaluacionId: String,
        db: FirebaseFirestore,
        context: Context,
        onFinish: (File) -> Unit
    ) {
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

                // Guardar archivo final
                val file = File(
                    context.getExternalFilesDir(null),
                    "Reporte_${System.currentTimeMillis()}.docx"
                )
                FileOutputStream(file).use { doc.write(it) }
                doc.close()
                onFinish(file)
            }
    }



    fun generarWordCompletoPrueba(
        context: Context,
        evaluacionId: String,
        onFinish: (File) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        val storage = FirebaseStorage.getInstance()

        // ===============================
        // ABRIR PLANTILLA
        // ===============================
        val assetManager = context.assets
        val inputStream = assetManager.open("plantilla.docx")
        val templateFile = File(context.filesDir, "plantilla_temp.docx")
        inputStream.use { input -> templateFile.outputStream().use { output -> input.copyTo(output) } }
        val doc = XWPFDocument(FileInputStream(templateFile))

        // ===============================
        // REEMPLAZO DE PLACEHOLDERS DE TEXTO
        // ===============================
        db.collection("evaluaciones")
            .document(evaluacionId)
            .collection("datosGenerales")
            .document("info")
            .get()
            .addOnSuccessListener { datosDoc ->

                val datos = datosDoc.toObject(DatosGenerales::class.java)
                if (datos == null) return@addOnSuccessListener

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

                // Reemplazar en párrafos
                for (paragraph in doc.paragraphs) {
                    var texto = paragraph.text
                    for ((placeholder, valor) in datosMap) {
                        texto = texto.replace(placeholder, valor)
                    }
                    val runs = paragraph.runs
                    for (run in runs) run.setText("", 0)
                    paragraph.createRun().setText(texto)
                }

                // Reemplazar en tablas
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

                // ===============================
                // PLACEHOLDER DE FOTO ({{foto_placa}})
                // ===============================
                // Obtenemos la URL desde Firestore: solo foto de placa
                db.collection("evaluaciones")
                    .document(evaluacionId)
                    .collection("fotos")
                    .whereEqualTo("tipo", "placa_frontal")
                    .limit(1)
                    .get()
                    .addOnSuccessListener { fotosSnapshot ->
                        val fotoDoc = fotosSnapshot.documents.firstOrNull()
                        val urlFoto = fotoDoc?.getString("url")

                        if (urlFoto.isNullOrEmpty()) {
                            // No hay foto, guardamos directamente
                            guardarDoc(doc, context, onFinish)
                        } else {
                            // Descargar foto y reemplazar placeholder
                            val tempFile = File.createTempFile("placa_temp", ".jpg", context.cacheDir)
                            val fotoRef = storage.getReferenceFromUrl(urlFoto)
                            fotoRef.getFile(tempFile)
                                .addOnSuccessListener {
                                    try {
                                        for (paragraph in doc.paragraphs) {
                                            if (paragraph.text.contains("{{foto_placa}}")) {
                                                val runs = paragraph.runs
                                                for (run in runs) run.setText("", 0)

                                                val runFoto = paragraph.createRun()
                                                val fis = FileInputStream(tempFile)
                                                runFoto.addPicture(
                                                    fis,
                                                    Document.PICTURE_TYPE_JPEG,
                                                    tempFile.name,
                                                    Units.toEMU(400.0),
                                                    Units.toEMU(300.0)
                                                )
                                                fis.close()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    } finally {
                                        tempFile.delete()
                                        guardarDoc(doc, context, onFinish)
                                    }
                                }
                                .addOnFailureListener {
                                    it.printStackTrace()
                                    // Aunque falle la foto, guardamos el documento
                                    guardarDoc(doc, context, onFinish)
                                }
                        }
                    }
            }
    }

    private fun guardarDoc(
        doc: XWPFDocument,
        context: Context,
        onFinish: (File) -> Unit
    ) {
        val file = File(context.getExternalFilesDir(null), "Reporte_${System.currentTimeMillis()}.docx")
        FileOutputStream(file).use { doc.write(it) }
        doc.close()
        onFinish(file)
    }

    fun generarWordCompleto(
        context: Context,
        evaluacionId: String,
        onFinish: (File) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        val storage = FirebaseStorage.getInstance()
        val fotosMap = mutableMapOf<String, String?>()

        // ===============================
        // ABRIR PLANTILLA DESDE ASSETS
        // ===============================
        val assetManager = context.assets
        val inputStream = assetManager.open("plantilla_reporte_tecnico.docx")
        val templateFile = File(context.filesDir, "plantilla_temp.docx")
        inputStream.use { input ->
            templateFile.outputStream().use { output -> input.copyTo(output) }
        }
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
                    // PLACEHOLDERS DE TEXTO
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

                    // Reemplazar en párrafos
                    for (paragraph in doc.paragraphs) {
                        var texto = paragraph.text
                        for ((placeholder, valor) in datosMap) texto =
                            texto.replace(placeholder, valor)
                        val runs = paragraph.runs
                        for (run in runs) run.setText("", 0)
                        paragraph.createRun().setText(texto)
                    }

                    // Reemplazar en tablas
                    for ((placeholder, urlFoto) in fotosMap) {
                        urlFoto?.let { url -> // url es String no nulo aquí
                            val ref = storage.getReferenceFromUrl(url)

                            // Reemplazar placeholder en párrafos
                            for (paragraph in doc.paragraphs) {
                                if (paragraph.text.contains(placeholder)) {
                                    val runs = paragraph.runs
                                    for (run in runs) run.setText("", 0) // borrar placeholder

                                    val runFoto = paragraph.createRun()
                                    val tempFile = File.createTempFile("foto_temp", ".jpg", context.cacheDir)

                                    ref.getFile(tempFile).addOnSuccessListener {
                                        try {
                                            val fis = FileInputStream(tempFile)
                                            runFoto.addPicture(
                                                fis,
                                                Document.PICTURE_TYPE_JPEG,
                                                tempFile.name,
                                                Units.toEMU(400.0),
                                                Units.toEMU(300.0)
                                            )
                                            fis.close()
                                            tempFile.delete()
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }.addOnFailureListener {
                                        it.printStackTrace()
                                    }
                                }
                            }
                        }
                    }

                    // ===============================
                    // PLACEHOLDERS DE FOTOS
                    // ===============================
                    // Se asume que en Firestore guardaste cada foto con un "tipo"
                    db.collection("evaluaciones").document(evaluacionId)
                        .collection("fotos").get()
                        .addOnSuccessListener { fotosSnapshot ->
                            val fotosMap = mutableMapOf<String, String>()

                            for (docFoto in fotosSnapshot) {
                                val tipo = docFoto.getString("tipo") // ejemplo: "placa_frontal"
                                val url = docFoto.getString("url")
                                if (!tipo.isNullOrBlank() && !url.isNullOrBlank()) {
                                    when (tipo) {
                                        "placa_frontal" -> fotosMap["{{foto_placa}}"] = url
                                        "motor" -> fotosMap["{{foto_frontal}}"] = url
                                        // Agregar más tipos según tus placeholders
                                    }
                                }
                            }

                            var fotosPendientes = fotosMap.size
                            if (fotosPendientes == 0) {
                                agregarSeccionesYGuardar(doc, evaluacionId, db, context, onFinish)
                            } else {
                                for ((placeholder, urlFoto) in fotosMap) {
                                    // Reemplazar placeholder en párrafos
                                    for (paragraph in doc.paragraphs) {
                                        if (paragraph.text.contains(placeholder)) {
                                            val runs = paragraph.runs
                                            for (run in runs) run.setText(
                                                "",
                                                0
                                            ) // borrar placeholder

                                            val runFoto = paragraph.createRun()
                                            val tempFile = File.createTempFile(
                                                "foto_temp",
                                                ".jpg",
                                                context.cacheDir
                                            )
                                            val fotoRef = storage.getReferenceFromUrl(urlFoto)

                                            fotoRef.getFile(tempFile).addOnSuccessListener {
                                                try {
                                                    val fis = FileInputStream(tempFile)
                                                    runFoto.addPicture(
                                                        fis,
                                                        Document.PICTURE_TYPE_JPEG,
                                                        tempFile.name,
                                                        Units.toEMU(400.0),
                                                        Units.toEMU(300.0)
                                                    )
                                                    fis.close()
                                                    tempFile.delete()
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                } finally {
                                                    fotosPendientes--
                                                    if (fotosPendientes == 0) {
                                                        agregarSeccionesYGuardar(
                                                            doc,
                                                            evaluacionId,
                                                            db,
                                                            context,
                                                            onFinish
                                                        )
                                                    }
                                                }
                                            }.addOnFailureListener {
                                                it.printStackTrace()
                                                fotosPendientes--
                                                if (fotosPendientes == 0) {
                                                    agregarSeccionesYGuardar(
                                                        doc,
                                                        evaluacionId,
                                                        db,
                                                        context,
                                                        onFinish
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                }
            }
    }

    // ===============================
// FUNCION PARA AGREGAR SECCIONES TÉCNICAS Y GUARDAR
// ===============================
    /*private fun agregarSeccionesYGuardar(
        doc: XWPFDocument,
        evaluacionId: String,
        db: FirebaseFirestore,
        context: Context,
        onFinish: (File) -> Unit
    ) {
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

                // Guardar archivo final
                val file = File(
                    context.getExternalFilesDir(null),
                    "Reporte_${System.currentTimeMillis()}.docx"
                )
                FileOutputStream(file).use { doc.write(it) }
                doc.close()
                onFinish(file)
            }
    }*/


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
