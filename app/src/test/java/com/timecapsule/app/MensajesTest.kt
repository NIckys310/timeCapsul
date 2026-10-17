package com.timecapsule.app

import com.timecapsule.app.data.Chapter
import com.timecapsule.app.data.Repo
import com.timecapsule.app.data.Timeline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Errores en español sencillo y la línea de tiempo guardada. */
class MensajesTest {
    private fun msg(texto: String?) = Repo.friendly(RuntimeException(texto))

    @Test fun `contrasena incorrecta`() = assertEquals("Correo o contraseña incorrectos.", msg("Invalid login credentials"))
    @Test fun `correo sin confirmar`() = assertEquals("Confirma tu correo con el enlace que te enviamos.", msg("Email not confirmed"))
    @Test fun `correo ya registrado`() = assertEquals("Ese correo ya tiene una cuenta. Inicia sesión.", msg("User already registered"))
    @Test fun `contrasena corta`() = assertEquals("La contraseña debe tener al menos 6 caracteres.", msg("Password should be at least 6 characters"))
    @Test fun `sin internet`() = assertEquals("Sin conexión a internet.", msg("Unable to resolve host \"x.supabase.co\""))
    @Test fun `archivo muy grande`() = assertEquals("Un archivo supera el límite de 50 MB.", msg("Payload too large"))
    @Test fun `demasiados intentos con la clave`() = assertEquals("Demasiados intentos. Espera 15 minutos para volver a probar.", msg("ERROR: Demasiados intentos"))

    @Test fun `el limite de la base se muestra en una sola linea`() =
        assertEquals("Máximo 20 fotos por cápsula", msg("P0001: Máximo 20 fotos por cápsula\nCONTEXT: PL/pgSQL"))

    @Test fun `un error corto desconocido se muestra tal cual`() = assertEquals("Algo raro", msg("Algo raro"))
    @Test fun `un error largo o vacio se cambia por un mensaje amable`() {
        assertEquals("Algo salió mal. Intenta de nuevo.", msg("x".repeat(500)))
        assertEquals("Algo salió mal. Intenta de nuevo.", msg(null))
    }

    @Test fun `la linea de tiempo se guarda y se recupera igual`() {
        val t = Timeline("Un año lleno de viajes", listOf(Chapter("Inicio", "Enero", "Todo empezó", "🌱", listOf("c-1"))))
        assertEquals(t, Repo.decodeTimeline(Repo.encodeTimeline(t)))
    }

    @Test fun `una linea de tiempo danada no rompe la app`() = assertNull(Repo.decodeTimeline("{roto"))
}
