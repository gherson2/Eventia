package com.example.moviles_eventia.session

import com.example.moviles_eventia.models.Sesion
import com.example.moviles_eventia.models.Usuario
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

class SesionPrueba : SesionStore {
    override val sesion = MutableStateFlow<Sesion?>(null)
    private val eventos = Channel<Unit>(Channel.CONFLATED)
    override val expirada = eventos.receiveAsFlow()
    var expiraciones = 0
    override suspend fun guardar(sesion: Sesion) { this.sesion.value = sesion }
    override suspend fun cerrar() { sesion.value = null }
    override suspend fun expirarSiCoincide(token: String) {
        val actual = sesion.value
        if (actual?.token == token && sesion.compareAndSet(actual, null)) {
            expiraciones++
            eventos.send(Unit)
        }
    }
    override suspend fun actualizarUsuarioSiCoincide(token: String, usuario: Usuario) {
        val actual = sesion.value
        if (actual?.token == token) sesion.compareAndSet(actual, actual.copy(usuario = usuario))
    }
}
