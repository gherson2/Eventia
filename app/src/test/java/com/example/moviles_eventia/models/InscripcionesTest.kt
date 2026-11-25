package com.example.moviles_eventia.models

import com.example.moviles_eventia.ui.screens.RepositorioPrueba
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class InscripcionesTest {
    private val ahora = Instant.parse("2026-11-25T12:00:00Z")
    private val evento = RepositorioPrueba().evento

    @Test fun separaFechasYOrdenaProximasAscendentesYPasadasDescendentes() {
        val grupos = agruparInscripciones(listOf(
            evento.copy(id = 1, startsAt = "2026-12-02T10:00:00Z"),
            evento.copy(id = 2, startsAt = "2026-12-01T10:00:00Z"),
            evento.copy(id = 3, startsAt = "2026-10-01T10:00:00Z"),
            evento.copy(id = 4, startsAt = "2026-11-01T10:00:00Z")
        ), ahora)
        assertEquals(listOf(2, 1), grupos.proximas.map { it.id })
        assertEquals(listOf(4, 3), grupos.pasadas.map { it.id })
        assertTrue(grupos.sinFecha.isEmpty())
    }

    @Test fun respetaZonaHorariaYConsideraProximoElInstanteActual() {
        val grupos = agruparInscripciones(listOf(
            evento.copy(id = 1, startsAt = "2026-11-25T08:00:00-04:00"),
            evento.copy(id = 2, startsAt = "2026-11-25T07:59:00-04:00")
        ), ahora)
        assertEquals(listOf(1), grupos.proximas.map { it.id })
        assertEquals(listOf(2), grupos.pasadas.map { it.id })
    }

    @Test fun fechaInvalidaNoSePierdeNiSePresentaComoPasada() {
        val grupos = agruparInscripciones(listOf(evento.copy(startsAt = "sin fecha")), ahora)
        assertTrue(grupos.proximas.isEmpty())
        assertTrue(grupos.pasadas.isEmpty())
        assertEquals(1, grupos.sinFecha.size)
    }
}
