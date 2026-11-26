package com.example.moviles_eventia.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.moviles_eventia.ui.screens.AccesoScreen
import com.example.moviles_eventia.ui.screens.AccesoUiState
import com.example.moviles_eventia.ui.screens.CuentaUiState
import com.example.moviles_eventia.ui.screens.PerfilScreen
import com.example.moviles_eventia.ui.screens.MisInscripcionesUiState
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// Solo composables con estados ficticios: no consulta API ni cambia la sesión real.
class CuentaPantallasTest {
    @get:Rule(order = 0) val primerPlano = PrimerPlanoTestRule()
    @get:Rule(order = 1) val compose = createComposeRule()

    @Test fun accesoMuestraErrorDeCampoYPermiteContinuarSinCuenta() {
        val volver = AtomicBoolean(false)
        compose.setContent {
            MaterialTheme {
                AccesoScreen(
                    estado = AccesoUiState(campos = mapOf("email" to "Correo ya registrado")),
                    alNombre = {}, alCorreo = {}, alClave = {}, alCambiarModo = {},
                    alEnviar = {}, alVolver = { volver.set(true) }
                )
            }
        }
        compose.onNodeWithText("Correo ya registrado").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Continuar sin cuenta").performScrollTo().performClick()
        assertTrue(volver.get())
    }

    @Test fun perfilSinSesionOfreceRegistroSinExigirCuentaParaExplorar() {
        val registrar = AtomicBoolean(false)
        compose.setContent {
            MaterialTheme {
                PerfilScreen(
                    estado = CuentaUiState(leyendoSesion = false),
                    inscripciones = MisInscripcionesUiState(),
                    alIngresar = {}, alRegistrar = { registrar.set(true) },
                    alActualizar = {}, alCerrar = {}, alEvento = {}
                )
            }
        }
        compose.onNodeWithText("Estás explorando sin cuenta").assertIsDisplayed()
        compose.onNodeWithText("Crear cuenta").performScrollTo().performClick()
        assertTrue(registrar.get())
    }
}
