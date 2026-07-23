package com.wellbeingguard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** מחדש את התזמון אחרי הפעלה מחדש של המכשיר או עדכון האפליקציה. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Prefs(context).monitoringEnabled) Scheduler.schedule(context)
    }
}
