package com.riyaz.rssclipboard.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_items")
data class SavedItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val fileName: String,
    val data: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt
)
