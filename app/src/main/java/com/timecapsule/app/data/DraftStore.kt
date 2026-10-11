package com.timecapsule.app.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Borrador de la cápsula que se está creando.
 * Se guarda en el celular cada vez que cambias algo, así no se pierde nada
 * si sales a Spotify/YouTube, contestas una llamada o Android cierra la app.
 * (La clave de la cápsula NO se guarda aquí, por seguridad.)
 */
@Serializable
data class SavedDraft(
    val title: String = "",
    val letter: String = "",
    val items: List<SavedItem> = emptyList(),
    val voiceNotes: List<String> = emptyList(),   // rutas de archivos
    val unlockAtEpoch: Long? = null,
    val useDate: Boolean = true,
    val usePlace: Boolean = true,
    val requirePlace: Boolean = false,
    val placeName: String = "",
    val emails: String = "",
    val template: String? = null,
    val songTitle: String = "",
    val songUrl: String = "",
    val questions: List<String> = emptyList(),
    val radius: Int = 150,
    val openTogether: Boolean = false,
    val contribute: Boolean = false,
    val routeId: String? = null,
) {
    val isEmpty get() = title.isBlank() && letter.isBlank() && items.isEmpty() && voiceNotes.isEmpty() &&
        songTitle.isBlank() && songUrl.isBlank() && questions.isEmpty()
}

@Serializable data class SavedItem(val uri: String, val kind: String)

object DraftStore {
    private val json = Json { ignoreUnknownKeys = true }
    private fun p(ctx: Context) = ctx.getSharedPreferences("timecapsule_draft", Context.MODE_PRIVATE)

    fun save(ctx: Context, d: SavedDraft) {
        if (d.isEmpty) clear(ctx) else p(ctx).edit().putString("draft", json.encodeToString(SavedDraft.serializer(), d)).apply()
    }

    fun load(ctx: Context): SavedDraft? =
        p(ctx).getString("draft", null)?.let { runCatching { json.decodeFromString(SavedDraft.serializer(), it) }.getOrNull() }
            ?.takeIf { !it.isEmpty }

    fun clear(ctx: Context) = p(ctx).edit().remove("draft").apply()
}
