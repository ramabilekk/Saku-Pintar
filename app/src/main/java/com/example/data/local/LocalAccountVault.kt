package com.example.data.local

import android.content.Context
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.MonthlyBudget
import com.example.data.model.PaymentMethod
import com.example.data.model.RecurringTemplate
import com.example.data.model.SavingsGoal
import com.example.data.model.SavingsGoalCategory
import com.example.util.Formatters
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

data class LocalVaultSnapshot(
  val expenses: List<Expense>,
  val budgets: Map<String, MonthlyBudget>,
  val recurringTemplates: List<RecurringTemplate>,
  val savingsGoals: List<SavingsGoal>,
  val walletBalances: Map<PaymentMethod, Long>,
) {
  val totalRecordCount: Int
    get() = expenses.size +
      budgets.size +
      recurringTemplates.size +
      savingsGoals.size +
      walletBalances.values.count { it > 0L }

  val hasAnyData: Boolean
    get() = totalRecordCount > 0
}

/**
 * Persistent on-device vault for Guest / Local accounts so user data survives restarts
 * and can be cleanly migrated to Firestore upon connecting a Google account.
 */
object LocalAccountVault {
  private const val PREF_PREFIX = "sakupintar_local_vault_"

  private fun prefs(context: Context, userId: String) =
    context.applicationContext.getSharedPreferences(PREF_PREFIX + userId, Context.MODE_PRIVATE)

  fun loadSnapshot(context: Context, userId: String): LocalVaultSnapshot = LocalVaultSnapshot(
    expenses = loadExpenses(context, userId),
    budgets = loadBudgets(context, userId),
    recurringTemplates = loadRecurring(context, userId),
    savingsGoals = loadGoals(context, userId),
    walletBalances = loadWallets(context, userId),
  )

  fun clearVault(context: Context, userId: String) {
    prefs(context, userId).edit().clear().apply()
  }

  fun loadExpenses(context: Context, userId: String): List<Expense> {
    val raw = prefs(context, userId).getString("expenses", null) ?: return emptyList()
    return runCatching {
      val arr = JSONArray(raw)
      buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.optJSONObject(i) ?: continue
          add(
            Expense(
              id = obj.optString("id"),
              userId = userId,
              title = obj.optString("title"),
              amount = obj.optLong("amount", 0L),
              category = obj.optString("category", ExpenseCategory.FOOD.name),
              paymentMethod = obj.optString("paymentMethod", PaymentMethod.QRIS.name),
              dateEpochDay = obj.optLong("dateEpochDay", LocalDate.now().toEpochDay()),
              monthKey = obj.optString("monthKey", Formatters.currentMonthKey()),
              note = obj.optString("note", ""),
              receiptPhotoBase64 = obj.optString("receiptPhotoBase64", ""),
            )
          )
        }
      }
    }.getOrDefault(emptyList())
  }

  fun saveExpenses(context: Context, userId: String, list: List<Expense>) {
    val arr = JSONArray()
    list.forEach { item ->
      arr.put(
        JSONObject().apply {
          put("id", item.id)
          put("title", item.title)
          put("amount", item.amount)
          put("category", item.category)
          put("paymentMethod", item.paymentMethod)
          put("dateEpochDay", item.dateEpochDay)
          put("monthKey", item.monthKey)
          put("note", item.note)
          put("receiptPhotoBase64", item.receiptPhotoBase64)
        }
      )
    }
    prefs(context, userId).edit().putString("expenses", arr.toString()).apply()
  }

  fun loadBudgets(context: Context, userId: String): Map<String, MonthlyBudget> {
    val raw = prefs(context, userId).getString("budgets", null) ?: return emptyMap()
    return runCatching {
      val arr = JSONArray(raw)
      buildMap {
        for (i in 0 until arr.length()) {
          val obj = arr.optJSONObject(i) ?: continue
          val monthKey = obj.optString("monthKey")
          if (monthKey.isBlank()) continue
          val catObj = obj.optJSONObject("categoryBudgets")
          val catMap = mutableMapOf<String, Long>()
          if (catObj != null) {
            catObj.keys().forEach { k -> catMap[k] = catObj.optLong(k, 0L) }
          }
          put(
            monthKey,
            MonthlyBudget(
              userId = userId,
              monthKey = monthKey,
              monthlyIncome = obj.optLong("monthlyIncome", 0L),
              strategy = obj.optString("strategy", "BALANCED_50_30_20"),
              savingsTarget = obj.optLong("savingsTarget", 0L),
              needsPercent = obj.optLong("needsPercent", 50L),
              wantsPercent = obj.optLong("wantsPercent", 30L),
              savingsPercent = obj.optLong("savingsPercent", 20L),
              categoryBudgets = catMap,
            )
          )
        }
      }
    }.getOrDefault(emptyMap())
  }

  fun saveBudgets(context: Context, userId: String, map: Map<String, MonthlyBudget>) {
    val arr = JSONArray()
    map.values.forEach { b ->
      val catObj = JSONObject()
      b.categoryBudgets.forEach { (k, v) -> catObj.put(k, v) }
      arr.put(
        JSONObject().apply {
          put("monthKey", b.monthKey)
          put("monthlyIncome", b.monthlyIncome)
          put("strategy", b.strategy)
          put("savingsTarget", b.savingsTarget)
          put("needsPercent", b.needsPercent)
          put("wantsPercent", b.wantsPercent)
          put("savingsPercent", b.savingsPercent)
          put("categoryBudgets", catObj)
        }
      )
    }
    prefs(context, userId).edit().putString("budgets", arr.toString()).apply()
  }

  fun loadRecurring(context: Context, userId: String): List<RecurringTemplate> {
    val raw = prefs(context, userId).getString("recurring", null) ?: return emptyList()
    return runCatching {
      val arr = JSONArray(raw)
      buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.optJSONObject(i) ?: continue
          add(
            RecurringTemplate(
              id = obj.optString("id"),
              userId = userId,
              title = obj.optString("title"),
              amount = obj.optLong("amount", 0L),
              category = obj.optString("category", ExpenseCategory.BILLS.name),
              paymentMethod = obj.optString("paymentMethod", PaymentMethod.TRANSFER.name),
              note = obj.optString("note", ""),
            )
          )
        }
      }
    }.getOrDefault(emptyList())
  }

  fun saveRecurring(context: Context, userId: String, list: List<RecurringTemplate>) {
    val arr = JSONArray()
    list.forEach { item ->
      arr.put(
        JSONObject().apply {
          put("id", item.id)
          put("title", item.title)
          put("amount", item.amount)
          put("category", item.category)
          put("paymentMethod", item.paymentMethod)
          put("note", item.note)
        }
      )
    }
    prefs(context, userId).edit().putString("recurring", arr.toString()).apply()
  }

  fun loadGoals(context: Context, userId: String): List<SavingsGoal> {
    val raw = prefs(context, userId).getString("goals", null) ?: return emptyList()
    return runCatching {
      val arr = JSONArray(raw)
      buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.optJSONObject(i) ?: continue
          add(
            SavingsGoal(
              id = obj.optString("id"),
              userId = userId,
              title = obj.optString("title"),
              targetAmount = obj.optLong("targetAmount", 0L),
              savedAmount = obj.optLong("savedAmount", 0L),
              category = obj.optString("category", SavingsGoalCategory.EMERGENCY.name),
              note = obj.optString("note", ""),
            )
          )
        }
      }
    }.getOrDefault(emptyList())
  }

  fun saveGoals(context: Context, userId: String, list: List<SavingsGoal>) {
    val arr = JSONArray()
    list.forEach { g ->
      arr.put(
        JSONObject().apply {
          put("id", g.id)
          put("title", g.title)
          put("targetAmount", g.targetAmount)
          put("savedAmount", g.savedAmount)
          put("category", g.category)
          put("note", g.note)
        }
      )
    }
    prefs(context, userId).edit().putString("goals", arr.toString()).apply()
  }

  fun loadWallets(context: Context, userId: String): Map<PaymentMethod, Long> {
    val raw = prefs(context, userId).getString("wallets", null) ?: return emptyMap()
    return runCatching {
      val obj = JSONObject(raw)
      PaymentMethod.entries.associateWith { m -> obj.optLong(m.name, 0L) }
    }.getOrDefault(emptyMap())
  }

  fun saveWallets(context: Context, userId: String, map: Map<PaymentMethod, Long>) {
    val obj = JSONObject()
    map.forEach { (m, b) -> obj.put(m.name, b) }
    prefs(context, userId).edit().putString("wallets", obj.toString()).apply()
  }
}
