package com.example.moviles_eventia.api

import com.example.moviles_eventia.api.dto.EventDto
import com.example.moviles_eventia.api.dto.CategoryDto
import com.example.moviles_eventia.api.dto.VenueDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface EventiaApi {

    @GET("events")
    suspend fun obtenerEventos(
        @Query("q") busqueda: String? = null,
        @Query("category") categoria: Int? = null
    ): List<EventDto>

    @GET("categories")
    suspend fun obtenerCategorias(): List<CategoryDto>

    @GET("events/{id}")
    suspend fun obtenerEvento(@Path("id") id: Int): EventDto

    @GET("venues")
    suspend fun obtenerLugares(): List<VenueDto>

    @GET("venues/{id}")
    suspend fun obtenerLugar(@Path("id") id: Int): VenueDto

    @GET("venues/{id}/events")
    suspend fun obtenerEventosDelLugar(@Path("id") id: Int): List<EventDto>
}
