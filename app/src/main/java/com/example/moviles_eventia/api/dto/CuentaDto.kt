package com.example.moviles_eventia.api.dto

data class LoginRequest(val email: String, val password: String) {
    override fun toString() = "LoginRequest(email=$email, password=***)"
}
data class RegistroRequest(val name: String, val email: String, val password: String) {
    override fun toString() = "RegistroRequest(name=$name, email=$email, password=***)"
}
data class UsuarioDto(val id: Int, val name: String?, val email: String?)
data class AutenticacionDto(val token: String?, val user: UsuarioDto?)
