package com.example.moviles_eventia.ui.screens

import androidx.lifecycle.ViewModelStore
import com.example.moviles_eventia.repositories.ConectividadPrueba
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConectividadViewModelTest {
    @get:Rule val regla = MainDispatcherRule()
    private val store = ViewModelStore()

    private fun crear(repo: ConectividadPrueba) = ConectividadViewModel(repo).also {
        store.put("conectividad", it)
    }

    @After fun limpiar() { store.clear() }

    @Test fun comienzaSinInventarEstadoYReflejaPerdidaYRecuperacion() = runTest {
        val repo = ConectividadPrueba()
        val vm = crear(repo)
        assertNull(vm.conectado.value)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.conectado.collect {} }
        runCurrent()
        assertEquals(true, vm.conectado.value)
        repo.conectado.value = false
        runCurrent()
        assertEquals(false, vm.conectado.value)
        repo.conectado.value = true
        runCurrent()
        assertEquals(true, vm.conectado.value)
    }

    @Test fun dejaDeObservarCuandoYaNoHayPantallasActivas() = runTest {
        val repo = ConectividadPrueba()
        val vm = crear(repo)
        val observacion = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.conectado.collect {}
        }
        runCurrent()
        assertEquals(1, repo.conectado.subscriptionCount.value)
        observacion.cancel()
        advanceTimeBy(5_001)
        runCurrent()
        assertEquals(0, repo.conectado.subscriptionCount.value)
    }
}
