package com.example.moviles_eventia.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.moviles_eventia.R
import com.example.moviles_eventia.models.Category
import com.example.moviles_eventia.models.Event
import com.example.moviles_eventia.models.fechaLocal

@Composable
fun Encabezado(
    titulo: String,
    alActualizar: () -> Unit,
    alVolver: (() -> Unit)? = null,
    cargando: Boolean = false
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (alVolver != null) IconButton(onClick = alVolver) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
        }
        Text(
            titulo,
            modifier = Modifier.weight(1f).padding(8.dp),
            style = MaterialTheme.typography.headlineSmall
        )
        IconButton(onClick = alActualizar, enabled = !cargando) {
            Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
        }
    }
}

@Composable
fun ImagenEvento(url: String?, nombre: String, modifier: Modifier = Modifier) {
    AsyncImage(
        model = url,
        contentDescription = "Imagen de $nombre",
        placeholder = painterResource(R.drawable.event_placeholder),
        error = painterResource(R.drawable.event_placeholder),
        fallback = painterResource(R.drawable.event_placeholder),
        contentScale = ContentScale.Crop,
        modifier = modifier
    )
}

// Reutilizada en el catálogo y en los próximos eventos de un lugar.
@Composable
fun TarjetaEvento(evento: Event, alSeleccionar: () -> Unit) {
    OutlinedCard(onClick = alSeleccionar, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ImagenEvento(evento.imageUrl, evento.title, Modifier.size(80.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    evento.category.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    evento.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                evento.summary?.takeIf { it.isNotBlank() }?.let {
                    Text(it, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall)
                }
                Text(fechaLocal(evento.startsAt), style = MaterialTheme.typography.bodySmall)
                Text(evento.venue.name, style = MaterialTheme.typography.bodySmall,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("Cupos: ${evento.availableSpots?.toString() ?: "No informados"}",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun FiltrosCategorias(categorias: List<Category>, seleccionada: Int?, alSeleccionar: (Int?) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(selected = seleccionada == null, onClick = { alSeleccionar(null) },
                label = { Text("Todas") })
        }
        items(categorias, key = { it.id }) { categoria ->
            FilterChip(
                selected = seleccionada == categoria.id,
                onClick = { alSeleccionar(categoria.id) },
                label = { Text(categoria.name) }
            )
        }
    }
}

@Composable
fun Aviso(mensaje: String, alReintentar: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(mensaje, style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = alReintentar) { Text("Reintentar") }
        }
    }
}

@Composable
fun EstadoVacio(
    titulo: String,
    mensaje: String = "",
    accion: String? = null,
    alActuar: () -> Unit = {}
) {
    Column(
        Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(titulo, style = MaterialTheme.typography.titleLarge)
        if (mensaje.isNotBlank()) Text(mensaje)
        if (accion != null) OutlinedButton(onClick = alActuar) { Text(accion) }
    }
}

@Composable
fun EstadoCargando() {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
