package com.timecapsule.app

import com.timecapsule.app.data.PasswordRules
import com.timecapsule.app.ui.TRANSLATIONS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PasswordTest {
    @Test fun `una contrasena fuerte pasa todas las reglas`() {
        assertTrue(PasswordRules.isSafe("Capsula#2026"))
    }

    @Test fun `una contrasena debil dice exactamente que le falta`() {
        assertEquals(listOf("Al menos 10 caracteres", "Una letra mayúscula", "Un número", "Un símbolo (!@#$…)"), PasswordRules.missingItems("hola"))
        assertFalse(PasswordRules.isSafe("abcdefghij"))
    }

    @Test fun `no acepta contrasenas faciles de adivinar`() {
        assertFalse(PasswordRules.isSafe("Password#2026"))
        assertFalse(PasswordRules.isSafe("123Abcdef!x"))
    }

    @Test fun `cada regla tiene traduccion al ingles`() {
        (PasswordRules.rules.map { it.text } + PasswordRules.missingItems("123")).forEach { assertTrue(it, it in TRANSLATIONS) }
    }

    @Test fun `la app nunca guarda la contrasena en el celular`() {
        val code = File("src/main/java").walkTopDown().filter { it.extension == "kt" }.joinToString("\n") { it.readText() }
        assertFalse(Regex("""put(String)?\(\s*"(pass|password|contrasena|contraseña)""", RegexOption.IGNORE_CASE).containsMatchIn(code))
    }
}
