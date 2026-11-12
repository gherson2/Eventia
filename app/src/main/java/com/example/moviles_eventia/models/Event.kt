
package com.example.moviles_eventia.models

data class Event(
    val id: Int,
    val title: String,
    val summary: String?,
    val startsAt: String,
    val category: Category,
    val imageUrl: String?,
    val venue: Venue,
    val capacity: Int,
    val availableSpots: Int
)

data class Category(
    val id: Int,
    val name: String
)

data class Venue(
    val id: Int,
    val name: String,
    val address: String?,
    val lat: Double?,
    val lng: Double?
)

