package com.example.data.budget

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

data class PaydayCycleRange(
  val monthKey: String,
  val startDate: LocalDate,
  val endDate: LocalDate,
  val paydayStartDay: Int,
) {
  val totalDays: Int
    get() = (ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1).coerceAtLeast(1)

  val isCustomCycle: Boolean
    get() = paydayStartDay > 1

  fun contains(epochDay: Long): Boolean =
    epochDay in startDate.toEpochDay()..endDate.toEpochDay()

  fun daysRemainingFrom(today: LocalDate = LocalDate.now()): Int = when {
    today.isAfter(endDate) -> 0
    today.isBefore(startDate) -> totalDays
    else -> (ChronoUnit.DAYS.between(today, endDate).toInt() + 1).coerceAtLeast(1)
  }

  fun formatRangeLabel(locale: Locale = Locale.getDefault()): String {
    val fmt = DateTimeFormatter.ofPattern("d MMM", locale)
    return "${startDate.format(fmt)} – ${endDate.format(fmt)}"
  }
}

/**
 * Manages the user's custom Payday Cycle start day (1..28).
 * - Day 1 = standard calendar month (e.g. 1 Nov – 30 Nov for "2026-11").
 * - Day N (>1, e.g. 25) = paycheck cycle spanning prev month day N through current month day N-1
 *   (e.g. 25 Oct – 24 Nov for "2026-11"), so daily safe limits align accurately with real payday.
 */
object PaydayCycleManager {
  private const val PREFS_NAME = "sakupintar_payday_cycle"
  private const val KEY_PAYDAY_START_DAY = "payday_start_day"

  private val _paydayStartDayFlow = MutableStateFlow(1)
  private var initialized = false

  val currentPaydayStartDay: Int
    get() = _paydayStartDayFlow.value

  fun ensureInitialized(context: Context) {
    if (initialized) return
    synchronized(this) {
      if (initialized) return
      val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      val saved = prefs.getInt(KEY_PAYDAY_START_DAY, 1).coerceIn(1, 28)
      _paydayStartDayFlow.value = saved
      initialized = true
    }
  }

  fun observePaydayStartDay(context: Context): StateFlow<Int> {
    ensureInitialized(context)
    return _paydayStartDayFlow.asStateFlow()
  }

  fun setPaydayStartDay(context: Context, day: Int) {
    val clamped = day.coerceIn(1, 28)
    val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putInt(KEY_PAYDAY_START_DAY, clamped).apply()
    initialized = true
    _paydayStartDayFlow.value = clamped
  }

  fun resolveCycle(
    monthKey: String,
    paydayStartDay: Int = currentPaydayStartDay,
    fallbackDate: LocalDate = LocalDate.now(),
  ): PaydayCycleRange {
    val clampedDay = paydayStartDay.coerceIn(1, 28)
    val ym = runCatching { YearMonth.parse(monthKey) }.getOrElse { YearMonth.from(fallbackDate) }
    if (clampedDay <= 1) {
      return PaydayCycleRange(
        monthKey = ym.toString(),
        startDate = ym.atDay(1),
        endDate = ym.atEndOfMonth(),
        paydayStartDay = 1,
      )
    }
    val prevYm = ym.minusMonths(1)
    val startDate = prevYm.atDay(clampedDay.coerceAtMost(prevYm.lengthOfMonth()))
    val endDate = ym.atDay((clampedDay - 1).coerceIn(1, ym.lengthOfMonth()))
    return PaydayCycleRange(
      monthKey = ym.toString(),
      startDate = startDate,
      endDate = endDate,
      paydayStartDay = clampedDay,
    )
  }

  fun currentCycleMonthKey(
    today: LocalDate = LocalDate.now(),
    paydayStartDay: Int = currentPaydayStartDay,
  ): String {
    val clampedDay = paydayStartDay.coerceIn(1, 28)
    val ym = YearMonth.from(today)
    return if (clampedDay > 1 && today.dayOfMonth >= clampedDay) {
      ym.plusMonths(1).toString()
    } else {
      ym.toString()
    }
  }
}
