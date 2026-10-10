package com.timecapsule.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.OffsetDateTime

/** Una cápsula tal como la entrega la vista segura "capsules_feed". */
@Serializable
data class Capsule(
    val id: String,
    val owner: String,
    @SerialName("owner_name") val ownerName: String? = null,
    val title: String,
    val letter: String? = null,                 // null mientras está cerrada
    @SerialName("created_at") val createdAt: String,
    @SerialName("unlock_at") val unlockAt: String? = null,
    @SerialName("place_name") val placeName: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    @SerialName("radius_m") val radiusM: Int = 150,
    @SerialName("require_place") val requirePlace: Boolean = false,
    val opened: Boolean = false,
    @SerialName("is_open") val isOpen: Boolean = false,
    @SerialName("ai_summary") val aiSummary: String? = null,
    @SerialName("media_count") val mediaCount: Int = 0,
    @SerialName("cover_path") val coverPath: String? = null,
    @SerialName("has_key") val hasKey: Boolean = false,        // se puede abrir antes con clave
    @SerialName("early_open") val earlyOpen: Boolean = false,  // ya se abrió con la clave
    // --- Actualización 3 ---
    @SerialName("song_url") val songUrl: String? = null,
    @SerialName("song_title") val songTitle: String? = null,
    val template: String? = null,
    val questions: List<String> = emptyList(),
    @SerialName("question_count") val questionCount: Int = 0,
    @SerialName("open_together") val openTogether: Boolean = false,
    val votes: Int = 0,
    val members: Int = 1,
    @SerialName("my_vote") val myVote: Boolean = false,
    @SerialName("route_id") val routeId: String? = null,
    @SerialName("route_order") val routeOrder: Int? = null,
    @SerialName("route_title") val routeTitle: String? = null,
    @SerialName("can_add") val canAdd: Boolean = false,
    @SerialName("reaction_count") val reactionCount: Int = 0,
    @SerialName("comment_count") val commentCount: Int = 0,
) {
    val unlockInstant: Instant? get() = unlockAt?.let { runCatching { OffsetDateTime.parse(it).toInstant() }.getOrNull() }
    val createdInstant: Instant get() = runCatching { OffsetDateTime.parse(createdAt).toInstant() }.getOrDefault(Instant.now())
    /** La condición de fecha ya se cumplió (o no hay fecha). */
    val dateReady: Boolean get() = unlockInstant?.let { !it.isAfter(Instant.now()) } ?: true
    val hasPlace: Boolean get() = lat != null && lng != null
}

@Serializable
data class NewCapsule(
    val title: String,
    val letter: String,
    @SerialName("owner_name") val ownerName: String,
    @SerialName("unlock_at") val unlockAt: String?,
    @SerialName("place_name") val placeName: String?,
    val lat: Double?,
    val lng: Double?,
    @SerialName("radius_m") val radiusM: Int,
    @SerialName("require_place") val requirePlace: Boolean,
    @SerialName("song_url") val songUrl: String? = null,
    @SerialName("song_title") val songTitle: String? = null,
    val template: String? = null,
    val questions: List<String> = emptyList(),
    @SerialName("open_together") val openTogether: Boolean = false,
    @SerialName("route_id") val routeId: String? = null,
    @SerialName("route_order") val routeOrder: Int? = null,
)

@Serializable data class IdOnly(val id: String)

@Serializable
data class Media(
    val id: String,
    @SerialName("capsule_id") val capsuleId: String,
    val path: String,
    val kind: String,   // "photo" | "video" | "audio"
    @SerialName("added_by_name") val addedByName: String? = null,
)

@Serializable
data class NewMedia(
    @SerialName("capsule_id") val capsuleId: String,
    val path: String,
    val kind: String,
    @SerialName("added_by_name") val addedByName: String? = null,
)

@Serializable data class NewShare(@SerialName("capsule_id") val capsuleId: String, @SerialName("user_id") val userId: String)
@Serializable data class FoundUser(val id: String, val name: String? = null)
@Serializable data class Member(val name: String? = null, val email: String? = null)

/* ---------- Línea de tiempo creada por la IA ---------- */
@Serializable
data class Timeline(
    val resumen: String = "",
    val capitulos: List<Chapter> = emptyList(),
    val lugares: List<PlaceNote> = emptyList(),
)

@Serializable
data class Chapter(
    val titulo: String,
    val periodo: String = "",
    val descripcion: String = "",
    val emoji: String = "✨",
    val capsulas: List<String> = emptyList(),
)

@Serializable
data class PlaceNote(
    val nombre: String,
    val resumen: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
)

/** Lo que el usuario llena al crear una cápsula. */
data class Draft(
    val title: String,
    val letter: String,
    val items: List<PickedItem>,
    val unlockAt: Instant?,
    val placeName: String?,
    val lat: Double?,
    val lng: Double?,
    val requirePlace: Boolean,
    val shareEmails: List<String>,
    val key: String?,          // clave opcional para abrirla cuando quieras
    val voiceNotes: List<java.io.File> = emptyList(),
    val songUrl: String? = null,
    val songTitle: String? = null,
    val template: String? = null,
    val questions: List<String> = emptyList(),
    val radiusM: Int = 150,
    val openTogether: Boolean = false,
    val shareContribute: Boolean = false,
    val routeId: String? = null,
    val routeOrder: Int? = null,
)

data class PickedItem(val uri: android.net.Uri, val kind: String)

/* ---------- Actualización 3: compartir ---------- */
@Serializable data class Reaction(@SerialName("user_id") val userId: String, val emoji: String)
@Serializable data class NewReaction(@SerialName("capsule_id") val capsuleId: String, val emoji: String)
@Serializable data class Comment(
    val id: String, @SerialName("user_id") val userId: String, @SerialName("user_name") val userName: String? = null,
    val body: String, @SerialName("created_at") val createdAt: String,
)
@Serializable data class NewComment(@SerialName("capsule_id") val capsuleId: String, @SerialName("user_name") val userName: String, val body: String)
@Serializable data class Letter(
    val id: String, @SerialName("user_id") val userId: String, @SerialName("user_name") val userName: String? = null,
    val body: String, @SerialName("created_at") val createdAt: String,
)
@Serializable data class NewLetter(@SerialName("capsule_id") val capsuleId: String, @SerialName("user_name") val userName: String, val body: String)
@Serializable data class Answer(@SerialName("user_id") val userId: String, val idx: Int, val answer: String)
@Serializable data class NewAnswer(@SerialName("capsule_id") val capsuleId: String, val idx: Int, val answer: String)
@Serializable data class Route(val id: String, val title: String)
@Serializable data class NewRoute(val title: String)
@Serializable data class Activity(val kind: String, @SerialName("capsule_id") val capsuleId: String, val title: String, val who: String? = null, val body: String? = null, val at: String)

/** Plantillas de carta: cada una trae un inicio que la persona completa. */
object LetterTemplates {
    val all = listOf(
        "futuro" to ("Mi yo del futuro" to """Querido yo del futuro:

Hoy es un día como cualquiera y quiero contarte cómo me siento…

Espero que todavía…

Con cariño,
"""),
        "familia" to ("Para mi familia" to """Querida familia:

Cuando lean esto, quiero que sepan…

Gracias por…

Los quiero,
"""),
        "aniversario" to ("Aniversario" to """Mi amor:

Hoy cumplimos… y quiero guardar este momento.

Lo que más me gusta de nosotros es…

Para el próximo aniversario, deseo…
"""),
        "graduacion" to ("Graduación" to """Querido yo graduado:

Hoy terminé… y estoy muy orgulloso porque…

Mi sueño ahora es…

No olvides que…
"""),
        "amigos" to ("Para mis amigos" to """Amigos:

Esta cápsula es para que nunca olvidemos…

Mi recuerdo favorito con ustedes es…
"""),
        "viaje" to ("Un viaje" to """Bitácora de viaje:

Estamos en… y lo que más me sorprendió fue…

Prometo volver para…
"""),
    )
    val questions = listOf(
        "¿Sigues viviendo en el mismo lugar?", "¿Cumpliste el sueño que tenías hoy?", "¿Quiénes siguen siendo tus mejores amigos?",
        "¿Qué canción te gusta ahora?", "¿Qué le dirías a tu yo de hoy?", "¿Sigues estudiando o trabajando en lo mismo?",
    )
}
