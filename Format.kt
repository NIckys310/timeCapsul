package com.timecapsule.app.util

import com.timecapsule.app.data.Capsule
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val es = Locale("es", "CO")
private val dateFmt = DateTimeFormatter.ofPattern("d MMM yyyy", es)
private val dateTimeFmt = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", es)
private val monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy", es)

fun Instant.fmtDate(): String = atZone(ZoneId.systemDefault()).format(dateFmt)
fun Instant.fmtDateTime(): String = atZone(ZoneId.systemDefault()).format(dateTimeFmt)
fun Instant.fmtMonth(): String = atZone(ZoneId.systemDefault()).format(monthFmt).replaceFirstChar { it.uppercase() }

/** "3 años", "5 días", "2 h 10 min"… */
fun humanUntil(target: Instant, now: Instant = Instant.now()): String {
    val d = Duration.between(now, target)
    if (d.isNegative || d.isZero) return "ya"
    val days = d.toDays()
    return when {
        days >= 730 -> "${days / 365} años"
        days >= 60 -> "${days / 30} meses"
        days >= 2 -> "$days días"
        d.toHours() >= 1 -> "${d.toHours()} h ${d.toMinutes() % 60} min"
        else -> "${d.toMinutes().coerceAtLeast(1)} min"
    }
}

/** Texto corto con la condición de apertura. */
fun Capsule.conditionText(): String {
    if (isOpen) return "Abierta"
    val parts = mutableListOf<String>()
    unlockInstant?.let { if (!dateReady) parts += "Se abre el ${it.fmtDate()} (faltan ${humanUntil(it)})" }
    if (requirePlace) parts += if (dateReady) "Ve a ${placeName ?: "su lugar"} para abrirla" else "y estando en ${placeName ?: "su lugar"}"
    if (hasKey) parts += "· o con tu clave"
    return parts.joinToString(" ").ifBlank { "Cerrada" }
}
