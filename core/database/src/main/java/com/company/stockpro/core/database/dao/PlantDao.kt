package com.company.stockpro.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.company.stockpro.core.database.entity.PlantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantDao {

    @Query("SELECT * FROM plants WHERE is_active = 1 ORDER BY name ASC")
    fun observeAllPlants(): Flow<List<PlantEntity>>

    @Query("SELECT * FROM plants WHERE is_active = 1 ORDER BY name ASC")
    suspend fun getAllPlants(): List<PlantEntity>

    @Query("SELECT * FROM plants WHERE id = :id LIMIT 1")
    suspend fun getPlantById(id: String): PlantEntity?

    @Query("SELECT * FROM plants WHERE code = :code COLLATE NOCASE LIMIT 1")
    suspend fun getPlantByCode(code: String): PlantEntity?

    @Query("SELECT * FROM plants WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun getPlantByName(name: String): PlantEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPlant(plant: PlantEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlants(plants: List<PlantEntity>)

    @Query("SELECT COUNT(*) FROM plants")
    suspend fun count(): Int

    @Query("DELETE FROM plants WHERE id = :id")
    suspend fun deletePlantById(id: String)

    @Query("SELECT COUNT(*) FROM plants WHERE is_active = 1")
    suspend fun countActivePlants(): Int
}
