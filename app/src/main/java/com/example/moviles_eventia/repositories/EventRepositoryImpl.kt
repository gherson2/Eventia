
package com.example.moviles_eventia.repositories

import com.example.moviles_eventia.api.EventiaApi
import com.example.moviles_eventia.models.Event
import com.example.moviles_eventia.models.filtrarEventos
import javax.inject.Inject
import com.example.moviles_eventia.models.DetalleEvento
import java.io.IOException
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

class EventRepositoryImpl @Inject constructor(
    private val api: EventiaApi,
    private val favoritos: FavoritosRepository,
    private val conectividad: ConectividadRepository
) : EventRepository {

    override suspend fun obtenerEventos(busqueda: String, categoriaId: Int?): List<Event> {

        val eventosDto = api.obtenerEventos(busqueda.trim().takeIf { it.isNotEmpty() }, categoriaId)

        return filtrarEventos(eventosDto.map { it.toEvent() }, busqueda, categoriaId)
    }

    override suspend fun obtenerCategorias() = api.obtenerCategorias().map { it.toCategory() }
    override suspend fun obtenerEvento(id: Int) = api.obtenerEvento(id).toEvent()
    override suspend fun obtenerDetalleEvento(id: Int): DetalleEvento {
        if (!conectividad.tieneConexion()) {
            val guardado = favoritos.obtenerFavorito(id)
                ?: throw IOException("No hay un favorito guardado para este evento.")
            return DetalleEvento(guardado, esCopiaGuardada = true)
        }
        return try {
            DetalleEvento(obtenerEvento(id))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            // Un 404 debe informarse, no ocultarse detrás de una copia antigua.
            val permiteCopia = e is IOException || (e is HttpException && e.code() in 500..599)
            if (!permiteCopia) throw e
            val guardado = favoritos.obtenerFavorito(id) ?: throw e
            DetalleEvento(guardado, esCopiaGuardada = true)
        }
    }
    override suspend fun obtenerLugares() = api.obtenerLugares().map { it.toVenue() }
    override suspend fun obtenerLugar(id: Int) = api.obtenerLugar(id).toVenue()
    override suspend fun obtenerEventosDelLugar(id: Int) =
        filtrarEventos(api.obtenerEventosDelLugar(id).map { it.toEvent() })
}
