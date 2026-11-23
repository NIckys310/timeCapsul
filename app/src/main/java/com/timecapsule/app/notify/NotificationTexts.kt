package com.timecapsule.app.notify

import com.timecapsule.app.data.Activity
import com.timecapsule.app.data.Capsule
import java.time.Duration

/* =========================================================
   TEXTOS Y REGLAS DE LOS AVISOS (sin Android: se pueden probar)
   Los Workers y Sync los usan para mostrar las notificaciones.
   ========================================================= */
object NotificationTexts {
    /** Cuenta regresiva: se avisa 1 semana, 1 día y 1 hora antes de que se abra. */
    val COUNTDOWN = listOf(Duration.ofDays(7) to "1 semana", Duration.ofDays(1) to "1 día", Duration.ofHours(1) to "1 hora")

    /** Cuánto esperar para cada aviso de cuenta regresiva (los que ya pasaron no se programan). */
    fun reminders(missingItems: Duration): List<Pair<Duration, String>> =
        if (missingItems.isNegative) emptyList()
        else COUNTDOWN.mapNotNull { (before, text) -> missingItems.minus(before).takeUnless { it.isNegative }?.let { it to text } }

    /** El lugar al que hay que ir, si la cápsula solo se abre allá. */
    fun placeToOpen(c: Capsule): String? = if (c.requirePlace) c.placeName ?: "su lugar" else null

    fun openingTitle(title: String) = "¡“$title” se puede abrir!"

    fun openingText(place: String?) =
        if (place != null) "Ya llegó la fecha. Ve a $place para abrirla." else "Ya puedes abrirla y revivir lo que guardaste."

    fun reminderTitle(howMuch: String) = "⏳ Falta $howMuch"
    fun reminderText(title: String) = "Tu cápsula “$title” se abrirá pronto."

    /** Cápsulas que alguien te acaba de compartir (no las conocías y no son tuyas). */
    fun newShared(list: List<Capsule>, knownOnes: Set<String>, me: String?): List<Capsule> =
        list.filter { it.id !in knownOnes && it.owner != me }

    fun sharedTitle(c: Capsule) = "${c.ownerName ?: "Alguien"} compartió una cápsula contigo"
    fun sharedText(c: Capsule) = "“${c.title}”. ${if (c.isOpen) "Ya puedes verla." else "Te avisaremos cuando se abra."}"

    /** "Ana comentó: ¡qué bonito!" o "Ana reaccionó ❤️" */
    fun activityText(a: Activity) =
        if (a.kind == "comment") "${a.who ?: "Alguien"} comentó: ${a.body}" else "${a.who ?: "Alguien"} reaccionó ${a.body}"

    /** Máximo 5 avisos de actividad por revisión, para no llenar el celular. */
    const val MAX_ACTIVITY = 5
}
