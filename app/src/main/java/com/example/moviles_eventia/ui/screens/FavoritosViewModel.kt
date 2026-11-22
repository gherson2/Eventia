package com.example.moviles_eventia.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moviles_eventia.models.Event
import com.example.moviles_eventia.repositories.FavoritosRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class FavoritosUiState(
    val eventos: List<Event> = emptyList(),
    val cargando: Boolean = true,
    val guardando: Boolean = false,
    val error: String? = null
)

data class MensajeFavorito(val texto: String, val quitado: Event? = null)

@HiltViewModel
class FavoritosViewModel @Inject constructor(
    private val repositorio: FavoritosRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(FavoritosUiState())
    val uiState = _uiState.asStateFlow()
    private val canalMensajes = Channel<MensajeFavorito>(Channel.BUFFERED)
    val mensajes = canalMensajes.receiveAsFlow()
    private var observacion: Job? = null
    private val escritura = Mutex()

    init { cargar() }

    fun cargar() {
        observacion?.cancel()
        _uiState.update { it.copy(cargando = true, error = null) }
        observacion = viewModelScope.launch {
            try {
                repositorio.observarFavoritos().collect { eventos ->
                    _uiState.update { it.copy(eventos = eventos, cargando = false, error = null) }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update {
                    it.copy(cargando = false, error = "No pudimos leer tus favoritos. Inténtalo nuevamente.")
                }
            }
        }
    }

    // La UI envía el estado deseado, no una alternancia basada en un Flow aún desactualizado.
    fun cambiarFavorito(evento: Event, guardar: Boolean) {
        modificar(evento, guardar, ofrecerDeshacer = !guardar)
    }

    fun restaurar(evento: Event) {
        modificar(evento, guardar = true, ofrecerDeshacer = false)
    }

    private fun modificar(evento: Event, guardar: Boolean, ofrecerDeshacer: Boolean) {
        viewModelScope.launch {
            escritura.withLock {
                _uiState.update { it.copy(guardando = true) }
                try {
                    if (guardar) repositorio.guardar(evento) else repositorio.quitar(evento.id)
                    if (ofrecerDeshacer) {
                        canalMensajes.send(MensajeFavorito("Quitado de favoritos", evento))
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    canalMensajes.send(MensajeFavorito("No pudimos guardar el cambio en favoritos."))
                } finally {
                    _uiState.update { it.copy(guardando = false) }
                }
            }
        }
    }
}
