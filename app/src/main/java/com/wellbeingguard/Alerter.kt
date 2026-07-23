package com.wellbeingguard

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** אחראי על שליחת ההתראה למכשירים הנוספים. */
object Alerter {

    data class Result(val smsSent: Int, val telegramSent: Int, val errors: List<String>)

    fun buildMessage(prefs: Prefs, usedMinutes: Int): String {
        val who = prefs.userLabel.ifBlank { "המשתמש" }
        return "⚠️ התראת זמן מסך\n$who עבר/ה את המגבלה היומית.\n" +
                "שימוש היום: ${UsageReader.formatMinutes(usedMinutes)}\n" +
                "מגבלה: ${UsageReader.formatMinutes(prefs.limitMinutes)}"
    }

    fun sendAll(ctx: Context, prefs: Prefs, message: String): Result {
        val errors = mutableListOf<String>()
        var sms = 0
        var tg = 0

        if (prefs.smsEnabled && hasSmsPermission(ctx)) {
            for (num in prefs.phoneList()) {
                try {
                    val mgr = smsManager(ctx)
                    val parts = mgr.divideMessage(message)
                    mgr.sendMultipartTextMessage(num, null, parts, null, null)
                    sms++
                } catch (e: Exception) {
                    errors.add("SMS ל-$num נכשל: ${e.message}")
                }
            }
        } else if (prefs.smsEnabled && prefs.phoneList().isNotEmpty()) {
            errors.add("אין הרשאת שליחת SMS")
        }

        val token = prefs.telegramToken.trim()
        if (token.isNotEmpty()) {
            for (chat in prefs.chatList()) {
                try {
                    if (sendTelegram(token, chat, message)) tg++ else errors.add("טלגרם ל-$chat נכשל")
                } catch (e: Exception) {
                    errors.add("טלגרם ל-$chat נכשל: ${e.message}")
                }
            }
        }
        return Result(sms, tg, errors)
    }

    fun hasSmsPermission(ctx: Context): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.SEND_SMS) ==
                PackageManager.PERMISSION_GRANTED

    @Suppress("DEPRECATION")
    private fun smsManager(ctx: Context): SmsManager =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            ctx.getSystemService(SmsManager::class.java)
        else SmsManager.getDefault()

    private fun sendTelegram(token: String, chatId: String, text: String): Boolean {
        val url = URL(
            "https://api.telegram.org/bot$token/sendMessage?chat_id=" +
                    URLEncoder.encode(chatId, "UTF-8") + "&text=" +
                    URLEncoder.encode(text, "UTF-8")
        )
        val conn = url.openConnection() as HttpURLConnection
        return try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            val code = conn.responseCode
            if (code != 200) {
                conn.errorStream?.bufferedReader()?.use(BufferedReader::readText)
            }
            code == 200
        } finally {
            conn.disconnect()
        }
    }
}
