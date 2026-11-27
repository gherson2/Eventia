package com.example.moviles_eventia.integration

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

// Red desconectada inyectada: no cambia modo avión ni ajustes del usuario.
class SinConexionAndroidTest {
    private lateinit var entorno: EntornoAndroidPrueba
    @Before fun preparar() { entorno = EntornoAndroidPrueba() }
    @After fun terminar() { entorno.close() }

    @Test fun cacheYDetalleFavoritoSobrevivenReaperturaSinPeticionesDeRed() = runBlocking {
        entorno.responder(listOf(entorno.evento))
        val remoto = entorno.eventos().observarCatalogo().toList().last()
        assertEquals(listOf(entorno.evento), remoto.eventos)
        entorno.responder(listOf(entorno.evento.category))
        entorno.eventos().obtenerCategorias()
        entorno.favoritos().guardar(entorno.evento)
        assertEquals(2, entorno.servidor.requestCount)
        entorno.reabrirBase()
        entorno.conectado.value = false
        val copia = entorno.eventos().observarCatalogo().toList().last()
        assertEquals(listOf(entorno.evento), copia.eventos)
        assertNotNull(copia.guardadoEn)
        assertEquals(listOf(entorno.evento.category), entorno.eventos().obtenerCategorias())
        val detalle = entorno.eventos().obtenerDetalleEvento(42)
        assertTrue(detalle.esCopiaGuardada)
        assertEquals("Descripción completa sin conexión", detalle.evento.description)
        entorno.favoritos().quitar(42)
        assertTrue(entorno.favoritos().observarFavoritos().first().isEmpty())
        entorno.favoritos().guardar(detalle.evento)
        assertEquals(1, entorno.favoritos().observarFavoritos().first().size)
        assertEquals(2, entorno.servidor.requestCount)
    }

    @Test fun errorDeActualizacionMantieneCacheYRecuperacionLaReemplaza() = runBlocking {
        entorno.responder(listOf(entorno.evento))
        entorno.eventos().observarCatalogo().toList()
        entorno.responder(mapOf("message" to "Error temporal"), 503)
        val fallo = entorno.eventos().observarCatalogo().toList().last()
        assertEquals(listOf(entorno.evento), fallo.eventos)
        assertNotNull(fallo.aviso)
        assertNotNull(fallo.guardadoEn)
        val actualizado = entorno.evento.copy(id = 43, title = "Evento actualizado")
        entorno.responder(listOf(actualizado))
        val recuperado = entorno.eventos().observarCatalogo().toList().last()
        assertEquals(listOf(actualizado), recuperado.eventos)
        assertNull(recuperado.guardadoEn)
        entorno.conectado.value = false
        assertEquals(listOf(actualizado), entorno.eventos().observarCatalogo().toList().last().eventos)
        assertEquals(3, entorno.servidor.requestCount)
    }
}
