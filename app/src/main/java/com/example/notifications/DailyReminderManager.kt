package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.i18n.LanguagePreferenceManager
import java.util.Calendar

data class ReminderSettings(
  val enabled: Boolean = false,
  val hour: Int = 20,
  val minute: Int = 0,
)

class DailyReminderReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent?) {
    if (!DailyReminderManager.getSettings(context).enabled) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val granted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
      ) == PackageManager.PERMISSION_GRANTED
      if (!granted) return
    }
    val localized = LanguagePreferenceManager.localizedContext(context)
    DailyReminderManager.ensureNotificationChannel(localized)

    val launchIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val contentPendingIntent = PendingIntent.getActivity(
      context,
      0,
      launchIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val notification = NotificationCompat.Builder(localized, CHANNEL_ID)
      .setSmallIcon(R.mipmap.ic_launcher_round)
      .setContentTitle(localized.getString(R.string.reminder_notification_title))
      .setContentText(localized.getString(R.string.reminder_notification_body))
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setContentIntent(contentPendingIntent)
      .setAutoCancel(true)
      .build()

    NotificationManagerCompat.from(localized).notify(NOTIFICATION_ID, notification)
    BillDueReminderManager.notifyDueBillsIfAny(context)
  }

  private companion object {
    const val CHANNEL_ID = "sakupintar_daily_reminder"
    const val NOTIFICATION_ID = 4101
  }
}

object DailyReminderManager {
  private const val PREFS_NAME = "sakupintar_reminder_prefs"
  private const val KEY_ENABLED = "reminder_enabled"
  private const val KEY_HOUR = "reminder_hour"
  private const val KEY_MINUTE = "reminder_minute"
  private const val CHANNEL_ID = "sakupintar_daily_reminder"
  private const val REQUEST_CODE = 4100

  fun getSettings(context: Context): ReminderSettings {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return ReminderSettings(
      enabled = prefs.getBoolean(KEY_ENABLED, false),
      hour = prefs.getInt(KEY_HOUR, 20),
      minute = prefs.getInt(KEY_MINUTE, 0),
    )
  }

  fun updateSettings(context: Context, settings: ReminderSettings): ReminderSettings {
    val sanitizedHour = settings.hour.coerceIn(0, 23)
    val sanitizedMinute = settings.minute.coerceIn(0, 59)
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit()
      .putBoolean(KEY_ENABLED, settings.enabled)
      .putInt(KEY_HOUR, sanitizedHour)
      .putInt(KEY_MINUTE, sanitizedMinute)
      .apply()

    val clean = settings.copy(hour = sanitizedHour, minute = sanitizedMinute)
    if (clean.enabled) {
      ensureNotificationChannel(LanguagePreferenceManager.localizedContext(context))
      scheduleDailyReminder(context, clean.hour, clean.minute)
    } else {
      cancelDailyReminder(context)
    }
    return clean
  }

  fun ensureNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val localized = LanguagePreferenceManager.localizedContext(context)
      val manager = localized.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      if (manager.getNotificationChannel(CHANNEL_ID) == null) {
        val channel = NotificationChannel(
          CHANNEL_ID,
          localized.getString(R.string.reminder_title),
          NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
          description = localized.getString(R.string.reminder_subtitle)
        }
        manager.createNotificationChannel(channel)
      }
    }
  }

  private fun scheduleDailyReminder(context: Context, hour: Int, minute: Int) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val pendingIntent = buildPendingIntent(context)
    val calendar = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, hour)
      set(Calendar.MINUTE, minute)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
      if (timeInMillis <= System.currentTimeMillis()) {
        add(Calendar.DAY_OF_YEAR, 1)
      }
    }
    alarmManager.setInexactRepeating(
      AlarmManager.RTC_WAKEUP,
      calendar.timeInMillis,
      AlarmManager.INTERVAL_DAY,
      pendingIntent,
    )
  }

  private fun cancelDailyReminder(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    alarmManager.cancel(buildPendingIntent(context))
  }

  private fun buildPendingIntent(context: Context): PendingIntent {
    val intent = Intent(context, DailyReminderReceiver::class.java)
    return PendingIntent.getBroadcast(
      context,
      REQUEST_CODE,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }
}
