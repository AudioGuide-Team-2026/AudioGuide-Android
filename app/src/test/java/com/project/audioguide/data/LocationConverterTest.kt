package com.project.audioguide.data

import com.project.audioguide.data.database.LocationConverter
import com.project.audioguide.domain.model.Location
import org.junit.Assert.assertEquals
import org.junit.Test

class LocationConverterTest {

    private val converter = LocationConverter()

    @Test
    fun fromLocation_formatsAsString() {
        assertEquals("59.956363, 30.310011", converter.fromLocation(Location(59.956363, 30.310011)))
    }

    @Test
    fun toLocation_parsesString() {
        assertEquals(Location(59.956363, 30.310011), converter.toLocation("59.956363, 30.310011"))
    }

    @Test
    fun toLocation_ignoresExtraSpaces() {
        assertEquals(Location(1.5, -2.5), converter.toLocation("  1.5 ,   -2.5 "))
    }

    @Test
    fun roundTrip_keepsValue() {
        val original = Location(-33.8688, 151.2093)
        assertEquals(original, converter.toLocation(converter.fromLocation(original)))
    }
}
