package com.project.audioguide.data.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.project.audioguide.domain.model.Location
import com.project.audioguide.domain.service.LocationService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class LocationServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : LocationService {

    // Создаем клиента прямо тут, чтобы Hilt не ругался
    private val fusedLocationProviderClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    override fun getLocation(): Flow<Location> = callbackFlow {
        // 1. Проверяем разрешения (точное или приблизительное)
        val hasFine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            close(SecurityException("Нет разрешения на GPS"))
            return@callbackFlow
        }

        // 2. Настраиваем запрос (каждые 5 секунд, минимум раз в 2 секунды)
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
            .setMinUpdateIntervalMillis(2000L)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { androidLocation ->
                    // Маппим Android Location в domain-модель и отправляем в MainViewModel
                    trySend(
                        Location(
                            latitude = androidLocation.latitude,
                            longitude = androidLocation.longitude
                        )
                    )
                }
            }
        }

        // 3. Запускаем
        try {
            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest, locationCallback, Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            close(e)
            return@callbackFlow
        }

        // 4. Останавливаем, когда ViewModel перестает слушать
        awaitClose {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        }
    }
}