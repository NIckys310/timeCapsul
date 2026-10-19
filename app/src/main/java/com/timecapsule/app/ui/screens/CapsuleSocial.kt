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
   REACCIONES, COMENTARIOS Y CARTAS DE LOS COLABORADORES
   (parte de la pantalla de una cápsula)
   ========================================================= */

/* ---------------- Reacciones ---------------- */
private val EMOJIS = listOf("❤️", "😂", "😢", "😮", "🙏", "🔥")

@Composable
fun ReactionsBar(c: Capsule) {
    val scope = rememberCoroutineScope()
    val me = Repo.userId()
    var list by remember { mutableStateOf<List<Reaction>>(emptyList()) }
    LaunchedEffect(c.id) { list = runCatching { Repo.reactions(c.id) }.getOrDefault(emptyList()) }
    val mine = list.firstOrNull { it.userId == me }?.emoji
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        EMOJIS.forEach { e ->
            val count = list.count { it.emoji == e }
            val scale by animateFloatAsState(if (mine == e) 1.25f else 1f, label = "reacción")
            Column(
                Modifier.clip(CircleShape).background(if (mine == e) p.raised else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable {
                        val new = if (mine == e) null else e
                        list = list.filter { it.userId != me } + listOfNotNull(new?.let { Reaction(me.orEmpty(), it) })
                        scope.launch { runCatching { Repo.react(c.id, new) } }
                    }.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(e, fontSize = 24.sp, modifier = Modifier.scale(scale))
                if (count > 0) Text("$count", fontFamily = Typewriter, style = MaterialTheme.typography.labelSmall, color = p.muted)
            }
        }
    }
}

/* ---------------- Comentarios ---------------- */
@Composable
fun CommentsCard(c: Capsule) {
    val scope = rememberCoroutineScope()
    val me = Repo.userId()
    var list by remember { mutableStateOf<List<Comment>>(emptyList()) }
    var text by remember { mutableStateOf("") }
    LaunchedEffect(c.id) { list = runCatching { Repo.comments(c.id) }.getOrDefault(emptyList()) }
    val p = LocalPalette.current
    Section("Comentarios (${list.size})", Icons.Filled.Groups) {
        list.forEach { k ->
            Column {
                Row {
                    Text(k.userName ?: "Alguien", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Text(runCatching { OffsetDateTime.parse(k.createdAt).toInstant().fmtDate() }.getOrDefault(""), fontFamily = Typewriter, style = MaterialTheme.typography.labelSmall, color = p.muted)
                }
                Text(k.body)
                if (k.userId == me) TextButton(onClick = { scope.launch { runCatching { Repo.deleteComment(k.id) }; list = list - k } }) { Text("Borrar") }
            }
            HorizontalDivider(color = p.line)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(text, { text = it }, placeholder = { Text("Escribe un comentario…") }, keyboardOptions = keyboard, modifier = Modifier.weight(1f))
            IconButton(onClick = {
                val body = text.trim(); if (body.isEmpty()) return@IconButton
                text = ""
                scope.launch { runCatching { Repo.addComment(c.id, body) }; list = runCatching { Repo.comments(c.id) }.getOrDefault(list) }
            }) { Icon(Icons.AutoMirrored.Filled.Send, "Enviar", tint = p.wax) }
        }
    }
}

/* ---------------- Cartas de los colaboradores ---------------- */
@Composable
fun ContributorLetters(c: Capsule) {
    var list by remember { mutableStateOf<List<Letter>>(emptyList()) }
    LaunchedEffect(c.id) { list = runCatching { Repo.letters(c.id) }.getOrDefault(emptyList()) }
    if (list.isEmpty()) return
    val p = LocalPalette.current
    list.forEach { l ->
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(p.letter).padding(18.dp)) {
            Text("Carta de ${l.userName ?: "alguien"}", fontFamily = Typewriter, style = MaterialTheme.typography.labelLarge, color = androidx.compose.ui.graphics.Color(0xFF7A5C3A))
            Spacer(Modifier.height(6.dp))
            Text(l.body, color = p.letterInk)
        }
    }
}
