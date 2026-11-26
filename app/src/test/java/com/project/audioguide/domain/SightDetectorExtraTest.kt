package com.project.audioguide.domain

import com.project.audioguide.domain.model.Location
import com.project.audioguide.domain.model.Sight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureTimeMillis

class SightDetectorExtraTest {

    private val detector = SightDetector()

    private fun sight(id: Int, lat: Double, lon: Double) = Sight(
        id = id,
        address = "Адрес $id",
        location = Location(lat, lon),
        name = "Объект $id",
        description = "Описание"
    )

    // Пользователь в точке (lat0, lon0), объекты расположены к северу
    private val user = Location(59.956363, 30.310011)

    @Test
    fun detect_emptyList_nothingNearby() {
        assertEquals(DetectionResult.NothingNearby, detector.detect(user, 0f, emptyList()))
    }

    @Test
    fun detect_choosesNearestSight() {
        val near = sight(1, 59.956363 + 0.00018, 30.310011)  // ~20 м
        val farther = sight(2, 59.956363 + 0.00036, 30.310011) // ~40 м

        val result = detector.detect(user, 0f, listOf(farther, near))

        assertTrue(result is DetectionResult.LookingAt)
        assertEquals(near, (result as DetectionResult.LookingAt).sight)
    }

    @Test
    fun detect_sightOutsideRadius_isIgnored() {
        val outside = sight(1, 59.956363 + 0.00054, 30.310011) // ~60 м

        assertEquals(DetectionResult.NothingNearby, detector.detect(user, 0f, listOf(outside)))
    }

    @Test
    fun detect_sightInsideRadius_isFound() {
        val inside = sight(1, 59.956363 + 0.00036, 30.310011) // ~40 м

        assertTrue(detector.detect(user, 0f, listOf(inside)) is DetectionResult.LookingAt)
    }

    @Test
    fun isLookingAtSight_withinThreshold_true() {
        // Объект строго на севере, азимут 14 градусов (порог 15)
        assertTrue(detector.isLookingAtSight(59.956363, 30.310011, 14f, 59.956543, 30.310011))
        assertTrue(detector.isLookingAtSight(59.956363, 30.310011, 346f, 59.956543, 30.310011))
    }

    @Test
    fun isLookingAtSight_outsideThreshold_false() {
        assertFalse(detector.isLookingAtSight(59.956363, 30.310011, 20f, 59.956543, 30.310011))
        assertFalse(detector.isLookingAtSight(59.956363, 30.310011, 180f, 59.956543, 30.310011))
    }

    @Test
    fun getAngleDifference_handlesNegativeAndLargeAngles() {
        assertEquals(20f, detector.getAngleDifference(-10f, 10f), 0.01f)
        assertEquals(10f, detector.getAngleDifference(370f, 0f), 0.01f)
        assertEquals(180f, detector.getAngleDifference(0f, 180f), 0.01f)
    }

    @Test
    fun calculateDistance_samePoint_isZero() {
        assertEquals(0f, detector.calculateDistance(59.9, 30.3, 59.9, 30.3), 0.001f)
    }

    // Нефункциональное требование: результат не позднее чем через 3 секунды
    @Test
    fun detect_manySights_fasterThanThreeSeconds() {
        val many = (1..10_000).map { sight(it, 59.0 + it * 0.0001, 30.0 + it * 0.0001) }

        val time = measureTimeMillis { detector.detect(user, 0f, many) }

        assertTrue("detect занял $time мс", time < 3000)
    }
}
