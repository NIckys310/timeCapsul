package com.timecapsule.app.util

import com.timecapsule.app.data.Capsule
import com.timecapsule.app.ui.Idioma
import com.timecapsule.app.ui.tr
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val es = Locale("es", "CO")
private val ingles get() = Idioma.codigo == "en"
private val local: Locale get() = if (ingles) Locale.ENGLISH else es
private val dateFmt get() = DateTimeFormatter.ofPattern(if (ingles) "MMM d, yyyy" else "d MMM yyyy", local)
private val dateTimeFmt get() = DateTimeFormatter.ofPattern(if (ingles) "MMM d, yyyy, h:mm a" else "d MMM yyyy, h:mm a", local)
private val monthFmt get() = DateTimeFormatter.ofPattern("MMMM yyyy", local)

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
    return parts.joinToString(" ") { tr(it) }.ifBlank { "Cerrada" }
}
