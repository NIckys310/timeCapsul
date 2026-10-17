package com.timecapsule.app

import com.timecapsule.app.util.fmtDate
import com.timecapsule.app.util.fmtMonth
import com.timecapsule.app.util.humanUntil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

/** Textos de tiempo: "faltan 3 años", "5 días", fechas en español. */
class FormatoTest {
    private val ahora = Instant.parse("2026-10-10T12:00:00Z")
    private fun falta(d: Duration) = humanUntil(ahora.plus(d), ahora)

    @Test fun `si ya llego la fecha dice ya`() {
        assertEquals("ya", humanUntil(ahora, ahora))
        assertEquals("ya", falta(Duration.ofDays(-3)))
    }

    @Test fun `menos de una hora en minutos`() = assertEquals("25 min", falta(Duration.ofMinutes(25)))
    @Test fun `nunca dice 0 minutos`() = assertEquals("1 min", falta(Duration.ofSeconds(20)))
    @Test fun `horas y minutos`() = assertEquals("5 h 30 min", falta(Duration.ofMinutes(330)))
    @Test fun `un dia todavia se cuenta en horas`() = assertEquals("30 h 0 min", falta(Duration.ofHours(30)))
    @Test fun `dias`() = assertEquals("12 días", falta(Duration.ofDays(12)))
    @Test fun `meses`() = assertEquals("4 meses", falta(Duration.ofDays(125)))
    @Test fun `anos`() = assertEquals("5 años", falta(Duration.ofDays(5 * 365 + 10)))

    @Test fun `la fecha se escribe en espanol con el ano`() {
        val t = Instant.parse("2030-03-15T15:00:00Z").fmtDate()
        assertTrue(t, t.contains("2030") && t.contains("mar"))
    }

    @Test fun `el mes empieza en mayuscula`() {
        val m = Instant.parse("2030-03-15T15:00:00Z").fmtMonth()
        assertTrue(m, m.startsWith("Marzo") && m.endsWith("2030"))
    }
}
