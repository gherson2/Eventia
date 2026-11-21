package com.example.moviles_eventia.di

import com.example.moviles_eventia.BuildConfig
import com.example.moviles_eventia.api.EventiaApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object RedModule {
    @Provides
    @Singleton
    fun proporcionarRetrofit(): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun proporcionarEventiaApi(retrofit: Retrofit): EventiaApi =
        retrofit.create(EventiaApi::class.java)
}
