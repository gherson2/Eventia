package com.example.moviles_eventia.repositories

import com.example.moviles_eventia.models.Event
import kotlinx.coroutines.flow.Flow

interface FavoritosRepository {
    fun observarFavoritos(): Flow<List<Event>>
    suspend fun obtenerFavorito(id: Int): Event?
    suspend fun guardar(evento: Event)
    suspend fun quitar(id: Int)
}
