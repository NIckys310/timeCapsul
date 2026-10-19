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

/* =========================================================
   CÁPSULAS EN GRUPO · aportar, abrir juntos y rutas
   (parte de la pantalla de una cápsula)
   ========================================================= */

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
