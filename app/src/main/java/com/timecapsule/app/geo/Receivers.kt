package com.timecapsule.app.geo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.timecapsule.app.notify.Notifier

/** Se activa cuando entras a un lugar donde guardaste una cápsula. */
class GeofenceReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError() || event.geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER) return
        val infos = GeofenceManager.stored(ctx).associateBy { it.id }
        event.triggeringGeofences?.forEach { g ->
            val i = infos[g.requestId] ?: return@forEach
            val place = i.place ?: "este lugar"
            val (title, text) = when {
                i.requirePlace && !i.isOpen && i.dateReady ->
                    "Llegaste a $place" to "Ya puedes abrir “${i.title}”. Toca para abrirla aquí."
                i.isOpen ->
                    "Tienes recuerdos aquí" to "Estás en $place. Mira las fotos y la carta de “${i.title}”."
                else ->
                    "Una cápsula te espera aquí" to "“${i.title}” se guardó en $place. Todavía no se puede abrir."
            }
            Notifier.show(ctx, ("g" + i.id).hashCode(), Notifier.CH_PLACE, title, text, i.id)
        }
    }
}

/** Después de reiniciar el celular, Android borra las geocercas: las volvemos a crear. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) GeofenceManager.reRegister(ctx)
    }
}
