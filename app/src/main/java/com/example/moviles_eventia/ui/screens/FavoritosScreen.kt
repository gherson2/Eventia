package com.example.moviles_eventia.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.moviles_eventia.models.Event
import com.example.moviles_eventia.ui.components.*

@Composable
fun FavoritosScreen(
    estado: FavoritosUiState,
    alReintentar: () -> Unit,
    alEvento: (Int) -> Unit,
    alQuitar: (Event) -> Unit
) {
    val lista = rememberLazyListState()
    Column(Modifier.fillMaxSize()) {
        Text("Favoritos", Modifier.padding(16.dp), style = MaterialTheme.typography.headlineSmall)
        Text(
            "Guardados en este dispositivo. La lista no necesita Internet.",
            Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall
        )
        estado.error?.let { Aviso(it, alReintentar) }
        when {
            estado.cargando && estado.eventos.isEmpty() -> EstadoCargando()
            estado.eventos.isEmpty() && estado.error == null ->
                EstadoVacio("Aún no tienes favoritos", "Toca el corazón de un evento para guardarlo.")
            else -> LazyColumn(
                state = lista,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(estado.eventos, key = { it.id }) { evento ->
                    TarjetaEvento(
                        evento = evento,
                        esFavorito = true,
                        favoritoHabilitado = !estado.guardando && !estado.cargando && estado.error == null,
                        alCambiarFavorito = { alQuitar(evento) },
                        alSeleccionar = { alEvento(evento.id) }
                    )
                }
            }
        }
    }
}
