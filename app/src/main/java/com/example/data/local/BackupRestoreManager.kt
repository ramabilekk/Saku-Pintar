package com.example.data.local

import android.content.Context
import android.net.Uri
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.MonthlyBudget
import com.example.data.model.PaymentMethod
import com.example.data.model.RecurringTemplate
import com.example.data.model.SavingsGoal
import com.example.data.model.SavingsGoalCategory
import com.example.util.Formatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

data class FullAppBackup(
  val schemaVersion: Int = 1,
  val exportedAtMillis: Long = System.currentTimeMillis(),
  val expenses: List<Expense> = emptyList(),
  val budgets: List<MonthlyBudget> = emptyList(),
  val recurringTemplates: List<RecurringTemplate> = emptyList(),
  val savingsGoals: List<SavingsGoal> = emptyList(),
  val walletBalances: Map<PaymentMethod, Long> = emptyMap(),
  val cashflowEntries: List<CashflowEntry> = emptyList(),
  val customCategories: List<CustomCategory> = emptyList(),
  val savingsHistory: List<SavingsHistoryEntry> = emptyList(),
  val debtRecords: List<DebtRecord> = emptyList(),
  val customWallets: List<CustomWallet> = emptyList(),
) {
  val totalItemCount: Int
    get() = expenses.size +
      budgets.size +
      recurringTemplates.size +
      savingsGoals.size +
      walletBalances.values.count { it > 0L } +
      cashflowEntries.size +
      customCategories.size +
      savingsHistory.size +
      debtRecords.size +
      customWallets.size
}

/**
 * Zero-permission SAF JSON Backup & Restore utility for both Google and Guest accounts.
 */
object BackupRestoreManager {
  fun suggestedBackupFileName(): String {
    val today = LocalDate.now().toString()
    return "SakuPintar_Backup_$today.json"
  }

  fun toJsonString(backup: FullAppBackup): String {
    val root = JSONObject()
    root.put("app", "SakuPintar")
    root.put("schemaVersion", backup.schemaVersion)
    root.put("exportedAtMillis", backup.exportedAtMillis)

    root.put("expenses", JSONArray().apply {
      backup.expenses.forEach { e ->
        put(JSONObject().apply {
          put("id", e.id)
          put("title", e.title)
          put("amount", e.amount)
          put("category", e.category)
          put("paymentMethod", e.paymentMethod)
          put("dateEpochDay", e.dateEpochDay)
          put("monthKey", e.monthKey)
          put("note", e.note)
        })
      }
    })

    root.put("budgets", JSONArray().apply {
      backup.budgets.forEach { b ->
        put(JSONObject().apply {
          put("monthKey", b.monthKey)
          put("monthlyIncome", b.monthlyIncome)
          put("strategy", b.strategy)
          put("savingsTarget", b.savingsTarget)
          put("needsPercent", b.needsPercent)
          put("wantsPercent", b.wantsPercent)
          put("savingsPercent", b.savingsPercent)
          put("categoryBudgets", JSONObject().apply {
            b.categoryBudgets.forEach { (k, v) -> put(k, v) }
          })
        })
      }
    })

    root.put("recurringTemplates", JSONArray().apply {
      backup.recurringTemplates.forEach { r ->
        put(JSONObject().apply {
          put("id", r.id)
          put("title", r.title)
          put("amount", r.amount)
          put("category", r.category)
          put("paymentMethod", r.paymentMethod)
          put("note", r.note)
        })
      }
    })

    root.put("savingsGoals", JSONArray().apply {
      backup.savingsGoals.forEach { g ->
        put(JSONObject().apply {
          put("id", g.id)
          put("title", g.title)
          put("targetAmount", g.targetAmount)
          put("savedAmount", g.savedAmount)
          put("category", g.category)
          put("note", g.note)
        })
      }
    })

    root.put("walletBalances", JSONObject().apply {
      backup.walletBalances.forEach { (m, bal) -> put(m.name, bal) }
    })

    root.put("cashflowEntries", JSONArray().apply {
      backup.cashflowEntries.forEach { c ->
        put(JSONObject().apply {
          put("id", c.id)
          put("type", c.type.name)
          put("title", c.title)
          put("amount", c.amount)
          put("targetWallet", c.targetWallet.name)
          if (c.sourceWallet != null) put("sourceWallet", c.sourceWallet.name)
          put("dateEpochDay", c.dateEpochDay)
          put("monthKey", c.monthKey)
          put("note", c.note)
        })
      }
    })

    root.put("customCategories", JSONArray().apply {
      backup.customCategories.forEach { cat ->
        put(JSONObject().apply {
          put("id", cat.id)
          put("name", cat.name)
          put("parentCategory", cat.parentCategory.name)
          put("colorHex", cat.colorHex)
        })
      }
    })

    root.put("savingsHistory", JSONArray().apply {
      backup.savingsHistory.forEach { h ->
        put(JSONObject().apply {
          put("id", h.id)
          put("goalId", h.goalId)
          put("type", h.type.name)
          put("amount", h.amount)
          put("balanceAfter", h.balanceAfter)
          put("timestampMillis", h.timestampMillis)
          put("note", h.note)
        })
      }
    })

    root.put("debtRecords", JSONArray().apply {
      backup.debtRecords.forEach { d ->
        put(JSONObject().apply {
          put("id", d.id)
          put("personName", d.personName)
          put("type", d.type.name)
          put("amount", d.amount)
          put("dueDateEpochDay", d.dueDateEpochDay)
          put("settled", d.settled)
          put("note", d.note)
        })
      }
    })

    root.put("customWallets", JSONArray().apply {
      backup.customWallets.forEach { w ->
        put(JSONObject().apply {
          put("id", w.id)
          put("name", w.name)
          put("walletType", w.walletType.name)
          put("balance", w.balance)
          put("colorHex", w.colorHex)
        })
      }
    })

    return root.toString(2)
  }

  fun parseFromJson(raw: String, userId: String): FullAppBackup {
    val root = JSONObject(raw)
    val appTag = root.optString("app", "")
    require(appTag == "SakuPintar" || root.has("expenses") || root.has("savingsGoals")) {
      "Format file cadangan SakuPintar tidak valid."
    }

    val expenses = buildList {
      val arr = root.optJSONArray("expenses") ?: JSONArray()
      for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        val title = o.optString("title").trim()
        val amount = o.optLong("amount", 0L)
        if (title.isEmpty() || amount <= 0L) continue
        add(
          Expense(
            id = o.optString("id").ifBlank { UUID.randomUUID().toString() },
            userId = userId,
            title = title.take(80),
            amount = amount.coerceAtMost(10_000_000_000L),
            category = ExpenseCategory.fromKey(o.optString("category")).name,
            paymentMethod = PaymentMethod.fromKey(o.optString("paymentMethod")).name,
            dateEpochDay = o.optLong("dateEpochDay", LocalDate.now().toEpochDay()),
            monthKey = o.optString("monthKey", Formatters.currentMonthKey()),
            note = o.optString("note", "").take(200),
          )
        )
      }
    }

    val budgets = buildList {
      val arr = root.optJSONArray("budgets") ?: JSONArray()
      for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        val monthKey = o.optString("monthKey", "")
        if (!Regex("^[0-9]{4}-(0[1-9]|1[0-2])$").matches(monthKey)) continue
        val catObj = o.optJSONObject("categoryBudgets")
        val catMap = mutableMapOf<String, Long>()
        if (catObj != null) {
          ExpenseCategory.entries.forEach { cat ->
            if (catObj.has(cat.name)) {
              catMap[cat.name] = catObj.optLong(cat.name, 0L).coerceAtLeast(0L)
            }
          }
        }
        add(
          MonthlyBudget(
            userId = userId,
            monthKey = monthKey,
            monthlyIncome = o.optLong("monthlyIncome", 0L).coerceAtLeast(0L),
            strategy = o.optString("strategy", "BALANCED_50_30_20"),
            savingsTarget = o.optLong("savingsTarget", 0L).coerceAtLeast(0L),
            needsPercent = o.optLong("needsPercent", 50L),
            wantsPercent = o.optLong("wantsPercent", 30L),
            savingsPercent = o.optLong("savingsPercent", 20L),
            categoryBudgets = catMap,
          )
        )
      }
    }

    val recurring = buildList {
      val arr = root.optJSONArray("recurringTemplates") ?: JSONArray()
      for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        val title = o.optString("title").trim()
        val amount = o.optLong("amount", 0L)
        if (title.isEmpty() || amount <= 0L) continue
        add(
          RecurringTemplate(
            id = o.optString("id").ifBlank { UUID.randomUUID().toString() },
            userId = userId,
            title = title.take(80),
            amount = amount.coerceAtMost(10_000_000_000L),
            category = ExpenseCategory.fromKey(o.optString("category")).name,
            paymentMethod = PaymentMethod.fromKey(o.optString("paymentMethod")).name,
            note = o.optString("note", "").take(200),
          )
        )
      }
    }

    val goals = buildList {
      val arr = root.optJSONArray("savingsGoals") ?: JSONArray()
      for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        val title = o.optString("title").trim()
        val target = o.optLong("targetAmount", 0L)
        if (title.isEmpty() || target <= 0L) continue
        add(
          SavingsGoal(
            id = o.optString("id").ifBlank { UUID.randomUUID().toString() },
            userId = userId,
            title = title.take(60),
            targetAmount = target,
            savedAmount = o.optLong("savedAmount", 0L).coerceAtLeast(0L),
            category = SavingsGoalCategory.fromKey(o.optString("category")).name,
            note = o.optString("note", "").take(120),
          )
        )
      }
    }

    val walletsObj = root.optJSONObject("walletBalances")
    val walletBalances = if (walletsObj != null) {
      PaymentMethod.entries.associateWith { m -> walletsObj.optLong(m.name, 0L).coerceAtLeast(0L) }
    } else {
      emptyMap()
    }

    val cashflow = buildList {
      val arr = root.optJSONArray("cashflowEntries") ?: JSONArray()
      for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        val title = o.optString("title").trim()
        val amount = o.optLong("amount", 0L)
        if (title.isEmpty() || amount <= 0L) continue
        val type = CashflowType.entries.firstOrNull { it.name == o.optString("type") } ?: CashflowType.INCOME
        val src = o.optString("sourceWallet", "")
        add(
          CashflowEntry(
            id = o.optString("id").ifBlank { UUID.randomUUID().toString() },
            userId = userId,
            type = type,
            title = title.take(80),
            amount = amount,
            targetWallet = PaymentMethod.fromKey(o.optString("targetWallet")),
            sourceWallet = src.takeIf { it.isNotBlank() }?.let { PaymentMethod.fromKey(it) },
            dateEpochDay = o.optLong("dateEpochDay", LocalDate.now().toEpochDay()),
            monthKey = o.optString("monthKey", Formatters.currentMonthKey()),
            note = o.optString("note", "").take(120),
          )
        )
      }
    }

    val customCategories = buildList {
      val arr = root.optJSONArray("customCategories") ?: JSONArray()
      for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        val name = o.optString("name").trim()
        if (name.isEmpty()) continue
        add(
          CustomCategory(
            id = o.optString("id").ifBlank { UUID.randomUUID().toString() },
            name = name.take(24),
            parentCategory = ExpenseCategory.fromKey(o.optString("parentCategory")),
            colorHex = o.optLong("colorHex", CustomCategoryManager.presetColorPalette.first()),
          )
        )
      }
    }

    val savingsHistory = buildList {
      val arr = root.optJSONArray("savingsHistory") ?: JSONArray()
      for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        val goalId = o.optString("goalId").trim()
        val amount = o.optLong("amount", 0L)
        if (goalId.isEmpty() || amount <= 0L) continue
        val type = SavingsHistoryType.entries.firstOrNull { it.name == o.optString("type") }
          ?: SavingsHistoryType.DEPOSIT
        add(
          SavingsHistoryEntry(
            id = o.optString("id").ifBlank { UUID.randomUUID().toString() },
            goalId = goalId,
            type = type,
            amount = amount,
            balanceAfter = o.optLong("balanceAfter", 0L).coerceAtLeast(0L),
            timestampMillis = o.optLong("timestampMillis", System.currentTimeMillis()),
            note = o.optString("note", "").take(120),
          )
        )
      }
    }

    val debts = buildList {
      val arr = root.optJSONArray("debtRecords") ?: JSONArray()
      for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        val person = o.optString("personName").trim()
        val amount = o.optLong("amount", 0L)
        if (person.isEmpty() || amount <= 0L) continue
        val type = DebtType.entries.firstOrNull { it.name == o.optString("type") } ?: DebtType.OWED_TO_ME
        add(
          DebtRecord(
            id = o.optString("id").ifBlank { UUID.randomUUID().toString() },
            userId = userId,
            personName = person.take(60),
            type = type,
            amount = amount,
            dueDateEpochDay = o.optLong("dueDateEpochDay", LocalDate.now().toEpochDay()),
            settled = o.optBoolean("settled", false),
            note = o.optString("note", "").take(160),
          )
        )
      }
    }

    val customWallets = buildList {
      val arr = root.optJSONArray("customWallets") ?: JSONArray()
      for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        val name = o.optString("name").trim()
        if (name.isEmpty()) continue
        val type = CustomWalletType.entries.firstOrNull { it.name == o.optString("walletType") }
          ?: CustomWalletType.BANK
        add(
          CustomWallet(
            id = o.optString("id").ifBlank { UUID.randomUUID().toString() },
            userId = userId,
            name = name.take(40),
            walletType = type,
            balance = o.optLong("balance", 0L).coerceAtLeast(0L),
            colorHex = o.optLong("colorHex", CustomWalletManager.presetColorPalette.first()),
          )
        )
      }
    }

    return FullAppBackup(
      schemaVersion = root.optInt("schemaVersion", 1),
      exportedAtMillis = root.optLong("exportedAtMillis", System.currentTimeMillis()),
      expenses = expenses,
      budgets = budgets,
      recurringTemplates = recurring,
      savingsGoals = goals,
      walletBalances = walletBalances,
      cashflowEntries = cashflow,
      customCategories = customCategories,
      savingsHistory = savingsHistory,
      debtRecords = debts,
      customWallets = customWallets,
    )
  }

  suspend fun writeBackupToUri(
    context: Context,
    uri: Uri,
    backup: FullAppBackup,
  ): Result<Int> = withContext(Dispatchers.IO) {
    runCatching {
      val json = toJsonString(backup)
      context.contentResolver.openOutputStream(uri)?.use { stream ->
        stream.write(json.toByteArray(Charsets.UTF_8))
        stream.flush()
      } ?: error("Tidak dapat membuka file tujuan.")
      backup.totalItemCount
    }
  }

  suspend fun readBackupFromUri(
    context: Context,
    uri: Uri,
    userId: String,
  ): Result<FullAppBackup> = withContext(Dispatchers.IO) {
    runCatching {
      val raw = context.contentResolver.openInputStream(uri)?.use { stream ->
        stream.bufferedReader(Charsets.UTF_8).readText()
      } ?: error("Tidak dapat membaca file cadangan.")
      parseFromJson(raw, userId)
    }
  }
}
