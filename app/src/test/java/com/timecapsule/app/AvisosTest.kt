package com.timecapsule.app

import com.timecapsule.app.data.Activity
import com.timecapsule.app.notify.Avisos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration

/** Textos y reglas de las notificaciones. */
class AvisosTest {
    @Test fun `faltando 10 dias se programan los 3 avisos de cuenta regresiva`() {
        val r = Avisos.recordatorios(Duration.ofDays(10))
        assertEquals(listOf("1 semana", "1 día", "1 hora"), r.map { it.second })
        assertEquals(Duration.ofDays(3), r[0].first)
        assertEquals(Duration.ofDays(9), r[1].first)
        assertEquals(Duration.ofDays(10).minusHours(1), r[2].first)
    }

    @Test fun `faltando 2 dias ya no se avisa la semana`() =
        assertEquals(listOf("1 día", "1 hora"), Avisos.recordatorios(Duration.ofDays(2)).map { it.second })

    @Test fun `faltando 30 minutos no hay cuenta regresiva`() = assertTrue(Avisos.recordatorios(Duration.ofMinutes(30)).isEmpty())

    @Test fun `si la fecha ya paso no se programa nada`() = assertTrue(Avisos.recordatorios(Duration.ofDays(-1)).isEmpty())

    @Test fun `justo a una hora se avisa en ese momento`() =
        assertEquals(listOf(Duration.ZERO to "1 hora"), Avisos.recordatorios(Duration.ofHours(1)))

    @Test fun `si solo se abre en un lugar el aviso dice a donde ir`() {
        assertEquals("Ya llegó la fecha. Ve a la playa para abrirla.", Avisos.textoApertura(Avisos.lugarParaAbrir(Capsulas.nueva(exigeLugar = true, nombreLugar = "la playa"))))
        assertEquals("su lugar", Avisos.lugarParaAbrir(Capsulas.nueva(exigeLugar = true)))
        assertNull(Avisos.lugarParaAbrir(Capsulas.nueva(lugar = true)))
        assertEquals("Ya puedes abrirla y revivir lo que guardaste.", Avisos.textoApertura(null))
    }

    @Test fun `titulos de apertura y cuenta regresiva`() {
        assertEquals("¡“Grado” se puede abrir!", Avisos.tituloApertura("Grado"))
        assertEquals("⏳ Falta 1 día", Avisos.tituloRecordatorio("1 día"))
        assertEquals("Tu cápsula “Grado” se abrirá pronto.", Avisos.textoRecordatorio("Grado"))
    }

    @Test fun `solo avisa cápsulas compartidas nuevas y que no son mias`() {
        val mia = Capsulas.nueva().copy(id = "a", owner = "yo")
        val conocida = Capsulas.nueva().copy(id = "b", owner = "ana")
        val nueva = Capsulas.nueva().copy(id = "c", owner = "ana", ownerName = "Ana")
        assertEquals(listOf("c"), Avisos.nuevasCompartidas(listOf(mia, conocida, nueva), setOf("b"), "yo").map { it.id })
    }

    @Test fun `el aviso de compartida dice quien y si ya se puede ver`() {
        val c = Capsulas.nueva().copy(ownerName = "Ana", title = "Viaje")
        assertEquals("Ana compartió una cápsula contigo", Avisos.tituloCompartida(c))
        assertEquals("“Viaje”. Te avisaremos cuando se abra.", Avisos.textoCompartida(c))
        assertEquals("“Viaje”. Ya puedes verla.", Avisos.textoCompartida(c.copy(isOpen = true)))
        assertEquals("Alguien compartió una cápsula contigo", Avisos.tituloCompartida(c.copy(ownerName = null)))
    }

    @Test fun `comentarios y reacciones`() {
        fun a(kind: String, who: String?, body: String) = Activity(kind, "c-1", "Viaje", who, body, "2026-10-18T12:00:00Z")
        assertEquals("Ana comentó: ¡qué bonito!", Avisos.textoActividad(a("comment", "Ana", "¡qué bonito!")))
        assertEquals("Leo reaccionó ❤️", Avisos.textoActividad(a("reaction", "Leo", "❤️")))
        assertEquals("Alguien reaccionó 😂", Avisos.textoActividad(a("reaction", null, "😂")))
    }

    @Test fun `maximo 5 avisos de actividad por revision`() = assertEquals(5, Avisos.MAX_ACTIVIDAD)
}
