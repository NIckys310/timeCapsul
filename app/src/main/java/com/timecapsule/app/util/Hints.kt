package com.timecapsule.app.util

import java.util.Locale

/* =========================================================
   "Caliente o frío": pistas para encontrar una cápsula
   guardada en un lugar (sin pantalla, para poder probarlas).
   ========================================================= */

enum class Hint(val text: String) {
    BUSCANDO("Buscando tu ubicación…"),
    LLEGASTE("🎉 ¡Llegaste! Ya puedes intentar abrirla"),
    MUY_CALIENTE("🔥 ¡Muy caliente!"),
    CALIENTE("♨️ Caliente"),
    TIBIO("🌤 Tibio"),
    FRIO("❄️ Frío"),
}

/** Qué tan cerca estás de la cápsula (distancia en metros; radio = dónde se puede abrir). */
fun hotColdHint(distanceM: Float?, radiusM: Int): Hint = when {
    distanceM == null -> Hint.BUSCANDO
    distanceM <= radiusM -> Hint.LLEGASTE
    distanceM < 200 -> Hint.MUY_CALIENTE
    distanceM < 1000 -> Hint.CALIENTE
    distanceM < 5000 -> Hint.TIBIO
    else -> Hint.FRIO
}

/** Si te estás acercando o alejando (ignora cambios de menos de 5 m, que son ruido del GPS). */
fun trend(currentMeters: Float, previousMeters: Float?): String = when {
    previousMeters == null -> ""
    currentMeters < previousMeters - 5 -> "Vas por buen camino ↓"
    currentMeters > previousMeters + 5 -> "Te estás alejando ↑"
    else -> "Sigue moviéndote"
}

/** 350 m · 1.2 km */
fun distanceText(meters: Float): String =
    if (meters >= 1000) "%.1f km".format(Locale.US, meters / 1000) else "${meters.toInt()} m"
