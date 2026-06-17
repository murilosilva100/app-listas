package com.example.applistas.data.local.dao

import androidx.room.*
import com.example.applistas.data.local.entity.Checklist
import kotlinx.coroutines.flow.Flow

@Dao
interface ChecklistDao {
    @Query("SELECT * FROM checklists ORDER BY timestamp DESC")
    fun getAllChecklists(): Flow<List<Checklist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklist(checklist: Checklist): Long

    @Delete
    suspend fun deleteChecklist(checklist: Checklist)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: Checklist)

    @Update
    suspend fun updateItem(item: Checklist)

    @Delete
    suspend fun deleteItem(item: Checklist)
}
