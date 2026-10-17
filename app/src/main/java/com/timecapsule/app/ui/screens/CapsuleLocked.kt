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
   CÁPSULA CERRADA · cuenta regresiva, abrir con clave o en el lugar
   (parte de la pantalla de una cápsula: CapsuleDetailScreen)
   ========================================================= */

/* ---------------- Cápsula cerrada ---------------- */
@Composable
internal fun LockedPanel(c: Capsule, onOpened: () -> Unit, onTimeUp: () -> Unit) {
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
            .goldBorder(RoundedCornerShape(8.dp)).padding(24.dp),
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
internal fun Countdown(d: Duration) {
    val parts = listOf(
        d.toDays() to "días", d.toHours() % 24 to "horas", d.toMinutes() % 60 to "min", d.seconds % 60 to "seg",
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        parts.forEach { (v, l) -> FlipNumber(v, l) }
    }
}
