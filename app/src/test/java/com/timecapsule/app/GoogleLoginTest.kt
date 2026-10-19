package com.timecapsule.app

import com.timecapsule.app.data.Config
import com.timecapsule.app.data.GoogleLogin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Continuar con Google: la huella del nonce y que no haya secretos en la app. */
class GoogleLoginTest {
    @Test fun `la huella del nonce es SHA-256 en hexadecimal`() =
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", GoogleLogin.huella("abc"))

    @Test fun `nonces distintos dan huellas distintas`() = assertNotEquals(GoogleLogin.huella("a"), GoogleLogin.huella("b"))

    @Test fun `el ID de cliente es el web de Google (o vacio mientras no se configura)`() {
        val id = Config.GOOGLE_WEB_CLIENT_ID
        assertTrue(id, id.isEmpty() || id.endsWith(".apps.googleusercontent.com"))
    }

    @Test fun `el boton aparece solo si el ID esta configurado`() =
        assertEquals(Config.GOOGLE_WEB_CLIENT_ID.isNotBlank(), GoogleLogin.disponible())
}
