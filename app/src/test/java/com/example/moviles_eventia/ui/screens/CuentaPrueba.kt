package com.example.moviles_eventia.ui.screens

import com.example.moviles_eventia.models.Usuario
import com.example.moviles_eventia.repositories.CuentaRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

class CuentaPrueba : CuentaRepository {
    override val usuario = MutableStateFlow<Usuario?>(null)
    private val canal = Channel<Unit>(Channel.CONFLATED)
    override val expirada = canal.receiveAsFlow()
    var logins = 0
    var registros = 0
    var refrescos = 0
    var error: Exception? = null
    override suspend fun iniciarSesion(correo: String, clave: String) {
        logins++
        error?.let { throw it }
        usuario.value = Usuario(7, "Ana", correo.trim())
    }
    override suspend fun registrar(nombre: String, correo: String, clave: String) {
        registros++
        error?.let { throw it }
        usuario.value = Usuario(7, nombre.trim(), correo.trim())
    }
    override suspend fun cerrarSesion() { usuario.value = null }
    override suspend fun actualizarUsuario() {
        refrescos++
        error?.let { throw it }
    }
}
