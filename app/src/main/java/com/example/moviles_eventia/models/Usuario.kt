package com.example.moviles_eventia.models

data class Usuario(val id: Int, val nombre: String, val correo: String)

data class Sesion(val token: String, val usuario: Usuario) {
    override fun toString() = "Sesion(usuario=$usuario, token=***)"
}

fun validarCuenta(nombre: String, correo: String, clave: String, registro: Boolean): Map<String, String> =
    buildMap {
        if (registro && nombre.isBlank()) put("name", "Ingresa tu nombre")
        if (!Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(correo.trim())) {
            put("email", "Ingresa un correo válido")
        }
        if (clave.length < 8) put("password", "La contraseña debe tener al menos 8 caracteres")
    }
