package com.example.applistas.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "checklists")
data class Checklist(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val validity: Long,
    val isSynced: Boolean = false,
    val isCompleted: Boolean = false
)
