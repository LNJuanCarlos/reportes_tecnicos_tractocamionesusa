package com.example.reportes.Models

data class EvaluacionTecnica(
    var id: String = "",
    var fecha: Long = System.currentTimeMillis(),
    var tecnico: String = "",
    var equipo: String = "",
    var secciones: List<SeccionTecnica> = emptyList()
)
