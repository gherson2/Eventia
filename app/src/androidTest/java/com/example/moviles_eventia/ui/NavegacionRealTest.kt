package com.example.moviles_eventia.ui

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.state.ToggleableState
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.example.moviles_eventia.MainActivity
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/**
 * Prueba optativa de integración con la app y el backend configurado.
 * Solo navega y consulta: no registra cuentas, no cambia favoritos y no envía
 * inscripciones. La app puede actualizar su caché pública durante las lecturas.
 */
class NavegacionRealTest {
    @get:Rule(order = 0) val primerPlano = PrimerPlanoTestRule()
    @get:Rule(order = 1) val compose = createEmptyComposeRule()
    private val errorConexion = "No pudimos conectarnos. Revisa tu conexión e inténtalo nuevamente."
    private val vertical = hasScrollToIndexAction() and
        SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)
    private val horizontal = hasScrollToIndexAction() and
        SemanticsMatcher.keyIsDefined(SemanticsProperties.HorizontalScrollAxisRange)

    private fun requiereServidor() = assumeTrue(
        "Requiere autorización explícita para usar la API real",
        InstrumentationRegistry.getArguments().getString("eventia.liveReadOnly") == "true"
    )

    private data class TarjetaLeida(val titulo: String, val categoria: String, val lugar: String)

    private fun leerPrimeraTarjeta(): TarjetaLeida {
        val imagen = SemanticsMatcher("Imagen de un evento") { nodo ->
            nodo.config.getOrNull(SemanticsProperties.ContentDescription)
                ?.any { it.startsWith("Imagen de ") } == true
        }
        compose.waitUntil(30_000) { compose.onAllNodes(imagen).fetchSemanticsNodes().isNotEmpty() }
        val titulo = compose.onAllNodes(imagen).fetchSemanticsNodes().first()
            .config[SemanticsProperties.ContentDescription].first().removePrefix("Imagen de ")
        val textos = compose.onNodeWithText(titulo).fetchSemanticsNode()
            .config[SemanticsProperties.Text].map { it.text }
        check(textos.size >= 4) { "No se encontró la tarjeta completa del catálogo: $textos" }
        return TarjetaLeida(titulo, textos.first(), textos[textos.lastIndex - 1])
    }

    private fun esperarActualizacion() {
        compose.waitUntil(30_000) {
            compose.onAllNodes(hasContentDescription("Actualizar") and isEnabled())
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun comprobarDetalle(titulo: String) {
        fun hayLista() = compose.onAllNodes(hasScrollToIndexAction()).fetchSemanticsNodes().isNotEmpty()
        fun hayErrorConexion() = compose.onAllNodesWithText(errorConexion).fetchSemanticsNodes().isNotEmpty()

        try {
            compose.onNodeWithText("Detalle del evento").assertIsDisplayed()
            compose.waitUntil(30_000) { hayLista() || hayErrorConexion() }
            if (!hayLista() && hayErrorConexion()) {
                // La integración depende de una red real. Verificar la recuperación
                // mediante la acción de UI, sin omitir fallos persistentes ni 404.
                compose.onNodeWithText(errorConexion).assertIsDisplayed()
                android.util.Log.w("EventiaNavegacion", "Fallo de conexión visible; un reintento de UI: $titulo")
                compose.onNodeWithText("Reintentar").performClick()
                compose.waitUntil(30_000) { hayLista() }
            }
            // Las filas fuera de pantalla pueden no estar compuestas todavía.
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(titulo))
            compose.onNodeWithText(titulo).assertIsDisplayed()
        } catch (error: Throwable) {
            android.util.Log.e("EventiaNavegacion", "No se pudo verificar el detalle de: $titulo")
            compose.onRoot(useUnmergedTree = true).printToLog("EventiaNavegacion")
            throw error
        }
    }

    @Test fun catalogoDetalleYMapaConDatosReales() {
        requiereServidor()
        ActivityScenario.launch(MainActivity::class.java).use {
            val imagenEvento = SemanticsMatcher("Imagen de un evento") { nodo ->
                nodo.config.getOrNull(SemanticsProperties.ContentDescription)
                    ?.any { descripcion -> descripcion.startsWith("Imagen de ") } == true
            }
            compose.waitUntil(30_000) {
                compose.onAllNodes(imagenEvento).fetchSemanticsNodes().isNotEmpty()
            }
            val titulo = compose.onAllNodes(imagenEvento).fetchSemanticsNodes().first()
                .config[SemanticsProperties.ContentDescription].first().removePrefix("Imagen de ")
            compose.onNodeWithText(titulo).performClick()
            comprobarDetalle(titulo)
            compose.onNodeWithContentDescription("Volver").performClick()
            compose.onNodeWithText(titulo).assertIsDisplayed()

            compose.onNodeWithText("Mapa", useUnmergedTree = false).performClick()
            compose.onNodeWithText("Mapa de eventos").assertIsDisplayed()
            val elegir = hasText("Elegir evento", substring = true) and hasClickAction()
            compose.waitUntil(30_000) {
                compose.onAllNodes(elegir and isEnabled()).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNode(elegir).performScrollTo().performClick()
            val opcion = compose.onAllNodes(hasClickAction() and hasAnyAncestor(isDialog()))
                .fetchSemanticsNodes().first { nodo ->
                    nodo.config.getOrNull(SemanticsProperties.Text)?.any { texto -> texto.text != "Cerrar" } == true
                }.config[SemanticsProperties.Text].first().text
            compose.onNode(hasText(opcion) and hasAnyAncestor(isDialog())).performClick()
            compose.onNodeWithTag("resumen-mapa").assertIsDisplayed()
            compose.onNodeWithText("Ver detalle").performScrollTo().performClick()
            comprobarDetalle(opcion)
            compose.onNodeWithContentDescription("Volver").performClick()
            compose.onNodeWithTag("resumen-mapa").assertIsDisplayed()
        }
    }

    @Test fun busquedaCategoriaYRotacionConservanFiltrosYPermitenLimpiar() {
        requiereServidor()
        ActivityScenario.launch(MainActivity::class.java).use { escenario ->
            val tarjeta = leerPrimeraTarjeta()
            // El campo de búsqueda contiene el mismo título, pero no es la tarjeta.
            val resultado = hasText(tarjeta.titulo) and hasClickAction() and !hasSetTextAction()
            // Catálogo y categorías se consultan en paralelo: ver tarjetas no
            // garantiza que la fila haya recibido los chips remotos.
            compose.waitUntil(30_000) {
                compose.onNode(horizontal).fetchSemanticsNode().children.size > 1
            }
            compose.onNode(hasSetTextAction()).performTextReplacement(tarjeta.titulo)
            compose.onNode(horizontal).performScrollToNode(hasText(tarjeta.categoria))
            val chip = hasText(tarjeta.categoria) and hasAnyAncestor(horizontal) and hasClickAction()
            compose.onNode(chip).performClick()
            esperarActualizacion()
            compose.onNode(vertical).performScrollToNode(resultado)
            compose.onNode(resultado).assertIsDisplayed().performClick()
            comprobarDetalle(tarjeta.titulo)
            compose.onNodeWithContentDescription("Volver").performClick()
            compose.onNode(resultado).assertIsDisplayed()
            val orientacionAnterior = AtomicInteger()
            escenario.onActivity { orientacionAnterior.set(it.requestedOrientation) }
            try {
                escenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
                compose.waitUntil(15_000) {
                    var horizontalActual = false
                    escenario.onActivity {
                        horizontalActual = it.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                    }
                    horizontalActual
                }
                compose.onNode(vertical).performScrollToNode(hasSetTextAction())
                compose.onNode(hasSetTextAction()).assertTextContains(tarjeta.titulo)
                compose.onNode(horizontal).performScrollToNode(hasText(tarjeta.categoria))
                compose.onNode(chip).assert(SemanticsMatcher("Categoría conservada al rotar") {
                    it.config.getOrNull(SemanticsProperties.Selected) == true ||
                        it.config.getOrNull(SemanticsProperties.ToggleableState) == ToggleableState.On
                })
                compose.onNode(vertical).performScrollToNode(resultado)
                compose.onNode(resultado).assertIsDisplayed()
            } finally {
                escenario.onActivity { it.requestedOrientation = orientacionAnterior.get() }
            }
            compose.onNode(vertical).performScrollToNode(hasText("Limpiar filtros"))
            compose.onNodeWithText("Limpiar filtros").performClick()
            esperarActualizacion()
            compose.onNode(vertical).performScrollToNode(hasSetTextAction())
            org.junit.Assert.assertEquals("", compose.onNode(hasSetTextAction()).fetchSemanticsNode()
                .config[SemanticsProperties.EditableText].text)
        }
    }

    @Test fun eventoLugarEventoYRegresoConservanLaNavegacion() {
        requiereServidor()
        ActivityScenario.launch(MainActivity::class.java).use {
            val tarjeta = leerPrimeraTarjeta()
            compose.onNodeWithText(tarjeta.titulo).performClick()
            comprobarDetalle(tarjeta.titulo)
            compose.onNode(vertical).performScrollToNode(hasText(tarjeta.lugar))
            compose.onNodeWithText(tarjeta.lugar).performClick()
            compose.onNodeWithText("Detalle del lugar").assertIsDisplayed()
            esperarActualizacion()
            compose.onNode(vertical).performScrollToNode(hasText(tarjeta.titulo))
            compose.onNodeWithText(tarjeta.titulo).performClick()
            comprobarDetalle(tarjeta.titulo)
            compose.onNodeWithContentDescription("Volver").performClick()
            compose.onNodeWithText("Detalle del lugar").assertIsDisplayed()
            compose.onNodeWithText(tarjeta.titulo).assertIsDisplayed()
            compose.onNodeWithContentDescription("Volver").performClick()
            comprobarDetalle(tarjeta.titulo)
        }
    }
}
