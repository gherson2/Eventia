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
import okhttp3.OkHttpClient
import com.example.moviles_eventia.api.AutorizacionInterceptor

@Module
@InstallIn(SingletonComponent::class)
object RedModule {
    @Provides
    @Singleton
    fun proporcionarCliente(interceptor: AutorizacionInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .retryOnConnectionFailure(false)
            .addInterceptor(interceptor)
            .build()

    @Provides
    @Singleton
    fun proporcionarRetrofit(cliente: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(cliente)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun proporcionarEventiaApi(retrofit: Retrofit): EventiaApi =
        retrofit.create(EventiaApi::class.java)
}
