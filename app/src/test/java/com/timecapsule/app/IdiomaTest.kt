package com.timecapsule.app

import com.timecapsule.app.ui.Idioma
import com.timecapsule.app.ui.TRADUCCIONES
import com.timecapsule.app.ui.Traductor
import com.timecapsule.app.ui.tr
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class IdiomaTest {
    private val t = Traductor(mapOf("Guardar" to "Save", "Lugar" to "Place", "Carta de {}" to "Letter from {}", "{#} años" to "{#} years", "Ve a {} para abrirla" to "Go to {} to open it"))

    @Test fun `por defecto la app esta en espanol`() {
        assertEquals("es", Idioma.codigo)
        assertEquals("Guardar", tr("Guardar"))
    }

    @Test fun `traduce textos exactos y en mayusculas`() {
        assertEquals("Save", t.traducir("Guardar"))
        assertEquals("SAVE", t.traducir("GUARDAR"))
    }

    @Test fun `traduce el dato cuando lo conoce y lo respeta cuando no`() {
        assertEquals("Go to Place to open it", t.traducir("Ve a Lugar para abrirla"))
        assertEquals("Letter from Lucia", t.traducir("Carta de Lucia"))
    }

    @Test fun `el hueco numerico no toca frases de una persona`() {
        assertEquals("3 years", t.traducir("3 años"))
        assertEquals("muchos años", t.traducir("muchos años"))
    }

    @Test fun `una carta larga nunca se toca`() {
        val carta = "Carta de " + "mucho amor ".repeat(40)
        assertEquals(carta, t.traducir(carta))
    }

    @Test fun `las plantillas de carta conservan sus saltos de linea`() {
        val es = "Amigos:\n\nEsta cápsula es para que nunca olvidemos…\n\nMi recuerdo favorito con ustedes es…\n"
        assertTrue(Traductor(TRADUCCIONES).traducir(es).startsWith("Friends:\n\n"))
    }

    @Test fun `el diccionario tiene los huecos parejos`() {
        val hueco = Regex("\\{#?\\}")
        TRADUCCIONES.forEach { (es, en) ->
            assertEquals(es, hueco.findAll(es).count(), hueco.findAll(en).count())
            assertTrue(es, en.isNotBlank())
        }
    }

    /** Si alguien agrega un Text("…") nuevo y olvida traducirlo, esta prueba avisa. */
    @Test fun `todos los textos fijos de la app estan traducidos`() {
        val llamada = Regex("""\b(?:Text|Ayuda)\("((?:[^"\\$]|\\.)+)"""")
        val iguales = setOf("Normal", "Time Capsule", "Video", "1 km", "Hotel", "Bar", "Café", "No", "Vivir mi vida · Marc Anthony",
            "Con esta clave la abres cuando quieras. Se guarda cifrada: si la olvidas, nadie puede recuperarla, ")
        val t = Traductor(TRADUCCIONES)
        val faltan = File("src/main/java/com/timecapsule/app").walkTopDown().filter { it.extension == "kt" && it.name != "Traducciones.kt" }
            .flatMap { f -> llamada.findAll(f.readText()).map { it.groupValues[1].replace("\\n", "\n") }.toList() }
            .filter { it.count(Char::isLetter) >= 2 && '%' !in it && it !in iguales && t.traducir(it) == it }
            .toList()
        assertEquals(emptyList<String>(), faltan.distinct())
    }
}
