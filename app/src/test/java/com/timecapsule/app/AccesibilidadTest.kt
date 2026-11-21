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
class AccesibilidadTest {
    private fun contraste(a: Color, b: Color): Double {
        val x = a.luminance().toDouble(); val y = b.luminance().toDouble()
        return (max(x, y) + 0.05) / (min(x, y) + 0.05)
    }

    private fun revisar(nombre: String, p: Palette, sobreLacre: Color, sobreDorado: Color) {
        listOf(
            "tinta/fondo" to contraste(p.ink, p.bg),
            "tinta/tarjeta" to contraste(p.ink, p.surface),
            "secundario/fondo" to contraste(p.muted, p.bg),
            "secundario/tarjeta" to contraste(p.muted, p.surface),
            "dorado/tarjeta" to contraste(p.gold, p.surface),
            "texto sobre botón" to contraste(sobreLacre, p.wax),
            "texto sobre dorado" to contraste(sobreDorado, p.gold),
            "carta" to contraste(p.letterInk, p.letter),
        ).forEach { (par, v) -> assertTrue("$nombre $par: %.2f".format(v), v >= 4.5) }
    }

    @Test fun `modo dia cumple contraste`() = revisar("día", DayPalette, Color(0xFFFFF4E8), Color(0xFFFFF8EC))
    @Test fun `modo noche cumple contraste`() = revisar("noche", NightPalette, Color(0xFF2A1408), Color(0xFF2A1F00))

    @Test fun `ningun texto mide menos de 12 sp`() {
        val pequenos = File("src/main/java/com/timecapsule/app").walkTopDown().filter { it.extension == "kt" }
            .flatMap { f -> Regex("""fontSize = (\d+(?:\.\d+)?)\.sp""").findAll(f.readText()).map { f.name to it.groupValues[1].toDouble() } }
            .filter { it.second < 12 }.toList()
        assertEquals(emptyList<Pair<String, Double>>(), pequenos)
    }
}
