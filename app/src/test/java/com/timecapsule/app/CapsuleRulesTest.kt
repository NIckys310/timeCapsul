package com.timecapsule.app

import com.timecapsule.app.data.CapsuleForm
import com.timecapsule.app.data.CapsuleRules
import com.timecapsule.app.util.NearbyPlace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/** Las reglas para sellar una cápsula nueva. */
class CapsuleRulesTest {
    private val now = Instant.parse("2026-10-16T12:00:00Z")
    private val good = CapsuleForm(
        title = "Grado 2026", letter = "Querido yo…", useDate = true, opensOn = now.plus(365, ChronoUnit.DAYS),
        usePlace = false,
    )
    private fun error(f: CapsuleForm) = CapsuleRules.validate(f, now)

    @Test fun `una capsula completa se puede sellar`() = assertNull(error(good))
    @Test fun `sin titulo no se puede`() = assertEquals("Ponle un título a tu cápsula.", error(good.copy(title = "  ")))
    @Test fun `sin carta ni fotos no se puede`() = assertEquals("Escribe una carta o agrega fotos o videos.", error(good.copy(letter = "")))
    @Test fun `solo con fotos (sin carta) si se puede`() = assertNull(error(good.copy(letter = "", media = 3)))

    @Test fun `necesita fecha o abrirse solo en el lugar`() {
        assertEquals("Elige una fecha o activa “Solo se abre en este lugar”.", error(good.copy(useDate = false)))
        assertNull(error(good.copy(useDate = false, usePlace = true, onlyAtPlace = true, hasLocation = true)))
    }

    @Test fun `si guarda el lugar hay que esperar la ubicacion`() =
        assertEquals("Espera a que se registre tu ubicación o apaga “Guardar el lugar”.", error(good.copy(usePlace = true, hasLocation = false)))

    @Test fun `la fecha debe ser en el futuro`() =
        assertEquals("La fecha de apertura debe ser en el futuro.", error(good.copy(opensOn = now.minusSeconds(60))))

    @Test fun `sin fecha marcada no importa la fecha vieja`() =
        assertNull(error(good.copy(useDate = false, opensOn = now.minusSeconds(60), usePlace = true, onlyAtPlace = true, hasLocation = true)))

    @Test fun `la clave debe tener al menos 4 caracteres`() =
        assertEquals("La clave debe tener al menos 4 caracteres.", error(good.copy(useKey = true, key = " 12 ", repeatedKey = " 12 ")))

    @Test fun `las dos claves deben coincidir`() =
        assertEquals("Las dos claves no coinciden.", error(good.copy(useKey = true, key = "secreto", repeatedKey = "secreta")))

    @Test fun `con clave correcta se puede sellar`() = assertNull(error(good.copy(useKey = true, key = "luna2026", repeatedKey = "luna2026")))

    @Test fun `se revisa primero el titulo (el error mas importante)`() =
        assertEquals("Ponle un título a tu cápsula.", error(CapsuleForm()))

    @Test fun `maximo 20 fotos y 1 video`() {
        assertTrue(CapsuleRules.fits("photo", photos = 19, videos = 0))
        assertFalse(CapsuleRules.fits("photo", photos = 20, videos = 0))
        assertTrue(CapsuleRules.fits("video", photos = 20, videos = 0))
        assertFalse(CapsuleRules.fits("video", photos = 0, videos = 1))
        assertFalse(CapsuleRules.fits("audio", photos = 0, videos = 0))
    }

    @Test fun `los correos se separan con coma, punto y coma, espacios o lineas`() =
        assertEquals(listOf("mama@correo.com", "amigo@correo.com", "tia@x.co"),
            CapsuleRules.emails(" mama@correo.com, amigo@correo.com;tia@x.co\n"))

    @Test fun `los correos invalidos o repetidos se ignoran`() =
        assertEquals(listOf("a@b.co"), CapsuleRules.emails("hola, @, x@, a@b.co a@b.co"))

    @Test fun `encuentra el enlace de YouTube o Spotify en lo copiado`() {
        assertEquals("https://youtu.be/abc123", CapsuleRules.songLinkOf("Mira esto https://youtu.be/abc123 ❤️"))
        assertEquals("https://open.spotify.com/track/9", CapsuleRules.songLinkOf("https://open.spotify.com/track/9"))
        assertNull(CapsuleRules.songLinkOf("https://google.com"))
        assertNull(CapsuleRules.songLinkOf(null))
    }

    @Test fun `el lugar sugerido es el sitio donde estas o la calle`() {
        val near = NearbyPlace("Parque Lleras", "parque", 20, 6.2, -75.5)
        val far = NearbyPlace("Museo", "museo", 300, 6.2, -75.5)
        assertEquals("Parque Lleras", CapsuleRules.suggestedPlace(listOf(far, near), "Calle 10 #40, Medellín"))
        assertEquals("Calle 10 #40", CapsuleRules.suggestedPlace(listOf(far), "Calle 10 #40, Medellín"))
        assertEquals("", CapsuleRules.suggestedPlace(emptyList(), null))
    }

    @Test fun `las fechas rapidas van de 1 mes a 10 anos`() {
        assertEquals(listOf("1 mes", "6 meses", "1 año", "5 años", "10 años"), CapsuleRules.dateOptions.map { it.first })
        val since = ZonedDateTime.of(2026, 10, 16, 12, 0, 0, 0, ZoneOffset.UTC)
        assertEquals(Instant.parse("2027-10-16T12:00:00Z"), CapsuleRules.dateIn(12, since))
    }

    @Test fun `la fecha rapida se marca con 2 minutos de margen`() {
        assertTrue(CapsuleRules.sameDate(now.plusSeconds(90), now))
        assertFalse(CapsuleRules.sameDate(now.plusSeconds(300), now))
        assertFalse(CapsuleRules.sameDate(null, now))
    }

    @Test fun `la plantilla reemplaza una carta vacia o se pone arriba`() {
        assertEquals("Querido yo:", CapsuleRules.withTemplate("  ", "Querido yo:"))
        assertEquals("Querido yo:\n\nHola", CapsuleRules.withTemplate("Hola", "Querido yo:"))
    }

    @Test fun `las preguntas no se repiten y son maximo 5`() {
        var p = listOf<String>()
        p = CapsuleRules.addQuestion(p, " ¿Sigues aquí? ")
        p = CapsuleRules.addQuestion(p, "¿Sigues aquí?")
        p = CapsuleRules.addQuestion(p, "   ")
        assertEquals(listOf("¿Sigues aquí?"), p)
        repeat(10) { p = CapsuleRules.addQuestion(p, "Pregunta $it") }
        assertEquals(CapsuleRules.MAX_QUESTIONS, p.size)
    }
}
