package com.example.moviles_eventia.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.After
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class DetallesViewModelTest {
    @get:Rule val regla = MainDispatcherRule()
    private val store = ViewModelStore()
    @After fun limpiar() { store.clear() }
    private fun crearDetalle(repo: RepositorioPrueba) = DetalleEventoViewModel(
        repo, SavedStateHandle(mapOf("eventoId" to 42)), CuentaPrueba(), InscripcionesPrueba()
    ).also { store.put("detalle", it) }

    @Test fun detalle404RetiraElEventoYExplicaQueNoEstaDisponible() = runTest {
        val repo = RepositorioPrueba()
        val vm = crearDetalle(repo)
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.evento)
        repo.error = HttpException(Response.error<Any>(404, "{}".toResponseBody()))
        vm.cargar()
        advanceUntilIdle()
        assertNull(vm.uiState.value.evento)
        assertEquals("Este evento ya no está disponible.", vm.uiState.value.error)
    }

    @Test fun detalleConFalloDeRedConservaElEvento() = runTest {
        val repo = RepositorioPrueba()
        val vm = crearDetalle(repo)
        advanceUntilIdle()
        repo.error = IOException("Sin red")
        vm.cargar()
        advanceUntilIdle()
        assertEquals(42, vm.uiState.value.evento?.id)
        assertNotNull(vm.uiState.value.error)
    }

    @Test fun falloDeEventosNoOcultaElDetalleDelLugar() = runTest {
        val repo = RepositorioPrueba()
        repo.errorEventosLugar = IOException("Sin red")
        val vm = DetalleLugarViewModel(repo, SavedStateHandle(mapOf("lugarId" to 7)))
        advanceUntilIdle()
        assertEquals(7, vm.uiState.value.lugar?.id)
        assertNotNull(vm.uiState.value.errorEventos)
        assertFalse(vm.uiState.value.cargando)
        repo.errorEventosLugar = null
        vm.cargar()
        advanceUntilIdle()
        assertEquals(42, vm.uiState.value.eventos.single().id)
        assertNull(vm.uiState.value.errorEventos)
    }

    @Test fun lugaresConErrorDeActualizacionConservanDatos() = runTest {
        val repo = RepositorioPrueba()
        val vm = LugaresViewModel(repo)
        advanceUntilIdle()
        repo.error = IOException("Sin red")
        vm.cargar()
        advanceUntilIdle()
        assertEquals(7, vm.uiState.value.lugares.single().id)
        assertNotNull(vm.uiState.value.error)
    }

    @Test fun detalleGuardadoInformaSuOrigenSinMostrarUnErrorBloqueante() = runTest {
        val repo = RepositorioPrueba()
        repo.esCopiaGuardada = true
        val vm = crearDetalle(repo)
        advanceUntilIdle()
        assertEquals(42, vm.uiState.value.evento?.id)
        assertTrue(vm.uiState.value.esCopiaGuardada)
        assertNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.cargando)
    }

    @Test fun actualizarConDatosRemotosRetiraLaMarcaDeCopiaGuardada() = runTest {
        val repo = RepositorioPrueba()
        repo.esCopiaGuardada = true
        val vm = crearDetalle(repo)
        advanceUntilIdle()
        repo.esCopiaGuardada = false
        vm.cargar()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.esCopiaGuardada)
        assertNull(vm.uiState.value.error)
    }
}
