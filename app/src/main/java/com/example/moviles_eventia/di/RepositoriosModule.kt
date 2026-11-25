package com.example.moviles_eventia.di

import com.example.moviles_eventia.repositories.EventRepository
import com.example.moviles_eventia.repositories.EventRepositoryImpl
import com.example.moviles_eventia.repositories.FavoritosRepository
import com.example.moviles_eventia.repositories.FavoritosRepositoryImpl
import com.example.moviles_eventia.repositories.ConectividadRepository
import com.example.moviles_eventia.repositories.ConectividadRepositoryImpl
import com.example.moviles_eventia.repositories.CuentaRepository
import com.example.moviles_eventia.repositories.CuentaRepositoryImpl
import com.example.moviles_eventia.session.SesionStore
import com.example.moviles_eventia.session.DataStoreSesion
import com.example.moviles_eventia.repositories.InscripcionRepository
import com.example.moviles_eventia.repositories.InscripcionRepositoryImpl
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
    abstract fun vincularInscripcionRepository(implementacion: InscripcionRepositoryImpl): InscripcionRepository

    @Binds
    @Singleton
    abstract fun vincularSesion(implementacion: DataStoreSesion): SesionStore

    @Binds
    @Singleton
    abstract fun vincularCuentaRepository(implementacion: CuentaRepositoryImpl): CuentaRepository
    @Binds
    @Singleton
    abstract fun vincularEventRepository(implementacion: EventRepositoryImpl): EventRepository

    @Binds
    @Singleton
    abstract fun vincularFavoritosRepository(implementacion: FavoritosRepositoryImpl): FavoritosRepository

    @Binds
    @Singleton
    abstract fun vincularConectividadRepository(implementacion: ConectividadRepositoryImpl): ConectividadRepository
}
