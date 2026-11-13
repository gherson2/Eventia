package com.example.moviles_eventia

import com.example.moviles_eventia.api.RetrofitClient
import com.example.moviles_eventia.repositories.EventRepository
import com.example.moviles_eventia.repositories.EventRepositoryImpl

object AppContainer {

    val eventRepository: EventRepository =
        EventRepositoryImpl(RetrofitClient.api)

}
