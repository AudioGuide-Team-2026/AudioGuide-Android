package com.project.audioguide.data.database

import androidx.room.TypeConverter
import com.project.audioguide.domain.model.Location

class LocationConverter {
    @TypeConverter
    fun fromLocation(location: Location): String {
        return "${location.latitude}, ${location.longitude}"
    }

    @TypeConverter
    fun toLocation(data: String): Location {
        val parts = data.split(",")
        val latitude = parts[0].trim().toDouble()
        val longitude = parts[1].trim().toDouble()
        return Location(latitude, longitude)
    }
}