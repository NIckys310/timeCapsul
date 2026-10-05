package com.timecapsule.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timecapsule.app.data.Capsule
import com.timecapsule.app.data.Chapter
import com.timecapsule.app.ui.MainViewModel
import com.timecapsule.app.ui.theme.CapsuleGradient
import com.timecapsule.app.ui.theme.Coral
import com.timecapsule.app.ui.theme.Cream
import com.timecapsule.app.ui.theme.Gold
import com.timecapsule.app.ui.theme.Muted
import com.timecapsule.app.ui.theme.NightRaised
import com.timecapsule.app.ui.theme.NightSurface
import com.timecapsule.app.util.fmtDate
import com.timecapsule.app.util.fmtMonth

@Composable
fun TimelineScreen(vm: MainViewModel, onOpen: (String) -> Unit) {
    val caps by vm.capsules.collectAsState()
    val tl by vm.timeline.collectAsState()
    val busy by vm.timelineLoading.collectAsState()
    val byId = caps.associateBy { it.id }

    // Sin IA: capítulos por mes. Con IA: capítulos con nombre y descripción.
    val chapters: List<Chapter> = tl?.capitulos?.takeIf { it.isNotEmpty() } ?: caps
        .sortedBy { it.createdInstant }
        .groupBy { it.createdInstant.fmtMonth() }
        .map { (month, list) -> Chapter(titulo = month, emoji = "🗓️", capsulas = list.map { it.id }) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(NightRaised).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Meta(Icons.Filled.AutoAwesome, "Tu historia, contada por IA", Gold)
                Text(tl?.resumen?.ifBlank { null } ?: "La IA agrupa tus cápsulas en capítulos de tu vida y cuenta lo que vivías en cada uno.",
                    color = Cream)
                if (busy) Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp)); Text("Escribiendo tu línea de tiempo…", color = Muted)
                } else if (tl == null) Button(onClick = { vm.buildTimeline() }, enabled = caps.isNotEmpty()) { Text("Crear con IA") }
                else OutlinedButton(onClick = { vm.buildTimeline() }) { Text("Actualizar con IA") }
            }
            Spacer(Modifier.height(20.dp))
        }
        if (caps.isEmpty()) item { EmptyState("Aún no hay historia", "Crea tu primera cápsula y aparecerá aquí.") }
        items(chapters.size) { i ->
            ChapterNode(chapters[i], chapters[i].capsulas.mapNotNull { byId[it] }, isLast = i == chapters.lastIndex, onOpen)
        }
    }
}

@Composable
private fun ChapterNode(ch: Chapter, caps: List<Capsule>, isLast: Boolean, onOpen: (String) -> Unit) {
    var expanded by remember { mutableStateOf(true) }
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        // Línea vertical con el punto del capítulo
        Box(Modifier.width(44.dp).fillMaxHeight()) {
            if (!isLast) Box(Modifier.align(Alignment.TopCenter).padding(top = 34.dp).width(2.dp).fillMaxHeight().background(NightRaised))
            Box(Modifier.align(Alignment.TopCenter).size(36.dp).clip(CircleShape).background(CapsuleGradient), contentAlignment = Alignment.Center) {
                Text(ch.emoji.ifBlank { "✨" }, fontSize = 16.sp)
            }
        }
        Column(Modifier.weight(1f).padding(bottom = 26.dp).animateContentSize()) {
            Column(Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(top = 4.dp)) {
                Text(ch.titulo, style = MaterialTheme.typography.titleLarge, color = Cream)
                if (ch.periodo.isNotBlank()) Text(ch.periodo, color = Coral, style = MaterialTheme.typography.labelLarge)
                if (ch.descripcion.isNotBlank()) {
                    Spacer(Modifier.height(4.dp)); Text(ch.descripcion, color = Muted)
                }
            }
            AnimatedVisibility(expanded) {
                Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    caps.forEach { c ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(NightSurface)
                                .clickable { onOpen(c.id) }.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)).background(NightRaised), contentAlignment = Alignment.Center) {
                                if (c.isOpen && c.coverPath != null) CloudImage(c.coverPath, Modifier.fillMaxSize())
                                else Icon(Icons.Filled.Lock, null, tint = if (c.isOpen) Coral else Gold)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(c.title, color = Cream, maxLines = 1)
                                Text(c.createdInstant.fmtDate() + (c.placeName?.let { " · $it" } ?: ""), color = Muted,
                                    style = MaterialTheme.typography.bodySmall, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}
