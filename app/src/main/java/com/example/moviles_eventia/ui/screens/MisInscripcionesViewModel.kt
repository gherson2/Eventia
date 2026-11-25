package com.example.moviles_eventia.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moviles_eventia.models.Event
import com.example.moviles_eventia.repositories.CuentaRepository
import com.example.moviles_eventia.repositories.InscripcionRepository
import com.example.moviles_eventia.repositories.SesionRequeridaException
import com.example.moviles_eventia.repositories.mensajeParaUsuario
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MisInscripcionesUiState(
    val usuarioId: Int? = null,
    val eventos: List<Event> = emptyList(),
    val cargando: Boolean = false,
    val cargado: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class MisInscripcionesViewModel @Inject constructor(
    cuenta: CuentaRepository,
    private val inscripciones: InscripcionRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(MisInscripcionesUiState())
    val uiState = _uiState.asStateFlow()
    private var consulta: Job? = null
    private var version = 0L
    private var activa = false

    init {
        viewModelScope.launch {
            try {
                cuenta.usuario.map { it?.id }.distinctUntilChanged().collect { id ->
                    version++
                    consulta?.cancel()
                    _uiState.value = MisInscripcionesUiState(usuarioId = id)
                    if (activa && id != null) cargar()
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                version++
                consulta?.cancel()
                _uiState.value = MisInscripcionesUiState(error = "No pudimos verificar la sesión.")
            }
        }
    }

    fun reanudar() {
        activa = true
        cargar()
    }

    fun pausar() {
        activa = false
        version++
        consulta?.cancel()
        _uiState.update { it.copy(cargando = false) }
    }

    fun cargar() {
        val usuarioId = _uiState.value.usuarioId ?: return
        if (!activa) return
        // Refrescar al volver descarta una consulta anterior, no una inscripción.
        consulta?.cancel()
        val actual = ++version
        _uiState.update { it.copy(cargando = true, error = null) }
        consulta = viewModelScope.launch {
            try {
                val eventos = inscripciones.obtenerMisInscripciones(usuarioId)
                if (actual == version) {
                    _uiState.update { it.copy(eventos = eventos, cargando = false, cargado = true) }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                if (actual == version) {
                    _uiState.update {
                        it.copy(
                            cargando = false,
                            error = if (e is SesionRequeridaException)
                                "Inicia sesión para consultar tus inscripciones." else e.mensajeParaUsuario()
                        )
                    }
                }
            } finally {
                if (actual == version) _uiState.update { it.copy(cargando = false) }
            }
        }
    }
}
