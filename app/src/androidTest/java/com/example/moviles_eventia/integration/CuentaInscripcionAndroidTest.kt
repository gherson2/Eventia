package com.example.moviles_eventia.integration

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import com.example.moviles_eventia.ui.screens.DetalleEventoViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException

// Retrofit, Bearer y DataStore reales en Android; servidor y cuentas ficticios.
class CuentaInscripcionAndroidTest {
    private lateinit var entorno: EntornoAndroidPrueba
    private val store = ViewModelStore()

    @Before fun preparar() { entorno = EntornoAndroidPrueba() }
    @After fun terminar() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { store.clear() }
        entorno.close()
    }

    @Test fun registroLoginPersistenciaYLogoutSinBorrarFavoritos() = runBlocking {
        val cuenta = entorno.cuenta()
        entorno.favoritos().guardar(entorno.evento)
        entorno.responderCuenta("token-registro")
        cuenta.registrar("Ana de prueba", "ana@example.test", "clave-prueba-123")
        assertEquals("/api/auth/register", entorno.servidor.takeRequest().path)
        assertEquals(7, cuenta.usuario.first()?.id)

        entorno.reabrirSesion()
        assertEquals("token-registro", entorno.sesiones.sesion.first()?.token)
        val reabierta = entorno.cuenta()
        entorno.responder(mapOf("id" to 7, "name" to "Ana actualizada", "email" to "ana@example.test"))
        reabierta.actualizarUsuario()
        assertEquals("Bearer token-registro", entorno.servidor.takeRequest().getHeader("Authorization"))
        assertEquals("Ana actualizada", reabierta.usuario.first()?.nombre)

        reabierta.cerrarSesion()
        assertNull(entorno.sesiones.sesion.first())
        assertNotNull(entorno.favoritos().obtenerFavorito(42))
        entorno.responderCuenta("token-login")
        reabierta.iniciarSesion("ana@example.test", "clave-prueba-123")
        assertEquals("/api/auth/login", entorno.servidor.takeRequest().path)
        assertEquals("token-login", entorno.sesiones.sesion.first()?.token)
    }

    @Test fun inscripcionPerfilYCancelacionRespetanCuposDelServidor() = runBlocking {
        entorno.responderCuenta("token-prueba")
        entorno.cuenta().iniciarSesion("ana@example.test", "clave-prueba-123")
        entorno.servidor.takeRequest()
        val inscrito = entorno.evento.copy(isAttending = true, availableSpots = 9)
        entorno.responder(inscrito)
        assertEquals(inscrito, entorno.inscripciones().cambiarInscripcion(42, true))
        val post = entorno.servidor.takeRequest()
        assertEquals("POST", post.method)
        assertEquals("Bearer token-prueba", post.getHeader("Authorization"))

        entorno.responder(listOf(inscrito, inscrito.copy(id = 43, startsAt = "2020-01-01T00:00:00Z")))
        val personales = entorno.inscripciones().obtenerMisInscripciones(7)
        assertEquals("/api/me/attendances", entorno.servidor.takeRequest().path)
        assertEquals(listOf(42, 43), personales.map { it.id })
        assertTrue(personales.all { it.isAttending })
        entorno.responder(entorno.evento.copy(availableSpots = 10))
        val cancelado = entorno.inscripciones().cambiarInscripcion(42, false)
        assertEquals("DELETE", entorno.servidor.takeRequest().method)
        assertFalse(cancelado.isAttending)
        assertEquals(10, cancelado.availableSpots)
        entorno.responder(emptyList<Any>())
        assertTrue(entorno.inscripciones().obtenerMisInscripciones(7).isEmpty())
        assertEquals("/api/me/attendances", entorno.servidor.takeRequest().path)
    }

    @Test fun respuesta401BorraSoloLaSesionDePruebaYEmiteExpiracion() = runBlocking {
        entorno.responderCuenta("token-caducado")
        val cuenta = entorno.cuenta()
        cuenta.iniciarSesion("ana@example.test", "clave-prueba-123")
        entorno.servidor.takeRequest()
        entorno.responder(mapOf("message" to "Token expirado"), 401)
        try {
            entorno.inscripciones().obtenerMisInscripciones(7)
            fail("Se esperaba 401")
        } catch (error: HttpException) {
            assertEquals(401, error.code())
        }
        entorno.servidor.takeRequest()
        assertNull(entorno.sesiones.sesion.first())
        withTimeout(5_000) { cuenta.expirada.first() }
    }

    @Test fun conflicto409RefrescaDetalleSinRepetirLaInscripcion() = runBlocking {
        entorno.responderCuenta("token-prueba")
        val cuenta = entorno.cuenta()
        cuenta.iniciarSesion("ana@example.test", "clave-prueba-123")
        entorno.servidor.takeRequest()
        entorno.responder(entorno.evento)
        val vm = withContext(Dispatchers.Main) {
            DetalleEventoViewModel(entorno.eventos(), SavedStateHandle(mapOf("eventoId" to 42)),
                cuenta, entorno.inscripciones()).also { store.put("detalle", it) }
        }
        withTimeout(5_000) { vm.uiState.first { it.datosRecientes && !it.cargando } }
        entorno.servidor.takeRequest()
        entorno.responder(mapOf("code" to "NO_SPOTS", "message" to "Sin cupos"), 409)
        entorno.responder(entorno.evento.copy(availableSpots = 0))
        withContext(Dispatchers.Main) { vm.cambiarInscripcion(); vm.cambiarInscripcion() }
        val estado = withTimeout(5_000) {
            vm.uiState.first { it.mensajeInscripcion != null && !it.modificandoInscripcion }
        }
        assertEquals("POST", entorno.servidor.takeRequest().method)
        assertEquals("GET", entorno.servidor.takeRequest().method)
        assertEquals(0, estado.evento?.availableSpots)
        assertFalse(estado.puedeGestionarInscripcion)
        assertEquals(4, entorno.servidor.requestCount)
    }
}
