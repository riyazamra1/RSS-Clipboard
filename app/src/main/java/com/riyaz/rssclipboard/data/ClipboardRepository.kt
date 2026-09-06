package com.riyaz.rssclipboard.data

import kotlinx.coroutines.flow.Flow

class ClipboardRepository(private val dao: ClipboardDao) {
    val items: Flow<List<ClipboardItem>> = dao.observeAll()
    suspend fun add(content: String) {
        if (content.isBlank()) return
        dao.insert(ClipboardItem(content = content.trim(), type = ClipboardDetector.detect(content)))
    }
    suspend fun delete(item: ClipboardItem) = dao.delete(item)
    suspend fun update(item: ClipboardItem) = dao.update(item)
    suspend fun deleteExpired() = dao.deleteExpired(System.currentTimeMillis())
    suspend fun clear() = dao.deleteAll()
}
