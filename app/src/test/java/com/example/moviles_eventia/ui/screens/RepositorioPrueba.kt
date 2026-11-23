package com.example.moviles_eventia.ui.screens

import com.example.moviles_eventia.models.Category
import com.example.moviles_eventia.models.Event
import com.example.moviles_eventia.models.Venue
import com.example.moviles_eventia.models.filtrarEventos
import com.example.moviles_eventia.models.DetalleEvento
import com.example.moviles_eventia.models.EstadoCatalogo
import kotlinx.coroutines.flow.flow
import com.example.moviles_eventia.repositories.EventRepository

class RepositorioPrueba : EventRepository {
    val lugar = Venue(7, "Casa cultural", "Calle 7", -17.3, -66.1)
    val evento = Event(42, "Jazz", "Concierto", "2099-12-01T20:00:00-04:00",
        Category(3, "Música"), null, lugar, 50, 10, "Descripción completa")
    var error: Exception? = null
    var errorEventosLugar: Exception? = null
    var esCopiaGuardada = false
    var resultadosCatalogo: List<EstadoCatalogo>? = null
    val consultas = mutableListOf<Pair<String, Int?>>()

    override suspend fun obtenerEventos(busqueda: String, categoriaId: Int?): List<Event> {
        consultas += busqueda to categoriaId
        error?.let { throw it }
        return filtrarEventos(listOf(evento), busqueda, categoriaId)
    }
    override fun observarCatalogo(busqueda: String, categoriaId: Int?) = flow {
        val resultados = resultadosCatalogo
        if (resultados == null) emit(EstadoCatalogo(obtenerEventos(busqueda, categoriaId)))
        else resultados.forEach { emit(it) }
    }
    override suspend fun obtenerCategorias() = listOf(evento.category)
    override suspend fun obtenerEvento(id: Int): Event {
        error?.let { throw it }
        return evento
    }
    override suspend fun obtenerDetalleEvento(id: Int) =
        DetalleEvento(obtenerEvento(id), esCopiaGuardada)
    override suspend fun obtenerLugares(): List<Venue> {
        error?.let { throw it }
        return listOf(lugar)
    }
    override suspend fun obtenerLugar(id: Int): Venue {
        error?.let { throw it }
        return lugar
    }
    override suspend fun obtenerEventosDelLugar(id: Int): List<Event> {
        errorEventosLugar?.let { throw it }
        return listOf(evento)
    }
}
