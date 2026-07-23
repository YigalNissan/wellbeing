package com.wellbeingguard

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifier.createChannels(this)
        if (Prefs(this).monitoringEnabled) Scheduler.schedule(this)
    }
}
