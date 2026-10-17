package com.timecapsule.app.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.timecapsule.app.data.Capsule
import com.timecapsule.app.data.Media
import com.timecapsule.app.data.Member
import com.timecapsule.app.data.Repo
import com.timecapsule.app.notify.UnlockWorker
import com.timecapsule.app.ui.theme.CapsuleGradient
import com.timecapsule.app.ui.theme.Cream
import com.timecapsule.app.ui.theme.Gold
import com.timecapsule.app.ui.theme.LockedGradient
import com.timecapsule.app.ui.theme.Muted
import com.timecapsule.app.ui.theme.Night
import com.timecapsule.app.ui.theme.NightRaised
import com.timecapsule.app.ui.theme.NightSurface
import com.timecapsule.app.util.LocationUtil
import com.timecapsule.app.util.Speech
import com.timecapsule.app.data.AppSettings
import com.timecapsule.app.ui.components.FlipNumber
import com.timecapsule.app.ui.components.goldBorder
import com.timecapsule.app.ui.components.AlbumPlayer
import com.timecapsule.app.ui.components.OpeningCeremony
import com.timecapsule.app.ui.components.Polaroid
import com.timecapsule.app.ui.components.WaxSeal
import com.timecapsule.app.ui.components.floating
import com.timecapsule.app.ui.components.paper
import com.timecapsule.app.ui.theme.LetterInk
import com.timecapsule.app.ui.theme.LetterPaper
import com.timecapsule.app.ui.theme.Line
import com.timecapsule.app.ui.theme.Olive
import com.timecapsule.app.ui.theme.Coral
import com.timecapsule.app.ui.theme.Typewriter
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Stop
import androidx.compose.foundation.border
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import com.timecapsule.app.util.fmtDate
import com.timecapsule.app.util.fmtDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CapsuleDetailScreen(id: String, onBack: () -> Unit, onChanged: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var capsule by remember { mutableStateOf<Capsule?>(null) }
    var media by remember { mutableStateOf<List<Media>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var msg by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableStateOf(0) }
    var viewer by remember { mutableStateOf<Media?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var justOpened by remember { mutableStateOf(false) }
    var ceremony by remember { mutableStateOf(false) }
    var album by remember { mutableStateOf(false) }
    val me = Repo.userId()

    LaunchedEffect(id, reload) {
        loading = true
        runCatching {
            capsule = Repo.capsule(id)
            media = if (capsule?.isOpen == true) Repo.media(id) else emptyList()
        }.onFailure { msg = Repo.friendly(it) }
        loading = false
        // Recién abierta: ceremonia del sello y, si está activado, lectura de la carta
        if (justOpened && capsule?.isOpen == true) { ceremony = true; justOpened = false }
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.paper(),
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } },
                actions = {
                    if (capsule?.owner == me && capsule != null) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, "Eliminar") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { pad ->
        val c = capsule
        if (c == null) {
            Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) {
                if (loading) CircularProgressIndicator() else Text(msg ?: "No se encontró la cápsula.", color = Muted)
            }
            return@Scaffold
        }
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(c.title, style = MaterialTheme.typography.headlineMedium)
            Text(
                buildString {
                    append("Sellada el ${c.createdInstant.fmtDate()}")
                    if (c.owner != me) append(" por ${c.ownerName ?: "alguien"}")
                }, color = Muted, fontFamily = Typewriter, style = MaterialTheme.typography.labelSmall,
            )
            c.placeName?.let { Meta(Icons.Filled.Place, it, Gold) }

            RouteBadge(c)
            if (!c.isOpen) {
                LockedPanel(c, onOpened = { justOpened = true; reload++; onChanged() }, onTimeUp = { justOpened = true; reload++; onChanged() })
                OpenTogetherPanel(c, onOpened = { justOpened = true; reload++; onChanged() })
                HotColdFinder(c)
                ContributePanel(c, onAdded = { reload++ })
            } else {
                OpenContent(c, media.filter { it.kind != "audio" }, onView = { viewer = it }, onSummary = { reload++ })
                val photoPaths = media.filter { it.kind == "photo" }.map { it.path }
                if (photoPaths.isNotEmpty()) Button(onClick = { album = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.AutoAwesome, null); Spacer(Modifier.width(8.dp)); Text("Ver álbum animado")
                }
                if (album) AlbumPlayer(c, photoPaths) { album = false }
                SongCard(c)
                VoiceNotes(media)
                ContributorLetters(c)
                QuestionsCard(c)
                ReactionsBar(c)
                CommentsCard(c)
            }

            if (c.owner == me) SharePanel(c.id)
            msg?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(28.dp))
        }
    }

    viewer?.let { m -> MediaViewer(m) { viewer = null } }

    if (ceremony) capsule?.let { c ->
        OpeningCeremony(c.title, media.filter { it.kind == "photo" }.map { it.path }) {
            ceremony = false
            if (AppSettings.look.value.readAloud && !c.letter.isNullOrBlank()) Speech.speak(ctx, c.letter)
        }
    }
    DisposableEffect(Unit) { onDispose { Speech.stop() } }

    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("¿Eliminar esta cápsula?") },
        text = { Text("Se borrarán la carta, las fotos y los videos para todos. No se puede deshacer.") },
        confirmButton = {
            TextButton(onClick = {
                confirmDelete = false
                scope.launch {
                    runCatching { Repo.delete(id) }
                        .onSuccess { UnlockWorker.cancel(ctx, id); onChanged(); onBack() }
                        .onFailure { msg = Repo.friendly(it) }
                }
            }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } },
    )
}

/* ---------------- Ver foto o video en grande ---------------- */
@Composable
private fun MediaViewer(m: Media, onClose: () -> Unit) {
    var url by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(m.path) { url = runCatching { Repo.signedUrl(m.path) }.getOrNull() }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            val u = url
            when {
                u == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                m.kind == "photo" -> AsyncImage(u, null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                else -> VideoPlayer(u)
            }
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                Icon(Icons.Filled.Close, "Cerrar", tint = Color.White)
            }
        }
    }
}

@Composable
private fun VideoPlayer(url: String) {
    val ctx = LocalContext.current
    val player = remember(url) {
        ExoPlayer.Builder(ctx).build().apply {
            setMediaItem(MediaItem.fromUri(url)); prepare(); playWhenReady = true
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    AndroidView(factory = { PlayerView(it).apply { this.player = player } }, modifier = Modifier.fillMaxSize())
}

/* ---------------- Compartir ---------------- */
@Composable
private fun SharePanel(capsuleId: String) {
    val scope = rememberCoroutineScope()
    var members by remember { mutableStateOf<List<Member>>(emptyList()) }
    var email by remember { mutableStateOf("") }
    var contribute by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    LaunchedEffect(capsuleId) { members = runCatching { Repo.members(capsuleId) }.getOrDefault(emptyList()) }

    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(NightSurface).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Meta(Icons.Filled.People, "Compartida con", Cream)
        if (members.isEmpty()) Text("Solo tú por ahora.", color = Muted)
        members.forEach { Text("• ${it.name ?: it.email}", color = Cream) }
        OutlinedTextField(email, { email = it }, label = { Text("Correo de familiar o amigo") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            busy = true; note = null
            scope.launch {
                runCatching { Repo.share(capsuleId, email, contribute) }
                    .onSuccess { name -> note = "Compartida con $name"; email = ""; members = runCatching { Repo.members(capsuleId) }.getOrDefault(members) }
                    .onFailure { note = Repo.friendly(it) }
                busy = false
            }
        }, enabled = email.contains('@') && !busy) { Text("Compartir") }
        SwitchRow("Que pueda agregar sus fotos y cartas", contribute) { contribute = it }
        note?.let { Text(it, color = Gold) }
        InviteButton(capsuleId)
    }
}
