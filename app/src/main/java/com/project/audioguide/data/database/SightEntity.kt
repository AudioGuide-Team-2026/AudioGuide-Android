package com.project.audioguide.data.database


import com.project.audioguide.domain.service.models.Location
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "sights")
data class SightEntity(
    @PrimaryKey (autoGenerate = true) val sightId: Int = 0,
    @ColumnInfo(name = "address") val address: String,
    @ColumnInfo(name = "location") val location: Location,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "description") val description: String
)