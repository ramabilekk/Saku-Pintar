package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.ai.AiFinancialInsight
import com.example.data.ai.AiParsedExpense
import com.example.data.ai.FinanceAiAssistant
import com.example.data.budget.BudgetAutoComposer
import com.example.data.budget.DailyBudgetInsight
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.MonthlyBudget
import com.example.data.model.PaymentMethod
import com.example.data.model.RecurringTemplate
import com.example.data.model.UserProfile
import com.example.data.repository.FinanceRepository
import com.example.i18n.CurrencyPreferenceManager
import com.example.i18n.LanguagePreferenceManager
import com.example.util.Formatters
import com.example.util.ImageUtils
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

sealed interface AiInsightUiState {
  data object Idle : AiInsightUiState
  data object Loading : AiInsightUiState
  data class Success(val insight: AiFinancialInsight, val monthKey: String) : AiInsightUiState
  data class Error(val message: String) : AiInsightUiState
}

sealed interface AiExpenseParseState {
  data object Idle : AiExpenseParseState
  data object Loading : AiExpenseParseState
  data class Success(
    val parsed: AiParsedExpense,
    val receiptPhotoBase64: String? = null,
  ) : AiExpenseParseState
  data class Error(val message: String) : AiExpenseParseState
}

data class CategoryProgress(
  val category: ExpenseCategory,
  val allocated: Long,
  val spent: Long,
) {
  val remaining: Long get() = allocated - spent
  val progressFraction: Float
    get() = if (allocated > 0L) (spent.toFloat() / allocated.toFloat()).coerceIn(0f, 1.5f) else 0f
}

data class FinanceDashboardData(
  val monthKey: String,
  val expenses: List<Expense>,
  val filteredExpenses: List<Expense>,
  val allHistoricalExpenses: List<Expense>,
  val recurringTemplates: List<RecurringTemplate>,
  val budget: MonthlyBudget?,
  val dailyInsight: DailyBudgetInsight,
  val categoryBreakdown: List<CategoryProgress>,
  val selectedCategoryFilter: ExpenseCategory?,
  val searchQuery: String,
)

class FinanceViewModel(
  private val repository: FinanceRepository,
  private val currentUserId: String,
  private val aiAssistant: FinanceAiAssistant = FinanceAiAssistant(),
  private val appContext: Context? = null,
) : ViewModel() {

  private fun localizedCtx(): Context? =
    appContext?.let { LanguagePreferenceManager.localizedContext(it) }

  private fun str(@StringRes resId: Int, fallback: String, vararg args: Any): String {
    val ctx = localizedCtx() ?: return if (args.isEmpty()) fallback else String.format(fallback, *args)
    return if (args.isEmpty()) ctx.getString(resId) else ctx.getString(resId, *args)
  }

  private val _selectedMonthKey = MutableStateFlow(Formatters.currentMonthKey())
  val selectedMonthKey: StateFlow<String> = _selectedMonthKey.asStateFlow()

  private val _selectedCategoryFilter = MutableStateFlow<ExpenseCategory?>(null)
  private val _searchQuery = MutableStateFlow("")
  private val _actionStatus = MutableStateFlow<String?>(null)
  val actionStatus: StateFlow<String?> = _actionStatus.asStateFlow()

  private val _isSaving = MutableStateFlow(false)
  val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

  private val _aiInsightState = MutableStateFlow<AiInsightUiState>(AiInsightUiState.Idle)
  val aiInsightState: StateFlow<AiInsightUiState> = _aiInsightState.asStateFlow()

  private val _aiParseState = MutableStateFlow<AiExpenseParseState>(AiExpenseParseState.Idle)
  val aiParseState: StateFlow<AiExpenseParseState> = _aiParseState.asStateFlow()

  val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile(currentUserId)
    .catch { error ->
      val firestoreError = generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
      if (firestoreError == null && error is CancellationException) throw error
      Log.w(TAG, "Error observing user profile", firestoreError ?: error)
      emit(null)
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = null,
    )

  @OptIn(ExperimentalCoroutinesApi::class)
  private val monthExpensesFlow = _selectedMonthKey.flatMapLatest { monthKey ->
    repository.observeExpensesForMonth(currentUserId, monthKey)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  private val monthBudgetFlow = _selectedMonthKey.flatMapLatest { monthKey ->
    repository.observeMonthlyBudget(currentUserId, monthKey)
  }

  private val allExpensesFlow = repository.observeAllExpenses(currentUserId)
  private val recurringTemplatesFlow = repository.observeRecurringTemplates(currentUserId)

  private val filterFlow = combine(_selectedCategoryFilter, _searchQuery) { cat, query ->
    cat to query
  }

  private val baseFinanceFlow = combine(
    _selectedMonthKey,
    monthExpensesFlow,
    monthBudgetFlow,
    allExpensesFlow,
    recurringTemplatesFlow,
  ) { monthKey, expenses, budget, allExpenses, templates ->
    FinanceBundle(
      monthKey = monthKey,
      expenses = expenses,
      budget = budget,
      allExpenses = allExpenses,
      recurringTemplates = templates,
    )
  }

  val uiState: StateFlow<UiState<FinanceDashboardData>> = combine(
    baseFinanceFlow,
    filterFlow,
  ) { bundle, (catFilter, query) ->
    val monthKey = bundle.monthKey
    val expenses = bundle.expenses
    val budget = bundle.budget
    val allExpenses = bundle.allExpenses

    val insight = BudgetAutoComposer.calculateDailyInsight(
      budget = budget,
      monthExpenses = expenses,
      monthKey = monthKey,
    )

    val spentByCategory = expenses.groupBy { it.categoryEnum }
      .mapValues { (_, items) -> items.sumOf { it.amount } }

    val breakdown = ExpenseCategory.entries.map { cat ->
      CategoryProgress(
        category = cat,
        allocated = budget?.limitFor(cat) ?: 0L,
        spent = spentByCategory[cat] ?: 0L,
      )
    }.sortedByDescending { maxOf(it.spent, it.allocated) }

    val ctx = localizedCtx()
    val trimmedQuery = query.trim().lowercase()
    val filtered = expenses.filter { item ->
      val matchesCategory = catFilter == null || item.categoryEnum == catFilter
      val localizedCategory = item.categoryEnum.label(ctx).lowercase()
      val matchesQuery = trimmedQuery.isEmpty() ||
        item.title.lowercase().contains(trimmedQuery) ||
        item.note.lowercase().contains(trimmedQuery) ||
        item.categoryEnum.labelId.lowercase().contains(trimmedQuery) ||
        localizedCategory.contains(trimmedQuery)
      matchesCategory && matchesQuery
    }

    FinanceDashboardData(
      monthKey = monthKey,
      expenses = expenses,
      filteredExpenses = filtered,
      allHistoricalExpenses = allExpenses,
      recurringTemplates = bundle.recurringTemplates,
      budget = budget,
      dailyInsight = insight,
      categoryBreakdown = breakdown,
      selectedCategoryFilter = catFilter,
      searchQuery = query,
    )
  }
    .map<FinanceDashboardData, UiState<FinanceDashboardData>> { UiState.Success(it) }
    .catch { error ->
      val firestoreError = generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
      if (firestoreError == null && error is CancellationException) throw error
      val rootError = firestoreError ?: error
      Log.w(TAG, "Error observing finance data", rootError)
      emit(UiState.Error(rootError.localizedMessage ?: str(R.string.connection_error_title, "Connection issue")))
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = UiState.Loading,
    )

  private data class FinanceBundle(
    val monthKey: String,
    val expenses: List<Expense>,
    val budget: MonthlyBudget?,
    val allExpenses: List<Expense>,
    val recurringTemplates: List<RecurringTemplate>,
  )

  fun shiftMonth(deltaMonths: Long) {
    _selectedMonthKey.value = Formatters.shiftMonthKey(_selectedMonthKey.value, deltaMonths)
    _aiInsightState.value = AiInsightUiState.Idle
  }

  fun selectMonth(monthKey: String) {
    val valid = runCatching { java.time.YearMonth.parse(monthKey).toString() }.getOrNull() ?: return
    _selectedMonthKey.value = valid
    _aiInsightState.value = AiInsightUiState.Idle
  }

  fun setCategoryFilter(category: ExpenseCategory?) {
    _selectedCategoryFilter.value = if (_selectedCategoryFilter.value == category) null else category
  }

  fun setSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun addExpense(
    title: String,
    amount: Long,
    category: ExpenseCategory,
    paymentMethod: PaymentMethod,
    dateEpochDay: Long,
    note: String,
    receiptPhotoBase64: String = "",
  ) {
    if (amount <= 0L || title.isBlank()) {
      _actionStatus.value = str(R.string.vm_expense_validation, "Expense name and amount are required.")
      return
    }
    val date = LocalDate.ofEpochDay(dateEpochDay)
    val expenseMonthKey = java.time.YearMonth.from(date).toString()

    viewModelScope.launch {
      _isSaving.value = true
      try {
        repository.addExpense(
          title = title,
          amount = amount,
          category = category.name,
          paymentMethod = paymentMethod.name,
          dateEpochDay = dateEpochDay,
          monthKey = expenseMonthKey,
          note = note,
          receiptPhotoBase64 = receiptPhotoBase64,
        )
        _selectedMonthKey.value = expenseMonthKey
        _actionStatus.value = str(R.string.vm_expense_added, "Expense recorded!")
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        val err = e.localizedMessage ?: str(R.string.error_generic, "Error")
        _actionStatus.value = str(R.string.vm_expense_add_failed, "Failed to save expense: %1\$s", err)
      } finally {
        _isSaving.value = false
      }
    }
  }

  fun updateExpense(
    existing: Expense,
    title: String,
    amount: Long,
    category: ExpenseCategory,
    paymentMethod: PaymentMethod,
    dateEpochDay: Long,
    note: String,
    receiptPhotoBase64: String = existing.receiptPhotoBase64,
  ) {
    if (amount <= 0L || title.isBlank()) {
      _actionStatus.value = str(R.string.vm_expense_validation, "Expense name and amount are required.")
      return
    }
    val date = LocalDate.ofEpochDay(dateEpochDay)
    val expenseMonthKey = java.time.YearMonth.from(date).toString()

    viewModelScope.launch {
      _isSaving.value = true
      try {
        repository.updateExpense(
          existing.copy(
            title = title,
            amount = amount,
            category = category.name,
            paymentMethod = paymentMethod.name,
            dateEpochDay = dateEpochDay,
            monthKey = expenseMonthKey,
            note = note,
            receiptPhotoBase64 = receiptPhotoBase64,
          )
        )
        _selectedMonthKey.value = expenseMonthKey
        _actionStatus.value = str(R.string.vm_expense_updated, "Record updated.")
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        val err = e.localizedMessage ?: str(R.string.error_generic, "Error")
        _actionStatus.value = str(R.string.vm_expense_update_failed, "Failed to update record: %1\$s", err)
      } finally {
        _isSaving.value = false
      }
    }
  }

  fun deleteExpense(expenseId: String) {
    viewModelScope.launch {
      try {
        repository.deleteExpense(expenseId)
        _actionStatus.value = str(R.string.vm_expense_deleted, "Expense deleted.")
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        val err = e.localizedMessage ?: str(R.string.error_generic, "Error")
        _actionStatus.value = str(R.string.vm_expense_delete_failed, "Failed to delete: %1\$s", err)
      }
    }
  }

  fun saveComposedBudget(budget: MonthlyBudget) {
    viewModelScope.launch {
      _isSaving.value = true
      try {
        repository.saveMonthlyBudget(budget.copy(userId = currentUserId, monthKey = _selectedMonthKey.value))
        _actionStatus.value = str(R.string.vm_budget_saved, "Monthly budget applied!")
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        val err = e.localizedMessage ?: str(R.string.error_generic, "Error")
        _actionStatus.value = str(R.string.vm_budget_save_failed, "Failed to save budget: %1\$s", err)
      } finally {
        _isSaving.value = false
      }
    }
  }

  fun generateMonthlyAiInsight() {
    val current = uiState.value as? UiState.Success ?: return
    val dashboard = current.data
    viewModelScope.launch {
      _aiInsightState.value = AiInsightUiState.Loading
      try {
        val insight = aiAssistant.analyzeMonthlyFinances(
          monthKey = dashboard.monthKey,
          expenses = dashboard.expenses,
          budget = dashboard.budget,
          dailyInsight = dashboard.dailyInsight,
          language = LanguagePreferenceManager.currentLanguage,
          currency = CurrencyPreferenceManager.currentCurrency,
        )
        _aiInsightState.value = AiInsightUiState.Success(
          insight = insight,
          monthKey = dashboard.monthKey,
        )
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _aiInsightState.value = AiInsightUiState.Error(
          e.localizedMessage ?: str(R.string.error_generic, "Failed to analyze finances with Gemini AI."),
        )
      }
    }
  }

  fun parseExpenseWithAi(rawInput: String) {
    viewModelScope.launch {
      _aiParseState.value = AiExpenseParseState.Loading
      try {
        val parsed = aiAssistant.parseNaturalExpense(
          rawInput = rawInput,
          language = LanguagePreferenceManager.currentLanguage,
          currency = CurrencyPreferenceManager.currentCurrency,
        )
        _aiParseState.value = AiExpenseParseState.Success(parsed)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _aiParseState.value = AiExpenseParseState.Error(
          e.localizedMessage ?: str(R.string.error_generic, "Failed to parse expense text."),
        )
      }
    }
  }

  fun parseReceiptUriWithAi(appContext: Context, uri: Uri) {
    viewModelScope.launch {
      _aiParseState.value = AiExpenseParseState.Loading
      try {
        val bitmap = ImageUtils.decodeScaledBitmap(appContext, uri)
        val encodedReceipt = runCatching { ImageUtils.encodeReceiptBitmapBase64(bitmap) }.getOrNull()
        val parsed = aiAssistant.parseReceiptImage(
          bitmap = bitmap,
          language = LanguagePreferenceManager.currentLanguage,
          currency = CurrencyPreferenceManager.currentCurrency,
        )
        _aiParseState.value = AiExpenseParseState.Success(
          parsed = parsed,
          receiptPhotoBase64 = encodedReceipt,
        )
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _aiParseState.value = AiExpenseParseState.Error(
          e.localizedMessage ?: str(R.string.error_generic, "Failed to scan receipt photo."),
        )
      }
    }
  }

  fun parseReceiptBase64WithAi(receiptPhotoBase64: String) {
    if (receiptPhotoBase64.isBlank()) return
    viewModelScope.launch {
      _aiParseState.value = AiExpenseParseState.Loading
      try {
        val bitmap = ImageUtils.decodeAvatarBitmap(receiptPhotoBase64)
          ?: error(str(R.string.error_generic, "Failed to read receipt image."))
        val parsed = aiAssistant.parseReceiptImage(
          bitmap = bitmap,
          language = LanguagePreferenceManager.currentLanguage,
          currency = CurrencyPreferenceManager.currentCurrency,
        )
        _aiParseState.value = AiExpenseParseState.Success(
          parsed = parsed,
          receiptPhotoBase64 = receiptPhotoBase64,
        )
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _aiParseState.value = AiExpenseParseState.Error(
          e.localizedMessage ?: str(R.string.error_generic, "Failed to scan receipt photo."),
        )
      }
    }
  }

  fun addRecurringTemplate(
    title: String,
    amount: Long,
    category: ExpenseCategory,
    paymentMethod: PaymentMethod,
    note: String,
  ) {
    if (amount <= 0L || title.isBlank()) {
      _actionStatus.value = str(R.string.vm_expense_validation, "Name and amount are required.")
      return
    }
    viewModelScope.launch {
      _isSaving.value = true
      try {
        repository.addRecurringTemplate(
          title = title,
          amount = amount,
          category = category.name,
          paymentMethod = paymentMethod.name,
          note = note,
        )
        _actionStatus.value = str(R.string.vm_recurring_added, "Recurring template added!")
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        val err = e.localizedMessage ?: str(R.string.error_generic, "Error")
        _actionStatus.value = str(R.string.vm_expense_add_failed, "Failed to save template: %1\$s", err)
      } finally {
        _isSaving.value = false
      }
    }
  }

  fun logRecurringTemplate(template: RecurringTemplate) {
    val today = LocalDate.now()
    addExpense(
      title = template.title,
      amount = template.amount,
      category = template.categoryEnum,
      paymentMethod = template.paymentMethodEnum,
      dateEpochDay = today.toEpochDay(),
      note = template.note.ifBlank { str(R.string.recurring_section_title, "Recurring Expense") },
    )
  }

  fun deleteRecurringTemplate(templateId: String) {
    viewModelScope.launch {
      try {
        repository.deleteRecurringTemplate(templateId)
        _actionStatus.value = str(R.string.vm_recurring_deleted, "Recurring template deleted.")
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        val err = e.localizedMessage ?: str(R.string.error_generic, "Error")
        _actionStatus.value = str(R.string.vm_expense_delete_failed, "Failed to delete: %1\$s", err)
      }
    }
  }

  fun saveDisplayName(newName: String) {
    val cleanName = newName.trim().take(50)
    if (cleanName.isEmpty()) {
      _actionStatus.value = str(R.string.vm_profile_name_empty, "Display name cannot be empty.")
      return
    }
    viewModelScope.launch {
      _isSaving.value = true
      try {
        repository.saveDisplayName(cleanName)
        _actionStatus.value = str(R.string.vm_profile_name_saved, "Display name updated to \"%1\$s\"!", cleanName)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        val err = e.localizedMessage ?: str(R.string.error_generic, "Error")
        _actionStatus.value = str(R.string.vm_expense_update_failed, "Failed to update name: %1\$s", err)
      } finally {
        _isSaving.value = false
      }
    }
  }

  fun saveProfilePhotoFromUri(appContext: Context, uri: Uri, fallbackDisplayName: String) {
    viewModelScope.launch {
      _isSaving.value = true
      try {
        val base64 = ImageUtils.encodeAvatarBase64(appContext, uri)
        repository.saveProfilePhoto(base64, fallbackDisplayName)
        _actionStatus.value = str(R.string.vm_profile_photo_saved, "Profile photo updated!")
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        val err = e.localizedMessage ?: str(R.string.error_generic, "Error")
        _actionStatus.value = str(R.string.vm_expense_update_failed, "Failed to save photo: %1\$s", err)
      } finally {
        _isSaving.value = false
      }
    }
  }

  fun saveProfilePhotoFromBitmap(bitmap: android.graphics.Bitmap, fallbackDisplayName: String) {
    viewModelScope.launch {
      _isSaving.value = true
      try {
        val base64 = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
          ImageUtils.encodeAvatarBitmapBase64(bitmap)
        }
        repository.saveProfilePhoto(base64, fallbackDisplayName)
        _actionStatus.value = str(R.string.vm_profile_photo_saved, "Profile photo updated!")
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        val err = e.localizedMessage ?: str(R.string.error_generic, "Error")
        _actionStatus.value = str(R.string.vm_expense_update_failed, "Failed to save photo: %1\$s", err)
      } finally {
        _isSaving.value = false
      }
    }
  }

  fun clearCustomProfilePhoto(fallbackDisplayName: String) {
    viewModelScope.launch {
      _isSaving.value = true
      try {
        repository.saveProfilePhoto("", fallbackDisplayName)
        _actionStatus.value = str(R.string.vm_profile_photo_synced, "Profile photo synced with Google account.")
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        val err = e.localizedMessage ?: str(R.string.error_generic, "Error")
        _actionStatus.value = str(R.string.vm_expense_update_failed, "Failed to sync photo: %1\$s", err)
      } finally {
        _isSaving.value = false
      }
    }
  }

  fun showStatusMessage(message: String) {
    _actionStatus.value = message
  }

  fun resetAiExpenseParser() {
    _aiParseState.value = AiExpenseParseState.Idle
  }

  fun clearActionStatus() {
    _actionStatus.value = null
  }

  private companion object {
    const val TAG = "FinanceVM"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
