package com.wellbeingguard

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object Scheduler {

    private const val WORK_NAME = "wellbeing_periodic_check"

    fun schedule(ctx: Context) {
        val prefs = Prefs(ctx)
        // מינימום של WorkManager הוא 15 דקות; ברירת המחדל שלנו 30
        val minutes = prefs.intervalMinutes.coerceAtLeast(15).toLong()
        val req = PeriodicWorkRequestBuilder<UsageCheckWorker>(minutes, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
            WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, req
        )
    }

    fun cancel(ctx: Context) {
        WorkManager.getInstance(ctx).cancelUniqueWork(WORK_NAME)
    }

    fun runNow(ctx: Context) {
        WorkManager.getInstance(ctx)
            .enqueue(OneTimeWorkRequestBuilder<UsageCheckWorker>().build())
    }
}
