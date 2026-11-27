package com.example.moviles_eventia.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.moviles_eventia.models.Category
import com.example.moviles_eventia.models.Event
import com.example.moviles_eventia.models.Usuario
import com.example.moviles_eventia.models.Venue
import com.example.moviles_eventia.ui.screens.*
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

// Ventana de prueba 320x480 dp y fuente 160%; no cambia ajustes del teléfono.
class AdaptacionPantallasTest {
    @get:Rule(order = 0) val primerPlano = PrimerPlanoTestRule()
    @get:Rule(order = 1) val compose = createComposeRule()
    private val evento = Event(42, "Evento con título extenso para pantalla pequeña",
        "Resumen de varias líneas para comprobar el desplazamiento", "2099-12-01T20:00:00Z",
        Category(3, "Música"), null, Venue(7, "Teatro de prueba", "Calle 7", null, null),
        50, 10, "Descripción extensa. ".repeat(100))
    private val lista = hasScrollToIndexAction()

    @Composable private fun Reducida(contenido: @Composable () -> Unit) {
        val densidad = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(densidad.density, 1.6f)) {
            MaterialTheme { Box(Modifier.requiredSize(320.dp, 480.dp)) { contenido() } }
        }
    }

    @Test fun catalogoConAvisosPermiteLlegarYEntrarAlUltimoEvento() {
        val abierto = AtomicInteger()
        compose.setContent {
            Reducida {
                ExplorarScreen(
                    ExplorarUiState(eventos = listOf(evento, evento.copy(id = 43, title = "Último evento")),
                        categorias = listOf(evento.category), cargando = false,
                        error = "Actualización fallida", errorCategorias = "Categorías sin actualizar",
                        copiaGuardadaEn = System.currentTimeMillis(), datosDisponibles = true),
                    LugaresUiState(cargando = false), FavoritosUiState(cargando = false),
                    {}, {}, {}, {}, {}, {}, {}, { abierto.set(it) }, {}
                )
            }
        }
        compose.onNode(lista and SemanticsMatcher.keyIsDefined(
            androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange))
            .performScrollToNode(hasText("Último evento"))
        compose.onNodeWithText("Último evento").assertIsDisplayed().performClick()
        assertEquals(43, abierto.get())
    }

    @Test fun detalleConTextoExtensoPermiteAlcanzarIndicacionesYFavoritos() {
        compose.setContent {
            Reducida {
                DetalleEventoScreen(DetalleEventoUiState(evento, cargando = false, datosRecientes = true),
                    false, true, {}, {}, {}, {}, {})
            }
        }
        compose.onNode(lista).performScrollToNode(hasText("Cómo llegar"))
        compose.onNodeWithText("Cómo llegar").assertIsDisplayed().assertHasClickAction()
        compose.onNode(lista).performScrollToNode(hasText("Guardar en favoritos"))
        compose.onNodeWithText("Guardar en favoritos").assertIsDisplayed().assertIsEnabled()
    }

    @Test fun perfilConFuenteGrandePermiteLeerInscripcionesYAccederAlDetalle() {
        val abierto = AtomicInteger()
        compose.setContent {
            Reducida {
                PerfilScreen(CuentaUiState(Usuario(7, "Nombre extenso de prueba", "ana@example.test"),
                    leyendoSesion = false),
                    MisInscripcionesUiState(usuarioId = 7, eventos = listOf(evento), cargado = true),
                    {}, {}, {}, {}, { abierto.set(it) })
            }
        }
        compose.onNode(lista).performScrollToNode(hasText(evento.title))
        compose.onNodeWithText(evento.title).assertIsDisplayed().performClick()
        assertEquals(42, abierto.get())
    }
}
