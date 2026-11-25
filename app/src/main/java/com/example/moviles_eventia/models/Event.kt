
package com.example.moviles_eventia.models

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class Event(
    val id: Int,
    val title: String,
    val summary: String?,
    val startsAt: String,
    val category: Category,
    val imageUrl: String?,
    val venue: Venue,
    val capacity: Int?,
    val availableSpots: Int?,
    val description: String = "",
    val endsAt: String? = null,
    val isAttending: Boolean = false
) {
    fun instante(): Instant? = runCatching { OffsetDateTime.parse(startsAt).toInstant() }.getOrNull()
    fun esProximo(ahora: Instant = Instant.now()): Boolean =
        instante()?.let { !it.isBefore(ahora) } ?: false
}

data class Category(
    val id: Int,
    val name: String
)

data class Venue(
    val id: Int,
    val name: String,
    val address: String?,
    val lat: Double?,
    val lng: Double?,
    val description: String = "",
    val openingHours: String = "",
    val imageUrl: String? = null,
    val upcomingEventsCount: Int = 0
) {
    val tieneCoordenadas: Boolean
        get() = lat != null && lng != null && lat.isFinite() && lng.isFinite() &&
            lat in -90.0..90.0 && lng in -180.0..180.0
}

fun filtrarEventos(
    eventos: List<Event>,
    busqueda: String = "",
    categoriaId: Int? = null,
    ahora: Instant = Instant.now()
): List<Event> = eventos.filter {
    it.esProximo(ahora) && it.title.contains(busqueda.trim(), ignoreCase = true) &&
        (categoriaId == null || it.category.id == categoriaId)
}.sortedBy { it.instante() }

fun fechaLocal(valor: String): String = runCatching {
    OffsetDateTime.parse(valor).atZoneSameInstant(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("EEE d MMM yyyy · HH:mm", Locale.forLanguageTag("es")))
}.getOrDefault("Fecha no disponible")

