package com.example.data.local

import android.content.Context
import android.util.Log
import com.example.data.repository.ExtrasCloudSyncRepository
import com.example.data.repository.FinanceRepository
import com.example.data.repository.SavingsRepository
import com.example.notifications.BillDueReminderManager
import kotlinx.coroutines.CancellationException

data class GuestMigrationResult(
  val migratedExpenses: Int = 0,
  val migratedBudgets: Int = 0,
  val migratedTemplates: Int = 0,
  val migratedGoals: Int = 0,
  val migratedWallets: Int = 0,
  val migratedExtras: Int = 0,
  val failedCount: Int = 0,
) {
  val totalMigrated: Int
    get() = migratedExpenses + migratedBudgets + migratedTemplates + migratedGoals + migratedWallets + migratedExtras
}

/**
 * Migrates all locally saved Guest Mode records (expenses, monthly budgets, recurring templates
 * with due dates, savings goals, wallet balances, cashflow, custom categories, savings history,
 * debt records, and custom wallets) into the newly signed-in Google user's Firestore collections
 * while strictly respecting Firestore security rules.
 */
object GuestToGoogleMigrationManager {
  private const val TAG = "GuestMigration"

  suspend fun migrateGuestToGoogle(
    context: Context,
    guestUserId: String,
    financeRepository: FinanceRepository,
    savingsRepository: SavingsRepository,
  ): GuestMigrationResult {
    val appCtx = context.applicationContext
    val snapshot = LocalAccountVault.loadSnapshot(appCtx, guestUserId)
    val guestCashflow = CashflowLedgerManager.observeEntries(appCtx, guestUserId).value
    val guestCategories = CustomCategoryManager.observeCategories(appCtx).value
    val guestSavingsHistory = SavingsHistoryManager.observeEntries(appCtx, guestUserId).value
    val guestDebts = DebtLedgerManager.observeRecords(appCtx, guestUserId).value
    val guestCustomWallets = CustomWalletManager.observeWallets(appCtx, guestUserId).value

    val hasAnyExtras = guestCashflow.isNotEmpty() ||
      guestCategories.isNotEmpty() ||
      guestSavingsHistory.isNotEmpty() ||
      guestDebts.isNotEmpty() ||
      guestCustomWallets.isNotEmpty()

    if (!snapshot.hasAnyData && !hasAnyExtras) {
      LocalAccountVault.clearVault(appCtx, guestUserId)
      return GuestMigrationResult()
    }

    var expensesOk = 0
    var budgetsOk = 0
    var templatesOk = 0
    var goalsOk = 0
    var walletsOk = 0
    var extrasOk = 0
    var failed = 0

    for (expense in snapshot.expenses) {
      if (expense.title.isBlank() || expense.amount <= 0L) continue
      try {
        financeRepository.addExpense(
          title = expense.title,
          amount = expense.amount,
          category = expense.categoryEnum.name,
          paymentMethod = expense.paymentMethodEnum.name,
          dateEpochDay = expense.dateEpochDay.coerceIn(0L, 100_000L),
          monthKey = expense.monthKey,
          note = expense.note,
          receiptPhotoBase64 = expense.receiptPhotoBase64,
        )
        expensesOk++
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.w(TAG, "Failed to migrate expense ${expense.id}", e)
        failed++
      }
    }

    for (budget in snapshot.budgets.values) {
      if (budget.monthKey.length != 7) continue
      try {
        financeRepository.saveMonthlyBudget(budget)
        budgetsOk++
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.w(TAG, "Failed to migrate budget ${budget.monthKey}", e)
        failed++
      }
    }

    for (template in snapshot.recurringTemplates) {
      if (template.title.isBlank() || template.amount <= 0L) continue
      try {
        val oldDue = BillDueReminderManager.getDueSchedule(appCtx, template.id)
        val newTemplateId = financeRepository.addRecurringTemplate(
          title = template.title,
          amount = template.amount,
          category = template.categoryEnum.name,
          paymentMethod = template.paymentMethodEnum.name,
          note = template.note,
        )
        if (oldDue != null && newTemplateId.isNotBlank()) {
          BillDueReminderManager.setDueSchedule(
            context = appCtx,
            templateId = newTemplateId,
            title = template.title,
            dueDayOfMonth = oldDue.dueDayOfMonth,
            reminderEnabled = oldDue.reminderEnabled,
          )
          BillDueReminderManager.removeDueSchedule(appCtx, template.id)
        }
        templatesOk++
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.w(TAG, "Failed to migrate recurring template ${template.id}", e)
        failed++
      }
    }

    for (goal in snapshot.savingsGoals) {
      if (goal.title.isBlank() || goal.targetAmount <= 0L) continue
      try {
        savingsRepository.createSavingsGoal(
          title = goal.title,
          targetAmount = goal.targetAmount,
          initialSavedAmount = goal.savedAmount.coerceAtLeast(0L),
          category = goal.categoryEnum,
          note = goal.note,
        )
        goalsOk++
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.w(TAG, "Failed to migrate savings goal ${goal.id}", e)
        failed++
      }
    }

    for ((method, balance) in snapshot.walletBalances) {
      if (balance <= 0L) continue
      try {
        savingsRepository.setWalletBalance(method, balance)
        walletsOk++
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.w(TAG, "Failed to migrate wallet balance ${method.name}", e)
        failed++
      }
    }

    if (hasAnyExtras) {
      val extrasRepo = ExtrasCloudSyncRepository(appCtx)
      guestCashflow.forEach { entry ->
        if (extrasRepo.upsertCashflowEntry(entry).isSuccess) extrasOk++ else failed++
      }
      guestCategories.forEach { cat ->
        if (extrasRepo.upsertCustomCategory(cat).isSuccess) extrasOk++ else failed++
      }
      guestSavingsHistory.forEach { entry ->
        if (extrasRepo.addSavingsHistoryEntry(entry).isSuccess) extrasOk++ else failed++
      }
      guestDebts.forEach { debt ->
        if (extrasRepo.upsertDebtRecord(debt).isSuccess) extrasOk++ else failed++
      }
      guestCustomWallets.forEach { wallet ->
        if (extrasRepo.upsertCustomWallet(wallet).isSuccess) extrasOk++ else failed++
      }
    }

    if (failed == 0) {
      LocalAccountVault.clearVault(appCtx, guestUserId)
      CashflowLedgerManager.clearAll(appCtx, guestUserId)
      SavingsHistoryManager.clearAll(appCtx, guestUserId)
      DebtLedgerManager.clearAll(appCtx, guestUserId)
      CustomWalletManager.clearAll(appCtx, guestUserId)
    }

    return GuestMigrationResult(
      migratedExpenses = expensesOk,
      migratedBudgets = budgetsOk,
      migratedTemplates = templatesOk,
      migratedGoals = goalsOk,
      migratedWallets = walletsOk,
      migratedExtras = extrasOk,
      failedCount = failed,
    )
  }
}
