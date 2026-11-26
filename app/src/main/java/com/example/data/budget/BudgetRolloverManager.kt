package com.example.data.budget

import android.content.Context
import com.example.data.model.Expense
import com.example.data.model.MonthlyBudget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.YearMonth

/**
 * Manages the optional Budget Rollover feature, carrying positive unused spendable budget
 * from the previous period into the active period's spendable budget and daily safe limit.
 */
object BudgetRolloverManager {
  private const val PREFS_NAME = "saku_pintar_rollover_prefs"
  private const val KEY_ROLLOVER_ENABLED = "budget_rollover_enabled"

  private val _rolloverEnabled = MutableStateFlow(false)
  private var initialized = false

  fun observeRolloverEnabled(context: Context): StateFlow<Boolean> {
    ensureLoaded(context)
    return _rolloverEnabled.asStateFlow()
  }

  fun isRolloverEnabled(context: Context): Boolean {
    ensureLoaded(context)
    return _rolloverEnabled.value
  }

  fun setRolloverEnabled(context: Context, enabled: Boolean) {
    val appCtx = context.applicationContext
    appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putBoolean(KEY_ROLLOVER_ENABLED, enabled)
      .apply()
    initialized = true
    _rolloverEnabled.value = enabled
  }

  private fun ensureLoaded(context: Context) {
    if (initialized) return
    val appCtx = context.applicationContext
    val saved = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .getBoolean(KEY_ROLLOVER_ENABLED, false)
    _rolloverEnabled.value = saved
    initialized = true
  }

  fun previousMonthKey(monthKey: String): String {
    val current = runCatching { YearMonth.parse(monthKey) }.getOrElse { YearMonth.now() }
    return current.minusMonths(1).toString()
  }

  /**
   * Computes positive unused spendable budget from the previous period.
   * Returns 0L when there is no previous budget or when the previous period overspent.
   */
  fun computeRolloverAmount(
    monthKey: String,
    paydayStartDay: Int,
    previousBudget: MonthlyBudget?,
    allExpenses: List<Expense>,
  ): Long {
    val prevBudget = previousBudget ?: return 0L
    val prevAllocated = prevBudget.totalAllocatedSpendBudget
    if (prevAllocated <= 0L) return 0L

    val prevKey = previousMonthKey(monthKey)
    val prevSpent = if (paydayStartDay > 1) {
      val prevCycle = PaydayCycleManager.resolveCycle(prevKey, paydayStartDay)
      allExpenses.filter { prevCycle.contains(it.dateEpochDay) }.sumOf { it.amount }
    } else {
      allExpenses.filter { it.monthKey == prevKey }.sumOf { it.amount }
    }

    return (prevAllocated - prevSpent).coerceAtLeast(0L)
  }
}
