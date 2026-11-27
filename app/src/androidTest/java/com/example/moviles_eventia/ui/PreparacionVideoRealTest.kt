package com.example.moviles_eventia.ui

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.util.Log
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.example.moviles_eventia.MainActivity
import java.io.File
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/**
 * Dos fases optativas. Entre ellas el orquestador activa modo avión y fuerza
 * el cierre de la app. Solo lee favoritos existentes: no los crea ni elimina.
 * La segunda fase restaura avión desactivado/Wi-Fi activo, su estado inicial.
 */
class PreparacionVideoRealTest {
    @get:Rule(order = 0) val primerPlano = PrimerPlanoTestRule()
    @get:Rule(order = 1) val compose = createEmptyComposeRule()
    private val instrumentacion get() = InstrumentationRegistry.getInstrumentation()
    private val archivo get() = File(instrumentacion.targetContext.cacheDir, "video-verificacion-favorito.txt")
    private val vertical = hasScrollToIndexAction() and
        SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)
    private val copia = hasText("Copia guardada en este dispositivo. Los datos y cupos pueden haber cambiado.")
    private val sinRed = hasText("Sin conexión. Puedes consultar los datos guardados en este dispositivo.")
    private val cache = hasText("Datos guardados · hace ", substring = true)
    private val imagen = SemanticsMatcher("Imagen de evento") {
        it.config.getOrNull(SemanticsProperties.ContentDescription)?.any { texto ->
            texto.startsWith("Imagen de ")
        } == true
    }

    private fun existe(selector: SemanticsMatcher) =
        compose.onAllNodes(selector).fetchSemanticsNodes().isNotEmpty()

    private fun shell(comando: String): String =
        instrumentacion.uiAutomation.executeShellCommand(comando).use { descriptor ->
            android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { it.readText() }
        }

    private fun fase(nombre: String) = assumeTrue(
        "Solo se ejecuta con la fase explícita de preparación del video",
        InstrumentationRegistry.getArguments().getString("eventia.videoPhase") == nombre
    )

    private fun tab(nombre: String) {
        compose.onNode(hasText(nombre) and hasClickAction()).performClick()
    }

    private fun desplazar(selector: SemanticsMatcher) {
        compose.onNode(vertical).performScrollToNode(selector)
    }

    private fun esperarCarga() {
        compose.waitUntil(30_000) {
            existe(hasContentDescription("Actualizar") and isEnabled())
        }
    }

    private fun tituloPrimero(): String {
        compose.waitUntil(30_000) { existe(imagen) }
        return compose.onAllNodes(imagen).fetchSemanticsNodes().first()
            .config[SemanticsProperties.ContentDescription].first { it.startsWith("Imagen de ") }
            .removePrefix("Imagen de ")
    }

    private fun detalle(titulo: String) {
        compose.onNodeWithText("Detalle del evento").assertIsDisplayed()
        compose.waitUntil(30_000) { existe(vertical) ||
            existe(hasText("No pudimos conectarnos. Revisa tu conexión e inténtalo nuevamente.")) }
        if (!existe(vertical)) {
            compose.onNodeWithText("Reintentar").performClick()
            compose.waitUntil(30_000) { existe(vertical) }
        }
        esperarCarga()
        desplazar(hasText(titulo))
        compose.onNodeWithText(titulo).assertIsDisplayed()
    }

    private fun rotarYRestaurar(escenario: ActivityScenario<MainActivity>, comprobar: () -> Unit) {
        var anterior = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        escenario.onActivity { anterior = it.requestedOrientation }
        try {
            escenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
            compose.waitUntil(15_000) {
                var horizontal = false
                escenario.onActivity {
                    horizontal = it.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                }
                horizontal
            }
            comprobar()
        } finally {
            escenario.onActivity { it.requestedOrientation = anterior }
        }
        compose.waitForIdle()
    }

    @Test fun prepararOnlineFavoritoExistenteVacioYRotaciones() {
        fase("online")
        ActivityScenario.launch(MainActivity::class.java).use { escenario ->
            tituloPrimero()
            esperarCarga()
            compose.onNode(hasSetTextAction()).performTextReplacement("zz_eventia_sin_coincidencias_93817")
            compose.waitUntil(30_000) { existe(hasText("Sin coincidencias")) }
            compose.onNodeWithText("Sin coincidencias").assertIsDisplayed()
            // Con búsqueda hay dos acciones Limpiar filtros; usar la del encabezado.
            compose.onAllNodesWithText("Limpiar filtros").onFirst().performClick()
            tituloPrimero()
            esperarCarga()
            tab("Favoritos")
            val titulo = tituloPrimero()
            archivo.writeText(titulo)
            compose.onNodeWithText(titulo).performClick()
            detalle(titulo)
            rotarYRestaurar(escenario) { detalle(titulo) }
            compose.onNodeWithContentDescription("Volver").performClick()
            tab("Mapa")
            compose.onNodeWithText("Mapa de eventos").assertIsDisplayed()
            val elegir = hasText("Elegir evento", substring = true) and hasClickAction()
            compose.waitUntil(30_000) { existe(elegir and isEnabled()) }
            compose.onNode(elegir).performScrollTo().performClick()
            val opcion = compose.onAllNodes(hasClickAction() and hasAnyAncestor(isDialog()))
                .fetchSemanticsNodes().first { nodo ->
                    nodo.config.getOrNull(SemanticsProperties.Text)?.any { it.text != "Cerrar" } == true
                }.config[SemanticsProperties.Text].first().text
            compose.onNode(hasText(opcion) and hasAnyAncestor(isDialog())).performClick()
            compose.onNodeWithTag("resumen-mapa").assertIsDisplayed()
            rotarYRestaurar(escenario) {
                compose.onNodeWithTag("resumen-mapa").assertIsDisplayed()
                compose.onNodeWithText(opcion).assertIsDisplayed()
            }
            Log.i("EventiaVideo", "ONLINE_OK: búsqueda vacía, detalle y mapa rotados; favorito existente=$titulo")
        }
    }

    @Test fun reabrirOfflineRecuperarConexionYAbrirMapsDesdeBoton() {
        fase("offline")
        check(archivo.exists()) { "Ejecuta antes la preparación online." }
        check(shell("settings get global airplane_mode_on").trim() == "1") {
            "Esta fase requiere modo avión real activado por el orquestador."
        }
        val titulo = archivo.readText()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitUntil(30_000) { existe(sinRed) }
            compose.onNode(sinRed).assertIsDisplayed()
            desplazar(cache)
            compose.onNode(cache).assertIsDisplayed()
            desplazar(imagen)
            tituloPrimero()
            tab("Favoritos")
            compose.waitUntil(10_000) { existe(imagen) }
            desplazar(hasText(titulo))
            compose.onNodeWithText(titulo).assertIsDisplayed().performClick()
            detalle(titulo)
            desplazar(copia)
            compose.onNode(copia).assertIsDisplayed()
            desplazar(hasText("Conéctate para gestionar tu inscripción"))
            compose.onNodeWithText("Conéctate para gestionar tu inscripción").assertIsNotEnabled()
            Log.i("EventiaVideo", "OFFLINE_OK: tras cierre forzado, caché, favorito y detalle local; escrituras bloqueadas")

            shell("cmd connectivity airplane-mode disable")
            shell("svc wifi enable")
            compose.waitUntil(60_000) { !existe(sinRed) }
            compose.onNodeWithContentDescription("Actualizar").performClick()
            esperarCarga()
            detalle(titulo)
            // La marca está cerca de la imagen: comprobar ausencia con la lista al inicio.
            compose.onNode(vertical).performScrollToIndex(0)
            compose.onNode(copia).assertDoesNotExist()
            Log.i("EventiaVideo", "RECUPERACION_DETALLE_OK: callback retira aviso y GET retira copia guardada")
            compose.onNodeWithContentDescription("Volver").performClick()
            tab("Explorar")
            desplazar(cache)
            compose.onNode(cache).assertIsDisplayed()
            compose.onNodeWithContentDescription("Actualizar").performClick()
            esperarCarga()
            compose.onNode(vertical).performScrollToIndex(0)
            compose.onNode(cache).assertDoesNotExist()
            tituloPrimero()
            Log.i("EventiaVideo", "RECUPERACION_CATALOGO_OK: actualización remota retira marca de caché")

            // Evento conocido con dirección válida; solo consulta, nunca inscripción.
            desplazar(hasSetTextAction())
            val tituloMaps = "Meetup de desarrolladores Android"
            compose.onNode(hasSetTextAction()).performTextReplacement(tituloMaps)
            val tarjeta = hasText(tituloMaps) and hasClickAction() and !hasSetTextAction()
            compose.waitUntil(30_000) { existe(tarjeta) }
            desplazar(tarjeta)
            compose.onNode(tarjeta).performClick()
            detalle(tituloMaps)
            desplazar(hasText("Cómo llegar"))
            compose.onNodeWithText("Cómo llegar").performClick()
            val actividad = shell("dumpsys activity activities")
            check(actividad.lineSequence().any { linea ->
                linea.contains("topResumedActivity") && linea.contains("com.google.android.apps.maps")
            }) { "El botón no dejó Google Maps como actividad reanudada." }
            Log.i("EventiaVideo", "MAPS_BOTON_OK: botón real dejó Google Maps en primer plano")
            shell("am start -W --activity-reorder-to-front -n com.example.moviles_eventia/.MainActivity")
            archivo.delete()
        }
    }
}
