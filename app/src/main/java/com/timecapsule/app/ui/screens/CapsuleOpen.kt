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

/* =========================================================
   CÁPSULA ABIERTA · carta, fotos, resumen de la IA y extras
   (parte de la pantalla de una cápsula: CapsuleDetailScreen)
   ========================================================= */

/* ---------------- Cápsula abierta ---------------- */
@Composable
internal fun OpenContent(c: Capsule, media: List<Media>, onView: (Media) -> Unit, onSummary: () -> Unit) {
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
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(NightRaised).padding(18.dp),
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
