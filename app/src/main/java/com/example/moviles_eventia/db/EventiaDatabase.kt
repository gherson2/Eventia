package com.example.moviles_eventia.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.moviles_eventia.db.dao.FavoritosDao
import com.example.moviles_eventia.db.entities.FavoritoEntity

@Database(entities = [FavoritoEntity::class], version = 1, exportSchema = true)
abstract class EventiaDatabase : RoomDatabase() {
    abstract fun favoritosDao(): FavoritosDao
}
