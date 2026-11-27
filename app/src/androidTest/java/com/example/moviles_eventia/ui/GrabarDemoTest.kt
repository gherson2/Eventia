package com.example.moviles_eventia.ui

import android.os.SystemClock
import android.util.Log
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
 * Grabación optativa de pantallas REALES, sin introducir credenciales.
 * No se ejecuta con liveReadOnly: exige eventia.recordDemo=true.
 * La fase cuenta exige además liveWrites y cancela solo su inscripción de demo.
 * La fase offline necesita orquestador que restaure avión/Wi-Fi en finally.
 */
class GrabarDemoTest {
    @get:Rule(order = 0) val primerPlano = PrimerPlanoTestRule()
    @get:Rule(order = 1) val compose = createEmptyComposeRule()
    private val instrumentacion get() = InstrumentationRegistry.getInstrumentation()
    private val vertical = hasScrollToIndexAction() and
        SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)
    private val horizontal = hasScrollToIndexAction() and
        SemanticsMatcher.keyIsDefined(SemanticsProperties.HorizontalScrollAxisRange)
    private val titulo = "Meetup de desarrolladores Android"
    private val lugar = "Centro de Convenciones del Sur"
    private val sinRed = hasText("Sin conexión. Puedes consultar los datos guardados en este dispositivo.")
    private var inicio = 0L
    private var clip = ""

    private fun shell(comando: String): String =
        instrumentacion.uiAutomation.executeShellCommand(comando).use { descriptor ->
            android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { it.readText() }
        }

    private fun habilitada() = assumeTrue("Grabación requiere autorización explícita",
        InstrumentationRegistry.getArguments().getString("eventia.recordDemo") == "true")

    private fun existe(selector: SemanticsMatcher) =
        compose.onAllNodes(selector).fetchSemanticsNodes().isNotEmpty()

    private fun pausa(segundos: Long) = SystemClock.sleep(segundos * 1_000)
    private fun marca(nombre: String) {
        Log.i("EventiaGrabacion", "$clip|${SystemClock.elapsedRealtime() - inicio}|$nombre")
    }
    private fun tab(nombre: String) = compose.onNode(hasText(nombre) and hasClickAction()).performClick()
    private fun scroll(selector: SemanticsMatcher) = compose.onNode(vertical).performScrollToNode(selector)

    private fun carga() {
        compose.waitUntil(30_000) { existe(hasContentDescription("Actualizar") and isEnabled()) }
    }

    private fun detalle(nombre: String = titulo) {
        for (intento in 0..3) {
            compose.waitUntil(30_000) { existe(vertical) || existe(hasText("Reintentar")) }
            if (existe(vertical)) break
            if (intento == 3) error("Lectura persistentemente fallida; no usar la toma como éxito.")
            carga()
            marca("REINTENTO_LECTURA_UI")
            compose.onNodeWithText("Reintentar").performClick()
            carga()
        }
        carga()
        scroll(hasText(nombre))
        compose.onNodeWithText(nombre).assertIsDisplayed()
    }

    private fun abrirEvento() {
        tab("Explorar")
        compose.onNodeWithText("Eventos").performClick()
        scroll(hasSetTextAction())
        compose.onNode(hasSetTextAction()).performTextReplacement(titulo)
        val tarjeta = hasText(titulo) and hasClickAction() and !hasSetTextAction()
        compose.waitUntil(30_000) { existe(tarjeta) }
        scroll(tarjeta)
        compose.onNode(tarjeta).performClick()
        detalle()
    }

    private fun refrescarGestion(etiqueta: String) {
        val gestion = hasText("Inscribirme") or hasText("Cancelar inscripción") or
            hasText("Actualiza el detalle para continuar")
        repeat(4) {
            compose.onNodeWithContentDescription("Actualizar").performClick()
            carga()
            detalle()
            scroll(gestion)
            if (existe(hasText(etiqueta) and isEnabled())) return
            marca("REINTENTO_LECTURA_UI")
        }
        error("No hay datos recientes para $etiqueta; no enviar una escritura.")
    }

    private fun grabar(nombre: String, segundos: Long, acciones: () -> Unit) {
        check(shell("pidof screenrecord").isBlank()) { "Hay otra grabación activa; no se interrumpe." }
        clip = nombre
        val ruta = "/data/local/tmp/eventia-demo-$nombre.mp4"
        inicio = SystemClock.elapsedRealtime()
        val salida = instrumentacion.uiAutomation.executeShellCommand(
            "screenrecord --size 720x1600 --bit-rate 4000000 --time-limit 180 $ruta"
        )
        try {
            pausa(1) // Inicio del codificador; después son pausas de lectura del video.
            marca("INICIO")
            acciones()
            val falta = inicio + segundos * 1_000 - SystemClock.elapsedRealtime()
            if (falta > 0) SystemClock.sleep(falta)
            marca("FIN")
        } finally {
            val pid = shell("pidof screenrecord").trim()
            if (pid.matches(Regex("\\d+"))) shell("kill -2 $pid")
            android.os.ParcelFileDescriptor.AutoCloseInputStream(salida).bufferedReader().use { it.readText() }
            check((shell("stat -c %s $ruta").trim().toLongOrNull() ?: 0) > 1_024) {
                "El grabador no produjo un MP4 válido."
            }
        }
    }

    @Test fun catalogoDetalleYLugar() {
        habilitada()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitUntil(30_000) { existe(hasText(titulo) and hasClickAction()) || existe(vertical) }
            carga()
            grabar("01-catalogo", 62) {
                marca("CATALOGO")
                pausa(5)
                compose.onNode(hasSetTextAction()).performTextReplacement("Android")
                val tarjeta = hasText(titulo) and hasClickAction() and !hasSetTextAction()
                compose.waitUntil(30_000) { existe(tarjeta) }
                marca("BUSQUEDA")
                pausa(4)
                compose.waitUntil(30_000) {
                    compose.onNode(horizontal).fetchSemanticsNode().children.size > 1
                }
                compose.onNode(horizontal).performScrollToNode(hasText("Tecnología"))
                compose.onNode(hasText("Tecnología") and hasAnyAncestor(horizontal) and hasClickAction()).performClick()
                carga()
                marca("CATEGORIA")
                pausa(4)
                compose.onNodeWithText("Limpiar filtros").performClick()
                carga()
                marca("LIMPIAR")
                pausa(3)
                scroll(tarjeta)
                compose.onNode(tarjeta).performClick()
                detalle()
                marca("DETALLE")
                pausa(5)
                scroll(hasText("Cupos disponibles:", substring = true))
                pausa(4)
                scroll(hasText("Descripción"))
                marca("DESCRIPCION")
                pausa(4)
                scroll(hasText(lugar))
                compose.onNodeWithText(lugar).performClick()
                compose.waitUntil(30_000) { existe(vertical) }
                carga()
                compose.onNode(vertical).performScrollToIndex(1)
                marca("LUGAR")
                pausa(5)
                scroll(hasText("Próximos eventos aquí"))
                pausa(5)
            }
        }
    }

    @Test fun mapaEIndicaciones() {
        habilitada()
        ActivityScenario.launch(MainActivity::class.java).use {
            tab("Mapa")
            val elegir = hasText("Elegir evento", substring = true) and hasClickAction()
            compose.waitUntil(30_000) { existe(elegir and isEnabled()) }
            carga()
            pausa(3)
            grabar("02-mapa", 48) {
                marca("MAPA")
                pausa(6)
                compose.onNode(elegir).performScrollTo().performClick()
                marca("LISTA_ACCESIBLE")
                pausa(4)
                compose.onNode(hasScrollToIndexAction() and hasAnyAncestor(isDialog()))
                    .performScrollToNode(hasText(titulo))
                compose.onNode(hasText(titulo) and hasAnyAncestor(isDialog())).performClick()
                compose.onNodeWithTag("resumen-mapa").assertIsDisplayed()
                marca("RESUMEN")
                pausa(7)
                compose.onNodeWithText("Ver detalle").performScrollTo().performClick()
                detalle()
                pausa(4)
                scroll(hasText("Cómo llegar"))
                marca("INDICACIONES")
                pausa(4)
                compose.onNodeWithText("Cómo llegar").performClick()
                compose.waitUntil(15_000) {
                    shell("dumpsys activity activities").lineSequence().any {
                        it.contains("topResumedActivity") && it.contains("com.google.android.apps.maps")
                    }
                }
                marca("MAPS")
                pausa(7)
                shell("am start -W --activity-reorder-to-front -n com.example.moviles_eventia/.MainActivity")
                marca("REGRESO")
                pausa(3)
            }
        }
    }

    @Test fun favoritosOfflineYRecuperacion() {
        habilitada()
        check(shell("settings get global airplane_mode_on").trim() == "1")
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitUntil(30_000) { existe(sinRed) }
            grabar("03-offline", 52) {
                scroll(hasText("Datos guardados · hace ", substring = true))
                marca("CACHE_OFFLINE")
                pausa(6)
                tab("Favoritos")
                val imagen = SemanticsMatcher("Favorito existente") { nodo ->
                    nodo.config.getOrNull(SemanticsProperties.ContentDescription)?.any {
                        it.startsWith("Imagen de ")
                    } == true
                }
                compose.waitUntil(10_000) { existe(imagen) }
                val favorito = compose.onAllNodes(imagen).fetchSemanticsNodes().first()
                    .config[SemanticsProperties.ContentDescription].first { it.startsWith("Imagen de ") }
                    .removePrefix("Imagen de ")
                marca("FAVORITOS")
                pausa(6)
                compose.onNodeWithText(favorito).performClick()
                detalle(favorito)
                scroll(hasText("Copia guardada en este dispositivo.", substring = true))
                marca("DETALLE_LOCAL")
                pausa(6)
                scroll(hasText("Conéctate para gestionar tu inscripción"))
                compose.onNodeWithText("Conéctate para gestionar tu inscripción").assertIsNotEnabled()
                pausa(4)
                shell("cmd connectivity airplane-mode disable")
                shell("svc wifi enable")
                compose.waitUntil(60_000) { !existe(sinRed) }
                marca("INTERNET_RECUPERADO")
                compose.onNodeWithContentDescription("Actualizar").performClick()
                carga()
                detalle(favorito)
                compose.onNode(vertical).performScrollToIndex(0)
                compose.onNodeWithText("Copia guardada en este dispositivo.", substring = true).assertDoesNotExist()
                marca("DETALLE_ACTUALIZADO")
                pausa(5)
                compose.onNodeWithContentDescription("Volver").performClick()
                tab("Explorar")
                compose.onNodeWithContentDescription("Actualizar").performClick()
                carga()
                compose.onNode(vertical).performScrollToIndex(0)
                marca("CATALOGO_ACTUALIZADO")
                pausa(4)
            }
        }
    }

    private fun perfil() {
        marca("PRIVACIDAD_INICIO")
        tab("Perfil")
        carga()
        // Solo la lista: no hacer pausas sobre nombre o correo.
        compose.onNode(vertical).performScrollToIndex(1)
        if (existe(hasText("Reintentar"))) {
            compose.onNodeWithContentDescription("Actualizar").performClick()
            carga()
            compose.onNode(vertical).performScrollToIndex(1)
        }
        check(!existe(hasText("Reintentar"))) { "Perfil no se recuperó; no presentar como éxito." }
        marca("PERFIL")
    }

    @Test fun cuentaInscripcionYCancelacion() {
        habilitada()
        assumeTrue("La escena de inscripción necesita permiso independiente",
            InstrumentationRegistry.getArguments().getString("eventia.liveWrites") == "true")
        ActivityScenario.launch(MainActivity::class.java).use {
            perfil()
            val proximas = SemanticsMatcher("Cantidad de inscripciones próximas") { nodo ->
                nodo.config.getOrNull(SemanticsProperties.Text)?.any {
                    Regex("""Próximas \(\d+\)""").matches(it.text)
                } == true
            }
            val cantidadInicial = compose.onNode(proximas).fetchSemanticsNode()
                .config[SemanticsProperties.Text].first { Regex("""Próximas \(\d+\)""").matches(it.text) }.text
            abrirEvento()
            scroll(hasText("Inscribirme"))
            compose.onNodeWithText("Inscribirme").assertIsEnabled()
            var intentada = false
            var cancelada = false
            try {
                grabar("04-cuenta", 55) {
                    marca("ANTES_INSCRIPCION")
                    scroll(hasText("Cupos disponibles:", substring = true))
                    pausa(6)
                    // Reconciliar justo antes de escribir; nunca reenviar un POST
                    // fallido sin consultar primero su posible resultado.
                    refrescarGestion("Inscribirme")
                    compose.onNodeWithText("Inscribirme").assertIsEnabled()
                    intentada = true
                    compose.onNodeWithText("Inscribirme").performClick()
                    compose.waitUntil(30_000) { existe(hasText("Inscripción confirmada.")) }
                    compose.onNodeWithText("Cancelar inscripción").assertIsEnabled()
                    marca("INSCRITO")
                    pausa(7)
                    compose.onNodeWithContentDescription("Volver").performClick()
                    perfil()
                    val tarjeta = hasText(titulo) and hasClickAction()
                    scroll(tarjeta)
                    compose.onNode(tarjeta).assertIsDisplayed()
                    pausa(7)
                    compose.onNode(tarjeta).performClick()
                    detalle()
                    marca("PRIVACIDAD_FIN")
                    scroll(hasText("Cancelar inscripción"))
                    pausa(4)
                    refrescarGestion("Cancelar inscripción")
                    compose.onNodeWithText("Cancelar inscripción").performClick()
                    compose.waitUntil(30_000) { existe(hasText("Inscripción cancelada.")) }
                    cancelada = true
                    marca("CANCELADO")
                    pausa(6)
                    compose.onNodeWithContentDescription("Volver").performClick()
                    perfil()
                    compose.onNodeWithText(cantidadInicial).assertIsDisplayed()
                    marca("PERFIL_RESTAURADO")
                    pausa(5)
                }
            } finally {
                if (intentada && !cancelada) {
                    if (!existe(hasText("Detalle del evento"))) abrirEvento()
                    compose.onNodeWithContentDescription("Actualizar").performClick()
                    carga()
                    detalle()
                    val gestion = hasText("Cancelar inscripción") or hasText("Inscribirme")
                    scroll(gestion)
                    if (existe(hasText("Cancelar inscripción") and isEnabled())) {
                        compose.onNodeWithText("Cancelar inscripción").performClick()
                        compose.waitUntil(30_000) { existe(hasText("Inscripción cancelada.")) }
                    }
                    compose.onNodeWithText("Inscribirme").assertIsEnabled()
                    Log.i("EventiaGrabacion", "INSCRIPCION_RESTAURADA")
                }
            }
        }
    }
}
