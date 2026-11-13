
package com.example.moviles_eventia.api.dto

data class EventDto(
    val id: Int,
    val title: String,
    val summary: String?,
    val startsAt: String,
    val category: CategoryDto,
    val imageUrl: String?,
    val venue: VenueDto,
    val capacity: Int,
    val availableSpots: Int
)

data class CategoryDto(
    val id: Int,
    val name: String
)

data class VenueDto(
    val id: Int,
    val name: String,
    val address: String?,
    val lat: Double?,
    val lng: Double?
)
