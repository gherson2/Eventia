package com.example.moviles_eventia.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.moviles_eventia.db.entities.CacheCatalogoEntity

@Dao
interface CacheCatalogoDao {
    @Query("SELECT * FROM cache_catalogo WHERE clave = :clave")
    suspend fun obtener(clave: String): CacheCatalogoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(copia: CacheCatalogoEntity)
}
