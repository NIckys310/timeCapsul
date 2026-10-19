package com.timecapsule.app.notify

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.timecapsule.app.data.Capsule
import com.timecapsule.app.data.Prefs
import com.timecapsule.app.data.Repo
import com.timecapsule.app.geo.GeofenceManager
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

/** Muestra la notificación cuando llega la fecha de apertura. */
class UnlockWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val id = inputData.getString("id") ?: return Result.success()
        val title = inputData.getString("title").orEmpty()
        val place = inputData.getString("place")
        Notifier.show(applicationContext, id.hashCode(), Notifier.CH_UNLOCK, Avisos.tituloApertura(title), Avisos.textoApertura(place), id)
        return Result.success()
    }

    companion object {
        fun schedule(ctx: Context, c: Capsule) {
            val at = c.unlockInstant ?: return
            val delay = Duration.between(Instant.now(), at)
            if (delay.isNegative) return
            val req = OneTimeWorkRequestBuilder<UnlockWorker>()
                .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
                .setInputData(workDataOf("id" to c.id, "title" to c.title, "place" to Avisos.lugarParaAbrir(c)))
                .build()
            WorkManager.getInstance(ctx).enqueueUniqueWork("unlock_${c.id}", ExistingWorkPolicy.REPLACE, req)
            // Cuenta regresiva: 1 semana, 1 día y 1 hora antes
            Avisos.recordatorios(delay).forEach { (wait, label) ->
                val r = OneTimeWorkRequestBuilder<ReminderWorker>()
                    .setInitialDelay(wait.toMillis(), TimeUnit.MILLISECONDS)
                    .setInputData(workDataOf("id" to c.id, "title" to c.title, "label" to label))
                    .build()
                WorkManager.getInstance(ctx).enqueueUniqueWork("remind_${label}_${c.id}", ExistingWorkPolicy.REPLACE, r)
            }
        }
        fun cancel(ctx: Context, id: String) {
            val wm = WorkManager.getInstance(ctx)
            wm.cancelUniqueWork("unlock_$id")
            Avisos.CUENTA_REGRESIVA.forEach { (_, label) -> wm.cancelUniqueWork("remind_${label}_$id") }
        }
    }
}

/** Aviso de cuenta regresiva ("falta 1 día para abrir…"). */
class ReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val id = inputData.getString("id") ?: return Result.success()
        val title = inputData.getString("title").orEmpty()
        val label = inputData.getString("label").orEmpty()
        Notifier.show(applicationContext, ("r$label$id").hashCode(), Notifier.CH_REMINDER, Avisos.tituloRecordatorio(label), Avisos.textoRecordatorio(title), id)
        return Result.success()
    }
}

/** Cada hora revisa la nube: cápsulas nuevas compartidas contigo, fechas y lugares. */
class SyncWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result = runCatching {
        Repo.ready()
        if (Repo.userId() == null) return Result.success()
        Sync.apply(applicationContext, Repo.feed(), notifyNew = true)
        Sync.activity(applicationContext)
        Sync.yearAgo(applicationContext)
        Result.success()
    }.getOrElse { Result.retry() }

    companion object {
        fun schedulePeriodic(ctx: Context) {
            val req = PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork("sync", ExistingPeriodicWorkPolicy.UPDATE, req)
        }
    }
}

object Sync {
    /** Programa avisos de fecha, actualiza lugares y avisa de cápsulas nuevas compartidas. */
    fun apply(ctx: Context, list: List<Capsule>, notifyNew: Boolean) {
        val me = Repo.userId()
        val known = Prefs.knownIds(ctx)
        if (notifyNew && Prefs.hasSynced(ctx)) {
            Avisos.nuevasCompartidas(list, known, me).forEach { c ->
                Notifier.show(ctx, ("s" + c.id).hashCode(), Notifier.CH_SHARE, Avisos.tituloCompartida(c), Avisos.textoCompartida(c), c.id)
            }
        }
        Prefs.setKnownIds(ctx, list.map { it.id }.toSet())
        Prefs.setSynced(ctx)
        list.filter { !it.dateReady }.forEach { UnlockWorker.schedule(ctx, it) }
        GeofenceManager.update(ctx, list)
    }

    /** Una vez al día: "Hace un año guardaste…" (lo escribe la IA). */
    suspend fun yearAgo(ctx: Context) {
        val p = ctx.getSharedPreferences("timecapsule", Context.MODE_PRIVATE)
        val today = java.time.LocalDate.now().toString()
        if (p.getString("yearAgoDay", null) == today) return
        p.edit().putString("yearAgoDay", today).apply()
        val r = runCatching { Repo.aiYearAgo() }.getOrNull() ?: return
        if (p.getString("yearAgoLast", null) == r.second) return
        p.edit().putString("yearAgoLast", r.second).apply()
        Notifier.show(ctx, ("y" + r.second).hashCode(), Notifier.CH_REMINDER, "📸 Hace un año: “${r.third}”", r.first, r.second)
    }

    /** Avisa cuando alguien reacciona o comenta en tus cápsulas. */
    suspend fun activity(ctx: Context) {
        val p = ctx.getSharedPreferences("timecapsule", Context.MODE_PRIVATE)
        val since = p.getString("activitySince", null) ?: Instant.now().toString().also { p.edit().putString("activitySince", it).apply(); return }
        val list = runCatching { Repo.recentActivity(since) }.getOrDefault(emptyList())
        list.take(Avisos.MAX_ACTIVIDAD).forEach { a ->
            Notifier.show(ctx, (a.kind + a.capsuleId + a.at).hashCode(), Notifier.CH_ACTIVITY, "“${a.title}”", Avisos.textoActividad(a), a.capsuleId)
        }
        p.edit().putString("activitySince", Instant.now().toString()).apply()
    }
}
