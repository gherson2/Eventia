package com.example.moviles_eventia.ui

import android.util.Log
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.example.moviles_eventia.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/**
 * ESCRIBE en el backend configurado usando la sesión que el usuario dejó abierta.
 * No forma parte del recorrido de lectura: exige autorización y título explícitos.
 * Nunca cancela una inscripción que ya existía al empezar. No registra cuentas,
 * no lee/imprime credenciales y no cambia favoritos ni ajustes del dispositivo.
 */
class InscripcionRealAutorizadaTest {
    @get:Rule(order = 0) val primerPlano = PrimerPlanoTestRule()
    @get:Rule(order = 1) val compose = createEmptyComposeRule()
    private val vertical = hasScrollToIndexAction() and
        SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)
    private val gestion = hasText("Inscribirme") or hasText("Cancelar inscripción") or
        hasText("Inicia sesión para inscribirte") or hasText("Sin cupos disponibles") or
        hasText("Procesando…") or hasText("Actualiza el detalle para continuar") or
        hasText("Conéctate para gestionar tu inscripción")

    private fun existe(selector: SemanticsMatcher) =
        compose.onAllNodes(selector).fetchSemanticsNodes().isNotEmpty()

    private fun esperarActualizacion() {
        compose.waitUntil(30_000) {
            existe(hasContentDescription("Actualizar") and isEnabled())
        }
    }

    private fun desplazar(selector: SemanticsMatcher) {
        compose.onNode(vertical).performScrollToNode(selector)
    }

    private fun abrirPestana(nombre: String) {
        compose.onNode(hasText(nombre) and hasClickAction()).performClick()
    }

    private fun perfilActualizado(reintentoPermitido: Boolean = true): Int {
        abrirPestana("Perfil")
        esperarActualizacion()
        // No imprimir el árbol: contiene nombre/correo de la cuenta autorizada.
        check(existe(vertical)) {
            "No hay una sesión recuperada en Perfil; no se enviaron escrituras."
        }
        desplazar(hasText("Cerrar sesión"))
        compose.onNodeWithText("Cerrar sesión").assertIsDisplayed()
        compose.onNodeWithContentDescription("Actualizar").performClick()
        esperarActualizacion()
        desplazar(hasText("Mis inscripciones"))
        val proximas = SemanticsMatcher("Pestaña Próximas con cantidad") { nodo ->
            nodo.config.getOrNull(SemanticsProperties.Text)?.any {
                Regex("""Próximas \(\d+\)""").matches(it.text)
            } == true
        }
        desplazar(proximas)
        val nodo = compose.onNode(proximas).fetchSemanticsNode()
        val texto = nodo.config[SemanticsProperties.Text].first {
            Regex("""Próximas \(\d+\)""").matches(it.text)
        }.text
        compose.onNode(proximas).performClick()
        if (existe(hasText("Reintentar"))) {
            val erroresConocidos = listOf(
                "El recurso solicitado ya no está disponible.",
                "El servidor no está disponible. Inténtalo nuevamente.",
                "No se pudo completar la solicitud. Inténtalo nuevamente.",
                "No pudimos conectarnos. Revisa tu conexión e inténtalo nuevamente.",
                "No se pudieron cargar los datos. Inténtalo nuevamente.",
                "No pudimos verificar la sesión."
            ).filter { existe(hasText(it)) }
            if (reintentoPermitido && erroresConocidos == listOf(
                    "No pudimos conectarnos. Revisa tu conexión e inténtalo nuevamente."
                )) {
                Log.w("EventiaInscripcionReal", "Perfil mostró conexión fallida; un único reintento de lectura")
                return perfilActualizado(reintentoPermitido = false)
            }
            error("Perfil mostró un error de consulta: $erroresConocidos")
        }
        return Regex("""\d+""").find(texto)!!.value.toInt()
    }

    private fun abrirDesdeCatalogo(titulo: String) {
        abrirPestana("Explorar")
        compose.onNodeWithText("Eventos").performClick()
        compose.waitUntil(30_000) { existe(vertical) }
        desplazar(hasSetTextAction())
        compose.onNode(hasSetTextAction()).performTextReplacement(titulo)
        val tarjeta = hasText(titulo) and hasClickAction() and !hasSetTextAction()
        compose.waitUntil(30_000) { existe(tarjeta) }
        desplazar(tarjeta)
        compose.onNode(tarjeta).performClick()
        esperarDetalle(titulo)
    }

    private fun esperarDetalle(titulo: String) {
        compose.onNodeWithText("Detalle del evento").assertIsDisplayed()
        compose.waitUntil(30_000) { existe(vertical) }
        esperarActualizacion()
        desplazar(hasText(titulo))
        compose.onNodeWithText(titulo).assertIsDisplayed()
        desplazar(gestion)
    }

    private fun cupos(): Int {
        val etiqueta = SemanticsMatcher("Cupos disponibles del servidor") { nodo ->
            nodo.config.getOrNull(SemanticsProperties.Text)?.any {
                it.text.startsWith("Cupos disponibles: ")
            } == true
        }
        desplazar(etiqueta)
        val texto = compose.onNode(etiqueta).fetchSemanticsNode()
            .config[SemanticsProperties.Text].first { it.text.startsWith("Cupos disponibles: ") }.text
        return texto.substringAfter(": ").toInt()
    }

    private fun esperarConfirmacion(texto: String) {
        compose.waitUntil(30_000) {
            existe(hasText(texto)) && existe(hasContentDescription("Actualizar") and isEnabled())
        }
        compose.onNodeWithText(texto).assertIsDisplayed()
    }

    private fun restaurarSiHaceFalta(titulo: String) {
        if (!existe(hasText("Detalle del evento"))) abrirDesdeCatalogo(titulo)
        esperarActualizacion()
        // Una escritura con respuesta perdida puede haberse procesado:
        // reconciliar por GET antes de decidir si hace falta cancelar.
        compose.onNodeWithContentDescription("Actualizar").performClick()
        esperarDetalle(titulo)
        if (existe(hasText("Cancelar inscripción") and isEnabled())) {
            compose.onNodeWithText("Cancelar inscripción").performClick()
            esperarConfirmacion("Inscripción cancelada.")
        }
        compose.onNodeWithText("Inscribirme").assertIsEnabled()
        Log.i("EventiaInscripcionReal", "Estado inicial restaurado: $titulo")
    }

    @Test fun sesionPersistidaInscripcionPerfilCancelacionYRegreso() {
        val argumentos = InstrumentationRegistry.getArguments()
        assumeTrue("Exige autorización de escritura independiente",
            argumentos.getString("eventia.liveWrites") == "true")
        val titulo = checkNotNull(argumentos.getString("eventia.eventTitle")?.takeIf { it.isNotBlank() }) {
            "Indica el título exacto del único evento autorizado."
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            val cantidadInicial = perfilActualizado()
            Log.i("EventiaInscripcionReal", "Sesión recuperada y Perfil consultado; próximas=$cantidadInicial")
            abrirDesdeCatalogo(titulo)
            // Fallar ANTES de escribir si ya estaba inscrito, no tiene cupos o no hay sesión.
            compose.onNodeWithText("Inscribirme").assertIsEnabled()
            val cuposIniciales = cupos()
            check(cuposIniciales > 0) { "El evento no tiene cupos." }
            var escrituraIntentada = false
            var cancelacionConfirmada = false
            var fallo: Throwable? = null
            try {
                desplazar(hasText("Inscribirme"))
                escrituraIntentada = true
                compose.onNodeWithText("Inscribirme").performClick()
                esperarConfirmacion("Inscripción confirmada.")
                compose.onNodeWithText("Cancelar inscripción").assertIsEnabled()
                val cuposInscrito = cupos()
                assertEquals("Cupos después de inscribir", cuposIniciales - 1, cuposInscrito)
                Log.i("EventiaInscripcionReal", "Inscripción confirmada: $titulo; cupos=$cuposInscrito")
                compose.onNodeWithContentDescription("Volver").performClick()
                assertEquals("Perfil debe incorporar la inscripción", cantidadInicial + 1, perfilActualizado())
                val tarjeta = hasText(titulo) and hasClickAction()
                desplazar(tarjeta)
                compose.onNode(tarjeta).performClick()
                esperarDetalle(titulo)
                compose.onNodeWithText("Cancelar inscripción").assertIsEnabled().performClick()
                esperarConfirmacion("Inscripción cancelada.")
                cancelacionConfirmada = true
                assertEquals("Cupos después de cancelar", cuposIniciales, cupos())
                compose.onNodeWithContentDescription("Volver").performClick()
                assertEquals("Perfil debe retirar solo la inscripción de prueba",
                    cantidadInicial, perfilActualizado())
                Log.i("EventiaInscripcionReal", "Cancelación y Perfil restaurados: $titulo; cupos=$cuposIniciales")
            } catch (error: Throwable) {
                fallo = error
                throw error
            } finally {
                if (escrituraIntentada && !cancelacionConfirmada) {
                    try {
                        restaurarSiHaceFalta(titulo)
                    } catch (limpieza: Throwable) {
                        Log.e("EventiaInscripcionReal", "RESTAURACION_PENDIENTE: $titulo")
                        if (fallo != null) fallo.addSuppressed(limpieza) else throw limpieza
                    }
                }
            }
        }
    }
}
