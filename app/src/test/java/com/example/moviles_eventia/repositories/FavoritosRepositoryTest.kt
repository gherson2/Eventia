package com.example.moviles_eventia.repositories

import com.example.moviles_eventia.db.dao.FavoritosDao
import com.example.moviles_eventia.db.entities.FavoritoEntity
import com.example.moviles_eventia.ui.screens.RepositorioPrueba
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class FavoritosRepositoryTest {
    private class DaoPrueba : FavoritosDao {
        val datos = MutableStateFlow<List<FavoritoEntity>>(emptyList())
        override fun observarFavoritos() = datos
        override suspend fun obtenerPorId(id: Int) = datos.value.find { it.id == id }
        override suspend fun guardar(favorito: FavoritoEntity) {
            datos.update { anteriores ->
                (anteriores.filterNot { it.id == favorito.id } + favorito).sortedBy { it.id }
            }
        }
        override suspend fun quitar(id: Int) { datos.update { it.filterNot { fila -> fila.id == id } } }
    }

    @Test fun guardaYRecuperaLaCopiaCompletaSinApi() = runTest {
        val dao = DaoPrueba()
        val repo = FavoritosRepositoryImpl(dao, Gson())
        val evento = RepositorioPrueba().evento
        repo.guardar(evento)
        assertEquals(evento, repo.observarFavoritos().first().single())
        assertEquals(evento.id, dao.datos.value.single().id)
    }

    @Test fun guardarDeNuevoActualizaSinDuplicarElId() = runTest {
        val dao = DaoPrueba()
        val repo = FavoritosRepositoryImpl(dao, Gson())
        val evento = RepositorioPrueba().evento
        repo.guardar(evento)
        repo.guardar(evento.copy(title = "Nombre actualizado"))
        assertEquals(1, dao.datos.value.size)
        assertEquals("Nombre actualizado", repo.observarFavoritos().first().single().title)
    }

    @Test fun quitarNoBorraLosDemasFavoritos() = runTest {
        val repo = FavoritosRepositoryImpl(DaoPrueba(), Gson())
        val evento = RepositorioPrueba().evento
        repo.guardar(evento)
        repo.guardar(evento.copy(id = 43))
        repo.quitar(evento.id)
        assertEquals(listOf(43), repo.observarFavoritos().first().map { it.id })
    }

    @Test fun favoritosPasadosPermanecenGuardados() = runTest {
        val repo = FavoritosRepositoryImpl(DaoPrueba(), Gson())
        val pasado = RepositorioPrueba().evento.copy(startsAt = "2020-01-01T12:00:00Z")
        repo.guardar(pasado)
        assertEquals(pasado, repo.observarFavoritos().first().single())
    }

    @Test fun conservaImagenAusenteYCoordenadasIncompletas() = runTest {
        val repo = FavoritosRepositoryImpl(DaoPrueba(), Gson())
        val evento = RepositorioPrueba().evento.let {
            it.copy(imageUrl = null, venue = it.venue.copy(lng = null))
        }
        repo.guardar(evento)
        val guardado = repo.observarFavoritos().first().single()
        assertNull(guardado.imageUrl)
        assertFalse(guardado.venue.tieneCoordenadas)
        assertEquals(evento.description, guardado.description)
    }

    @Test fun obtienePorIdSoloElFavoritoSolicitado() = runTest {
        val repo = FavoritosRepositoryImpl(DaoPrueba(), Gson())
        val evento = RepositorioPrueba().evento
        repo.guardar(evento)
        repo.guardar(evento.copy(id = 43))
        assertEquals(evento, repo.obtenerFavorito(evento.id))
        assertNull(repo.obtenerFavorito(999))
    }
}
