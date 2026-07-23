package com.wellbeingguard

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import java.util.Calendar

/**
 * קורא את זמן המסך של היום מתוך מנוע ה-Usage Stats של אנדרואיד –
 * אותו מקור נתונים שעליו מבוססת אפליקציית Digital Wellbeing.
 */
object UsageReader {

    private const val SCREEN_NON_INTERACTIVE = 16
    private const val KEYGUARD_SHOWN = 17
    private const val ACTIVITY_STOPPED = 23

    fun hasUsageAccess(ctx: Context): Boolean {
        val ops = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ops.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun startOfToday(): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    /** סך זמן המסך היום, בדקות. */
    fun screenTimeTodayMinutes(ctx: Context): Int =
        (screenTimeTodayMillis(ctx) / 60000L).toInt()

    fun screenTimeTodayMillis(ctx: Context): Long {
        if (!hasUsageAccess(ctx)) return -1L

        val usm = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val start = startOfToday()
        val now = System.currentTimeMillis()
        val events = usm.queryEvents(start, now)
        val ev = UsageEvents.Event()

        var total = 0L
        var openPkg: String? = null
        var openSince = 0L

        while (events.hasNextEvent()) {
            events.getNextEvent(ev)
            when (ev.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    // אם משהו אחר היה פתוח – סוגרים אותו קודם
                    if (openPkg != null) {
                        total += (ev.timeStamp - openSince).coerceAtLeast(0)
                    }
                    openPkg = ev.packageName
                    openSince = ev.timeStamp
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND, ACTIVITY_STOPPED -> {
                    if (openPkg != null && ev.packageName == openPkg) {
                        total += (ev.timeStamp - openSince).coerceAtLeast(0)
                        openPkg = null
                    }
                }
                SCREEN_NON_INTERACTIVE, KEYGUARD_SHOWN -> {
                    // מסך כבוי / ננעל – סוגרים כל מפגש פתוח
                    if (openPkg != null) {
                        total += (ev.timeStamp - openSince).coerceAtLeast(0)
                        openPkg = null
                    }
                }
            }
        }
        // אפליקציה שעדיין פתוחה ברגע זה
        if (openPkg != null) total += (now - openSince).coerceAtLeast(0)
        return total
    }

    fun formatMinutes(minutes: Int): String {
        if (minutes < 0) return "לא זמין"
        val h = minutes / 60
        val m = minutes % 60
        return if (h > 0) "$h שע' ו-$m דק'" else "$m דק'"
    }
}
