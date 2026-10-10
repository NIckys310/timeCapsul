package com.timecapsule.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Lock
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
fun CapsuleCard(c: Capsule, me: String?, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(NightSurface).clickable(onClick = onClick)
    ) {
        Box(Modifier.fillMaxWidth().height(if (c.isOpen && c.coverPath != null) 170.dp else 96.dp)) {
            if (c.isOpen && c.coverPath != null) {
                CloudImage(c.coverPath, Modifier.fillMaxSize())
            } else {
                Box(Modifier.fillMaxSize().background(if (c.isOpen) CapsuleGradient else LockedGradient))
                if (!c.isOpen) Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Lock, null, tint = Gold)
                    Spacer(Modifier.width(8.dp))
                    Text(if (c.mediaCount > 0) "${c.mediaCount} recuerdos esperando" else "Carta sellada", color = Gold)
                }
            }
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(c.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                c.conditionText(),
                color = if (c.isOpen) MaterialTheme.colorScheme.primary else Gold,
                fontWeight = FontWeight.Medium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Creada el ${c.createdInstant.fmtDate()}", color = Muted, style = MaterialTheme.typography.bodySmall)
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
