package com.example.moviles_eventia.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.moviles_eventia.models.Category
import com.example.moviles_eventia.models.Event
import com.example.moviles_eventia.models.Venue
import com.example.moviles_eventia.ui.screens.DetalleEventoScreen
import com.example.moviles_eventia.ui.screens.DetalleEventoUiState
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// Estados locales: no inscribe usuarios reales ni utiliza la sesión del dispositivo.
class InscripcionPantallaTest {
    @get:Rule(order = 0) val primerPlano = PrimerPlanoTestRule()
    @get:Rule(order = 1) val compose = createComposeRule()
    private val evento = Event(42, "Jazz", "Concierto", "2099-12-01T20:00:00-04:00",
        Category(3, "Música"), null, Venue(7, "Teatro", "Calle 7", null, null), 50, 10)

    @Test fun sinCuentaLaAccionPermiteSolicitarAcceso() {
        val acceso = AtomicBoolean(false)
        compose.setContent {
            MaterialTheme {
                DetalleEventoScreen(
                    estado = DetalleEventoUiState(evento, cargando = false, datosRecientes = true),
                    esFavorito = false, favoritoHabilitado = true, alCambiarFavorito = {},
                    alVolver = {}, alActualizar = {}, alLugar = {}, alInscripcion = { acceso.set(true) }
                )
            }
        }
        compose.onNodeWithText("Inicia sesión para inscribirte").performScrollTo().assertIsEnabled().performClick()
        assertTrue(acceso.get())
    }

    @Test fun copiaGuardadaNoPermiteEnviarUnaInscripcion() {
        compose.setContent {
            MaterialTheme {
                DetalleEventoScreen(
                    estado = DetalleEventoUiState(evento, cargando = false,
                        esCopiaGuardada = true, haySesion = true),
                    esFavorito = true, favoritoHabilitado = true, alCambiarFavorito = {},
                    alVolver = {}, alActualizar = {}, alLugar = {}, alInscripcion = {}
                )
            }
        }
        compose.onNodeWithText("Conéctate para gestionar tu inscripción")
            .performScrollTo().assertIsNotEnabled()
    }
}
