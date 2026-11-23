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
import com.timecapsule.app.ui.Text
import com.timecapsule.app.ui.CustomText
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
import com.timecapsule.app.data.CapsuleRules
import com.timecapsule.app.data.CapsuleForm

/* =========================================================
   NUEVA CÁPSULA · secciones de la pantalla
   Cada sección solo muestra sus datos y avisa los cambios; la pantalla
   (CreateCapsuleScreen) guarda el estado y las acomoda en orden.
   Las reglas (límites, validación) están en data/NewCapsuleForm.kt.
   ========================================================= */

/** Teclado con mayúscula al iniciar frases. Acepta tildes, ñ, ü y cualquier carácter. */
internal val textKeyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, keyboardType = KeyboardType.Text)

@Composable
private fun Help(text: String) = Text(text, color = Muted, style = MaterialTheme.typography.bodySmall)

/* ---------------- Borrador recuperado ---------------- */
@Composable
internal fun RecoveredDraft(onStartOver: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(NightRaised).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("✓ Recuperamos tu borrador", Modifier.weight(1f), color = Gold)
        TextButton(onClick = onStartOver) { Text("Empezar de nuevo") }
    }
}

/* ---------------- Título (con ideas de la IA) ---------------- */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TitleSection(
    title: String, onTitle: (String) -> Unit,
    ideas: List<String>, thinking: Boolean, canSuggest: Boolean, aiBusy: Boolean, onSuggest: () -> Unit,
) {
    OutlinedTextField(title, onTitle, label = { Text("Título") }, singleLine = true,
        keyboardOptions = textKeyboard,
        placeholder = { Text("Cena de aniversario, Grado 2026…") }, modifier = Modifier.fillMaxWidth())
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = onSuggest, enabled = !aiBusy && canSuggest) {
            if (thinking) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp) else Icon(Icons.Filled.AutoAwesome, null)
            Spacer(Modifier.width(6.dp)); Text("Sugerir título y portada")
        }
    }
    if (ideas.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ideas.forEach { t -> FilterChip(selected = title == t, onClick = { onTitle(t) }, label = { Text(t) }) }
    }
}

/* ---------------- Carta (con ayuda de la IA y plantillas) ---------------- */
@Composable
internal fun LetterSection(
    letter: String, onLetter: (String) -> Unit,
    thinking: Boolean, aiBusy: Boolean, onHelp: () -> Unit, canUndo: Boolean, onUndo: () -> Unit,
    template: String?, onTemplate: (id: String, text: String) -> Unit,
) {
    OutlinedTextField(letter, onLetter, label = { Text("Carta para el futuro") },
        keyboardOptions = textKeyboard,
        placeholder = { Text("Querido yo del futuro…") }, minLines = 5, modifier = Modifier.fillMaxWidth())
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = onHelp, enabled = !aiBusy) {
            if (thinking) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp) else Icon(Icons.Filled.AutoAwesome, null)
            Spacer(Modifier.width(6.dp)); Text(if (letter.isBlank()) "Escribe un borrador" else "Mejorar mi carta")
        }
        if (canUndo) TextButton(onClick = onUndo) { Text("Deshacer") }
    }
    Help("¿Te ayudo a empezar? Elige una plantilla:")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(LetterTemplates.all) { _, (id, par) ->
            FilterChip(selected = template == id, onClick = { onTemplate(id, par.second) }, label = { Text(par.first) })
        }
    }
}

/* ---------------- Notas de voz ---------------- */
@Composable
internal fun VoiceNotesSection(notes: List<File>, onDelete: (Int) -> Unit, recordingNow: Boolean, ms: Long, onRecord: () -> Unit) {
    Text("Notas de voz", style = MaterialTheme.typography.titleMedium)
    notes.forEachIndexed { i, f -> VoiceNotePlayer(f.absolutePath, "Nota ${i + 1}", onDelete = { onDelete(i) }) }
    if (notes.size < CapsuleRules.MAX_VOICE_NOTES) OutlinedButton(onClick = onRecord, modifier = Modifier.fillMaxWidth()) {
        Icon(if (recordingNow) Icons.Filled.Stop else Icons.Filled.Mic, null,
            tint = if (recordingNow) MaterialTheme.colorScheme.error else androidx.compose.material3.LocalContentColor.current)
        Spacer(Modifier.width(8.dp))
        Text(if (recordingNow) "Detener · ${fmtDuration(ms)} / 3:00" else "Grabar una nota de voz")
    }
}

/* ---------------- Canción del momento ---------------- */
@Composable
internal fun SongSection(
    title: String, onTitle: (String) -> Unit, link: String, onLink: (String) -> Unit, onSearch: (base: String) -> Unit,
) {
    Text("Canción del momento", style = MaterialTheme.typography.titleMedium)
    OutlinedTextField(title, onTitle, label = { Text("Canción y artista") }, singleLine = true,
        leadingIcon = { Icon(Icons.Filled.MusicNote, null) }, keyboardOptions = textKeyboard,
        placeholder = { Text("Vivir mi vida · Marc Anthony") }, modifier = Modifier.fillMaxWidth())
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { onSearch("https://www.youtube.com/results?search_query=") }, enabled = title.isNotBlank()) { Text("Buscar en YouTube") }
        OutlinedButton(onClick = { onSearch("https://open.spotify.com/search/") }, enabled = title.isNotBlank()) { Text("En Spotify") }
    }
    OutlinedTextField(link, onLink, label = { Text("Enlace de la canción (opcional)") }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        placeholder = { Text("Pega aquí el enlace de YouTube o Spotify") }, modifier = Modifier.fillMaxWidth())
    Help("Tranquila: todo lo que llevas se guarda solo. En YouTube o Spotify toca Compartir → Copiar enlace y al volver se pega aquí.")
}

/* ---------------- Preguntas para el futuro ---------------- */
@Composable
internal fun QuestionsSection(questions: List<String>, onRemove: (Int) -> Unit, onAdd: (String) -> Unit) {
    var newOne by remember { mutableStateOf("") }
    Text("Preguntas para tu yo del futuro", style = MaterialTheme.typography.titleMedium)
    questions.forEachIndexed { i, q ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.QuestionAnswer, null, tint = Gold, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp)); Text(q, Modifier.weight(1f))
            IconButton(onClick = { onRemove(i) }) { Icon(Icons.Filled.Close, com.timecapsule.app.ui.tr("Quitar pregunta")) }
        }
    }
    if (questions.size < CapsuleRules.MAX_QUESTIONS) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(LetterTemplates.questions.filter { it !in questions }) { _, q ->
                FilterChip(selected = false, onClick = { onAdd(q) }, label = { Text(q) })
            }
        }
        OutlinedTextField(newOne, { newOne = it }, label = { Text("Escribe tu propia pregunta") }, singleLine = true,
            keyboardOptions = textKeyboard, modifier = Modifier.fillMaxWidth(),
            trailingIcon = { IconButton(onClick = { if (newOne.isNotBlank()) { onAdd(newOne); newOne = "" } }) { Icon(Icons.Filled.Add, com.timecapsule.app.ui.tr("Agregar")) } })
    }
    Help("Las responderás cuando la cápsula se abra.")
}

/* ---------------- Fotos y videos ---------------- */
@Composable
internal fun MediaSection(media: List<PickedItem>, onGallery: () -> Unit, onCamera: () -> Unit, onRemove: (Int) -> Unit) {
    Text("Fotos y videos", style = MaterialTheme.typography.titleMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = onGallery) { Icon(Icons.Filled.PhotoLibrary, null); Spacer(Modifier.width(6.dp)); Text("Galería") }
        OutlinedButton(onClick = onCamera) { Icon(Icons.Filled.PhotoCamera, null); Spacer(Modifier.width(6.dp)); Text("Cámara") }
    }
    if (media.isNotEmpty()) LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(media, key = { _, m -> m.uri.toString() }) { i, m ->
            Box(Modifier.size(92.dp).clip(RoundedCornerShape(6.dp)).background(NightRaised)) {
                if (m.kind == "photo") AsyncImage(m.uri, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else Icon(Icons.Filled.Videocam, com.timecapsule.app.ui.tr("Video"), tint = Gold, modifier = Modifier.align(Alignment.Center).size(36.dp))
                IconButton(onClick = { onRemove(i) },
                    modifier = Modifier.align(Alignment.TopEnd).size(28.dp).padding(3.dp).clip(CircleShape).background(Night)) {
                    Icon(Icons.Filled.Close, com.timecapsule.app.ui.tr("Quitar"), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
    Help("${media.size} seleccionados · videos de máximo 50 MB")
}

/* ---------------- Cuándo se abre ---------------- */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DateSection(useDate: Boolean, onUseDate: (Boolean) -> Unit, opensOn: Instant?, onOpensOn: (Instant) -> Unit, onPickDate: () -> Unit) {
    SwitchRow("Abrir en una fecha", useDate, onUseDate)
    if (!useDate) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CapsuleRules.dateOptions.forEach { (text, months) ->
            val date = CapsuleRules.dateIn(months)
            FilterChip(selected = CapsuleRules.sameDate(opensOn, date), onClick = { onOpensOn(date) }, label = { Text(text) })
        }
    }
    OutlinedButton(onClick = onPickDate, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Filled.Schedule, null); Spacer(Modifier.width(8.dp))
        Text(opensOn?.let { "Se abre el ${it.fmtDateTime()}" } ?: "Elegir fecha y hora")
    }
    Help("Ese día te llega una notificación y la cápsula se abre sola.")
}

/* ---------------- Clave para abrirla antes ---------------- */
@Composable
internal fun KeySection(
    useKey: Boolean, onUseKey: (Boolean) -> Unit,
    key: String, onKey: (String) -> Unit, repeated: String, onRepeated: (String) -> Unit,
) {
    SwitchRow("Poder abrirla antes con una clave", useKey, onUseKey)
    if (!useKey) return
    var showKey by remember { mutableStateOf(false) }
    val vt = if (showKey) VisualTransformation.None else PasswordVisualTransformation()
    val eye = @Composable {
        IconButton(onClick = { showKey = !showKey }) {
            Icon(if (showKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, com.timecapsule.app.ui.tr(if (showKey) "Ocultar" else "Mostrar"))
        }
    }
    OutlinedTextField(key, onKey, label = { Text("Clave") }, singleLine = true,
        leadingIcon = { Icon(Icons.Filled.Key, null) }, trailingIcon = eye, visualTransformation = vt,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false), modifier = Modifier.fillMaxWidth())
    OutlinedTextField(repeated, onRepeated, label = { Text("Repite la clave") }, singleLine = true,
        visualTransformation = vt, isError = repeated.isNotEmpty() && key != repeated,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false), modifier = Modifier.fillMaxWidth())
    Help("Con esta clave la abres cuando quieras. Se guarda cifrada: si la olvidas, nadie puede recuperarla, " +
        "pero la cápsula igual se abrirá en su fecha.")
}

/* ---------------- Dónde (lugar, radio y ruta) ---------------- */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PlaceSection(
    usePlace: Boolean, onUsePlace: (Boolean) -> Unit,
    searching: Boolean, lat: Double?, lng: Double?, precision: Int?, address: String?,
    nearbyPlaces: List<NearbyPlace>, name: String, onName: (String) -> Unit, onRefresh: () -> Unit,
    onlyHere: Boolean, onOnlyHere: (Boolean) -> Unit, radius: Int, onRadius: (Int) -> Unit,
    routes: List<Route>, routeIdValue: String?, onRoute: (String?) -> Unit, onCreateRoute: (String) -> Unit,
) {
    SwitchRow("Guardar el lugar", usePlace, onUsePlace)
    if (!usePlace) return
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(NightRaised).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (searching) Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp)); Text("Buscando dónde estás…", color = Muted)
        } else if (lat != null) {
            Meta(Icons.Filled.Place, address ?: "Ubicación registrada", Cream)
            Help("%.5f, %.5f · precisión %d m".format(lat, lng, precision ?: 0))
        } else Text("Aún no tenemos tu ubicación.", color = Muted)
    }
    if (nearbyPlaces.isNotEmpty()) {
        Text("¿Dónde estás? Toca el lugar:", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            nearbyPlaces.forEach { p ->
                FilterChip(selected = name == p.name, onClick = { onName(p.name) }, label = { Text("${p.name} · ${p.kind} · ${p.distance} m") })
            }
        }
    }
    OutlinedTextField(name, onName, label = { Text("Nombre del lugar") },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        placeholder = { Text("Restaurante, casa de la abuela…") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedButton(onClick = onRefresh, enabled = !searching, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Filled.MyLocation, null); Spacer(Modifier.width(8.dp)); Text("Actualizar mi ubicación")
    }
    Help("Cuando vuelvas a este lugar te llegará una notificación con estos recuerdos.")
    SwitchRow("Solo se abre estando en este lugar", onlyHere, onOnlyHere)
    Text("¿Qué tan cerca hay que estar?", style = MaterialTheme.typography.titleSmall)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(50 to "50 m", 100 to "100 m", 150 to "150 m", 500 to "500 m", 1000 to "1 km").forEach { (m, l) ->
            FilterChip(selected = radius == m, onClick = { onRadius(m) }, label = { Text(l) })
        }
    }
    // Ruta de cápsulas
    var newRoute by remember { mutableStateOf("") }
    Meta(Icons.Filled.Route, "Ruta de cápsulas (se abren en orden)", Cream)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = routeIdValue == null, onClick = { onRoute(null) }, label = { Text("Ninguna") })
        routes.forEach { r -> FilterChip(selected = routeIdValue == r.id, onClick = { onRoute(r.id) }, label = { CustomText(r.title) }) }
    }
    OutlinedTextField(newRoute, { newRoute = it }, label = { Text("Crear una ruta nueva") }, singleLine = true,
        keyboardOptions = textKeyboard, modifier = Modifier.fillMaxWidth(),
        trailingIcon = { IconButton(onClick = { if (newRoute.isNotBlank()) { onCreateRoute(newRoute); newRoute = "" } }) { Icon(Icons.Filled.Add, com.timecapsule.app.ui.tr("Crear ruta")) } })
    if (routeIdValue != null) Help("Esta cápsula solo se abrirá cuando la anterior de la ruta esté abierta.")
}

/* ---------------- Compartir ---------------- */
@Composable
internal fun ShareSection(
    emails: String, onEmails: (String) -> Unit,
    collaborative: Boolean, onCollaborative: (Boolean) -> Unit, openTogether: Boolean, onOpenTogether: (Boolean) -> Unit,
) {
    OutlinedTextField(emails, onEmails, label = { Text("Compartir con (correos)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        placeholder = { Text("mama@correo.com, amigo@correo.com") }, modifier = Modifier.fillMaxWidth())
    Help("Deben tener cuenta en Time Capsule. Verán la cápsula cuando se abra. También puedes invitar por enlace o QR después de crearla.")
    if (CapsuleRules.emails(emails).isNotEmpty()) {
        SwitchRow("Pueden agregar sus fotos y cartas (colaborativa)", collaborative, onCollaborative)
        SwitchRow("Abrir juntos: se abre cuando todos lo acepten", openTogether, onOpenTogether)
    }
}

/* ---------------- Fila con interruptor ---------------- */
@Composable
fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        Switch(checked, onChange)
    }
}
