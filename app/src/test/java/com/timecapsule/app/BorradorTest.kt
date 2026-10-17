package com.timecapsule.app

import com.timecapsule.app.data.SavedDraft
import com.timecapsule.app.data.SavedItem
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** El borrador que evita perder la cápsula al salir a Spotify o YouTube. */
class BorradorTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test fun `un borrador nuevo esta vacio`() = assertTrue(SavedDraft().isEmpty)

    @Test fun `con titulo, carta, fotos, notas de voz, cancion o preguntas ya no esta vacio`() {
        assertFalse(SavedDraft(title = "Hola").isEmpty)
        assertFalse(SavedDraft(letter = "Querido yo").isEmpty)
        assertFalse(SavedDraft(items = listOf(SavedItem("content://foto", "image"))).isEmpty)
        assertFalse(SavedDraft(voiceNotes = listOf("/nota.m4a")).isEmpty)
        assertFalse(SavedDraft(songUrl = "https://open.spotify.com/track/1").isEmpty)
        assertFalse(SavedDraft(questions = listOf("¿Sigues aquí?")).isEmpty)
    }

    @Test fun `solo espacios en blanco cuenta como vacio`() = assertTrue(SavedDraft(title = "   ", letter = "\n").isEmpty)

    @Test fun `cambiar solo opciones (radio, fecha) no cuenta como contenido`() =
        assertTrue(SavedDraft(radius = 500, useDate = false, openTogether = true).isEmpty)

    @Test fun `se guarda y se recupera igual (al volver de Spotify)`() {
        val b = SavedDraft(
            title = "Viaje a Cartagena", letter = "Querido yo…", items = listOf(SavedItem("content://1", "image"), SavedItem("content://2", "video")),
            voiceNotes = listOf("/a.m4a"), unlockAtEpoch = 1_900_000_000_000, songTitle = "Canción", songUrl = "https://youtu.be/x",
            questions = listOf("¿Volviste?"), radius = 300, openTogether = true, routeId = "r-1",
        )
        val texto = json.encodeToString(SavedDraft.serializer(), b)
        assertEquals(b, json.decodeFromString(SavedDraft.serializer(), texto))
    }

    @Test fun `un borrador viejo con campos que ya no existen se sigue abriendo`() {
        val viejo = """{"title":"Hola","campo_que_ya_no_existe":true}"""
        assertEquals("Hola", json.decodeFromString(SavedDraft.serializer(), viejo).title)
    }

    @Test fun `la clave de la capsula nunca se guarda en el borrador`() {
        val campos = SavedDraft.serializer().descriptor.let { d -> (0 until d.elementsCount).map { d.getElementName(it).lowercase() } }
        assertFalse(campos.toString(), campos.any { "key" in it || "clave" in it || "password" in it || "pin" in it })
    }
}
