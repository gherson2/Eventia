package com.example.moviles_eventia.ui.screens

import androidx.lifecycle.ViewModelStore
import com.example.moviles_eventia.models.Event
import com.example.moviles_eventia.repositories.FavoritosRepository
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritosViewModelTest {
    @get:Rule val regla = MainDispatcherRule()
    private val store = ViewModelStore()
    private val evento = RepositorioPrueba().evento

    private class RepositorioFavoritosPrueba : FavoritosRepository {
        val datos = MutableStateFlow<List<Event>>(emptyList())
        var errorLectura: Exception? = null
        var errorEscritura: Exception? = null
        var observadores = 0
        override fun observarFavoritos(): Flow<List<Event>> = flow {
            errorLectura?.let { throw it }
            emitAll(datos)
        }.onStart { observadores++ }.onCompletion { observadores-- }
        override suspend fun obtenerFavorito(id: Int) = datos.value.find { it.id == id }
        override suspend fun guardar(evento: Event) {
            errorEscritura?.let { throw it }
            datos.update { it.filterNot { anterior -> anterior.id == evento.id } + evento }
        }
        override suspend fun quitar(id: Int) {
            errorEscritura?.let { throw it }
            datos.update { it.filterNot { anterior -> anterior.id == id } }
        }
    }

    private fun crear(repo: FavoritosRepository) = FavoritosViewModel(repo).also {
        store.put("favoritos", it)
    }

    @After fun limpiar() { store.clear() }

    @Test fun observaLosFavoritosYGuardaSinCuenta() = runTest {
        val vm = crear(RepositorioFavoritosPrueba())
        runCurrent()
        assertFalse(vm.uiState.value.cargando)
        vm.cambiarFavorito(evento, guardar = true)
        runCurrent()
        assertEquals(evento, vm.uiState.value.eventos.single())
        assertFalse(vm.uiState.value.guardando)
    }

    @Test fun quitarOfreceDeshacerYRestaurarRecuperaElEvento() = runTest {
        val repo = RepositorioFavoritosPrueba()
        repo.datos.value = listOf(evento)
        val vm = crear(repo)
        runCurrent()
        vm.cambiarFavorito(evento, guardar = false)
        runCurrent()
        assertTrue(vm.uiState.value.eventos.isEmpty())
        val mensaje = vm.mensajes.first()
        assertEquals("Quitado de favoritos", mensaje.texto)
        assertEquals(evento, mensaje.quitado)
        vm.restaurar(requireNotNull(mensaje.quitado))
        runCurrent()
        assertEquals(evento, vm.uiState.value.eventos.single())
    }

    @Test fun falloDeEscrituraNoFingeQueSeQuitoNiOfreceDeshacer() = runTest {
        val repo = RepositorioFavoritosPrueba()
        repo.datos.value = listOf(evento)
        repo.errorEscritura = IOException("Disco lleno")
        val vm = crear(repo)
        runCurrent()
        vm.cambiarFavorito(evento, guardar = false)
        runCurrent()
        assertEquals(listOf(evento), vm.uiState.value.eventos)
        val mensaje = vm.mensajes.first()
        assertNull(mensaje.quitado)
        assertTrue(mensaje.texto.contains("No pudimos"))
        assertFalse(vm.uiState.value.guardando)
    }

    @Test fun errorDeLecturaPermiteReintentar() = runTest {
        val repo = RepositorioFavoritosPrueba()
        repo.errorLectura = IOException("Error local")
        val vm = crear(repo)
        runCurrent()
        assertNotNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.cargando)
        repo.errorLectura = null
        repo.datos.value = listOf(evento)
        vm.cargar()
        runCurrent()
        assertNull(vm.uiState.value.error)
        assertEquals(listOf(evento), vm.uiState.value.eventos)
    }

    @Test fun reintentarNoDuplicaElObservador() = runTest {
        val repo = RepositorioFavoritosPrueba()
        val vm = crear(repo)
        runCurrent()
        vm.cargar()
        runCurrent()
        vm.cargar()
        runCurrent()
        assertEquals(1, repo.observadores)
    }

    @Test fun solicitudesRepetidasDeGuardarNoAlternanNiDuplican() = runTest {
        val vm = crear(RepositorioFavoritosPrueba())
        runCurrent()
        vm.cambiarFavorito(evento, guardar = true)
        vm.cambiarFavorito(evento, guardar = true)
        runCurrent()
        assertEquals(listOf(evento), vm.uiState.value.eventos)
        assertFalse(vm.uiState.value.guardando)
    }
}
