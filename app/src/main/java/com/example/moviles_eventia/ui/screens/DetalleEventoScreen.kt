package com.example.moviles_eventia.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.moviles_eventia.models.fechaLocal
import com.example.moviles_eventia.ui.components.*

@Composable
fun DetalleEventoScreen(
    estado: DetalleEventoUiState,
    alVolver: () -> Unit,
    alActualizar: () -> Unit,
    alLugar: (Int) -> Unit
) {
    val contexto = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        Encabezado("Detalle del evento", alActualizar, alVolver, estado.cargando)
        val evento = estado.evento
        when {
            evento == null && estado.cargando -> EstadoCargando()
            evento == null -> EstadoVacio(
                estado.error ?: "Evento no disponible", accion = "Reintentar", alActuar = alActualizar
            )
            else -> LazyColumn(
                state = rememberLazyListState(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (estado.cargando) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                estado.error?.let { item { Aviso(it, alActualizar) } }
                item { ImagenEvento(evento.imageUrl, evento.title, Modifier.fillMaxWidth().height(210.dp)) }
                item {
                    Text(evento.category.name, color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge)
                    Text(evento.title, style = MaterialTheme.typography.headlineMedium)
                }
                item {
                    Text("Inicio: ${fechaLocal(evento.startsAt)}")
                    evento.endsAt?.let { Text("Fin: ${fechaLocal(it)}") }
                }
                item {
                    Text("Lugar", style = MaterialTheme.typography.titleMedium)
                    if (evento.venue.id > 0) {
                        TextButton(onClick = { alLugar(evento.venue.id) }) { Text(evento.venue.name) }
                    } else Text(evento.venue.name)
                    Text(evento.venue.address.orEmpty().ifBlank { "Dirección no disponible" })
                }
                item {
                    Text("Cupos disponibles: ${evento.availableSpots?.toString() ?: "No informados"}")
                    Text("Capacidad: ${evento.capacity?.toString() ?: "No informada"}")
                    if (evento.availableSpots == 0) Text(
                        "Sin cupos disponibles", color = MaterialTheme.colorScheme.error
                    )
                }
                item {
                    Text("Descripción", style = MaterialTheme.typography.titleLarge)
                    Text(evento.description.ifBlank { "Sin descripción disponible." })
                }
                item {
                    Text("Ubicación", style = MaterialTheme.typography.titleLarge)
                    if (evento.venue.tieneCoordenadas) {
                        Text("${evento.venue.lat}, ${evento.venue.lng}")
                    } else Text("Coordenadas no disponibles. Puedes buscar por la dirección del lugar.")
                    OutlinedButton(
                        onClick = {
                            abrirIndicaciones(contexto, evento.venue) {
                                Toast.makeText(contexto, it, Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Cómo llegar") }
                }
            }
        }
    }
}
