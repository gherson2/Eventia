package com.example.moviles_eventia.api

import java.io.EOFException
import java.io.IOException
import java.net.SocketException
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Una conexión reutilizada puede cerrarse antes de entregar las cabeceras.
 * Reintenta una sola vez GET, nunca escrituras, estados HTTP ni errores de sesión.
 * Se coloca antes de autorización para volver a comprobar la sesión esperada.
 */
class ReintentoLecturaInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val solicitud = chain.request()
        try {
            return chain.proceed(solicitud)
        } catch (error: IOException) {
            val conexionCerrada = generateSequence<Throwable>(error) { it.cause }.take(4)
                .any { it is EOFException || it is SocketException }
            if (solicitud.method != "GET" || chain.call().isCanceled() || !conexionCerrada) throw error
            return chain.proceed(solicitud)
        }
    }
}
