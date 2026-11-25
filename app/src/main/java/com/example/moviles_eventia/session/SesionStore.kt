package com.example.moviles_eventia.session

import com.example.moviles_eventia.models.Sesion
import com.example.moviles_eventia.models.Usuario
import kotlinx.coroutines.flow.Flow

interface SesionStore {
    val sesion: Flow<Sesion?>
    val expirada: Flow<Unit>
    suspend fun guardar(sesion: Sesion)
    suspend fun cerrar()
    suspend fun expirarSiCoincide(token: String)
    suspend fun actualizarUsuarioSiCoincide(token: String, usuario: Usuario)
}
