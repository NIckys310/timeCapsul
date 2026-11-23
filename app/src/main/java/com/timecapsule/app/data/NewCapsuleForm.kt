package com.timecapsule.app.data

import com.timecapsule.app.util.NearbyPlace
import java.time.Instant
import java.time.ZonedDateTime
import kotlin.math.abs

/* =========================================================
   REGLAS PARA CREAR UNA CÁPSULA (sin pantalla: se pueden probar)
   ========================================================= */

/** Lo que la persona llenó en "Nueva cápsula" y que hay que revisar antes de sellarla. */
data class CapsuleForm(
    val title: String = "",
    val letter: String = "",
    val media: Int = 0,                 // fotos + videos elegidos
    val useDate: Boolean = true,
    val opensOn: Instant? = null,
    val usePlace: Boolean = true,
    val onlyAtPlace: Boolean = false,
    val hasLocation: Boolean = false,
    val useKey: Boolean = false,
    val key: String = "",
    val repeatedKey: String = "",
)

object CapsuleRules {
    const val MAX_PHOTOS = 20
    const val MAX_VIDEOS = 1
    const val MAX_VOICE_NOTES = 5
    const val MAX_QUESTIONS = 5
    const val MIN_KEY_LENGTH = 4
    const val VOICE_NOTE_MAX_MS = 180_000L

    /** Devuelve el problema a corregir (en español sencillo) o null si se puede sellar. */
    fun validate(f: CapsuleForm, now: Instant = Instant.now()): String? = when {
        f.title.isBlank() -> "Ponle un título a tu cápsula."
        f.letter.isBlank() && f.media == 0 -> "Escribe una carta o agrega fotos o videos."
        !f.useDate && !(f.usePlace && f.onlyAtPlace) -> "Elige una fecha o activa “Solo se abre en este lugar”."
        f.usePlace && !f.hasLocation -> "Espera a que se registre tu ubicación o apaga “Guardar el lugar”."
        f.useDate && f.opensOn?.isBefore(now) == true -> "La fecha de apertura debe ser en el futuro."
        f.useKey && f.key.trim().length < MIN_KEY_LENGTH -> "La clave debe tener al menos $MIN_KEY_LENGTH caracteres."
        f.useKey && f.key != f.repeatedKey -> "Las dos claves no coinciden."
        else -> null
    }

    /** ¿Cabe otro medio de este tipo? Máximo 20 fotos y 1 video por cápsula. */
    fun fits(type: String, photos: Int, videos: Int): Boolean = when (type) {
        "photo" -> photos < MAX_PHOTOS
        "video" -> videos < MAX_VIDEOS
        else -> false
    }

    /** "mama@correo.com, amigo@correo.com" → lista de correos válidos (separados por coma, punto y coma, espacio o línea). */
    fun emails(text: String): List<String> =
        text.split(',', ';', ' ', '\n').map { it.trim() }.filter { it.contains('@') && !it.startsWith('@') && !it.endsWith('@') }.distinct()

    /** Busca un enlace de YouTube o Spotify en lo que copiaste (para pegarlo solo al volver). */
    fun songLinkOf(text: String?): String? =
        Regex("""https?://\S*(youtube\.com|youtu\.be|spotify\.com)\S*""").find(text.orEmpty())?.value

    /** El nombre del lugar: el sitio con nombre donde estás (a 35 m o menos) o la primera parte de la dirección. */
    fun suggestedPlace(nearbyPlaces: List<NearbyPlace>, address: String?): String =
        nearbyPlaces.firstOrNull { it.distance <= 35 }?.name ?: address?.substringBefore(",").orEmpty()

    /** Botones rápidos de fecha: 1 mes, 6 meses, 1 año, 5 años y 10 años. */
    val dateOptions = listOf("1 mes" to 1L, "6 meses" to 6L, "1 año" to 12L, "5 años" to 60L, "10 años" to 120L)

    fun dateIn(months: Long, since: ZonedDateTime = ZonedDateTime.now()): Instant = since.plusMonths(months).toInstant()

    /** El botón de fecha rápida se ve marcado si la fecha elegida coincide (con 2 minutos de margen). */
    fun sameDate(a: Instant?, b: Instant): Boolean = a != null && abs(a.epochSecond - b.epochSecond) < 120

    /** Aplica una plantilla: si la carta está vacía la reemplaza; si no, la pone arriba. */
    fun withTemplate(letter: String, template: String): String = if (letter.isBlank()) template else template + "\n\n" + letter

    /** Agrega una pregunta propia (sin repetir, máximo 5). */
    fun addQuestion(questions: List<String>, newOne: String): List<String> {
        val q = newOne.trim()
        return if (q.isBlank() || q in questions || questions.size >= MAX_QUESTIONS) questions else questions + q
    }
}
