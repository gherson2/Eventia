package com.example.moviles_eventia.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moviles_eventia.models.Category
import com.example.moviles_eventia.models.Event
import com.example.moviles_eventia.models.filtrarEventos
import com.example.moviles_eventia.repositories.EventRepository
import com.example.moviles_eventia.repositories.mensajeParaUsuario
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExplorarUiState(
    val eventos: List<Event> = emptyList(),
    val categorias: List<Category> = emptyList(),
    val busqueda: String = "",
    val categoriaId: Int? = null,
    val cargando: Boolean = true,
    val error: String? = null,
    val errorCategorias: String? = null
)

class ExplorarViewModel(
    private val repositorio: EventRepository,
    private val savedState: SavedStateHandle
) : ViewModel() {
    private val _uiState = MutableStateFlow(ExplorarUiState(
        busqueda = savedState["busqueda"] ?: "",
        categoriaId = savedState.get<Int>("categoriaId")?.takeIf { it >= 0 }
    ))
    val uiState = _uiState.asStateFlow()
    private var consulta: Job? = null
    private var consultaCategorias: Job? = null

    init {
        obtenerEventos()
        obtenerCategorias()
    }

    fun cambiarBusqueda(texto: String) {
        savedState["busqueda"] = texto
        _uiState.update {
            it.copy(busqueda = texto, eventos = filtrarEventos(it.eventos, texto, it.categoriaId))
        }
        cargar(esperar = true)
    }

    fun cambiarCategoria(id: Int?) {
        savedState["categoriaId"] = id ?: -1
        _uiState.update {
            it.copy(categoriaId = id, eventos = filtrarEventos(it.eventos, it.busqueda, id))
        }
        cargar()
    }

    fun limpiarFiltros() {
        savedState["busqueda"] = ""
        savedState["categoriaId"] = -1
        _uiState.update { it.copy(busqueda = "", categoriaId = null) }
        cargar()
    }

    fun obtenerEventos() = cargar()

    fun obtenerCategorias() {
        if (consultaCategorias?.isActive == true) return
        consultaCategorias = viewModelScope.launch {
            try {
                val categorias = repositorio.obtenerCategorias()
                _uiState.update { it.copy(categorias = categorias, errorCategorias = null) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(errorCategorias = e.mensajeParaUsuario()) }
            }
        }
    }

    private fun cargar(esperar: Boolean = false) {
        consulta?.cancel()
        // Actualizar nunca elimina los datos existentes ante un fallo de red.
        _uiState.update { it.copy(cargando = true, error = null) }
        consulta = viewModelScope.launch {
            if (esperar) delay(300)
            val filtros = _uiState.value
            try {
                val eventos = repositorio.obtenerEventos(filtros.busqueda, filtros.categoriaId)
                _uiState.update { it.copy(eventos = eventos, cargando = false, error = null) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(cargando = false, error = e.mensajeParaUsuario()) }
            }
        }
    }
}
