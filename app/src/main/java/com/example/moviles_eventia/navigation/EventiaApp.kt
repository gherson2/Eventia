
package com.example.moviles_eventia.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.moviles_eventia.ui.screens.ExplorarScreen
import com.example.moviles_eventia.ui.screens.ExplorarViewModel
import com.example.moviles_eventia.ui.screens.LugaresViewModel
import com.example.moviles_eventia.ui.screens.DetalleEventoViewModel
import com.example.moviles_eventia.ui.screens.DetalleEventoScreen
import com.example.moviles_eventia.ui.screens.DetalleLugarViewModel
import com.example.moviles_eventia.ui.screens.DetalleLugarScreen
import com.example.moviles_eventia.ui.screens.FavoritosViewModel
import com.example.moviles_eventia.ui.screens.FavoritosScreen
import com.example.moviles_eventia.models.Event
import com.example.moviles_eventia.ui.screens.ConectividadViewModel
import com.example.moviles_eventia.ui.screens.CuentaViewModel
import com.example.moviles_eventia.ui.screens.PerfilScreen
import com.example.moviles_eventia.ui.screens.AccesoViewModel
import com.example.moviles_eventia.ui.screens.AccesoScreen
import com.example.moviles_eventia.ui.screens.MisInscripcionesViewModel
import com.example.moviles_eventia.ui.screens.MapaScreen
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

private enum class Pantalla(
    val ruta: String,
    val titulo: String,
    val simbolo: String
) {
    EXPLORAR("explorar", "Explorar", "⌕"),
    MAPA("mapa", "Mapa", "⌖"),
    FAVORITOS("favoritos", "Favoritos", "♥"),
    PERFIL("perfil", "Perfil", "●")
}

@Composable
fun EventiaApp() {

    val navController = rememberNavController()
    // Explorar y Mapa comparten catálogo, búsqueda y categoría.
    val catalogoVm: ExplorarViewModel = hiltViewModel()
    val catalogo by catalogoVm.uiState.collectAsStateWithLifecycle()
    // Un único observador de Room comparte el estado entre todos los destinos.
    val favoritosVm: FavoritosViewModel = hiltViewModel()
    val favoritos by favoritosVm.uiState.collectAsStateWithLifecycle()
    val conectividadVm: ConectividadViewModel = hiltViewModel()
    val conectado by conectividadVm.conectado.collectAsStateWithLifecycle()
    val cuentaVm: CuentaViewModel = hiltViewModel()
    val cuenta by cuentaVm.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val alcance = rememberCoroutineScope()
    val cambiarFavorito: (Event) -> Unit = { evento ->
        favoritosVm.cambiarFavorito(evento, guardar = favoritos.eventos.none { it.id == evento.id })
    }

    LaunchedEffect(favoritosVm, snackbar) {
        favoritosVm.mensajes.collect { mensaje ->
            val resultado = snackbar.showSnackbar(
                message = mensaje.texto,
                actionLabel = if (mensaje.quitado != null) "Deshacer" else null,
                duration = SnackbarDuration.Long,
                withDismissAction = true
            )
            if (resultado == SnackbarResult.ActionPerformed) {
                mensaje.quitado?.let(favoritosVm::restaurar)
            }
        }
    }

    LaunchedEffect(cuentaVm, navController) {
        cuentaVm.expiraciones.collect {
            if (navController.currentDestination?.route?.startsWith("acceso") != true) {
                navController.navigate("acceso") { launchSingleTop = true }
            }
            alcance.launch { snackbar.showSnackbar("Tu sesión expiró. Inicia sesión nuevamente.") }
        }
    }

    val entradaActual by navController.currentBackStackEntryAsState()
    val rutaActual = entradaActual?.destination?.route

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (Pantalla.entries.any { it.ruta == rutaActual }) NavigationBar {
                Pantalla.entries.forEach { pantalla ->

                    NavigationBarItem(
                        selected = rutaActual == pantalla.ruta,

                        onClick = {
                            navController.navigate(pantalla.ruta) {
                                popUpTo(
                                    navController.graph.findStartDestination().id
                                ) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },

                        icon = {
                            Text(pantalla.simbolo)
                        },

                        label = {
                            Text(pantalla.titulo)
                        }
                    )
                }
            }
        }
    ) { innerPadding ->

        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            if (conectado == false) {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(
                        "Sin conexión. Puedes consultar los datos guardados en este dispositivo.",
                        Modifier.fillMaxWidth().padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            NavHost(
                navController = navController,
                startDestination = Pantalla.EXPLORAR.ruta,
                modifier = Modifier.weight(1f)
            ) {
                // PANTALLA EXPLORAR
                composable(Pantalla.EXPLORAR.ruta) {
                    val lugaresViewModel: LugaresViewModel = hiltViewModel()
                    val lugares by lugaresViewModel.uiState.collectAsStateWithLifecycle()

                    ExplorarScreen(
                        estado = catalogo,
                        lugares = lugares,
                        favoritos = favoritos,
                        alCambiarFavorito = cambiarFavorito,
                        alBuscar = catalogoVm::cambiarBusqueda,
                        alFiltrar = catalogoVm::cambiarCategoria,
                        alLimpiar = catalogoVm::limpiarFiltros,
                        alActualizar = catalogoVm::obtenerEventos,
                        alCargarCategorias = catalogoVm::obtenerCategorias,
                        alActualizarLugares = lugaresViewModel::cargar,
                        alEvento = { navController.navigate("evento/$it") },
                        alLugar = { navController.navigate("lugar/$it") }
                    )
                }

                composable(
                    route = "evento/{eventoId}",
                    arguments = listOf(navArgument("eventoId") { type = NavType.IntType })
                ) {
                    val vm: DetalleEventoViewModel = hiltViewModel()
                    val estado by vm.uiState.collectAsStateWithLifecycle()
                    LaunchedEffect(vm, navController) {
                        vm.solicitarAcceso.collect {
                            navController.navigate("acceso") { launchSingleTop = true }
                        }
                    }
                    DetalleEventoScreen(
                        estado = estado,
                        esFavorito = favoritos.eventos.any { it.id == estado.evento?.id },
                        favoritoHabilitado = !favoritos.cargando && !favoritos.guardando && favoritos.error == null,
                        alCambiarFavorito = cambiarFavorito,
                        alVolver = { navController.popBackStack() },
                        alActualizar = vm::cargar,
                        alInscripcion = vm::cambiarInscripcion,
                        alLugar = { navController.navigate("lugar/$it") }
                    )
                }

                composable(
                    route = "lugar/{lugarId}",
                    arguments = listOf(navArgument("lugarId") { type = NavType.IntType })
                ) {
                    val vm: DetalleLugarViewModel = hiltViewModel()
                    val estado by vm.uiState.collectAsStateWithLifecycle()
                    DetalleLugarScreen(
                        estado = estado,
                        favoritos = favoritos,
                        alCambiarFavorito = cambiarFavorito,
                        alVolver = { navController.popBackStack() },
                        alActualizar = vm::cargar,
                        alEvento = { navController.navigate("evento/$it") }
                    )
                }

                // PANTALLA MAPA
                composable(Pantalla.MAPA.ruta) {
                    MapaScreen(
                        estado = catalogo, favoritos = favoritos,
                        alFiltrar = catalogoVm::cambiarCategoria,
                        alLimpiar = catalogoVm::limpiarFiltros,
                        alActualizar = catalogoVm::obtenerEventos,
                        alCargarCategorias = catalogoVm::obtenerCategorias,
                        alEvento = { navController.navigate("evento/$it") },
                        alCambiarFavorito = cambiarFavorito,
                        cargarTeselas = conectado != false
                    )
                }

                // PANTALLA FAVORITOS
                composable(Pantalla.FAVORITOS.ruta) {
                    FavoritosScreen(
                        estado = favoritos,
                        alReintentar = favoritosVm::cargar,
                        alEvento = { navController.navigate("evento/$it") },
                        alQuitar = { favoritosVm.cambiarFavorito(it, guardar = false) }
                    )
                }

                // PANTALLA PERFIL
                composable(Pantalla.PERFIL.ruta) {
                    val misVm: MisInscripcionesViewModel = hiltViewModel()
                    val misInscripciones by misVm.uiState.collectAsStateWithLifecycle()
                    LifecycleResumeEffect(misVm) {
                        misVm.reanudar()
                        onPauseOrDispose { misVm.pausar() }
                    }
                    PerfilScreen(
                        estado = cuenta,
                        inscripciones = misInscripciones,
                        alIngresar = { navController.navigate("acceso") },
                        alRegistrar = { navController.navigate("acceso?registro=true") },
                        alActualizar = { cuentaVm.actualizar(); misVm.cargar() },
                        alCerrar = cuentaVm::cerrarSesion,
                        alEvento = { navController.navigate("evento/$it") }
                    )
                }

                composable(
                    route = "acceso?registro={registro}",
                    arguments = listOf(navArgument("registro") {
                        type = NavType.BoolType
                        defaultValue = false
                    })
                ) {
                    val vm: AccesoViewModel = hiltViewModel()
                    val estado by vm.uiState.collectAsStateWithLifecycle()
                    LaunchedEffect(estado.autenticado) {
                        if (estado.autenticado) navController.popBackStack()
                    }
                    AccesoScreen(
                        estado = estado,
                        alNombre = vm::cambiarNombre,
                        alCorreo = vm::cambiarCorreo,
                        alClave = vm::cambiarClave,
                        alCambiarModo = vm::cambiarModo,
                        alEnviar = vm::enviar,
                        alVolver = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
