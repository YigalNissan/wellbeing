package com.wellbeingguard

import android.content.Context

/** כל ההגדרות של האפליקציה, נשמרות מקומית במכשיר. */
class Prefs(ctx: Context) {

    private val sp = ctx.applicationContext.getSharedPreferences("wellbeing_guard", Context.MODE_PRIVATE)

    /** מגבלת זמן מסך יומית, בדקות */
    var limitMinutes: Int
        get() = sp.getInt("limit_minutes", 180)
        set(v) = sp.edit().putInt("limit_minutes", v).apply()

    /** כל כמה דקות לבדוק (ברירת מחדל 30) */
    var intervalMinutes: Int
        get() = sp.getInt("interval_minutes", 30)
        set(v) = sp.edit().putInt("interval_minutes", v).apply()

    /** כמה זמן להמתין בין התראה להתראה, בדקות */
    var cooldownMinutes: Int
        get() = sp.getInt("cooldown_minutes", 60)
        set(v) = sp.edit().putInt("cooldown_minutes", v).apply()

    /** מספרי טלפון נוספים, מופרדים בפסיק */
    var phones: String
        get() = sp.getString("phones", "") ?: ""
        set(v) = sp.edit().putString("phones", v).apply()

    var smsEnabled: Boolean
        get() = sp.getBoolean("sms_enabled", true)
        set(v) = sp.edit().putBoolean("sms_enabled", v).apply()

    /** Telegram – חלופה ל-SMS (חינמי, לא דורש הרשאת SMS) */
    var telegramToken: String
        get() = sp.getString("tg_token", "") ?: ""
        set(v) = sp.edit().putString("tg_token", v).apply()

    /** מזהי צ'אט בטלגרם, מופרדים בפסיק */
    var telegramChats: String
        get() = sp.getString("tg_chats", "") ?: ""
        set(v) = sp.edit().putString("tg_chats", v).apply()

    var monitoringEnabled: Boolean
        get() = sp.getBoolean("monitoring_enabled", false)
        set(v) = sp.edit().putBoolean("monitoring_enabled", v).apply()

    var userLabel: String
        get() = sp.getString("user_label", "") ?: ""
        set(v) = sp.edit().putString("user_label", v).apply()

    var lastAlertAt: Long
        get() = sp.getLong("last_alert_at", 0L)
        set(v) = sp.edit().putLong("last_alert_at", v).apply()

    var lastCheckAt: Long
        get() = sp.getLong("last_check_at", 0L)
        set(v) = sp.edit().putLong("last_check_at", v).apply()

    var lastCheckMinutes: Int
        get() = sp.getInt("last_check_minutes", 0)
        set(v) = sp.edit().putInt("last_check_minutes", v).apply()

    fun phoneList(): List<String> =
        phones.split(",", ";", "\n").map { it.trim() }.filter { it.length >= 6 }

    fun chatList(): List<String> =
        telegramChats.split(",", ";", "\n").map { it.trim() }.filter { it.isNotEmpty() }
}
