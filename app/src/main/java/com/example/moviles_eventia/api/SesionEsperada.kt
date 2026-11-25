package com.example.moviles_eventia.api

// Tag local de Retrofit/OkHttp: no se envía como cabecera ni se serializa.
data class SesionEsperada(val token: String) {
    override fun toString() = "SesionEsperada(token=***)"
}
