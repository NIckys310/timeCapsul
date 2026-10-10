package com.timecapsule.app

import android.app.Application
import com.timecapsule.app.notify.Notifier
import org.osmdroid.config.Configuration

class TimeCapsuleApp : Application() {
    override fun onCreate() {
        super.onCreate()
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
