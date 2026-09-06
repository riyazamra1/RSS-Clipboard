package com.riyaz.rssclipboard.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter fun fromType(value: ClipboardType): String = value.name
    @TypeConverter fun toType(value: String): ClipboardType = ClipboardType.valueOf(value)
}

@Database(entities = [ClipboardItem::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clipboardDao(): ClipboardDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "rss_clipboard.db"
            ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
        }
    }
}
