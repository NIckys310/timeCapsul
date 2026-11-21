package com.timecapsule.app

import android.app.Application
import com.timecapsule.app.notify.Notifier
import com.timecapsule.app.ui.Idioma
import com.timecapsule.app.util.Fallos
import org.osmdroid.config.Configuration

class TimeCapsuleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Fallos.iniciar(this)
        Idioma.cargar(this)
        Notifier.createChannels(this)
        // Mapas de OpenStreetMap (no requieren clave)
        Configuration.getInstance().apply {
            load(this@TimeCapsuleApp, getSharedPreferences("osmdroid", MODE_PRIVATE))
            userAgentValue = packageName
            osmdroidBasePath = cacheDir
            osmdroidTileCache = cacheDir.resolve("tiles")
        }
    }
}
