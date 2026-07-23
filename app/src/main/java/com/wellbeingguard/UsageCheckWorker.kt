package com.wellbeingguard

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** רץ פעם ב-X דקות (ברירת מחדל 30) ובודק חריגה. */
class UsageCheckWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val ctx = applicationContext
        val prefs = Prefs(ctx)

        if (!prefs.monitoringEnabled) return@withContext Result.success()
        if (!UsageReader.hasUsageAccess(ctx)) {
            Notifier.notify(
                ctx, "נדרשת הרשאה",
                "אין גישה לנתוני השימוש. פתח את האפליקציה ואשר 'גישה לנתוני שימוש'.", 1002
            )
            return@withContext Result.success()
        }

        val used = UsageReader.screenTimeTodayMinutes(ctx)
        prefs.lastCheckAt = System.currentTimeMillis()
        prefs.lastCheckMinutes = used

        if (used >= prefs.limitMinutes) {
            val now = System.currentTimeMillis()
            val cooldownMs = prefs.cooldownMinutes * 60_000L
            if (now - prefs.lastAlertAt >= cooldownMs) {
                prefs.lastAlertAt = now
                val msg = Alerter.buildMessage(prefs, used)
                Notifier.notify(ctx, "חריגה ממגבלת זמן המסך", msg)
                Alerter.sendAll(ctx, prefs, msg)
            }
        }
        Result.success()
    }
}
