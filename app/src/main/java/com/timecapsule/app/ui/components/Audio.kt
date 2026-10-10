package com.timecapsule.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.timecapsule.app.ui.theme.LocalPalette
import com.timecapsule.app.ui.theme.Typewriter
import com.timecapsule.app.util.fmtDuration
import kotlinx.coroutines.delay

/**
 * Reproductor de nota de voz (archivo local o enlace de la nube).
 * source: ruta de archivo, content:// o https://
 */
@Composable
fun VoiceNotePlayer(source: String?, label: String, onDelete: (() -> Unit)? = null) {
    val ctx = LocalContext.current
    val p = LocalPalette.current
    var playing by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableLongStateOf(0L) }
    val player = remember(source) {
        source?.let { src ->
            ExoPlayer.Builder(ctx).build().apply {
                setMediaItem(MediaItem.fromUri(src)); prepare()
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_READY) duration = this@apply.duration.coerceAtLeast(0)
                        if (state == Player.STATE_ENDED) { seekTo(0); pause(); progress = 0f }
                    }
                })
            }
        }
    }
    DisposableEffect(player) { onDispose { player?.release() } }
    LaunchedEffect(playing) {
        while (playing && player != null) {
            progress = if (player.duration > 0) player.currentPosition.toFloat() / player.duration else 0f
            delay(150)
        }
    }
    Row(
        Modifier.fillMaxWidth().background(p.surface, RoundedCornerShape(6.dp)).border(1.dp, p.line, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { player?.let { if (it.isPlaying) it.pause() else it.play() } }, enabled = player != null) {
            Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (playing) "Pausar" else "Escuchar", tint = p.wax)
        }
        androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Mic, null, tint = p.muted, modifier = Modifier.padding(end = 4.dp).height(14.dp))
                Text(label, color = p.ink, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(fmtDuration(duration), color = p.muted, fontFamily = Typewriter, style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = p.wax, trackColor = p.raised)
        }
        Spacer(Modifier.width(4.dp))
        if (onDelete != null) IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "Quitar nota", tint = p.muted) }
    }
}
