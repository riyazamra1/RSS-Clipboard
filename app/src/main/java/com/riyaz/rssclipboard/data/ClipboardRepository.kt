package com.riyaz.rssclipboard.data

import kotlinx.coroutines.flow.Flow

class ClipboardRepository(private val dao: ClipboardDao) {
    val items: Flow<List<ClipboardItem>> = dao.observeAll()

    suspend fun add(content: String) {
        val normalized = content.trim()
        if (normalized.isBlank()) return

        val now = System.currentTimeMillis()
        val existing = dao.findByContent(normalized)
        if (existing != null) {
            // Re-copying an existing item refreshes it and moves it to the top.
            dao.update(
                existing.copy(
                    createdAt = now,
                    expiresAt = now + ClipboardItem.DAY_MS
                )
            )
            return
        }

        dao.insert(
            ClipboardItem(
                content = normalized,
                type = ClipboardDetector.detect(normalized),
                createdAt = now,
                expiresAt = now + ClipboardItem.DAY_MS
            )
        )
    }

    suspend fun delete(item: ClipboardItem) = dao.delete(item)

    suspend fun update(item: ClipboardItem, content: String) {
        val normalized = content.trim()
        if (normalized.isBlank()) return

        val duplicate = dao.findByContent(normalized)
        if (duplicate != null && duplicate.id != item.id) {
            // Never leave two history entries with the same content.
            val now = System.currentTimeMillis()
            dao.update(
                duplicate.copy(
                    createdAt = now,
                    expiresAt = now + ClipboardItem.DAY_MS,
                    pinned = duplicate.pinned || item.pinned
                )
            )
            dao.delete(item)
            return
        }

        dao.update(
            item.copy(
                content = normalized,
                type = ClipboardDetector.detect(normalized)
            )
        )
    }

    suspend fun deleteExpired() = dao.deleteExpired(System.currentTimeMillis())
    suspend fun clear() = dao.deleteAll()
}
