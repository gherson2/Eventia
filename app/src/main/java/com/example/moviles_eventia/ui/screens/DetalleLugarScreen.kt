package com.example.moviles_eventia.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.moviles_eventia.ui.components.*

@Composable
fun DetalleLugarScreen(
    estado: DetalleLugarUiState,
    alVolver: () -> Unit,
    alActualizar: () -> Unit,
    alEvento: (Int) -> Unit
) {
    val contexto = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        Encabezado("Detalle del lugar", alActualizar, alVolver, estado.cargando)
        val lugar = estado.lugar
        when {
            lugar == null && estado.cargando -> EstadoCargando()
            lugar == null -> EstadoVacio(
                estado.error ?: "Lugar no disponible", accion = "Reintentar", alActuar = alActualizar
            )
            else -> LazyColumn(
                state = rememberLazyListState(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (estado.cargando) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                estado.error?.let { item { Aviso(it, alActualizar) } }
                item { ImagenEvento(lugar.imageUrl, lugar.name, Modifier.fillMaxWidth().height(190.dp)) }
                item { Text(lugar.name, style = MaterialTheme.typography.headlineMedium) }
                item { Text(lugar.address.orEmpty().ifBlank { "Dirección no disponible" }) }
                item { Text("Horario: ${lugar.openingHours.ifBlank { "No informado" }}") }
                item { Text(lugar.description.ifBlank { "Sin descripción disponible." }) }
                if (lugar.tieneCoordenadas) item { Text("Ubicación: ${lugar.lat}, ${lugar.lng}") }
                item {
                    OutlinedButton(
                        onClick = {
                            abrirIndicaciones(contexto, lugar) {
                                Toast.makeText(contexto, it, Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Cómo llegar") }
                }
                item { Text("Próximos eventos aquí", style = MaterialTheme.typography.titleLarge) }
                estado.errorEventos?.let { item { Aviso("Eventos del lugar: $it", alActualizar) } }
                if (estado.eventos.isEmpty() && !estado.cargando && estado.errorEventos == null && estado.error == null) {
                    item { Text("No hay próximos eventos en este lugar.") }
                }
                items(estado.eventos, key = { it.id }) { evento ->
                    TarjetaEvento(evento) { alEvento(evento.id) }
                }
            }
        }
    }
}
