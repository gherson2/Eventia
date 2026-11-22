package com.example.moviles_eventia.di

import com.example.moviles_eventia.repositories.EventRepository
import com.example.moviles_eventia.repositories.EventRepositoryImpl
import com.example.moviles_eventia.repositories.FavoritosRepository
import com.example.moviles_eventia.repositories.FavoritosRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoriosModule {
    @Binds
    @Singleton
    abstract fun vincularEventRepository(implementacion: EventRepositoryImpl): EventRepository

    @Binds
    @Singleton
    abstract fun vincularFavoritosRepository(implementacion: FavoritosRepositoryImpl): FavoritosRepository
}
