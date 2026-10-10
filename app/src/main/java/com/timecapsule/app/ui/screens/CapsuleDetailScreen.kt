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

            if (!c.isOpen) LockedPanel(c, onOpened = { justOpened = true; reload++; onChanged() }, onTimeUp = { justOpened = true; reload++; onChanged() })
            else OpenContent(c, media, onView = { viewer = it }, onSummary = { reload++ })

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

/* ---------------- Cápsula cerrada ---------------- */
@Composable
private fun LockedPanel(c: Capsule, onOpened: () -> Unit, onTimeUp: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var now by remember { mutableStateOf(Instant.now()) }
    var checking by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }
    val target = c.unlockInstant

    LaunchedEffect(target) {
        while (target != null && now.isBefore(target)) { delay(1000); now = Instant.now() }
        if (target != null && !c.requirePlace) onTimeUp()
    }

    fun tryOpenHere() {
        checking = true; note = null
        scope.launch {
            val loc = LocationUtil.current(ctx)
            if (loc == null) note = "No pudimos saber dónde estás. Activa el GPS y da permiso de ubicación."
            else {
                val dist = LocationUtil.distanceMeters(loc.latitude, loc.longitude, c.lat!!, c.lng!!)
                if (dist <= c.radiusM + loc.accuracy) {
                    runCatching { Repo.open(c.id) }
                        .onSuccess { ok -> if (ok) onOpened() else note = "Todavía no llega la fecha de apertura." }
                        .onFailure { note = Repo.friendly(it) }
                } else note = "Estás a ${if (dist > 1000) "%.1f km".format(dist / 1000) else "${dist.toInt()} m"} de ${c.placeName ?: "su lugar"}. Acércate para abrirla."
            }
            checking = false
        }
    }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { tryOpenHere() }

    Column(
        Modifier.fillMaxWidth().floating(amplitude = 2.dp).clip(RoundedCornerShape(8.dp)).background(LockedGradient)
            .border(1.dp, Line, RoundedCornerShape(8.dp)).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        WaxSeal(size = 76.dp, glow = target == null || !now.isBefore(target))
        if (target != null && now.isBefore(target)) {
            Text("Se abre en", color = Muted)
            Countdown(Duration.between(now, target))
            Text(target.fmtDateTime(), color = Cream)
        }
        if (c.requirePlace) {
            Text(
                if (target == null || !now.isBefore(target)) "Ve a ${c.placeName ?: "su lugar"} y ábrela allí."
                else "Además, solo se abrirá estando en ${c.placeName ?: "su lugar"}.",
                textAlign = TextAlign.Center, color = Cream,
            )
            if (target == null || !now.isBefore(target)) Button(onClick = {
                perm.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }, enabled = !checking) { Text(if (checking) "Revisando ubicación…" else "Estoy aquí, abrir") }
        }
        if (c.mediaCount > 0) Text("${c.mediaCount} fotos y videos esperando", color = Muted)
        note?.let { Text(it, color = Gold, textAlign = TextAlign.Center) }

        // ---------- Abrir antes con la clave ----------
        if (c.hasKey) {
            var key by remember { mutableStateOf("") }
            var opening by remember { mutableStateOf(false) }
            var keyMsg by remember { mutableStateOf<String?>(null) }
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                key, { key = it; keyMsg = null }, label = { Text("Clave") }, singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Key, null) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = {
                opening = true
                scope.launch {
                    runCatching { Repo.openWithKey(c.id, key) }
                        .onSuccess { ok -> if (ok) onOpened() else keyMsg = "Clave incorrecta." }
                        .onFailure { keyMsg = Repo.friendly(it) }
                    opening = false
                }
            }, enabled = key.isNotEmpty() && !opening, modifier = Modifier.fillMaxWidth()) {
                Text(if (opening) "Abriendo…" else "Abrir ahora con la clave")
            }
            keyMsg?.let { Text(it, color = Gold) }
        }
    }
}

@Composable
private fun Countdown(d: Duration) {
    val parts = listOf(
        d.toDays() to "días", d.toHours() % 24 to "horas", d.toMinutes() % 60 to "min", d.seconds % 60 to "seg",
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        parts.forEach { (v, l) -> FlipNumber(v, l) }
    }
}

/* ---------------- Cápsula abierta ---------------- */
@Composable
private fun OpenContent(c: Capsule, media: List<Media>, onView: (Media) -> Unit, onSummary: () -> Unit) {
    val scope = rememberCoroutineScope()
    var summary by remember(c.id) { mutableStateOf(c.aiSummary) }
    var aiBusy by remember { mutableStateOf(false) }
    var aiError by remember { mutableStateOf<String?>(null) }

    Row(Modifier.clip(RoundedCornerShape(50)).background(Olive).padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.LockOpen, null, tint = LetterPaper, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp)); Text("Abierta", color = LetterPaper, fontFamily = Typewriter)
    }

    // Resumen con IA
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(NightRaised).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Meta(Icons.Filled.AutoAwesome, "Recuerdo resumido por IA", Gold)
        when {
            aiBusy -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp); Spacer(Modifier.width(10.dp)); Text("La IA está mirando tus fotos…", color = Muted)
            }
            summary != null -> Text(summary!!, color = Cream, style = MaterialTheme.typography.bodyLarge)
            else -> OutlinedButton(onClick = {
                aiBusy = true; aiError = null
                scope.launch {
                    runCatching { Repo.aiSummary(c.id) }
                        .onSuccess { summary = it; onSummary() }
                        .onFailure { aiError = Repo.friendly(it) }
                    aiBusy = false
                }
            }) { Text("Resumir este recuerdo") }
        }
        aiError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }

    // Carta
    if (!c.letter.isNullOrBlank()) {
        val ctx = LocalContext.current
        var speaking by remember { mutableStateOf(false) }
        val lineColor = Line
        val marginColor = Coral.copy(alpha = 0.35f)
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(LetterPaper)
                .drawBehind {
                    // Renglones y margen rojo como en una hoja de cuaderno antigua
                    val step = 28.dp.toPx()
                    var y = 56.dp.toPx()
                    while (y < size.height) { drawLine(lineColor.copy(alpha = 0.55f), Offset(0f, y), Offset(size.width, y), 1f); y += step }
                    drawLine(marginColor, Offset(36.dp.toPx(), 0f), Offset(36.dp.toPx(), size.height), 2f)
                }
                .padding(start = 46.dp, end = 18.dp, top = 18.dp, bottom = 22.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Carta", color = Color(0xFF7A5C3A), fontFamily = Typewriter, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    if (speaking) { Speech.stop(); speaking = false } else { Speech.speak(ctx, c.letter); speaking = true }
                }) { Icon(if (speaking) Icons.Filled.Stop else Icons.AutoMirrored.Filled.VolumeUp, if (speaking) "Detener lectura" else "Leer en voz alta", tint = Color(0xFF7A5C3A)) }
            }
            Text(c.letter, color = LetterInk, style = MaterialTheme.typography.bodyLarge.copy(lineHeight = MaterialTheme.typography.bodyLarge.fontSize * 1.75f))
        }
    }

    // Galería
    if (media.isNotEmpty()) {
        Text("Recuerdos (${media.size})", style = MaterialTheme.typography.titleMedium)
        media.chunked(2).forEachIndexed { r, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                row.forEachIndexed { i, m ->
                    val tilt = listOf(-3f, 2.5f, 1.5f, -2f)[(r * 2 + i) % 4]
                    Polaroid(Modifier.weight(1f).clickable { onView(m) }, rotation = tilt) {
                        if (m.kind == "photo") CloudImage(m.path, Modifier.fillMaxSize())
                        else Icon(Icons.Filled.PlayCircle, "Video", tint = Gold, modifier = Modifier.align(Alignment.Center).size(44.dp))
                    }
                }
                repeat(2 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
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
    var note by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    LaunchedEffect(capsuleId) { members = runCatching { Repo.members(capsuleId) }.getOrDefault(emptyList()) }

    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(NightSurface).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Meta(Icons.Filled.People, "Compartida con", Cream)
        if (members.isEmpty()) Text("Solo tú por ahora.", color = Muted)
        members.forEach { Text("• ${it.name ?: it.email}", color = Cream) }
        OutlinedTextField(email, { email = it }, label = { Text("Correo de familiar o amigo") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            busy = true; note = null
            scope.launch {
                runCatching { Repo.share(capsuleId, email) }
                    .onSuccess { name -> note = "Compartida con $name"; email = ""; members = runCatching { Repo.members(capsuleId) }.getOrDefault(members) }
                    .onFailure { note = Repo.friendly(it) }
                busy = false
            }
        }, enabled = email.contains('@') && !busy) { Text("Compartir") }
        note?.let { Text(it, color = Gold) }
    }
}
