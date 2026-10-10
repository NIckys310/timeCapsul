package com.timecapsule.app.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

object LocationUtil {
    fun hasFine(ctx: Context) =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    suspend fun current(ctx: Context): Location? {
        if (!hasFine(ctx)) return null
        val client = LocationServices.getFusedLocationProviderClient(ctx)
        return runCatching {
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token).await()
                ?: client.lastLocation.await()
        }.getOrNull()
    }

    /** Nombre aproximado del lugar (calle, barrio, ciudad). */
    suspend fun placeName(ctx: Context, lat: Double, lng: Double): String? = withContext(Dispatchers.IO) {
        runCatching {
            @Suppress("DEPRECATION")
            Geocoder(ctx, Locale("es")).getFromLocation(lat, lng, 1)?.firstOrNull()?.let { a ->
                listOfNotNull(a.thoroughfare ?: a.subLocality, a.locality).distinct().joinToString(", ").ifBlank { null }
            }
        }.getOrNull()
    }

    fun distanceMeters(aLat: Double, aLng: Double, bLat: Double, bLng: Double): Float {
        val out = FloatArray(1)
        Location.distanceBetween(aLat, aLng, bLat, bLng, out)
        return out[0]
    }
}
