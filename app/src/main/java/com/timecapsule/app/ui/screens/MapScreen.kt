package com.timecapsule.app.ui.screens

import android.Manifest
import android.graphics.drawable.GradientDrawable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.timecapsule.app.data.Capsule
import com.timecapsule.app.ui.MainViewModel
import com.timecapsule.app.ui.theme.Cream
import com.timecapsule.app.ui.theme.Gold
import com.timecapsule.app.ui.theme.Muted
import com.timecapsule.app.ui.theme.NightSurface
import com.timecapsule.app.util.LocationUtil
import com.timecapsule.app.util.conditionText
import com.timecapsule.app.util.fmtDate
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@Composable
fun MapScreen(vm: MainViewModel, onOpen: (String) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val caps by vm.capsules.collectAsState()
    val tl by vm.timeline.collectAsState()
    val withPlace = caps.filter { it.hasPlace }.sortedByDescending { it.createdInstant }
    var selected by remember { mutableStateOf<Capsule?>(null) }
    var hasLocation by remember { mutableStateOf(LocationUtil.hasFine(ctx)) }
    // Brillo animado de los cofres cercanos (cambia de fase cada 0,5 s)
    var phase by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(500); phase = (phase + 1) % 4 } }

    val map = remember {
        MapView(ctx).apply {
            setTileSource(TileSourceFactory.MAPNIK)   // mapa real de OpenStreetMap
            setMultiTouchControls(true)
            controller.setZoom(5.5)
            controller.setCenter(GeoPoint(4.6, -74.08))
            minZoomLevel = 3.0
        }
    }
    // Punto azul con tu ubicación GPS en tiempo real
    val myLocation = remember { MyLocationNewOverlay(GpsMyLocationProvider(ctx), map) }

    fun goToMe() = scope.launch {
        val loc = LocationUtil.current(ctx) ?: return@launch
        map.controller.animateTo(GeoPoint(loc.latitude, loc.longitude), 16.0, 800L)
    }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        hasLocation = res[Manifest.permission.ACCESS_FINE_LOCATION] == true || res[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (hasLocation) { myLocation.enableMyLocation(); goToMe() }
    }

    LaunchedEffect(Unit) {
        if (!hasLocation) perm.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        else if (withPlace.isEmpty()) goToMe()
    }
    DisposableEffect(hasLocation) {
        map.onResume()
        if (hasLocation) myLocation.enableMyLocation()
        onDispose { myLocation.disableMyLocation(); map.onPause() }
    }
    DisposableEffect(Unit) { onDispose { map.onDetach() } }

    Box(Modifier.fillMaxSize()) {
        AndroidView(factory = { map }, modifier = Modifier.fillMaxSize(), update = { m ->
            m.overlays.clear()
            if (hasLocation) m.overlays.add(myLocation)
            val me = myLocation.myLocation
            // Rutas: línea punteada que une las cápsulas en su orden
            withPlace.filter { it.routeId != null }.groupBy { it.routeId }.values.forEach { stops ->
                val pts = stops.sortedBy { it.routeOrder ?: 0 }.map { GeoPoint(it.lat!!, it.lng!!) }
                if (pts.size > 1) m.overlays.add(org.osmdroid.views.overlay.Polyline(m).apply {
                    setPoints(pts)
                    outlinePaint.color = 0xFFB8893A.toInt(); outlinePaint.strokeWidth = 7f
                    outlinePaint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(22f, 16f), phase * 9.5f)
                })
            }
            withPlace.forEach { c ->
                m.overlays.add(Marker(m).apply {
                    position = GeoPoint(c.lat!!, c.lng!!)
                    title = c.placeName ?: c.title
                    snippet = c.title
                    val near = me != null && LocationUtil.distanceMeters(me.latitude, me.longitude, c.lat!!, c.lng!!) <= 500
                    icon = chest(m.context, open = c.isOpen, glow = if (near && !c.isOpen) phase else -1)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    setOnMarkerClickListener { _, _ -> selected = c; true }
                })
            }
            if (withPlace.isNotEmpty() && m.tag != withPlace.size) {
                m.tag = withPlace.size
                m.post {
                    if (withPlace.size == 1) { m.controller.setZoom(16.0); m.controller.setCenter(GeoPoint(withPlace[0].lat!!, withPlace[0].lng!!)) }
                    else m.zoomToBoundingBox(BoundingBox.fromGeoPoints(withPlace.map { GeoPoint(it.lat!!, it.lng!!) }).increaseByScale(1.4f), false)
                }
            }
            m.invalidate()
        })

        // Lista de lugares registrados (arriba)
        if (withPlace.isEmpty()) Text(
            "Cuando guardes una cápsula con la ubicación encendida, su lugar quedará marcado aquí.",
            color = Cream, modifier = Modifier.align(Alignment.TopCenter).padding(16.dp)
                .clip(RoundedCornerShape(6.dp)).background(NightSurface).padding(14.dp),
        ) else LazyRow(
            Modifier.align(Alignment.TopCenter).fillMaxWidth(),
            contentPadding = PaddingValues(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(withPlace, key = { it.id }) { c ->
                AssistChip(
                    onClick = {
                        selected = c
                        map.controller.animateTo(GeoPoint(c.lat!!, c.lng!!), 17.0, 700L)
                    },
                    label = { Text(c.placeName ?: c.title, maxLines = 1) },
                    leadingIcon = { Icon(Icons.Filled.Place, null, tint = if (c.isOpen) MaterialTheme.colorScheme.primary else Gold) },
                    modifier = Modifier.background(NightSurface, RoundedCornerShape(8.dp)),
                )
            }
        }

        // Botón "ir a mi ubicación"
        if (selected == null) SmallFloatingActionButton(
            onClick = { if (hasLocation) goToMe() else perm.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(18.dp),
        ) { Icon(Icons.Filled.MyLocation, "Mi ubicación") }

        // Tarjeta del lugar seleccionado
        selected?.let { c ->
            val note = tl?.lugares?.firstOrNull { p ->
                (c.placeName != null && p.nombre.contains(c.placeName, ignoreCase = true)) ||
                    (p.lat != null && p.lng != null && kotlin.math.abs(p.lat - c.lat!!) < 0.01 && kotlin.math.abs(p.lng - c.lng!!) < 0.01)
            }
            val samePlace = withPlace.count { LocationUtil.distanceMeters(it.lat!!, it.lng!!, c.lat!!, c.lng!!) < 150 }
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(14.dp)
                    .clip(RoundedCornerShape(6.dp)).background(NightSurface).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Meta(Icons.Filled.Place, c.placeName ?: "Lugar sin nombre", Gold)
                Text(c.title, style = MaterialTheme.typography.titleLarge, color = Cream)
                Text("Guardada el ${c.createdInstant.fmtDate()} · %.5f, %.5f".format(c.lat, c.lng), color = Muted, style = MaterialTheme.typography.bodySmall)
                Text(c.conditionText(), color = if (c.isOpen) MaterialTheme.colorScheme.primary else Gold)
                if (samePlace > 1) Text("Tienes $samePlace cápsulas en este lugar.", color = Muted)
                note?.let { Meta(Icons.Filled.AutoAwesome, it.resumen, Gold) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onOpen(c.id) }) { Text(if (c.isOpen) "Ver recuerdos" else "Ver cápsula") }
                    TextButton(onClick = { selected = null }) { Text("Cerrar") }
                }
            }
        }
    }
}

/** Punto redondo con borde, del color del estado de la cápsula. */
/** Cofre dibujado: cerrado (madera con cerradura dorada) o abierto (con luz). glow 0..3 = brillo animado. */
private fun chest(ctx: android.content.Context, open: Boolean, glow: Int): android.graphics.drawable.BitmapDrawable {
    val s = 96
    val b = android.graphics.Bitmap.createBitmap(s, s, android.graphics.Bitmap.Config.ARGB_8888)
    val c = android.graphics.Canvas(b)
    val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    if (glow >= 0) {   // halo dorado que late
        p.color = 0xFFE2B865.toInt(); p.alpha = 60 + glow * 35
        c.drawCircle(s / 2f, s / 2f, 30f + glow * 4f, p)
    }
    if (open) { p.color = 0xFFFFE9A8.toInt(); p.alpha = 200; c.drawCircle(s / 2f, 40f, 22f, p) }
    p.alpha = 255
    // cuerpo
    p.color = 0xFF7A4A28.toInt(); c.drawRoundRect(22f, 46f, 74f, 78f, 6f, 6f, p)
    // tapa (inclinada si está abierto)
    c.save()
    if (open) c.rotate(-28f, 22f, 46f)
    p.color = 0xFF94592F.toInt(); c.drawRoundRect(20f, 30f, 76f, 48f, 10f, 10f, p)
    c.restore()
    // herrajes dorados
    p.color = 0xFFB8893A.toInt()
    c.drawRect(22f, 56f, 74f, 61f, p)
    if (!open) c.drawRoundRect(42f, 40f, 54f, 58f, 3f, 3f, p)
    p.style = android.graphics.Paint.Style.STROKE; p.strokeWidth = 3f; p.color = 0xFF3B2416.toInt()
    c.drawRoundRect(22f, 46f, 74f, 78f, 6f, 6f, p)
    return android.graphics.drawable.BitmapDrawable(ctx.resources, b)
}
