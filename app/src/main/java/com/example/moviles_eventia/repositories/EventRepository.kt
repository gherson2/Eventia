
package com.example.moviles_eventia.repositories

import com.example.moviles_eventia.models.Event
import com.example.moviles_eventia.models.Category
import com.example.moviles_eventia.models.Venue
import com.example.moviles_eventia.models.DetalleEvento
import com.example.moviles_eventia.models.EstadoCatalogo
import kotlinx.coroutines.flow.Flow

interface EventRepository {

    suspend fun obtenerEventos(busqueda: String = "", categoriaId: Int? = null): List<Event>
    fun observarCatalogo(busqueda: String = "", categoriaId: Int? = null): Flow<EstadoCatalogo>
    suspend fun obtenerCategorias(): List<Category>
    suspend fun obtenerEvento(id: Int): Event
    suspend fun obtenerDetalleEvento(id: Int): DetalleEvento
    suspend fun obtenerLugares(): List<Venue>
    suspend fun obtenerLugar(id: Int): Venue
    suspend fun obtenerEventosDelLugar(id: Int): List<Event>

}
