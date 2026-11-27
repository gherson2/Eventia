package com.example.moviles_eventia.repositories

import com.example.moviles_eventia.BuildConfig
import java.io.IOException
import java.util.logging.Logger
import retrofit2.HttpException

/** Diagnóstico debug sin mensajes/cuerpos, identidad de usuario ni credenciales. */
internal fun registrarFalloLectura(ruta: String, error: Exception) {
    if (!BuildConfig.DEBUG) return
    val tipos = generateSequence<Throwable>(error) { it.cause }.take(4)
        .joinToString(" -> ") { it.javaClass.simpleName }
    val estado = (error as? HttpException)?.code()?.toString() ?: "sin respuesta HTTP"
    Logger.getLogger("EventiaLecturas").warning("$ruta: $tipos; $estado")
}

fun Throwable.mensajeParaUsuario(): String = when (this) {
    is HttpException -> when (code()) {
        404 -> "El recurso solicitado ya no está disponible."
        in 500..599 -> "El servidor no está disponible. Inténtalo nuevamente."
        else -> "No se pudo completar la solicitud. Inténtalo nuevamente."
    }
    is IOException -> "No pudimos conectarnos. Revisa tu conexión e inténtalo nuevamente."
    else -> "No se pudieron cargar los datos. Inténtalo nuevamente."
}
