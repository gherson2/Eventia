package com.example.moviles_eventia.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.example.moviles_eventia.models.Usuario
import com.example.moviles_eventia.repositories.SesionRequeridaException
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class InscripcionViewModelTest {
    @get:Rule val regla = MainDispatcherRule()
    private val store = ViewModelStore()
    private val repo = RepositorioPrueba()
    private val cuenta = CuentaPrueba()
    private val inscripciones = InscripcionesPrueba()

    private fun crear(conSesion: Boolean = true): DetalleEventoViewModel {
        if (conSesion) cuenta.usuario.value = Usuario(7, "Ana", "ana@example.test")
        return DetalleEventoViewModel(repo, SavedStateHandle(mapOf("eventoId" to 42)), cuenta, inscripciones)
            .also { store.put("detalle", it) }
    }
    @After fun limpiar() { store.clear() }

    @Test fun sinSesionSolicitaAccesoSinInscribirse() = runTest {
        val vm = crear(conSesion = false)
        runCurrent()
        vm.cambiarInscripcion()
        assertEquals(Unit, vm.solicitarAcceso.first())
        assertTrue(inscripciones.llamadas.isEmpty())
    }

    @Test fun alRegresarDelLoginRefrescaElDetalleParaLaCuenta() = runTest {
        val vm = crear(conSesion = false)
        runCurrent()
        repo.detalleRespuesta = repo.evento.copy(isAttending = true)
        cuenta.usuario.value = Usuario(7, "Ana", "ana@example.test")
        runCurrent()
        assertEquals(2, repo.consultasDetalle)
        assertTrue(vm.uiState.value.haySesion)
        assertTrue(vm.uiState.value.evento!!.isAttending)
    }

    @Test fun exitoUsaLosCuposDelServidorYCambiaElEstadoDelBoton() = runTest {
        inscripciones.respuesta = repo.evento.copy(availableSpots = 7, isAttending = true)
        val vm = crear()
        runCurrent()
        vm.cambiarInscripcion()
        runCurrent()
        assertEquals(listOf(42 to true), inscripciones.llamadas)
        assertEquals(7, vm.uiState.value.evento?.availableSpots)
        assertTrue(vm.uiState.value.evento!!.isAttending)
        assertEquals("Inscripción confirmada.", vm.uiState.value.mensajeInscripcion)
        assertFalse(vm.uiState.value.modificandoInscripcion)
    }

    @Test fun puedeCancelarAunqueYaNoQuedenCupos() = runTest {
        repo.detalleRespuesta = repo.evento.copy(availableSpots = 0, isAttending = true)
        inscripciones.respuesta = repo.evento.copy(availableSpots = 1, isAttending = false)
        val vm = crear()
        runCurrent()
        assertTrue(vm.uiState.value.puedeGestionarInscripcion)
        vm.cambiarInscripcion()
        runCurrent()
        assertEquals(listOf(42 to false), inscripciones.llamadas)
        assertFalse(vm.uiState.value.evento!!.isAttending)
        assertEquals("Inscripción cancelada.", vm.uiState.value.mensajeInscripcion)
    }

    @Test fun copiaOfflineYEventoSinCuposNoPermitenInscribirse() = runTest {
        repo.esCopiaGuardada = true
        val vm = crear()
        runCurrent()
        vm.cambiarInscripcion()
        assertTrue(inscripciones.llamadas.isEmpty())
        repo.esCopiaGuardada = false
        repo.detalleRespuesta = repo.evento.copy(availableSpots = 0)
        vm.cargar()
        runCurrent()
        assertFalse(vm.uiState.value.puedeGestionarInscripcion)
        vm.cambiarInscripcion()
        assertTrue(inscripciones.llamadas.isEmpty())
    }

    @Test fun doblePulsacionSoloEnviaUnaOperacion() = runTest {
        val continuar = CompletableDeferred<Unit>()
        inscripciones.alCambiar = { _, _ -> continuar.await(); inscripciones.respuesta }
        val vm = crear()
        runCurrent()
        vm.cambiarInscripcion()
        vm.cambiarInscripcion()
        runCurrent()
        assertEquals(1, inscripciones.llamadas.size)
        continuar.complete(Unit)
        runCurrent()
    }

    @Test fun conflicto409RefrescaCuposYMuestraUnMensaje() = runTest {
        val vm = crear()
        runCurrent()
        inscripciones.error = HttpException(Response.error<Any>(409, "{}".toResponseBody()))
        repo.detalleRespuesta = repo.evento.copy(availableSpots = 0)
        vm.cambiarInscripcion()
        runCurrent()
        assertEquals(2, repo.consultasDetalle)
        assertEquals(0, vm.uiState.value.evento?.availableSpots)
        assertNotNull(vm.uiState.value.mensajeInscripcion)
        assertFalse(vm.uiState.value.modificandoInscripcion)
    }

    @Test fun falloDeRedNoReintentaPostAutomaticamente() = runTest {
        val vm = crear()
        runCurrent()
        inscripciones.error = IOException("La respuesta se perdió")
        vm.cambiarInscripcion()
        runCurrent()
        assertEquals(repo.evento, vm.uiState.value.evento)
        assertNotNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.datosRecientes)
        vm.cambiarInscripcion()
        runCurrent()
        assertEquals(1, inscripciones.llamadas.size)
        vm.cargar()
        runCurrent()
        assertTrue(vm.uiState.value.datosRecientes)
        assertEquals(1, inscripciones.llamadas.size)
    }

    @Test fun sesionDesaparecidaSolicitaAccesoEnVezDeCerrarLaApp() = runTest {
        val vm = crear()
        runCurrent()
        inscripciones.error = SesionRequeridaException()
        vm.cambiarInscripcion()
        runCurrent()
        assertEquals(Unit, vm.solicitarAcceso.first())
        assertFalse(vm.uiState.value.modificandoInscripcion)
    }

    @Test fun respuestaTardiaNoContaminaOtraCuenta() = runTest {
        val continuar = CompletableDeferred<Unit>()
        inscripciones.alCambiar = { _, _ ->
            withContext(NonCancellable) { continuar.await() }
            repo.evento.copy(isAttending = true, availableSpots = 7)
        }
        val vm = crear()
        runCurrent()
        vm.cambiarInscripcion()
        runCurrent()
        cuenta.usuario.value = Usuario(8, "Luis", "luis@example.test")
        runCurrent()
        continuar.complete(Unit)
        runCurrent()
        assertFalse(vm.uiState.value.evento!!.isAttending)
        assertEquals(10, vm.uiState.value.evento?.availableSpots)
        assertNull(vm.uiState.value.mensajeInscripcion)
    }

    @Test fun eventoPasadoNoAdmiteOperaciones() = runTest {
        repo.detalleRespuesta = repo.evento.copy(startsAt = "2020-01-01T12:00:00Z")
        val vm = crear()
        runCurrent()
        vm.cambiarInscripcion()
        runCurrent()
        assertTrue(inscripciones.llamadas.isEmpty())
    }

    @Test fun conflictoConRefrescoFallidoConservaDatosPeroBloqueaNuevasEscrituras() = runTest {
        val vm = crear()
        runCurrent()
        inscripciones.error = HttpException(Response.error<Any>(409, "{}".toResponseBody()))
        repo.error = IOException("Sin respuesta")
        vm.cambiarInscripcion()
        runCurrent()
        assertEquals(repo.evento, vm.uiState.value.evento)
        assertNotNull(vm.uiState.value.error)
        assertNotNull(vm.uiState.value.mensajeInscripcion)
        assertFalse(vm.uiState.value.puedeGestionarInscripcion)
        vm.cambiarInscripcion()
        runCurrent()
        assertEquals(1, inscripciones.llamadas.size)
    }

    @Test fun cerrarSesionRetiraElEstadoPersonalDelDetalle() = runTest {
        repo.detalleRespuesta = repo.evento.copy(isAttending = true)
        val vm = crear()
        runCurrent()
        assertTrue(vm.uiState.value.evento!!.isAttending)
        cuenta.cerrarSesion()
        runCurrent()
        assertFalse(vm.uiState.value.haySesion)
        assertFalse(vm.uiState.value.evento!!.isAttending)
        assertEquals(2, repo.consultasDetalle)
    }
}
