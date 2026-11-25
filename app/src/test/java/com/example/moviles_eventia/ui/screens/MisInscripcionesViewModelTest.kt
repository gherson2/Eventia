package com.example.moviles_eventia.ui.screens

import androidx.lifecycle.ViewModelStore
import com.example.moviles_eventia.models.Usuario
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.*
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MisInscripcionesViewModelTest {
    @get:Rule val regla = MainDispatcherRule()
    private val store = ViewModelStore()
    private val cuenta = CuentaPrueba()
    private val repo = InscripcionesPrueba()
    private val evento = RepositorioPrueba().evento
    private fun crear(sesion: Boolean = true): MisInscripcionesViewModel {
        if (sesion) cuenta.usuario.value = Usuario(7, "Ana", "ana@example.test")
        return MisInscripcionesViewModel(cuenta, repo).also { store.put("inscripciones", it) }
    }
    @After fun limpiar() { store.clear() }

    @Test fun sinCuentaNoConsultaLaApi() = runTest {
        val vm = crear(sesion = false)
        vm.reanudar()
        runCurrent()
        vm.cargar()
        runCurrent()
        assertEquals(0, repo.consultas)
        assertNull(vm.uiState.value.usuarioId)
    }

    @Test fun consultaSoloMientrasPerfilEstaActivo() = runTest {
        repo.lista = listOf(evento)
        val vm = crear()
        runCurrent()
        assertEquals(0, repo.consultas)
        vm.reanudar()
        runCurrent()
        assertEquals(1, repo.consultas)
        assertEquals(7, vm.uiState.value.usuarioId)
        assertEquals(listOf(evento), vm.uiState.value.eventos)
        assertTrue(vm.uiState.value.cargado)
    }

    @Test fun volverDelDetalleRecargaYRetiraUnaInscripcionCancelada() = runTest {
        repo.lista = listOf(evento)
        val vm = crear()
        vm.reanudar()
        runCurrent()
        vm.pausar()
        repo.lista = emptyList()
        vm.reanudar()
        runCurrent()
        assertEquals(2, repo.consultas)
        assertTrue(vm.uiState.value.eventos.isEmpty())
        assertTrue(vm.uiState.value.cargado)
    }

    @Test fun falloDeActualizacionMantieneSoloLosDatosDeLaMismaCuenta() = runTest {
        repo.lista = listOf(evento)
        val vm = crear()
        vm.reanudar()
        runCurrent()
        repo.errorConsulta = IOException("Sin conexión")
        vm.cargar()
        runCurrent()
        assertEquals(listOf(evento), vm.uiState.value.eventos)
        assertNotNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.cargando)
    }

    @Test fun primerFalloNoSePresentaComoListaVaciaExitosa() = runTest {
        repo.errorConsulta = IOException("Sin conexión")
        val vm = crear()
        vm.reanudar()
        runCurrent()
        assertFalse(vm.uiState.value.cargado)
        assertNotNull(vm.uiState.value.error)
        assertTrue(vm.uiState.value.eventos.isEmpty())
    }

    @Test fun logoutLimpiaLosEventosYNoIniciaOtraConsulta() = runTest {
        repo.lista = listOf(evento)
        val vm = crear()
        vm.reanudar()
        runCurrent()
        cuenta.cerrarSesion()
        runCurrent()
        assertNull(vm.uiState.value.usuarioId)
        assertTrue(vm.uiState.value.eventos.isEmpty())
        assertFalse(vm.uiState.value.cargado)
        assertEquals(1, repo.consultas)
    }

    @Test fun pausaNoRecargaAlCambiarDeCuentaHastaVolver() = runTest {
        repo.lista = listOf(evento)
        val vm = crear()
        vm.reanudar()
        runCurrent()
        vm.pausar()
        cuenta.usuario.value = Usuario(8, "Luis", "luis@example.test")
        runCurrent()
        assertEquals(8, vm.uiState.value.usuarioId)
        assertTrue(vm.uiState.value.eventos.isEmpty())
        assertEquals(1, repo.consultas)
        repo.lista = listOf(evento.copy(id = 43))
        vm.reanudar()
        runCurrent()
        assertEquals(listOf(43), vm.uiState.value.eventos.map { it.id })
        assertEquals(listOf(7, 8), repo.usuariosConsultados)
    }

    @Test fun respuestaTardiaDeOtraCuentaNoContaminaLaLista() = runTest {
        val continuar = CompletableDeferred<Unit>()
        repo.alConsultar = {
            withContext(NonCancellable) { continuar.await() }
            listOf(evento)
        }
        val vm = crear()
        vm.reanudar()
        runCurrent()
        repo.alConsultar = null
        repo.lista = listOf(evento.copy(id = 43))
        cuenta.usuario.value = Usuario(8, "Luis", "luis@example.test")
        runCurrent()
        continuar.complete(Unit)
        runCurrent()
        assertEquals(8, vm.uiState.value.usuarioId)
        assertEquals(listOf(43), vm.uiState.value.eventos.map { it.id })
    }
}
