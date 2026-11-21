package com.example.moviles_eventia.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moviles_eventia.models.Event
import com.example.moviles_eventia.repositories.EventRepository
import com.example.moviles_eventia.repositories.mensajeParaUsuario
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class DetalleEventoUiState(
    val evento: Event? = null,
    val cargando: Boolean = true,
    val error: String? = null
)

class DetalleEventoViewModel(
    private val repositorio: EventRepository,
    savedState: SavedStateHandle
) : ViewModel() {
    private val id: Int = checkNotNull(savedState["eventoId"])
    private val _uiState = MutableStateFlow(DetalleEventoUiState())
    val uiState = _uiState.asStateFlow()
    private var consulta: Job? = null

    init { cargar() }

    fun cargar() {
        if (consulta?.isActive == true) return
        _uiState.update { it.copy(cargando = true, error = null) }
        consulta = viewModelScope.launch {
            try {
                val evento = repositorio.obtenerEvento(id)
                _uiState.value = DetalleEventoUiState(evento = evento, cargando = false)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                val noExiste = e is HttpException && e.code() == 404
                _uiState.update {
                    it.copy(
                        evento = if (noExiste) null else it.evento,
                        cargando = false,
                        error = if (noExiste) "Este evento ya no está disponible." else e.mensajeParaUsuario()
                    )
                }
            }
        }
    }
}
