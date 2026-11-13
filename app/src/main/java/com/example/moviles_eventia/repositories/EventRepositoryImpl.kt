
package com.example.moviles_eventia.repositories

import com.example.moviles_eventia.api.EventiaApi
import com.example.moviles_eventia.models.Event

class EventRepositoryImpl(
    private val api: EventiaApi
) : EventRepository {

    override suspend fun obtenerEventos(): List<Event> {

        val eventosDto = api.obtenerEventos()

        return eventosDto.map { it.toEvent() }
    }
}
