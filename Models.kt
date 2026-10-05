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
)

@Serializable data class IdOnly(val id: String)

@Serializable
data class Media(
    val id: String,
    @SerialName("capsule_id") val capsuleId: String,
    val path: String,
    val kind: String,   // "photo" | "video"
)

@Serializable
data class NewMedia(
    @SerialName("capsule_id") val capsuleId: String,
    val path: String,
    val kind: String,
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
)

data class PickedItem(val uri: android.net.Uri, val kind: String)
