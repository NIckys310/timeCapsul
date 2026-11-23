package com.timecapsule.app.ui.screens

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.timecapsule.app.data.Draft
import com.timecapsule.app.data.DraftStore
import com.timecapsule.app.data.CapsuleForm
import com.timecapsule.app.data.PickedItem
import com.timecapsule.app.data.CapsuleRules
import com.timecapsule.app.data.Repo
import com.timecapsule.app.data.Route
import com.timecapsule.app.data.SavedDraft
import com.timecapsule.app.data.SavedItem
import com.timecapsule.app.util.LocationUtil
import com.timecapsule.app.util.MediaUtil
import com.timecapsule.app.util.NearbyPlace
import com.timecapsule.app.util.Places
import com.timecapsule.app.util.VoiceRecorder
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime

/** Lo que "Nueva cápsula" necesita de la nube. En las pruebas se reemplaza por una versión falsa. */
interface CapsuleService {
    suspend fun routes(): List<Route>
    suspend fun createNewRoute(name: String): Route
    suspend fun nextInRoute(routeIdValue: String): Int
    suspend fun suggest(photos: List<String>, letter: String, place: String?): Pair<List<String>, Int>
    suspend fun improveLetter(letter: String, title: String, tone: String): String
    suspend fun create(ctx: Context, d: Draft, progress: (String, Float) -> Unit): String
    fun message(e: Throwable): String
}

/** La versión real: Supabase y la función de IA. */
object SupabaseService : CapsuleService {
    override suspend fun routes() = Repo.routes()
    override suspend fun createNewRoute(name: String) = Repo.createRoute(name)
    override suspend fun nextInRoute(routeIdValue: String) = Repo.nextRouteOrder(routeIdValue)
    override suspend fun suggest(photos: List<String>, letter: String, place: String?) = Repo.aiSuggest(photos, letter, place)
    override suspend fun improveLetter(letter: String, title: String, tone: String) = Repo.aiLetter(letter, title, tone)
    override suspend fun create(ctx: Context, d: Draft, progress: (String, Float) -> Unit) = Repo.create(ctx, d, progress)
    override fun message(e: Throwable) = Repo.friendly(e)
}

/**
 * NUEVA CÁPSULA · el estado y la lógica de la pantalla.
 * Vive más que la pantalla: girar el celular, abrir la cámara o la galería no borra nada.
 * Si Android cierra la app, el borrador automático (DraftStore) lo recupera.
 * La pantalla (CreateCapsuleScreen) solo dibuja y le avisa a este ViewModel.
 */
class NewCapsuleViewModel(
    app: Application,
    private val cloud: CapsuleService,
    private val loadDraft: Boolean = true,
) : AndroidViewModel(app) {
    private val ctx: Context get() = getApplication()

    // ---------- Contenido ----------
    var title by mutableStateOf(""); private set
    var letter by mutableStateOf(""); private set
    val media = mutableStateListOf<PickedItem>()
    val voiceNotes = mutableStateListOf<File>()
    var song by mutableStateOf(""); private set
    var songLink by mutableStateOf(""); private set
    val questions = mutableStateListOf<String>()
    var template by mutableStateOf<String?>(null); private set

    // ---------- Apertura ----------
    var useDate by mutableStateOf(true); private set
    var opensOn by mutableStateOf<Instant?>(ZonedDateTime.now().plusYears(1).toInstant()); private set
    var useKey by mutableStateOf(false); private set
    var key by mutableStateOf(""); private set
    var key2 by mutableStateOf(""); private set

    // ---------- Lugar ----------
    var usePlace by mutableStateOf(true); private set
    var onlyAtPlace by mutableStateOf(false); private set
    var placeNameText by mutableStateOf(""); private set
    var address by mutableStateOf<String?>(null); private set
    var nearbyPlaces by mutableStateOf<List<NearbyPlace>>(emptyList()); private set
    var lat by mutableStateOf<Double?>(null); private set
    var lng by mutableStateOf<Double?>(null); private set
    var precision by mutableStateOf<Int?>(null); private set
    var searchingPlace by mutableStateOf(false); private set
    var radius by mutableIntStateOf(150); private set
    var routes by mutableStateOf<List<Route>>(emptyList()); private set
    var routeIdValue by mutableStateOf<String?>(null); private set

    // ---------- Compartir ----------
    var emails by mutableStateOf(""); private set
    var collaborative by mutableStateOf(false); private set
    var openTogether by mutableStateOf(false); private set

    // ---------- Estado de la pantalla ----------
    var error by mutableStateOf<String?>(null); private set
    var saving by mutableStateOf(false); private set
    var progress by mutableFloatStateOf(0f); private set
    var progressText by mutableStateOf(""); private set
    /** Id de la cápsula creada: la pantalla la abre y se cierra. */
    var created by mutableStateOf<String?>(null); private set
    var recoveredDraft by mutableStateOf(false); private set
    var aiBusy by mutableStateOf<String?>(null); private set
    var titleIdeas by mutableStateOf<List<String>>(emptyList()); private set
    var previousLetter by mutableStateOf<String?>(null); private set
    var recordingNow by mutableStateOf(false); private set
    var recordedMs by mutableLongStateOf(0L); private set
    var showDate by mutableStateOf(false)
    var showTime by mutableStateOf(false)
    var chosenDate by mutableStateOf<LocalDate?>(null)
    /** Foto que la cámara está tomando (sobrevive si se gira el celular mientras tanto). */
    var cameraPhoto by mutableStateOf<Uri?>(null)

    private val recorder by lazy { VoiceRecorder(ctx) }

    init {
        viewModelScope.launch {
            routes = runCatching { cloud.routes() }.onFailure { record("rutas", it) }.getOrDefault(emptyList())
        }
        if (loadDraft) viewModelScope.launch {
            DraftStore.load(ctx)?.let { restore(it) }
            saveDraftOnly()
        }
    }

    // ---------- Cambios desde la pantalla ----------
    fun changeTitle(v: String) { title = v }
    fun changeLetter(v: String) { letter = v }
    fun changeSong(v: String) { song = v }
    fun changeSongLink(v: String) { songLink = v }
    fun changeUseDate(v: Boolean) { useDate = v }
    fun changeOpensOn(v: Instant?) { opensOn = v }
    fun changeUseKey(v: Boolean) { useKey = v }
    fun changeKey(v: String) { key = v }
    fun changeKey2(v: String) { key2 = v }
    fun changeUsePlace(v: Boolean) { usePlace = v }
    fun changeOnlyAtPlace(v: Boolean) { onlyAtPlace = v }
    fun changePlaceName(v: String) { placeNameText = v }
    fun changeRadius(v: Int) { radius = v }
    fun changeRoute(v: String?) { routeIdValue = v }
    fun changeEmails(v: String) { emails = v }
    fun changeCollaborative(v: Boolean) { collaborative = v }
    fun changeOpenTogether(v: Boolean) { openTogether = v }
    fun showError(v: String?) { error = v }

    fun useTemplate(id: String, text: String) { template = id; letter = CapsuleRules.withTemplate(letter, text) }
    fun undoLetter() { previousLetter?.let { letter = it }; previousLetter = null }
    fun removeMedia(i: Int) { media.removeAt(i) }
    fun removeQuestion(i: Int) { questions.removeAt(i) }
    fun addQuestion(q: String) {
        val newOnes = CapsuleRules.addQuestion(questions, q)
        if (newOnes.size != questions.size) questions += newOnes.last()
    }

    /** Al volver de YouTube o Spotify: si copiaste el enlace de la canción, se pega solo. */
    fun pasteSongIfAny(clipboard: String) {
        if (songLink.isBlank()) CapsuleRules.songLinkOf(clipboard)?.let { songLink = it }
    }

    fun startOver() {
        runCatching { DraftStore.clear(ctx) }.onFailure { record("borrador", it) }; recoveredDraft = false
        title = ""; letter = ""; media.clear(); voiceNotes.clear(); song = ""; songLink = ""
        questions.clear(); template = null; emails = ""
    }

    // ---------- Fotos y videos ----------
    fun addMedia(uris: List<Uri>) {
        var skipped = 0
        uris.forEach { u ->
            if (media.any { it.uri == u }) return@forEach
            runCatching { ctx.contentResolver.takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                .onFailure { record("permiso de archivo", it) }
            val type = MediaUtil.kindOf(ctx, u)
            if (CapsuleRules.fits(type, media.count { it.kind == "photo" }, media.count { it.kind == "video" })) media += PickedItem(u, type)
            else skipped++
        }
        if (skipped > 0) error = "Máximo ${CapsuleRules.MAX_PHOTOS} fotos y ${CapsuleRules.MAX_VIDEOS} video por cápsula. Se omitieron $skipped."
    }

    fun takenPhoto(ok: Boolean) {
        val uri = cameraPhoto ?: return
        cameraPhoto = null
        if (!ok) return
        if (CapsuleRules.fits("photo", media.count { it.kind == "photo" }, 0)) media += PickedItem(uri, "photo")
        else error = "Máximo ${CapsuleRules.MAX_PHOTOS} fotos por cápsula."
    }

    // ---------- Notas de voz ----------
    fun toggleRecording(permissionGranted: Boolean) {
        if (recordingNow) { stopRecording(); return }
        if (!permissionGranted) { error = "Sin permiso del micrófono no se pueden grabar notas de voz."; return }
        runCatching { recorder.start(); recordingNow = true }
            .onFailure { record("micrófono", it); error = "No se pudo usar el micrófono." }
        if (recordingNow) viewModelScope.launch {
            while (recordingNow) {
                recordedMs = recorder.elapsedMs
                if (recordedMs >= CapsuleRules.VOICE_NOTE_MAX_MS) stopRecording()
                delay(200)
            }
        }
    }

    private fun stopRecording() {
        recorder.stop()?.let { voiceNotes += it }
        recordingNow = false
    }

    fun deleteNote(i: Int) { voiceNotes.removeAt(i).delete() }

    // ---------- Lugar ----------
    /** Toma el GPS real y busca la dirección y los sitios con nombre alrededor. */
    fun locate() {
        searchingPlace = true
        viewModelScope.launch {
            val loc = runCatching { LocationUtil.current(ctx) }.onFailure { record("GPS", it) }.getOrNull()
            if (loc == null) {
                error = "No se pudo obtener tu ubicación. Revisa que el GPS esté encendido."
            } else {
                lat = loc.latitude; lng = loc.longitude; precision = loc.accuracy.toInt()
                address = Places.address(ctx, loc.latitude, loc.longitude)
                nearbyPlaces = Places.nearby(loc.latitude, loc.longitude)
                if (placeNameText.isBlank()) placeNameText = CapsuleRules.suggestedPlace(nearbyPlaces, address)
            }
            searchingPlace = false
        }
    }

    fun createNewRoute(name: String) = viewModelScope.launch {
        runCatching { cloud.createNewRoute(name) }
            .onSuccess { r -> routes = listOf(r) + routes; routeIdValue = r.id }
            .onFailure { error = cloud.message(it) }
    }

    // ---------- IA ----------
    fun suggestTitle() {
        aiBusy = "titulo"; error = null
        viewModelScope.launch {
            runCatching {
                val photos = media.filter { it.kind == "photo" }.take(3)
                val images = photos.map { Base64.encodeToString(MediaUtil.compressPhoto(ctx, it.uri, 640), Base64.NO_WRAP) }
                val (titulos, coverIndex) = cloud.suggest(images, letter, placeNameText.ifBlank { null })
                titleIdeas = titulos
                // La foto elegida como portada pasa al primer lugar
                photos.getOrNull(coverIndex)?.let { best -> media.remove(best); media.add(0, best) }
            }.onFailure { error = cloud.message(it) }
            aiBusy = null
        }
    }

    fun improveLetter(tone: String) {
        aiBusy = "carta"; error = null
        viewModelScope.launch {
            runCatching { cloud.improveLetter(letter, title, tone) }
                .onSuccess { previousLetter = letter; letter = it }
                .onFailure { error = cloud.message(it) }
            aiBusy = null
        }
    }

    // ---------- Sellar ----------
    fun form() = CapsuleForm(
        title = title, letter = letter, media = media.size, useDate = useDate, opensOn = opensOn,
        usePlace = usePlace, onlyAtPlace = onlyAtPlace, hasLocation = lat != null,
        useKey = useKey, key = key, repeatedKey = key2,
    )

    fun seal() {
        error = CapsuleRules.validate(form())
        if (error != null) return
        saving = true
        viewModelScope.launch {
            runCatching {
                val list = CapsuleRules.emails(emails)
                cloud.create(ctx, Draft(
                    title = title, letter = letter, items = media.toList(),
                    unlockAt = if (useDate) opensOn else null,
                    placeName = if (usePlace) placeNameText.ifBlank { address } else null,
                    lat = if (usePlace) lat else null, lng = if (usePlace) lng else null,
                    requirePlace = usePlace && onlyAtPlace,
                    shareEmails = list,
                    key = if (useKey) key else null,
                    voiceNotes = voiceNotes.toList(),
                    songUrl = songLink, songTitle = song, template = template,
                    questions = questions.toList(), radiusM = radius,
                    openTogether = openTogether && list.isNotEmpty(), shareContribute = collaborative,
                    routeId = routeIdValue, routeOrder = routeIdValue?.let { runCatching { cloud.nextInRoute(it) }.getOrDefault(1) },
                )) { text, p -> progressText = text; progress = p }
            }.onSuccess { id -> runCatching { DraftStore.clear(ctx) }.onFailure { record("borrador", it) }; created = id }
             .onFailure { error = cloud.message(it); saving = false }
        }
    }

    // ---------- Borrador automático ----------
    private fun restore(d: SavedDraft) {
        title = d.title; letter = d.letter; template = d.template
        media.clear(); media.addAll(d.items.map { PickedItem(Uri.parse(it.uri), it.kind) })
        voiceNotes.clear(); voiceNotes.addAll(d.voiceNotes.map { File(it) }.filter { it.exists() })
        useDate = d.useDate; opensOn = d.unlockAtEpoch?.let { Instant.ofEpochMilli(it) } ?: opensOn
        usePlace = d.usePlace; onlyAtPlace = d.requirePlace; placeNameText = d.placeName
        emails = d.emails; song = d.songTitle; songLink = d.songUrl
        questions.clear(); questions.addAll(d.questions)
        radius = d.radius; openTogether = d.openTogether; collaborative = d.contribute; routeIdValue = d.routeId
        recoveredDraft = true
    }

    fun draft() = SavedDraft(
        title = title, letter = letter, items = media.map { SavedItem(it.uri.toString(), it.kind) },
        voiceNotes = voiceNotes.map { it.absolutePath }, unlockAtEpoch = opensOn?.toEpochMilli(), useDate = useDate,
        usePlace = usePlace, requirePlace = onlyAtPlace, placeName = placeNameText, emails = emails, template = template,
        songTitle = song, songUrl = songLink, questions = questions.toList(), radius = radius,
        openTogether = openTogether, contribute = collaborative, routeId = routeIdValue,
    )

    /** Cada cambio se guarda 300 ms después (no en cada letra). */
    private suspend fun saveDraftOnly() {
        snapshotFlow { draft() }.collectLatest { d -> delay(300); DraftStore.save(ctx, d) }
    }

    override fun onCleared() {
        if (recordingNow) stopRecording()
    }

    private fun record(where: String, e: Throwable) = Log.w("NuevaCapsula", "Falló $where", e)

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { NewCapsuleViewModel(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!, SupabaseService) }
        }
    }
}
