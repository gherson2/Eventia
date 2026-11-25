package com.example.moviles_eventia.ui.screens

import androidx.lifecycle.ViewModelStore
import com.example.moviles_eventia.models.Usuario
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CuentaViewModelTest {
    @get:Rule val regla = MainDispatcherRule()
    private val store = ViewModelStore()
    private fun crear(repo: CuentaPrueba) = CuentaViewModel(repo).also { store.put("cuenta", it) }
    @After fun limpiar() { store.clear() }

    @Test fun sinCuentaNoHaceSolicitudDePerfilNiBloqueaLaApp() = runTest {
        val repo = CuentaPrueba()
        val vm = crear(repo)
        runCurrent()
        assertNull(vm.uiState.value.usuario)
        assertFalse(vm.uiState.value.leyendoSesion)
        assertEquals(0, repo.refrescos)
    }

    @Test fun abrirSinRedConservaElUsuarioPersistido() = runTest {
        val repo = CuentaPrueba()
        val usuario = Usuario(7, "Ana", "ana@example.test")
        repo.usuario.value = usuario
        repo.error = IOException("Sin red")
        val vm = crear(repo)
        runCurrent()
        assertEquals(usuario, vm.uiState.value.usuario)
        assertEquals(1, repo.refrescos)
        assertNotNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.operando)
    }

    @Test fun cerrarSesionActualizaElPerfil() = runTest {
        val repo = CuentaPrueba()
        repo.usuario.value = Usuario(7, "Ana", "ana@example.test")
        val vm = crear(repo)
        runCurrent()
        vm.cerrarSesion()
        runCurrent()
        assertNull(vm.uiState.value.usuario)
        assertFalse(vm.uiState.value.operando)
    }
}
