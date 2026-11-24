package com.project.audioguide.domain

import com.project.audioguide.domain.model.Location
import com.project.audioguide.domain.model.Sight
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

sealed interface DetectionResult {
    data object NothingNearby : DetectionResult
    data class NearbyNotLooking(val sight: Sight, val distanceMeters: Float) : DetectionResult
    data class LookingAt(val sight: Sight, val distanceMeters: Float) : DetectionResult
}

class SightDetector @Inject constructor() {
    private val searchRadiusMeters = 50f // Радиус поиска 50 метров
    private val angleThreshold = 15f     // Погрешность наведения (15 градусов)
    private val earthRadiusMeters = 6_371_000.0

    // Вычисление расстояния (формула гаверсинусов)
    fun calculateDistance(
        userLat: Double, userLon: Double,
        sightLat: Double, sightLon: Double
    ): Float {
        val dLat = Math.toRadians(sightLat - userLat)
        val dLon = Math.toRadians(sightLon - userLon)
        val lat1Rad = Math.toRadians(userLat)
        val lat2Rad = Math.toRadians(sightLat)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(lat1Rad) * cos(lat2Rad) * sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (earthRadiusMeters * c).toFloat()
    }

    // Вычисление направления (азимута) на объект в диапазоне [0, 360)
    fun calculateBearing(
        userLat: Double, userLon: Double,
        sightLat: Double, sightLon: Double
    ): Float {
        val lat1Rad = Math.toRadians(userLat)
        val lat2Rad = Math.toRadians(sightLat)
        val dLon = Math.toRadians(sightLon - userLon)

        val y = sin(dLon) * cos(lat2Rad)
        val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(dLon)
        val bearing = Math.toDegrees(atan2(y, x)).toFloat()
        return (bearing + 360f) % 360f
    }

    // Разница углов (учитываем переход через 360 градусов)
    fun getAngleDifference(compassAzimuth: Float, sightBearing: Float): Float {
        val normAzimuth = ((compassAzimuth % 360f) + 360f) % 360f
        val normBearing = ((sightBearing % 360f) + 360f) % 360f
        val diff = abs(normAzimuth - normBearing)
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

    // Поиск ближайшего объекта и проверка направления
    fun detect(
        userLocation: Location,
        compassAzimuth: Float,
        sights: List<Sight>
    ): DetectionResult {
        var nearestSight: Sight? = null
        var minDistance = Float.MAX_VALUE

        for (sight in sights) {
            val dist = calculateDistance(
                userLocation.latitude, userLocation.longitude,
                sight.location.latitude, sight.location.longitude
            )
            if (dist <= searchRadiusMeters && dist < minDistance) {
                nearestSight = sight
                minDistance = dist
            }
        }

        val sight = nearestSight ?: return DetectionResult.NothingNearby

        return if (isLookingAtSight(
                userLocation.latitude, userLocation.longitude, compassAzimuth,
                sight.location.latitude, sight.location.longitude
            )
        ) {
            DetectionResult.LookingAt(sight, minDistance)
        } else {
            DetectionResult.NearbyNotLooking(sight, minDistance)
        }
    }
}