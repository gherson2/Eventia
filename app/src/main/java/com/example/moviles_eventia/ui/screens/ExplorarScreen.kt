package com.example.moviles_eventia.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.moviles_eventia.ui.components.*
import com.example.moviles_eventia.models.Event
import com.example.moviles_eventia.models.antiguedadCache

@Composable
fun ExplorarScreen(
    estado: ExplorarUiState,
    lugares: LugaresUiState,
    favoritos: FavoritosUiState,
    alCambiarFavorito: (Event) -> Unit,
    alBuscar: (String) -> Unit,
    alFiltrar: (Int?) -> Unit,
    alLimpiar: () -> Unit,
    alActualizar: () -> Unit,
    alCargarCategorias: () -> Unit,
    alActualizarLugares: () -> Unit,
    alEvento: (Int) -> Unit,
    alLugar: (Int) -> Unit
) {
    var pestana by rememberSaveable { mutableIntStateOf(0) }
    val listaEventos = rememberLazyListState()
    val listaLugares = rememberLazyListState()

    Column(Modifier.fillMaxSize()) {
        Encabezado(
            "Explorar",
            alActualizar = if (pestana == 0) alActualizar else alActualizarLugares,
            cargando = if (pestana == 0) estado.cargando else lugares.cargando
        )
        PrimaryTabRow(selectedTabIndex = pestana) {
            Tab(selected = pestana == 0, onClick = { pestana = 0 }, text = { Text("Eventos") })
            Tab(selected = pestana == 1, onClick = { pestana = 1 }, text = { Text("Lugares") })
        }
        if (pestana == 0) {
            OutlinedTextField(
                value = estado.busqueda,
                onValueChange = alBuscar,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                label = { Text("Buscar eventos por nombre") },
                singleLine = true
            )
            FiltrosCategorias(estado.categorias, estado.categoriaId, alFiltrar)
            if (estado.busqueda.isNotBlank() || estado.categoriaId != null) {
                TextButton(onClick = alLimpiar, modifier = Modifier.padding(horizontal = 8.dp)) {
                    Text("Limpiar filtros")
                }
            }
            estado.errorCategorias?.let { Aviso("Categorías: $it", alCargarCategorias) }
            estado.copiaGuardadaEn?.let {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text("Datos guardados · hace ${antiguedadCache(it)}",
                        Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                }
            }
            if (estado.cargando && estado.eventos.isNotEmpty()) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            if (estado.datosDisponibles) estado.error?.let { Aviso(it, alActualizar) }
            when {
                estado.cargando && estado.eventos.isEmpty() -> EstadoCargando()
                estado.eventos.isEmpty() && estado.error != null && !estado.datosDisponibles ->
                    EstadoVacio("No pudimos cargar los eventos", estado.error, "Reintentar", alActualizar)
                estado.eventos.isEmpty() ->
                    EstadoVacio("Sin coincidencias", "No hay eventos próximos con estos filtros.",
                        "Limpiar filtros", alLimpiar)
                else -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    state = listaEventos,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(estado.eventos, key = { it.id }) { evento ->
                        TarjetaEvento(
                            evento = evento,
                            esFavorito = favoritos.eventos.any { it.id == evento.id },
                            favoritoHabilitado = !favoritos.cargando && !favoritos.guardando && favoritos.error == null,
                            alCambiarFavorito = { alCambiarFavorito(evento) },
                            alSeleccionar = { alEvento(evento.id) }
                        )
                    }
                }
            }
        } else {
            if (lugares.cargando && lugares.lugares.isNotEmpty()) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            if (lugares.lugares.isNotEmpty()) lugares.error?.let { Aviso(it, alActualizarLugares) }
            when {
                lugares.cargando && lugares.lugares.isEmpty() -> EstadoCargando()
                lugares.lugares.isEmpty() && lugares.error != null ->
                    EstadoVacio("No pudimos cargar los lugares", lugares.error, "Reintentar", alActualizarLugares)
                lugares.lugares.isEmpty() -> EstadoVacio("No hay lugares disponibles")
                else -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    state = listaLugares,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(lugares.lugares, key = { it.id }) { lugar ->
                        OutlinedCard(onClick = { alLugar(lugar.id) }, modifier = Modifier.fillMaxWidth()) {
                            ImagenEvento(lugar.imageUrl, lugar.name, Modifier.fillMaxWidth().height(140.dp))
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(lugar.name, style = MaterialTheme.typography.titleMedium)
                                Text(lugar.address.orEmpty().ifBlank { "Dirección no disponible" })
                                Text("${lugar.upcomingEventsCount} eventos próximos",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
