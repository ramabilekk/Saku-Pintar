package com.example.data.model

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.example.R
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class BudgetGroup(
  val labelId: String,
  @StringRes val labelRes: Int,
) {
  NEEDS("Kebutuhan Pokok", R.string.group_needs),
  WANTS("Gaya Hidup", R.string.group_wants);

  fun label(context: Context?): String = context?.getString(labelRes) ?: labelId
}

enum class ExpenseCategory(
  val labelId: String,
  val shortLabel: String,
  @StringRes val labelRes: Int,
  @StringRes val shortLabelRes: Int,
  val group: BudgetGroup,
  val accentColor: Color,
  val defaultWeight: Double,
) {
  FOOD(
    labelId = "Makan & Minum",
    shortLabel = "Makan",
    labelRes = R.string.cat_food,
    shortLabelRes = R.string.cat_food_short,
    group = BudgetGroup.NEEDS,
    accentColor = Color(0xFFEA580C),
    defaultWeight = 0.38,
  ),
  BILLS(
    labelId = "Tagihan & Rumah",
    shortLabel = "Tagihan",
    labelRes = R.string.cat_bills,
    shortLabelRes = R.string.cat_bills_short,
    group = BudgetGroup.NEEDS,
    accentColor = Color(0xFF2563EB),
    defaultWeight = 0.32,
  ),
  TRANSPORT(
    labelId = "Transportasi",
    shortLabel = "Transport",
    labelRes = R.string.cat_transport,
    shortLabelRes = R.string.cat_transport_short,
    group = BudgetGroup.NEEDS,
    accentColor = Color(0xFF4338CA),
    defaultWeight = 0.18,
  ),
  HEALTH(
    labelId = "Kesehatan",
    shortLabel = "Sehat",
    labelRes = R.string.cat_health,
    shortLabelRes = R.string.cat_health_short,
    group = BudgetGroup.NEEDS,
    accentColor = Color(0xFF0284C7),
    defaultWeight = 0.12,
  ),
  SHOPPING(
    labelId = "Belanja",
    shortLabel = "Belanja",
    labelRes = R.string.cat_shopping,
    shortLabelRes = R.string.cat_shopping_short,
    group = BudgetGroup.WANTS,
    accentColor = Color(0xFFD97706),
    defaultWeight = 0.45,
  ),
  ENTERTAINMENT(
    labelId = "Hiburan",
    shortLabel = "Hiburan",
    labelRes = R.string.cat_entertainment,
    shortLabelRes = R.string.cat_entertainment_short,
    group = BudgetGroup.WANTS,
    accentColor = Color(0xFFBE185D),
    defaultWeight = 0.35,
  ),
  OTHER(
    labelId = "Lainnya",
    shortLabel = "Lainnya",
    labelRes = R.string.cat_other,
    shortLabelRes = R.string.cat_other_short,
    group = BudgetGroup.WANTS,
    accentColor = Color(0xFF57534E),
    defaultWeight = 0.20,
  );

  fun label(context: Context?): String = context?.getString(labelRes) ?: labelId

  fun shortLabel(context: Context?): String = context?.getString(shortLabelRes) ?: shortLabel

  companion object {
    fun fromKey(key: String): ExpenseCategory =
      entries.firstOrNull { it.name == key } ?: OTHER
  }
}

enum class PaymentMethod(
  val labelId: String,
  @StringRes val labelRes: Int,
) {
  QRIS("QRIS", R.string.method_qris),
  CASH("Tunai", R.string.method_cash),
  TRANSFER("Transfer", R.string.method_transfer),
  CARD("Kartu", R.string.method_card);

  fun label(context: Context?): String = context?.getString(labelRes) ?: labelId

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
