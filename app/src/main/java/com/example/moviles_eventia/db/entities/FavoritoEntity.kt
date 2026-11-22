package com.example.moviles_eventia.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

// Copia completa e independiente del catálogo remoto.
@Entity(tableName = "favoritos")
data class FavoritoEntity(
    @PrimaryKey val id: Int,
    val json: String
)
