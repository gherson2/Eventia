
package com.example.moviles_eventia.api.dto

data class EventDto(
    val id: Int,
    val title: String?,
    val summary: String?,
    val startsAt: String?,
    val category: CategoryDto?,
    val imageUrl: String?,
    val venue: VenueDto?,
    val capacity: Int?,
    val availableSpots: Int?,
    val description: String? = null,
    val endsAt: String? = null
)

data class CategoryDto(
    val id: Int,
    val name: String?
)

data class VenueDto(
    val id: Int,
    val name: String?,
    val address: String?,
    val lat: Double?,
    val lng: Double?,
    val description: String? = null,
    @com.google.gson.annotations.SerializedName(value = "openingHours", alternate = ["hours", "opening_hours"])
    val openingHours: String? = null,
    val imageUrl: String? = null,
    val upcomingEventsCount: Int? = null
)
