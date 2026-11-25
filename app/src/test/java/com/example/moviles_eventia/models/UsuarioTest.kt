package com.example.moviles_eventia.models

import org.junit.Assert.*
import org.junit.Test

class UsuarioTest {
    @Test fun registroExigeNombreCorreoValidoYClaveDeOchoCaracteres() {
        val errores = validarCuenta(" ", "correo-invalido", "1234567", true)
        assertEquals(setOf("name", "email", "password"), errores.keys)
    }
    @Test fun loginNoExigeNombreYNoRecortaLaContrasena() {
        assertTrue(validarCuenta("", " ana@example.test ", "12345678", false).isEmpty())
    }
    @Test fun correoConEspaciosInternosNoEsValido() {
        assertTrue(validarCuenta("Ana", "a na@example.test", "12345678", true).containsKey("email"))
    }
}
