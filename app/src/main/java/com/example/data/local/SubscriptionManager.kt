package com.example.data.local

import android.content.Context
import com.example.data.model.ExpenseCategory
import com.example.data.model.PaymentMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.UUID

enum class SubscriptionCycle {
  WEEKLY,
  MONTHLY,
  YEARLY;

  companion object {
    fun fromKey(key: String): SubscriptionCycle =
      entries.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: MONTHLY
  }
}

data class SubscriptionItem(
  val id: String = UUID.randomUUID().toString(),
  val userId: String = "",
  val name: String,
  val amount: Long,
  val billingCycle: SubscriptionCycle = SubscriptionCycle.MONTHLY,
  val renewalDay: Int = 1,
  val category: ExpenseCategory = ExpenseCategory.ENTERTAINMENT,
  val paymentMethod: PaymentMethod = PaymentMethod.QRIS,
  val active: Boolean = true,
  val note: String = "",
) {
  /** Monthly equivalent cost in IDR for consistent budget & recurring cost aggregation. */
  fun monthlyEquivalent(): Long = when (billingCycle) {
    SubscriptionCycle.WEEKLY -> (amount * 52L) / 12L
    SubscriptionCycle.MONTHLY -> amount
    SubscriptionCycle.YEARLY -> amount / 12L
  }

  /** Annualized cost in IDR. */
  fun yearlyEquivalent(): Long = when (billingCycle) {
    SubscriptionCycle.WEEKLY -> amount * 52L
    SubscriptionCycle.MONTHLY -> amount * 12L
    SubscriptionCycle.YEARLY -> amount
  }

  /** Days until the next renewal day of month (for weekly, capped to 0..6 days). */
  fun daysUntilRenewal(today: LocalDate = LocalDate.now()): Int {
    if (billingCycle == SubscriptionCycle.WEEKLY) {
      val targetDow = ((renewalDay - 1) % 7) + 1
      val diff = (targetDow - today.dayOfWeek.value + 7) % 7
      return diff
    }
    val ym = YearMonth.from(today)
    val clamped = renewalDay.coerceIn(1, ym.lengthOfMonth())
    val thisMonthDate = ym.atDay(clamped)
    val nextDate = if (!thisMonthDate.isBefore(today)) {
      thisMonthDate
    } else {
      val nextMonth = ym.plusMonths(1)
      nextMonth.atDay(renewalDay.coerceIn(1, nextMonth.lengthOfMonth()))
    }
    return ChronoUnit.DAYS.between(today, nextDate).toInt().coerceAtLeast(0)
  }
}

/**
 * Local persistent manager for recurring subscriptions (Pelacak Langganan).
 * Backed by per-user SharedPreferences and seamlessly hydrated from Firestore via [replaceAll].
 */
object SubscriptionManager {
  private const val PREFS_NAME = "sakupintar_subscriptions"
  private const val KEY_PREFIX = "subs_"

  private val flows = mutableMapOf<String, MutableStateFlow<List<SubscriptionItem>>>()

  fun observeSubscriptions(context: Context, userId: String): StateFlow<List<SubscriptionItem>> {
    val key = userKey(userId)
    return synchronized(flows) {
      flows.getOrPut(key) {
        MutableStateFlow(load(context.applicationContext, key))
      }
    }.asStateFlow()
  }

  fun addSubscription(
    context: Context,
    userId: String,
    name: String,
    amount: Long,
    billingCycle: SubscriptionCycle,
    renewalDay: Int,
    category: ExpenseCategory,
    paymentMethod: PaymentMethod,
    note: String = "",
  ): SubscriptionItem? {
    val cleanName = name.trim().take(60)
    if (cleanName.isEmpty() || amount <= 0L) return null
    val item = SubscriptionItem(
      id = "sub_${UUID.randomUUID().toString().replace("-", "").take(12)}",
      userId = userId,
      name = cleanName,
      amount = amount.coerceIn(1L, 100_000_000_000L),
      billingCycle = billingCycle,
      renewalDay = renewalDay.coerceIn(1, 31),
      category = category,
      paymentMethod = paymentMethod,
      active = true,
      note = note.trim().take(160),
    )
    updateList(context, userId) { current -> current + item }
    return item
  }

  fun updateSubscription(
    context: Context,
    userId: String,
    updatedItem: SubscriptionItem,
  ): SubscriptionItem? {
    val cleanName = updatedItem.name.trim().take(60)
    if (cleanName.isEmpty() || updatedItem.amount <= 0L) return null
    val normalized = updatedItem.copy(
      userId = userId,
      name = cleanName,
      amount = updatedItem.amount.coerceIn(1L, 100_000_000_000L),
      renewalDay = updatedItem.renewalDay.coerceIn(1, 31),
      note = updatedItem.note.trim().take(160),
    )
    updateList(context, userId) { current ->
      if (current.any { it.id == normalized.id }) {
        current.map { if (it.id == normalized.id) normalized else it }
      } else {
        current + normalized
      }
    }
    return normalized
  }

  fun toggleActive(context: Context, userId: String, subId: String): SubscriptionItem? {
    var result: SubscriptionItem? = null
    updateList(context, userId) { current ->
      current.map { item ->
        if (item.id == subId) {
          val toggled = item.copy(active = !item.active)
          result = toggled
          toggled
        } else {
          item
        }
      }
    }
    return result
  }

  fun deleteSubscription(context: Context, userId: String, subId: String) {
    updateList(context, userId) { current -> current.filterNot { it.id == subId } }
  }

  fun replaceAll(context: Context, userId: String, items: List<SubscriptionItem>) {
    val key = userKey(userId)
    val sorted = items.sortedWith(
      compareByDescending<SubscriptionItem> { it.active }.thenBy { it.daysUntilRenewal() }
    )
    val flow = synchronized(flows) {
      flows.getOrPut(key) { MutableStateFlow(sorted) }
    }
    flow.value = sorted
    save(context.applicationContext, key, sorted)
  }

  private fun updateList(
    context: Context,
    userId: String,
    transform: (List<SubscriptionItem>) -> List<SubscriptionItem>,
  ) {
    val key = userKey(userId)
    val flow = synchronized(flows) {
      flows.getOrPut(key) { MutableStateFlow(load(context.applicationContext, key)) }
    }
    val updated = transform(flow.value).sortedWith(
      compareByDescending<SubscriptionItem> { it.active }.thenBy { it.daysUntilRenewal() }
    )
    flow.value = updated
    save(context.applicationContext, key, updated)
  }

  private fun userKey(userId: String): String = KEY_PREFIX + userId.ifBlank { "local_guest" }

  private fun load(context: Context, key: String): List<SubscriptionItem> {
    val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .getString(key, null) ?: return emptyList()
    return runCatching {
      val arr = JSONArray(raw)
      buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.optJSONObject(i) ?: continue
          val name = obj.optString("name", "").trim()
          val amount = obj.optLong("amount", 0L)
          if (name.isEmpty() || amount <= 0L) continue
          add(
            SubscriptionItem(
              id = obj.optString("id", UUID.randomUUID().toString()),
              userId = obj.optString("userId", ""),
              name = name,
              amount = amount,
              billingCycle = SubscriptionCycle.fromKey(obj.optString("billingCycle", "MONTHLY")),
              renewalDay = obj.optInt("renewalDay", 1).coerceIn(1, 31),
              category = ExpenseCategory.fromKey(obj.optString("category", "ENTERTAINMENT")),
              paymentMethod = PaymentMethod.fromKey(obj.optString("paymentMethod", "QRIS")),
              active = obj.optBoolean("active", true),
              note = obj.optString("note", ""),
            )
          )
        }
      }
    }.getOrDefault(emptyList())
  }

  private fun save(context: Context, key: String, items: List<SubscriptionItem>) {
    val arr = JSONArray()
    items.forEach { item ->
      arr.put(
        JSONObject().apply {
          put("id", item.id)
          put("userId", item.userId)
          put("name", item.name)
          put("amount", item.amount)
          put("billingCycle", item.billingCycle.name)
          put("renewalDay", item.renewalDay)
          put("category", item.category.name)
          put("paymentMethod", item.paymentMethod.name)
          put("active", item.active)
          put("note", item.note)
        }
      )
    }
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putString(key, arr.toString())
      .apply()
  }
}
