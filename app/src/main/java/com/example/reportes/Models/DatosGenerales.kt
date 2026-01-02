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
    val frontal: String = "VISTA FRONTAL DE LA UNIDAD",
    val km: String = "",
    val hr: String = "",
    val motorUrl: String? = null,
    val fecha: Long = 0L,

    val vinFotoUrl: String? = null,
    val placaFotoUrl: String? = null,
    val frontalFotoUrl: String? = null,
    val kmFotoUrl: String? = null,
    var cajaFotoUrl: String? = null,
    var motorFotoUrl: String? = null
)