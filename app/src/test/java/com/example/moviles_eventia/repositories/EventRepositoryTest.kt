package com.example.moviles_eventia.repositories

import com.example.moviles_eventia.api.EventiaApi
import com.example.moviles_eventia.api.dto.EventDto
import com.google.gson.Gson
import com.example.moviles_eventia.ui.screens.RepositorioPrueba
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class EventRepositoryTest {
    private lateinit var servidor: MockWebServer
    private lateinit var repositorio: EventRepository
    private lateinit var api: EventiaApi
    private lateinit var favoritos: FavoritosPrueba
    private lateinit var conectividad: ConectividadPrueba
    private lateinit var cache: CacheCatalogoPrueba
    private val eventoJson = """
        {
          "id":42,"title":"Jazz en vivo","summary":"Música","description":"Descripción completa",
          "startsAt":"2099-12-01T20:00:00-04:00","endsAt":"2099-12-01T22:00:00-04:00",
          "category":{"id":3,"name":"Música"},"imageUrl":null,
          "venue":{"id":7,"name":"Casa cultural","address":"Calle 7","lat":-17.3,"lng":null},
          "capacity":50,"availableSpots":0
        }
    """.trimIndent()

    @Before fun preparar() {
        servidor = MockWebServer()
        servidor.start()
        api = Retrofit.Builder()
            .baseUrl(servidor.url("/api/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(EventiaApi::class.java)
        favoritos = FavoritosPrueba()
        conectividad = ConectividadPrueba()
        cache = CacheCatalogoPrueba()
        repositorio = EventRepositoryImpl(api, favoritos, conectividad, cache, Gson())
    }

    @After fun terminar() { servidor.shutdown() }

    private fun responder(json: String) {
        servidor.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody(json))
    }

    @Test fun consultaBusquedaYCategoriaEnLaMismaSolicitud() = runTest {
        responder("[$eventoJson]")
        val eventos = repositorio.obtenerEventos(" Jazz ", 3)
        val solicitud = servidor.takeRequest()
        assertEquals("/api/events", solicitud.requestUrl!!.encodedPath)
        assertEquals("Jazz", solicitud.requestUrl!!.queryParameter("q"))
        assertEquals("3", solicitud.requestUrl!!.queryParameter("category"))
        assertEquals(42, eventos.single().id)
        assertNull(eventos.single().imageUrl)
    }

    @Test fun omiteBusquedaVaciaYFiltraPasados() = runTest {
        responder("[${eventoJson.replace("2099-", "2020-")}]")
        assertTrue(repositorio.obtenerEventos().isEmpty())
        assertEquals("/api/events", servidor.takeRequest().path)
    }

    @Test fun obtieneDetalleCompleto() = runTest {
        responder(eventoJson)
        val evento = repositorio.obtenerEvento(42)
        assertEquals("Descripción completa", evento.description)
        assertEquals(0, evento.availableSpots)
        assertFalse(evento.venue.tieneCoordenadas)
        assertEquals("/api/events/42", servidor.takeRequest().path)
    }

    @Test fun obtieneCategoriasYLugares() = runTest {
        responder("""[{"id":3,"name":"Música"}]""")
        assertEquals("Música", repositorio.obtenerCategorias().single().name)
        assertEquals("/api/categories", servidor.takeRequest().path)
        responder("""[{"id":7,"name":"Casa cultural","openingHours":"09:00–18:00","upcomingEventsCount":2}]""")
        val lugar = repositorio.obtenerLugares().single()
        assertEquals("09:00–18:00", lugar.openingHours)
        assertEquals(2, lugar.upcomingEventsCount)
        assertEquals("/api/venues", servidor.takeRequest().path)
    }

    @Test fun obtieneLugarYSusEventosPorRutasSeparadas() = runTest {
        responder("""{"id":7,"name":"Casa cultural","description":"Auditorio"}""")
        assertEquals("Auditorio", repositorio.obtenerLugar(7).description)
        assertEquals("/api/venues/7", servidor.takeRequest().path)
        responder("[$eventoJson]")
        assertEquals(42, repositorio.obtenerEventosDelLugar(7).single().id)
        assertEquals("/api/venues/7/events", servidor.takeRequest().path)
    }

    @Test fun conservaElCodigo404ParaLaPresentacion() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(404).setBody("""{"message":"Not found"}"""))
        try {
            repositorio.obtenerEvento(999)
            fail("Se esperaba 404")
        } catch (e: HttpException) {
            assertEquals(404, e.code())
        }
    }

    @Test fun transformaNulosSinEnviarDtoALaUi() {
        val dto = Gson().fromJson("""{"id":1,"title":null}""", EventDto::class.java)
        val evento = dto.toEvent()
        assertEquals("Evento", evento.title)
        assertEquals("Sin categoría", evento.category.name)
        assertEquals("Lugar no disponible", evento.venue.name)
        assertNull(evento.availableSpots)
    }

    @Test fun resuelveImagenesRelativasYRechazaEsquemasInseguros() {
        assertEquals("https://example.com/storage/foto.jpg",
            direccionImagen("/storage/foto.jpg", "https://example.com/api/"))
        assertEquals("https://example.com/storage/foto.jpg",
            direccionImagen("http://localhost/storage/foto.jpg", "https://example.com/api/"))
        assertNull(direccionImagen("file:///private/photo.jpg"))
        assertNull(direccionImagen(null))
    }

    @Test fun sinConexionAbreElFavoritoSinConsultarLaApi() = runTest {
        val guardado = RepositorioPrueba().evento
        favoritos.guardar(guardado)
        conectividad.conectado.value = false
        val detalle = repositorio.obtenerDetalleEvento(guardado.id)
        assertEquals(guardado, detalle.evento)
        assertTrue(detalle.esCopiaGuardada)
        assertEquals(0, servidor.requestCount)
    }

    @Test fun sinConexionNiFavoritoInformaErrorSinSolicitudes() = runTest {
        conectividad.conectado.value = false
        try {
            repositorio.obtenerDetalleEvento(999)
            fail("No debe inventar un detalle")
        } catch (_: IOException) {
            assertEquals(0, servidor.requestCount)
        }
    }

    @Test fun conConexionPrefiereElDetalleRemotoYNoLoMarcaComoGuardado() = runTest {
        favoritos.guardar(RepositorioPrueba().evento.copy(title = "Título antiguo"))
        responder(eventoJson)
        val detalle = repositorio.obtenerDetalleEvento(42)
        assertFalse(detalle.esCopiaGuardada)
        assertEquals("Jazz en vivo", detalle.evento.title)
        assertEquals("/api/events/42", servidor.takeRequest().path)
    }

    @Test fun falloDeRedUsaCopiaAunqueAndroidIndiqueConexion() = runTest {
        val guardado = RepositorioPrueba().evento
        favoritos.guardar(guardado)
        val apiFallida = object : EventiaApi by api {
            override suspend fun obtenerEvento(id: Int): EventDto = throw IOException("Sin respuesta")
        }
        val repo = EventRepositoryImpl(apiFallida, favoritos, conectividad, cache, Gson())
        val detalle = repo.obtenerDetalleEvento(guardado.id)
        assertEquals(guardado, detalle.evento)
        assertTrue(detalle.esCopiaGuardada)
    }

    @Test fun errorDelServidorPermiteConsultarLaCopiaGuardada() = runTest {
        favoritos.guardar(RepositorioPrueba().evento)
        servidor.enqueue(MockResponse().setResponseCode(503))
        assertTrue(repositorio.obtenerDetalleEvento(42).esCopiaGuardada)
    }

    @Test fun un404NoSeOcultaConElFavoritoAntiguo() = runTest {
        favoritos.guardar(RepositorioPrueba().evento)
        servidor.enqueue(MockResponse().setResponseCode(404))
        try {
            repositorio.obtenerDetalleEvento(42)
            fail("Debe informar que el evento no existe")
        } catch (e: HttpException) {
            assertEquals(404, e.code())
            assertNotNull(favoritos.obtenerFavorito(42))
        }
    }

    @Test fun cancelarNoDevuelveUnaCopiaComoSiLaConsultaHubieraTerminado() = runTest {
        favoritos.guardar(RepositorioPrueba().evento)
        val apiCancelada = object : EventiaApi by api {
            override suspend fun obtenerEvento(id: Int): EventDto = throw CancellationException("Cancelado")
        }
        val repo = EventRepositoryImpl(apiCancelada, favoritos, conectividad, cache, Gson())
        try {
            repo.obtenerDetalleEvento(42)
            fail("Debe propagar la cancelación")
        } catch (e: CancellationException) {
            assertEquals("Cancelado", e.message)
        }
    }

    @Test fun falloDeRedSinFavoritoConservaElErrorOriginal() = runTest {
        val fallo = IOException("Servidor inaccesible")
        val apiFallida = object : EventiaApi by api {
            override suspend fun obtenerEvento(id: Int): EventDto = throw fallo
        }
        val repo = EventRepositoryImpl(apiFallida, favoritos, conectividad, cache, Gson())
        try {
            repo.obtenerDetalleEvento(42)
            fail("No existe copia para recuperar")
        } catch (e: IOException) {
            assertSame(fallo, e)
        }
    }

    @Test fun quitarFavoritoTambienRetiraSuDetalleLocal() = runTest {
        val guardado = RepositorioPrueba().evento
        favoritos.guardar(guardado)
        favoritos.quitar(guardado.id)
        conectividad.conectado.value = false
        try {
            repositorio.obtenerDetalleEvento(guardado.id)
            fail("La copia se quitó")
        } catch (_: IOException) {
            assertEquals(0, servidor.requestCount)
        }
    }
}
