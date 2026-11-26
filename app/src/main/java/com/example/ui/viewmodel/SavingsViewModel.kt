package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.local.CashflowLedgerManager
import com.example.data.local.SavingsHistoryManager
import com.example.data.local.SavingsHistoryType
import com.example.data.model.PaymentMethod
import com.example.data.model.SavingsGoal
import com.example.data.model.SavingsGoalCategory
import com.example.data.repository.ExtrasCloudSyncRepository
import com.example.data.repository.SavingsRepository
import com.example.i18n.CurrencyPreferenceManager
import com.example.i18n.LanguagePreferenceManager
import com.example.util.Formatters
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SavingsUiData(
  val goals: List<SavingsGoal> = emptyList(),
  val walletBalances: Map<PaymentMethod, Long> = emptyMap(),
) {
  val totalSaved: Long get() = goals.sumOf { it.savedAmount }
  val totalTarget: Long get() = goals.sumOf { it.targetAmount }
  val totalWalletBalance: Long get() = PaymentMethod.entries.sumOf { walletBalances[it] ?: 0L }
  val overallProgressFraction: Float
    get() = if (totalTarget > 0L) (totalSaved.toFloat() / totalTarget.toFloat()).coerceIn(0f, 1f) else 0f
}

class SavingsViewModel(
  private val repository: SavingsRepository,
  private val currentUserId: String,
  private val appContext: Context? = null,
) : ViewModel() {

  private val extrasSync: ExtrasCloudSyncRepository? = appContext?.let {
    ExtrasCloudSyncRepository(it)
  }

  private fun localizedCtx(): Context? =
    appContext?.let { LanguagePreferenceManager.localizedContext(it) }

  private fun str(@StringRes resId: Int, fallback: String, vararg args: Any): String {
    val ctx = localizedCtx() ?: return if (args.isEmpty()) fallback else String.format(fallback, *args)
    return if (args.isEmpty()) ctx.getString(resId) else ctx.getString(resId, *args)
  }

  private val _actionStatus = MutableStateFlow<String?>(null)
  val actionStatus: StateFlow<String?> = _actionStatus.asStateFlow()

  private val _isSaving = MutableStateFlow(false)
  val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

  val uiState: StateFlow<UiState<SavingsUiData>> = combine(
    repository.observeSavingsGoals(currentUserId),
    repository.observeWalletBalances(currentUserId),
  ) { goals, wallets ->
    val walletMap = PaymentMethod.entries.associateWith { method ->
      wallets.firstOrNull { it.paymentMethodEnum == method }?.balance ?: 0L
    }
    SavingsUiData(
      goals = goals,
      walletBalances = walletMap,
    )
  }
    .map<SavingsUiData, UiState<SavingsUiData>> { UiState.Success(it) }
    .catch { error ->
      val firestoreError = generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
      if (firestoreError == null && error is CancellationException) throw error
      val rootError = firestoreError ?: error
      Log.w(TAG, "Error observing savings data", rootError)
      emit(UiState.Error(rootError.localizedMessage ?: str(R.string.savings_error_title, "Failed to load savings")))
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = UiState.Loading,
    )

  fun createGoal(
    title: String,
    targetAmount: Long,
    initialSavedAmount: Long,
    category: SavingsGoalCategory,
    note: String,
  ) {
    if (title.isBlank() || targetAmount <= 0L) {
      _actionStatus.value = str(R.string.vm_goal_validation, "Goal name and target amount are required.")
      return
    }
    viewModelScope.launch {
      _isSaving.value = true
      try {
        val createdId = repository.createSavingsGoal(
          title = title,
          targetAmount = targetAmount,
          initialSavedAmount = initialSavedAmount,
          category = category,
          note = note,
        )
        if (initialSavedAmount > 0L) {
          appContext?.let { ctx ->
            val recorded = SavingsHistoryManager.recordEvent(
              context = ctx,
              userId = currentUserId,
              goalId = createdId,
              type = SavingsHistoryType.INITIAL,
              amount = initialSavedAmount,
              balanceAfter = initialSavedAmount,
            )
            if (recorded != null) {
              extrasSync?.addSavingsHistoryEntry(recorded)
            }
          }
        }
        _actionStatus.value = str(R.string.vm_goal_created, "Savings goal \"%1\$s\" created!", title.trim())
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        val err = e.localizedMessage ?: str(R.string.error_generic, "Error")
        _actionStatus.value = str(R.string.vm_expense_add_failed, "Failed to save: %1\$s", err)
      } finally {
        _isSaving.value = false
      }
    }
  }

  fun updateGoal(
    goalId: String,
    title: String,
    targetAmount: Long,
    category: SavingsGoalCategory,
    note: String,
  ) {
    if (title.isBlank() || targetAmount <= 0L) {
      _actionStatus.value = str(R.string.vm_goal_validation, "Goal name and target amount are required.")
      return
    }
    viewModelScope.launch {
      _isSaving.value = true
      try {
        repository.updateSavingsGoal(
          goalId = goalId,
          title = title,
          targetAmount = targetAmount,
          category = category,
          note = note,
        )
        _actionStatus.value = str(R.string.savings_goal_updated, "Savings goal \"%1\$s\" updated!", title.trim())
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        val err = e.localizedMessage ?: str(R.string.error_generic, "Error")
        _actionStatus.value = str(R.string.vm_expense_update_failed, "Failed to update: %1\$s", err)
      } finally {
        _isSaving.value = false
      }
    }
  }

  fun depositToGoal(goal: SavingsGoal, amount: Long) {
    if (amount <= 0L) {
      _actionStatus.value = str(R.string.editor_error_amount, "Enter an amount greater than 0.")
      return
    }
    viewModelScope.launch {
      _isSaving.value = true
      try {
        val nextSaved = repository.adjustSavingsGoalAmount(goal.id, amount)
        appContext?.let { ctx ->
          val recorded = SavingsHistoryManager.recordEvent(
            context = ctx,
            userId = currentUserId,
            goalId = goal.id,
            type = SavingsHistoryType.DEPOSIT,
            amount = amount,
            balanceAfter = nextSaved,
          )
          if (recorded != null) {
            extrasSync?.addSavingsHistoryEntry(recorded)
          }
        }
        val statusPrefix = if (goal.targetAmount > 0L && nextSaved >= goal.targetAmount) {
          str(R.string.vm_goal_reached, "Congrats! Goal \"%1\$s\" reached!", goal.title)
        } else {
          val formattedAmount = Formatters.formatMoney(amount, CurrencyPreferenceManager.currentCurrency)
          str(R.string.vm_goal_deposited, "Deposited %1\$s to %2\$s.", formattedAmount, goal.title)
        }
        _actionStatus.value = statusPrefix
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        val err = e.localizedMessage ?: str(R.string.error_generic, "Error")
        _actionStatus.value = str(R.string.vm_expense_update_failed, "Failed to deposit: %1\$s", err)
      } finally {
        _isSaving.value = false
      }
    }
  }

  fun withdrawFromGoal(goal: SavingsGoal, amount: Long) {
    if (amount <= 0L) {
      _actionStatus.value = str(R.string.editor_error_amount, "Enter an amount greater than 0.")
      return
    }
    if (amount > goal.savedAmount) {
      val formattedSaved = Formatters.formatMoney(goal.savedAmount, CurrencyPreferenceManager.currentCurrency)
      _actionStatus.value = str(R.string.savings_tx_exceeds, "Exceeds saved balance (%1\$s)", formattedSaved)
      return
    }
    viewModelScope.launch {
      _isSaving.value = true
      try {
        val nextSaved = repository.adjustSavingsGoalAmount(goal.id, -amount)
        appContext?.let { ctx ->
          val recorded = SavingsHistoryManager.recordEvent(
            context = ctx,
            userId = currentUserId,
            goalId = goal.id,
            type = SavingsHistoryType.WITHDRAW,
            amount = amount,
            balanceAfter = nextSaved,
          )
          if (recorded != null) {
            extrasSync?.addSavingsHistoryEntry(recorded)
          }
        }
        val formattedWithdraw = Formatters.formatMoney(amount, CurrencyPreferenceManager.currentCurrency)
        _actionStatus.value = str(R.string.vm_goal_withdrawn, "Recorded withdrawal of %1\$s from %2\$s.", formattedWithdraw, goal.title)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        val err = e.localizedMessage ?: str(R.string.error_generic, "Error")
        _actionStatus.value = str(R.string.vm_expense_update_failed, "Failed to withdraw: %1\$s", err)
      } finally {
        _isSaving.value = false
      }
    }
  }

  fun deleteGoal(goalId: String) {
    viewModelScope.launch {
      try {
        repository.deleteSavingsGoal(goalId)
        appContext?.let { ctx ->
          SavingsHistoryManager.deleteHistoryForGoal(ctx, currentUserId, goalId)
        }
        _actionStatus.value = str(R.string.vm_goal_deleted, "Savings goal deleted.")
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        val err = e.localizedMessage ?: str(R.string.error_generic, "Error")
        _actionStatus.value = str(R.string.vm_expense_delete_failed, "Failed to delete: %1\$s", err)
      }
    }
  }

  fun setWalletBalance(paymentMethod: PaymentMethod, newBalance: Long) {
    if (newBalance < 0L) {
      _actionStatus.value = str(R.string.editor_error_amount, "Balance cannot be negative.")
      return
    }
    viewModelScope.launch {
      _isSaving.value = true
      try {
        repository.setWalletBalance(paymentMethod, newBalance)
        val methodLabel = paymentMethod.label(localizedCtx())
        val formattedBalance = Formatters.formatMoney(newBalance, CurrencyPreferenceManager.currentCurrency)
        _actionStatus.value = str(R.string.vm_wallet_updated, "%1\$s balance updated: %2\$s", methodLabel, formattedBalance)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        val err = e.localizedMessage ?: str(R.string.error_generic, "Error")
        _actionStatus.value = str(R.string.vm_expense_update_failed, "Failed to save balance: %1\$s", err)
      } finally {
        _isSaving.value = false
      }
    }
  }

  fun adjustWalletBalance(paymentMethod: PaymentMethod, delta: Long) {
    if (delta == 0L) return
    val currentData = (uiState.value as? UiState.Success)?.data ?: return
    val currentBal = currentData.walletBalances[paymentMethod] ?: 0L
    if (delta < 0L && currentBal == 0L) return
    val nextBal = (currentBal + delta).coerceAtLeast(0L)
    viewModelScope.launch {
      runCatching {
        repository.setWalletBalance(paymentMethod, nextBal)
      }.onFailure { err ->
        if (err is CancellationException) throw err
        Log.w(TAG, "Auto wallet adjustment skipped", err)
      }
    }
  }

  fun recordIncomeToWallet(
    title: String,
    amount: Long,
    targetWallet: PaymentMethod,
    note: String,
  ) {
    if (title.isBlank() || amount <= 0L) {
      _actionStatus.value = str(R.string.editor_error_amount, "Enter a valid amount.")
      return
    }
    val currentData = (uiState.value as? UiState.Success)?.data
    val currentBal = currentData?.walletBalances?.get(targetWallet) ?: 0L
    val nextBal = currentBal + amount
    viewModelScope.launch {
      _isSaving.value = true
      try {
        repository.setWalletBalance(targetWallet, nextBal)
        appContext?.let { ctx ->
          val entry = CashflowLedgerManager.recordIncome(
            context = ctx,
            userId = currentUserId,
            title = title,
            amount = amount,
            targetWallet = targetWallet,
            note = note,
          )
          extrasSync?.upsertCashflowEntry(entry)
        }
        val formattedAmt = Formatters.formatMoney(amount, CurrencyPreferenceManager.currentCurrency)
        val walletLabel = targetWallet.label(localizedCtx())
        _actionStatus.value = str(
          R.string.cashflow_income_saved,
          "Income %1\$s added to %2\$s!",
          formattedAmt,
          walletLabel,
        )
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        val err = e.localizedMessage ?: str(R.string.error_generic, "Error")
        _actionStatus.value = str(R.string.vm_expense_add_failed, "Failed to save: %1\$s", err)
      } finally {
        _isSaving.value = false
      }
    }
  }

  fun transferBetweenWallets(
    fromWallet: PaymentMethod,
    toWallet: PaymentMethod,
    amount: Long,
    note: String,
  ) {
    if (fromWallet == toWallet) {
      _actionStatus.value = str(R.string.cashflow_transfer_same_wallet_error, "Choose two different wallets.")
      return
    }
    if (amount <= 0L) {
      _actionStatus.value = str(R.string.editor_error_amount, "Enter an amount greater than 0.")
      return
    }
    val currentData = (uiState.value as? UiState.Success)?.data
    val fromBal = currentData?.walletBalances?.get(fromWallet) ?: 0L
    val toBal = currentData?.walletBalances?.get(toWallet) ?: 0L
    if (amount > fromBal) {
      _actionStatus.value = str(
        R.string.cashflow_transfer_insufficient,
        "Insufficient balance in %1\$s.",
        fromWallet.label(localizedCtx()),
      )
      return
    }
    viewModelScope.launch {
      _isSaving.value = true
      try {
        repository.setWalletBalance(fromWallet, (fromBal - amount).coerceAtLeast(0L))
        repository.setWalletBalance(toWallet, toBal + amount)
        appContext?.let { ctx ->
          val entry = CashflowLedgerManager.recordTransfer(
            context = ctx,
            userId = currentUserId,
            fromWallet = fromWallet,
            toWallet = toWallet,
            amount = amount,
            note = note,
          )
          extrasSync?.upsertCashflowEntry(entry)
        }
        val formattedAmt = Formatters.formatMoney(amount, CurrencyPreferenceManager.currentCurrency)
        _actionStatus.value = str(
          R.string.cashflow_transfer_saved,
          "Transferred %1\$s from %2\$s to %3\$s!",
          formattedAmt,
          fromWallet.label(localizedCtx()),
          toWallet.label(localizedCtx()),
        )
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        val err = e.localizedMessage ?: str(R.string.error_generic, "Error")
        _actionStatus.value = str(R.string.vm_expense_update_failed, "Failed to transfer: %1\$s", err)
      } finally {
        _isSaving.value = false
      }
    }
  }

  fun deleteCashflowEntry(entryId: String) {
    appContext?.let { ctx ->
      CashflowLedgerManager.deleteEntry(ctx, currentUserId, entryId)
      viewModelScope.launch {
        extrasSync?.deleteCashflowEntry(entryId)
      }
      _actionStatus.value = str(R.string.cashflow_entry_deleted, "Cashflow entry removed.")
    }
  }

  fun clearActionStatus() {
    _actionStatus.value = null
  }

  private companion object {
    const val TAG = "SavingsVM"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
