package com.example.moviles_eventia.models

import java.time.Instant

data class PuntoMapa(val latitud: Double, val longitud: Double, val eventos: List<Event>)

fun agruparEventosMapa(
    eventos: List<Event>,
    categoriaId: Int? = null,
    ahora: Instant = Instant.now()
): List<PuntoMapa> = filtrarEventos(eventos, categoriaId = categoriaId, ahora = ahora)
    .filter { it.venue.tieneCoordenadas }
    .distinctBy { it.id }
    .groupBy { requireNotNull(it.venue.lat) to requireNotNull(it.venue.lng) }
    .map { (coordenadas, agrupados) -> PuntoMapa(coordenadas.first, coordenadas.second, agrupados) }

fun siguienteEventoMapa(puntos: List<PuntoMapa>, seleccionadoId: Int): Event? {
    val eventos = puntos.firstOrNull { punto -> punto.eventos.any { it.id == seleccionadoId } }?.eventos
        ?: return null
    val indice = eventos.indexOfFirst { it.id == seleccionadoId }
    return eventos[(indice + 1) % eventos.size]
}
