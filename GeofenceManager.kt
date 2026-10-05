package com.timecapsule.app.geo

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.timecapsule.app.data.Capsule
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** Lo mínimo que se guarda en el celular para avisar sin internet. */
@Serializable
data class GeoInfo(
    val id: String, val title: String, val place: String?, val lat: Double, val lng: Double,
    val radius: Float, val isOpen: Boolean, val requirePlace: Boolean, val dateReady: Boolean,
)

object GeofenceManager {
    private const val PREFS = "geofences"
    private val json = Json { ignoreUnknownKeys = true }

    fun hasPermission(ctx: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val bg = Build.VERSION.SDK_INT < 29 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine && bg
    }

    fun stored(ctx: Context): List<GeoInfo> =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("list", null)?.let {
            runCatching { json.decodeFromString(ListSerializer(GeoInfo.serializer()), it) }.getOrNull()
        } ?: emptyList()

    fun update(ctx: Context, capsules: List<Capsule>) {
        val infos = capsules.filter { it.hasPlace }.take(90).map {
            GeoInfo(it.id, it.title, it.placeName, it.lat!!, it.lng!!, it.radiusM.toFloat().coerceAtLeast(100f),
                it.isOpen, it.requirePlace, it.dateReady)
        }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("list", json.encodeToString(ListSerializer(GeoInfo.serializer()), infos)).apply()
        register(ctx, infos)
    }

    fun reRegister(ctx: Context) = register(ctx, stored(ctx))

    @SuppressLint("MissingPermission")
    private fun register(ctx: Context, infos: List<GeoInfo>) {
        if (!hasPermission(ctx)) return
        val client = LocationServices.getGeofencingClient(ctx)
        val pi = pendingIntent(ctx)
        client.removeGeofences(pi).addOnCompleteListener {
            if (infos.isEmpty()) return@addOnCompleteListener
            val fences = infos.map {
                Geofence.Builder()
                    .setRequestId(it.id)
                    .setCircularRegion(it.lat, it.lng, it.radius)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                    .setLoiteringDelay(60_000)
                    .build()
            }
            val req = GeofencingRequest.Builder().setInitialTrigger(0).addGeofences(fences).build()
            client.addGeofences(req, pi)
        }
    }

    private fun pendingIntent(ctx: Context): PendingIntent =
        PendingIntent.getBroadcast(
            ctx, 0, Intent(ctx, GeofenceReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
}
