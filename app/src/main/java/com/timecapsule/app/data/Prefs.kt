package com.timecapsule.app.data

import android.content.Context

/** Pequeño almacenamiento local (línea de tiempo y cápsulas ya vistas). */
object Prefs {
    private fun p(ctx: Context) = ctx.getSharedPreferences("timecapsule", Context.MODE_PRIVATE)

    fun saveTimeline(ctx: Context, t: Timeline) = p(ctx).edit().putString("timeline", Repo.encodeTimeline(t)).apply()
    fun loadTimeline(ctx: Context): Timeline? = p(ctx).getString("timeline", null)?.let { Repo.decodeTimeline(it) }

    fun knownIds(ctx: Context): Set<String> = p(ctx).getStringSet("known", emptySet()) ?: emptySet()
    fun setKnownIds(ctx: Context, ids: Set<String>) = p(ctx).edit().putStringSet("known", ids).apply()
    fun hasSynced(ctx: Context) = p(ctx).getBoolean("synced", false)
    fun setSynced(ctx: Context) = p(ctx).edit().putBoolean("synced", true).apply()
    fun clear(ctx: Context) = p(ctx).edit().clear().apply()
}
