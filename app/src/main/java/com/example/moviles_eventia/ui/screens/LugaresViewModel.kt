package com.example.moviles_eventia.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moviles_eventia.models.Venue
import com.example.moviles_eventia.repositories.EventRepository
import com.example.moviles_eventia.repositories.mensajeParaUsuario
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LugaresUiState(
    val lugares: List<Venue> = emptyList(),
    val cargando: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class LugaresViewModel @Inject constructor(private val repositorio: EventRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(LugaresUiState())
    val uiState = _uiState.asStateFlow()
    private var consulta: Job? = null

    init { cargar() }

    fun cargar() {
        if (consulta?.isActive == true) return
        _uiState.update { it.copy(cargando = true, error = null) }
        consulta = viewModelScope.launch {
            try {
                val lugares = repositorio.obtenerLugares()
                _uiState.update { it.copy(lugares = lugares, cargando = false) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(cargando = false, error = e.mensajeParaUsuario()) }
            }
        }
    }
}
