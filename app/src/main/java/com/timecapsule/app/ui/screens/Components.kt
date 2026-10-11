package com.timecapsule.app.ui.screens

import androidx.compose.runtime.remember
import com.timecapsule.app.ui.components.goldBorder
import com.timecapsule.app.ui.components.pressScale
import com.timecapsule.app.ui.components.enterAnim
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.filled.LockOpen
import com.timecapsule.app.ui.components.Envelope
import com.timecapsule.app.ui.components.Polaroid
import com.timecapsule.app.ui.components.WaxSeal
import com.timecapsule.app.ui.components.floating
import com.timecapsule.app.ui.theme.LetterPaper
import com.timecapsule.app.ui.theme.Line
import com.timecapsule.app.ui.theme.NightRaised
import com.timecapsule.app.ui.theme.Olive
import com.timecapsule.app.ui.theme.Typewriter
import com.timecapsule.app.ui.theme.Wax
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.timecapsule.app.data.Capsule
import com.timecapsule.app.data.Repo
import com.timecapsule.app.ui.theme.CapsuleGradient
import com.timecapsule.app.ui.theme.Cream
import com.timecapsule.app.ui.theme.Gold
import com.timecapsule.app.ui.theme.LockedGradient
import com.timecapsule.app.ui.theme.Muted
import com.timecapsule.app.ui.theme.NightSurface
import com.timecapsule.app.util.conditionText
import com.timecapsule.app.util.fmtDate

/** Imagen guardada en la nube (pide un enlace temporal y la muestra). */
@Composable
fun CloudImage(path: String, modifier: Modifier = Modifier) {
    val url by produceState<String?>(null, path) { value = runCatching { Repo.signedUrl(path) }.getOrNull() }
    if (url != null) AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier)
    else Box(modifier.background(NightSurface))
}

@Composable
fun CapsuleCard(c: Capsule, me: String?, index: Int = 0, onClick: () -> Unit) {
    val readyHere = !c.isOpen && c.dateReady
    val press = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Column(
        Modifier.fillMaxWidth().enterAnim(delayMs = (index * 70).coerceAtMost(560)).floating(phase = (c.id.hashCode() and 0xFFF))
            .pressScale(press)
            .shadow(4.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp)).background(NightSurface).goldBorder(RoundedCornerShape(8.dp))
            .clickable(interactionSource = press, indication = androidx.compose.material3.ripple(), onClick = onClick)
    ) {
        Box(Modifier.fillMaxWidth().height(if (c.isOpen && c.coverPath != null) 190.dp else 118.dp), contentAlignment = Alignment.Center) {
            if (c.isOpen && c.coverPath != null) {
                // Abierta: la portada como polaroid sobre papel
                Box(Modifier.fillMaxSize().background(NightRaised))
                Polaroid(Modifier.width(150.dp).padding(top = 12.dp), rotation = if (c.id.hashCode() % 2 == 0) -3f else 3f) {
                    CloudImage(c.coverPath, Modifier.fillMaxSize())
                }
            } else if (c.isOpen) {
                Box(Modifier.fillMaxSize().background(CapsuleGradient))
                Icon(Icons.Filled.LockOpen, null, tint = LetterPaper, modifier = Modifier.size(34.dp))
            } else {
                // Cerrada: sobre sellado con lacre (brilla si ya se puede abrir)
                Envelope(Modifier.fillMaxSize())
                WaxSeal(size = 54.dp, glow = readyHere)
                Text(
                    if (c.mediaCount > 0) "${c.mediaCount} recuerdos esperando" else "Carta sellada",
                    fontFamily = Typewriter, style = MaterialTheme.typography.labelSmall, color = Cream,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
                )
            }
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(c.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                c.conditionText(),
                color = if (c.isOpen) Olive else Wax,
                fontWeight = FontWeight.Medium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Sellada el ${c.createdInstant.fmtDate()}", color = Muted, fontFamily = Typewriter, style = MaterialTheme.typography.labelSmall)
                if (c.isOpen && c.mediaCount > 0) Meta(Icons.Filled.PhotoLibrary, "${c.mediaCount}")
                c.placeName?.let { Meta(Icons.Filled.Place, it) }
            }
            if (c.owner != me) Meta(Icons.Filled.People, "Compartida por ${c.ownerName ?: "alguien"}")
        }
    }
}

@Composable
fun Meta(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: Color = Muted) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, color = color, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun EmptyState(title: String, text: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = Cream)
        Spacer(Modifier.height(8.dp))
        Text(text, color = Muted, style = MaterialTheme.typography.bodyMedium)
    }
}
