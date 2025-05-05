package com.example.boardingscreen

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
import java.util.Calendar

@Suppress("DEPRECATION")
class AppUsageService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString()
            if (packageName == "com.instagram.android" && isTimeLimitExceeded()) {
                showWarningDialog()
            }
        }
    }

    override fun onInterrupt() {
        // Handle interrupt
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onServiceConnected() {
        super.onServiceConnected()
        // Configure the service
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.DEFAULT
        }
        serviceInfo = info

    }

    private fun isTimeLimitExceeded(): Boolean {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()

        // Set startTime to the beginning of the day (00:00 AM)
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis

        // Query usage events for the entire day
        val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        var totalTimeInForeground = 0L
        var lastForegroundTime = 0L

        // Define WhatsApp package name
        val whatsappPackageName = "com.instagram.android"

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.packageName == whatsappPackageName) {
                when (event.eventType) {
                    UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                        lastForegroundTime = event.timeStamp
                    }
                    UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                        if (lastForegroundTime != 0L) {
                            totalTimeInForeground += (event.timeStamp - lastForegroundTime)
                            lastForegroundTime = 0L
                        }
                    }
                }
            }
        }

        // Convert total time in foreground to minutes
        val totalTimeInForegroundMinutes = totalTimeInForeground / 1000 / 60

        // Check if total time in foreground is more than 10 minutes
        return totalTimeInForegroundMinutes > 10
    }

    private fun showWarningDialog() {
       // val dialogIntent = Intent(this, WarningDialogActivity::class.java)
      //  dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      //  startActivity(dialogIntent)
    }

}
