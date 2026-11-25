package com.example.moviles_eventia.repositories

import com.example.moviles_eventia.api.AutorizacionInterceptor
import com.example.moviles_eventia.api.EventiaApi
import com.example.moviles_eventia.api.SesionEsperada
import com.example.moviles_eventia.models.Sesion
import com.example.moviles_eventia.models.Usuario
import com.example.moviles_eventia.session.SesionPrueba
import com.example.moviles_eventia.session.SesionStore
import com.example.moviles_eventia.ui.screens.RepositorioPrueba
import com.google.gson.Gson
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class InscripcionRepositoryTest {
    private lateinit var servidor: MockWebServer
    private lateinit var almacen: SesionPrueba
    private lateinit var conectividad: ConectividadPrueba
    private lateinit var api: EventiaApi
    private lateinit var repo: InscripcionRepository
    private val usuario = Usuario(7, "Ana", "ana@example.test")
    private val evento = RepositorioPrueba().evento

    private fun crearApi(sesiones: SesionStore): EventiaApi {
        val cliente = OkHttpClient.Builder().addInterceptor(AutorizacionInterceptor(sesiones))
            .callTimeout(3, TimeUnit.SECONDS).build()
        return Retrofit.Builder().baseUrl(servidor.url("/api/")).client(cliente)
            .addConverterFactory(GsonConverterFactory.create()).build().create(EventiaApi::class.java)
    }

    @Before fun preparar() {
        servidor = MockWebServer()
        servidor.start()
        almacen = SesionPrueba()
        almacen.sesion.value = Sesion("token-prueba", usuario)
        conectividad = ConectividadPrueba()
        api = crearApi(almacen)
        repo = InscripcionRepositoryImpl(api, almacen, conectividad)
    }
    @After fun terminar() { servidor.shutdown() }

    @Test fun postAutenticadoDevuelveCuposYEstadoActualizados() = runTest {
        servidor.enqueue(MockResponse().setBody(Gson().toJson(evento.copy(availableSpots = 7, isAttending = true))))
        val resultado = repo.cambiarInscripcion(42, true)
        val solicitud = servidor.takeRequest()
        assertEquals("POST", solicitud.method)
        assertEquals("/api/events/42/attendance", solicitud.path)
        assertEquals("Bearer token-prueba", solicitud.getHeader("Authorization"))
        assertNull(solicitud.getHeader("X-Eventia-Expected-Token"))
        assertEquals(7, resultado.availableSpots)
        assertTrue(resultado.isAttending)
    }

    @Test fun deleteCancelaYRespetaLosCuposDevueltos() = runTest {
        servidor.enqueue(MockResponse().setBody(Gson().toJson(evento.copy(availableSpots = 11))))
        val resultado = repo.cambiarInscripcion(42, false)
        assertEquals("DELETE", servidor.takeRequest().method)
        assertFalse(resultado.isAttending)
        assertEquals(11, resultado.availableSpots)
    }

    @Test fun respuestaExitosaSinIsAttendingConfirmaLaOperacion() = runTest {
        val json = Gson().toJson(evento).replace(",\"isAttending\":false", "")
        servidor.enqueue(MockResponse().setBody(json))
        assertTrue(repo.cambiarInscripcion(42, true).isAttending)
    }

    @Test fun sinSesionNoEnviaSolicitudes() = runTest {
        almacen.cerrar()
        try { repo.cambiarInscripcion(42, true); fail("Requiere sesión") }
        catch (_: SesionRequeridaException) { }
        assertEquals(0, servidor.requestCount)
    }

    @Test fun sinConexionNoEncolaLaInscripcion() = runTest {
        conectividad.conectado.value = false
        try { repo.cambiarInscripcion(42, true); fail("Requiere Internet") }
        catch (_: IOException) { }
        assertEquals(0, servidor.requestCount)
    }

    @Test fun conflicto409SePropagaParaRefrescarElDetalle() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(409))
        try { repo.cambiarInscripcion(42, true); fail("Debe informar conflicto") }
        catch (e: HttpException) { assertEquals(409, e.code()) }
        assertEquals("token-prueba", almacen.sesion.value?.token)
    }

    @Test fun un401BorraLaSesionConElInterceptorExistente() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(401))
        try { repo.cambiarInscripcion(42, true); fail("Debe informar expiración") }
        catch (e: HttpException) { assertEquals(401, e.code()) }
        assertNull(almacen.sesion.value)
        assertEquals(1, almacen.expiraciones)
    }

    @Test fun cambiarDeCuentaAntesDelEnvioNoInscribiraLaCuentaNueva() = runTest {
        var lecturas = 0
        val cambiante = object : SesionStore by almacen {
            override val sesion = flow {
                lecturas++
                if (lecturas == 2) almacen.guardar(Sesion("token-nuevo", usuario.copy(id = 8)))
                emit(almacen.sesion.value)
            }
        }
        val cuenta = InscripcionRepositoryImpl(crearApi(cambiante), cambiante, conectividad)
        try { cuenta.cambiarInscripcion(42, true); fail("La sesión cambió") }
        catch (_: IOException) { }
        assertEquals(0, servidor.requestCount)
        assertEquals("token-nuevo", almacen.sesion.value?.token)
    }

    @Test fun cerrarSesionDuranteLaSolicitudImpideAplicarLaRespuesta() = runTest {
        servidor.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                runBlocking { almacen.cerrar() }
                return MockResponse().setBody(Gson().toJson(evento.copy(isAttending = true)))
            }
        }
        try { repo.cambiarInscripcion(42, true); fail("La sesión cambió") }
        catch (_: CancellationException) { }
        assertNull(almacen.sesion.value)
    }

    @Test fun tagDeSesionNoExponeElTokenEnSuRepresentacion() {
        assertFalse(SesionEsperada("privado").toString().contains("privado"))
    }

    @Test fun listaPersonalUsaBearerYConservaEventosPasados() = runTest {
        val pasado = evento.copy(id = 43, startsAt = "2020-01-01T12:00:00Z")
        servidor.enqueue(MockResponse().setBody(Gson().toJson(listOf(evento, pasado, evento))))
        val lista = repo.obtenerMisInscripciones(7)
        val solicitud = servidor.takeRequest()
        assertEquals("GET", solicitud.method)
        assertEquals("/api/me/attendances", solicitud.path)
        assertEquals("Bearer token-prueba", solicitud.getHeader("Authorization"))
        assertEquals(listOf(42, 43), lista.map { it.id })
        assertTrue(lista.all { it.isAttending })
    }

    @Test fun listaPersonalSinSesionNoConsultaLaApi() = runTest {
        almacen.cerrar()
        try { repo.obtenerMisInscripciones(7); fail("Se necesita sesión") }
        catch (_: SesionRequeridaException) { }
        assertEquals(0, servidor.requestCount)
    }

    @Test fun listaPersonalSinRedNoSePresentaComoVacia() = runTest {
        conectividad.conectado.value = false
        try { repo.obtenerMisInscripciones(7); fail("Se necesita conexión") }
        catch (_: IOException) { }
        assertEquals(0, servidor.requestCount)
    }

    @Test fun un401EnLaListaPersonalExpiraLaSesion() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(401))
        try { repo.obtenerMisInscripciones(7); fail("Se esperaba 401") }
        catch (e: HttpException) { assertEquals(401, e.code()) }
        assertNull(almacen.sesion.value)
        assertEquals(1, almacen.expiraciones)
    }

    @Test fun listaTardiaSeDescartaAlCambiarDeCuenta() = runTest {
        servidor.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                runBlocking { almacen.guardar(Sesion("nuevo", usuario.copy(id = 8))) }
                return MockResponse().setBody(Gson().toJson(listOf(evento)))
            }
        }
        try { repo.obtenerMisInscripciones(7); fail("No debe devolver datos de otra cuenta") }
        catch (_: CancellationException) { }
        assertEquals("nuevo", almacen.sesion.value?.token)
    }

    @Test fun cambiarCuentaAntesDeConsultarNoEnviaUnaPeticionConOtraSesion() = runTest {
        var lecturas = 0
        val cambiante = object : SesionStore by almacen {
            override val sesion = flow {
                lecturas++
                if (lecturas == 2) almacen.guardar(Sesion("nuevo", usuario.copy(id = 8)))
                emit(almacen.sesion.value)
            }
        }
        val cuenta = InscripcionRepositoryImpl(crearApi(cambiante), cambiante, conectividad)
        try { cuenta.obtenerMisInscripciones(7); fail("La sesión cambió") }
        catch (_: IOException) { }
        assertEquals(0, servidor.requestCount)
    }

    @Test fun noConsultaDatosSiElPerfilEsperabaOtraCuenta() = runTest {
        try { repo.obtenerMisInscripciones(8); fail("El perfil no corresponde a la sesión") }
        catch (_: CancellationException) { }
        assertEquals(0, servidor.requestCount)
    }
}
