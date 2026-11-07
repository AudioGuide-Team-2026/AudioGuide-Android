package com.project.audioguide.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [SightEntity::class],
    version = 1,
    autoMigrations = []
)
@TypeConverters(LocationConverter::class)
abstract class SightDatabase : RoomDatabase() {
    abstract fun sightDao(): SightDao

    companion object {
        @Volatile
        private var INSTANCE: SightDatabase? = null

        fun getInstance(context: Context): SightDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.inMemoryDatabaseBuilder(
                    context.applicationContext,
                    SightDatabase::class.java
                )
                    .build()
                    .also { INSTANCE = it }
            }
        }

        @Synchronized
        fun reset() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}