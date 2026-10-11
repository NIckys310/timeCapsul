package com.timecapsule.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Ajustes de apariencia y accesibilidad (se guardan en el celular).
 * Las pantallas los leen como un estado: si cambian, la app se repinta sola.
 */
data class Look(
    val theme: String = "system",       // system | light | dark
    val highContrast: Boolean = false,
    val textScale: Float = 1f,          // 1.0 · 1.15 · 1.3
    val vibrateOnOpen: Boolean = true,
    val readAloud: Boolean = false,     // leer la carta en voz alta al abrir
    val animations: Boolean = true,
    val appLock: Boolean = false,       // pedir huella o PIN al abrir la app
    val hideRecents: Boolean = false,   // ocultar en apps recientes y bloquear capturas
)

object AppSettings {
    private val _look = MutableStateFlow(Look())
    val look = _look.asStateFlow()

    private fun p(ctx: Context) = ctx.getSharedPreferences("timecapsule_look", Context.MODE_PRIVATE)

    fun load(ctx: Context) {
        val s = p(ctx)
        _look.value = Look(
            theme = s.getString("theme", "system") ?: "system",
            highContrast = s.getBoolean("contrast", false),
            textScale = s.getFloat("textScale", 1f),
            vibrateOnOpen = s.getBoolean("vibrate", true),
            readAloud = s.getBoolean("readAloud", false),
            animations = s.getBoolean("animations", true),
            appLock = s.getBoolean("appLock", false),
            hideRecents = s.getBoolean("hideRecents", false),
        )
    }

    fun update(ctx: Context, change: (Look) -> Look) {
        val n = change(_look.value)
        _look.value = n
        p(ctx).edit()
            .putString("theme", n.theme).putBoolean("contrast", n.highContrast).putFloat("textScale", n.textScale)
            .putBoolean("vibrate", n.vibrateOnOpen).putBoolean("readAloud", n.readAloud).putBoolean("animations", n.animations)
            .putBoolean("appLock", n.appLock).putBoolean("hideRecents", n.hideRecents)
            .apply()
    }
}
