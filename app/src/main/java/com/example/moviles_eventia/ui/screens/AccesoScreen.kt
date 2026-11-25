package com.example.moviles_eventia.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun AccesoScreen(
    estado: AccesoUiState,
    alNombre: (String) -> Unit,
    alCorreo: (String) -> Unit,
    alClave: (String) -> Unit,
    alCambiarModo: () -> Unit,
    alEnviar: () -> Unit,
    alVolver: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(estado.registro) { visible = false }
    Column(Modifier.fillMaxSize().imePadding()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = alVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") }
            Text(if (estado.registro) "Crear cuenta" else "Iniciar sesión",
                style = MaterialTheme.typography.headlineSmall)
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Eventia", style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary)
            Text("Tu cuenta es opcional para explorar y guardar favoritos.")
            if (estado.registro) OutlinedTextField(
                value = estado.nombre, onValueChange = alNombre, singleLine = true,
                label = { Text("Nombre") }, enabled = !estado.enviando,
                modifier = Modifier.fillMaxWidth(), isError = estado.campos["name"] != null,
                supportingText = { estado.campos["name"]?.let { Text(it) } }
            )
            OutlinedTextField(
                value = estado.correo, onValueChange = alCorreo, singleLine = true,
                label = { Text("Correo electrónico") }, enabled = !estado.enviando,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(), isError = estado.campos["email"] != null,
                supportingText = { estado.campos["email"]?.let { Text(it) } }
            )
            OutlinedTextField(
                value = estado.clave, onValueChange = alClave, singleLine = true,
                label = { Text("Contraseña") }, enabled = !estado.enviando,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(), isError = estado.campos["password"] != null,
                supportingText = { Text(estado.campos["password"] ?: "Mínimo 8 caracteres") },
                trailingIcon = {
                    TextButton(onClick = { visible = !visible }, enabled = !estado.enviando) {
                        Text(if (visible) "Ocultar" else "Mostrar")
                    }
                }
            )
            estado.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = alEnviar, enabled = !estado.enviando,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(if (estado.enviando) "Espera…" else if (estado.registro) "Crear cuenta" else "Iniciar sesión")
            }
            if (estado.enviando) CircularProgressIndicator()
            TextButton(onClick = alCambiarModo, enabled = !estado.enviando) {
                Text(if (estado.registro) "Ya tengo una cuenta" else "Crear una cuenta")
            }
            TextButton(onClick = alVolver) { Text("Continuar sin cuenta") }
        }
    }
}
