package com.timecapsule.app

import com.timecapsule.app.util.ErrorLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ErrorLogTest {
    @Test fun `sin iniciar no se cae`() {
        ErrorLog.record("prueba", IllegalStateException("x"))
    }

    @Test fun `guarda donde paso y el mensaje, el mas reciente primero`() {
        val f = File.createTempFile("fallos", ".log").apply { delete() }
        ErrorLog.startIn(f)
        ErrorLog.record("primero", IllegalStateException("uno"))
        ErrorLog.record("segundo", IllegalArgumentException("dos"))
        val r = ErrorLog.recent()
        assertTrue(r[0].contains("segundo") && r[0].contains("IllegalArgumentException: dos"))
        assertTrue(r[1].contains("primero"))
    }

    @Test fun `solo guarda los ultimos 60`() {
        val f = File.createTempFile("fallos", ".log").apply { delete() }
        ErrorLog.startIn(f)
        repeat(75) { ErrorLog.record("n$it", RuntimeException("e")) }
        val r = ErrorLog.recent()
        assertEquals(60, r.size)
        assertTrue(r.first().contains("n74"))
    }
}
