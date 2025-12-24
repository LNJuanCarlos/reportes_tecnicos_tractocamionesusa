package com.example.reportes.Models

data class Evaluacion(
    val cliente: String = "",
    val marca: String = "",
    val vin: String = "",
    val modelo: String = "",
    val placa: String = "",
    val anio: Int = 0,
    val cajaCambios: String = "",
    val motorModelo: String = "",
    val motorSerie: String = "",
    val km: Int = 0,
    val horas: Int = 0,
    val fechaEvaluacion: String = "",
    val fotosGenerales: FotosGenerales = FotosGenerales(),
    val motor: List<SeccionItem> = emptyList(),
    val transmision: List<SeccionItem> = emptyList(),
    val neumaticos: List<SeccionItem> = emptyList(),
    val cabina: List<SeccionItem> = emptyList(),
    val chasis: List<SeccionItem> = emptyList(),
    val conclusiones: String = "",
    val supervisor: Persona = Persona(),
    val tecnico: Persona = Persona()

)
