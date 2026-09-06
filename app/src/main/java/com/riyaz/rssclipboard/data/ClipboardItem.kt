package com.riyaz.rssclipboard.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clipboard_items")
data class ClipboardItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val type: ClipboardType,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = createdAt + DAY_MS,
    val pinned: Boolean = false
) {
    companion object { const val DAY_MS = 24L * 60L * 60L * 1000L }
}

enum class ClipboardType { TEXT, URL, EMAIL, PHONE, SENSITIVE }
