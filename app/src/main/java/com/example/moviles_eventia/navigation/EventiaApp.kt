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
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.moviles_eventia.PantallaBienvenidaEventia

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
            NavigationBar {

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

            composable(Pantalla.EXPLORAR.ruta) {
                PantallaBienvenidaEventia()
            }

            composable(Pantalla.MAPA.ruta) {
                PantallaTemporal("Mapa")
            }

            composable(Pantalla.FAVORITOS.ruta) {
                PantallaTemporal("Favoritos")
            }

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
