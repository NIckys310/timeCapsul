package com.timecapsule.app

import com.timecapsule.app.data.Capsule
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/** Cápsulas de ejemplo para las pruebas. */
object SampleCapsules {
    fun iso(i: Instant): String = i.atOffset(ZoneOffset.UTC).toString()
    fun inDays(days: Long): String = iso(Instant.now().plus(days, ChronoUnit.DAYS))

    fun newOne(
        opensIn: String? = null,
        place: Boolean = false,
        requiresPlace: Boolean = false,
        placeNameText: String? = null,
        key: Boolean = false,
        openCapsule: Boolean = false,
    ) = Capsule(
        id = "c-1", owner = "u-1", title = "Para mi yo de 2030", createdAt = "2026-10-10T12:00:00Z",
        unlockAt = opensIn, lat = if (place) 4.6 else null, lng = if (place) -74.08 else null,
        placeName = placeNameText, requirePlace = requiresPlace, hasKey = key, isOpen = openCapsule,
    )
}
