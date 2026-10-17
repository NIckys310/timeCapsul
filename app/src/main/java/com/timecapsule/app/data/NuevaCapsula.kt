package com.timecapsule.app.data

import com.timecapsule.app.util.NearbyPlace
import java.time.Instant
import java.time.ZonedDateTime
import kotlin.math.abs

/* =========================================================
   REGLAS PARA CREAR UNA CÁPSULA (sin pantalla: se pueden probar)
   ========================================================= */

/** Lo que la persona llenó en "Nueva cápsula" y que hay que revisar antes de sellarla. */
data class FormularioCapsula(
    val titulo: String = "",
    val carta: String = "",
    val medios: Int = 0,                 // fotos + videos elegidos
    val usarFecha: Boolean = true,
    val abreEl: Instant? = null,
    val usarLugar: Boolean = true,
    val soloEnElLugar: Boolean = false,
    val tieneUbicacion: Boolean = false,
    val usarClave: Boolean = false,
    val clave: String = "",
    val claveRepetida: String = "",
)

object ReglasCapsula {
    const val MAX_FOTOS = 20
    const val MAX_VIDEOS = 1
    const val MAX_NOTAS_DE_VOZ = 5
    const val MAX_PREGUNTAS = 5
    const val MIN_CLAVE = 4
    const val NOTA_DE_VOZ_MAX_MS = 180_000L

    /** Devuelve el problema a corregir (en español sencillo) o null si se puede sellar. */
    fun validar(f: FormularioCapsula, ahora: Instant = Instant.now()): String? = when {
        f.titulo.isBlank() -> "Ponle un título a tu cápsula."
        f.carta.isBlank() && f.medios == 0 -> "Escribe una carta o agrega fotos o videos."
        !f.usarFecha && !(f.usarLugar && f.soloEnElLugar) -> "Elige una fecha o activa “Solo se abre en este lugar”."
        f.usarLugar && !f.tieneUbicacion -> "Espera a que se registre tu ubicación o apaga “Guardar el lugar”."
        f.usarFecha && f.abreEl?.isBefore(ahora) == true -> "La fecha de apertura debe ser en el futuro."
        f.usarClave && f.clave.trim().length < MIN_CLAVE -> "La clave debe tener al menos $MIN_CLAVE caracteres."
        f.usarClave && f.clave != f.claveRepetida -> "Las dos claves no coinciden."
        else -> null
    }

    /** ¿Cabe otro medio de este tipo? Máximo 20 fotos y 1 video por cápsula. */
    fun cabe(tipo: String, fotos: Int, videos: Int): Boolean = when (tipo) {
        "photo" -> fotos < MAX_FOTOS
        "video" -> videos < MAX_VIDEOS
        else -> false
    }

    /** "mama@correo.com, amigo@correo.com" → lista de correos válidos (separados por coma, punto y coma, espacio o línea). */
    fun correos(texto: String): List<String> =
        texto.split(',', ';', ' ', '\n').map { it.trim() }.filter { it.contains('@') && !it.startsWith('@') && !it.endsWith('@') }.distinct()

    /** Busca un enlace de YouTube o Spotify en lo que copiaste (para pegarlo solo al volver). */
    fun enlaceDeCancion(texto: String?): String? =
        Regex("""https?://\S*(youtube\.com|youtu\.be|spotify\.com)\S*""").find(texto.orEmpty())?.value

    /** El nombre del lugar: el sitio con nombre donde estás (a 35 m o menos) o la primera parte de la dirección. */
    fun lugarSugerido(cercanos: List<NearbyPlace>, direccion: String?): String =
        cercanos.firstOrNull { it.distance <= 35 }?.name ?: direccion?.substringBefore(",").orEmpty()

    /** Botones rápidos de fecha: 1 mes, 6 meses, 1 año, 5 años y 10 años. */
    val opcionesDeFecha = listOf("1 mes" to 1L, "6 meses" to 6L, "1 año" to 12L, "5 años" to 60L, "10 años" to 120L)

    fun fechaEn(meses: Long, desde: ZonedDateTime = ZonedDateTime.now()): Instant = desde.plusMonths(meses).toInstant()

    /** El botón de fecha rápida se ve marcado si la fecha elegida coincide (con 2 minutos de margen). */
    fun mismaFecha(a: Instant?, b: Instant): Boolean = a != null && abs(a.epochSecond - b.epochSecond) < 120

    /** Aplica una plantilla: si la carta está vacía la reemplaza; si no, la pone arriba. */
    fun conPlantilla(carta: String, plantilla: String): String = if (carta.isBlank()) plantilla else plantilla + "\n\n" + carta

    /** Agrega una pregunta propia (sin repetir, máximo 5). */
    fun agregarPregunta(preguntas: List<String>, nueva: String): List<String> {
        val q = nueva.trim()
        return if (q.isBlank() || q in preguntas || preguntas.size >= MAX_PREGUNTAS) preguntas else preguntas + q
    }
}
