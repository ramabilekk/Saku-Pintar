package com.example.data.budget

import com.example.data.model.BudgetStrategy
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BudgetAutoComposerTest {

  @Test
  fun composeBalancedBudget_allocatesExactIncomeAcrossSavingsAndCategories() {
    val budget = BudgetAutoComposer.composeBudget(
      userId = "u1",
      monthKey = "2026-11",
      monthlyIncome = 10_000_000L,
      strategy = BudgetStrategy.BALANCED_50_30_20,
    )

    assertEquals(2_000_000L, budget.savingsTarget)
    assertEquals(8_000_000L, budget.totalAllocatedSpendBudget)
    assertEquals(50L, budget.needsPercent)
    assertEquals(30L, budget.wantsPercent)
    assertEquals(20L, budget.savingsPercent)
    assertEquals(ExpenseCategory.entries.size, budget.categoryBudgets.size)
  }

  @Test
  fun composeAdaptiveBudget_reflectsHistoricalSpendingPattern() {
    val history = listOf(
      Expense(id = "1", amount = 3_000_000L, category = ExpenseCategory.FOOD.name),
      Expense(id = "2", amount = 500_000L, category = ExpenseCategory.TRANSPORT.name),
    )

    val budget = BudgetAutoComposer.composeBudget(
      userId = "u1",
      monthKey = "2026-11",
      monthlyIncome = 8_000_000L,
      strategy = BudgetStrategy.ADAPTIVE_HISTORY,
      historicalExpenses = history,
    )

    assertEquals(1_600_000L, budget.savingsTarget)
    assertEquals(6_400_000L, budget.totalAllocatedSpendBudget)
    assertTrue(budget.limitFor(ExpenseCategory.FOOD) > budget.limitFor(ExpenseCategory.ENTERTAINMENT))
  }

  @Test
  fun calculateDailyInsight_computesSafeDailyLimitAndStatus() {
    val budget = BudgetAutoComposer.composeBudget(
      userId = "u1",
      monthKey = "2026-11",
      monthlyIncome = 10_000_000L,
      strategy = BudgetStrategy.BALANCED_50_30_20,
    )
    val today = LocalDate.of(2026, 11, 21) // 10 days remaining in Nov (21..30)
    val expenses = listOf(
      Expense(id = "e1", amount = 2_000_000L, dateEpochDay = today.minusDays(5).toEpochDay()),
      Expense(id = "e2", amount = 100_000L, dateEpochDay = today.toEpochDay()),
    )

    val insight = BudgetAutoComposer.calculateDailyInsight(
      budget = budget,
      monthExpenses = expenses,
      monthKey = "2026-11",
      today = today,
    )

    assertEquals(10, insight.daysRemaining)
    // Start of day remaining: 8,000,000 - 2,000,000 = 6,000,000 / 10 days = 600,000/day
    assertEquals(600_000L, insight.dailySafeLimit)
    assertEquals(100_000L, insight.todaySpent)
    assertEquals(500_000L, insight.todayRemaining)
    assertEquals(BudgetPaceStatus.SAFE, insight.paceStatus)
  }
}
