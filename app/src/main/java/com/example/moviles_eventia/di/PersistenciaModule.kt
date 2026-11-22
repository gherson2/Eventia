package com.example.moviles_eventia.di

import android.content.Context
import androidx.room.Room
import com.example.moviles_eventia.db.EventiaDatabase
import com.example.moviles_eventia.db.dao.FavoritosDao
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
        Room.databaseBuilder(contexto, EventiaDatabase::class.java, "eventia.db").build()

    @Provides
    fun proporcionarFavoritosDao(db: EventiaDatabase): FavoritosDao = db.favoritosDao()

    @Provides
    @Singleton
    fun proporcionarGson(): Gson = Gson()
}
