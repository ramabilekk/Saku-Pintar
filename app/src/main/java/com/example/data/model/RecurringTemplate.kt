package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class RecurringTemplate(
  @DocumentId val id: String = "",
  val userId: String = "",
  val title: String = "",
  val amount: Long = 0L,
  val category: String = ExpenseCategory.BILLS.name,
  val paymentMethod: String = PaymentMethod.TRANSFER.name,
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
