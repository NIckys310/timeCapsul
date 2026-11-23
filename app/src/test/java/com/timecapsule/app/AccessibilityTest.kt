package com.timecapsule.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.timecapsule.app.ui.theme.DayPalette
import com.timecapsule.app.ui.theme.NightPalette
import com.timecapsule.app.ui.theme.Palette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.max
import kotlin.math.min

/** Accesibilidad: contraste de colores (WCAG AA 4,5:1) y letras de al menos 12 sp. */
class AccessibilityTest {
    private fun contrast(a: Color, b: Color): Double {
        val x = a.luminance().toDouble(); val y = b.luminance().toDouble()
        return (max(x, y) + 0.05) / (min(x, y) + 0.05)
    }

    private fun check(name: String, p: Palette, waxEnvelope: Color, goldEnvelope: Color) {
        listOf(
            "tinta/fondo" to contrast(p.ink, p.bg),
            "tinta/tarjeta" to contrast(p.ink, p.surface),
            "secundario/fondo" to contrast(p.muted, p.bg),
            "secundario/tarjeta" to contrast(p.muted, p.surface),
            "dorado/tarjeta" to contrast(p.gold, p.surface),
            "texto sobre botón" to contrast(waxEnvelope, p.wax),
            "texto sobre dorado" to contrast(goldEnvelope, p.gold),
            "carta" to contrast(p.letterInk, p.letter),
        ).forEach { (par, v) -> assertTrue("$name $par: %.2f".format(v), v >= 4.5) }
    }

    @Test fun `modo dia cumple contraste`() = check("día", DayPalette, Color(0xFFFFF4E8), Color(0xFFFFF8EC))
    @Test fun `modo noche cumple contraste`() = check("noche", NightPalette, Color(0xFF2A1408), Color(0xFF2A1F00))

    @Test fun `ningun texto mide menos de 12 sp`() {
        val small = File("src/main/java/com/timecapsule/app").walkTopDown().filter { it.extension == "kt" }
            .flatMap { f -> Regex("""fontSize = (\d+(?:\.\d+)?)\.sp""").findAll(f.readText()).map { f.name to it.groupValues[1].toDouble() } }
            .filter { it.second < 12 }.toList()
        assertEquals(emptyList<Pair<String, Double>>(), small)
    }
}
