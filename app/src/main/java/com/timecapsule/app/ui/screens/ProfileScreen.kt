package com.timecapsule.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import com.timecapsule.app.ui.theme.Line
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.timecapsule.app.data.Repo
import com.timecapsule.app.data.AppSettings
import androidx.compose.material3.FilterChip
import com.timecapsule.app.geo.GeofenceManager
import com.timecapsule.app.notify.Notifier
import com.timecapsule.app.ui.MainViewModel
import com.timecapsule.app.ui.theme.Cream
import com.timecapsule.app.ui.theme.Gold
import com.timecapsule.app.ui.theme.Muted
import com.timecapsule.app.ui.theme.NightSurface

@Composable
fun ProfileScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val caps by vm.capsules.collectAsState()
    val me = Repo.userId()
    var tick by remember { mutableIntStateOf(0) }   // fuerza a releer permisos
    val geoOn = remember(tick) { GeofenceManager.hasPermission(ctx) }
    val notifOn = remember(tick) { Notifier.canNotify(ctx) }

    val bgPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++; vm.refresh() }
    val finePerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        tick++
        if (res[Manifest.permission.ACCESS_FINE_LOCATION] == true && Build.VERSION.SDK_INT >= 29) {
            bgPerm.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else vm.refresh()
    }
    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Card {
            Text(Repo.userName(), style = MaterialTheme.typography.headlineSmall, color = Cream)
            Text(Repo.userEmail(), color = Muted)
        }
        Card {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Stat("${caps.count { it.owner == me }}", "creadas")
                Stat("${caps.count { it.isOpen }}", "abiertas")
                Stat("${caps.count { !it.isOpen }}", "cerradas")
                Stat("${caps.count { it.owner != me }}", "compartidas")
            }
        }
        Card {
            Text("Recuerdos por ubicación", style = MaterialTheme.typography.titleMedium, color = Cream)
            Text(
                if (geoOn) "Activado. Te avisaremos cuando vuelvas a un lugar donde guardaste recuerdos."
                else "Para avisarte cuando vuelvas a un lugar, Time Capsule necesita tu ubicación “todo el tiempo”. " +
                    "En la siguiente pantalla elige “Permitir todo el tiempo”.",
                color = Muted,
            )
            if (!geoOn) Button(onClick = {
                finePerm.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }) { Text("Activar") }
        }
        if (!notifOn && Build.VERSION.SDK_INT >= 33) Card {
            Text("Notificaciones desactivadas", style = MaterialTheme.typography.titleMedium, color = Gold)
            Text("Sin ellas no sabrás cuándo se abre una cápsula.", color = Muted)
            Button(onClick = { notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text("Activar notificaciones") }
        }
        LookCard()
        OutlinedButton(onClick = { vm.signOut() }, modifier = Modifier.fillMaxWidth()) { Text("Cerrar sesión") }
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(NightSurface).border(1.dp, Line, RoundedCornerShape(6.dp)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
}

@Composable
private fun Stat(value: String, label: String) {
    Column {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = Cream)
        Text(label, color = Muted, style = MaterialTheme.typography.bodySmall)
    }
}

/* ---------- Apariencia y accesibilidad ---------- */
@Composable
private fun LookCard() {
    val ctx = LocalContext.current
    val look by AppSettings.look.collectAsState()
    Card {
        Text("Apariencia y accesibilidad", style = MaterialTheme.typography.titleMedium, color = Cream)
        Text("Tema", color = Muted, style = MaterialTheme.typography.bodySmall)
        Choice(listOf("system" to "Automático", "light" to "Día", "dark" to "Noche"), look.theme) { v -> AppSettings.update(ctx) { it.copy(theme = v) } }
        Text("Tamaño del texto", color = Muted, style = MaterialTheme.typography.bodySmall)
        Choice(listOf("1.0" to "Normal", "1.15" to "Grande", "1.3" to "Muy grande"), look.textScale.toString()) { v -> AppSettings.update(ctx) { it.copy(textScale = v.toFloat()) } }
        SwitchRow("Alto contraste", look.highContrast) { v -> AppSettings.update(ctx) { it.copy(highContrast = v) } }
        SwitchRow("Animaciones", look.animations) { v -> AppSettings.update(ctx) { it.copy(animations = v) } }
        SwitchRow("Vibrar al abrir una cápsula", look.vibrateOnOpen) { v -> AppSettings.update(ctx) { it.copy(vibrateOnOpen = v) } }
        SwitchRow("Leer la carta en voz alta al abrir", look.readAloud) { v -> AppSettings.update(ctx) { it.copy(readAloud = v) } }
    }
}

@Composable
private fun Choice(options: List<Pair<String, String>>, selected: String, onPick: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            FilterChip(selected = selected == value, onClick = { onPick(value) }, label = { Text(label) })
        }
    }
}
