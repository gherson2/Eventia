
package com.example.moviles_eventia.repositories

import com.example.moviles_eventia.api.dto.EventDto
import com.example.moviles_eventia.models.Category
import com.example.moviles_eventia.models.Event
import com.example.moviles_eventia.models.Venue
import com.example.moviles_eventia.api.dto.CategoryDto
import com.example.moviles_eventia.api.dto.VenueDto
import com.example.moviles_eventia.BuildConfig
import java.net.URI

fun EventDto.toEvent(): Event {

    return Event(
        id = id,
        title = title?.takeIf { it.isNotBlank() } ?: "Evento",
        summary = summary,
        startsAt = startsAt.orEmpty(),
        category = category?.toCategory() ?: Category(0, "Sin categoría"),
        imageUrl = direccionImagen(imageUrl),
        venue = venue?.toVenue() ?: Venue(0, "Lugar no disponible", null, null, null),
        capacity = capacity,
        availableSpots = availableSpots,
        description = description?.takeIf { it.isNotBlank() } ?: summary.orEmpty(),
        endsAt = endsAt
    )
}

fun CategoryDto.toCategory() = Category(id, name.orEmpty().ifBlank { "Sin categoría" })

fun VenueDto.toVenue() = Venue(
    id = id,
    name = name.orEmpty().ifBlank { "Lugar no disponible" },
    address = address,
    lat = lat,
    lng = lng,
    description = description.orEmpty(),
    openingHours = openingHours.orEmpty(),
    imageUrl = direccionImagen(imageUrl),
    upcomingEventsCount = upcomingEventsCount ?: 0
)

fun direccionImagen(valor: String?, baseUrl: String = BuildConfig.BASE_URL): String? {
    if (valor.isNullOrBlank()) return null
    return runCatching {
        val base = URI(baseUrl)
        val uri = base.resolve(valor)
        if (uri.scheme !in listOf("http", "https")) return null
        if (uri.host == "localhost" || uri.host == "127.0.0.1") {
            URI(base.scheme, null, base.host, base.port, uri.path, uri.query, uri.fragment).toString()
        } else uri.toString()
    }.getOrNull()
}
