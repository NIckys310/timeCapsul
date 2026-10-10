package com.timecapsule.app.util

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Lee textos en voz alta (la carta de la cápsula) con la voz en español del celular. */
object Speech {
    private var tts: TextToSpeech? = null
    private var ready = false
    private var pending: String? = null

    fun speak(ctx: Context, text: String) {
        if (text.isBlank()) return
        val t = tts
        if (t != null && ready) { t.speak(text, TextToSpeech.QUEUE_FLUSH, null, "carta"); return }
        pending = text
        if (t == null) tts = TextToSpeech(ctx.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                tts?.language = Locale("es", "CO").takeIf { tts?.isLanguageAvailable(it) ?: -1 >= TextToSpeech.LANG_AVAILABLE } ?: Locale("es")
                tts?.setSpeechRate(0.95f)
                pending?.let { tts?.speak(it, TextToSpeech.QUEUE_FLUSH, null, "carta") }
                pending = null
            }
        }
    }

    fun isSpeaking() = tts?.isSpeaking == true
    fun stop() { tts?.stop() }
}
