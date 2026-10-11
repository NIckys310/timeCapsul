package com.timecapsule.app.ui.components

import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.timecapsule.app.data.AppSettings
import com.timecapsule.app.ui.theme.LocalPalette
import com.timecapsule.app.ui.theme.Typewriter
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/* ---------- ¿Animaciones activas? (Ajustes → Animaciones) ---------- */
@Composable
fun animationsOn(): Boolean = AppSettings.look.collectAsState().value.animations

/* ---------- Fondo de papel: puntitos sepia y viñeta suave ---------- */
fun Modifier.paper(): Modifier = composed {
    val p = LocalPalette.current
    val specks = remember { List(220) { Triple(Random.nextFloat(), Random.nextFloat(), Random.nextFloat()) } }
    val grain = remember { List(34) { Triple(Random.nextFloat(), 0.4f + Random.nextFloat() * 1.2f, Random.nextFloat()) } }
    drawBehind {
        drawRect(p.bg)
        if (p.dark) {
            // Vetas de madera: líneas onduladas suaves en tonos dorados y cafés
            grain.forEach { (y0, w, a) ->
                val path = androidx.compose.ui.graphics.Path()
                val y = y0 * size.height
                path.moveTo(0f, y)
                var x = 0f
                while (x <= size.width) { path.lineTo(x, y + sin(x / (60f + a * 90f) + a * 6f) * (6f + a * 10f)); x += 12f }
                drawPath(path, (if (a > 0.6f) p.gold else p.raised).copy(alpha = 0.10f + a * 0.10f), style = Stroke(width = w * 2f))
            }
        }
        specks.forEach { (x, y, a) -> drawCircle(p.muted.copy(alpha = 0.05f + a * 0.06f), radius = 0.6f + a * 1.4f, center = Offset(x * size.width, y * size.height)) }
        drawRect(Brush.radialGradient(listOf(Color.Transparent, p.muted.copy(alpha = if (p.dark) 0.18f else 0.10f)), center = center, radius = size.maxDimension * 0.75f))
    }
}

/* ---------- Tarjetas que flotan suavemente ---------- */
fun Modifier.floating(amplitude: Dp = 3.dp, periodMs: Int = 3200, phase: Int = 0): Modifier = composed {
    if (!animationsOn()) return@composed this
    val t = rememberInfiniteTransition(label = "flotar")
    val y by t.animateFloat(-1f, 1f, infiniteRepeatable(tween(periodMs, delayMillis = phase % periodMs, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "y")
    graphicsLayer { translationY = y * amplitude.toPx() }
}

/* ---------- Sello de lacre (se puede romper: cracked 0..1) ---------- */
@Composable
fun WaxSeal(modifier: Modifier = Modifier, size: Dp = 64.dp, cracked: Float = 0f, glow: Boolean = false) {
    val p = LocalPalette.current
    val pulse = if (glow && animationsOn()) rememberInfiniteTransition(label = "brillo")
        .animateFloat(0.0f, 1f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "g").value else 0f
    Canvas(modifier.size(size)) {
        val r = this.size.minDimension / 2f
        if (glow) drawCircle(p.gold.copy(alpha = 0.18f + 0.22f * pulse), radius = r * (1.15f + 0.12f * pulse))
        // Las dos mitades se separan al romperse
        listOf(-1f, 1f).forEach { side ->
            val dx = side * cracked * r * 0.9f
            rotate(degrees = side * cracked * 28f, pivot = Offset(center.x + dx, center.y)) {
                clipHalf(side) { drawSealDisk(p.wax, p.waxDark, p.gold, Offset(center.x + dx, center.y + cracked * r * 0.5f), r) }
            }
        }
    }
}

private fun DrawScope.clipHalf(side: Float, block: DrawScope.() -> Unit) {
    val path = Path().apply {
        // Línea de quiebre en zigzag por el centro
        val cx = size.width / 2f
        val pts = listOf(0f to 0f, 0.18f to 0.06f, 0.32f to -0.05f, 0.5f to 0.05f, 0.68f to -0.04f, 0.84f to 0.05f, 1f to 0f)
        if (side < 0) { moveTo(0f, 0f); lineTo(cx, 0f) } else { moveTo(size.width, 0f); lineTo(cx, 0f) }
        pts.forEach { (yy, xx) -> lineTo(cx + xx * size.width, yy * size.height) }
        if (side < 0) { lineTo(0f, size.height) } else { lineTo(size.width, size.height) }
        close()
    }
    drawContext.canvas.save()
    drawContext.canvas.clipPath(path)
    block()
    drawContext.canvas.restore()
}

private fun DrawScope.drawSealDisk(wax: Color, waxDark: Color, gold: Color, c: Offset, r: Float) {
    // Borde irregular de cera derretida
    val blob = Path()
    val n = 28
    for (i in 0..n) {
        val a = 2 * PI * i / n
        val rr = r * (0.9f + 0.08f * sin(a * 5 + 1.3).toFloat() + 0.04f * cos(a * 9).toFloat())
        val pt = Offset(c.x + (rr * cos(a)).toFloat(), c.y + (rr * sin(a)).toFloat())
        if (i == 0) blob.moveTo(pt.x, pt.y) else blob.lineTo(pt.x, pt.y)
    }
    blob.close()
    drawPath(blob, Brush.radialGradient(listOf(wax, waxDark), center = Offset(c.x - r * 0.3f, c.y - r * 0.3f), radius = r * 1.4f))
    drawCircle(waxDark.copy(alpha = 0.55f), radius = r * 0.62f, center = c, style = Stroke(width = r * 0.08f))
    // Reloj de arena grabado en el lacre
    val h = r * 0.34f
    val glass = Path().apply {
        moveTo(c.x - h * 0.7f, c.y - h); lineTo(c.x + h * 0.7f, c.y - h); lineTo(c.x, c.y)
        lineTo(c.x + h * 0.7f, c.y + h); lineTo(c.x - h * 0.7f, c.y + h); lineTo(c.x, c.y); close()
    }
    drawPath(glass, gold.copy(alpha = 0.85f), style = Stroke(width = r * 0.07f))
    drawCircle(Color.White.copy(alpha = 0.22f), radius = r * 0.18f, center = Offset(c.x - r * 0.38f, c.y - r * 0.4f))
}

/* ---------- Sobre (la solapa se abre con flap 0..1) ---------- */
@Composable
fun Envelope(modifier: Modifier = Modifier, flap: Float = 0f) {
    val p = LocalPalette.current
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val body = if (p.dark) Color(0xFF4A3B2C) else Color(0xFFE8D6B3)
        val shade = if (p.dark) Color(0xFF3A2E22) else Color(0xFFD9C29A)
        drawRect(body)
        // Pliegues inferiores
        drawPath(Path().apply { moveTo(0f, h); lineTo(w / 2, h * 0.52f); lineTo(w, h); close() }, shade)
        drawPath(Path().apply { moveTo(0f, 0f); lineTo(w * 0.5f, h * 0.55f); lineTo(0f, h); close() }, shade.copy(alpha = 0.55f))
        drawPath(Path().apply { moveTo(w, 0f); lineTo(w * 0.5f, h * 0.55f); lineTo(w, h); close() }, shade.copy(alpha = 0.55f))
        // Solapa: se "levanta" escalándose hacia arriba
        val tip = h * (0.58f - 1.1f * flap)
        drawPath(Path().apply { moveTo(0f, 0f); lineTo(w / 2, tip); lineTo(w, 0f); close() }, if (flap > 0.5f) shade else body.copy(red = body.red * 0.97f))
        drawPath(Path().apply { moveTo(0f, 0f); lineTo(w / 2, tip); lineTo(w, 0f) }, p.line, style = Stroke(1.5f))
    }
}

/* ---------- Foto tipo polaroid ---------- */
@Composable
fun Polaroid(modifier: Modifier = Modifier, rotation: Float = 0f, caption: String? = null, content: @Composable BoxScope.() -> Unit) {
    Column(
        modifier
            .graphicsLayer { rotationZ = rotation }
            .shadow(6.dp, RectangleShape)
            .background(Color(0xFFFDFBF5))
            .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = if (caption != null) 6.dp else 22.dp)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).background(Color(0xFF2A2420))) { content() }
        if (caption != null) Text(
            caption, color = Color(0xFF4A3B2E), fontFamily = Typewriter, style = MaterialTheme.typography.labelSmall,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        )
    }
}

/* ---------- Número que cambia deslizándose (cuenta regresiva) ---------- */
@Composable
fun FlipNumber(value: Long, label: String, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.border(1.dp, p.line, MaterialTheme.shapes.small).background(p.surface, MaterialTheme.shapes.small)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            AnimatedContent(
                targetState = value,
                transitionSpec = { (slideInVertically { -it } + fadeIn()) togetherWith (slideOutVertically { it } + fadeOut()) },
                label = "cifra",
            ) { v -> Text(v.toString().padStart(2, '0'), style = MaterialTheme.typography.displaySmall, fontFamily = Typewriter, color = p.ink, fontWeight = FontWeight.Bold) }
        }
        Text(label, color = p.muted, fontFamily = Typewriter, style = MaterialTheme.typography.labelSmall)
    }
}

/* ---------- Confeti ---------- */
private data class Bit(val x: Float, val speed: Float, val drift: Float, val spin: Float, val color: Color, val w: Float, val delay: Float)

@Composable
fun Confetti(modifier: Modifier = Modifier, count: Int = 90, durationMs: Int = 2600) {
    val p = LocalPalette.current
    val colors = listOf(p.wax, p.gold, p.olive, Color(0xFFFDFBF5), Color(0xFF6FA8C9))
    val bits = remember { List(count) { Bit(Random.nextFloat(), 0.6f + Random.nextFloat() * 0.8f, Random.nextFloat() * 2 - 1, Random.nextFloat() * 720 - 360, colors.random(), 6f + Random.nextFloat() * 8f, Random.nextFloat() * 0.35f) } }
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) { t.animateTo(1f, tween(durationMs, easing = LinearEasing)) }
    Canvas(modifier.fillMaxSize()) {
        bits.forEach { b ->
            val k = ((t.value - b.delay) / (1 - b.delay)).coerceIn(0f, 1f)
            if (k <= 0f || k >= 1f) return@forEach
            val x = b.x * size.width + b.drift * 80f * sin(k * 6f)
            val y = -20f + k * b.speed * size.height * 1.2f
            rotate(b.spin * k, Offset(x, y)) {
                drawRect(b.color.copy(alpha = 1f - k * 0.4f), topLeft = Offset(x - b.w / 2, y - b.w / 4), size = Size(b.w, b.w / 2))
            }
        }
    }
}

/* ---------- Muelle para animaciones con rebote ---------- */
val bouncy = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)

/* ---------- Borde dorado (en la noche brilla más) ---------- */
fun Modifier.goldBorder(shape: androidx.compose.ui.graphics.Shape, width: Dp = 1.5.dp): Modifier = composed {
    val p = LocalPalette.current
    val brush = if (p.dark) Brush.linearGradient(listOf(p.gold, p.line, p.gold.copy(alpha = 0.7f), p.line))
                else Brush.linearGradient(listOf(p.line, p.line))
    border(width, brush, shape)
}

/* ---------- Entrada animada: sube y aparece (una sola vez) ---------- */
fun Modifier.enterAnim(delayMs: Int = 0): Modifier = composed {
    if (!animationsOn()) return@composed this
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(delayMs.toLong()); a.animateTo(1f, tween(520, easing = FastOutSlowInEasing)) }
    graphicsLayer { alpha = a.value; translationY = (1 - a.value) * 60f; scaleX = 0.96f + 0.04f * a.value; scaleY = scaleX }
}

/* ---------- Se hunde un poco al tocarlo ---------- */
fun Modifier.pressScale(interaction: androidx.compose.foundation.interaction.InteractionSource): Modifier = composed {
    val pressed by interaction.collectIsPressedAsState()
    val s by androidx.compose.animation.core.animateFloatAsState(if (pressed) 0.96f else 1f, bouncy, label = "presionar")
    graphicsLayer { scaleX = s; scaleY = s }
}

/* ---------- Late suavemente (para llamar la atención) ---------- */
fun Modifier.pulse(): Modifier = composed {
    if (!animationsOn()) return@composed this
    val t = rememberInfiniteTransition(label = "latido")
    val s by t.animateFloat(1f, 1.06f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "s")
    graphicsLayer { scaleX = s; scaleY = s }
}
