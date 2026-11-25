package com.example.moviles_eventia.models

import java.time.Instant

data class GruposInscripciones(
    val proximas: List<Event>,
    val pasadas: List<Event>,
    val sinFecha: List<Event>
)

fun agruparInscripciones(eventos: List<Event>, ahora: Instant = Instant.now()): GruposInscripciones {
    val conFecha = eventos.map { it to it.instante() }
    return GruposInscripciones(
        proximas = conFecha.filter { (_, fecha) -> fecha != null && !fecha.isBefore(ahora) }
            .sortedBy { it.second }.map { it.first },
        pasadas = conFecha.filter { (_, fecha) -> fecha != null && fecha.isBefore(ahora) }
            .sortedByDescending { it.second }.map { it.first },
        sinFecha = conFecha.filter { it.second == null }.map { it.first }
    )
}
