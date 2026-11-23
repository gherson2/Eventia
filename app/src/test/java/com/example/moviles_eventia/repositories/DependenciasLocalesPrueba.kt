package com.example.moviles_eventia.repositories

import com.example.moviles_eventia.models.Event
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import com.example.moviles_eventia.db.dao.CacheCatalogoDao
import com.example.moviles_eventia.db.entities.CacheCatalogoEntity

class FavoritosPrueba : FavoritosRepository {
    val datos = MutableStateFlow<List<Event>>(emptyList())
    override fun observarFavoritos() = datos
    override suspend fun obtenerFavorito(id: Int) = datos.value.find { it.id == id }
    override suspend fun guardar(evento: Event) {
        datos.update { it.filterNot { anterior -> anterior.id == evento.id } + evento }
    }
    override suspend fun quitar(id: Int) { datos.update { it.filterNot { evento -> evento.id == id } } }
}

class ConectividadPrueba(inicial: Boolean = true) : ConectividadRepository {
    override val conectado = MutableStateFlow(inicial)
    override fun tieneConexion() = conectado.value
}

class CacheCatalogoPrueba : CacheCatalogoDao {
    val datos = mutableMapOf<String, CacheCatalogoEntity>()
    var errorLectura: Exception? = null
    var errorEscritura: Exception? = null
    override suspend fun obtener(clave: String): CacheCatalogoEntity? {
        errorLectura?.let { throw it }
        return datos[clave]
    }
    override suspend fun guardar(copia: CacheCatalogoEntity) {
        errorEscritura?.let { throw it }
        datos[copia.clave] = copia
    }
}
