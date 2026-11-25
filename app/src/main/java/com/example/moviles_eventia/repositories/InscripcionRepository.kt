package com.example.moviles_eventia.repositories

import com.example.moviles_eventia.models.Event

class SesionRequeridaException : Exception("Inicia sesión para gestionar tu inscripción.")

interface InscripcionRepository {
    suspend fun cambiarInscripcion(id: Int, inscribir: Boolean): Event
    suspend fun obtenerMisInscripciones(usuarioId: Int): List<Event>
}
