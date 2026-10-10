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

/** Teclado con mayúscula al iniciar frases. Acepta tildes, ñ, ü y cualquier carácter. */
private val textKeyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, keyboardType = KeyboardType.Text)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateCapsuleScreen(onBack: () -> Unit, onCreated: (String) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var letter by remember { mutableStateOf("") }
    val items = remember { mutableStateListOf<PickedItem>() }
    var unlockAt by remember { mutableStateOf<Instant?>(ZonedDateTime.now().plusYears(1).toInstant()) }
    var useDate by remember { mutableStateOf(true) }

    // Clave para abrirla cuando quieras
    var useKey by remember { mutableStateOf(false) }
    var key by remember { mutableStateOf("") }
    var key2 by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }

    // Lugar (se activa solo si das permiso de ubicación)
    var usePlace by remember { mutableStateOf(true) }
    var requirePlace by remember { mutableStateOf(false) }
    var placeName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf<String?>(null) }
    var nearby by remember { mutableStateOf<List<NearbyPlace>>(emptyList()) }
    var lat by remember { mutableStateOf<Double?>(null) }
    var lng by remember { mutableStateOf<Double?>(null) }
    var accuracy by remember { mutableStateOf<Int?>(null) }
    var locating by remember { mutableStateOf(false) }

    var emails by remember { mutableStateOf("") }
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var pickedDate by remember { mutableStateOf<LocalDate?>(null) }
    var saving by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var progressText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(20)) { uris ->
        uris.forEach { u -> if (items.none { it.uri == u }) items += PickedItem(u, MediaUtil.kindOf(ctx, u)) }
    }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) cameraUri?.let { items += PickedItem(it, "photo") }
    }

    /** Toma el GPS real y busca la dirección y los sitios con nombre alrededor. */
    fun locate() {
        locating = true
        scope.launch {
            val loc = LocationUtil.current(ctx)
            if (loc == null) {
                error = "No se pudo obtener tu ubicación. Revisa que el GPS esté encendido."
            } else {
                lat = loc.latitude; lng = loc.longitude; accuracy = loc.accuracy.toInt()
                address = Places.address(ctx, loc.latitude, loc.longitude)
                nearby = Places.nearby(loc.latitude, loc.longitude)
                // Si estás prácticamente dentro de un lugar con nombre, se elige solo
                val closest = nearby.firstOrNull { it.distance <= 35 }
                if (placeName.isBlank()) placeName = closest?.name ?: address?.substringBefore(",").orEmpty()
            }
            locating = false
        }
    }
    val locPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        if (res[Manifest.permission.ACCESS_FINE_LOCATION] == true || res[Manifest.permission.ACCESS_COARSE_LOCATION] == true) locate()
        else usePlace = false
    }
    fun askLocation() = locPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))

    // Al abrir la pantalla: si ya diste permiso, registra el lugar automáticamente
    LaunchedEffect(Unit) { if (LocationUtil.hasFine(ctx)) locate() else askLocation() }

    fun save() {
        error = null
        when {
            title.isBlank() -> { error = "Ponle un título a tu cápsula."; return }
            letter.isBlank() && items.isEmpty() -> { error = "Escribe una carta o agrega fotos o videos."; return }
            !useDate && !(usePlace && requirePlace) -> { error = "Elige una fecha o activa “Solo se abre en este lugar”."; return }
            usePlace && lat == null -> { error = "Espera a que se registre tu ubicación o apaga “Guardar el lugar”."; return }
            useDate && unlockAt?.isBefore(Instant.now()) == true -> { error = "La fecha de apertura debe ser en el futuro."; return }
            useKey && key.trim().length < 4 -> { error = "La clave debe tener al menos 4 caracteres."; return }
            useKey && key != key2 -> { error = "Las dos claves no coinciden."; return }
        }
        saving = true
        scope.launch {
            runCatching {
                Repo.create(ctx, Draft(
                    title = title, letter = letter, items = items.toList(),
                    unlockAt = if (useDate) unlockAt else null,
                    placeName = if (usePlace) placeName.ifBlank { address } else null,
                    lat = if (usePlace) lat else null, lng = if (usePlace) lng else null,
                    requirePlace = usePlace && requirePlace,
                    shareEmails = emails.split(',', ';', ' ', '\n').map { it.trim() }.filter { it.contains('@') },
                    key = if (useKey) key else null,
                )) { text, p -> progressText = text; progress = p }
            }.onSuccess { onCreated(it) }
             .onFailure { error = Repo.friendly(it); saving = false }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.paper(),
        topBar = {
            TopAppBar(
                title = { Text("Nueva cápsula") },
                navigationIcon = { IconButton(onClick = onBack, enabled = !saving) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OutlinedTextField(title, { title = it }, label = { Text("Título") }, singleLine = true,
                keyboardOptions = textKeyboard,
                placeholder = { Text("Cena de aniversario, Grado 2026…") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(letter, { letter = it }, label = { Text("Carta para el futuro") },
                keyboardOptions = textKeyboard,
                placeholder = { Text("Querido yo del futuro…") }, minLines = 5, modifier = Modifier.fillMaxWidth())

            // ---------- Fotos y videos ----------
            Text("Fotos y videos", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = {
                    pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                }) { Icon(Icons.Filled.PhotoLibrary, null); Spacer(Modifier.width(6.dp)); Text("Galería") }
                OutlinedButton(onClick = {
                    val dir = File(ctx.cacheDir, "camera").apply { mkdirs() }
                    val file = File(dir, "foto_${System.currentTimeMillis()}.jpg")
                    val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", file)
                    cameraUri = uri
                    takePhoto.launch(uri)
                }) { Icon(Icons.Filled.PhotoCamera, null); Spacer(Modifier.width(6.dp)); Text("Cámara") }
            }
            if (items.isNotEmpty()) LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(items, key = { _, item -> item.uri.toString() }) { i, item ->
                    Box(Modifier.size(92.dp).clip(RoundedCornerShape(6.dp)).background(NightRaised)) {
                        if (item.kind == "photo") AsyncImage(item.uri, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        else Icon(Icons.Filled.Videocam, "Video", tint = Gold, modifier = Modifier.align(Alignment.Center).size(36.dp))
                        IconButton(onClick = { items.removeAt(i) },
                            modifier = Modifier.align(Alignment.TopEnd).size(28.dp).padding(3.dp).clip(CircleShape).background(Night)) {
                            Icon(Icons.Filled.Close, "Quitar", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            Text("${items.size} seleccionados · videos de máximo 50 MB", color = Muted, style = MaterialTheme.typography.bodySmall)

            HorizontalDivider()

            // ---------- Cuándo se abre ----------
            SwitchRow("Abrir en una fecha", useDate) { useDate = it }
            if (useDate) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("1 mes" to 1L, "6 meses" to 6L, "1 año" to 12L, "5 años" to 60L, "10 años" to 120L).forEach { (label, months) ->
                        val target = ZonedDateTime.now().plusMonths(months).toInstant()
                        FilterChip(
                            selected = unlockAt?.let { kotlin.math.abs(it.epochSecond - target.epochSecond) < 120 } == true,
                            onClick = { unlockAt = target }, label = { Text(label) })
                    }
                }
                OutlinedButton(onClick = { showDate = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Schedule, null); Spacer(Modifier.width(8.dp))
                    Text(unlockAt?.let { "Se abre el ${it.fmtDateTime()}" } ?: "Elegir fecha y hora")
                }
                Text("Ese día te llega una notificación y la cápsula se abre sola.", color = Muted, style = MaterialTheme.typography.bodySmall)
            }

            // ---------- Clave ----------
            SwitchRow("Poder abrirla antes con una clave", useKey) { useKey = it }
            if (useKey) {
                val vt = if (showKey) VisualTransformation.None else PasswordVisualTransformation()
                val eye = @Composable {
                    IconButton(onClick = { showKey = !showKey }) {
                        Icon(if (showKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, if (showKey) "Ocultar" else "Mostrar")
                    }
                }
                OutlinedTextField(key, { key = it }, label = { Text("Clave") }, singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Key, null) }, trailingIcon = eye, visualTransformation = vt,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(key2, { key2 = it }, label = { Text("Repite la clave") }, singleLine = true,
                    visualTransformation = vt, isError = key2.isNotEmpty() && key != key2,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
                Text("Con esta clave la abres cuando quieras. Se guarda cifrada: si la olvidas, nadie puede recuperarla, " +
                    "pero la cápsula igual se abrirá en su fecha.", color = Muted, style = MaterialTheme.typography.bodySmall)
            }

            HorizontalDivider()

            // ---------- Dónde ----------
            SwitchRow("Guardar el lugar", usePlace) { on ->
                usePlace = on
                if (on && lat == null) { if (LocationUtil.hasFine(ctx)) locate() else askLocation() }
            }
            if (usePlace) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(NightRaised).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (locating) Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp)); Text("Buscando dónde estás…", color = Muted)
                    } else if (lat != null) {
                        Meta(Icons.Filled.Place, address ?: "Ubicación registrada", Cream)
                        Text("%.5f, %.5f · precisión %d m".format(lat, lng, accuracy ?: 0), color = Muted, style = MaterialTheme.typography.bodySmall)
                    } else Text("Aún no tenemos tu ubicación.", color = Muted)
                }
                if (nearby.isNotEmpty()) {
                    Text("¿Dónde estás? Toca el lugar:", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        nearby.forEach { p ->
                            FilterChip(selected = placeName == p.name, onClick = { placeName = p.name },
                                label = { Text("${p.name} · ${p.kind} · ${p.distance} m") })
                        }
                    }
                }
                OutlinedTextField(placeName, { placeName = it }, label = { Text("Nombre del lugar") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    placeholder = { Text("Restaurante, casa de la abuela…") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedButton(onClick = { if (LocationUtil.hasFine(ctx)) locate() else askLocation() },
                    enabled = !locating, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.MyLocation, null); Spacer(Modifier.width(8.dp)); Text("Actualizar mi ubicación")
                }
                Text("Cuando vuelvas a este lugar te llegará una notificación con estos recuerdos.", color = Muted, style = MaterialTheme.typography.bodySmall)
                SwitchRow("Solo se abre estando en este lugar", requirePlace) { requirePlace = it }
            }

            HorizontalDivider()

            // ---------- Compartir ----------
            OutlinedTextField(emails, { emails = it }, label = { Text("Compartir con (correos)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                placeholder = { Text("mama@correo.com, amigo@correo.com") }, modifier = Modifier.fillMaxWidth())
            Text("Deben tener cuenta en Time Capsule. Verán la cápsula cuando se abra.", color = Muted, style = MaterialTheme.typography.bodySmall)

            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (saving) {
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                Text(progressText, color = Muted)
            }
            Button(onClick = ::save, enabled = !saving, modifier = Modifier.fillMaxWidth().height(54.dp)) {
                Text(if (saving) "Guardando…" else "Sellar cápsula")
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    // ---------- Selector de fecha y hora ----------
    if (showDate) {
        val todayUtc = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = unlockAt?.let { it.atZone(ZoneId.systemDefault()).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() },
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= todayUtc
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        pickedDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                        showDate = false; showTime = true
                    }
                }) { Text("Siguiente") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Cancelar") } },
        ) { DatePicker(state) }
    }
    if (showTime) {
        val t = rememberTimePickerState(initialHour = 9, initialMinute = 0, is24Hour = false)
        AlertDialog(
            onDismissRequest = { showTime = false },
            title = { Text("¿A qué hora?") },
            text = { TimePicker(t) },
            confirmButton = {
                TextButton(onClick = {
                    pickedDate?.let { d -> unlockAt = d.atTime(LocalTime.of(t.hour, t.minute)).atZone(ZoneId.systemDefault()).toInstant() }
                    showTime = false
                }) { Text("Listo") }
            },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        Switch(checked, onChange)
    }
}
