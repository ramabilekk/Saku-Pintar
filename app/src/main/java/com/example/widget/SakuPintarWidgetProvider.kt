package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.i18n.AppCurrency
import com.example.i18n.CurrencyPreferenceManager
import com.example.i18n.LanguagePreferenceManager
import com.example.util.Formatters

object WidgetQuickActions {
  const val EXTRA_WIDGET_ACTION = "com.example.EXTRA_WIDGET_ACTION"
  const val ACTION_QUICK_ADD = "quick_add_expense"
  const val ACTION_QUICK_SCAN = "quick_scan_receipt"
}

data class WidgetFinanceSnapshot(
  val dailySafeLimit: Long = 0L,
  val todaySpent: Long = 0L,
  val remainingBudget: Long = 0L,
  val hasBudget: Boolean = false,
  val monthKey: String = Formatters.currentMonthKey(),
)

object WidgetSnapshotStore {
  private const val PREFS_NAME = "sakupintar_widget_snapshot"

  fun updateSnapshot(
    context: Context,
    dailySafeLimit: Long,
    todaySpent: Long,
    remainingBudget: Long,
    hasBudget: Boolean,
    monthKey: String,
  ) {
    val appCtx = context.applicationContext
    appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putLong("dailySafeLimit", dailySafeLimit)
      .putLong("todaySpent", todaySpent)
      .putLong("remainingBudget", remainingBudget)
      .putBoolean("hasBudget", hasBudget)
      .putString("monthKey", monthKey)
      .apply()

    SakuPintarWidgetProvider.refreshAllWidgets(appCtx)
  }

  fun readSnapshot(context: Context): WidgetFinanceSnapshot {
    val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return WidgetFinanceSnapshot(
      dailySafeLimit = prefs.getLong("dailySafeLimit", 0L),
      todaySpent = prefs.getLong("todaySpent", 0L),
      remainingBudget = prefs.getLong("remainingBudget", 0L),
      hasBudget = prefs.getBoolean("hasBudget", false),
      monthKey = prefs.getString("monthKey", null) ?: Formatters.currentMonthKey(),
    )
  }
}

class SakuPintarWidgetProvider : AppWidgetProvider() {

  override fun onUpdate(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetIds: IntArray,
  ) {
    appWidgetIds.forEach { widgetId ->
      updateSingleWidget(context, appWidgetManager, widgetId)
    }
  }

  companion object {
    fun refreshAllWidgets(context: Context) {
      val appCtx = context.applicationContext
      val manager = AppWidgetManager.getInstance(appCtx)
      val ids = manager.getAppWidgetIds(ComponentName(appCtx, SakuPintarWidgetProvider::class.java))
      if (ids.isEmpty()) return
      ids.forEach { widgetId ->
        updateSingleWidget(appCtx, manager, widgetId)
      }
    }

    private fun updateSingleWidget(
      context: Context,
      appWidgetManager: AppWidgetManager,
      appWidgetId: Int,
    ) {
      val localized = LanguagePreferenceManager.localizedContext(context)
      val currency: AppCurrency = CurrencyPreferenceManager.currentCurrency
      val snapshot = WidgetSnapshotStore.readSnapshot(context)

      val views = RemoteViews(context.packageName, R.layout.widget_sakupintar)

      views.setTextViewText(
        R.id.widget_brand_title,
        localized.getString(R.string.app_name),
      )
      views.setTextViewText(
        R.id.widget_month_label,
        Formatters.formatMonthTitle(snapshot.monthKey, LanguagePreferenceManager.currentLanguage.toLocale()),
      )
      views.setTextViewText(
        R.id.widget_safe_limit_label,
        localized.getString(R.string.widget_safe_limit_label),
      )
      views.setTextViewText(
        R.id.widget_safe_limit_value,
        if (snapshot.hasBudget) {
          Formatters.formatMoney(snapshot.dailySafeLimit, currency)
        } else {
          Formatters.formatMoney(snapshot.todaySpent, currency)
        },
      )
      views.setTextViewText(
        R.id.widget_today_spent,
        localized.getString(
          R.string.widget_today_spent_format,
          Formatters.formatMoney(snapshot.todaySpent, currency),
        ),
      )
      views.setTextViewText(
        R.id.widget_btn_add_expense,
        localized.getString(R.string.widget_action_add),
      )
      views.setTextViewText(
        R.id.widget_btn_scan_receipt,
        localized.getString(R.string.widget_action_scan),
      )

      // Open App on Root Tap
      views.setOnClickPendingIntent(
        R.id.widget_root,
        buildLaunchPendingIntent(context, null, requestCode = 5000 + appWidgetId),
      )

      // Quick Add Expense Button
      views.setOnClickPendingIntent(
        R.id.widget_btn_add_expense,
        buildLaunchPendingIntent(
          context,
          WidgetQuickActions.ACTION_QUICK_ADD,
          requestCode = 5100 + appWidgetId,
        ),
      )

      // Quick AI Receipt Scan Button
      views.setOnClickPendingIntent(
        R.id.widget_btn_scan_receipt,
        buildLaunchPendingIntent(
          context,
          WidgetQuickActions.ACTION_QUICK_SCAN,
          requestCode = 5200 + appWidgetId,
        ),
      )

      appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun buildLaunchPendingIntent(
      context: Context,
      action: String?,
      requestCode: Int,
    ): PendingIntent {
      val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        if (action != null) {
          putExtra(WidgetQuickActions.EXTRA_WIDGET_ACTION, action)
        }
      }
      return PendingIntent.getActivity(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    }
  }
}
