package com.example.moviles_eventia.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.moviles_eventia.db.entities.FavoritoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritosDao {
    @Query("SELECT * FROM favoritos ORDER BY id")
    fun observarFavoritos(): Flow<List<FavoritoEntity>>

    @Query("SELECT * FROM favoritos WHERE id = :id")
    suspend fun obtenerPorId(id: Int): FavoritoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(favorito: FavoritoEntity)

    @Query("DELETE FROM favoritos WHERE id = :id")
    suspend fun quitar(id: Int)
}
