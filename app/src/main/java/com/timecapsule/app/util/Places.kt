package com.timecapsule.app.util

import android.content.Context
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

/** Un lugar real cercano (restaurante, parque, colegio…) sacado de OpenStreetMap. */
data class NearbyPlace(val name: String, val kind: String, val distance: Int, val lat: Double, val lng: Double)

/**
 * Nombres de lugares 100 % reales desde OpenStreetMap:
 *  - address(): la dirección exacta del punto (calle, número, barrio, ciudad)
 *  - nearby(): negocios y sitios con nombre a menos de 120 m
 */
object Places {
    private const val UA = "TimeCapsuleApp/1.0 (app educativa Android)"
    private val json = Json { ignoreUnknownKeys = true }

    private fun get(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.setRequestProperty("User-Agent", UA)
        c.setRequestProperty("Accept-Language", "es")
        c.connectTimeout = 10_000
        c.readTimeout = 15_000
        return c.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }.also { c.disconnect() }
    }

    private fun JsonObject.str(k: String) = this[k]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }

    /** Dirección real del punto. Si internet falla, usa el geocodificador del celular. */
    suspend fun address(ctx: Context, lat: Double, lng: Double): String? = withContext(Dispatchers.IO) {
        runCatching {
            val o = json.parseToJsonElement(
                get("https://nominatim.openstreetmap.org/reverse?format=jsonv2&zoom=18&addressdetails=1&lat=$lat&lon=$lng")
            ).jsonObject
            val a = o["address"]?.jsonObject
            val street = listOfNotNull(a?.str("road"), a?.str("house_number")).joinToString(" ").ifBlank { null }
            val area = a?.str("neighbourhood") ?: a?.str("suburb") ?: a?.str("quarter")
            val city = a?.str("city") ?: a?.str("town") ?: a?.str("village") ?: a?.str("municipality")
            val parts = listOfNotNull(o.str("name"), street, area, city).distinct()
            parts.joinToString(", ").ifBlank { o.str("display_name") }
        }.getOrNull() ?: runCatching {
            @Suppress("DEPRECATION")
            Geocoder(ctx, Locale("es")).getFromLocation(lat, lng, 1)?.firstOrNull()?.getAddressLine(0)
        }.getOrNull()
    }

    /** Sitios con nombre alrededor (máx. 120 m), del más cercano al más lejano. */
    suspend fun nearby(lat: Double, lng: Double, radius: Int = 120): List<NearbyPlace> = withContext(Dispatchers.IO) {
        runCatching {
            val q = """[out:json][timeout:12];nwr(around:$radius,$lat,$lng)[name][~"^(amenity|shop|tourism|leisure|historic|office|building)$"~"."];out center 25;"""
            val body = get("https://overpass-api.de/api/interpreter?data=" + URLEncoder.encode(q, "UTF-8"))
            json.parseToJsonElement(body).jsonObject["elements"]!!.jsonArray.mapNotNull { e ->
                val o = e.jsonObject
                val tags = o["tags"]?.jsonObject ?: return@mapNotNull null
                val name = tags.str("name") ?: return@mapNotNull null
                val pLat = o["lat"]?.jsonPrimitive?.doubleOrNull ?: o["center"]?.jsonObject?.get("lat")?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
                val pLng = o["lon"]?.jsonPrimitive?.doubleOrNull ?: o["center"]?.jsonObject?.get("lon")?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
                val kind = kindEs(tags.str("amenity") ?: tags.str("shop") ?: tags.str("tourism") ?: tags.str("leisure") ?: tags.str("historic") ?: "")
                NearbyPlace(name, kind, LocationUtil.distanceMeters(lat, lng, pLat, pLng).toInt(), pLat, pLng)
            }.distinctBy { it.name.lowercase() }.sortedBy { it.distance }.take(10)
        }.getOrDefault(emptyList())
    }

    private fun kindEs(k: String) = when (k) {
        "restaurant" -> "Restaurante"; "cafe" -> "Café"; "fast_food" -> "Comida rápida"; "bar", "pub" -> "Bar"
        "ice_cream" -> "Heladería"; "bakery" -> "Panadería"; "school" -> "Colegio"; "university", "college" -> "Universidad"
        "park" -> "Parque"; "place_of_worship" -> "Iglesia"; "hospital", "clinic" -> "Salud"; "cinema" -> "Cine"
        "supermarket", "mall" -> "Tienda"; "hotel", "hostel" -> "Hotel"; "museum" -> "Museo"; "attraction", "viewpoint" -> "Atracción"
        "stadium", "sports_centre", "pitch" -> "Deportes"; "pharmacy" -> "Farmacia"; "bank" -> "Banco"
        "" -> "Lugar"; else -> k.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }
}
