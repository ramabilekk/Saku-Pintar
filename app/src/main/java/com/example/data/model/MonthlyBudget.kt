package com.example.data.model

import androidx.annotation.StringRes
import com.example.R
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class BudgetStrategy(
  val titleId: String,
  val subtitleId: String,
  @StringRes val titleRes: Int,
  @StringRes val subtitleRes: Int,
  val needsPercent: Long,
  val wantsPercent: Long,
  val savingsPercent: Long,
) {
  BALANCED_50_30_20(
    titleId = "Seimbang 50 / 30 / 20",
    subtitleId = "50% Kebutuhan, 30% Gaya Hidup, 20% Tabungan",
    titleRes = R.string.strategy_balanced_title,
    subtitleRes = R.string.strategy_balanced_sub,
    needsPercent = 50L,
    wantsPercent = 30L,
    savingsPercent = 20L,
  ),
  FRUGAL_60_10_30(
    titleId = "Hemat Maksimal 60 / 10 / 30",
    subtitleId = "Fokus kebutuhan pokok & 30% tabungan agresif",
    titleRes = R.string.strategy_frugal_title,
    subtitleRes = R.string.strategy_frugal_sub,
    needsPercent = 60L,
    wantsPercent = 10L,
    savingsPercent = 30L,
  ),
  ADAPTIVE_HISTORY(
    titleId = "Adaptif Berbasis Riwayat",
    subtitleId = "Menyesuaikan alokasi dari pola pengeluaran Anda",
    titleRes = R.string.strategy_adaptive_title,
    subtitleRes = R.string.strategy_adaptive_sub,
    needsPercent = 50L,
    wantsPercent = 30L,
    savingsPercent = 20L,
  ),
  CUSTOM(
    titleId = "Kustom Manual",
    subtitleId = "Atur sendiri batas tiap kategori",
    titleRes = R.string.strategy_custom_title,
    subtitleRes = R.string.strategy_custom_sub,
    needsPercent = 50L,
    wantsPercent = 30L,
    savingsPercent = 20L,
  );

  companion object {
    fun fromKey(key: String): BudgetStrategy =
      entries.firstOrNull { it.name == key } ?: BALANCED_50_30_20
  }
}

@IgnoreExtraProperties
data class MonthlyBudget(
  @DocumentId val id: String = "",
  val userId: String = "",
  val monthKey: String = "",
  val monthlyIncome: Long = 0L,
  val strategy: String = BudgetStrategy.BALANCED_50_30_20.name,
  val savingsTarget: Long = 0L,
  val needsPercent: Long = 50L,
  val wantsPercent: Long = 30L,
  val savingsPercent: Long = 20L,
  val categoryBudgets: Map<String, Long> = emptyMap(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  @get:Exclude
  val strategyEnum: BudgetStrategy
    get() = BudgetStrategy.fromKey(strategy)

  @get:Exclude
  val totalAllocatedSpendBudget: Long
    get() = categoryBudgets.values.sum()

  fun limitFor(category: ExpenseCategory): Long =
    categoryBudgets[category.name] ?: 0L
}
