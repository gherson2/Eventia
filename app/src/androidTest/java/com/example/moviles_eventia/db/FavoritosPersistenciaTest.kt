package com.example.moviles_eventia.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.moviles_eventia.models.Category
import com.example.moviles_eventia.models.Event
import com.example.moviles_eventia.models.Venue
import com.example.moviles_eventia.repositories.FavoritosRepositoryImpl
import com.google.gson.Gson
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FavoritosPersistenciaTest {
    @Test fun favoritoSobreviveACerrarYReabrirRoom() = runBlocking {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        // Base aislada de prueba: nunca se abre ni se borra eventia.db.
        val nombre = "test-favoritos-${UUID.randomUUID()}.db"
        var db: EventiaDatabase? = null
        try {
            val primera = Room.databaseBuilder(contexto, EventiaDatabase::class.java, nombre).build()
            db = primera
            val repo = FavoritosRepositoryImpl(primera.favoritosDao(), Gson())
            val evento = Event(42, "Jazz", "Concierto", "2099-12-01T20:00:00-04:00",
                Category(3, "Música"), null, Venue(7, "Teatro", "Calle 7", null, null),
                50, 10, "Descripción completa")
            repo.guardar(evento)
            primera.close()

            val segunda = Room.databaseBuilder(contexto, EventiaDatabase::class.java, nombre).build()
            db = segunda
            val reabierto = FavoritosRepositoryImpl(segunda.favoritosDao(), Gson())
            assertEquals(evento, reabierto.observarFavoritos().first().single())
            assertEquals(evento, reabierto.obtenerFavorito(evento.id))
            assertNull(reabierto.obtenerFavorito(999))
            reabierto.quitar(evento.id)
            assertTrue(reabierto.observarFavoritos().first().isEmpty())
        } finally {
            db?.close()
            contexto.deleteDatabase(nombre)
        }
    }
}
