package com.stackbleedctrl.pollen.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat

data class LocationSnapshot(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val provider: String,
    val ageMs: Long
) {
    fun toDisplayString(): String {
        val accuracy = accuracyMeters?.let { "${it.toInt()}m" } ?: "unknown"
        return "Lat=$latitude, Lng=$longitude, Accuracy=$accuracy, Provider=$provider, Age=${ageMs}ms"
    }
}

class LocationSnapshotProvider(
    private val context: Context
) {
    fun getLastKnownLocation(): LocationSnapshot? {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) return null

        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )

        val bestLocation: Location = providers
            .mapNotNull { provider ->
                try {
                    manager.getLastKnownLocation(provider)
                } catch (_: SecurityException) {
                    null
                } catch (_: Exception) {
                    null
                }
            }
            .maxByOrNull { it.time }
            ?: return null

        return LocationSnapshot(
            latitude = bestLocation.latitude,
            longitude = bestLocation.longitude,
            accuracyMeters = bestLocation.accuracy.takeIf { it > 0f },
            provider = bestLocation.provider ?: "unknown",
            ageMs = System.currentTimeMillis() - bestLocation.time
        )
    }
}
