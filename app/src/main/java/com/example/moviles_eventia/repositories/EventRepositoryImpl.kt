
package com.example.moviles_eventia.repositories

import com.example.moviles_eventia.api.EventiaApi
import com.example.moviles_eventia.models.Event
import com.example.moviles_eventia.models.filtrarEventos

class EventRepositoryImpl(
    private val api: EventiaApi
) : EventRepository {

    override suspend fun obtenerEventos(busqueda: String, categoriaId: Int?): List<Event> {

        val eventosDto = api.obtenerEventos(busqueda.trim().takeIf { it.isNotEmpty() }, categoriaId)

        return filtrarEventos(eventosDto.map { it.toEvent() }, busqueda, categoriaId)
    }

    override suspend fun obtenerCategorias() = api.obtenerCategorias().map { it.toCategory() }
    override suspend fun obtenerEvento(id: Int) = api.obtenerEvento(id).toEvent()
    override suspend fun obtenerLugares() = api.obtenerLugares().map { it.toVenue() }
    override suspend fun obtenerLugar(id: Int) = api.obtenerLugar(id).toVenue()
    override suspend fun obtenerEventosDelLugar(id: Int) =
        filtrarEventos(api.obtenerEventosDelLugar(id).map { it.toEvent() })
}
