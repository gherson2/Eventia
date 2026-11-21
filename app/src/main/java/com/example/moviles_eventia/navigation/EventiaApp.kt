
package com.example.moviles_eventia.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.moviles_eventia.ui.screens.ExplorarScreen
import com.example.moviles_eventia.ui.screens.ExplorarViewModel
import com.example.moviles_eventia.AppContainer
import com.example.moviles_eventia.ui.screens.LugaresViewModel
import com.example.moviles_eventia.ui.screens.DetalleEventoViewModel
import com.example.moviles_eventia.ui.screens.DetalleEventoScreen
import com.example.moviles_eventia.ui.screens.DetalleLugarViewModel
import com.example.moviles_eventia.ui.screens.DetalleLugarScreen

// Inyección manual por ahora; Hilt queda para una siguiente etapa.
private val fabricaViewModels = viewModelFactory {
    initializer { ExplorarViewModel(AppContainer.eventRepository, createSavedStateHandle()) }
    initializer { LugaresViewModel(AppContainer.eventRepository) }
    initializer { DetalleEventoViewModel(AppContainer.eventRepository, createSavedStateHandle()) }
    initializer { DetalleLugarViewModel(AppContainer.eventRepository, createSavedStateHandle()) }
}

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

    val entradaActual by navController.currentBackStackEntryAsState()
    val rutaActual = entradaActual?.destination?.route

    Scaffold(
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

        NavHost(
            navController = navController,
            startDestination = Pantalla.EXPLORAR.ruta,
            modifier = Modifier.padding(innerPadding)
        ) {

            // PANTALLA EXPLORAR
            composable(Pantalla.EXPLORAR.ruta) {

                val viewModel: ExplorarViewModel = viewModel(factory = fabricaViewModels)
                val lugaresViewModel: LugaresViewModel = viewModel(factory = fabricaViewModels)

                val estado by viewModel.uiState.collectAsStateWithLifecycle()
                val lugares by lugaresViewModel.uiState.collectAsStateWithLifecycle()

                ExplorarScreen(
                    estado = estado,
                    lugares = lugares,
                    alBuscar = viewModel::cambiarBusqueda,
                    alFiltrar = viewModel::cambiarCategoria,
                    alLimpiar = viewModel::limpiarFiltros,
                    alActualizar = viewModel::obtenerEventos,
                    alCargarCategorias = viewModel::obtenerCategorias,
                    alActualizarLugares = lugaresViewModel::cargar,
                    alEvento = { navController.navigate("evento/$it") },
                    alLugar = { navController.navigate("lugar/$it") }
                )
            }

            composable(
                route = "evento/{eventoId}",
                arguments = listOf(navArgument("eventoId") { type = NavType.IntType })
            ) {
                val vm: DetalleEventoViewModel = viewModel(factory = fabricaViewModels)
                val estado by vm.uiState.collectAsStateWithLifecycle()
                DetalleEventoScreen(
                    estado = estado,
                    alVolver = { navController.popBackStack() },
                    alActualizar = vm::cargar,
                    alLugar = { navController.navigate("lugar/$it") }
                )
            }

            composable(
                route = "lugar/{lugarId}",
                arguments = listOf(navArgument("lugarId") { type = NavType.IntType })
            ) {
                val vm: DetalleLugarViewModel = viewModel(factory = fabricaViewModels)
                val estado by vm.uiState.collectAsStateWithLifecycle()
                DetalleLugarScreen(
                    estado = estado,
                    alVolver = { navController.popBackStack() },
                    alActualizar = vm::cargar,
                    alEvento = { navController.navigate("evento/$it") }
                )
            }

            // PANTALLA MAPA
            composable(Pantalla.MAPA.ruta) {
                PantallaTemporal("Mapa")
            }

            // PANTALLA FAVORITOS
            composable(Pantalla.FAVORITOS.ruta) {
                PantallaTemporal("Favoritos")
            }

            // PANTALLA PERFIL
            composable(Pantalla.PERFIL.ruta) {
                PantallaTemporal("Perfil")
            }
        }
    }
}

@Composable
private fun PantallaTemporal(titulo: String) {

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = titulo,
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Sección en desarrollo",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
