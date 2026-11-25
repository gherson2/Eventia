package com.example.moviles_eventia.api

import com.example.moviles_eventia.session.SesionStore
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AutorizacionInterceptor @Inject constructor(
    private val almacen: SesionStore
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val admiteToken = Regex("/(?:me(?:/.*)?|events/\\d+(?:/attendance)?)$")
            .containsMatchIn(original.url.encodedPath)
        // Retrofit usa OkHttp asíncrono: este código corre en su hilo de red, no en UI.
        val token = if (admiteToken) runBlocking { almacen.sesion.first()?.token } else null
        val solicitud = original.newBuilder().header("Accept", "application/json")
            .removeHeader("Authorization").apply {
                if (token != null) header("Authorization", "Bearer $token")
            }.build()
        val respuesta = chain.proceed(solicitud)
        if (respuesta.code == 401 && token != null) {
            try {
                runBlocking { almacen.expirarSiCoincide(token) }
            } catch (e: Exception) {
                respuesta.close()
                throw IOException("No se pudo limpiar la sesión local.", e)
            }
        }
        return respuesta
    }
}
