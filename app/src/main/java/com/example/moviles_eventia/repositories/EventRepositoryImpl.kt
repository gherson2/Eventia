
package com.example.moviles_eventia.repositories

import com.example.moviles_eventia.api.EventiaApi
import com.example.moviles_eventia.models.Event
import com.example.moviles_eventia.models.filtrarEventos
import javax.inject.Inject
import com.example.moviles_eventia.models.DetalleEvento
import java.io.IOException
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import com.example.moviles_eventia.db.dao.CacheCatalogoDao
import com.example.moviles_eventia.db.entities.CacheCatalogoEntity
import com.example.moviles_eventia.models.Category
import com.example.moviles_eventia.models.EstadoCatalogo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.Locale

class EventRepositoryImpl @Inject constructor(
    private val api: EventiaApi,
    private val favoritos: FavoritosRepository,
    private val conectividad: ConectividadRepository,
    private val cache: CacheCatalogoDao,
    private val gson: Gson
) : EventRepository {
    private val tipoEventos = object : TypeToken<List<Event>>() {}.type
    private val tipoCategorias = object : TypeToken<List<Category>>() {}.type

    override suspend fun obtenerEventos(busqueda: String, categoriaId: Int?): List<Event> {

        val eventosDto = api.obtenerEventos(busqueda.trim().takeIf { it.isNotEmpty() }, categoriaId)

        return filtrarEventos(eventosDto.map { it.toEvent() }, busqueda, categoriaId)
    }

    override fun observarCatalogo(busqueda: String, categoriaId: Int?) = flow {
        val clave = if (busqueda.isBlank() && categoriaId == null) "catalogo"
            else "eventos:${busqueda.trim().lowercase(Locale.ROOT)}:$categoriaId"
        var errorLocal = false
        val copia = try {
            val exacta = cache.obtener(clave)
            val completa = if (clave == "catalogo") null else cache.obtener("catalogo")
            listOfNotNull(exacta, completa).maxByOrNull { it.guardadoEn }?.let { fila ->
                val datos: List<Event> = requireNotNull(gson.fromJson(fila.json, tipoEventos))
                fila to filtrarEventos(datos, busqueda, categoriaId)
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            errorLocal = true
            null
        }
        emit(EstadoCatalogo(copia?.second, cargando = true, guardadoEn = copia?.first?.guardadoEn))

        if (!conectividad.tieneConexion()) {
            emit(EstadoCatalogo(
                eventos = copia?.second,
                guardadoEn = copia?.first?.guardadoEn,
                aviso = if (copia != null) null else if (errorLocal)
                    "No pudimos leer el catálogo guardado. Reintenta cuando recuperes la conexión."
                    else "Sin conexión. Todavía no hay un catálogo guardado en este dispositivo."
            ))
            return@flow
        }
        val eventos = try {
            obtenerEventos(busqueda, categoriaId)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(EstadoCatalogo(copia?.second, guardadoEn = copia?.first?.guardadoEn,
                aviso = e.mensajeParaUsuario()))
            return@flow
        }
        var aviso: String? = null
        try {
            // Las búsquedas no reemplazan el catálogo completo, ni los favoritos.
            cache.guardar(CacheCatalogoEntity(clave, gson.toJson(eventos), System.currentTimeMillis()))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            aviso = "Eventos actualizados, pero no pudimos guardarlos para usarlos sin conexión."
        }
        emit(EstadoCatalogo(eventos, aviso = aviso))
    }.flowOn(Dispatchers.IO)

    override suspend fun obtenerCategorias(): List<Category> = withContext(Dispatchers.IO) {
        if (!conectividad.tieneConexion()) {
            val copia = cache.obtener("categorias") ?: throw IOException("Sin categorías guardadas")
            return@withContext requireNotNull(gson.fromJson<List<Category>>(copia.json, tipoCategorias))
        }
        val categorias = try {
            api.obtenerCategorias().map { it.toCategory() }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            if (e !is IOException && !(e is HttpException && e.code() in 500..599)) throw e
            val copia = cache.obtener("categorias") ?: throw e
            return@withContext requireNotNull(gson.fromJson<List<Category>>(copia.json, tipoCategorias))
        }
        try {
            cache.guardar(CacheCatalogoEntity("categorias", gson.toJson(categorias), System.currentTimeMillis()))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            // Una copia fallida no impide usar las categorías ya descargadas.
        }
        categorias
    }
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
