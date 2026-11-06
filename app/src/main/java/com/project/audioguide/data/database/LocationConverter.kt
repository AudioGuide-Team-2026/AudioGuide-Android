package com.project.audioguide.data.database

import androidx.room.TypeConverter
import com.project.audioguide.domain.service.models.Location

class LocationConverter {
    @TypeConverter
    fun fromLocation(location: Location): String {
        return "${location.latitude}, ${location.longitude}"
    }

    @TypeConverter
    fun toLocation(data: String): Location {
        val parts = data.split(",")
        val latitude = parts[0].toDouble()
        val longitude = parts[1].toDouble()
        return Location(latitude, longitude)
    }
}