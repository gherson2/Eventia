package com.example.moviles_eventia.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.moviles_eventia.ui.components.*

@Composable
fun PerfilScreen(
    estado: CuentaUiState,
    alIngresar: () -> Unit,
    alRegistrar: () -> Unit,
    alActualizar: () -> Unit,
    alCerrar: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Encabezado("Perfil", alActualizar, cargando = estado.operando || estado.leyendoSesion)
        estado.error?.let { Aviso(it, alActualizar) }
        if (estado.leyendoSesion) EstadoCargando()
        else Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            val usuario = estado.usuario
            if (usuario == null) {
                Text("Estás explorando sin cuenta", style = MaterialTheme.typography.titleLarge)
                Text("Puedes consultar eventos y guardar favoritos sin iniciar sesión.")
                Button(onClick = alIngresar, modifier = Modifier.fillMaxWidth()) { Text("Iniciar sesión") }
                OutlinedButton(onClick = alRegistrar, modifier = Modifier.fillMaxWidth()) { Text("Crear cuenta") }
            } else {
                Text(usuario.nombre.ifBlank { "Mi cuenta" }, style = MaterialTheme.typography.headlineMedium)
                Text(usuario.correo)
                if (estado.operando) LinearProgressIndicator(Modifier.fillMaxWidth())
                OutlinedButton(onClick = alCerrar, enabled = !estado.operando,
                    modifier = Modifier.fillMaxWidth()) { Text("Cerrar sesión") }
            }
        }
    }
}
