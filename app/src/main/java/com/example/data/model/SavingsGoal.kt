package com.example.data.model

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.example.R
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class SavingsGoalCategory(
  val labelId: String,
  @StringRes val labelRes: Int,
  val accentColor: Color,
) {
  EMERGENCY(labelId = "Dana Darurat", labelRes = R.string.savings_cat_emergency, accentColor = Color(0xFF2563EB)),
  GADGET(labelId = "Gadget & Kerja", labelRes = R.string.savings_cat_gadget, accentColor = Color(0xFF4F46E5)),
  TRAVEL(labelId = "Liburan", labelRes = R.string.savings_cat_travel, accentColor = Color(0xFF0EA5E9)),
  VEHICLE(labelId = "Kendaraan", labelRes = R.string.savings_cat_vehicle, accentColor = Color(0xFFD97706)),
  EDUCATION(labelId = "Pendidikan", labelRes = R.string.savings_cat_education, accentColor = Color(0xFF0284C7)),
  HOME(labelId = "Rumah", labelRes = R.string.savings_cat_home, accentColor = Color(0xFFBE185D)),
  OTHER(labelId = "Lainnya", labelRes = R.string.savings_cat_other, accentColor = Color(0xFF57534E));

  companion object {
    fun fromKey(key: String): SavingsGoalCategory =
      entries.firstOrNull { it.name == key } ?: OTHER
  }
}

@IgnoreExtraProperties
data class SavingsGoal(
  @DocumentId val id: String = "",
  val userId: String = "",
  val title: String = "",
  val targetAmount: Long = 0L,
  val savedAmount: Long = 0L,
  val category: String = SavingsGoalCategory.EMERGENCY.name,
  val note: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  @get:Exclude
  val categoryEnum: SavingsGoalCategory
    get() = SavingsGoalCategory.fromKey(category)

  @get:Exclude
  val progressFraction: Float
    get() = if (targetAmount > 0L) (savedAmount.toFloat() / targetAmount.toFloat()).coerceIn(0f, 1f) else 0f

  @get:Exclude
  val remainingAmount: Long
    get() = (targetAmount - savedAmount).coerceAtLeast(0L)

  @get:Exclude
  val isCompleted: Boolean
    get() = targetAmount > 0L && savedAmount >= targetAmount
}

@IgnoreExtraProperties
data class WalletBalance(
  @DocumentId val id: String = "",
  val userId: String = "",
  val paymentMethod: String = PaymentMethod.CASH.name,
  val balance: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  @get:Exclude
  val paymentMethodEnum: PaymentMethod
    get() = PaymentMethod.fromKey(paymentMethod)
}
