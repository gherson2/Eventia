package com.example.moviles_eventia.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moviles_eventia.models.Usuario
import com.example.moviles_eventia.repositories.CuentaRepository
import com.example.moviles_eventia.repositories.mensajeParaUsuario
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CuentaUiState(
    val usuario: Usuario? = null,
    val leyendoSesion: Boolean = true,
    val operando: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CuentaViewModel @Inject constructor(
    private val repositorio: CuentaRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CuentaUiState())
    val uiState = _uiState.asStateFlow()
    val expiraciones = repositorio.expirada
    private var operacion: Job? = null

    init {
        viewModelScope.launch {
            try {
                repositorio.usuario.collect { usuario ->
                    _uiState.update { it.copy(usuario = usuario, leyendoSesion = false, error = null) }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(leyendoSesion = false, error = "No pudimos leer la sesión local.") }
            }
        }
        viewModelScope.launch {
            try {
                if (repositorio.usuario.first() != null) actualizar()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            }
        }
    }

    fun actualizar() {
        if (operacion?.isActive == true || _uiState.value.operando) return
        operacion = viewModelScope.launch {
            _uiState.update { it.copy(operando = true, error = null) }
            try {
                repositorio.actualizarUsuario()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(error = e.mensajeParaUsuario()) }
            } finally {
                _uiState.update { it.copy(operando = false) }
            }
        }
    }

    fun cerrarSesion() {
        operacion?.cancel()
        operacion = viewModelScope.launch {
            _uiState.update { it.copy(operando = true, error = null) }
            try {
                repositorio.cerrarSesion()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(error = "No se pudo cerrar la sesión. Inténtalo nuevamente.") }
            } finally {
                _uiState.update { it.copy(operando = false) }
            }
        }
    }
}
