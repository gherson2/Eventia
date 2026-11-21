package com.example.moviles_eventia.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moviles_eventia.models.Event
import com.example.moviles_eventia.models.Venue
import com.example.moviles_eventia.repositories.EventRepository
import com.example.moviles_eventia.repositories.mensajeParaUsuario
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class DetalleLugarUiState(
    val lugar: Venue? = null,
    val eventos: List<Event> = emptyList(),
    val cargando: Boolean = true,
    val error: String? = null,
    val errorEventos: String? = null
)

class DetalleLugarViewModel(
    private val repositorio: EventRepository,
    savedState: SavedStateHandle
) : ViewModel() {
    private val id: Int = checkNotNull(savedState["lugarId"])
    private val _uiState = MutableStateFlow(DetalleLugarUiState())
    val uiState = _uiState.asStateFlow()
    private var consulta: Job? = null

    init { cargar() }

    fun cargar() {
        if (consulta?.isActive == true) return
        _uiState.update { it.copy(cargando = true, error = null, errorEventos = null) }
        consulta = viewModelScope.launch {
            try {
                val lugar = repositorio.obtenerLugar(id)
                _uiState.update { it.copy(lugar = lugar) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                val noExiste = e is HttpException && e.code() == 404
                _uiState.update {
                    it.copy(
                        lugar = if (noExiste) null else it.lugar,
                        eventos = if (noExiste) emptyList() else it.eventos,
                        cargando = false,
                        error = if (noExiste) "Este lugar ya no está disponible." else e.mensajeParaUsuario()
                    )
                }
                return@launch
            }
            try {
                val eventos = repositorio.obtenerEventosDelLugar(id)
                _uiState.update { it.copy(eventos = eventos, cargando = false) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                // Un fallo de eventos no oculta la información del lugar.
                _uiState.update { it.copy(cargando = false, errorEventos = e.mensajeParaUsuario()) }
            }
        }
    }
}
