package com.example.moviles_eventia.repositories

import kotlinx.coroutines.flow.Flow

interface ConectividadRepository {
    val conectado: Flow<Boolean>
    fun tieneConexion(): Boolean
}
