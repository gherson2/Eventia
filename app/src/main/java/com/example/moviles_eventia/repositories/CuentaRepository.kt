package com.example.moviles_eventia.repositories

import com.example.moviles_eventia.models.Usuario
import kotlinx.coroutines.flow.Flow

interface CuentaRepository {
    val usuario: Flow<Usuario?>
    val expirada: Flow<Unit>
    suspend fun iniciarSesion(correo: String, clave: String)
    suspend fun registrar(nombre: String, correo: String, clave: String)
    suspend fun cerrarSesion()
    suspend fun actualizarUsuario()
}
