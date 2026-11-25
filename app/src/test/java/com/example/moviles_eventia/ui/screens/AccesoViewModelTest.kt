package com.example.moviles_eventia.ui.screens

import androidx.lifecycle.SavedStateHandle
import com.example.moviles_eventia.repositories.ErrorCuenta
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccesoViewModelTest {
    @get:Rule val regla = MainDispatcherRule()

    @Test fun validaAntesDeConsultarLaApi() = runTest {
        val repo = CuentaPrueba()
        val vm = AccesoViewModel(repo, SavedStateHandle(mapOf("registro" to true)))
        vm.enviar()
        runCurrent()
        assertEquals(setOf("name", "email", "password"), vm.uiState.value.campos.keys)
        assertEquals(0, repo.registros)
    }

    @Test fun loginExitosoLimpiaLaClaveYEvitaDobleEnvio() = runTest {
        val repo = CuentaPrueba()
        val vm = AccesoViewModel(repo, SavedStateHandle())
        vm.cambiarCorreo("ana@example.test")
        vm.cambiarClave("clave1234")
        vm.enviar()
        vm.enviar()
        runCurrent()
        assertEquals(1, repo.logins)
        assertTrue(vm.uiState.value.autenticado)
        assertEquals("", vm.uiState.value.clave)
        assertFalse(vm.uiState.value.enviando)
    }

    @Test fun registroUsaElFlujoCorrecto() = runTest {
        val repo = CuentaPrueba()
        val vm = AccesoViewModel(repo, SavedStateHandle(mapOf("registro" to true)))
        vm.cambiarNombre("Ana")
        vm.cambiarCorreo("ana@example.test")
        vm.cambiarClave("clave1234")
        vm.enviar()
        runCurrent()
        assertEquals(1, repo.registros)
        assertEquals(0, repo.logins)
    }

    @Test fun muestraLosErroresDelServidorJuntoAlCampo() = runTest {
        val repo = CuentaPrueba()
        repo.error = ErrorCuenta("Revisa los datos", mapOf("email" to "Correo ya registrado"))
        val vm = AccesoViewModel(repo, SavedStateHandle())
        vm.cambiarCorreo("ana@example.test")
        vm.cambiarClave("clave1234")
        vm.enviar()
        runCurrent()
        assertEquals("Correo ya registrado", vm.uiState.value.campos["email"])
        assertEquals("Revisa los datos", vm.uiState.value.error)
        assertFalse(vm.uiState.value.autenticado)
    }

    @Test fun restauracionNoIncluyeLaContrasena() = runTest {
        val guardado = SavedStateHandle()
        val repo = CuentaPrueba()
        val vm = AccesoViewModel(repo, guardado)
        vm.cambiarCorreo("ana@example.test")
        vm.cambiarClave("secreto123")
        val restaurado = AccesoViewModel(repo, guardado)
        assertEquals("ana@example.test", restaurado.uiState.value.correo)
        assertEquals("", restaurado.uiState.value.clave)
        assertFalse(guardado.keys().any { it.contains("clave") || it.contains("password") })
    }

    @Test fun cambiarDeModoLimpiaClaveYErrores() = runTest {
        val vm = AccesoViewModel(CuentaPrueba(), SavedStateHandle())
        vm.cambiarClave("corta")
        vm.enviar()
        vm.cambiarModo()
        assertTrue(vm.uiState.value.registro)
        assertEquals("", vm.uiState.value.clave)
        assertTrue(vm.uiState.value.campos.isEmpty())
    }
}
