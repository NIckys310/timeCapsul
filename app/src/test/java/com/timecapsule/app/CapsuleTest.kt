package com.timecapsule.app

import com.timecapsule.app.util.conditionText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** Cuándo y dónde se puede abrir una cápsula. */
class CapsuleTest {
    @Test fun `sin fecha la condicion de fecha ya se cumple`() = assertTrue(SampleCapsules.newOne().dateReady)

    @Test fun `con fecha futura todavia no se puede abrir`() = assertFalse(SampleCapsules.newOne(opensIn = SampleCapsules.inDays(10)).dateReady)

    @Test fun `con fecha pasada ya se puede abrir`() = assertTrue(SampleCapsules.newOne(opensIn = SampleCapsules.inDays(-1)).dateReady)

    @Test fun `entiende la fecha que manda Supabase`() =
        assertEquals(Instant.parse("2030-01-01T05:00:00Z"), SampleCapsules.newOne(opensIn = "2030-01-01T00:00:00-05:00").unlockInstant)

    @Test fun `una fecha danada no rompe la app`() {
        val c = SampleCapsules.newOne(opensIn = "no es fecha")
        assertNull(c.unlockInstant)
        assertTrue(c.dateReady)
    }

    @Test fun `tiene lugar solo si guardo coordenadas`() {
        assertTrue(SampleCapsules.newOne(place = true).hasPlace)
        assertFalse(SampleCapsules.newOne().hasPlace)
    }

    @Test fun `una capsula abierta dice Abierta`() = assertEquals("Abierta", SampleCapsules.newOne(openCapsule = true).conditionText())

    @Test fun `sin condiciones dice Cerrada`() = assertEquals("Cerrada", SampleCapsules.newOne().conditionText())

    @Test fun `con fecha futura dice cuando se abre y cuanto falta`() {
        val t = SampleCapsules.newOne(opensIn = SampleCapsules.inDays(400)).conditionText()
        assertTrue(t, t.startsWith("Se abre el") && t.contains("faltan") && t.contains("meses"))
    }

    @Test fun `si la fecha ya llego pero exige lugar, pide ir al lugar`() =
        assertEquals("Ve a Mi colegio para abrirla", SampleCapsules.newOne(requiresPlace = true, placeNameText = "Mi colegio").conditionText())

    @Test fun `fecha y lugar se combinan en una frase`() {
        val t = SampleCapsules.newOne(opensIn = SampleCapsules.inDays(30), requiresPlace = true, placeNameText = "la playa").conditionText()
        assertTrue(t, t.contains("y estando en la playa"))
    }

    @Test fun `sin nombre del lugar dice su lugar`() =
        assertEquals("Ve a su lugar para abrirla", SampleCapsules.newOne(requiresPlace = true).conditionText())

    @Test fun `avisa si se puede abrir antes con clave`() {
        val t = SampleCapsules.newOne(opensIn = SampleCapsules.inDays(30), key = true).conditionText()
        assertTrue(t, t.endsWith("· o con tu clave"))
    }
}
