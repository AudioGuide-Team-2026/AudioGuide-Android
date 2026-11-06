package com.project.audioguide.data.database

import androidx.room.Database
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

        fun getInstance(context: android.content.Context): SightDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    SightDatabase::class.java,
                    "sight_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}