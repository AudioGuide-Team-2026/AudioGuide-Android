package com.project.audioguide.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class LocationService(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private val tag = "LocationService"

    private var locationCallback: LocationCallback? = null
    private var locationRequest: LocationRequest? = null

    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requiredPermissions(): Array<String> = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    // Запуск постоянного прослушивания GPS
    fun startLocationUpdates(onLocation: (Location) -> Unit, onError: (Exception) -> Unit) {
        if (!hasPermission()) return

        // Настраиваем частоту обновлений: каждые 5 секунд (5000 мс)
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
            .setMinUpdateIntervalMillis(2000L) // Минимум 2 секунды между обновлениями
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation
                if (location != null) {
                    onLocation(location)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest!!,
                locationCallback!!,
                Looper.getMainLooper()
            )
            Log.d(tag, "Слушатель GPS запущен")
        } catch (e: SecurityException) {
            Log.e(tag, "SecurityException при запуске GPS", e)
            onError(e)
        }
    }

    // Остановка прослушивания (для экономии батареи)
    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
            Log.d(tag, "Слушатель GPS остановлен")
        }
    }
}