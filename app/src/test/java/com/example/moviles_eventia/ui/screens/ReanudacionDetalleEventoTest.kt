package com.example.moviles_eventia.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.example.moviles_eventia.models.Usuario
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReanudacionDetalleEventoTest {
    @get:Rule val regla = MainDispatcherRule()
    private val store = ViewModelStore()
    private val repo = RepositorioPrueba()
    private val cuenta = CuentaPrueba().apply {
        usuario.value = Usuario(7, "Ana", "ana@example.test")
    }
    private val inscripciones = InscripcionesPrueba()

    private fun crear(clave: String = "detalle") = DetalleEventoViewModel(
        repo, SavedStateHandle(mapOf("eventoId" to 42)), cuenta, inscripciones
    ).also { store.put(clave, it) }

    @After fun limpiar() { store.clear() }

    @Test fun primeraReanudacionNoDuplicaLaCargaInicialTerminada() = runTest {
        val vm = crear()
        runCurrent()
        vm.reanudar()
        runCurrent()
        assertEquals(1, repo.consultasDetalle)
    }

    @Test fun primeraReanudacionNoDuplicaLaCargaInicialPendiente() = runTest {
        val continuar = CompletableDeferred<Unit>()
        repo.antesDelDetalle = { continuar.await() }
        val vm = crear()
        vm.reanudar()
        runCurrent()
        assertEquals(1, repo.consultasDetalle)
        continuar.complete(Unit)
        runCurrent()
        assertEquals(1, repo.consultasDetalle)
        assertTrue(vm.uiState.value.datosRecientes)
    }

    @Test fun regresarRecuperaInscripcionYCuposCambiadosEnOtraPantalla() = runTest {
        val vm = crear()
        vm.reanudar()
        runCurrent()
        vm.pausar()
        repo.detalleRespuesta = repo.evento.copy(isAttending = true, availableSpots = 9)
        vm.reanudar()
        assertFalse(vm.uiState.value.puedeGestionarInscripcion)
        runCurrent()
        assertEquals(2, repo.consultasDetalle)
        assertEquals(9, vm.uiState.value.evento?.availableSpots)
        assertTrue(vm.uiState.value.evento!!.isAttending)
        assertTrue(vm.uiState.value.puedeGestionarInscripcion)
        assertTrue(inscripciones.llamadas.isEmpty())
    }

    @Test fun regresarTrasCancelarRecuperaElBotonParaInscribirse() = runTest {
        repo.detalleRespuesta = repo.evento.copy(isAttending = true, availableSpots = 9)
        val vm = crear()
        vm.reanudar()
        runCurrent()
        vm.pausar()
        repo.detalleRespuesta = repo.evento.copy(isAttending = false, availableSpots = 10)
        vm.reanudar()
        runCurrent()
        assertFalse(vm.uiState.value.evento!!.isAttending)
        assertEquals(10, vm.uiState.value.evento?.availableSpots)
    }

    @Test fun dosDetallesDelMismoEventoSeSincronizanAlVolverAlAnterior() = runTest {
        val anterior = crear("anterior")
        anterior.reanudar()
        runCurrent()
        anterior.pausar()

        val anidado = crear("anidado")
        anidado.reanudar()
        runCurrent()
        inscripciones.alCambiar = { _, _ ->
            repo.evento.copy(isAttending = true, availableSpots = 9).also {
                repo.detalleRespuesta = it
            }
        }
        anidado.cambiarInscripcion()
        runCurrent()
        assertTrue(anidado.uiState.value.evento!!.isAttending)
        assertFalse(anterior.uiState.value.evento!!.isAttending)

        anidado.pausar()
        anterior.reanudar()
        runCurrent()
        assertEquals(3, repo.consultasDetalle)
        assertTrue(anterior.uiState.value.evento!!.isAttending)
        assertEquals(9, anterior.uiState.value.evento?.availableSpots)
        assertEquals(listOf(42 to true), inscripciones.llamadas)
    }

    @Test fun falloAlRegresarConservaElEventoPeroBloqueaEscriturasConDatosAntiguos() = runTest {
        val vm = crear()
        vm.reanudar()
        runCurrent()
        vm.pausar()
        repo.error = IOException("Sin conexión")
        vm.reanudar()
        runCurrent()
        assertEquals(repo.evento, vm.uiState.value.evento)
        assertNotNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.cargando)
        assertFalse(vm.uiState.value.puedeGestionarInscripcion)
    }

    @Test fun unaConsultaAnteriorNoEvitaLaActualizacionAlRegresar() = runTest {
        val continuar = CompletableDeferred<Unit>()
        repo.antesDelDetalle = {
            continuar.await()
            // La primera respuesta representa una lectura anterior al regreso.
            repo.detalleRespuesta = repo.evento.copy(availableSpots = 8)
            repo.antesDelDetalle = { repo.detalleRespuesta = repo.evento.copy(availableSpots = 7) }
        }
        val vm = crear()
        vm.reanudar()
        runCurrent()
        vm.pausar()
        vm.reanudar()
        runCurrent()
        assertEquals(1, repo.consultasDetalle)
        continuar.complete(Unit)
        runCurrent()
        assertEquals(2, repo.consultasDetalle)
        assertEquals(7, vm.uiState.value.evento?.availableSpots)
        assertFalse(vm.uiState.value.cargando)
    }

    @Test fun regresarDuranteInscripcionNoLaCancelaNiLaRepite() = runTest {
        val continuar = CompletableDeferred<Unit>()
        inscripciones.alCambiar = { _, _ ->
            continuar.await()
            repo.evento.copy(isAttending = true, availableSpots = 9)
        }
        val vm = crear()
        vm.reanudar()
        runCurrent()
        vm.cambiarInscripcion()
        runCurrent()
        vm.pausar()
        repo.detalleRespuesta = repo.evento.copy(isAttending = true, availableSpots = 9)
        vm.reanudar()
        runCurrent()
        assertEquals(1, repo.consultasDetalle)
        assertTrue(vm.uiState.value.modificandoInscripcion)
        continuar.complete(Unit)
        runCurrent()
        assertEquals(listOf(42 to true), inscripciones.llamadas)
        assertEquals(2, repo.consultasDetalle)
        assertTrue(vm.uiState.value.evento!!.isAttending)
        assertFalse(vm.uiState.value.modificandoInscripcion)
    }

    @Test fun noEjecutaRefrescoPendienteSiLaPantallaVolvioASalir() = runTest {
        val continuar = CompletableDeferred<Unit>()
        repo.antesDelDetalle = { continuar.await() }
        val vm = crear()
        vm.reanudar()
        runCurrent()
        vm.pausar()
        vm.reanudar()
        vm.pausar()
        continuar.complete(Unit)
        runCurrent()
        assertEquals(1, repo.consultasDetalle)
        vm.reanudar()
        runCurrent()
        assertEquals(2, repo.consultasDetalle)
    }

    @Test fun respuestaDeInscripcionPerdidaSoloOriginaUnaLecturaAlRegresar() = runTest {
        val continuar = CompletableDeferred<Unit>()
        inscripciones.alCambiar = { _, _ ->
            continuar.await()
            throw IOException("Se perdió la respuesta después de procesarse en el servidor")
        }
        val vm = crear()
        vm.reanudar()
        runCurrent()
        vm.cambiarInscripcion()
        runCurrent()
        vm.pausar()
        vm.reanudar()
        repo.detalleRespuesta = repo.evento.copy(isAttending = true, availableSpots = 9)
        continuar.complete(Unit)
        runCurrent()
        assertEquals(listOf(42 to true), inscripciones.llamadas)
        assertEquals(2, repo.consultasDetalle)
        assertTrue(vm.uiState.value.evento!!.isAttending)
        assertFalse(vm.uiState.value.modificandoInscripcion)
    }
}
