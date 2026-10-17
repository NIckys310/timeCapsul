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

/* ---------------- Canción del momento ---------------- */
@Composable
internal fun SeccionCancion(
    titulo: String, onTitulo: (String) -> Unit, enlace: String, onEnlace: (String) -> Unit, onBuscar: (base: String) -> Unit,
) {
    Text("Canción del momento", style = MaterialTheme.typography.titleMedium)
    OutlinedTextField(titulo, onTitulo, label = { Text("Canción y artista") }, singleLine = true,
        leadingIcon = { Icon(Icons.Filled.MusicNote, null) }, keyboardOptions = tecladoTexto,
        placeholder = { Text("Vivir mi vida · Marc Anthony") }, modifier = Modifier.fillMaxWidth())
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { onBuscar("https://www.youtube.com/results?search_query=") }, enabled = titulo.isNotBlank()) { Text("Buscar en YouTube") }
        OutlinedButton(onClick = { onBuscar("https://open.spotify.com/search/") }, enabled = titulo.isNotBlank()) { Text("En Spotify") }
    }
    OutlinedTextField(enlace, onEnlace, label = { Text("Enlace de la canción (opcional)") }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        placeholder = { Text("Pega aquí el enlace de YouTube o Spotify") }, modifier = Modifier.fillMaxWidth())
    Ayuda("Tranquila: todo lo que llevas se guarda solo. En YouTube o Spotify toca Compartir → Copiar enlace y al volver se pega aquí.")
}

/* ---------------- Preguntas para el futuro ---------------- */
@Composable
internal fun SeccionPreguntas(preguntas: List<String>, onQuitar: (Int) -> Unit, onAgregar: (String) -> Unit) {
    var nueva by remember { mutableStateOf("") }
    Text("Preguntas para tu yo del futuro", style = MaterialTheme.typography.titleMedium)
    preguntas.forEachIndexed { i, q ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.QuestionAnswer, null, tint = Gold, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp)); Text(q, Modifier.weight(1f))
            IconButton(onClick = { onQuitar(i) }) { Icon(Icons.Filled.Close, "Quitar pregunta") }
        }
    }
    if (preguntas.size < ReglasCapsula.MAX_PREGUNTAS) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(LetterTemplates.questions.filter { it !in preguntas }) { _, q ->
                FilterChip(selected = false, onClick = { onAgregar(q) }, label = { Text(q) })
            }
        }
        OutlinedTextField(nueva, { nueva = it }, label = { Text("Escribe tu propia pregunta") }, singleLine = true,
            keyboardOptions = tecladoTexto, modifier = Modifier.fillMaxWidth(),
            trailingIcon = { IconButton(onClick = { if (nueva.isNotBlank()) { onAgregar(nueva); nueva = "" } }) { Icon(Icons.Filled.Add, "Agregar") } })
    }
    Ayuda("Las responderás cuando la cápsula se abra.")
}

/* ---------------- Fotos y videos ---------------- */
@Composable
internal fun SeccionMedios(medios: List<PickedItem>, onGaleria: () -> Unit, onCamara: () -> Unit, onQuitar: (Int) -> Unit) {
    Text("Fotos y videos", style = MaterialTheme.typography.titleMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = onGaleria) { Icon(Icons.Filled.PhotoLibrary, null); Spacer(Modifier.width(6.dp)); Text("Galería") }
        OutlinedButton(onClick = onCamara) { Icon(Icons.Filled.PhotoCamera, null); Spacer(Modifier.width(6.dp)); Text("Cámara") }
    }
    if (medios.isNotEmpty()) LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(medios, key = { _, m -> m.uri.toString() }) { i, m ->
            Box(Modifier.size(92.dp).clip(RoundedCornerShape(6.dp)).background(NightRaised)) {
                if (m.kind == "photo") AsyncImage(m.uri, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else Icon(Icons.Filled.Videocam, "Video", tint = Gold, modifier = Modifier.align(Alignment.Center).size(36.dp))
                IconButton(onClick = { onQuitar(i) },
                    modifier = Modifier.align(Alignment.TopEnd).size(28.dp).padding(3.dp).clip(CircleShape).background(Night)) {
                    Icon(Icons.Filled.Close, "Quitar", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
    Ayuda("${medios.size} seleccionados · videos de máximo 50 MB")
}

/* ---------------- Cuándo se abre ---------------- */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SeccionFecha(usarFecha: Boolean, onUsarFecha: (Boolean) -> Unit, abreEl: Instant?, onAbreEl: (Instant) -> Unit, onElegirFecha: () -> Unit) {
    SwitchRow("Abrir en una fecha", usarFecha, onUsarFecha)
    if (!usarFecha) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ReglasCapsula.opcionesDeFecha.forEach { (texto, meses) ->
            val fecha = ReglasCapsula.fechaEn(meses)
            FilterChip(selected = ReglasCapsula.mismaFecha(abreEl, fecha), onClick = { onAbreEl(fecha) }, label = { Text(texto) })
        }
    }
    OutlinedButton(onClick = onElegirFecha, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Filled.Schedule, null); Spacer(Modifier.width(8.dp))
        Text(abreEl?.let { "Se abre el ${it.fmtDateTime()}" } ?: "Elegir fecha y hora")
    }
    Ayuda("Ese día te llega una notificación y la cápsula se abre sola.")
}
