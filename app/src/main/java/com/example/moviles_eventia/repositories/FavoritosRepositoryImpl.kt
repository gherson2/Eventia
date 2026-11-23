package com.example.moviles_eventia.repositories

import com.example.moviles_eventia.db.dao.FavoritosDao
import com.example.moviles_eventia.db.entities.FavoritoEntity
import com.example.moviles_eventia.models.Event
import com.google.gson.Gson
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class FavoritosRepositoryImpl @Inject constructor(
    private val dao: FavoritosDao,
    private val gson: Gson
) : FavoritosRepository {
    override fun observarFavoritos() = dao.observarFavoritos().map { favoritos ->
        favoritos.map { favorito ->
            requireNotNull(gson.fromJson(favorito.json, Event::class.java)) {
                "El favorito ${favorito.id} no contiene datos válidos"
            }
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun obtenerFavorito(id: Int): Event? = withContext(Dispatchers.IO) {
        dao.obtenerPorId(id)?.let { gson.fromJson(it.json, Event::class.java) }
    }

    override suspend fun guardar(evento: Event) = withContext(Dispatchers.IO) {
        dao.guardar(FavoritoEntity(evento.id, gson.toJson(evento)))
    }

    override suspend fun quitar(id: Int) = dao.quitar(id)
}
