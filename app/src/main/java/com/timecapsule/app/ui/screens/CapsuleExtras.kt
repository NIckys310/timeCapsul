package com.timecapsule.app.ui.screens

import com.timecapsule.app.ui.components.goldBorder
import com.timecapsule.app.ui.components.enterAnim
import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.timecapsule.app.data.Answer
import com.timecapsule.app.data.Capsule
import com.timecapsule.app.data.Comment
import com.timecapsule.app.data.Letter
import com.timecapsule.app.data.Media
import com.timecapsule.app.data.PickedItem
import com.timecapsule.app.data.Reaction
import com.timecapsule.app.data.Repo
import com.timecapsule.app.ui.components.VoiceNotePlayer
import com.timecapsule.app.ui.theme.LocalPalette
import com.timecapsule.app.ui.theme.Typewriter
import com.timecapsule.app.util.MediaUtil
import com.timecapsule.app.util.VoiceRecorder
import com.timecapsule.app.util.fmtDuration
import com.timecapsule.app.util.fmtDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.time.OffsetDateTime

internal val keyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)

@Composable
internal fun Section(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable () -> Unit) {
    val p = LocalPalette.current
    Column(
        Modifier.fillMaxWidth().enterAnim().clip(RoundedCornerShape(8.dp)).background(p.surface).goldBorder(RoundedCornerShape(8.dp))
            .padding(16.dp).animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Meta(icon, title, p.ink)
        content()
    }
}

/* ---------------- Canción del momento ---------------- */
@Composable
fun SongCard(c: Capsule) {
    if (c.songTitle.isNullOrBlank() && c.songUrl.isNullOrBlank()) return
    val ctx = LocalContext.current
    Section("Canción del momento", Icons.Filled.MusicNote) {
        Text(c.songTitle ?: "Escuchar", style = MaterialTheme.typography.titleMedium, fontStyle = FontStyle.Italic)
        Button(onClick = {
            val url = c.songUrl?.takeIf { it.startsWith("http") }
                ?: "https://www.youtube.com/results?search_query=" + Uri.encode(c.songTitle.orEmpty())
            runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        }) { Icon(Icons.Filled.MusicNote, null); Spacer(Modifier.width(6.dp)); Text("Escuchar") }
    }
}

/* ---------------- Notas de voz ---------------- */
@Composable
fun VoiceNotes(media: List<Media>) {
    val notes = media.filter { it.kind == "audio" }
    if (notes.isEmpty()) return
    Section("Notas de voz (${notes.size})", Icons.Filled.Mic) {
        notes.forEachIndexed { i, m ->
            var url by remember(m.path) { mutableStateOf<String?>(null) }
            LaunchedEffect(m.path) { url = runCatching { Repo.signedUrl(m.path) }.getOrNull() }
            VoiceNotePlayer(url, m.addedByName?.let { "Nota de $it" } ?: "Nota ${i + 1}")
        }
    }
}

/* ---------------- Aportar a una cápsula colaborativa (antes de abrirse) ---------------- */
@Composable
fun ContributePanel(c: Capsule, onAdded: () -> Unit) {
    if (!c.canAdd || c.isOpen || c.owner == Repo.userId()) return
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val items = remember { mutableStateListOf<PickedItem>() }
    val voice = remember { mutableStateListOf<File>() }
    val recorder = remember { VoiceRecorder(ctx) }
    var recording by remember { mutableStateOf(false) }
    var recMs by remember { mutableStateOf(0L) }
    var letter by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
        uris.forEach { u -> items += PickedItem(u, MediaUtil.kindOf(ctx, u)) }
    }
    val mic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) { recorder.start(); recording = true } }
    LaunchedEffect(recording) { while (recording) { recMs = recorder.elapsedMs; delay(200) } }
    DisposableEffect(Unit) { onDispose { recorder.stop() } }

    Section("Agrega tus recuerdos a esta cápsula", Icons.Filled.AddPhotoAlternate) {
        Text("Es una cápsula colaborativa: tus fotos, notas de voz y carta se verán cuando se abra.", color = LocalPalette.current.muted)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) }) { Text("Fotos (${items.size})") }
            OutlinedButton(onClick = {
                if (recording) { recorder.stop()?.let { voice += it }; recording = false } else mic.launch(Manifest.permission.RECORD_AUDIO)
            }) { Icon(if (recording) Icons.Filled.Stop else Icons.Filled.Mic, null); Text(if (recording) " ${fmtDuration(recMs)}" else " Voz (${voice.size})") }
        }
        OutlinedTextField(letter, { letter = it }, label = { Text("Tu carta") }, minLines = 3, keyboardOptions = keyboard, modifier = Modifier.fillMaxWidth())
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        Button(enabled = !busy && (items.isNotEmpty() || voice.isNotEmpty() || letter.isNotBlank()), onClick = {
            busy = true
            scope.launch {
                runCatching {
                    Repo.addItems(ctx, c.id, items.toList(), voice.toList()) { status = it }
                    if (letter.isNotBlank()) Repo.addLetter(c.id, letter)
                }.onSuccess { status = "¡Listo! Tus recuerdos quedaron sellados."; items.clear(); voice.clear(); letter = ""; onAdded() }
                 .onFailure { status = Repo.friendly(it) }
                busy = false
            }
        }) { Text("Sellar mis recuerdos") }
        status?.let { Text(it, color = LocalPalette.current.gold) }
    }
}

/* ---------------- Abrir juntos ---------------- */
@Composable
fun OpenTogetherPanel(c: Capsule, onOpened: () -> Unit) {
    if (!c.openTogether || c.isOpen || !c.dateReady) return
    val scope = rememberCoroutineScope()
    var voted by remember { mutableStateOf(c.myVote) }
    var votes by remember { mutableStateOf(c.votes) }
    val p = LocalPalette.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Abrir juntos: $votes de ${c.members} aceptaron", fontFamily = Typewriter, color = p.ink)
        LinearProgressIndicator(progress = { votes.toFloat() / c.members.coerceAtLeast(1) }, modifier = Modifier.fillMaxWidth(), color = p.olive)
        if (!voted) Button(onClick = {
            scope.launch {
                runCatching { Repo.voteOpen(c.id) }.onSuccess { open -> voted = true; votes++; if (open) onOpened() }
            }
        }) { Icon(Icons.Filled.Groups, null); Spacer(Modifier.width(6.dp)); Text("Acepto abrirla") }
        else Text("Ya aceptaste. Se abrirá cuando todos acepten.", color = p.muted)
    }
}

/* ---------------- Parte de una ruta ---------------- */
@Composable
fun RouteBadge(c: Capsule) {
    if (c.routeId == null) return
    Meta(Icons.Filled.Route, "Parada ${c.routeOrder ?: 1} de la ruta “${c.routeTitle ?: "Ruta"}”", LocalPalette.current.gold)
}

/* ---------------- Invitar por enlace o QR ---------------- */
@Composable
fun InviteButton(capsuleId: String) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf<String?>(null) }
    var contribute by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var ask by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { ask = true }, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Filled.QrCode2, null); Spacer(Modifier.width(6.dp)); Text("Invitar con enlace o QR")
    }
    if (ask) AlertDialog(
        onDismissRequest = { ask = false; code = null },
        title = { Text(if (code == null) "Invitar a la cápsula" else "Código de invitación") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val c = code
                if (c == null) {
                    SwitchRow("Que pueda agregar sus fotos y cartas", contribute) { contribute = it }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                } else {
                    val link = "timecapsule://invitar/$c"
                    val bmp = remember(link) { qrBitmap(link, 640) }
                    Image(bmp.asImageBitmap(), "Código QR", Modifier.size(220.dp).background(androidx.compose.ui.graphics.Color.White).padding(8.dp))
                    Text(c, fontFamily = Typewriter, fontSize = 28.sp, letterSpacing = 4.sp)
                    Text("Escanéalo con la cámara de otro celular que tenga Time Capsule, o compártelo: en la app se escribe en “Unirme con código”. Vence en 7 días.",
                        color = LocalPalette.current.muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            val c = code
            if (c == null) TextButton(onClick = {
                scope.launch { runCatching { Repo.createInvite(capsuleId, contribute) }.onSuccess { code = it }.onFailure { error = Repo.friendly(it) } }
            }) { Text("Crear código") }
            else TextButton(onClick = {
                val text = "Te invito a mi cápsula del tiempo 💌\nAbre Time Capsule → “Unirme con código” y escribe: $c\n(o toca: timecapsule://invitar/$c)"
                runCatching { ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "Compartir invitación")) }
            }) { Icon(Icons.Filled.Share, null); Spacer(Modifier.width(4.dp)); Text("Compartir") }
        },
        dismissButton = { TextButton(onClick = { ask = false; code = null }) { Text("Cerrar") } },
    )
}

fun qrBitmap(text: String, size: Int): Bitmap {
    val m = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
    for (x in 0 until size) for (y in 0 until size) bmp.setPixel(x, y, if (m[x, y]) 0xFF2E241C.toInt() else 0xFFFFFFFF.toInt())
    return bmp
}

/* ---------------- Caliente o frío (búsqueda del tesoro) ---------------- */
@Composable
fun HotColdFinder(c: Capsule) {
    if (c.isOpen || !c.hasPlace) return
    val ctx = LocalContext.current
    val p = LocalPalette.current
    var searching by remember { mutableStateOf(false) }
    var dist by remember { mutableStateOf<Float?>(null) }
    var prev by remember { mutableStateOf<Float?>(null) }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        searching = res.values.any { it }
    }
    LaunchedEffect(searching) {
        while (searching) {
            val loc = com.timecapsule.app.util.LocationUtil.current(ctx)
            if (loc != null) { prev = dist; dist = com.timecapsule.app.util.LocationUtil.distanceMeters(loc.latitude, loc.longitude, c.lat!!, c.lng!!) }
            delay(4000)
        }
    }
    Section("Búsqueda del tesoro", Icons.Filled.Route) {
        if (!searching) {
            Text("¿No recuerdas exactamente dónde está? Te guiamos con “caliente o frío”.", color = p.muted)
            OutlinedButton(onClick = {
                if (com.timecapsule.app.util.LocationUtil.hasFine(ctx)) searching = true
                else perm.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }) { Text("Empezar a buscar") }
        } else {
            val d = dist
            val pista = com.timecapsule.app.util.pistaCalienteFrio(d, c.radiusM)
            val color = when (pista) {
                com.timecapsule.app.util.Pista.BUSCANDO -> p.muted
                com.timecapsule.app.util.Pista.LLEGASTE -> p.olive
                com.timecapsule.app.util.Pista.MUY_CALIENTE -> p.wax
                com.timecapsule.app.util.Pista.CALIENTE, com.timecapsule.app.util.Pista.TIBIO -> p.gold
                com.timecapsule.app.util.Pista.FRIO -> androidx.compose.ui.graphics.Color(0xFF6FA8C9)
            }
            val label = pista.texto
            Text(label, style = MaterialTheme.typography.headlineSmall, color = color)
            if (d != null) {
                val trend = com.timecapsule.app.util.tendencia(d, prev)
                Text("${com.timecapsule.app.util.distanciaTexto(d)} · $trend", fontFamily = Typewriter, color = p.muted)
                LinearProgressIndicator(progress = { (1f - (d / 5000f)).coerceIn(0.03f, 1f) }, modifier = Modifier.fillMaxWidth(), color = color)
            }
            TextButton(onClick = { searching = false }) { Text("Detener") }
        }
    }
}
