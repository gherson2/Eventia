package com.example.moviles_eventia.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.moviles_eventia.models.Category
import com.example.moviles_eventia.models.Event
import com.example.moviles_eventia.models.Usuario
import com.example.moviles_eventia.models.Venue
import com.example.moviles_eventia.ui.screens.CuentaUiState
import com.example.moviles_eventia.ui.screens.MisInscripcionesUiState
import com.example.moviles_eventia.ui.screens.PerfilScreen
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

// Composables aislados con datos ficticios; no se consulta la cuenta del teléfono.
class MisInscripcionesPantallaTest {
    @get:Rule val compose = createComposeRule()
    private val usuario = Usuario(7, "Ana", "ana@example.test")
    private val evento = Event(42, "Jazz futuro", "Concierto", "2099-12-01T20:00:00Z",
        Category(3, "Música"), null, Venue(7, "Teatro", "Calle 7", null, null), 50, 10)

    @Test fun separaPestanasYPermiteAbrirElDetalle() {
        val abierto = AtomicInteger(0)
        compose.setContent {
            MaterialTheme {
                PerfilScreen(
                    estado = CuentaUiState(usuario, leyendoSesion = false),
                    inscripciones = MisInscripcionesUiState(usuarioId = 7, cargado = true,
                        eventos = listOf(evento, evento.copy(id = 43, title = "Jazz pasado",
                            startsAt = "2020-01-01T12:00:00Z"))),
                    alIngresar = {}, alRegistrar = {}, alActualizar = {}, alCerrar = {},
                    alEvento = { abierto.set(it) }
                )
            }
        }
        compose.onNodeWithText("Próximas (1)").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Jazz futuro").performScrollTo().performClick()
        assertEquals(42, abierto.get())
        compose.onNodeWithText("Pasadas (1)").performScrollTo().performClick()
        compose.onNodeWithText("Jazz pasado").performScrollTo().assertIsDisplayed()
    }

    @Test fun noMuestraLaListaDeOtraCuenta() {
        compose.setContent {
            MaterialTheme {
                PerfilScreen(
                    estado = CuentaUiState(usuario.copy(id = 8), leyendoSesion = false),
                    inscripciones = MisInscripcionesUiState(usuarioId = 7, cargado = true,
                        eventos = listOf(evento)),
                    alIngresar = {}, alRegistrar = {}, alActualizar = {}, alCerrar = {}, alEvento = {}
                )
            }
        }
        compose.onNodeWithText("Jazz futuro").assertDoesNotExist()
    }

    @Test fun errorDeConsultaNoDiceQueLaListaEstaVacia() {
        compose.setContent {
            MaterialTheme {
                PerfilScreen(
                    estado = CuentaUiState(usuario, leyendoSesion = false),
                    inscripciones = MisInscripcionesUiState(usuarioId = 7, error = "Sin conexión"),
                    alIngresar = {}, alRegistrar = {}, alActualizar = {}, alCerrar = {}, alEvento = {}
                )
            }
        }
        compose.onNodeWithText("Sin conexión").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("No tienes inscripciones próximas.").assertDoesNotExist()
    }
}
