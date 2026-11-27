package com.example.moviles_eventia.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moviles_eventia.models.Event
import com.example.moviles_eventia.models.DetalleEvento
import com.example.moviles_eventia.repositories.EventRepository
import com.example.moviles_eventia.repositories.CuentaRepository
import com.example.moviles_eventia.repositories.InscripcionRepository
import com.example.moviles_eventia.repositories.SesionRequeridaException
import com.example.moviles_eventia.repositories.mensajeParaUsuario
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class DetalleEventoUiState(
    val evento: Event? = null,
    val cargando: Boolean = true,
    val error: String? = null,
    val esCopiaGuardada: Boolean = false,
    val haySesion: Boolean = false,
    val datosRecientes: Boolean = false,
    val modificandoInscripcion: Boolean = false,
    val mensajeInscripcion: String? = null
) {
    val puedeGestionarInscripcion: Boolean
        get() = datosRecientes && !esCopiaGuardada && !cargando && !modificandoInscripcion &&
            evento?.esProximo() == true &&
            (evento.isAttending || evento.availableSpots != 0)
}

@HiltViewModel
class DetalleEventoViewModel @Inject constructor(
    private val repositorio: EventRepository,
    savedState: SavedStateHandle,
    cuenta: CuentaRepository,
    private val inscripciones: InscripcionRepository
) : ViewModel() {
    private val id: Int = checkNotNull(savedState["eventoId"])
    private val _uiState = MutableStateFlow(DetalleEventoUiState())
    val uiState = _uiState.asStateFlow()
    private var consulta: Job? = null
    private var modificacion: Job? = null
    private var generacion = 0L
    private var activa = false
    private var seReanudo = false
    private var refrescoPendiente = false
    private val acceso = Channel<Unit>(Channel.CONFLATED)
    val solicitarAcceso = acceso.receiveAsFlow()

    init {
        viewModelScope.launch {
            cuenta.usuario.map { it?.id }.distinctUntilChanged().collect { usuarioId ->
                generacion++
                refrescoPendiente = false
                consulta?.cancel()
                modificacion?.cancel()
                _uiState.update {
                    it.copy(
                        haySesion = usuarioId != null, datosRecientes = false,
                        modificandoInscripcion = false, mensajeInscripcion = null,
                        evento = it.evento?.copy(isAttending = false)
                    )
                }
                cargar()
            }
        }
    }

    fun reanudar() {
        activa = true
        // El constructor ya solicita la primera carga. No duplicarla al abrir.
        if (!seReanudo) {
            seReanudo = true
            return
        }
        refrescoPendiente = true
        _uiState.update { it.copy(datosRecientes = false) }
        refrescarSiPendiente()
    }

    fun pausar() {
        activa = false
        _uiState.update { it.copy(datosRecientes = false) }
        // No cancelar una inscripción: el servidor puede haberla procesado.
    }

    private fun refrescarSiPendiente() {
        if (!activa || !refrescoPendiente ||
            consulta?.isActive == true || modificacion?.isActive == true) return
        refrescoPendiente = false
        cargar()
    }

    fun cargar() {
        if (consulta?.isActive == true || modificacion?.isActive == true) return
        val version = generacion
        _uiState.update { it.copy(cargando = true, datosRecientes = false, error = null, mensajeInscripcion = null) }
        consulta = viewModelScope.launch {
            try {
                val detalle = repositorio.obtenerDetalleEvento(id)
                if (version == generacion) aplicarDetalle(detalle)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                if (version == generacion) mostrarError(e)
            } finally {
                if (version == generacion) {
                    consulta = null
                    refrescarSiPendiente()
                }
            }
        }
    }

    fun cambiarInscripcion() {
        val actual = _uiState.value
        if (!actual.haySesion) {
            acceso.trySend(Unit)
            return
        }
        val evento = actual.evento ?: return
        if (!actual.puedeGestionarInscripcion) return
        val version = generacion
        val inscribir = !evento.isAttending
        _uiState.update { it.copy(modificandoInscripcion = true, error = null, mensajeInscripcion = null) }
        modificacion = viewModelScope.launch {
            try {
                val actualizado = inscripciones.cambiarInscripcion(id, inscribir)
                if (version == generacion) {
                    _uiState.update {
                        it.copy(evento = actualizado, datosRecientes = true, esCopiaGuardada = false,
                            mensajeInscripcion = if (inscribir) "Inscripción confirmada." else "Inscripción cancelada.")
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                if (version != generacion) return@launch
                _uiState.update { it.copy(datosRecientes = false) }
                when {
                    e is SesionRequeridaException -> {
                        acceso.trySend(Unit)
                        _uiState.update { it.copy(error = "Inicia sesión para gestionar tu inscripción.") }
                    }
                    e is HttpException && e.code() == 409 -> {
                        _uiState.update {
                            it.copy(mensajeInscripcion =
                                "No se pudo completar la operación: no hay cupos o la inscripción ya cambió.")
                        }
                        try {
                            val detalle = repositorio.obtenerDetalleEvento(id)
                            if (version == generacion) aplicarDetalle(detalle)
                        } catch (refresco: Exception) {
                            if (refresco is CancellationException) throw refresco
                            if (version == generacion) mostrarError(refresco)
                        }
                    }
                    else -> mostrarError(e)
                }
            } finally {
                if (version == generacion) {
                    modificacion = null
                    _uiState.update { it.copy(modificandoInscripcion = false) }
                    refrescarSiPendiente()
                }
            }
        }
    }

    private fun aplicarDetalle(detalle: DetalleEvento) {
        _uiState.update {
            it.copy(
                evento = detalle.evento.copy(
                    isAttending = it.haySesion && !detalle.esCopiaGuardada && detalle.evento.isAttending
                ),
                cargando = false,
                error = null,
                esCopiaGuardada = detalle.esCopiaGuardada,
                datosRecientes = !detalle.esCopiaGuardada
            )
        }
    }

    private fun mostrarError(error: Exception) {
        val noExiste = error is HttpException && error.code() == 404
        _uiState.update {
            it.copy(
                evento = if (noExiste) null else it.evento,
                esCopiaGuardada = if (noExiste) false else it.esCopiaGuardada,
                cargando = false,
                datosRecientes = false,
                error = if (noExiste) "Este evento ya no está disponible." else error.mensajeParaUsuario()
            )
        }
    }
}
