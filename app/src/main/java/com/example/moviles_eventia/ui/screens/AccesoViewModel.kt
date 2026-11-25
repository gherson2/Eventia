package com.example.moviles_eventia.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moviles_eventia.models.validarCuenta
import com.example.moviles_eventia.repositories.CuentaRepository
import com.example.moviles_eventia.repositories.ErrorCuenta
import com.example.moviles_eventia.repositories.mensajeParaUsuario
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccesoUiState(
    val nombre: String = "",
    val correo: String = "",
    val clave: String = "",
    val registro: Boolean = false,
    val enviando: Boolean = false,
    val error: String? = null,
    val campos: Map<String, String> = emptyMap(),
    val autenticado: Boolean = false
)

@HiltViewModel
class AccesoViewModel @Inject constructor(
    private val repositorio: CuentaRepository,
    private val guardado: SavedStateHandle
) : ViewModel() {
    private val _uiState = MutableStateFlow(AccesoUiState(
        nombre = guardado["nombre"] ?: "",
        correo = guardado["correo"] ?: "",
        registro = guardado["registro"] ?: false
    ))
    val uiState = _uiState.asStateFlow()

    fun cambiarNombre(valor: String) {
        if (_uiState.value.enviando) return
        guardado["nombre"] = valor
        _uiState.update { it.copy(nombre = valor, error = null, campos = it.campos - "name") }
    }
    fun cambiarCorreo(valor: String) {
        if (_uiState.value.enviando) return
        guardado["correo"] = valor
        _uiState.update { it.copy(correo = valor, error = null, campos = it.campos - "email") }
    }
    fun cambiarClave(valor: String) {
        if (_uiState.value.enviando) return
        // La contraseña vive solo en memoria del ViewModel, nunca en SavedStateHandle.
        _uiState.update { it.copy(clave = valor, error = null, campos = it.campos - "password") }
    }
    fun cambiarModo() {
        if (_uiState.value.enviando) return
        val registro = !_uiState.value.registro
        guardado["registro"] = registro
        _uiState.update { it.copy(registro = registro, clave = "", error = null, campos = emptyMap()) }
    }
    fun enviar() {
        val formulario = _uiState.value
        if (formulario.enviando || formulario.autenticado) return
        val campos = validarCuenta(formulario.nombre, formulario.correo, formulario.clave, formulario.registro)
        if (campos.isNotEmpty()) {
            _uiState.update { it.copy(campos = campos, error = null) }
            return
        }
        _uiState.update { it.copy(enviando = true, error = null, campos = emptyMap()) }
        viewModelScope.launch {
            try {
                if (formulario.registro) {
                    repositorio.registrar(formulario.nombre, formulario.correo, formulario.clave)
                } else repositorio.iniciarSesion(formulario.correo, formulario.clave)
                _uiState.update { it.copy(enviando = false, clave = "", autenticado = true) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(
                    enviando = false,
                    error = if (e is ErrorCuenta) e.message else e.mensajeParaUsuario(),
                    campos = if (e is ErrorCuenta) e.campos else emptyMap()
                ) }
            }
        }
    }
}
