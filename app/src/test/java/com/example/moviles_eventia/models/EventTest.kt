package com.example.moviles_eventia.models

import java.time.Instant
import java.util.TimeZone
import org.junit.Assert.*
import org.junit.Test

class EventTest {
    private val ahora = Instant.parse("2026-11-21T12:00:00Z")
    private val lugar = Venue(1, "Teatro", "Calle 1", -17.3, -66.1)
    private fun evento(id: Int, nombre: String, fecha: String, categoria: Int = 1) =
        Event(id, nombre, null, fecha, Category(categoria, "Categoría"), null, lugar, 50, 10)

    @Test fun combinaNombreCategoriaYFecha() {
        val eventos = listOf(
            evento(1, "Jazz en vivo", "2026-12-02T20:00:00-04:00"),
            evento(2, "Jazz taller", "2026-12-01T20:00:00-04:00", 2),
            evento(3, "Jazz pasado", "2026-11-01T20:00:00-04:00"),
            evento(4, "Teatro", "2026-12-01T20:00:00-04:00")
        )
        assertEquals(listOf(1), filtrarEventos(eventos, " JAZZ ", 1, ahora).map { it.id })
    }

    @Test fun ordenaPorInstanteYDescartaFechasInvalidas() {
        val eventos = listOf(
            evento(1, "Más tarde", "2026-12-02T20:00:00-04:00"),
            evento(2, "Inválido", "sin fecha"),
            evento(3, "Primero", "2026-12-01T20:00:00-04:00")
        )
        assertEquals(listOf(3, 1), filtrarEventos(eventos, ahora = ahora).map { it.id })
    }

    @Test fun aceptaEventoQueEmpiezaAhora() {
        assertTrue(evento(1, "Ahora", "2026-11-21T08:00:00-04:00").esProximo(ahora))
    }

    @Test fun convierteLaFechaALaZonaDelDispositivo() {
        val anterior = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            assertTrue(fechaLocal("2026-12-01T20:00:00-04:00").contains("00:00"))
        } finally {
            TimeZone.setDefault(anterior)
        }
    }

    @Test fun fechaInvalidaTieneMensajeComprensible() {
        assertEquals("Fecha no disponible", fechaLocal("no válida"))
    }

    @Test fun validaCoordenadasCompletasYFinitas() {
        assertTrue(lugar.tieneCoordenadas)
        assertFalse(lugar.copy(lng = null).tieneCoordenadas)
        assertFalse(lugar.copy(lat = 91.0).tieneCoordenadas)
        assertFalse(lugar.copy(lng = Double.NaN).tieneCoordenadas)
        assertFalse(lugar.copy(lat = Double.POSITIVE_INFINITY).tieneCoordenadas)
    }
}
