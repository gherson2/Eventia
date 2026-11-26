package com.example.moviles_eventia.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.moviles_eventia.models.Event
import com.example.moviles_eventia.models.agruparEventosMapa
import com.example.moviles_eventia.models.antiguedadCache
import com.example.moviles_eventia.models.fechaLocal
import com.example.moviles_eventia.models.siguienteEventoMapa
import com.example.moviles_eventia.ui.components.*

@Composable
fun MapaScreen(
    estado: ExplorarUiState,
    favoritos: FavoritosUiState,
    alFiltrar: (Int?) -> Unit,
    alLimpiar: () -> Unit,
    alActualizar: () -> Unit,
    alCargarCategorias: () -> Unit,
    alEvento: (Int) -> Unit,
    alCambiarFavorito: (Event) -> Unit,
    cargarTeselas: Boolean = true
) {
    var seleccionadoId by rememberSaveable { mutableStateOf<Int?>(null) }
    var elegir by rememberSaveable { mutableStateOf(false) }
    val puntos = agruparEventosMapa(estado.eventos, estado.categoriaId)
    val eventos = puntos.flatMap { it.eventos }
    val seleccionado = eventos.find { it.id == seleccionadoId }
    LaunchedEffect(eventos.map { it.id }, seleccionadoId) {
        if (seleccionadoId != null && seleccionado == null) seleccionadoId = null
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val controlesMax = (maxHeight * 0.30f).coerceAtMost(180.dp)
        Column(Modifier.fillMaxSize()) {
            Encabezado("Mapa de eventos", alActualizar, cargando = estado.cargando)
            if (estado.cargando) LinearProgressIndicator(Modifier.fillMaxWidth())
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = controlesMax)) {
                item { FiltrosCategorias(estado.categorias, estado.categoriaId, alFiltrar) }
                item {
                    Row(Modifier.padding(horizontal = 8.dp)) {
                        TextButton(onClick = { elegir = true }, enabled = eventos.isNotEmpty()) {
                            Text("Elegir evento (${eventos.size})")
                        }
                        if (estado.busqueda.isNotBlank() || estado.categoriaId != null) {
                            TextButton(onClick = alLimpiar) { Text("Limpiar filtros") }
                        }
                    }
                }
                if (estado.busqueda.isNotBlank()) item {
                    Text("Búsqueda activa: ${estado.busqueda}", Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.bodySmall)
                }
                estado.copiaGuardadaEn?.let { fecha -> item {
                    Text("Datos guardados · hace ${antiguedadCache(fecha)}",
                        Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.bodySmall)
                } }
                estado.errorCategorias?.let { item { Aviso("Categorías: $it", alCargarCategorias) } }
                estado.error?.let { item { Aviso(it, alActualizar) } }
                if (!cargarTeselas) item {
                    Text("El fondo necesita Internet para cargar zonas nuevas.",
                        Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.bodySmall)
                }
            }
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val resumenMax = maxHeight * 0.60f
                MapaEventos(puntos, { seleccionadoId = it }, Modifier.fillMaxSize(), cargarTeselas)
                if (eventos.isEmpty() && !estado.cargando) {
                    Surface(
                        Modifier.align(Alignment.TopCenter).padding(12.dp)
                            .heightIn(max = maxHeight * 0.70f).verticalScroll(rememberScrollState()),
                        shape = MaterialTheme.shapes.medium,
                        tonalElevation = 4.dp
                    ) {
                        EstadoVacio(
                            if (!estado.datosDisponibles && estado.error != null) "No pudimos cargar el mapa"
                                else "Sin eventos con ubicación",
                            "Solo se muestran eventos próximos con coordenadas válidas.",
                            if (estado.error != null) "Reintentar" else "Limpiar filtros",
                            if (estado.error != null) alActualizar else alLimpiar
                        )
                    }
                }
                if (seleccionado != null) {
                    Surface(
                        Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 24.dp)
                            .heightIn(max = resumenMax).testTag("resumen-mapa"),
                        tonalElevation = 6.dp,
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column(Modifier.verticalScroll(rememberScrollState()).padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(seleccionado.title, style = MaterialTheme.typography.titleMedium)
                            Button(onClick = { alEvento(seleccionado.id) }, modifier = Modifier.fillMaxWidth()) {
                                Text("Ver detalle")
                            }
                            Text(fechaLocal(seleccionado.startsAt), style = MaterialTheme.typography.bodySmall)
                            Text(seleccionado.venue.name, style = MaterialTheme.typography.bodySmall)
                            Text(seleccionado.category.name, style = MaterialTheme.typography.labelMedium)
                            val grupo = puntos.first { punto -> punto.eventos.any { it.id == seleccionado.id } }
                            if (grupo.eventos.size > 1) {
                                TextButton(onClick = {
                                    seleccionadoId = siguienteEventoMapa(puntos, seleccionado.id)?.id
                                }) { Text("Otro evento aquí (${grupo.eventos.size})") }
                            }
                            OutlinedButton(
                                enabled = !favoritos.cargando && !favoritos.guardando && favoritos.error == null,
                                onClick = { alCambiarFavorito(seleccionado) }
                            ) {
                                Text(if (favoritos.eventos.any { it.id == seleccionado.id })
                                    "Quitar de favoritos" else "Guardar en favoritos")
                            }
                            TextButton(onClick = { seleccionadoId = null }) { Text("Cerrar resumen") }
                        }
                    }
                }
            }
        }
    }
    if (elegir) AlertDialog(
        onDismissRequest = { elegir = false },
        title = { Text("Eventos con ubicación") },
        text = {
            LazyColumn(Modifier.heightIn(max = 360.dp)) {
                items(eventos, key = { it.id }) { evento ->
                    TextButton(onClick = { seleccionadoId = evento.id; elegir = false },
                        modifier = Modifier.fillMaxWidth()) { Text(evento.title) }
                }
            }
        },
        confirmButton = { TextButton(onClick = { elegir = false }) { Text("Cerrar") } }
    )
}
