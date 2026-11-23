package com.timecapsule.app.ui.screens

import com.timecapsule.app.util.ErrorLog

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
import com.timecapsule.app.ui.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.timecapsule.app.data.CapsuleRules
import com.timecapsule.app.data.CapsuleForm
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
 * NUEVA CÁPSULA · solo dibuja.
 * El estado y la lógica viven en NewCapsuleViewModel (sobreviven a girar el celular);
 * las secciones están en CreateCapsuleSections.kt y las reglas en data/NewCapsuleForm.kt.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateCapsuleScreen(
    onBack: () -> Unit,
    onCreated: (String) -> Unit,
    vm: NewCapsuleViewModel = viewModel(factory = NewCapsuleViewModel.Factory),
) {
    val ctx = LocalContext.current
    LaunchedEffect(vm.created) { vm.created?.let(onCreated) }

    // Al volver de YouTube/Spotify: si copiaste el enlace de la canción, se pega solo
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) {
                val cb = ctx.getSystemService(ClipboardManager::class.java)
                vm.pasteSongIfAny(runCatching { cb?.primaryClip?.getItemAt(0)?.text?.toString() }.getOrNull().orEmpty())
            }
        }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> vm.toggleRecording(ok) }
    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(20)) { vm.addMedia(it) }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { vm.takenPhoto(it) }
    val locPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        if (res[Manifest.permission.ACCESS_FINE_LOCATION] == true || res[Manifest.permission.ACCESS_COARSE_LOCATION] == true) vm.locate()
        else vm.changeUsePlace(false)
    }
    fun locateWithPermission() {
        if (LocationUtil.hasFine(ctx)) vm.locate()
        else locPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }
    // Al abrir la pantalla por primera vez: registra el lugar (o pide permiso)
    LaunchedEffect(Unit) { if (vm.lat == null && vm.usePlace) locateWithPermission() }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.paper(),
        topBar = {
            TopAppBar(
                title = { Text("Nueva cápsula") },
                navigationIcon = { IconButton(onClick = onBack, enabled = !vm.saving) { Icon(Icons.AutoMirrored.Filled.ArrowBack, com.timecapsule.app.ui.tr("Volver")) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (vm.recoveredDraft) RecoveredDraft { vm.startOver() }
            TitleSection(vm.title, vm::changeTitle, vm.titleIdeas, thinking = vm.aiBusy == "titulo",
                canSuggest = vm.media.any { it.kind == "photo" } || vm.letter.isNotBlank(), aiBusy = vm.aiBusy != null, onSuggest = vm::suggestTitle)
            LetterSection(vm.letter, vm::changeLetter, thinking = vm.aiBusy == "carta", aiBusy = vm.aiBusy != null, onHelp = { vm.improveLetter("cálido") },
                canUndo = vm.previousLetter != null, onUndo = vm::undoLetter,
                template = vm.template, onTemplate = vm::useTemplate)
            VoiceNotesSection(vm.voiceNotes, onDelete = vm::deleteNote, recordingNow = vm.recordingNow, ms = vm.recordedMs, onRecord = {
                if (vm.recordingNow) vm.toggleRecording(true) else micPermission.launch(Manifest.permission.RECORD_AUDIO)
            })
            SongSection(vm.song, vm::changeSong, vm.songLink, vm::changeSongLink, onSearch = { base -> openSearch(ctx, base, vm.song) })
            QuestionsSection(vm.questions, onRemove = vm::removeQuestion, onAdd = vm::addQuestion)
            MediaSection(vm.media,
                onGallery = { pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) },
                onCamera = {
                    val dir = File(ctx.cacheDir, "camera").apply { mkdirs() }
                    val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", File(dir, "foto_${System.currentTimeMillis()}.jpg"))
                    vm.cameraPhoto = uri
                    takePhoto.launch(uri)
                },
                onRemove = vm::removeMedia)

            HorizontalDivider()
            DateSection(vm.useDate, vm::changeUseDate, vm.opensOn, vm::changeOpensOn, onPickDate = { vm.showDate = true })
            KeySection(vm.useKey, vm::changeUseKey, vm.key, vm::changeKey, vm.key2, vm::changeKey2)

            HorizontalDivider()
            PlaceSection(
                usePlace = vm.usePlace, onUsePlace = { on -> vm.changeUsePlace(on); if (on && vm.lat == null) locateWithPermission() },
                searching = vm.searchingPlace, lat = vm.lat, lng = vm.lng, precision = vm.precision, address = vm.address,
                nearbyPlaces = vm.nearbyPlaces, name = vm.placeNameText, onName = vm::changePlaceName,
                onRefresh = ::locateWithPermission,
                onlyHere = vm.onlyAtPlace, onOnlyHere = vm::changeOnlyAtPlace, radius = vm.radius, onRadius = vm::changeRadius,
                routes = vm.routes, routeIdValue = vm.routeIdValue, onRoute = vm::changeRoute,
                onCreateRoute = { vm.createNewRoute(it) },
            )

            HorizontalDivider()
            ShareSection(vm.emails, vm::changeEmails, vm.collaborative, vm::changeCollaborative, vm.openTogether, vm::changeOpenTogether)

            vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (vm.saving) {
                LinearProgressIndicator(progress = { vm.progress }, modifier = Modifier.fillMaxWidth())
                Text(vm.progressText, color = Muted)
            }
            Button(onClick = vm::seal, enabled = !vm.saving, modifier = Modifier.fillMaxWidth().height(54.dp)) {
                Text(if (vm.saving) "Guardando…" else "Sellar cápsula")
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    // ---------- Selector de fecha y hora ----------
    if (vm.showDate) {
        val todayUtc = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = vm.opensOn?.let { it.atZone(ZoneId.systemDefault()).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() },
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= todayUtc
            },
        )
        DatePickerDialog(
            onDismissRequest = { vm.showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        vm.chosenDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                        vm.showDate = false; vm.showTime = true
                    }
                }) { Text("Siguiente") }
            },
            dismissButton = { TextButton(onClick = { vm.showDate = false }) { Text("Cancelar") } },
        ) { DatePicker(state) }
    }
    if (vm.showTime) {
        val t = rememberTimePickerState(initialHour = 9, initialMinute = 0, is24Hour = false)
        AlertDialog(
            onDismissRequest = { vm.showTime = false },
            title = { Text("¿A qué hora?") },
            text = { TimePicker(t) },
            confirmButton = {
                TextButton(onClick = {
                    vm.chosenDate?.let { d -> vm.changeOpensOn(d.atTime(LocalTime.of(t.hour, t.minute)).atZone(ZoneId.systemDefault()).toInstant()) }
                    vm.showTime = false
                }) { Text("Listo") }
            },
            dismissButton = { TextButton(onClick = { vm.showTime = false }) { Text("Cancelar") } },
        )
    }
}

/** Abre YouTube o Spotify buscando la canción. */
private fun openSearch(ctx: android.content.Context, base: String, q: String) {
    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, AUri.parse(base + AUri.encode(q.trim())))) }.onFailure { ErrorLog.record("CreateCapsuleScreen:291", it) }
}
