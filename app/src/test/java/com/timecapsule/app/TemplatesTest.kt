package com.timecapsule.app

import com.timecapsule.app.data.LetterTemplates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Plantillas de carta y preguntas para el futuro. */
class TemplatesTest {
    @Test fun `hay 6 plantillas de carta`() = assertEquals(6, LetterTemplates.all.size)

    @Test fun `las plantillas no repiten identificador`() {
        val ids = LetterTemplates.all.map { it.first }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test fun `cada plantilla tiene nombre y un inicio para completar`() {
        for ((id, template) in LetterTemplates.all) {
            val (name, text) = template
            assertTrue(id, name.isNotBlank())
            assertTrue(id, text.lines().size >= 3 && "…" in text)
        }
    }

    @Test fun `las plantillas tienen saltos de linea de verdad (no la letra n)`() {
        for ((id, p) in LetterTemplates.all) assertTrue(id, "\\n" !in p.second && "\n" in p.second)
    }

    @Test fun `las preguntas para el futuro son preguntas`() {
        assertTrue(LetterTemplates.questions.size >= 5)
        for (q in LetterTemplates.questions) assertTrue(q, q.startsWith("¿") && q.endsWith("?"))
    }
}
