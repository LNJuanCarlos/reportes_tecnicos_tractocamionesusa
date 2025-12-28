package com.example.reportes.Models

data class DatosGenerales(
    val cliente: String = "",
    val marca: String = "",
    val modelo:String = "",
    val anio: String = "",
    val cajaCambios: String = "",
    val motorModelo: String = "",
    val motorSerie: String = "",
    val placa: String = "",
    val vin: String = "",
    val fotos: List<String>? = null,
    val km: String = "",
    val hr: String = "",
    val placaFrontalUrl: String? = null,
    val vinFotoUrl: String? = null,
    val motorUrl: String? = null
)