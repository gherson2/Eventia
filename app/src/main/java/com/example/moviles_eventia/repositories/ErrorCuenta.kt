package com.example.moviles_eventia.repositories

import com.google.gson.JsonParser
import retrofit2.HttpException

class ErrorCuenta(
    override val message: String,
    val campos: Map<String, String> = emptyMap()
) : Exception(message)

fun HttpException.errorCuenta(): ErrorCuenta {
    val contenido = runCatching {
        JsonParser.parseString(response()?.errorBody()?.string().orEmpty()).asJsonObject
    }.getOrNull()
    val mensaje = runCatching { contenido?.get("message")?.asString }.getOrNull()
    val campos = runCatching {
        contenido?.getAsJsonObject("fields")?.entrySet()?.mapNotNull { (clave, valor) ->
            if (clave !in setOf("name", "email", "password")) null
            else {
                val texto = if (valor.isJsonArray) valor.asJsonArray.firstOrNull()?.asString else valor.asString
                texto?.let { clave to it }
            }
        }?.toMap().orEmpty()
    }.getOrDefault(emptyMap())
    val alternativa = when (code()) {
        401 -> "Correo o contraseña incorrectos."
        400, 422 -> "Revisa los datos del formulario."
        else -> mensajeParaUsuario()
    }
    return ErrorCuenta(mensaje?.takeIf { it.isNotBlank() } ?: alternativa, campos)
}
