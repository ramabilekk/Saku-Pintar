package com.example.data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

enum class DebtType {
  OWED_TO_ME,
  I_OWE,
}

data class DebtRecord(
  val id: String = UUID.randomUUID().toString(),
  val userId: String = "",
  val personName: String = "",
  val type: DebtType = DebtType.OWED_TO_ME,
  val amount: Long = 0L,
  val dueDateEpochDay: Long = LocalDate.now().plusDays(7).toEpochDay(),
  val settled: Boolean = false,
  val note: String = "",
)

/**
 * Manages personal Debt / Receivable / Split Bill records locally per user ID,
 * with clean sync hooks for Firestore cloud replication.
 */
object DebtLedgerManager {
  private const val PREF_PREFIX = "sakupintar_debt_ledger_"
  private const val KEY_RECORDS = "debts_json"
  private const val MAX_RECORDS = 300

  private val flowsByUser = mutableMapOf<String, MutableStateFlow<List<DebtRecord>>>()

  fun observeRecords(context: Context, userId: String): StateFlow<List<DebtRecord>> {
    val flow = synchronized(flowsByUser) {
      flowsByUser.getOrPut(userId) {
        MutableStateFlow(loadFromDisk(context, userId))
      }
    }
    return flow.asStateFlow()
  }

  fun addRecord(
    context: Context,
    userId: String,
    personName: String,
    type: DebtType,
    amount: Long,
    dueDateEpochDay: Long,
    note: String = "",
    id: String = UUID.randomUUID().toString(),
  ): DebtRecord {
    val record = DebtRecord(
      id = id,
      userId = userId,
      personName = personName.trim().take(60),
      type = type,
      amount = amount.coerceAtLeast(1L),
      dueDateEpochDay = dueDateEpochDay.coerceIn(0L, 100_000L),
      settled = false,
      note = note.trim().take(160),
    )
    val current = observeRecords(context, userId).value
    val updated = (listOf(record) + current.filterNot { it.id == record.id }).take(MAX_RECORDS)
    updateFlowAndDisk(context, userId, updated)
    return record
  }

  fun toggleSettled(context: Context, userId: String, recordId: String): DebtRecord? {
    val current = observeRecords(context, userId).value
    var updatedRecord: DebtRecord? = null
    val updated = current.map { item ->
      if (item.id == recordId) {
        val toggled = item.copy(settled = !item.settled)
        updatedRecord = toggled
        toggled
      } else {
        item
      }
    }
    if (updatedRecord != null) {
      updateFlowAndDisk(context, userId, updated)
    }
    return updatedRecord
  }

  fun deleteRecord(context: Context, userId: String, recordId: String) {
    val current = observeRecords(context, userId).value
    val updated = current.filterNot { it.id == recordId }
    if (updated.size != current.size) {
      updateFlowAndDisk(context, userId, updated)
    }
  }

  fun replaceAll(context: Context, userId: String, records: List<DebtRecord>) {
    val sorted = records.sortedWith(
      compareBy<DebtRecord> { it.settled }
        .thenBy { it.dueDateEpochDay }
        .thenByDescending { it.amount }
    ).take(MAX_RECORDS)
    updateFlowAndDisk(context, userId, sorted)
  }

  fun clearAll(context: Context, userId: String) {
    synchronized(flowsByUser) {
      flowsByUser[userId]?.value = emptyList()
    }
    prefs(context, userId).edit().clear().apply()
  }

  private fun updateFlowAndDisk(context: Context, userId: String, records: List<DebtRecord>) {
    synchronized(flowsByUser) {
      flowsByUser.getOrPut(userId) { MutableStateFlow(records) }.value = records
    }
    saveToDisk(context, userId, records)
  }

  private fun prefs(context: Context, userId: String) =
    context.applicationContext.getSharedPreferences(PREF_PREFIX + userId, Context.MODE_PRIVATE)

  private fun loadFromDisk(context: Context, userId: String): List<DebtRecord> {
    val raw = prefs(context, userId).getString(KEY_RECORDS, null) ?: return emptyList()
    return runCatching {
      val arr = JSONArray(raw)
      buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.optJSONObject(i) ?: continue
          val type = DebtType.entries.firstOrNull { it.name == obj.optString("type") }
            ?: DebtType.OWED_TO_ME
          val name = obj.optString("personName", "").trim()
          val amount = obj.optLong("amount", 0L)
          if (name.isEmpty() || amount <= 0L) continue
          add(
            DebtRecord(
              id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
              userId = userId,
              personName = name,
              type = type,
              amount = amount,
              dueDateEpochDay = obj.optLong("dueDateEpochDay", LocalDate.now().toEpochDay()),
              settled = obj.optBoolean("settled", false),
              note = obj.optString("note", ""),
            )
          )
        }
      }
    }.getOrDefault(emptyList())
  }

  private fun saveToDisk(context: Context, userId: String, records: List<DebtRecord>) {
    val arr = JSONArray()
    records.forEach { r ->
      arr.put(
        JSONObject().apply {
          put("id", r.id)
          put("personName", r.personName)
          put("type", r.type.name)
          put("amount", r.amount)
          put("dueDateEpochDay", r.dueDateEpochDay)
          put("settled", r.settled)
          put("note", r.note)
        }
      )
    }
    prefs(context, userId).edit().putString(KEY_RECORDS, arr.toString()).apply()
  }
}
