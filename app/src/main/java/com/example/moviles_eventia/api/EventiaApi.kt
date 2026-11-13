package com.example.moviles_eventia.api

import com.example.moviles_eventia.api.dto.EventDto
import retrofit2.http.GET

interface EventiaApi {

    @GET("events")
    suspend fun obtenerEventos(): List<EventDto>

}