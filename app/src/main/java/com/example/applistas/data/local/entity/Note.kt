package com.example.applistas.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class NotePriority {
    LOW,
    MEDIUM,
    HIGH
}

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val priority: NotePriority,
    @Embedded(prefix = "address_")
    val address: Address? = null,
    val isSynced: Boolean = false
)

data class Address(
    val latitude: Double,
    val longitude: Double,
)
