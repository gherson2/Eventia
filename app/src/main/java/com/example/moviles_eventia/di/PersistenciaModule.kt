package com.example.moviles_eventia.di

import android.content.Context
import androidx.room.Room
import com.example.moviles_eventia.db.EventiaDatabase
import com.example.moviles_eventia.db.dao.FavoritosDao
import com.example.moviles_eventia.db.dao.CacheCatalogoDao
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PersistenciaModule {
    @Provides
    @Singleton
    fun proporcionarBaseDeDatos(@ApplicationContext contexto: Context): EventiaDatabase =
        Room.databaseBuilder(contexto, EventiaDatabase::class.java, "eventia.db")
            .addMigrations(EventiaDatabase.MIGRACION_1_2)
            .build()

    @Provides
    fun proporcionarFavoritosDao(db: EventiaDatabase): FavoritosDao = db.favoritosDao()

    @Provides
    fun proporcionarCacheCatalogoDao(db: EventiaDatabase): CacheCatalogoDao = db.cacheCatalogoDao()

    @Provides
    @Singleton
    fun proporcionarGson(): Gson = Gson()
}
