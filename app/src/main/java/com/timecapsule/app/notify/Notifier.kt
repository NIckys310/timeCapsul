package com.timecapsule.app.notify

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.timecapsule.app.R
import com.timecapsule.app.ui.MainActivity

object Notifier {
    const val CH_UNLOCK = "unlock_v2"
    const val CH_PLACE = "place_v2"
    /** Vibración de Time Capsule: corto-corto-largo (como un latido), se siente distinta en el reloj. */
    private val PATTERN = longArrayOf(0, 120, 90, 120, 90, 420)
    const val CH_SHARE = "share"
    const val CH_ACTIVITY = "activity"
    const val CH_REMINDER = "reminder"
    const val EXTRA_CAPSULE = "capsuleId"

    fun createChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        listOf("unlock", "place").forEach { runCatching { nm.deleteNotificationChannel(it) } }
        nm.createNotificationChannel(NotificationChannel(CH_UNLOCK, "Cápsulas que se abren", NotificationManager.IMPORTANCE_HIGH)
            .apply { enableVibration(true); vibrationPattern = PATTERN })
        nm.createNotificationChannel(NotificationChannel(CH_PLACE, "Recuerdos de lugares", NotificationManager.IMPORTANCE_HIGH)
            .apply { enableVibration(true); vibrationPattern = PATTERN })
        nm.createNotificationChannel(NotificationChannel(CH_SHARE, "Cápsulas compartidas", NotificationManager.IMPORTANCE_DEFAULT))
        nm.createNotificationChannel(NotificationChannel(CH_ACTIVITY, "Reacciones y comentarios", NotificationManager.IMPORTANCE_DEFAULT))
        nm.createNotificationChannel(NotificationChannel(CH_REMINDER, "Cuenta regresiva", NotificationManager.IMPORTANCE_DEFAULT))
    }

    fun canNotify(ctx: Context) = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun show(ctx: Context, id: Int, channel: String, title: String, text: String, capsuleId: String?) {
        if (!canNotify(ctx)) return
        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_CAPSULE, capsuleId)
        }
        val pi = PendingIntent.getActivity(ctx, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFFA63D2F.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        NotificationManagerCompat.from(ctx).notify(id, n)
    }
}
