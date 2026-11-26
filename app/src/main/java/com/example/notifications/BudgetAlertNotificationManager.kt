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
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.MonthlyBudget
import com.example.i18n.CurrencyPreferenceManager
import com.example.i18n.LanguagePreferenceManager
import com.example.security.PrivacySecurityManager
import com.example.util.Formatters

/**
 * Evaluates category budget usage after expense mutations and posts localized Android notifications
 * when a category crosses the 80% warning or 100% over-budget threshold (deduplicated per cycle).
 */
object BudgetAlertNotificationManager {
  private const val PREFS_NAME = "sakupintar_budget_alerts_sent"
  private const val CHANNEL_ID = "sakupintar_budget_alerts"
  private const val BASE_NOTIFICATION_ID = 4300

  fun evaluateAndNotifyIfThresholdCrossed(
    context: Context,
    monthKey: String,
    category: ExpenseCategory,
    budget: MonthlyBudget?,
    periodExpenses: List<Expense>,
    newlyAddedAmount: Long = 0L,
  ) {
    val appCtx = context.applicationContext
    if (!PrivacySecurityManager.isBudgetAlertsEnabled(appCtx)) return
    val allocated = budget?.categoryBudgets?.get(category.name) ?: return
    if (allocated <= 0L) return

    val spent = periodExpenses
      .filter { it.categoryEnum == category }
      .sumOf { it.amount } + newlyAddedAmount
    if (spent <= 0L) return

    val ratio = spent.toDouble() / allocated.toDouble()
    val threshold = when {
      ratio >= 1.0 -> 100
      ratio >= 0.8 -> 80
      else -> return
    }

    val dedupeKey = "${monthKey}_${category.name}_$threshold"
    val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    if (prefs.getBoolean(dedupeKey, false)) return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val granted = ContextCompat.checkSelfPermission(
        appCtx,
        Manifest.permission.POST_NOTIFICATIONS,
      ) == PackageManager.PERMISSION_GRANTED
      if (!granted) return
    }

    val localized = LanguagePreferenceManager.localizedContext(appCtx)
    ensureChannel(localized)

    val currency = CurrencyPreferenceManager.currentCurrency
    val categoryName = category.label(localized)
    val spentFormatted = Formatters.formatMoney(spent, currency)
    val limitFormatted = Formatters.formatMoney(allocated, currency)

    val title = if (threshold >= 100) {
      localized.getString(R.string.budget_alert_100_title, categoryName)
    } else {
      localized.getString(R.string.budget_alert_80_title, categoryName)
    }
    val body = if (threshold >= 100) {
      localized.getString(R.string.budget_alert_100_body, spentFormatted, limitFormatted)
    } else {
      val pct = (ratio * 100).toInt().coerceIn(80, 99)
      localized.getString(R.string.budget_alert_80_body, pct, spentFormatted, limitFormatted)
    }

    val launchIntent = Intent(appCtx, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
      appCtx,
      BASE_NOTIFICATION_ID + category.ordinal * 10 + (if (threshold >= 100) 1 else 0),
      launchIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val notification = NotificationCompat.Builder(localized, CHANNEL_ID)
      .setSmallIcon(R.mipmap.ic_launcher_round)
      .setContentTitle(title)
      .setContentText(body)
      .setStyle(NotificationCompat.BigTextStyle().bigText(body))
      .setPriority(
        if (threshold >= 100) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT
      )
      .setContentIntent(pendingIntent)
      .setAutoCancel(true)
      .build()

    runCatching {
      NotificationManagerCompat.from(localized).notify(
        BASE_NOTIFICATION_ID + category.ordinal * 10 + (if (threshold >= 100) 1 else 0),
        notification,
      )
      prefs.edit().putBoolean(dedupeKey, true).apply()
    }
  }

  fun ensureChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val localized = LanguagePreferenceManager.localizedContext(context)
      val manager = localized.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      if (manager.getNotificationChannel(CHANNEL_ID) == null) {
        val channel = NotificationChannel(
          CHANNEL_ID,
          localized.getString(R.string.budget_alert_channel_name),
          NotificationManager.IMPORTANCE_HIGH,
        ).apply {
          description = localized.getString(R.string.budget_alert_channel_desc)
        }
        manager.createNotificationChannel(channel)
      }
    }
  }
}
