package com.example.moviles_eventia.api

import com.example.moviles_eventia.api.dto.EventDto
import com.example.moviles_eventia.api.dto.CategoryDto
import com.example.moviles_eventia.api.dto.VenueDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Tag
import com.example.moviles_eventia.api.dto.LoginRequest
import com.example.moviles_eventia.api.dto.RegistroRequest
import com.example.moviles_eventia.api.dto.AutenticacionDto
import com.example.moviles_eventia.api.dto.UsuarioDto

interface EventiaApi {
    @POST("auth/login")
    suspend fun iniciarSesion(@Body solicitud: LoginRequest): AutenticacionDto

    @POST("auth/register")
    suspend fun registrar(@Body solicitud: RegistroRequest): AutenticacionDto

    @GET("me")
    suspend fun obtenerUsuario(): UsuarioDto

    @GET("events")
    suspend fun obtenerEventos(
        @Query("q") busqueda: String? = null,
        @Query("category") categoria: Int? = null
    ): List<EventDto>

    @GET("categories")
    suspend fun obtenerCategorias(): List<CategoryDto>

    @GET("events/{id}")
    suspend fun obtenerEvento(@Path("id") id: Int): EventDto

    @POST("events/{id}/attendance")
    suspend fun inscribir(@Path("id") id: Int, @Tag sesion: SesionEsperada): EventDto

    @DELETE("events/{id}/attendance")
    suspend fun cancelarInscripcion(@Path("id") id: Int, @Tag sesion: SesionEsperada): EventDto

    @GET("venues")
    suspend fun obtenerLugares(): List<VenueDto>

    @GET("venues/{id}")
    suspend fun obtenerLugar(@Path("id") id: Int): VenueDto

    @GET("venues/{id}/events")
    suspend fun obtenerEventosDelLugar(@Path("id") id: Int): List<EventDto>
}
