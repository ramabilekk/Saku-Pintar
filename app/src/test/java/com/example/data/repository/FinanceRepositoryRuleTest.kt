package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.budget.BudgetAutoComposer
import com.example.data.model.BudgetStrategy
import com.example.data.model.ExpenseCategory
import com.example.data.model.PaymentMethod
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.LocalDate
import java.util.UUID

class FinanceRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun expenseLifecycle_ownerCreateObserveUpdateDelete_succeeds() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = FinanceRepository(firestore, auth)
    val monthKey = "2026-11"
    val uniqueTitle = "Kopi-${UUID.randomUUID().toString().take(6)}"

    val expenseId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.addExpense(
        title = uniqueTitle,
        amount = 28_000L,
        category = ExpenseCategory.FOOD.name,
        paymentMethod = PaymentMethod.QRIS.name,
        dateEpochDay = LocalDate.of(2026, 11, 13).toEpochDay(),
        monthKey = monthKey,
        note = "Kopi susu",
      )
    }
    assertTrue(expenseId.isNotBlank())

    val expenses = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeExpensesForMonth(uid, monthKey).first { list -> list.any { it.id == expenseId } }
    }
    val created = expenses.first { it.id == expenseId }
    assertEquals(uniqueTitle, created.title)
    assertEquals(28_000L, created.amount)
    assertNotNull(created.createdAt)

    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.updateExpense(created.copy(amount = 35_000L, title = "$uniqueTitle-Edited"))
    }

    val updatedList = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeExpensesForMonth(uid, monthKey).first { list ->
        list.any { it.id == expenseId && it.amount == 35_000L }
      }
    }
    assertEquals("$uniqueTitle-Edited", updatedList.first { it.id == expenseId }.title)

    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.deleteExpense(expenseId)
    }
    val afterDelete = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeExpensesForMonth(uid, monthKey).first { list -> list.none { it.id == expenseId } }
    }
    assertTrue(afterDelete.none { it.id == expenseId })
  }

  @Test
  fun monthlyBudgetLifecycle_autoComposeSaveAndUpdate_succeeds() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = FinanceRepository(firestore, auth)
    val monthKey = "2026-11"

    val composed = BudgetAutoComposer.composeBudget(
      userId = uid,
      monthKey = monthKey,
      monthlyIncome = 10_000_000L,
      strategy = BudgetStrategy.BALANCED_50_30_20,
    )

    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.saveMonthlyBudget(composed)
    }

    val loaded = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeMonthlyBudget(uid, monthKey).first { it != null && it.monthlyIncome == 10_000_000L }
    }
    assertNotNull(loaded)
    assertEquals(2_000_000L, loaded!!.savingsTarget)
    assertEquals(8_000_000L, loaded.totalAllocatedSpendBudget)

    val updated = BudgetAutoComposer.composeBudget(
      userId = uid,
      monthKey = monthKey,
      monthlyIncome = 12_000_000L,
      strategy = BudgetStrategy.FRUGAL_60_10_30,
    )
    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.saveMonthlyBudget(updated)
    }

    val reloaded = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeMonthlyBudget(uid, monthKey).first { it != null && it.monthlyIncome == 12_000_000L }
    }
    assertEquals(3_600_000L, reloaded!!.savingsTarget)
    assertEquals(BudgetStrategy.FRUGAL_60_10_30.name, reloaded.strategy)
  }

  @Test
  fun observeExpenses_crossUserAccess_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = FinanceRepository(firestore, auth)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.addExpense(
        title = "Rahasia Alice",
        amount = 50_000L,
        category = ExpenseCategory.OTHER.name,
        paymentMethod = PaymentMethod.CASH.name,
        dateEpochDay = LocalDate.of(2026, 11, 13).toEpochDay(),
        monthKey = "2026-11",
      )
    }

    signInTestUser(BOB_EMAIL)
    val bobRepo = FinanceRepository(firestore, auth)
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        bobRepo.observeExpensesForMonth(aliceUid, "2026-11").first()
      }
      fail("Expected PERMISSION_DENIED when Bob queries Alice's expenses")
    } catch (e: Exception) {
      val fsError = checkNotNull(e.unwrapFirestoreException()) {
        "Expected FirebaseFirestoreException, got $e"
      }
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsError.code)
    }
  }

  @Test
  fun observeExpenses_unauthenticatedCaller_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repo = FinanceRepository(firestore, auth)
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeExpensesForMonth("alice_uid_placeholder", "2026-11").first()
      }
      fail("Expected PERMISSION_DENIED when unauthenticated")
    } catch (e: Exception) {
      val fsError = checkNotNull(e.unwrapFirestoreException()) {
        "Expected FirebaseFirestoreException, got $e"
      }
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsError.code)
    }
  }

  private companion object {
    const val ALICE_EMAIL = "alice_sakupintar@test.com"
    const val BOB_EMAIL = "bob_sakupintar@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
