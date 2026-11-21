
package com.example.moviles_eventia.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.moviles_eventia.BuildConfig

object RetrofitClient {

    val api: EventiaApi = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(EventiaApi::class.java)
}
