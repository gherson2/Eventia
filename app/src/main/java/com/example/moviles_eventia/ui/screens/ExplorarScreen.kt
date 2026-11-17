
package com.example.moviles_eventia.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ExplorarScreen(estado: ExplorarUiState) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "Explorar eventos",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (estado.cargando) {

            CircularProgressIndicator()

        } else if (estado.error != null) {

            Text(text = estado.error)

        } else if (estado.eventos.isEmpty()) {

            Text(text = "No hay eventos disponibles")

        } else {

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                items(estado.eventos) { evento ->

                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {

                            Text(
                                text = evento.title,
                                style = MaterialTheme.typography.titleMedium
                            )

                            Text(
                                text = evento.summary
                                    ?: "Sin descripción"
                            )

                            Text(
                                text = "Lugar: ${evento.venue.name}"
                            )

                            Text(
                                text = "Cupos: ${evento.availableSpots}"
                            )
                        }
                    }
                }
            }
        }
    }
}
