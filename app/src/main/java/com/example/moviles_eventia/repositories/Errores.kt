package com.example.moviles_eventia.repositories

import java.io.IOException
import retrofit2.HttpException

fun Throwable.mensajeParaUsuario(): String = when (this) {
    is HttpException -> when (code()) {
        404 -> "El recurso solicitado ya no está disponible."
        in 500..599 -> "El servidor no está disponible. Inténtalo nuevamente."
        else -> "No se pudo completar la solicitud. Inténtalo nuevamente."
    }
    is IOException -> "No pudimos conectarnos. Revisa tu conexión e inténtalo nuevamente."
    else -> "No se pudieron cargar los datos. Inténtalo nuevamente."
}
