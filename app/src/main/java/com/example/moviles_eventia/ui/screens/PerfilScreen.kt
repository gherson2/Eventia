package com.example.moviles_eventia.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.moviles_eventia.ui.components.*
import com.example.moviles_eventia.models.agruparInscripciones
import com.example.moviles_eventia.models.Usuario

@Composable
fun PerfilScreen(
    estado: CuentaUiState,
    inscripciones: MisInscripcionesUiState,
    alIngresar: () -> Unit,
    alRegistrar: () -> Unit,
    alActualizar: () -> Unit,
    alCerrar: () -> Unit,
    alEvento: (Int) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Encabezado("Perfil", alActualizar,
            cargando = estado.operando || estado.leyendoSesion || inscripciones.cargando)
        estado.error?.let { Aviso(it, alActualizar) }
        val usuario = estado.usuario
        when {
            estado.leyendoSesion -> EstadoCargando()
            usuario == null -> Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Estás explorando sin cuenta", style = MaterialTheme.typography.titleLarge)
                Text("Puedes consultar eventos y guardar favoritos sin iniciar sesión.")
                Button(onClick = alIngresar, modifier = Modifier.fillMaxWidth()) { Text("Iniciar sesión") }
                OutlinedButton(onClick = alRegistrar, modifier = Modifier.fillMaxWidth()) { Text("Crear cuenta") }
            }
            else -> key(usuario.id) {
                // Nunca mostrar filas de A bajo el perfil de B durante un cambio de sesión.
                val datos = if (inscripciones.usuarioId == usuario.id) inscripciones
                    else MisInscripcionesUiState(usuarioId = usuario.id,
                        cargando = inscripciones.error == null, error = inscripciones.error)
                PerfilConSesion(usuario, estado.operando, datos, alActualizar, alCerrar, alEvento)
            }
        }
    }
}

@Composable
private fun PerfilConSesion(
    usuario: Usuario,
    operando: Boolean,
    inscripciones: MisInscripcionesUiState,
    alActualizar: () -> Unit,
    alCerrar: () -> Unit,
    alEvento: (Int) -> Unit
) {
    var pestana by rememberSaveable { mutableIntStateOf(0) }
    val lista = rememberLazyListState()
    val grupos = agruparInscripciones(inscripciones.eventos)
    val visibles = if (pestana == 0) grupos.proximas else grupos.pasadas
    LazyColumn(
        state = lista,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "cuenta") {
            Text(usuario.nombre.ifBlank { "Mi cuenta" }, style = MaterialTheme.typography.headlineMedium)
            Text(usuario.correo)
            if (operando) LinearProgressIndicator(Modifier.fillMaxWidth())
            OutlinedButton(onClick = alCerrar, enabled = !operando,
                modifier = Modifier.fillMaxWidth()) { Text("Cerrar sesión") }
        }
        item(key = "titulo") { Text("Mis inscripciones", style = MaterialTheme.typography.titleLarge) }
        item(key = "pestanas") {
            PrimaryTabRow(selectedTabIndex = pestana) {
                Tab(selected = pestana == 0, onClick = { pestana = 0 },
                    text = { Text("Próximas (${grupos.proximas.size})") })
                Tab(selected = pestana == 1, onClick = { pestana = 1 },
                    text = { Text("Pasadas (${grupos.pasadas.size})") })
            }
        }
        if (inscripciones.cargando) item(key = "carga") { EstadoCargando() }
        inscripciones.error?.let {
            item(key = "error") { Aviso(it, alActualizar) }
        }
        if (!inscripciones.cargando && inscripciones.cargado && visibles.isEmpty()) {
            item(key = "vacio") {
                Text(if (pestana == 0) "No tienes inscripciones próximas." else "No tienes inscripciones pasadas.")
            }
        }
        items(visibles, key = { "evento:${it.id}" }) { evento ->
            TarjetaEvento(evento, alSeleccionar = { alEvento(evento.id) })
        }
        if (grupos.sinFecha.isNotEmpty()) {
            item(key = "sinFecha") { Text("Fecha no disponible", style = MaterialTheme.typography.titleMedium) }
            items(grupos.sinFecha, key = { "sinFecha:${it.id}" }) { evento ->
                TarjetaEvento(evento, alSeleccionar = { alEvento(evento.id) })
            }
        }
    }
}
