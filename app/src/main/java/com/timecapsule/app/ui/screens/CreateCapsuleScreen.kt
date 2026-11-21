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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.timecapsule.app.data.ReglasCapsula
import com.timecapsule.app.data.FormularioCapsula
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

/**
 * NUEVA CÁPSULA · solo dibuja.
 * El estado y la lógica viven en NuevaCapsulaViewModel (sobreviven a girar el celular);
 * las secciones están en CreateCapsuleSections.kt y las reglas en data/NuevaCapsula.kt.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateCapsuleScreen(
    onBack: () -> Unit,
    onCreated: (String) -> Unit,
    vm: NuevaCapsulaViewModel = viewModel(factory = NuevaCapsulaViewModel.Fabrica),
) {
    val ctx = LocalContext.current
    LaunchedEffect(vm.creada) { vm.creada?.let(onCreated) }

    // Al volver de YouTube/Spotify: si copiaste el enlace de la canción, se pega solo
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) {
                val cb = ctx.getSystemService(ClipboardManager::class.java)
                vm.pegarCancionSiHay(runCatching { cb?.primaryClip?.getItemAt(0)?.text?.toString() }.getOrNull().orEmpty())
            }
        }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> vm.alternarGrabacion(ok) }
    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(20)) { vm.agregarMedios(it) }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { vm.fotoTomada(it) }
    val locPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        if (res[Manifest.permission.ACCESS_FINE_LOCATION] == true || res[Manifest.permission.ACCESS_COARSE_LOCATION] == true) vm.ubicar()
        else vm.cambiarUsarLugar(false)
    }
    fun ubicarConPermiso() {
        if (LocationUtil.hasFine(ctx)) vm.ubicar()
        else locPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }
    // Al abrir la pantalla por primera vez: registra el lugar (o pide permiso)
    LaunchedEffect(Unit) { if (vm.lat == null && vm.usarLugar) ubicarConPermiso() }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.paper(),
        topBar = {
            TopAppBar(
                title = { Text("Nueva cápsula") },
                navigationIcon = { IconButton(onClick = onBack, enabled = !vm.guardando) { Icon(Icons.AutoMirrored.Filled.ArrowBack, com.timecapsule.app.ui.tr("Volver")) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (vm.borradorRecuperado) BorradorRecuperado { vm.empezarDeNuevo() }
            SeccionTitulo(vm.titulo, vm::cambiarTitulo, vm.ideasDeTitulo, pensando = vm.iaOcupada == "titulo",
                puedeSugerir = vm.medios.any { it.kind == "photo" } || vm.carta.isNotBlank(), iaOcupada = vm.iaOcupada != null, onSugerir = vm::sugerirTitulo)
            SeccionCarta(vm.carta, vm::cambiarCarta, pensando = vm.iaOcupada == "carta", iaOcupada = vm.iaOcupada != null, onAyuda = { vm.mejorarCarta("cálido") },
                puedeDeshacer = vm.cartaAnterior != null, onDeshacer = vm::deshacerCarta,
                plantilla = vm.plantilla, onPlantilla = vm::usarPlantilla)
            SeccionNotasDeVoz(vm.notasDeVoz, onBorrar = vm::borrarNota, grabando = vm.grabando, ms = vm.msGrabados, onGrabar = {
                if (vm.grabando) vm.alternarGrabacion(true) else micPermission.launch(Manifest.permission.RECORD_AUDIO)
            })
            SeccionCancion(vm.cancion, vm::cambiarCancion, vm.enlaceCancion, vm::cambiarEnlaceCancion, onBuscar = { base -> openSearch(ctx, base, vm.cancion) })
            SeccionPreguntas(vm.preguntas, onQuitar = vm::quitarPregunta, onAgregar = vm::agregarPregunta)
            SeccionMedios(vm.medios,
                onGaleria = { pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) },
                onCamara = {
                    val dir = File(ctx.cacheDir, "camera").apply { mkdirs() }
                    val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", File(dir, "foto_${System.currentTimeMillis()}.jpg"))
                    vm.fotoEnCamara = uri
                    takePhoto.launch(uri)
                },
                onQuitar = vm::quitarMedio)

            HorizontalDivider()
            SeccionFecha(vm.usarFecha, vm::cambiarUsarFecha, vm.abreEl, vm::cambiarAbreEl, onElegirFecha = { vm.verFecha = true })
            SeccionClave(vm.usarClave, vm::cambiarUsarClave, vm.clave, vm::cambiarClave, vm.clave2, vm::cambiarClave2)

            HorizontalDivider()
            SeccionLugar(
                usarLugar = vm.usarLugar, onUsarLugar = { on -> vm.cambiarUsarLugar(on); if (on && vm.lat == null) ubicarConPermiso() },
                buscando = vm.buscandoLugar, lat = vm.lat, lng = vm.lng, precision = vm.precision, direccion = vm.direccion,
                cercanos = vm.cercanos, nombre = vm.nombreLugar, onNombre = vm::cambiarNombreLugar,
                onActualizar = ::ubicarConPermiso,
                soloAqui = vm.soloEnElLugar, onSoloAqui = vm::cambiarSoloEnElLugar, radio = vm.radio, onRadio = vm::cambiarRadio,
                rutas = vm.rutas, rutaId = vm.rutaId, onRuta = vm::cambiarRuta,
                onCrearRuta = { vm.crearRuta(it) },
            )

            HorizontalDivider()
            SeccionCompartir(vm.correos, vm::cambiarCorreos, vm.colaborativa, vm::cambiarColaborativa, vm.abrirJuntos, vm::cambiarAbrirJuntos)

            vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (vm.guardando) {
                LinearProgressIndicator(progress = { vm.progreso }, modifier = Modifier.fillMaxWidth())
                Text(vm.textoProgreso, color = Muted)
            }
            Button(onClick = vm::sellar, enabled = !vm.guardando, modifier = Modifier.fillMaxWidth().height(54.dp)) {
                Text(if (vm.guardando) "Guardando…" else "Sellar cápsula")
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    // ---------- Selector de fecha y hora ----------
    if (vm.verFecha) {
        val todayUtc = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = vm.abreEl?.let { it.atZone(ZoneId.systemDefault()).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() },
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= todayUtc
            },
        )
        DatePickerDialog(
            onDismissRequest = { vm.verFecha = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        vm.fechaElegida = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                        vm.verFecha = false; vm.verHora = true
                    }
                }) { Text("Siguiente") }
            },
            dismissButton = { TextButton(onClick = { vm.verFecha = false }) { Text("Cancelar") } },
        ) { DatePicker(state) }
    }
    if (vm.verHora) {
        val t = rememberTimePickerState(initialHour = 9, initialMinute = 0, is24Hour = false)
        AlertDialog(
            onDismissRequest = { vm.verHora = false },
            title = { Text("¿A qué hora?") },
            text = { TimePicker(t) },
            confirmButton = {
                TextButton(onClick = {
                    vm.fechaElegida?.let { d -> vm.cambiarAbreEl(d.atTime(LocalTime.of(t.hour, t.minute)).atZone(ZoneId.systemDefault()).toInstant()) }
                    vm.verHora = false
                }) { Text("Listo") }
            },
            dismissButton = { TextButton(onClick = { vm.verHora = false }) { Text("Cancelar") } },
        )
    }
}

/** Abre YouTube o Spotify buscando la canción. */
private fun openSearch(ctx: android.content.Context, base: String, q: String) {
    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, AUri.parse(base + AUri.encode(q.trim())))) }
}
