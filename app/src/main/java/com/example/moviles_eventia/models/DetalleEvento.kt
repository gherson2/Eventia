package com.example.moviles_eventia.models

// La fuente se decide en el repositorio, no en la pantalla.
data class DetalleEvento(
    val evento: Event,
    val esCopiaGuardada: Boolean = false
)
