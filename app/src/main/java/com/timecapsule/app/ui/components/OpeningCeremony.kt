package com.timecapsule.app.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.timecapsule.app.data.AppSettings
import com.timecapsule.app.ui.screens.CloudImage
import com.timecapsule.app.ui.theme.LocalPalette
import com.timecapsule.app.ui.theme.Typewriter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

/** Vibración especial al abrir: tres toques que crecen, como un corazón. */
fun vibrateOpen(ctx: Context) {
    if (!AppSettings.look.value.vibrateOnOpen) return
    val v: Vibrator? = if (Build.VERSION.SDK_INT >= 31) (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    else @Suppress("DEPRECATION") (ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
    runCatching { v?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 60, 90, 80, 90, 180), intArrayOf(0, 120, 0, 180, 0, 255), -1)) }
}

/**
 * Ceremonia de apertura: el sello de lacre tiembla y se rompe, el sobre se abre,
 * sale una luz dorada, suben las polaroids y cae confeti. Toca para saltar.
 */
@Composable
fun OpeningCeremony(title: String, photoPaths: List<String>, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val p = LocalPalette.current
    val shake = remember { Animatable(0f) }
    val crack = remember { Animatable(0f) }
    val flap = remember { Animatable(0f) }
    val light = remember { Animatable(0f) }
    val rise = remember { Animatable(0f) }
    val textIn = remember { Animatable(0f) }
    val confetti = remember { Animatable(0f) }
    val photos = photoPaths.take(3)

    LaunchedEffect(Unit) {
        vibrateOpen(ctx)
        if (!AppSettings.look.value.animations) { onDone(); return@LaunchedEffect }
        shake.animateTo(1f, tween(650))
        launch { crack.animateTo(1f, tween(450, easing = FastOutSlowInEasing)) }
        delay(350)
        launch { flap.animateTo(1f, tween(500, easing = FastOutSlowInEasing)) }
        launch { light.animateTo(1f, tween(900)) }
        delay(450)
        confetti.snapTo(1f)
        launch { rise.animateTo(1f, bouncy) }
        launch { textIn.animateTo(1f, tween(600)) }
        delay(2600)
        onDone()
    }

    Dialog(onDismissRequest = onDone, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = true)) {
        Box(
            Modifier.fillMaxSize()
                .drawBehind {
                    drawRect(Color.Black.copy(alpha = 0.78f))
                    // Luz dorada que sale del sobre
                    drawCircle(Brush.radialGradient(listOf(p.gold.copy(alpha = 0.75f * light.value), Color.Transparent), center = center, radius = size.minDimension * (0.2f + 0.7f * light.value)), radius = size.minDimension)
                }
                .clickable(remember { MutableInteractionSource() }, indication = null) { onDone() },
            contentAlignment = Alignment.Center,
        ) {
            // Polaroids que suben del sobre
            photos.forEachIndexed { i, path ->
                val angle = listOf(-12f, 8f, -3f)[i]
                val dx = listOf(-70, 64, 0)[i]
                Polaroid(
                    Modifier.width(140.dp).offset(x = dx.dp, y = (60 - 210 * rise.value - i * 8).dp)
                        .graphicsLayer { alpha = rise.value.coerceIn(0f, 1f); rotationZ = angle * rise.value },
                ) { CloudImage(path, Modifier.fillMaxSize()) }
            }
            Box(Modifier.offset(y = 70.dp), contentAlignment = Alignment.Center) {
                Envelope(Modifier.size(230.dp, 150.dp), flap = flap.value)
                WaxSeal(
                    Modifier.offset(y = (-14).dp).graphicsLayer { rotationZ = sin(shake.value * 30f) * 9f * (1 - crack.value) },
                    size = 72.dp, cracked = crack.value,
                )
            }
            Column(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp).graphicsLayer { alpha = textIn.value; translationY = (1 - textIn.value) * 40f },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("¡Se abrió tu cápsula!", style = MaterialTheme.typography.headlineMedium, color = Color(0xFFFDF6E8))
                Spacer(Modifier.height(6.dp))
                Text(title, style = MaterialTheme.typography.titleLarge, color = p.gold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(14.dp))
                Text("toca para continuar", fontFamily = Typewriter, color = Color(0xFFD9CDB8), style = MaterialTheme.typography.labelSmall)
            }
            if (confetti.value > 0f) Confetti(Modifier.background(Color.Transparent))
        }
    }
}
