package com.example.reportes.Models

data class ItemSeccion(
    var orden: Int = 0,
    var fotoUrl: String = "",   // Firebase
    var fotoLocal: String = "", // Path local
    var observacion: String = ""
)
