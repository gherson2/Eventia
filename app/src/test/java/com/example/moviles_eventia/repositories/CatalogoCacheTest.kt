package com.example.moviles_eventia.repositories

import com.example.moviles_eventia.api.EventiaApi
import com.example.moviles_eventia.db.entities.CacheCatalogoEntity
import com.example.moviles_eventia.models.Category
import com.example.moviles_eventia.models.Event
import com.example.moviles_eventia.ui.screens.RepositorioPrueba
import com.google.gson.Gson
import java.io.IOException
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CatalogoCacheTest {
    private lateinit var servidor: MockWebServer
    private lateinit var repo: EventRepository
    private lateinit var cache: CacheCatalogoPrueba
    private lateinit var conexion: ConectividadPrueba
    private lateinit var favoritos: FavoritosPrueba
    private val gson = Gson()
    private val evento = RepositorioPrueba().evento

    @Before fun preparar() {
        servidor = MockWebServer()
        servidor.start()
        val api = Retrofit.Builder().baseUrl(servidor.url("/api/"))
            .addConverterFactory(GsonConverterFactory.create()).build()
            .create(EventiaApi::class.java)
        cache = CacheCatalogoPrueba()
        conexion = ConectividadPrueba()
        favoritos = FavoritosPrueba()
        repo = EventRepositoryImpl(api, favoritos, conexion, cache, gson)
    }

    @After fun terminar() { servidor.shutdown() }

    private suspend fun guardar(eventos: List<Event>, clave: String = "catalogo", fecha: Long = 1L) {
        cache.guardar(CacheCatalogoEntity(clave, gson.toJson(eventos), fecha))
    }

    private fun responder(eventos: List<Event>) {
        servidor.enqueue(MockResponse().setHeader("Content-Type", "application/json")
            .setBody(gson.toJson(eventos)))
    }

    @Test fun emitePrimeroLaCopiaYDespuesReemplazaConLaRed() = runTest {
        guardar(listOf(evento.copy(title = "Antiguo")))
        responder(listOf(evento))
        val resultados = repo.observarCatalogo().toList()
        assertTrue(resultados.first().cargando)
        assertEquals("Antiguo", resultados.first().eventos!!.single().title)
        assertEquals(1L, resultados.first().guardadoEn)
        assertEquals("Jazz", resultados.last().eventos!!.single().title)
        assertNull(resultados.last().guardadoEn)
        assertFalse(resultados.last().cargando)
        assertTrue(cache.datos.getValue("catalogo").guardadoEn > 1L)
    }

    @Test fun sinRedFiltraLaCopiaPorNombreCategoriaYFecha() = runTest {
        guardar(listOf(
            evento,
            evento.copy(id = 43, category = Category(4, "Otra")),
            evento.copy(id = 44, startsAt = "2020-01-01T10:00:00Z")
        ))
        conexion.conectado.value = false
        val resultado = repo.observarCatalogo("JAZZ", 3).toList().last()
        assertEquals(listOf(evento), resultado.eventos)
        assertEquals(1L, resultado.guardadoEn)
        assertNull(resultado.aviso)
        assertEquals(0, servidor.requestCount)
    }

    @Test fun buscarNoReemplazaElCatalogoCompletoNiLosFavoritos() = runTest {
        val completo = listOf(evento, evento.copy(id = 43, title = "Teatro"))
        guardar(completo)
        favoritos.guardar(evento)
        val anterior = cache.datos.getValue("catalogo")
        responder(listOf(evento))
        repo.observarCatalogo("Jazz", 3).toList()
        assertEquals(anterior, cache.datos["catalogo"])
        assertTrue(cache.datos.containsKey("eventos:jazz:3"))
        assertEquals(evento, favoritos.obtenerFavorito(evento.id))
        conexion.conectado.value = false
        assertEquals(completo, repo.observarCatalogo().toList().last().eventos)
    }

    @Test fun sinRedNiCacheNoInventaUnaListaVaciaExitosa() = runTest {
        conexion.conectado.value = false
        val resultado = repo.observarCatalogo().toList().last()
        assertNull(resultado.eventos)
        assertNotNull(resultado.aviso)
        assertFalse(resultado.cargando)
        assertEquals(0, servidor.requestCount)
    }

    @Test fun falloDeActualizacionMantieneLaCopiaYLaAntiguedad() = runTest {
        guardar(listOf(evento))
        val anterior = cache.datos.getValue("catalogo")
        servidor.enqueue(MockResponse().setResponseCode(503))
        val resultado = repo.observarCatalogo().toList().last()
        assertEquals(listOf(evento), resultado.eventos)
        assertEquals(1L, resultado.guardadoEn)
        assertNotNull(resultado.aviso)
        assertEquals(anterior, cache.datos["catalogo"])
    }

    @Test fun respuestaVaciaValidaReemplazaLaCopiaAnterior() = runTest {
        guardar(listOf(evento))
        responder(emptyList())
        assertEquals(emptyList<Event>(), repo.observarCatalogo().toList().last().eventos)
        conexion.conectado.value = false
        val copia = repo.observarCatalogo().toList().last()
        assertEquals(emptyList<Event>(), copia.eventos)
        assertNotNull(copia.guardadoEn)
        assertNull(copia.aviso)
    }

    @Test fun falloAlGuardarNoOcultaLaRespuestaRemota() = runTest {
        cache.errorEscritura = IOException("Sin espacio")
        responder(listOf(evento))
        val resultado = repo.observarCatalogo().toList().last()
        assertEquals(listOf(evento), resultado.eventos)
        assertNull(resultado.guardadoEn)
        assertTrue(resultado.aviso!!.contains("no pudimos guardarlos"))
    }

    @Test fun categoriasGuardadasPermitenFiltrarSinRed() = runTest {
        servidor.enqueue(MockResponse().setBody("""[{"id":3,"name":"Música"}]"""))
        assertEquals(listOf(Category(3, "Música")), repo.obtenerCategorias())
        conexion.conectado.value = false
        assertEquals(listOf(Category(3, "Música")), repo.obtenerCategorias())
        assertEquals(1, servidor.requestCount)
    }

    @Test fun unaCopiaCompletaMasRecienteSuperaLaBusquedaAntigua() = runTest {
        guardar(listOf(evento.copy(title = "Jazz antiguo")), "eventos:jazz:3", 1L)
        guardar(listOf(evento), fecha = 2L)
        conexion.conectado.value = false
        val resultado = repo.observarCatalogo("Jazz", 3).toList().last()
        assertEquals(listOf(evento), resultado.eventos)
        assertEquals(2L, resultado.guardadoEn)
    }

    @Test fun cacheDanadaNoImpideRecuperarElCatalogoDesdeLaRed() = runTest {
        cache.datos["catalogo"] = CacheCatalogoEntity("catalogo", "{invalido", 1L)
        responder(listOf(evento))
        val resultado = repo.observarCatalogo().toList().last()
        assertEquals(listOf(evento), resultado.eventos)
        assertNull(resultado.aviso)
    }

    @Test fun catalogoNoGuardaUnaInscripcionPersonalComoEstadoDeCuenta() = runTest {
        responder(listOf(evento.copy(isAttending = true)))
        val resultado = repo.observarCatalogo().toList().last()
        assertFalse(resultado.eventos!!.single().isAttending)
        val json = com.google.gson.JsonParser.parseString(cache.datos.getValue("catalogo").json).asJsonArray
        assertFalse(json[0].asJsonObject["isAttending"].asBoolean)
    }
}
