package com.example.moviles_eventia.models

import org.junit.Assert.assertEquals
import org.junit.Test

class EstadoCatalogoTest {
    @Test fun muestraMinutosHorasYDiasSinEdadesNegativas() {
        val ahora = 200_000_000L
        assertEquals("menos de 1 min", antiguedadCache(ahora + 1_000, ahora))
        assertEquals("menos de 1 min", antiguedadCache(ahora - 30_000, ahora))
        assertEquals("15 min", antiguedadCache(ahora - 15 * 60_000, ahora))
        assertEquals("2 h", antiguedadCache(ahora - 120 * 60_000, ahora))
        assertEquals("2 días", antiguedadCache(ahora - 2 * 86_400_000, ahora))
    }
}
