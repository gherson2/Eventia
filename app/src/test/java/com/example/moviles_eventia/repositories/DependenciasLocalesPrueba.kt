package com.example.moviles_eventia.repositories

import com.example.moviles_eventia.models.Event
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

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
