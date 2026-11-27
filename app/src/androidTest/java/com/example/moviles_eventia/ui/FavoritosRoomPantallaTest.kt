package com.example.moviles_eventia.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.platform.app.InstrumentationRegistry
import com.example.moviles_eventia.integration.EntornoAndroidPrueba
import com.example.moviles_eventia.ui.screens.FavoritosScreen
import com.example.moviles_eventia.ui.screens.FavoritosViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class FavoritosRoomPantallaTest {
    @get:Rule(order = 0) val primerPlano = PrimerPlanoTestRule()
    @get:Rule(order = 1) val compose = createComposeRule()
    private lateinit var entorno: EntornoAndroidPrueba
    private val store = ViewModelStore()
    @Before fun preparar() { entorno = EntornoAndroidPrueba() }
    @After fun terminar() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { store.clear() }
        entorno.close()
    }

    @Test fun quitarYDeshacerConSnackbarRealNoNecesitaRed() {
        entorno.conectado.value = false
        runBlocking { entorno.favoritos().guardar(entorno.evento) }
        lateinit var vm: FavoritosViewModel
        compose.runOnUiThread {
            vm = FavoritosViewModel(entorno.favoritos()).also { store.put("favoritos", it) }
        }
        compose.setContent {
            MaterialTheme {
                val estado by vm.uiState.collectAsStateWithLifecycle()
                val snackbar = remember { SnackbarHostState() }
                LaunchedEffect(vm) {
                    vm.mensajes.collect { mensaje ->
                        val resultado = snackbar.showSnackbar(mensaje.texto,
                            actionLabel = "Deshacer", duration = SnackbarDuration.Long)
                        if (resultado == SnackbarResult.ActionPerformed) mensaje.quitado?.let(vm::restaurar)
                    }
                }
                Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
                    Box(Modifier.padding(padding)) {
                        FavoritosScreen(estado, vm::cargar, {}, { vm.cambiarFavorito(it, false) })
                    }
                }
            }
        }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText(entorno.evento.title).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("Quitar ${entorno.evento.title} de favoritos").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Deshacer").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Deshacer").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText(entorno.evento.title).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(entorno.evento.title).assertIsDisplayed()
        assertEquals(0, entorno.servidor.requestCount)
    }
}
