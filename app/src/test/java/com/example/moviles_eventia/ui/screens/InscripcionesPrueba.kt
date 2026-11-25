package com.example.moviles_eventia.ui.screens

import com.example.moviles_eventia.models.Event
import com.example.moviles_eventia.repositories.InscripcionRepository

class InscripcionesPrueba(var respuesta: Event = RepositorioPrueba().evento.copy(isAttending = true)) :
    InscripcionRepository {
    val llamadas = mutableListOf<Pair<Int, Boolean>>()
    var error: Exception? = null
    var alCambiar: (suspend (Int, Boolean) -> Event)? = null
    var consultas = 0
    val usuariosConsultados = mutableListOf<Int>()
    var lista: List<Event> = emptyList()
    var errorConsulta: Exception? = null
    var alConsultar: (suspend () -> List<Event>)? = null
    override suspend fun obtenerMisInscripciones(usuarioId: Int): List<Event> {
        consultas++
        usuariosConsultados += usuarioId
        errorConsulta?.let { throw it }
        return alConsultar?.invoke() ?: lista
    }
    override suspend fun cambiarInscripcion(id: Int, inscribir: Boolean): Event {
        llamadas += id to inscribir
        error?.let { throw it }
        return alCambiar?.invoke(id, inscribir) ?: respuesta
    }
}
