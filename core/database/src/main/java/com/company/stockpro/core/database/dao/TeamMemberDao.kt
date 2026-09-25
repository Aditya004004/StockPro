package com.company.stockpro.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.company.stockpro.core.database.entity.TeamMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TeamMemberDao {

    @Query("SELECT * FROM team_members WHERE plant_id = :plantId ORDER BY is_active DESC, display_name ASC")
    fun observeAll(plantId: String): Flow<List<TeamMemberEntity>>

    @Query("SELECT * FROM team_members WHERE id = :id")
    suspend fun get(id: String): TeamMemberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(members: List<TeamMemberEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(member: TeamMemberEntity)

    @Query("UPDATE team_members SET role = :role WHERE id = :id")
    suspend fun updateRole(id: String, role: String)

    @Query("UPDATE team_members SET is_active = :isActive WHERE id = :id")
    suspend fun updateActiveStatus(id: String, isActive: Boolean)

    @Query("SELECT COUNT(*) FROM team_members")
    suspend fun count(): Int
}
