package com.timecapsule.app

import com.timecapsule.app.data.FormularioCapsula
import com.timecapsule.app.data.ReglasCapsula
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
class ReglasCapsulaTest {
    private val ahora = Instant.parse("2026-10-16T12:00:00Z")
    private val bien = FormularioCapsula(
        titulo = "Grado 2026", carta = "Querido yo…", usarFecha = true, abreEl = ahora.plus(365, ChronoUnit.DAYS),
        usarLugar = false,
    )
    private fun error(f: FormularioCapsula) = ReglasCapsula.validar(f, ahora)

    @Test fun `una capsula completa se puede sellar`() = assertNull(error(bien))
    @Test fun `sin titulo no se puede`() = assertEquals("Ponle un título a tu cápsula.", error(bien.copy(titulo = "  ")))
    @Test fun `sin carta ni fotos no se puede`() = assertEquals("Escribe una carta o agrega fotos o videos.", error(bien.copy(carta = "")))
    @Test fun `solo con fotos (sin carta) si se puede`() = assertNull(error(bien.copy(carta = "", medios = 3)))

    @Test fun `necesita fecha o abrirse solo en el lugar`() {
        assertEquals("Elige una fecha o activa “Solo se abre en este lugar”.", error(bien.copy(usarFecha = false)))
        assertNull(error(bien.copy(usarFecha = false, usarLugar = true, soloEnElLugar = true, tieneUbicacion = true)))
    }

    @Test fun `si guarda el lugar hay que esperar la ubicacion`() =
        assertEquals("Espera a que se registre tu ubicación o apaga “Guardar el lugar”.", error(bien.copy(usarLugar = true, tieneUbicacion = false)))

    @Test fun `la fecha debe ser en el futuro`() =
        assertEquals("La fecha de apertura debe ser en el futuro.", error(bien.copy(abreEl = ahora.minusSeconds(60))))

    @Test fun `sin fecha marcada no importa la fecha vieja`() =
        assertNull(error(bien.copy(usarFecha = false, abreEl = ahora.minusSeconds(60), usarLugar = true, soloEnElLugar = true, tieneUbicacion = true)))

    @Test fun `la clave debe tener al menos 4 caracteres`() =
        assertEquals("La clave debe tener al menos 4 caracteres.", error(bien.copy(usarClave = true, clave = " 12 ", claveRepetida = " 12 ")))

    @Test fun `las dos claves deben coincidir`() =
        assertEquals("Las dos claves no coinciden.", error(bien.copy(usarClave = true, clave = "secreto", claveRepetida = "secreta")))

    @Test fun `con clave correcta se puede sellar`() = assertNull(error(bien.copy(usarClave = true, clave = "luna2026", claveRepetida = "luna2026")))

    @Test fun `se revisa primero el titulo (el error mas importante)`() =
        assertEquals("Ponle un título a tu cápsula.", error(FormularioCapsula()))

    @Test fun `maximo 20 fotos y 1 video`() {
        assertTrue(ReglasCapsula.cabe("photo", fotos = 19, videos = 0))
        assertFalse(ReglasCapsula.cabe("photo", fotos = 20, videos = 0))
        assertTrue(ReglasCapsula.cabe("video", fotos = 20, videos = 0))
        assertFalse(ReglasCapsula.cabe("video", fotos = 0, videos = 1))
        assertFalse(ReglasCapsula.cabe("audio", fotos = 0, videos = 0))
    }

    @Test fun `los correos se separan con coma, punto y coma, espacios o lineas`() =
        assertEquals(listOf("mama@correo.com", "amigo@correo.com", "tia@x.co"),
            ReglasCapsula.correos(" mama@correo.com, amigo@correo.com;tia@x.co\n"))

    @Test fun `los correos invalidos o repetidos se ignoran`() =
        assertEquals(listOf("a@b.co"), ReglasCapsula.correos("hola, @, x@, a@b.co a@b.co"))

    @Test fun `encuentra el enlace de YouTube o Spotify en lo copiado`() {
        assertEquals("https://youtu.be/abc123", ReglasCapsula.enlaceDeCancion("Mira esto https://youtu.be/abc123 ❤️"))
        assertEquals("https://open.spotify.com/track/9", ReglasCapsula.enlaceDeCancion("https://open.spotify.com/track/9"))
        assertNull(ReglasCapsula.enlaceDeCancion("https://google.com"))
        assertNull(ReglasCapsula.enlaceDeCancion(null))
    }

    @Test fun `el lugar sugerido es el sitio donde estas o la calle`() {
        val cerca = NearbyPlace("Parque Lleras", "parque", 20, 6.2, -75.5)
        val lejos = NearbyPlace("Museo", "museo", 300, 6.2, -75.5)
        assertEquals("Parque Lleras", ReglasCapsula.lugarSugerido(listOf(lejos, cerca), "Calle 10 #40, Medellín"))
        assertEquals("Calle 10 #40", ReglasCapsula.lugarSugerido(listOf(lejos), "Calle 10 #40, Medellín"))
        assertEquals("", ReglasCapsula.lugarSugerido(emptyList(), null))
    }

    @Test fun `las fechas rapidas van de 1 mes a 10 anos`() {
        assertEquals(listOf("1 mes", "6 meses", "1 año", "5 años", "10 años"), ReglasCapsula.opcionesDeFecha.map { it.first })
        val desde = ZonedDateTime.of(2026, 10, 16, 12, 0, 0, 0, ZoneOffset.UTC)
        assertEquals(Instant.parse("2027-10-16T12:00:00Z"), ReglasCapsula.fechaEn(12, desde))
    }

    @Test fun `la fecha rapida se marca con 2 minutos de margen`() {
        assertTrue(ReglasCapsula.mismaFecha(ahora.plusSeconds(90), ahora))
        assertFalse(ReglasCapsula.mismaFecha(ahora.plusSeconds(300), ahora))
        assertFalse(ReglasCapsula.mismaFecha(null, ahora))
    }

    @Test fun `la plantilla reemplaza una carta vacia o se pone arriba`() {
        assertEquals("Querido yo:", ReglasCapsula.conPlantilla("  ", "Querido yo:"))
        assertEquals("Querido yo:\n\nHola", ReglasCapsula.conPlantilla("Hola", "Querido yo:"))
    }

    @Test fun `las preguntas no se repiten y son maximo 5`() {
        var p = listOf<String>()
        p = ReglasCapsula.agregarPregunta(p, " ¿Sigues aquí? ")
        p = ReglasCapsula.agregarPregunta(p, "¿Sigues aquí?")
        p = ReglasCapsula.agregarPregunta(p, "   ")
        assertEquals(listOf("¿Sigues aquí?"), p)
        repeat(10) { p = ReglasCapsula.agregarPregunta(p, "Pregunta $it") }
        assertEquals(ReglasCapsula.MAX_PREGUNTAS, p.size)
    }
}
