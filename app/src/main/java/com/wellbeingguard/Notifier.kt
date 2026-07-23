package com.wellbeingguard

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object Notifier {

    const val CHANNEL_ALERTS = "wg_alerts"

    fun createChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = ctx.getSystemService(NotificationManager::class.java)
            val ch = NotificationChannel(
                CHANNEL_ALERTS,
                "התראות חריגה מזמן מסך",
                NotificationManager.IMPORTANCE_HIGH
            )
            ch.description = "מתריע כשעברת את מגבלת זמן המסך היומית"
            nm.createNotificationChannel(ch)
        }
    }

    fun notify(ctx: Context, title: String, text: String, id: Int = 1001) {
        val pi = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(id, n)
        } catch (_: SecurityException) {
            // אין הרשאת התראות – מתעלמים בשקט
        }
    }
}
