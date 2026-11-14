package com.example.data.budget

import com.example.data.model.BudgetGroup
import com.example.data.model.BudgetStrategy
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.MonthlyBudget
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToLong

enum class BudgetPaceStatus {
  SAFE,
  CAUTION,
  OVER,
}

data class DailyBudgetInsight(
  val totalBudget: Long,
  val totalSpent: Long,
  val remainingBudget: Long,
  val daysInMonth: Int,
  val daysRemaining: Int,
  val dailySafeLimit: Long,
  val todaySpent: Long,
  val todayRemaining: Long,
  val paceStatus: BudgetPaceStatus,
)

object BudgetAutoComposer {

  fun composeBudget(
    userId: String,
    monthKey: String,
    monthlyIncome: Long,
    strategy: BudgetStrategy,
    historicalExpenses: List<Expense> = emptyList(),
    customNeedsPercent: Long? = null,
    customWantsPercent: Long? = null,
    customSavingsPercent: Long? = null,
  ): MonthlyBudget {
    val sanitizedIncome = monthlyIncome.coerceAtLeast(0L)
    val needsPct = (customNeedsPercent ?: strategy.needsPercent).coerceIn(0L, 100L)
    val wantsPct = (customWantsPercent ?: strategy.wantsPercent).coerceIn(0L, 100L - needsPct)
    val savingsPct = (customSavingsPercent ?: (100L - needsPct - wantsPct)).coerceIn(0L, 100L)

    val normalizedNeedsPct = if (needsPct + wantsPct + savingsPct == 100L) needsPct else strategy.needsPercent
    val normalizedWantsPct = if (needsPct + wantsPct + savingsPct == 100L) wantsPct else strategy.wantsPercent
    val normalizedSavingsPct = 100L - normalizedNeedsPct - normalizedWantsPct

    val savingsTarget = roundToTenThousand((sanitizedIncome * normalizedSavingsPct) / 100.0)
      .coerceAtMost(sanitizedIncome)
    val spendablePool = (sanitizedIncome - savingsTarget).coerceAtLeast(0L)

    val categoryBudgets: Map<String, Long> =
      if (strategy == BudgetStrategy.ADAPTIVE_HISTORY && historicalExpenses.isNotEmpty()) {
        composeAdaptiveCategoryMap(spendablePool, historicalExpenses)
      } else {
        composeProportionalCategoryMap(
          spendablePool = spendablePool,
          needsPercent = normalizedNeedsPct,
          wantsPercent = normalizedWantsPct,
        )
      }

    return MonthlyBudget(
      id = monthKey,
      userId = userId,
      monthKey = monthKey,
      monthlyIncome = sanitizedIncome,
      strategy = strategy.name,
      savingsTarget = savingsTarget,
      needsPercent = normalizedNeedsPct,
      wantsPercent = normalizedWantsPct,
      savingsPercent = normalizedSavingsPct,
      categoryBudgets = categoryBudgets,
    )
  }

  private fun composeProportionalCategoryMap(
    spendablePool: Long,
    needsPercent: Long,
    wantsPercent: Long,
  ): Map<String, Long> {
    if (spendablePool <= 0L) {
      return ExpenseCategory.entries.associate { it.name to 0L }
    }
    val totalSpendPercent = (needsPercent + wantsPercent).coerceAtLeast(1L)
    val needsPool = roundToTenThousand(spendablePool * (needsPercent.toDouble() / totalSpendPercent))
      .coerceAtMost(spendablePool)
    val wantsPool = (spendablePool - needsPool).coerceAtLeast(0L)

    val result = mutableMapOf<String, Long>()
    val needsCategories = ExpenseCategory.entries.filter { it.group == BudgetGroup.NEEDS }
    val wantsCategories = ExpenseCategory.entries.filter { it.group == BudgetGroup.WANTS }

    allocateGroup(needsPool, needsCategories, result)
    allocateGroup(wantsPool, wantsCategories, result)
    return result
  }

  private fun allocateGroup(
    pool: Long,
    categories: List<ExpenseCategory>,
    output: MutableMap<String, Long>,
  ) {
    if (categories.isEmpty()) return
    var allocated = 0L
    categories.forEachIndexed { index, category ->
      if (index == categories.lastIndex) {
        output[category.name] = (pool - allocated).coerceAtLeast(0L)
      } else {
        val share = roundToTenThousand(pool * category.defaultWeight)
          .coerceAtMost((pool - allocated).coerceAtLeast(0L))
        output[category.name] = share
        allocated += share
      }
    }
  }

  private fun composeAdaptiveCategoryMap(
    spendablePool: Long,
    historicalExpenses: List<Expense>,
  ): Map<String, Long> {
    if (spendablePool <= 0L) {
      return ExpenseCategory.entries.associate { it.name to 0L }
    }
    val totalHistorical = historicalExpenses.sumOf { it.amount }.coerceAtLeast(1L).toDouble()
    val byCategory = historicalExpenses.groupBy { it.categoryEnum }
      .mapValues { (_, list) -> list.sumOf { it.amount }.toDouble() }

    // Blend 65% actual spending pattern with 35% balanced baseline so zero-spend categories retain a cushion
    val rawWeights = ExpenseCategory.entries.associateWith { category ->
      val historicalRatio = (byCategory[category] ?: 0.0) / totalHistorical
      val baselineRatio =
        if (category.group == BudgetGroup.NEEDS) category.defaultWeight * 0.625
        else category.defaultWeight * 0.375
      (historicalRatio * 0.65) + (baselineRatio * 0.35)
    }
    val weightSum = rawWeights.values.sum().coerceAtLeast(0.0001)

    val result = mutableMapOf<String, Long>()
    var allocated = 0L
    val entries = ExpenseCategory.entries
    entries.forEachIndexed { index, category ->
      if (index == entries.lastIndex) {
        outputLast(category, spendablePool - allocated, result)
      } else {
        val normalizedWeight = (rawWeights[category] ?: 0.0) / weightSum
        val amount = roundToTenThousand(spendablePool * normalizedWeight)
          .coerceAtMost((spendablePool - allocated).coerceAtLeast(0L))
        result[category.name] = amount
        allocated += amount
      }
    }
    return result
  }

  private fun outputLast(
    category: ExpenseCategory,
    remaining: Long,
    output: MutableMap<String, Long>,
  ) {
    output[category.name] = remaining.coerceAtLeast(0L)
  }

  fun calculateDailyInsight(
    budget: MonthlyBudget?,
    monthExpenses: List<Expense>,
    monthKey: String,
    today: LocalDate = LocalDate.now(),
  ): DailyBudgetInsight {
    val yearMonth = runCatching { YearMonth.parse(monthKey) }.getOrElse { YearMonth.from(today) }
    val daysInMonth = yearMonth.lengthOfMonth()
    val totalBudget = budget?.totalAllocatedSpendBudget ?: 0L
    val totalSpent = monthExpenses.sumOf { it.amount }
    val remaining = totalBudget - totalSpent

    val currentYearMonth = YearMonth.from(today)
    val daysRemaining = when {
      yearMonth.isBefore(currentYearMonth) -> 0
      yearMonth.isAfter(currentYearMonth) -> daysInMonth
      else -> (daysInMonth - today.dayOfMonth + 1).coerceAtLeast(1)
    }

    val todayEpochDay = today.toEpochDay()
    val todaySpent = monthExpenses
      .filter { it.dateEpochDay == todayEpochDay }
      .sumOf { it.amount }

    // Daily safe limit spreads remaining budget (before today's spending) across remaining days
    val remainingAtStartOfDay = (totalBudget - (totalSpent - todaySpent)).coerceAtLeast(0L)
    val dailySafeLimit =
      if (daysRemaining > 0 && totalBudget > 0L) remainingAtStartOfDay / daysRemaining
      else 0L
    val todayRemaining = dailySafeLimit - todaySpent

    val ratio = if (totalBudget > 0L) totalSpent.toDouble() / totalBudget.toDouble() else 0.0
    val status = when {
      totalBudget <= 0L -> BudgetPaceStatus.SAFE
      remaining < 0L -> BudgetPaceStatus.OVER
      ratio >= 0.85 || (dailySafeLimit > 0L && todaySpent > dailySafeLimit) -> BudgetPaceStatus.CAUTION
      else -> BudgetPaceStatus.SAFE
    }

    return DailyBudgetInsight(
      totalBudget = totalBudget,
      totalSpent = totalSpent,
      remainingBudget = remaining,
      daysInMonth = daysInMonth,
      daysRemaining = daysRemaining,
      dailySafeLimit = dailySafeLimit,
      todaySpent = todaySpent,
      todayRemaining = todayRemaining,
      paceStatus = status,
    )
  }

  private fun roundToTenThousand(value: Double): Long {
    if (value <= 0.0) return 0L
    return (value / 10_000.0).roundToLong() * 10_000L
  }
}
