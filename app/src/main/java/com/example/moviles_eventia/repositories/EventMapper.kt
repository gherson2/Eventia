
package com.example.moviles_eventia.repositories

import com.example.moviles_eventia.api.dto.EventDto
import com.example.moviles_eventia.models.Category
import com.example.moviles_eventia.models.Event
import com.example.moviles_eventia.models.Venue

fun EventDto.toEvent(): Event {

    return Event(
        id = id,
        title = title,
        summary = summary,
        startsAt = startsAt,
        category = Category(
            id = category.id,
            name = category.name
        ),
        imageUrl = imageUrl,
        venue = Venue(
            id = venue.id,
            name = venue.name,
            address = venue.address,
            lat = venue.lat,
            lng = venue.lng
        ),
        capacity = capacity,
        availableSpots = availableSpots
    )
}
