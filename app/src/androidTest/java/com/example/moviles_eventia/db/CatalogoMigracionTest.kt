package com.example.moviles_eventia.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.moviles_eventia.db.entities.CacheCatalogoEntity
import com.example.moviles_eventia.models.Category
import com.example.moviles_eventia.models.Event
import com.example.moviles_eventia.models.Venue
import com.google.gson.Gson
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CatalogoMigracionTest {
    @Test fun migrarConservaFavoritosYLaCacheSobreviveALaReapertura() = runBlocking {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val nombre = "test-migracion-${UUID.randomUUID()}.db"
        val evento = Event(42, "Jazz", "Concierto", "2099-12-01T20:00:00-04:00",
            Category(3, "Música"), null, Venue(7, "Teatro", "Calle 7", null, null), 50, 10)
        val gson = Gson()
        var db: EventiaDatabase? = null
        try {
            // Esquema real de la versión 1, en un archivo aislado.
            contexto.openOrCreateDatabase(nombre, 0, null).use { anterior ->
                anterior.execSQL(
                    "CREATE TABLE favoritos (id INTEGER NOT NULL, json TEXT NOT NULL, PRIMARY KEY(id))"
                )
                anterior.execSQL("INSERT INTO favoritos(id, json) VALUES(?, ?)",
                    arrayOf<Any>(evento.id, gson.toJson(evento)))
                anterior.version = 1
            }
            val migrada = Room.databaseBuilder(contexto, EventiaDatabase::class.java, nombre)
                .addMigrations(EventiaDatabase.MIGRACION_1_2).build()
            db = migrada
            assertEquals(gson.toJson(evento), migrada.favoritosDao().obtenerPorId(evento.id)?.json)
            val copia = CacheCatalogoEntity("catalogo", gson.toJson(listOf(evento)), 123L)
            migrada.cacheCatalogoDao().guardar(copia)
            migrada.close()

            val reabierta = Room.databaseBuilder(contexto, EventiaDatabase::class.java, nombre)
                .addMigrations(EventiaDatabase.MIGRACION_1_2).build()
            db = reabierta
            assertEquals(copia, reabierta.cacheCatalogoDao().obtener("catalogo"))
            assertNotNull(reabierta.favoritosDao().obtenerPorId(evento.id))
        } finally {
            db?.close()
            contexto.deleteDatabase(nombre)
        }
    }
}
