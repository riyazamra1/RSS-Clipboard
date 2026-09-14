package com.riyaz.rssclipboard.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedDao {
    @Query("SELECT * FROM saved_items ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<SavedItem>>

    @Insert
    suspend fun insert(item: SavedItem)

    @Update
    suspend fun update(item: SavedItem)

    @Delete
    suspend fun delete(item: SavedItem)

    @Query("DELETE FROM saved_items")
    suspend fun deleteAll()
}
