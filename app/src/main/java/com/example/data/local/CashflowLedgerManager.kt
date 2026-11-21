package com.example.data.local

import android.content.Context
import com.example.data.model.PaymentMethod
import com.example.util.Formatters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

enum class CashflowType {
  INCOME,
  TRANSFER,
}

data class CashflowEntry(
  val id: String = UUID.randomUUID().toString(),
  val userId: String = "",
  val type: CashflowType = CashflowType.INCOME,
  val title: String = "",
  val amount: Long = 0L,
  val targetWallet: PaymentMethod = PaymentMethod.TRANSFER,
  val sourceWallet: PaymentMethod? = null,
  val dateEpochDay: Long = LocalDate.now().toEpochDay(),
  val monthKey: String = Formatters.currentMonthKey(),
  val note: String = "",
)

/**
 * Persists Income and Wallet-to-Wallet Transfer history per user ID while syncing actual wallet
 * balances with Firestore (Google account) or LocalAccountVault (Guest account).
 */
object CashflowLedgerManager {
  private const val PREF_PREFIX = "sakupintar_cashflow_ledger_"
  private const val KEY_ENTRIES = "entries_json"
  private const val MAX_ENTRIES = 300

  private val flowsByUser = mutableMapOf<String, MutableStateFlow<List<CashflowEntry>>>()

  fun observeEntries(context: Context, userId: String): StateFlow<List<CashflowEntry>> {
    val flow = synchronized(flowsByUser) {
      flowsByUser.getOrPut(userId) {
        MutableStateFlow(loadFromDisk(context, userId))
      }
    }
    return flow.asStateFlow()
  }

  fun recordIncome(
    context: Context,
    userId: String,
    title: String,
    amount: Long,
    targetWallet: PaymentMethod,
    dateEpochDay: Long = LocalDate.now().toEpochDay(),
    note: String = "",
  ): CashflowEntry {
    val date = LocalDate.ofEpochDay(dateEpochDay)
    val entry = CashflowEntry(
      id = UUID.randomUUID().toString(),
      userId = userId,
      type = CashflowType.INCOME,
      title = title.trim().take(60),
      amount = amount.coerceAtLeast(0L),
      targetWallet = targetWallet,
      sourceWallet = null,
      dateEpochDay = dateEpochDay,
      monthKey = Formatters.currentMonthKey(date),
      note = note.trim().take(120),
    )
    prependEntry(context, userId, entry)
    return entry
  }

  fun recordTransfer(
    context: Context,
    userId: String,
    fromWallet: PaymentMethod,
    toWallet: PaymentMethod,
    amount: Long,
    dateEpochDay: Long = LocalDate.now().toEpochDay(),
    note: String = "",
  ): CashflowEntry {
    val date = LocalDate.ofEpochDay(dateEpochDay)
    val entry = CashflowEntry(
      id = UUID.randomUUID().toString(),
      userId = userId,
      type = CashflowType.TRANSFER,
      title = "${fromWallet.name} → ${toWallet.name}",
      amount = amount.coerceAtLeast(0L),
      targetWallet = toWallet,
      sourceWallet = fromWallet,
      dateEpochDay = dateEpochDay,
      monthKey = Formatters.currentMonthKey(date),
      note = note.trim().take(120),
    )
    prependEntry(context, userId, entry)
    return entry
  }

  fun deleteEntry(context: Context, userId: String, entryId: String): CashflowEntry? {
    val current = observeEntries(context, userId).value
    val target = current.firstOrNull { it.id == entryId } ?: return null
    val updated = current.filterNot { it.id == entryId }
    updateFlowAndDisk(context, userId, updated)
    return target
  }

  private fun prependEntry(context: Context, userId: String, entry: CashflowEntry) {
    val current = observeEntries(context, userId).value
    val updated = (listOf(entry) + current).take(MAX_ENTRIES)
    updateFlowAndDisk(context, userId, updated)
  }

  private fun updateFlowAndDisk(context: Context, userId: String, entries: List<CashflowEntry>) {
    synchronized(flowsByUser) {
      flowsByUser.getOrPut(userId) { MutableStateFlow(entries) }.value = entries
    }
    saveToDisk(context, userId, entries)
  }

  private fun prefs(context: Context, userId: String) =
    context.applicationContext.getSharedPreferences(PREF_PREFIX + userId, Context.MODE_PRIVATE)

  private fun loadFromDisk(context: Context, userId: String): List<CashflowEntry> {
    val raw = prefs(context, userId).getString(KEY_ENTRIES, null) ?: return emptyList()
    return runCatching {
      val arr = JSONArray(raw)
      buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.optJSONObject(i) ?: continue
          val type = runCatching { CashflowType.valueOf(obj.optString("type", "INCOME")) }
            .getOrDefault(CashflowType.INCOME)
          val sourceKey = obj.optString("sourceWallet", "")
          add(
            CashflowEntry(
              id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
              userId = userId,
              type = type,
              title = obj.optString("title", ""),
              amount = obj.optLong("amount", 0L),
              targetWallet = PaymentMethod.fromKey(obj.optString("targetWallet")),
              sourceWallet = sourceKey.takeIf { it.isNotBlank() }?.let { PaymentMethod.fromKey(it) },
              dateEpochDay = obj.optLong("dateEpochDay", LocalDate.now().toEpochDay()),
              monthKey = obj.optString("monthKey", Formatters.currentMonthKey()),
              note = obj.optString("note", ""),
            )
          )
        }
      }
    }.getOrDefault(emptyList())
  }

  private fun saveToDisk(context: Context, userId: String, entries: List<CashflowEntry>) {
    val arr = JSONArray()
    entries.forEach { e ->
      arr.put(
        JSONObject().apply {
          put("id", e.id)
          put("type", e.type.name)
          put("title", e.title)
          put("amount", e.amount)
          put("targetWallet", e.targetWallet.name)
          put("sourceWallet", e.sourceWallet?.name ?: "")
          put("dateEpochDay", e.dateEpochDay)
          put("monthKey", e.monthKey)
          put("note", e.note)
        }
      )
    }
    prefs(context, userId).edit().putString(KEY_ENTRIES, arr.toString()).apply()
  }
}
