package com.wellbeingguard

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.wellbeingguard.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private lateinit var prefs: Prefs

    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { refresh() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        prefs = Prefs(this)

        loadIntoFields()

        b.btnUsageAccess.setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            Toast.makeText(this, "אתר את \"Wellbeing Guard\" ואפשר גישה", Toast.LENGTH_LONG).show()
        }

        b.btnPerms.setOnClickListener {
            val list = mutableListOf<String>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                list.add(Manifest.permission.POST_NOTIFICATIONS)
            list.add(Manifest.permission.SEND_SMS)
            permLauncher.launch(list.toTypedArray())
        }

        b.btnBattery.setOnClickListener {
            try {
                startActivity(
                    Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:$packageName")
                    )
                )
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }

        b.btnSave.setOnClickListener {
            if (saveFields()) {
                Snackbar.make(b.root, "ההגדרות נשמרו", Snackbar.LENGTH_SHORT).show()
                if (prefs.monitoringEnabled) Scheduler.schedule(this)
                refresh()
            }
        }

        b.swEnable.setOnCheckedChangeListener { _, checked ->
            if (checked && !UsageReader.hasUsageAccess(this)) {
                b.swEnable.isChecked = false
                Snackbar.make(b.root, "צריך קודם לאשר גישה לנתוני שימוש", Snackbar.LENGTH_LONG).show()
                return@setOnCheckedChangeListener
            }
            saveFields()
            prefs.monitoringEnabled = checked
            if (checked) Scheduler.schedule(this) else Scheduler.cancel(this)
            refresh()
        }

        b.btnCheckNow.setOnClickListener {
            saveFields()
            Scheduler.runNow(this)
            Snackbar.make(b.root, "בדיקה הופעלה – רענן בעוד רגע", Snackbar.LENGTH_SHORT).show()
        }

        b.btnTest.setOnClickListener {
            saveFields()
            val used = UsageReader.screenTimeTodayMinutes(this)
            val msg = "🔔 הודעת בדיקה מאפליקציית Wellbeing Guard\n" +
                    Alerter.buildMessage(prefs, if (used < 0) 0 else used)
            Notifier.notify(this, "בדיקה: כך תיראה ההתראה", msg)
            CoroutineScope(Dispatchers.IO).launch {
                val res = Alerter.sendAll(this@MainActivity, prefs, msg)
                withContext(Dispatchers.Main) {
                    val txt = "נשלחו: ${res.smsSent} SMS, ${res.telegramSent} טלגרם" +
                            if (res.errors.isEmpty()) "" else "\n" + res.errors.joinToString("\n")
                    Snackbar.make(b.root, txt, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun loadIntoFields() {
        b.etLabel.setText(prefs.userLabel)
        b.etLimit.setText(formatHours(prefs.limitMinutes))
        b.etInterval.setText(prefs.intervalMinutes.toString())
        b.etCooldown.setText(prefs.cooldownMinutes.toString())
        b.etPhones.setText(prefs.phones)
        b.swSms.isChecked = prefs.smsEnabled
        b.etTgToken.setText(prefs.telegramToken)
        b.etTgChats.setText(prefs.telegramChats)
        b.swEnable.isChecked = prefs.monitoringEnabled
    }

    private fun formatHours(minutes: Int): String {
        val h = minutes / 60.0
        return if (h == Math.floor(h)) h.toInt().toString() else String.format(Locale.US, "%.2f", h)
    }

    private fun saveFields(): Boolean {
        val hours = b.etLimit.text.toString().trim().replace(",", ".").toDoubleOrNull()
        if (hours == null || hours <= 0) {
            b.etLimit.error = "הזן מספר שעות חוקי"
            return false
        }
        val interval = b.etInterval.text.toString().trim().toIntOrNull() ?: 30
        val cooldown = b.etCooldown.text.toString().trim().toIntOrNull() ?: 60

        prefs.userLabel = b.etLabel.text.toString().trim()
        prefs.limitMinutes = Math.round(hours * 60).toInt().coerceAtLeast(1)
        prefs.intervalMinutes = interval.coerceAtLeast(15)
        prefs.cooldownMinutes = cooldown.coerceAtLeast(1)
        prefs.phones = b.etPhones.text.toString().trim()
        prefs.smsEnabled = b.swSms.isChecked
        prefs.telegramToken = b.etTgToken.text.toString().trim()
        prefs.telegramChats = b.etTgChats.text.toString().trim()
        return true
    }

    private fun refresh() {
        val hasAccess = UsageReader.hasUsageAccess(this)
        b.tvAccessState.text = if (hasAccess) "✅ גישה לנתוני שימוש: מאושרת"
        else "❌ גישה לנתוני שימוש: חסרה – לחץ על הכפתור"

        b.tvSmsState.text = if (Alerter.hasSmsPermission(this)) "✅ הרשאת SMS: מאושרת"
        else "⚠️ הרשאת SMS: חסרה (אפשר להשתמש בטלגרם במקום)"

        val used = if (hasAccess) UsageReader.screenTimeTodayMinutes(this) else -1
        b.tvUsage.text = if (used >= 0)
            "זמן מסך היום: ${UsageReader.formatMinutes(used)}\nמתוך מגבלה של ${UsageReader.formatMinutes(prefs.limitMinutes)}"
        else "זמן מסך היום: לא זמין"

        val last = prefs.lastCheckAt
        b.tvLastCheck.text = if (last > 0) {
            val f = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(last))
            "בדיקה אחרונה: $f · נמדדו ${UsageReader.formatMinutes(prefs.lastCheckMinutes)}"
        } else "בדיקה אחרונה: טרם בוצעה"

        b.tvMonitorState.text = if (prefs.monitoringEnabled)
            "הניטור פעיל – בדיקה כל ${prefs.intervalMinutes} דקות"
        else "הניטור כבוי"
    }
}
