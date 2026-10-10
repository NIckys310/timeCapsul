package com.timecapsule.app.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/**
 * Graba notas de voz en el celular (formato m4a, buena calidad y poco peso).
 * Máximo 3 minutos por nota.
 */
class VoiceRecorder(private val ctx: Context) {
    private var recorder: MediaRecorder? = null
    var file: File? = null
        private set
    private var startedAt = 0L

    val isRecording get() = recorder != null
    val elapsedMs get() = if (recorder != null) System.currentTimeMillis() - startedAt else 0L

    fun start(): File {
        stop()
        val dir = File(ctx.cacheDir, "voz").apply { mkdirs() }
        val f = File(dir, "nota_${System.currentTimeMillis()}.m4a")
        val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(ctx) else @Suppress("DEPRECATION") MediaRecorder()
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setAudioEncodingBitRate(96_000)
        r.setAudioSamplingRate(44_100)
        r.setMaxDuration(3 * 60_000)
        r.setOutputFile(f.absolutePath)
        r.prepare()
        r.start()
        recorder = r
        file = f
        startedAt = System.currentTimeMillis()
        return f
    }

    /** Detiene y devuelve el archivo (o null si fue demasiado corto). */
    fun stop(): File? {
        val r = recorder ?: return null
        recorder = null
        val ok = runCatching { r.stop() }.isSuccess
        r.release()
        val f = file
        return if (ok && f != null && f.length() > 2_000) f else { f?.delete(); null }
    }
}

fun fmtDuration(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}
