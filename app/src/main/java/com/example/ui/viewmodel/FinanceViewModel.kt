package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.budget.BudgetAutoComposer
import com.example.data.budget.DailyBudgetInsight
import com.example.data.model.BudgetStrategy
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.MonthlyBudget
import com.example.data.model.PaymentMethod
import com.example.data.repository.FinanceRepository
import com.example.util.Formatters
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
  val budget: MonthlyBudget?,
  val dailyInsight: DailyBudgetInsight,
  val categoryBreakdown: List<CategoryProgress>,
  val selectedCategoryFilter: ExpenseCategory?,
  val searchQuery: String,
)

class FinanceViewModel(
  private val repository: FinanceRepository,
  private val currentUserId: String,
) : ViewModel() {

  private val _selectedMonthKey = MutableStateFlow(Formatters.currentMonthKey())
  val selectedMonthKey: StateFlow<String> = _selectedMonthKey.asStateFlow()

  private val _selectedCategoryFilter = MutableStateFlow<ExpenseCategory?>(null)
  private val _searchQuery = MutableStateFlow("")
  private val _actionStatus = MutableStateFlow<String?>(null)
  val actionStatus: StateFlow<String?> = _actionStatus.asStateFlow()

  private val _isSaving = MutableStateFlow(false)
  val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

  @OptIn(ExperimentalCoroutinesApi::class)
  private val monthExpensesFlow = _selectedMonthKey.flatMapLatest { monthKey ->
    repository.observeExpensesForMonth(currentUserId, monthKey)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  private val monthBudgetFlow = _selectedMonthKey.flatMapLatest { monthKey ->
    repository.observeMonthlyBudget(currentUserId, monthKey)
  }

  private val allExpensesFlow = repository.observeAllExpenses(currentUserId)

  private val filterFlow = combine(_selectedCategoryFilter, _searchQuery) { cat, query ->
    cat to query
  }

  val uiState: StateFlow<UiState<FinanceDashboardData>> = combine(
    _selectedMonthKey,
    monthExpensesFlow,
    monthBudgetFlow,
    allExpensesFlow,
    filterFlow,
  ) { monthKey, expenses, budget, allExpenses, (catFilter, query) ->
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

    val trimmedQuery = query.trim().lowercase()
    val filtered = expenses.filter { item ->
      val matchesCategory = catFilter == null || item.categoryEnum == catFilter
      val matchesQuery = trimmedQuery.isEmpty() ||
        item.title.lowercase().contains(trimmedQuery) ||
        item.note.lowercase().contains(trimmedQuery) ||
        item.categoryEnum.labelId.lowercase().contains(trimmedQuery)
      matchesCategory && matchesQuery
    }

    FinanceDashboardData(
      monthKey = monthKey,
      expenses = expenses,
      filteredExpenses = filtered,
      allHistoricalExpenses = allExpenses,
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
      emit(UiState.Error(rootError.localizedMessage ?: "Gagal memuat data keuangan."))
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = UiState.Loading,
    )

  fun shiftMonth(deltaMonths: Long) {
    _selectedMonthKey.value = Formatters.shiftMonthKey(_selectedMonthKey.value, deltaMonths)
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
  ) {
    if (amount <= 0L || title.isBlank()) {
      _actionStatus.value = "Nama pengeluaran dan nominal harus diisi."
      return
    }
    val date = java.time.LocalDate.ofEpochDay(dateEpochDay)
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
        )
        _selectedMonthKey.value = expenseMonthKey
        _actionStatus.value = "Pengeluaran tercatat!"
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _actionStatus.value = "Gagal menyimpan pengeluaran: ${e.localizedMessage ?: "Terjadi kesalahan"}"
      } finally {
        _isSaving.value = false
      }
    }
  }

  fun updateExpense(existing: Expense, title: String, amount: Long, category: ExpenseCategory, paymentMethod: PaymentMethod, dateEpochDay: Long, note: String) {
    if (amount <= 0L || title.isBlank()) {
      _actionStatus.value = "Nama pengeluaran dan nominal harus diisi."
      return
    }
    val date = java.time.LocalDate.ofEpochDay(dateEpochDay)
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
          )
        )
        _selectedMonthKey.value = expenseMonthKey
        _actionStatus.value = "Catatan diperbarui."
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _actionStatus.value = "Gagal memperbarui catatan: ${e.localizedMessage ?: "Terjadi kesalahan"}"
      } finally {
        _isSaving.value = false
      }
    }
  }

  fun deleteExpense(expenseId: String) {
    viewModelScope.launch {
      try {
        repository.deleteExpense(expenseId)
        _actionStatus.value = "Pengeluaran dihapus."
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _actionStatus.value = "Gagal menghapus: ${e.localizedMessage ?: "Terjadi kesalahan"}"
      }
    }
  }

  fun saveComposedBudget(budget: MonthlyBudget) {
    viewModelScope.launch {
      _isSaving.value = true
      try {
        repository.saveMonthlyBudget(budget.copy(userId = currentUserId, monthKey = _selectedMonthKey.value))
        _actionStatus.value = "Anggaran bulanan otomatis diterapkan!"
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _actionStatus.value = "Gagal menyimpan anggaran: ${e.localizedMessage ?: "Terjadi kesalahan"}"
      } finally {
        _isSaving.value = false
      }
    }
  }

  fun clearActionStatus() {
    _actionStatus.value = null
  }

  private companion object {
    const val TAG = "FinanceVM"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
