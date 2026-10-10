package com.timecapsule.app.data

import android.content.Context
import com.timecapsule.app.util.MediaUtil
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.util.UUID
import kotlin.time.Duration.Companion.hours

/** Todo lo que la app hace con la nube pasa por aquí. */
object Repo {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private const val BUCKET = "media"
    private val urlCache = HashMap<String, Pair<String, Long>>()

    /* ---------------- Cuenta ---------------- */

    /** Espera a que se cargue la sesión guardada (útil en segundo plano). */
    suspend fun ready() { supabase.auth.sessionStatus.first { it !is SessionStatus.Initializing } }

    fun userId(): String? = supabase.auth.currentUserOrNull()?.id
    fun userEmail(): String = supabase.auth.currentUserOrNull()?.email.orEmpty()
    fun userName(): String {
        val meta = supabase.auth.currentUserOrNull()?.userMetadata
        return meta?.get("name")?.jsonPrimitive?.contentOrNull ?: userEmail().substringBefore("@")
    }

    suspend fun signIn(email: String, password: String) {
        supabase.auth.signInWith(Email) { this.email = email.trim(); this.password = password }
    }

    /** Devuelve true si la cuenta quedó lista; false si hay que confirmar el correo. */
    suspend fun signUp(name: String, email: String, password: String): Boolean {
        supabase.auth.signUpWith(Email) {
            this.email = email.trim()
            this.password = password
            data = buildJsonObject { put("name", name.trim()) }
        }
        return supabase.auth.currentSessionOrNull() != null
    }

    suspend fun signOut() = supabase.auth.signOut()

    /* ---------------- Cápsulas ---------------- */

    suspend fun feed(): List<Capsule> =
        supabase.from("capsules_feed").select { order("created_at", Order.DESCENDING) }.decodeList()

    suspend fun capsule(id: String): Capsule? =
        supabase.from("capsules_feed").select { filter { eq("id", id) } }.decodeSingleOrNull()

    suspend fun media(capsuleId: String): List<Media> =
        supabase.from("capsule_media").select { filter { eq("capsule_id", capsuleId) } }.decodeList()

    /** Enlace temporal para ver una foto o video (solo funciona si la cápsula está abierta). */
    suspend fun signedUrl(path: String): String {
        urlCache[path]?.let { (url, exp) -> if (exp > System.currentTimeMillis()) return url }
        val url = supabase.storage.from(BUCKET).createSignedUrl(path, 2.hours)
        urlCache[path] = url to System.currentTimeMillis() + 90 * 60_000
        return url
    }

    /** Crea la cápsula, sube fotos/videos y la comparte. */
    suspend fun create(ctx: Context, d: Draft, onProgress: (String, Float) -> Unit): String = withContext(Dispatchers.IO) {
        val uid = userId() ?: error("Inicia sesión de nuevo")
        onProgress("Creando la cápsula…", 0.05f)
        val id = supabase.from("capsules").insert(
            NewCapsule(
                title = d.title.trim(), letter = d.letter.trim(), ownerName = userName(),
                unlockAt = d.unlockAt?.toString(), placeName = d.placeName?.trim()?.ifBlank { null },
                lat = d.lat, lng = d.lng, radiusM = 150, requirePlace = d.requirePlace,
            )
        ) { select(Columns.list("id")) }.decodeSingle<IdOnly>().id

        // La clave se guarda cifrada en el servidor (nadie puede leerla, ni siquiera tú)
        d.key?.trim()?.takeIf { it.isNotEmpty() }?.let { k ->
            onProgress("Guardando la clave…", 0.08f)
            supabase.postgrest.rpc("set_capsule_key", buildJsonObject { put("cid", id); put("k", k) })
        }

        d.items.forEachIndexed { i, item ->
            onProgress("Subiendo recuerdo ${i + 1} de ${d.items.size}…", 0.1f + 0.8f * i / d.items.size.coerceAtLeast(1))
            val (bytes, ext, mime) = if (item.kind == "video") {
                val m = MediaUtil.mime(ctx, item.uri)
                Triple(MediaUtil.readVideo(ctx, item.uri), if (m.contains("quicktime")) "mov" else "mp4", m)
            } else {
                Triple(MediaUtil.compressPhoto(ctx, item.uri), "jpg", "image/jpeg")
            }
            val path = "$uid/$id/${UUID.randomUUID()}.$ext"
            supabase.storage.from(BUCKET).upload(path, bytes) {
                upsert = false
                contentType = ContentType.parse(mime)
            }
            supabase.from("capsule_media").insert(NewMedia(id, path, item.kind))
        }

        val notFound = mutableListOf<String>()
        d.shareEmails.filter { it.isNotBlank() }.forEach { email ->
            onProgress("Compartiendo con $email…", 0.95f)
            runCatching { share(id, email) }.onFailure { notFound += email }
        }
        onProgress(if (notFound.isEmpty()) "¡Listo!" else "Listo. Sin cuenta: ${notFound.joinToString()}", 1f)
        id
    }

    /** Abre una cápsula que depende de un lugar (el servidor revisa la fecha). */
    suspend fun open(id: String): Boolean =
        supabase.postgrest.rpc("open_capsule", buildJsonObject { put("cid", id) }).decodeAs<Boolean>()

    /** Abre la cápsula antes de tiempo con su clave. Devuelve false si la clave es incorrecta. */
    suspend fun openWithKey(id: String, key: String): Boolean =
        supabase.postgrest.rpc("open_with_key", buildJsonObject { put("cid", id); put("k", key) }).decodeAs<Boolean>()

    /** Comparte con otra persona que ya tenga cuenta. Devuelve su nombre. */
    suspend fun share(capsuleId: String, email: String): String {
        val found = supabase.postgrest.rpc("find_user", buildJsonObject { put("e", email.trim()) })
            .decodeList<FoundUser>().firstOrNull()
            ?: error("No hay ninguna cuenta con $email. Pídele que se registre en Time Capsule.")
        supabase.from("capsule_shares").insert(NewShare(capsuleId, found.id))
        return found.name ?: email
    }

    suspend fun members(capsuleId: String): List<Member> =
        supabase.postgrest.rpc("capsule_members", buildJsonObject { put("cid", capsuleId) }).decodeList()

    suspend fun delete(id: String) {
        supabase.from("capsules").delete { filter { eq("id", id) } }
    }

    /* ---------------- IA ---------------- */

    suspend fun aiSummary(capsuleId: String): String {
        val res = supabase.functions.invoke(
            function = "ai-memories",
            body = buildJsonObject { put("action", "summary"); put("capsule_id", capsuleId) },
        )
        val obj = json.parseToJsonElement(res.bodyAsText()).jsonObject
        obj["error"]?.jsonPrimitive?.contentOrNull?.let { error(it) }
        return obj["summary"]?.jsonPrimitive?.contentOrNull ?: error("La IA no devolvió un resumen")
    }

    suspend fun aiTimeline(): Timeline {
        val res = supabase.functions.invoke(
            function = "ai-memories",
            body = buildJsonObject { put("action", "timeline") },
        )
        val text = res.bodyAsText()
        json.parseToJsonElement(text).jsonObject["error"]?.jsonPrimitive?.contentOrNull?.let { error(it) }
        return json.decodeFromString(Timeline.serializer(), text)
    }

    fun encodeTimeline(t: Timeline): String = json.encodeToString(Timeline.serializer(), t)
    fun decodeTimeline(s: String): Timeline? = runCatching { json.decodeFromString(Timeline.serializer(), s) }.getOrNull()

    /** Convierte errores técnicos en mensajes claros. */
    fun friendly(e: Throwable): String {
        val m = e.message.orEmpty()
        return when {
            m.contains("Invalid login credentials", true) -> "Correo o contraseña incorrectos."
            m.contains("Email not confirmed", true) -> "Confirma tu correo con el enlace que te enviamos."
            m.contains("already registered", true) -> "Ese correo ya tiene una cuenta. Inicia sesión."
            m.contains("Password should be", true) -> "La contraseña debe tener al menos 6 caracteres."
            m.contains("Unable to resolve host", true) || m.contains("timeout", true) -> "Sin conexión a internet."
            m.contains("Payload too large", true) || m.contains("exceeded the maximum", true) -> "Un archivo supera el límite de 50 MB."
            m.contains("TU-PROYECTO") -> "Falta configurar Supabase en Config.kt."
            m.isNotBlank() && m.length < 160 -> m
            else -> "Algo salió mal. Intenta de nuevo."
        }
    }
}
