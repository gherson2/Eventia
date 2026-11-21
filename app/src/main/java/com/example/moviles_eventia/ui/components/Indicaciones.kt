package com.example.moviles_eventia.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import com.example.moviles_eventia.models.Venue

fun abrirIndicaciones(contexto: Context, lugar: Venue, alError: (String) -> Unit) {
    val uri = when {
        lugar.tieneCoordenadas ->
            "geo:${lugar.lat},${lugar.lng}?q=${lugar.lat},${lugar.lng}(${Uri.encode(lugar.name)})".toUri()
        !lugar.address.isNullOrBlank() ->
            "geo:0,0?q=${Uri.encode("${lugar.name}, ${lugar.address}")}".toUri()
        else -> {
            alError("Este lugar no tiene una ubicación disponible.")
            return
        }
    }
    try {
        contexto.startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (_: ActivityNotFoundException) {
        alError("No hay una aplicación de mapas instalada.")
    } catch (_: SecurityException) {
        alError("No se pudo abrir la aplicación de mapas.")
    }
}
