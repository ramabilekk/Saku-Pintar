package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class BudgetGroup(val labelId: String) {
  NEEDS("Kebutuhan Pokok"),
  WANTS("Gaya Hidup"),
}

enum class ExpenseCategory(
  val labelId: String,
  val shortLabel: String,
  val group: BudgetGroup,
  val accentColor: Color,
  val defaultWeight: Double,
) {
  FOOD(
    labelId = "Makan & Minum",
    shortLabel = "Makan",
    group = BudgetGroup.NEEDS,
    accentColor = Color(0xFF0F766E),
    defaultWeight = 0.38,
  ),
  BILLS(
    labelId = "Tagihan & Rumah",
    shortLabel = "Tagihan",
    group = BudgetGroup.NEEDS,
    accentColor = Color(0xFF0369A1),
    defaultWeight = 0.32,
  ),
  TRANSPORT(
    labelId = "Transportasi",
    shortLabel = "Transport",
    group = BudgetGroup.NEEDS,
    accentColor = Color(0xFF4338CA),
    defaultWeight = 0.18,
  ),
  HEALTH(
    labelId = "Kesehatan",
    shortLabel = "Sehat",
    group = BudgetGroup.NEEDS,
    accentColor = Color(0xFF059669),
    defaultWeight = 0.12,
  ),
  SHOPPING(
    labelId = "Belanja",
    shortLabel = "Belanja",
    group = BudgetGroup.WANTS,
    accentColor = Color(0xFFD97706),
    defaultWeight = 0.45,
  ),
  ENTERTAINMENT(
    labelId = "Hiburan",
    shortLabel = "Hiburan",
    group = BudgetGroup.WANTS,
    accentColor = Color(0xFFBE185D),
    defaultWeight = 0.35,
  ),
  OTHER(
    labelId = "Lainnya",
    shortLabel = "Lainnya",
    group = BudgetGroup.WANTS,
    accentColor = Color(0xFF57534E),
    defaultWeight = 0.20,
  );

  companion object {
    fun fromKey(key: String): ExpenseCategory =
      entries.firstOrNull { it.name == key } ?: OTHER
  }
}

enum class PaymentMethod(val labelId: String) {
  QRIS("QRIS"),
  CASH("Tunai"),
  TRANSFER("Transfer"),
  CARD("Kartu");

  companion object {
    fun fromKey(key: String): PaymentMethod =
      entries.firstOrNull { it.name == key } ?: QRIS
  }
}

@IgnoreExtraProperties
data class Expense(
  @DocumentId val id: String = "",
  val userId: String = "",
  val title: String = "",
  val amount: Long = 0L,
  val category: String = ExpenseCategory.FOOD.name,
  val paymentMethod: String = PaymentMethod.QRIS.name,
  val dateEpochDay: Long = 0L,
  val monthKey: String = "",
  val note: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  @get:Exclude
  val categoryEnum: ExpenseCategory
    get() = ExpenseCategory.fromKey(category)

  @get:Exclude
  val paymentMethodEnum: PaymentMethod
    get() = PaymentMethod.fromKey(paymentMethod)
}
