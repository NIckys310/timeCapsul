package com.timecapsule.app

import com.timecapsule.app.data.ReglasContrasena
import com.timecapsule.app.ui.TRADUCCIONES
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ContrasenaTest {
    @Test fun `una contrasena fuerte pasa todas las reglas`() {
        assertTrue(ReglasContrasena.esSegura("Capsula#2026"))
    }

    @Test fun `una contrasena debil dice exactamente que le falta`() {
        assertEquals(listOf("Al menos 10 caracteres", "Una letra mayúscula", "Un número", "Un símbolo (!@#$…)"), ReglasContrasena.faltan("hola"))
        assertFalse(ReglasContrasena.esSegura("abcdefghij"))
    }

    @Test fun `no acepta contrasenas faciles de adivinar`() {
        assertFalse(ReglasContrasena.esSegura("Password#2026"))
        assertFalse(ReglasContrasena.esSegura("123Abcdef!x"))
    }

    @Test fun `cada regla tiene traduccion al ingles`() {
        (ReglasContrasena.reglas.map { it.texto } + ReglasContrasena.faltan("123")).forEach { assertTrue(it, it in TRADUCCIONES) }
    }

    @Test fun `la app nunca guarda la contrasena en el celular`() {
        val codigo = File("src/main/java").walkTopDown().filter { it.extension == "kt" }.joinToString("\n") { it.readText() }
        assertFalse(Regex("""put(String)?\(\s*"(pass|password|contrasena|contraseña)""", RegexOption.IGNORE_CASE).containsMatchIn(codigo))
    }
}
