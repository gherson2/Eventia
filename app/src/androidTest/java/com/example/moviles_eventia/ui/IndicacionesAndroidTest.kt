package com.example.moviles_eventia.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import com.example.moviles_eventia.models.Venue
import com.example.moviles_eventia.ui.components.abrirIndicaciones
import org.junit.Assert.*
import org.junit.Test

// Intercepta startActivity: verifica el intent sin abrir una cuenta de Maps.
class IndicacionesAndroidTest {
    private class CapturaContexto(base: Context, val sinAplicacion: Boolean = false) : ContextWrapper(base) {
        var solicitud: Intent? = null
        override fun startActivity(intent: Intent) {
            solicitud = intent
            if (sinAplicacion) throw ActivityNotFoundException("No hay receptor de prueba")
        }
    }
    private val base get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun coordenadasValidasGeneranUnIntentGeo() {
        val contexto = CapturaContexto(base)
        abrirIndicaciones(contexto, Venue(7, "Teatro", "Calle 7", -17.39, -66.15)) { fail(it) }
        assertEquals(Intent.ACTION_VIEW, contexto.solicitud?.action)
        assertEquals("geo", contexto.solicitud?.data?.scheme)
        assertTrue(contexto.solicitud!!.data.toString().startsWith("geo:-17.39,-66.15?"))
    }

    @Test fun coordenadasInvalidasUsanDireccionSinMarcadorInventado() {
        val contexto = CapturaContexto(base)
        abrirIndicaciones(contexto, Venue(7, "Teatro", "Calle 7", Double.NaN, null)) { fail(it) }
        assertTrue(contexto.solicitud!!.data.toString().startsWith("geo:0,0?q="))
    }

    @Test fun sinAplicacionCompatibleDevuelveAvisoSinCerrar() {
        val contexto = CapturaContexto(base, sinAplicacion = true)
        var error: String? = null
        abrirIndicaciones(contexto, Venue(7, "Teatro", "Calle 7", -17.39, -66.15)) { error = it }
        assertEquals("No hay una aplicación de mapas instalada.", error)
    }

    @Test fun sinCoordenadasNiDireccionNoIntentaAbrirOtraApp() {
        val contexto = CapturaContexto(base)
        var error: String? = null
        abrirIndicaciones(contexto, Venue(7, "Teatro", null, null, null)) { error = it }
        assertNull(contexto.solicitud)
        assertNotNull(error)
    }
}
