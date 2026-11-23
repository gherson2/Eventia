package com.example.moviles_eventia.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.moviles_eventia.db.dao.FavoritosDao
import com.example.moviles_eventia.db.entities.FavoritoEntity
import com.example.moviles_eventia.db.entities.CacheCatalogoEntity
import com.example.moviles_eventia.db.dao.CacheCatalogoDao
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [FavoritoEntity::class, CacheCatalogoEntity::class], version = 2, exportSchema = true)
abstract class EventiaDatabase : RoomDatabase() {
    abstract fun favoritosDao(): FavoritosDao
    abstract fun cacheCatalogoDao(): CacheCatalogoDao

    companion object {
        val MIGRACION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `cache_catalogo` " +
                        "(`clave` TEXT NOT NULL, `json` TEXT NOT NULL, `guardadoEn` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`clave`))"
                )
            }
        }
    }
}
