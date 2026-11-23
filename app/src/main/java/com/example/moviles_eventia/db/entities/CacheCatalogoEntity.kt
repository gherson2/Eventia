package com.example.moviles_eventia.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cache_catalogo")
data class CacheCatalogoEntity(
    @PrimaryKey val clave: String,
    val json: String,
    val guardadoEn: Long
)
