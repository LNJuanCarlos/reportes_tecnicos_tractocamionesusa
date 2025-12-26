package com.example.reportes.Models

data class SeccionTecnicaFirestore(
    val completada: Boolean = true,
    val items: List<ItemSeccion> = emptyList()
)
