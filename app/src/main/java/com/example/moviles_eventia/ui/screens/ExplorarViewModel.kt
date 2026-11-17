
package com.example.moviles_eventia.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moviles_eventia.AppContainer
import com.example.moviles_eventia.models.Event
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ExplorarUiState(
    val eventos: List<Event> = emptyList(),
    val cargando: Boolean = false,
    val error: String? = null
)

class ExplorarViewModel : ViewModel() {

    private val repositorio = AppContainer.eventRepository

    private val _uiState = MutableStateFlow(ExplorarUiState())
    val uiState: StateFlow<ExplorarUiState> = _uiState

    init {
        obtenerEventos()
    }

    fun obtenerEventos() {
        viewModelScope.launch {

            _uiState.value = ExplorarUiState(cargando = true)

            try {
                val lista = repositorio.obtenerEventos()

                _uiState.value = ExplorarUiState(
                    eventos = lista
                )

            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    throw e
                }

                _uiState.value = ExplorarUiState(
                    error = "No se pudieron cargar los eventos"
                )
            }
        }
    }
}
