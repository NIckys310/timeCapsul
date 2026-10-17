package com.timecapsule.app.util

import java.util.Locale

/* =========================================================
   "Caliente o frío": pistas para encontrar una cápsula
   guardada en un lugar (sin pantalla, para poder probarlas).
   ========================================================= */

enum class Pista(val texto: String) {
    BUSCANDO("Buscando tu ubicación…"),
    LLEGASTE("🎉 ¡Llegaste! Ya puedes intentar abrirla"),
    MUY_CALIENTE("🔥 ¡Muy caliente!"),
    CALIENTE("♨️ Caliente"),
    TIBIO("🌤 Tibio"),
    FRIO("❄️ Frío"),
}

/** Qué tan cerca estás de la cápsula (distancia en metros; radio = dónde se puede abrir). */
fun pistaCalienteFrio(distanciaM: Float?, radioM: Int): Pista = when {
    distanciaM == null -> Pista.BUSCANDO
    distanciaM <= radioM -> Pista.LLEGASTE
    distanciaM < 200 -> Pista.MUY_CALIENTE
    distanciaM < 1000 -> Pista.CALIENTE
    distanciaM < 5000 -> Pista.TIBIO
    else -> Pista.FRIO
}

/** Si te estás acercando o alejando (ignora cambios de menos de 5 m, que son ruido del GPS). */
fun tendencia(actualM: Float, anteriorM: Float?): String = when {
    anteriorM == null -> ""
    actualM < anteriorM - 5 -> "Vas por buen camino ↓"
    actualM > anteriorM + 5 -> "Te estás alejando ↑"
    else -> "Sigue moviéndote"
}

/** 350 m · 1.2 km */
fun distanciaTexto(metros: Float): String =
    if (metros >= 1000) "%.1f km".format(Locale.US, metros / 1000) else "${metros.toInt()} m"
