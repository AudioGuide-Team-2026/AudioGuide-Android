package com.project.audioguide.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

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

    @Query("DELETE FROM sqlite_sequence WHERE name = 'sights'")
    suspend fun resetSequence()

    @Transaction
    suspend fun replaceAll(sights: List<SightEntity>) {
        deleteAll()
        resetSequence()
        insertAll(sights)
    }
}