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
        val text = if (place != null) "Ya llegó la fecha. Ve a $place para abrirla."
                   else "Ya puedes abrirla y revivir lo que guardaste."
        Notifier.show(applicationContext, id.hashCode(), Notifier.CH_UNLOCK, "¡“$title” se puede abrir!", text, id)
        return Result.success()
    }

    companion object {
        fun schedule(ctx: Context, c: Capsule) {
            val at = c.unlockInstant ?: return
            val delay = Duration.between(Instant.now(), at)
            if (delay.isNegative) return
            val req = OneTimeWorkRequestBuilder<UnlockWorker>()
                .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
                .setInputData(workDataOf("id" to c.id, "title" to c.title, "place" to (if (c.requirePlace) (c.placeName ?: "su lugar") else null)))
                .build()
            WorkManager.getInstance(ctx).enqueueUniqueWork("unlock_${c.id}", ExistingWorkPolicy.REPLACE, req)
        }
        fun cancel(ctx: Context, id: String) = WorkManager.getInstance(ctx).cancelUniqueWork("unlock_$id")
    }
}

/** Cada 6 horas revisa la nube: cápsulas nuevas compartidas contigo, fechas y lugares. */
class SyncWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result = runCatching {
        Repo.ready()
        if (Repo.userId() == null) return Result.success()
        Sync.apply(applicationContext, Repo.feed(), notifyNew = true)
        Result.success()
    }.getOrElse { Result.retry() }

    companion object {
        fun schedulePeriodic(ctx: Context) {
            val req = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork("sync", ExistingPeriodicWorkPolicy.KEEP, req)
        }
    }
}

object Sync {
    /** Programa avisos de fecha, actualiza lugares y avisa de cápsulas nuevas compartidas. */
    fun apply(ctx: Context, list: List<Capsule>, notifyNew: Boolean) {
        val me = Repo.userId()
        val known = Prefs.knownIds(ctx)
        if (notifyNew && Prefs.hasSynced(ctx)) {
            list.filter { it.id !in known && it.owner != me }.forEach { c ->
                Notifier.show(ctx, ("s" + c.id).hashCode(), Notifier.CH_SHARE,
                    "${c.ownerName ?: "Alguien"} compartió una cápsula contigo", "“${c.title}”. ${if (c.isOpen) "Ya puedes verla." else "Te avisaremos cuando se abra."}", c.id)
            }
        }
        Prefs.setKnownIds(ctx, list.map { it.id }.toSet())
        Prefs.setSynced(ctx)
        list.filter { !it.dateReady }.forEach { UnlockWorker.schedule(ctx, it) }
        GeofenceManager.update(ctx, list)
    }
}
