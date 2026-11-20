package com.project.audioguide.domain

import kotlin.math.abs

class SightDetector {
    private val searchRadiusMeters = 50f // Радиус поиска 50 метров
    private val angleThreshold = 15f     // Погрешность наведения (15 градусов)

    // Вычисление расстояния
    fun calculateDistance(
        userLat: Double, userLon: Double,
        sightLat: Double, sightLon: Double
    ): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(userLat, userLon, sightLat, sightLon, results)
        return results[0]
    }

    // Вычисление направления (азимута) на объект
    fun calculateBearing(
        userLat: Double, userLon: Double,
        sightLat: Double, sightLon: Double
    ): Float {
        val results = FloatArray(2)
        android.location.Location.distanceBetween(userLat, userLon, sightLat, sightLon, results)
        return results[1]
    }

    // Разница углов (учитываем переход через 360 градусов)
    fun getAngleDifference(compassAzimuth: Float, sightBearing: Float): Float {
        val diff = abs(compassAzimuth - sightBearing) % 360f
        return if (diff > 180f) 360f - diff else diff
    }

    // Главная проверка: смотрим ли мы на объект?
    fun isLookingAtSight(
        userLat: Double, userLon: Double, compassAzimuth: Float,
        sightLat: Double, sightLon: Double
    ): Boolean {
        val bearing = calculateBearing(userLat, userLon, sightLat, sightLon)
        return getAngleDifference(compassAzimuth, bearing) <= angleThreshold
    }
}