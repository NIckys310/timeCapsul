package com.timecapsule.app

import com.timecapsule.app.data.Capsule
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/** Cápsulas de ejemplo para las pruebas. */
object Capsulas {
    fun iso(i: Instant): String = i.atOffset(ZoneOffset.UTC).toString()
    fun enDias(dias: Long): String = iso(Instant.now().plus(dias, ChronoUnit.DAYS))

    fun nueva(
        abreEn: String? = null,
        lugar: Boolean = false,
        exigeLugar: Boolean = false,
        nombreLugar: String? = null,
        clave: Boolean = false,
        abierta: Boolean = false,
    ) = Capsule(
        id = "c-1", owner = "u-1", title = "Para mi yo de 2030", createdAt = "2026-10-10T12:00:00Z",
        unlockAt = abreEn, lat = if (lugar) 4.6 else null, lng = if (lugar) -74.08 else null,
        placeName = nombreLugar, requirePlace = exigeLugar, hasKey = clave, isOpen = abierta,
    )
}
