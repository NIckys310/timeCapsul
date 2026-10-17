package com.timecapsule.app.ui.screens

import com.timecapsule.app.ui.components.paper
import androidx.compose.ui.graphics.Color
import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.timecapsule.app.data.Draft
import com.timecapsule.app.data.DraftStore
import com.timecapsule.app.data.SavedDraft
import com.timecapsule.app.data.SavedItem
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.collectLatest
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.content.ClipboardManager
import androidx.compose.material.icons.filled.AutoAwesome
import android.util.Base64
import com.timecapsule.app.data.LetterTemplates
import com.timecapsule.app.data.Route
import com.timecapsule.app.ui.components.VoiceNotePlayer
import com.timecapsule.app.util.VoiceRecorder
import com.timecapsule.app.util.fmtDuration
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Groups
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableLongStateOf
import android.content.Intent
import android.net.Uri as AUri
import kotlinx.coroutines.delay
import com.timecapsule.app.data.PickedItem
import com.timecapsule.app.data.Repo
import com.timecapsule.app.data.ReglasCapsula
import com.timecapsule.app.data.FormularioCapsula
import com.timecapsule.app.ui.theme.Cream
import com.timecapsule.app.ui.theme.Gold
import com.timecapsule.app.ui.theme.Muted
import com.timecapsule.app.ui.theme.Night
import com.timecapsule.app.ui.theme.NightRaised
import com.timecapsule.app.util.LocationUtil
import com.timecapsule.app.util.MediaUtil
import com.timecapsule.app.util.NearbyPlace
import com.timecapsule.app.util.Places
import com.timecapsule.app.util.fmtDateTime
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * NUEVA CÁPSULA
 * Esta pantalla guarda lo que vas llenando (con borrador automático) y acomoda las
 * secciones de CreateCapsuleSections.kt. Las reglas están en data/NuevaCapsula.kt.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateCapsuleScreen(onBack: () -> Unit, onCreated: (String) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var letter by remember { mutableStateOf("") }
    val items = remember { mutableStateListOf<PickedItem>() }
    var unlockAt by remember { mutableStateOf<Instant?>(ZonedDateTime.now().plusYears(1).toInstant()) }
    var useDate by remember { mutableStateOf(true) }

    // Clave para abrirla cuando quieras
    var useKey by remember { mutableStateOf(false) }
    var key by remember { mutableStateOf("") }
    var key2 by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }

    // Lugar (se activa solo si das permiso de ubicación)
    var usePlace by remember { mutableStateOf(true) }
    var requirePlace by remember { mutableStateOf(false) }
    var placeName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf<String?>(null) }
    var nearby by remember { mutableStateOf<List<NearbyPlace>>(emptyList()) }
    var lat by remember { mutableStateOf<Double?>(null) }
    var lng by remember { mutableStateOf<Double?>(null) }
    var accuracy by remember { mutableStateOf<Int?>(null) }
    var locating by remember { mutableStateOf(false) }

    var emails by remember { mutableStateOf("") }
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var pickedDate by remember { mutableStateOf<LocalDate?>(null) }
    var saving by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var progressText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    // Actualización 3: plantilla, notas de voz, canción, preguntas, radio, abrir juntos, colaboradores y rutas
    var template by remember { mutableStateOf<String?>(null) }
    // IA: títulos sugeridos y ayuda con la carta
    var aiBusy by remember { mutableStateOf<String?>(null) }
    var titleIdeas by remember { mutableStateOf<List<String>>(emptyList()) }
    var letterBefore by remember { mutableStateOf<String?>(null) }
    fun suggestTitle() {
        aiBusy = "titulo"; error = null
        scope.launch {
            runCatching {
                val photos = items.filter { it.kind == "photo" }.take(3)
                val imgs = photos.map { Base64.encodeToString(MediaUtil.compressPhoto(ctx, it.uri, 640), Base64.NO_WRAP) }
                val (titles, cover) = Repo.aiSuggest(imgs, letter, placeName.ifBlank { null })
                titleIdeas = titles
                // La foto elegida como portada pasa al primer lugar
                photos.getOrNull(cover)?.let { best -> items.remove(best); items.add(0, best) }
            }.onFailure { error = Repo.friendly(it) }
            aiBusy = null
        }
    }
    fun helpLetter(tono: String) {
        aiBusy = "carta"; error = null
        scope.launch {
            runCatching { Repo.aiLetter(letter, title, tono) }
                .onSuccess { letterBefore = letter; letter = it }
                .onFailure { error = Repo.friendly(it) }
            aiBusy = null
        }
    }
    val voiceNotes = remember { mutableStateListOf<File>() }
    val recorder = remember { VoiceRecorder(ctx) }
    var recording by remember { mutableStateOf(false) }
    var recMs by remember { mutableLongStateOf(0L) }
    var songTitle by remember { mutableStateOf("") }
    var songUrl by remember { mutableStateOf("") }
    val questions = remember { mutableStateListOf<String>() }
    var radius by remember { mutableStateOf(150) }
    var openTogether by remember { mutableStateOf(false) }
    var contribute by remember { mutableStateOf(false) }
    var routes by remember { mutableStateOf<List<Route>>(emptyList()) }
    var routeId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { routes = runCatching { Repo.routes() }.getOrDefault(emptyList()) }

    // ---------- Borrador automático ----------
    var draftReady by remember { mutableStateOf(false) }
    var restored by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        DraftStore.load(ctx)?.let { d ->
            title = d.title; letter = d.letter; template = d.template
            items.clear(); items.addAll(d.items.map { PickedItem(Uri.parse(it.uri), it.kind) })
            voiceNotes.clear(); voiceNotes.addAll(d.voiceNotes.map { File(it) }.filter { it.exists() })
            useDate = d.useDate; unlockAt = d.unlockAtEpoch?.let { Instant.ofEpochMilli(it) } ?: unlockAt
            usePlace = d.usePlace; requirePlace = d.requirePlace; placeName = d.placeName
            emails = d.emails; songTitle = d.songTitle; songUrl = d.songUrl
            questions.clear(); questions.addAll(d.questions)
            radius = d.radius; openTogether = d.openTogether; contribute = d.contribute; routeId = d.routeId
            restored = true
        }
        draftReady = true
    }
    LaunchedEffect(Unit) {
        snapshotFlow {
            SavedDraft(
                title = title, letter = letter, items = items.map { SavedItem(it.uri.toString(), it.kind) },
                voiceNotes = voiceNotes.map { it.absolutePath }, unlockAtEpoch = unlockAt?.toEpochMilli(), useDate = useDate,
                usePlace = usePlace, requirePlace = requirePlace, placeName = placeName, emails = emails, template = template,
                songTitle = songTitle, songUrl = songUrl, questions = questions.toList(), radius = radius,
                openTogether = openTogether, contribute = contribute, routeId = routeId,
            )
        }.collectLatest { d -> if (draftReady) { kotlinx.coroutines.delay(300); DraftStore.save(ctx, d) } }
    }
    // Al volver de YouTube/Spotify: si copiaste el enlace de la canción, se pega solo
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME && songUrl.isBlank()) {
                val cb = ctx.getSystemService(ClipboardManager::class.java)
                val clip = runCatching { cb?.primaryClip?.getItemAt(0)?.text?.toString() }.getOrNull().orEmpty()
                ReglasCapsula.enlaceDeCancion(clip)?.let { songUrl = it }
            }
        }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }
    LaunchedEffect(recording) { while (recording) { recMs = recorder.elapsedMs; if (recMs >= ReglasCapsula.NOTA_DE_VOZ_MAX_MS) { recorder.stop()?.let { voiceNotes += it }; recording = false }; delay(200) } }
    DisposableEffect(Unit) { onDispose { recorder.stop() } }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) runCatching { recorder.start(); recording = true }.onFailure { error = "No se pudo usar el micrófono." }
        else error = "Sin permiso del micrófono no se pueden grabar notas de voz."
    }

    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(20)) { uris ->
        var skipped = 0
        uris.forEach { u ->
            if (items.any { it.uri == u }) return@forEach
            runCatching { ctx.contentResolver.takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            val kind = MediaUtil.kindOf(ctx, u)
            val photos = items.count { it.kind == "photo" }; val videos = items.count { it.kind == "video" }
            if (ReglasCapsula.cabe(kind, photos, videos)) items += PickedItem(u, kind) else skipped++
        }
        if (skipped > 0) error = "Máximo ${ReglasCapsula.MAX_FOTOS} fotos y ${ReglasCapsula.MAX_VIDEOS} video por cápsula. Se omitieron $skipped."
    }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) cameraUri?.let {
            if (ReglasCapsula.cabe("photo", items.count { i -> i.kind == "photo" }, 0)) items += PickedItem(it, "photo")
            else error = "Máximo ${ReglasCapsula.MAX_FOTOS} fotos por cápsula."
        }
    }

    /** Toma el GPS real y busca la dirección y los sitios con nombre alrededor. */
    fun locate() {
        locating = true
        scope.launch {
            val loc = LocationUtil.current(ctx)
            if (loc == null) {
                error = "No se pudo obtener tu ubicación. Revisa que el GPS esté encendido."
            } else {
                lat = loc.latitude; lng = loc.longitude; accuracy = loc.accuracy.toInt()
                address = Places.address(ctx, loc.latitude, loc.longitude)
                nearby = Places.nearby(loc.latitude, loc.longitude)
                // Si estás prácticamente dentro de un lugar con nombre, se elige solo
                if (placeName.isBlank()) placeName = ReglasCapsula.lugarSugerido(nearby, address)
            }
            locating = false
        }
    }
    val locPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        if (res[Manifest.permission.ACCESS_FINE_LOCATION] == true || res[Manifest.permission.ACCESS_COARSE_LOCATION] == true) locate()
        else usePlace = false
    }
    fun askLocation() = locPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))

    // Al abrir la pantalla: si ya diste permiso, registra el lugar automáticamente
    LaunchedEffect(Unit) { if (LocationUtil.hasFine(ctx)) locate() else askLocation() }

    fun save() {
        error = null
        error = ReglasCapsula.validar(FormularioCapsula(
            titulo = title, carta = letter, medios = items.size, usarFecha = useDate, abreEl = unlockAt,
            usarLugar = usePlace, soloEnElLugar = requirePlace, tieneUbicacion = lat != null,
            usarClave = useKey, clave = key, claveRepetida = key2,
        ))
        if (error != null) return
        saving = true
        scope.launch {
            runCatching {
                Repo.create(ctx, Draft(
                    title = title, letter = letter, items = items.toList(),
                    unlockAt = if (useDate) unlockAt else null,
                    placeName = if (usePlace) placeName.ifBlank { address } else null,
                    lat = if (usePlace) lat else null, lng = if (usePlace) lng else null,
                    requirePlace = usePlace && requirePlace,
                    shareEmails = ReglasCapsula.correos(emails),
                    key = if (useKey) key else null,
                    voiceNotes = voiceNotes.toList(),
                    songUrl = songUrl, songTitle = songTitle, template = template,
                    questions = questions.toList(), radiusM = radius,
                    openTogether = openTogether && ReglasCapsula.correos(emails).isNotEmpty(), shareContribute = contribute,
                    routeId = routeId, routeOrder = routeId?.let { rid -> runCatching { Repo.nextRouteOrder(rid) }.getOrDefault(1) },
                )) { text, p -> progressText = text; progress = p }
            }.onSuccess { DraftStore.clear(ctx); onCreated(it) }
             .onFailure { error = Repo.friendly(it); saving = false }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.paper(),
        topBar = {
            TopAppBar(
                title = { Text("Nueva cápsula") },
                navigationIcon = { IconButton(onClick = onBack, enabled = !saving) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (restored) BorradorRecuperado {
                DraftStore.clear(ctx); restored = false
                title = ""; letter = ""; items.clear(); voiceNotes.clear(); songTitle = ""; songUrl = ""; questions.clear(); template = null; emails = ""
            }
            SeccionTitulo(title, { title = it }, titleIdeas, pensando = aiBusy == "titulo",
                puedeSugerir = items.any { it.kind == "photo" } || letter.isNotBlank(), iaOcupada = aiBusy != null, onSugerir = ::suggestTitle)
            SeccionCarta(letter, { letter = it }, pensando = aiBusy == "carta", iaOcupada = aiBusy != null, onAyuda = { helpLetter("cálido") },
                puedeDeshacer = letterBefore != null, onDeshacer = { letter = letterBefore!!; letterBefore = null },
                plantilla = template, onPlantilla = { id, texto -> template = id; letter = ReglasCapsula.conPlantilla(letter, texto) })
            SeccionNotasDeVoz(voiceNotes, onBorrar = { i -> voiceNotes.removeAt(i).delete() }, grabando = recording, ms = recMs, onGrabar = {
                if (recording) { recorder.stop()?.let { voiceNotes += it }; recording = false }
                else micPermission.launch(Manifest.permission.RECORD_AUDIO)
            })
            SeccionCancion(songTitle, { songTitle = it }, songUrl, { songUrl = it }, onBuscar = { base -> openSearch(ctx, base, songTitle) })
            SeccionPreguntas(questions, onQuitar = { questions.removeAt(it) }, onAgregar = { q ->
                val nuevas = ReglasCapsula.agregarPregunta(questions, q)
                if (nuevas.size != questions.size) questions += nuevas.last()
            })
            SeccionMedios(items,
                onGaleria = { pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) },
                onCamara = {
                    val dir = File(ctx.cacheDir, "camera").apply { mkdirs() }
                    val file = File(dir, "foto_${System.currentTimeMillis()}.jpg")
                    val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", file)
                    cameraUri = uri
                    takePhoto.launch(uri)
                },
                onQuitar = { items.removeAt(it) })

            HorizontalDivider()
            SeccionFecha(useDate, { useDate = it }, unlockAt, { unlockAt = it }, onElegirFecha = { showDate = true })
            SeccionClave(useKey, { useKey = it }, key, { key = it }, key2, { key2 = it })

            HorizontalDivider()
            SeccionLugar(
                usarLugar = usePlace, onUsarLugar = { on ->
                    usePlace = on
                    if (on && lat == null) { if (LocationUtil.hasFine(ctx)) locate() else askLocation() }
                },
                buscando = locating, lat = lat, lng = lng, precision = accuracy, direccion = address,
                cercanos = nearby, nombre = placeName, onNombre = { placeName = it },
                onActualizar = { if (LocationUtil.hasFine(ctx)) locate() else askLocation() },
                soloAqui = requirePlace, onSoloAqui = { requirePlace = it }, radio = radius, onRadio = { radius = it },
                rutas = routes, rutaId = routeId, onRuta = { routeId = it },
                onCrearRuta = { nombre ->
                    scope.launch {
                        runCatching { Repo.createRoute(nombre) }.onSuccess { r -> routes = listOf(r) + routes; routeId = r.id }
                            .onFailure { error = Repo.friendly(it) }
                    }
                },
            )

            HorizontalDivider()
            SeccionCompartir(emails, { emails = it }, contribute, { contribute = it }, openTogether, { openTogether = it })

            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (saving) {
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                Text(progressText, color = Muted)
            }
            Button(onClick = ::save, enabled = !saving, modifier = Modifier.fillMaxWidth().height(54.dp)) {
                Text(if (saving) "Guardando…" else "Sellar cápsula")
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    // ---------- Selector de fecha y hora ----------
    if (showDate) {
        val todayUtc = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = unlockAt?.let { it.atZone(ZoneId.systemDefault()).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() },
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= todayUtc
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        pickedDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                        showDate = false; showTime = true
                    }
                }) { Text("Siguiente") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Cancelar") } },
        ) { DatePicker(state) }
    }
    if (showTime) {
        val t = rememberTimePickerState(initialHour = 9, initialMinute = 0, is24Hour = false)
        AlertDialog(
            onDismissRequest = { showTime = false },
            title = { Text("¿A qué hora?") },
            text = { TimePicker(t) },
            confirmButton = {
                TextButton(onClick = {
                    pickedDate?.let { d -> unlockAt = d.atTime(LocalTime.of(t.hour, t.minute)).atZone(ZoneId.systemDefault()).toInstant() }
                    showTime = false
                }) { Text("Listo") }
            },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text("Cancelar") } },
        )
    }
}

/** Abre YouTube o Spotify buscando la canción. */
private fun openSearch(ctx: android.content.Context, base: String, q: String) {
    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, AUri.parse(base + AUri.encode(q.trim())))) }
}
