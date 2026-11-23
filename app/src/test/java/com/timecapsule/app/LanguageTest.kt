package com.timecapsule.app

import com.timecapsule.app.ui.Language
import com.timecapsule.app.ui.TRANSLATIONS
import com.timecapsule.app.ui.Translator
import com.timecapsule.app.ui.tr
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LanguageTest {
    private val t = Translator(mapOf("Guardar" to "Save", "Lugar" to "Place", "Carta de {}" to "Letter from {}", "{#} años" to "{#} years", "Ve a {} para abrirla" to "Go to {} to open it"))

    @Test fun `por defecto la app esta en espanol`() {
        assertEquals("es", Language.code)
        assertEquals("Guardar", tr("Guardar"))
    }

    @Test fun `traduce textos exactos y en mayusculas`() {
        assertEquals("Save", t.translate("Guardar"))
        assertEquals("SAVE", t.translate("GUARDAR"))
    }

    @Test fun `traduce el dato cuando lo conoce y lo respeta cuando no`() {
        assertEquals("Go to Place to open it", t.translate("Ve a Lugar para abrirla"))
        assertEquals("Letter from Lucia", t.translate("Carta de Lucia"))
    }

    @Test fun `el hueco numerico no toca frases de una persona`() {
        assertEquals("3 years", t.translate("3 años"))
        assertEquals("muchos años", t.translate("muchos años"))
    }

    @Test fun `tambien traduce textos de una sola letra`() {
        assertEquals("— or —", Translator(TRANSLATIONS).translate("— o —"))
    }

    @Test fun `el icono se conserva y se traduce el nombre`() {
        assertEquals("⚙️ Save", t.translate("⚙️ Guardar"))
    }

    @Test fun `una carta larga nunca se toca`() {
        val letter = "Carta de " + "mucho amor ".repeat(40)
        assertEquals(letter, t.translate(letter))
    }

    @Test fun `las plantillas de carta conservan sus saltos de linea`() {
        val es = "Amigos:\n\nEsta cápsula es para que nunca olvidemos…\n\nMi recuerdo favorito con ustedes es…\n"
        assertTrue(Translator(TRANSLATIONS).translate(es).startsWith("Friends:\n\n"))
    }

    @Test fun `el diccionario tiene los huecos parejos`() {
        val placeholder = Regex("\\{#?\\}")
        TRANSLATIONS.forEach { (es, en) ->
            assertEquals(es, placeholder.findAll(es).count(), placeholder.findAll(en).count())
            assertTrue(es, en.isNotBlank())
        }
    }

    /** Si alguien agrega un Text("…") nuevo y olvida traducirlo, esta prueba avisa. */
    @Test fun `todos los textos fijos de la app estan traducidos`() {
        val call = Regex("""\b(?:Text|Ayuda)\("((?:[^"\\$]|\\.)+)"""")
        val equal = setOf("Normal", "Time Capsule", "Video", "1 km", "Hotel", "Bar", "Café", "No", "Vivir mi vida · Marc Anthony",
            "Con esta clave la abres cuando quieras. Se guarda cifrada: si la olvidas, nadie puede recuperarla, ")
        val t = Translator(TRANSLATIONS)
        val missingItems = File("src/main/java/com/timecapsule/app").walkTopDown().filter { it.extension == "kt" && it.name != "Translations.kt" }
            .flatMap { f -> call.findAll(f.readText()).map { it.groupValues[1].replace("\\n", "\n") }.toList() }
            .filter { it.count(Char::isLetter) >= 2 && '%' !in it && it !in equal && t.translate(it) == it }
            .toList()
        assertEquals(emptyList<String>(), missingItems.distinct())
    }
}
