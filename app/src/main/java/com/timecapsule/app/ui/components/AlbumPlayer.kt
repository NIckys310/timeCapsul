package com.timecapsule.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.timecapsule.app.data.Album
import com.timecapsule.app.data.Capsule
import com.timecapsule.app.data.Repo
import com.timecapsule.app.ui.screens.CloudImage
import com.timecapsule.app.ui.theme.LocalPalette
import com.timecapsule.app.ui.theme.Typewriter
import kotlinx.coroutines.delay

/**
 * Álbum animado: portada, fotos con zoom lento (efecto Ken Burns), textos de la IA
 * y un cierre. Si la IA no está disponible, muestra las fotos sin textos.
 */
@Composable
fun AlbumPlayer(c: Capsule, photoPaths: List<String>, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val p = LocalPalette.current
    var album by remember { mutableStateOf<Album?>(null) }
    var loading by remember { mutableStateOf(true) }
    var step by remember { mutableIntStateOf(-1) }          // -1 portada · 0..n-1 fotos · n cierre
    LaunchedEffect(c.id) {
        album = runCatching { Repo.aiAlbum(c.id) }.getOrNull()
        loading = false
    }
    val paths = album?.paths?.takeIf { it.isNotEmpty() } ?: photoPaths
    val n = paths.size
    LaunchedEffect(loading) {
        if (loading) return@LaunchedEffect
        while (step < n) { delay(if (step == -1) 3000 else 4500); step++ }
        delay(4000); onClose()
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            if (loading) Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = p.gold)
                Text("Preparando tu álbum…", color = Color(0xFFE9DFCB), fontFamily = Typewriter, modifier = Modifier.padding(top = 12.dp))
            } else AnimatedContent(step, transitionSpec = { fadeIn(tween(900)) togetherWith fadeOut(tween(900)) }, label = "álbum") { s ->
                when {
                    s == -1 -> Slide(title = c.title, text = album?.intro ?: c.placeName.orEmpty())
                    s >= n -> Slide(title = "Fin", text = album?.cierre ?: "Gracias por guardar este recuerdo.")
                    else -> KenBurns(paths[s], album?.fotos?.getOrNull(s), s)
                }
            }
            // Puntos de avance
            Row(Modifier.align(Alignment.TopCenter).padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(n + 2) { i -> Box(Modifier.size(width = 18.dp, height = 3.dp).clip(CircleShape).background(if (i <= step + 1) p.gold else Color.White.copy(alpha = 0.3f))) }
            }
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) { Icon(Icons.Filled.Close, "Cerrar", tint = Color.White) }
            if (!c.songUrl.isNullOrBlank() || !c.songTitle.isNullOrBlank()) TextButton(
                onClick = {
                    val url = c.songUrl?.takeIf { it.startsWith("http") } ?: "https://www.youtube.com/results?search_query=" + android.net.Uri.encode(c.songTitle.orEmpty())
                    runCatching { ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))) }
                },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp),
            ) { Icon(Icons.Filled.MusicNote, null, tint = p.gold); Text(" Poner la canción: ${c.songTitle ?: "abrir enlace"}", color = p.gold) }
            Box(Modifier.fillMaxSize().clickable { if (!loading) step = (step + 1).coerceAtMost(n) })
        }
    }
}

@Composable
private fun Slide(title: String, text: String) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        WaxSeal(size = 64.dp, glow = true)
        Text(title, style = MaterialTheme.typography.displaySmall, color = Color(0xFFF7EEDC), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 18.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, color = p.gold, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun KenBurns(path: String, caption: String?, index: Int) {
    val zoom = remember { Animatable(1f) }
    LaunchedEffect(path) { zoom.animateTo(1.14f, tween(5200, easing = LinearEasing)) }
    val dir = if (index % 2 == 0) 1f else -1f
    Box(Modifier.fillMaxSize()) {
        CloudImage(path, Modifier.fillMaxSize().graphicsLayer {
            scaleX = zoom.value; scaleY = zoom.value
            translationX = dir * (zoom.value - 1f) * 220f
        })
        if (!caption.isNullOrBlank()) Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
                .padding(start = 24.dp, end = 24.dp, top = 40.dp, bottom = 70.dp)
        ) { Text(caption, color = Color(0xFFF7EEDC), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
    }
}
