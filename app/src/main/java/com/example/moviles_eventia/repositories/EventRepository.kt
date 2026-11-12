
package com.example.moviles_eventia.repositories

import com.example.moviles_eventia.models.Event

interface EventRepository {

    suspend fun obtenerEventos(): List<Event>

}
