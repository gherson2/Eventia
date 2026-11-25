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
import com.example.moviles_eventia.models.Event
import com.example.moviles_eventia.ui.components.*

@Composable
fun DetalleEventoScreen(
    estado: DetalleEventoUiState,
    esFavorito: Boolean,
    favoritoHabilitado: Boolean,
    alCambiarFavorito: (Event) -> Unit,
    alVolver: () -> Unit,
    alActualizar: () -> Unit,
    alInscripcion: () -> Unit,
    alLugar: (Int) -> Unit
) {
    val contexto = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        Encabezado("Detalle del evento", alActualizar, alVolver,
            estado.cargando || estado.modificandoInscripcion)
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
                if (estado.esCopiaGuardada) item {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            "Copia guardada en este dispositivo. Los datos y cupos pueden haber cambiado.",
                            Modifier.fillMaxWidth().padding(12.dp)
                        )
                    }
                }
                item { ImagenEvento(evento.imageUrl, evento.title, Modifier.fillMaxWidth().height(210.dp)) }
                item {
                    OutlinedButton(
                        onClick = { alCambiarFavorito(evento) },
                        enabled = favoritoHabilitado,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (esFavorito) "Quitar de favoritos" else "Guardar en favoritos")
                    }
                }
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
                    val etiqueta = if (estado.esCopiaGuardada) "Cupos guardados" else "Cupos disponibles"
                    Text("$etiqueta: ${evento.availableSpots?.toString() ?: "No informados"}")
                    Text("Capacidad: ${evento.capacity?.toString() ?: "No informada"}")
                    if (evento.availableSpots == 0) Text(
                        if (estado.esCopiaGuardada) "La copia guardada indicaba que no había cupos."
                            else "Sin cupos disponibles",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                item {
                    estado.mensajeInscripcion?.let {
                        Text(it, color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp))
                    }
                    if (estado.haySesion && estado.datosRecientes && evento.isAttending) {
                        Text("Estás inscrito en este evento.", style = MaterialTheme.typography.titleMedium)
                    }
                    Button(
                        onClick = alInscripcion,
                        enabled = estado.puedeGestionarInscripcion,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    ) {
                        Text(when {
                            estado.modificandoInscripcion -> "Procesando…"
                            estado.esCopiaGuardada -> "Conéctate para gestionar tu inscripción"
                            !estado.datosRecientes -> "Actualiza el detalle para continuar"
                            !evento.esProximo() -> "Inscripciones no disponibles"
                            !evento.isAttending && evento.availableSpots == 0 -> "Sin cupos disponibles"
                            !estado.haySesion -> "Inicia sesión para inscribirte"
                            evento.isAttending -> "Cancelar inscripción"
                            else -> "Inscribirme"
                        })
                    }
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
