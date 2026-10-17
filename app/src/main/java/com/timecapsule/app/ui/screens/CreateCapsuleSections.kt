package com.timecapsule.app.ui.screens

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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
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
import com.timecapsule.app.data.ReglasCapsula
import com.timecapsule.app.data.FormularioCapsula

/* =========================================================
   NUEVA CÁPSULA · secciones de la pantalla
   Cada sección solo muestra sus datos y avisa los cambios; la pantalla
   (CreateCapsuleScreen) guarda el estado y las acomoda en orden.
   Las reglas (límites, validación) están en data/NuevaCapsula.kt.
   ========================================================= */

/** Teclado con mayúscula al iniciar frases. Acepta tildes, ñ, ü y cualquier carácter. */
internal val tecladoTexto = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, keyboardType = KeyboardType.Text)

@Composable
private fun Ayuda(texto: String) = Text(texto, color = Muted, style = MaterialTheme.typography.bodySmall)

/* ---------------- Borrador recuperado ---------------- */
@Composable
internal fun BorradorRecuperado(onEmpezarDeNuevo: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(NightRaised).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("✓ Recuperamos tu borrador", Modifier.weight(1f), color = Gold)
        TextButton(onClick = onEmpezarDeNuevo) { Text("Empezar de nuevo") }
    }
}

/* ---------------- Título (con ideas de la IA) ---------------- */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SeccionTitulo(
    titulo: String, onTitulo: (String) -> Unit,
    ideas: List<String>, pensando: Boolean, puedeSugerir: Boolean, iaOcupada: Boolean, onSugerir: () -> Unit,
) {
    OutlinedTextField(titulo, onTitulo, label = { Text("Título") }, singleLine = true,
        keyboardOptions = tecladoTexto,
        placeholder = { Text("Cena de aniversario, Grado 2026…") }, modifier = Modifier.fillMaxWidth())
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = onSugerir, enabled = !iaOcupada && puedeSugerir) {
            if (pensando) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp) else Icon(Icons.Filled.AutoAwesome, null)
            Spacer(Modifier.width(6.dp)); Text("Sugerir título y portada")
        }
    }
    if (ideas.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ideas.forEach { t -> FilterChip(selected = titulo == t, onClick = { onTitulo(t) }, label = { Text(t) }) }
    }
}

/* ---------------- Carta (con ayuda de la IA y plantillas) ---------------- */
@Composable
internal fun SeccionCarta(
    carta: String, onCarta: (String) -> Unit,
    pensando: Boolean, iaOcupada: Boolean, onAyuda: () -> Unit, puedeDeshacer: Boolean, onDeshacer: () -> Unit,
    plantilla: String?, onPlantilla: (id: String, texto: String) -> Unit,
) {
    OutlinedTextField(carta, onCarta, label = { Text("Carta para el futuro") },
        keyboardOptions = tecladoTexto,
        placeholder = { Text("Querido yo del futuro…") }, minLines = 5, modifier = Modifier.fillMaxWidth())
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = onAyuda, enabled = !iaOcupada) {
            if (pensando) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp) else Icon(Icons.Filled.AutoAwesome, null)
            Spacer(Modifier.width(6.dp)); Text(if (carta.isBlank()) "Escribe un borrador" else "Mejorar mi carta")
        }
        if (puedeDeshacer) TextButton(onClick = onDeshacer) { Text("Deshacer") }
    }
    Ayuda("¿Te ayudo a empezar? Elige una plantilla:")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(LetterTemplates.all) { _, (id, par) ->
            FilterChip(selected = plantilla == id, onClick = { onPlantilla(id, par.second) }, label = { Text(par.first) })
        }
    }
}

/* ---------------- Notas de voz ---------------- */
@Composable
internal fun SeccionNotasDeVoz(notas: List<File>, onBorrar: (Int) -> Unit, grabando: Boolean, ms: Long, onGrabar: () -> Unit) {
    Text("Notas de voz", style = MaterialTheme.typography.titleMedium)
    notas.forEachIndexed { i, f -> VoiceNotePlayer(f.absolutePath, "Nota ${i + 1}", onDelete = { onBorrar(i) }) }
    if (notas.size < ReglasCapsula.MAX_NOTAS_DE_VOZ) OutlinedButton(onClick = onGrabar, modifier = Modifier.fillMaxWidth()) {
        Icon(if (grabando) Icons.Filled.Stop else Icons.Filled.Mic, null,
            tint = if (grabando) MaterialTheme.colorScheme.error else androidx.compose.material3.LocalContentColor.current)
        Spacer(Modifier.width(8.dp))
        Text(if (grabando) "Detener · ${fmtDuration(ms)} / 3:00" else "Grabar una nota de voz")
    }
}
