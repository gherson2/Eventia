
package com.example.moviles_eventia.repositories

import com.example.moviles_eventia.models.Event
import com.example.moviles_eventia.models.Category
import com.example.moviles_eventia.models.Venue

interface EventRepository {

    suspend fun obtenerEventos(busqueda: String = "", categoriaId: Int? = null): List<Event>
    suspend fun obtenerCategorias(): List<Category>
    suspend fun obtenerEvento(id: Int): Event
    suspend fun obtenerLugares(): List<Venue>
    suspend fun obtenerLugar(id: Int): Venue
    suspend fun obtenerEventosDelLugar(id: Int): List<Event>

}
