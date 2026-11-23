package com.example.moviles_eventia.models

data class EstadoCatalogo(
    val eventos: List<Event>? = null,
    val cargando: Boolean = false,
    val guardadoEn: Long? = null,
    val aviso: String? = null
)

fun antiguedadCache(guardadoEn: Long, ahora: Long = System.currentTimeMillis()): String {
    val minutos = ((ahora - guardadoEn).coerceAtLeast(0) / 60_000)
    return when {
        minutos == 0L -> "menos de 1 min"
        minutos < 60 -> "$minutos min"
        minutos < 1_440 -> "${minutos / 60} h"
        else -> "${minutos / 1_440} días"
    }
}
