package com.project.audioguide.domain

import com.project.audioguide.domain.model.Location
import com.project.audioguide.domain.model.Sight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SightDetectorTest {

    private val detector = SightDetector()

    private val itmoMain = Sight(
        id = 1,
        address = "Кронверкский пр., 49",
        location = Location(59.956363, 30.310011),
        name = "Главный корпус ИТМО",
        description = "Описание"
    )

    @Test
    fun calculateDistance_shortDistance_isAccurate() {
        val distance = detector.calculateDistance(
            59.956363, 30.310011,
            59.956543, 30.310011 // ~20 метров на север
        )
        assertEquals(20f, distance, 2f)
    }

    @Test
    fun calculateDistance_betweenBuildings_isExpected() {
        val distance = detector.calculateDistance(
            59.956363, 30.310011, // Кронверкский
            59.927288, 30.338353  // Ломоносова
        )
        assertTrue(distance in 3500f..3700f)
    }

    @Test
    fun calculateBearing_cardinalDirections_areCorrect() {
        val north = detector.calculateBearing(59.956363, 30.310011, 59.956543, 30.310011)
        val east = detector.calculateBearing(59.956363, 30.310011, 59.956363, 30.310500)
        val south = detector.calculateBearing(59.956363, 30.310011, 59.956183, 30.310011)
        val west = detector.calculateBearing(59.956363, 30.310011, 59.956363, 30.309500)

        assertEquals(0f, north, 0.5f)
        assertEquals(90f, east, 0.5f)
        assertEquals(180f, south, 0.5f)
        assertEquals(270f, west, 0.5f)
    }

    @Test
    fun getAngleDifference_wrapAround360_isCorrect() {
        assertEquals(10f, detector.getAngleDifference(355f, 5f), 0.01f)
        assertEquals(10f, detector.getAngleDifference(5f, 355f), 0.01f)
    }

    @Test
    fun detect_returnsCorrectStates() {
        val userNearSouth = Location(59.956183, 30.310011) // 20м южнее ИТМО -> азимут на него примерно 0°
        val userFarAway = Location(59.927288, 30.338353)   // примерно 3.6 км

        assertEquals(
            DetectionResult.NothingNearby,
            detector.detect(userFarAway, 0f, listOf(itmoMain))
        )

        val notLooking = detector.detect(userNearSouth, 120f, listOf(itmoMain))
        assertTrue(notLooking is DetectionResult.NearbyNotLooking)

        val looking = detector.detect(userNearSouth, 355f, listOf(itmoMain))
        assertTrue(looking is DetectionResult.LookingAt)
    }
}