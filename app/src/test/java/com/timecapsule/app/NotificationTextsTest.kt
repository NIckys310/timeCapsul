package com.timecapsule.app

import com.timecapsule.app.data.Activity
import com.timecapsule.app.notify.NotificationTexts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration

/** Textos y reglas de las notificaciones. */
class NotificationTextsTest {
    @Test fun `faltando 10 dias se programan los 3 avisos de cuenta regresiva`() {
        val r = NotificationTexts.reminders(Duration.ofDays(10))
        assertEquals(listOf("1 semana", "1 día", "1 hora"), r.map { it.second })
        assertEquals(Duration.ofDays(3), r[0].first)
        assertEquals(Duration.ofDays(9), r[1].first)
        assertEquals(Duration.ofDays(10).minusHours(1), r[2].first)
    }

    @Test fun `faltando 2 dias ya no se avisa la semana`() =
        assertEquals(listOf("1 día", "1 hora"), NotificationTexts.reminders(Duration.ofDays(2)).map { it.second })

    @Test fun `faltando 30 minutos no hay cuenta regresiva`() = assertTrue(NotificationTexts.reminders(Duration.ofMinutes(30)).isEmpty())

    @Test fun `si la fecha ya paso no se programa nada`() = assertTrue(NotificationTexts.reminders(Duration.ofDays(-1)).isEmpty())

    @Test fun `justo a una hora se avisa en ese momento`() =
        assertEquals(listOf(Duration.ZERO to "1 hora"), NotificationTexts.reminders(Duration.ofHours(1)))

    @Test fun `si solo se abre en un lugar el aviso dice a donde ir`() {
        assertEquals("Ya llegó la fecha. Ve a la playa para abrirla.", NotificationTexts.openingText(NotificationTexts.placeToOpen(SampleCapsules.newOne(requiresPlace = true, placeNameText = "la playa"))))
        assertEquals("su lugar", NotificationTexts.placeToOpen(SampleCapsules.newOne(requiresPlace = true)))
        assertNull(NotificationTexts.placeToOpen(SampleCapsules.newOne(place = true)))
        assertEquals("Ya puedes abrirla y revivir lo que guardaste.", NotificationTexts.openingText(null))
    }

    @Test fun `titulos de apertura y cuenta regresiva`() {
        assertEquals("¡“Grado” se puede abrir!", NotificationTexts.openingTitle("Grado"))
        assertEquals("⏳ Falta 1 día", NotificationTexts.reminderTitle("1 día"))
        assertEquals("Tu cápsula “Grado” se abrirá pronto.", NotificationTexts.reminderText("Grado"))
    }

    @Test fun `solo avisa cápsulas compartidas nuevas y que no son mias`() {
        val mine = SampleCapsules.newOne().copy(id = "a", owner = "yo")
        val knownOne = SampleCapsules.newOne().copy(id = "b", owner = "ana")
        val newOne = SampleCapsules.newOne().copy(id = "c", owner = "ana", ownerName = "Ana")
        assertEquals(listOf("c"), NotificationTexts.newShared(listOf(mine, knownOne, newOne), setOf("b"), "yo").map { it.id })
    }

    @Test fun `el aviso de compartida dice quien y si ya se puede ver`() {
        val c = SampleCapsules.newOne().copy(ownerName = "Ana", title = "Viaje")
        assertEquals("Ana compartió una cápsula contigo", NotificationTexts.sharedTitle(c))
        assertEquals("“Viaje”. Te avisaremos cuando se abra.", NotificationTexts.sharedText(c))
        assertEquals("“Viaje”. Ya puedes verla.", NotificationTexts.sharedText(c.copy(isOpen = true)))
        assertEquals("Alguien compartió una cápsula contigo", NotificationTexts.sharedTitle(c.copy(ownerName = null)))
    }

    @Test fun `comentarios y reacciones`() {
        fun a(kind: String, who: String?, body: String) = Activity(kind, "c-1", "Viaje", who, body, "2026-10-18T12:00:00Z")
        assertEquals("Ana comentó: ¡qué bonito!", NotificationTexts.activityText(a("comment", "Ana", "¡qué bonito!")))
        assertEquals("Leo reaccionó ❤️", NotificationTexts.activityText(a("reaction", "Leo", "❤️")))
        assertEquals("Alguien reaccionó 😂", NotificationTexts.activityText(a("reaction", null, "😂")))
    }

    @Test fun `maximo 5 avisos de actividad por revision`() = assertEquals(5, NotificationTexts.MAX_ACTIVITY)
}
