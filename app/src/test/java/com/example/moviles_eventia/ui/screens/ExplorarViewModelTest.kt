package com.example.moviles_eventia.ui.screens

import androidx.lifecycle.SavedStateHandle
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExplorarViewModelTest {
    @get:Rule val regla = MainDispatcherRule()

    @Test fun espera300MilisegundosYConsultaSoloLaUltimaBusqueda() = runTest {
        val repo = RepositorioPrueba()
        val vm = ExplorarViewModel(repo, SavedStateHandle())
        runCurrent()
        repo.consultas.clear()
        vm.cambiarBusqueda("Ja")
        runCurrent()
        advanceTimeBy(200)
        vm.cambiarBusqueda("Jazz")
        runCurrent()
        advanceTimeBy(299)
        assertTrue(repo.consultas.isEmpty())
        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf("Jazz" to null), repo.consultas)
    }

    @Test fun actualizarConErrorNoBorraLaLista() = runTest {
        val repo = RepositorioPrueba()
        val vm = ExplorarViewModel(repo, SavedStateHandle())
        advanceUntilIdle()
        val anteriores = vm.uiState.value.eventos
        repo.error = IOException("Sin red")
        vm.obtenerEventos()
        assertEquals(anteriores, vm.uiState.value.eventos)
        assertTrue(vm.uiState.value.cargando)
        advanceUntilIdle()
        assertEquals(anteriores, vm.uiState.value.eventos)
        assertFalse(vm.uiState.value.cargando)
        assertNotNull(vm.uiState.value.error)
    }

    @Test fun combinaFiltrosYLosLimpia() = runTest {
        val repo = RepositorioPrueba()
        val vm = ExplorarViewModel(repo, SavedStateHandle())
        advanceUntilIdle()
        vm.cambiarBusqueda("Jazz")
        vm.cambiarCategoria(3)
        advanceUntilIdle()
        assertEquals("Jazz" to 3, repo.consultas.last())
        vm.limpiarFiltros()
        advanceUntilIdle()
        assertEquals("" to null, repo.consultas.last())
        assertEquals(1, vm.uiState.value.eventos.size)
    }

    @Test fun restauraFiltrosDesdeSavedStateHandle() = runTest {
        val repo = RepositorioPrueba()
        val guardado = SavedStateHandle(mapOf("busqueda" to "Jazz", "categoriaId" to 3))
        val vm = ExplorarViewModel(repo, guardado)
        advanceUntilIdle()
        assertEquals("Jazz", vm.uiState.value.busqueda)
        assertEquals(3, vm.uiState.value.categoriaId)
        assertEquals("Jazz" to 3, repo.consultas.single())
        vm.limpiarFiltros()
        advanceUntilIdle()
        assertEquals("", guardado.get<String>("busqueda"))
        assertEquals(-1, guardado.get<Int>("categoriaId"))
    }

    @Test fun filtrosSinCoincidenciasProducenVacioSinError() = runTest {
        val vm = ExplorarViewModel(RepositorioPrueba(), SavedStateHandle())
        advanceUntilIdle()
        vm.cambiarBusqueda("Inexistente")
        advanceUntilIdle()
        assertTrue(vm.uiState.value.eventos.isEmpty())
        assertNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.cargando)
    }
}
