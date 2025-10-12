package com.example.directorioempresas

data class Empresa(
    val id: Long,
    val nombre: String,
    val web: String,
    val telefono: String,
    val email: String,
    val productos: String,
    val clasificacion: String
)
