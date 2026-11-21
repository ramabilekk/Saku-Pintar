package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.RecurringTemplate
import com.example.i18n.LanguagePreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

data class BillDueSchedule(
  val templateId: String,
  val title: String,
  val dueDayOfMonth: Int,
  val reminderEnabled: Boolean = true,
) {
  /** Returns days remaining until the next due date (0 = due today). */
  fun daysUntilDue(today: LocalDate = LocalDate.now()): Int {
    val thisMonth = YearMonth.from(today)
    val clampedThisMonthDay = dueDayOfMonth.coerceIn(1, thisMonth.lengthOfMonth())
    val thisMonthDate = thisMonth.atDay(clampedThisMonthDay)
    val nextDueDate = if (!thisMonthDate.isBefore(today)) {
      thisMonthDate
    } else {
      val nextMonth = thisMonth.plusMonths(1)
      nextMonth.atDay(dueDayOfMonth.coerceIn(1, nextMonth.lengthOfMonth()))
    }
    return ChronoUnit.DAYS.between(today, nextDueDate).toInt().coerceAtLeast(0)
  }
}

data class UpcomingBillAlert(
  val template: RecurringTemplate,
  val schedule: BillDueSchedule,
  val daysUntilDue: Int,
)

/**
 * Manages bill due-date schedules per RecurringTemplate and dispatches due-date notifications
 * without modifying Firestore schema.
 */
object BillDueReminderManager {
  private const val PREFS_NAME = "sakupintar_bill_due_schedules"
  private const val KEY_MAP = "schedules_json"
  private const val CHANNEL_ID = "sakupintar_bill_due_reminders"
  private const val NOTIFICATION_ID_BASE = 4200

  private val _schedules = MutableStateFlow<Map<String, BillDueSchedule>>(emptyMap())
  @Volatile
  private var isLoaded = false

  fun observeSchedules(context: Context): StateFlow<Map<String, BillDueSchedule>> {
    ensureLoaded(context)
    return _schedules.asStateFlow()
  }

  fun getDueSchedule(context: Context, templateId: String): BillDueSchedule? {
    ensureLoaded(context)
    return _schedules.value[templateId]
  }

  fun setDueSchedule(
    context: Context,
    templateId: String,
    title: String,
    dueDayOfMonth: Int?,
    reminderEnabled: Boolean = true,
  ) {
    ensureLoaded(context)
    if (dueDayOfMonth == null || dueDayOfMonth !in 1..31) {
      removeDueSchedule(context, templateId)
      return
    }
    val updated = _schedules.value + (
      templateId to BillDueSchedule(
        templateId = templateId,
        title = title.trim().take(80),
        dueDayOfMonth = dueDayOfMonth.coerceIn(1, 31),
        reminderEnabled = reminderEnabled,
      )
      )
    _schedules.value = updated
    persist(context, updated)
  }

  fun removeDueSchedule(context: Context, templateId: String) {
    ensureLoaded(context)
    if (!_schedules.value.containsKey(templateId)) return
    val updated = _schedules.value - templateId
    _schedules.value = updated
    persist(context, updated)
  }

  fun computeUpcomingBills(
    templates: List<RecurringTemplate>,
    schedules: Map<String, BillDueSchedule>,
    withinDays: Int = 5,
    today: LocalDate = LocalDate.now(),
  ): List<UpcomingBillAlert> {
    return templates.mapNotNull { tpl ->
      val schedule = schedules[tpl.id] ?: return@mapNotNull null
      val days = schedule.daysUntilDue(today)
      if (days <= withinDays) {
        UpcomingBillAlert(
          template = tpl,
          schedule = schedule,
          daysUntilDue = days,
        )
      } else {
        null
      }
    }.sortedBy { it.daysUntilDue }
  }

  fun notifyDueBillsIfAny(context: Context) {
    ensureLoaded(context)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val granted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
      ) == PackageManager.PERMISSION_GRANTED
      if (!granted) return
    }
    val today = LocalDate.now()
    val dueSoon = _schedules.value.values
      .filter { it.reminderEnabled }
      .map { it to it.daysUntilDue(today) }
      .filter { (_, days) -> days <= 1 }

    if (dueSoon.isEmpty()) return

    val localized = LanguagePreferenceManager.localizedContext(context)
    ensureChannel(localized)

    val launchIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
      context,
      4201,
      launchIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    dueSoon.take(3).forEachIndexed { index, (schedule, days) ->
      val body = if (days == 0) {
        localized.getString(R.string.bill_due_notif_today, schedule.title)
      } else {
        localized.getString(R.string.bill_due_notif_tomorrow, schedule.title)
      }
      val notification = NotificationCompat.Builder(localized, CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher_round)
        .setContentTitle(localized.getString(R.string.bill_due_notif_title))
        .setContentText(body)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()

      NotificationManagerCompat.from(localized).notify(NOTIFICATION_ID_BASE + index, notification)
    }
  }

  private fun ensureChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      if (manager.getNotificationChannel(CHANNEL_ID) == null) {
        val channel = NotificationChannel(
          CHANNEL_ID,
          context.getString(R.string.bill_due_notif_title),
          NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
          description = context.getString(R.string.bill_due_notif_channel_desc)
        }
        manager.createNotificationChannel(channel)
      }
    }
  }

  private fun ensureLoaded(context: Context) {
    if (isLoaded) return
    synchronized(this) {
      if (isLoaded) return
      val raw = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_MAP, null)
      val parsed = if (raw.isNullOrBlank()) {
        emptyMap()
      } else {
        runCatching {
          val obj = JSONObject(raw)
          buildMap {
            obj.keys().forEach { key ->
              val item = obj.optJSONObject(key) ?: return@forEach
              val day = item.optInt("dueDayOfMonth", 0)
              if (day in 1..31) {
                put(
                  key,
                  BillDueSchedule(
                    templateId = key,
                    title = item.optString("title", ""),
                    dueDayOfMonth = day,
                    reminderEnabled = item.optBoolean("reminderEnabled", true),
                  )
                )
              }
            }
          }
        }.getOrDefault(emptyMap())
      }
      _schedules.value = parsed
      isLoaded = true
    }
  }

  private fun persist(context: Context, map: Map<String, BillDueSchedule>) {
    val root = JSONObject()
    map.forEach { (id, schedule) ->
      root.put(
        id,
        JSONObject().apply {
          put("title", schedule.title)
          put("dueDayOfMonth", schedule.dueDayOfMonth)
          put("reminderEnabled", schedule.reminderEnabled)
        }
      )
    }
    context.applicationContext
      .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putString(KEY_MAP, root.toString())
      .apply()
  }
}
