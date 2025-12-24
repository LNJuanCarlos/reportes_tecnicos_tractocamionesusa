package com.example.reportes.Models

data class SeccionTecnica(
    var nombre: String = "",
    var items: List<SeccionItem> = emptyList()
)
