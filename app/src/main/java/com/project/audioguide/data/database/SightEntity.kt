package com.project.audioguide.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.project.audioguide.domain.model.Location
import com.project.audioguide.domain.model.Sight

@Entity(tableName = "sights")
data class SightEntity(
    @PrimaryKey(autoGenerate = true) val sightId: Int = 0,
    @ColumnInfo(name = "address") val address: String,
    @ColumnInfo(name = "location") val location: Location,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "description") val description: String
)

fun SightEntity.toDomain(): Sight = Sight(
    id = sightId,
    address = address,
    location = location,
    name = name,
    description = description
)