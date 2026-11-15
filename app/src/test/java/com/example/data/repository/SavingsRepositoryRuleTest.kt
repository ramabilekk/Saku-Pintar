package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.PaymentMethod
import com.example.data.model.SavingsGoalCategory
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class SavingsRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun savingsGoalLifecycle_ownerCreateDepositWithdrawDelete_succeeds() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = SavingsRepository(firestore, auth)
    val title = "Dana Darurat-${UUID.randomUUID().toString().take(5)}"

    val goalId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createSavingsGoal(
        title = title,
        targetAmount = 5_000_000L,
        initialSavedAmount = 500_000L,
        category = SavingsGoalCategory.EMERGENCY,
        note = "Target awal",
      )
    }
    assertTrue(goalId.isNotBlank())

    val createdList = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeSavingsGoals(uid).first { list -> list.any { it.id == goalId } }
    }
    val created = createdList.first { it.id == goalId }
    assertEquals(500_000L, created.savedAmount)

    val afterDeposit = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.adjustSavingsGoalAmount(goalId, 250_000L)
    }
    assertEquals(750_000L, afterDeposit)

    val afterWithdraw = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.adjustSavingsGoalAmount(goalId, -150_000L)
    }
    assertEquals(600_000L, afterWithdraw)

    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.deleteSavingsGoal(goalId)
    }
    val afterDelete = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeSavingsGoals(uid).first { list -> list.none { it.id == goalId } }
    }
    assertTrue(afterDelete.none { it.id == goalId })
  }

  @Test
  fun walletBalanceLifecycle_andCrossUserRejection_succeeds() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = SavingsRepository(firestore, auth)

    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.setWalletBalance(PaymentMethod.QRIS, 1_250_000L)
    }
    val aliceWallets = withTimeout(FLOW_TIMEOUT_MS) {
      aliceRepo.observeWalletBalances(aliceUid).first { list ->
        list.any { it.paymentMethodEnum == PaymentMethod.QRIS && it.balance == 1_250_000L }
      }
    }
    assertEquals(1_250_000L, aliceWallets.first { it.paymentMethodEnum == PaymentMethod.QRIS }.balance)

    signInTestUser(BOB_EMAIL)
    val bobRepo = SavingsRepository(firestore, auth)
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        bobRepo.observeSavingsGoals(aliceUid).first()
      }
      fail("Expected PERMISSION_DENIED when Bob queries Alice's savingsGoals")
    } catch (e: Exception) {
      val fsError = checkNotNull(e.unwrapFirestoreException()) {
        "Expected FirebaseFirestoreException, got $e"
      }
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsError.code)
    }
  }

  private companion object {
    const val ALICE_EMAIL = "alice_savings@test.com"
    const val BOB_EMAIL = "bob_savings@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
