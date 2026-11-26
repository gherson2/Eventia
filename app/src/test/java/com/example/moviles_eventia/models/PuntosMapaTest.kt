package com.example.moviles_eventia.models

import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class PuntosMapaTest {
    private val ahora = Instant.parse("2026-11-25T12:00:00Z")
    private val evento = Event(1, "Jazz", null, "2099-12-01T20:00:00Z",
        Category(3, "Música"), null, Venue(7, "Teatro", null, -17.39, -66.15), 50, 10)

    @Test fun agrupaCoordenadasCoincidentesAunqueLosLugaresSeanDistintos() {
        val puntos = agruparEventosMapa(listOf(evento,
            evento.copy(id = 2, venue = evento.venue.copy(id = 8))), ahora = ahora)
        assertEquals(1, puntos.size)
        assertEquals(listOf(1, 2), puntos.single().eventos.map { it.id })
    }

    @Test fun descartaCoordenadasNulasFueraDeRangoYNoFinitas() {
        val invalidos = listOf(
            null to -66.0, -17.0 to null, 91.0 to 0.0, 0.0 to -181.0,
            Double.NaN to 0.0, 0.0 to Double.POSITIVE_INFINITY
        ).mapIndexed { indice, (lat, lng) ->
            evento.copy(id = indice + 2, venue = evento.venue.copy(lat = lat, lng = lng))
        }
        assertEquals(listOf(1), agruparEventosMapa(invalidos + evento, ahora = ahora)
            .flatMap { it.eventos }.map { it.id })
    }

    @Test fun respetaCategoriaYExcluyePasadosYFechasInvalidas() {
        val eventos = listOf(evento, evento.copy(id = 2, category = Category(4, "Arte")),
            evento.copy(id = 3, startsAt = "2020-01-01T00:00:00Z"),
            evento.copy(id = 4, startsAt = "sin fecha"))
        assertEquals(listOf(1), agruparEventosMapa(eventos, 3, ahora)
            .flatMap { it.eventos }.map { it.id })
        assertTrue(agruparEventosMapa(eventos, 99, ahora).isEmpty())
    }

    @Test fun noDuplicaEventosYOrdenaPorFecha() {
        val temprano = evento.copy(id = 2, startsAt = "2099-01-01T00:00:00Z")
        assertEquals(listOf(2, 1), agruparEventosMapa(listOf(evento, temprano, evento), ahora = ahora)
            .single().eventos.map { it.id })
    }

    @Test fun recorreEventosCoincidentesCircularmente() {
        val puntos = agruparEventosMapa(listOf(evento, evento.copy(id = 2)), ahora = ahora)
        assertEquals(2, siguienteEventoMapa(puntos, 1)?.id)
        assertEquals(1, siguienteEventoMapa(puntos, 2)?.id)
        assertNull(siguienteEventoMapa(puntos, 999))
        assertNull(siguienteEventoMapa(emptyList(), 1))
    }

    @Test fun aceptaCeroYLimitesGeograficos() {
        listOf(0.0 to 0.0, -90.0 to -180.0, 90.0 to 180.0).forEach { (lat, lng) ->
            assertEquals(1, agruparEventosMapa(
                listOf(evento.copy(venue = evento.venue.copy(lat = lat, lng = lng))), ahora = ahora).size)
        }
    }
}
