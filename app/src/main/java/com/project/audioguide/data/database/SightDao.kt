package com.project.audioguide.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SightDao {
    @Insert
    suspend fun insert(sight: SightEntity)

    @Insert
    suspend fun insertAll(sights: List<SightEntity>)

    @Query("SELECT * FROM sights")
    suspend fun getAll(): List<SightEntity>

    @Query("DELETE FROM sights")
    suspend fun deleteAll()
}