package com.example.moviles_eventia.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.example.moviles_eventia.MainActivity
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
        assumeTrue(
            "Requiere autorización explícita para usar la API real",
            InstrumentationRegistry.getArguments().getString("eventia.liveReadOnly") == "true"
        )
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
}
