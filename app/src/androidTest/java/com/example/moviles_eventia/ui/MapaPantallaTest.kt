package com.example.moviles_eventia.ui

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.example.moviles_eventia.models.Category
import com.example.moviles_eventia.models.Event
import com.example.moviles_eventia.models.Venue
import com.example.moviles_eventia.ui.screens.ExplorarUiState
import com.example.moviles_eventia.ui.screens.FavoritosUiState
import com.example.moviles_eventia.ui.screens.MapaScreen
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.util.GeoPoint

// Datos ficticios y teselas deshabilitadas: no usa cuentas ni descarga mapas.
class MapaPantallaTest {
    @get:Rule(order = 0) val primerPlano = PrimerPlanoTestRule()
    @get:Rule(order = 1) val compose = createComposeRule()
    private val evento = Event(42, "Jazz futuro", null, "2099-12-01T20:00:00Z",
        Category(3, "Música"), null, Venue(7, "Teatro", null, -17.39, -66.15), 50, 10)
    private val estado = ExplorarUiState(eventos = listOf(evento),
        categorias = listOf(evento.category), cargando = false, datosDisponibles = true)

    private fun mostrar(datos: ExplorarUiState = estado, alEvento: (Int) -> Unit = {}) {
        compose.setContent {
            MaterialTheme {
                MapaScreen(datos, FavoritosUiState(cargando = false),
                    {}, {}, {}, {}, alEvento, {}, cargarTeselas = false)
            }
        }
    }

    private fun mapa(): MapView {
        val actividad = ActivityLifecycleMonitorRegistry.getInstance()
            .getActivitiesInStage(Stage.RESUMED).single() as Activity
        fun encontrar(vista: View): MapView? {
            if (vista is MapView) return vista
            if (vista is ViewGroup) for (i in 0 until vista.childCount) {
                encontrar(vista.getChildAt(i))?.let { return it }
            }
            return null
        }
        return requireNotNull(encontrar(actividad.window.decorView))
    }

    @Test fun elegirEventoMuestraResumenYAbreSuDetalle() {
        val abierto = AtomicInteger()
        mostrar(alEvento = { abierto.set(it) })
        compose.onNodeWithText("Elegir evento (1)").performScrollTo().performClick()
        compose.onNodeWithText("Jazz futuro").performClick()
        compose.onNodeWithTag("resumen-mapa").assertIsDisplayed()
        compose.onNodeWithText("Ver detalle").performScrollTo().performClick()
        assertEquals(42, abierto.get())
    }

    @Test fun tocarMarcadorNativoAbreElResumen() {
        mostrar()
        var posicion = Offset.Zero
        compose.runOnIdle {
            val pixel = mapa().projection.toPixels(GeoPoint(-17.39, -66.15), null)
            // El ancla está en la base del icono: tocar unos píxeles por encima.
            posicion = Offset(pixel.x.toFloat(), pixel.y - 10f)
        }
        compose.onNodeWithTag("mapa-eventos").performTouchInput { click(posicion) }
        // osmdroid confirma el toque después del intervalo de doble toque;
        // ese Handler nativo no avanza con el reloj virtual de Compose.
        compose.waitUntil(5_000) {
            compose.onNodeWithTag("resumen-mapa").isDisplayed()
        }
        compose.onNodeWithTag("resumen-mapa").assertIsDisplayed()
        compose.onNodeWithText("Jazz futuro").assertIsDisplayed()
    }

    @Test fun marcadoresAgrupadosNoIncluyenCoordenadasInvalidas() {
        mostrar(estado.copy(eventos = listOf(evento, evento.copy(id = 43),
            evento.copy(id = 44, venue = evento.venue.copy(lat = null)))))
        compose.waitForIdle()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val marcadores = mapa().overlays.filterIsInstance<Marker>()
            assertEquals(1, marcadores.size)
            assertEquals("2 eventos aquí", marcadores.single().title)
        }
        compose.onNodeWithText("Elegir evento (2)").performScrollTo().performClick()
        compose.onAllNodesWithText("Jazz futuro")[0].performClick()
        compose.onNodeWithText("Otro evento aquí (2)").performScrollTo().assertIsDisplayed()
    }

    @Test fun cambiarCategoriaRetiraMarcadoresYResumenAnterior() {
        val datos = mutableStateOf(estado)
        compose.setContent {
            MaterialTheme {
                MapaScreen(datos.value, FavoritosUiState(cargando = false),
                    { datos.value = datos.value.copy(categoriaId = it) }, {}, {}, {}, {}, {},
                    cargarTeselas = false)
            }
        }
        compose.onNodeWithText("Elegir evento (1)").performScrollTo().performClick()
        compose.onNodeWithText("Jazz futuro").performClick()
        compose.runOnIdle { datos.value = estado.copy(categoriaId = 99) }
        compose.onNodeWithTag("resumen-mapa").assertDoesNotExist()
        compose.runOnIdle { assertTrue(mapa().overlays.filterIsInstance<Marker>().isEmpty()) }
    }

    @Test fun restauraSeleccionTrasRecrearEstadoGuardable() {
        val restauracion = StateRestorationTester(compose)
        restauracion.setContent {
            MaterialTheme {
                MapaScreen(estado, FavoritosUiState(cargando = false),
                    {}, {}, {}, {}, {}, {}, cargarTeselas = false)
            }
        }
        compose.onNodeWithText("Elegir evento (1)").performScrollTo().performClick()
        compose.onNodeWithText("Jazz futuro").performClick()
        restauracion.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("resumen-mapa").assertIsDisplayed()
        compose.onNodeWithText("Jazz futuro").assertIsDisplayed()
    }

    @Test fun sinCoordenadasMuestraVacioSinCerrarLaPantalla() {
        mostrar(estado.copy(eventos = listOf(evento.copy(venue = evento.venue.copy(lng = null)))))
        compose.onNodeWithText("Sin eventos con ubicación").assertIsDisplayed()
        compose.onNodeWithText("Elegir evento (0)").performScrollTo().assertIsNotEnabled()
    }
}
