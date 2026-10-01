package com.company.stockpro.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.company.stockpro.core.database.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Query("SELECT * FROM notifications WHERE plant_id = :plantId ORDER BY created_at DESC")
    fun observeAll(plantId: String): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE plant_id = :plantId AND is_read = 0")
    fun observeUnreadCount(plantId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notifications: List<NotificationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity)

    @Query("UPDATE notifications SET is_read = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Query("UPDATE notifications SET is_read = 1 WHERE plant_id = :plantId")
    suspend fun markAllAsRead(plantId: String)

    @Query("SELECT * FROM notifications WHERE plant_id = :plantId")
    suspend fun getAll(plantId: String): List<NotificationEntity>

    @Query("DELETE FROM notifications WHERE plant_id = :plantId")
    suspend fun deleteAll(plantId: String)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM notifications")
    suspend fun count(): Int
}
