package com.riyaz.rssclipboard.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipboardDao {
    @Query("SELECT * FROM clipboard_items ORDER BY pinned DESC, createdAt DESC")
    fun observeAll(): Flow<List<ClipboardItem>>

    @Query("SELECT * FROM clipboard_items WHERE content = :content LIMIT 1")
    suspend fun findByContent(content: String): ClipboardItem?

    @Insert
    suspend fun insert(item: ClipboardItem)

    @Delete
    suspend fun delete(item: ClipboardItem)

    @Update
    suspend fun update(item: ClipboardItem)

    @Query("DELETE FROM clipboard_items WHERE expiresAt < :now AND pinned = 0")
    suspend fun deleteExpired(now: Long)

    @Query("DELETE FROM clipboard_items")
    suspend fun deleteAll()
}
