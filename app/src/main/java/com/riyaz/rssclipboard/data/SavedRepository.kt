package com.riyaz.rssclipboard.data

import kotlinx.coroutines.flow.Flow

class SavedRepository(private val dao: SavedDao) {
    val items: Flow<List<SavedItem>> = dao.observeAll()

    suspend fun add(category: String, fileName: String, data: String, description: String) {
        val cleanData = data.trim()
        if (cleanData.isBlank()) return
        val now = System.currentTimeMillis()
        dao.insert(
            SavedItem(
                category = category.trim().ifBlank { "General" },
                fileName = fileName.trim().ifBlank { "Untitled" },
                data = cleanData,
                description = description.trim(),
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun update(item: SavedItem, category: String, fileName: String, data: String, description: String) {
        val cleanData = data.trim()
        if (cleanData.isBlank()) return
        dao.update(
            item.copy(
                category = category.trim().ifBlank { "General" },
                fileName = fileName.trim().ifBlank { "Untitled" },
                data = cleanData,
                description = description.trim(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun delete(item: SavedItem) = dao.delete(item)
    suspend fun clear() = dao.deleteAll()
}
