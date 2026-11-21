package com.example.data.local

import android.content.Context
import com.example.data.model.SavingsGoal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.math.ceil

enum class SavingsHistoryType {
  INITIAL,
  DEPOSIT,
  WITHDRAW,
}

data class SavingsHistoryEntry(
  val id: String = UUID.randomUUID().toString(),
  val goalId: String = "",
  val type: SavingsHistoryType = SavingsHistoryType.DEPOSIT,
  val amount: Long = 0L,
  val balanceAfter: Long = 0L,
  val timestampMillis: Long = System.currentTimeMillis(),
  val note: String = "",
)

data class GoalCompletionEstimate(
  val estimatedDaysLeft: Int,
  val estimatedTargetDate: LocalDate,
  val averageMonthlyPace: Long,
)

/**
 * Tracks chronological Deposit / Withdrawal / Initial balance records per savings goal,
 * and estimates completion date based on real deposit velocity.
 */
object SavingsHistoryManager {
  private const val PREF_PREFIX = "sakupintar_savings_history_"
  private const val KEY_ENTRIES = "history_json"
  private const val MAX_ENTRIES = 500

  private val flowsByUser = mutableMapOf<String, MutableStateFlow<List<SavingsHistoryEntry>>>()

  fun observeEntries(context: Context, userId: String): StateFlow<List<SavingsHistoryEntry>> {
    val flow = synchronized(flowsByUser) {
      flowsByUser.getOrPut(userId) {
        MutableStateFlow(loadFromDisk(context, userId))
      }
    }
    return flow.asStateFlow()
  }

  fun recordEvent(
    context: Context,
    userId: String,
    goalId: String,
    type: SavingsHistoryType,
    amount: Long,
    balanceAfter: Long,
    note: String = "",
    timestampMillis: Long = System.currentTimeMillis(),
  ) {
    if (goalId.isBlank() || amount <= 0L) return
    val entry = SavingsHistoryEntry(
      id = UUID.randomUUID().toString(),
      goalId = goalId,
      type = type,
      amount = amount,
      balanceAfter = balanceAfter.coerceAtLeast(0L),
      timestampMillis = timestampMillis,
      note = note.trim().take(120),
    )
    val current = observeEntries(context, userId).value
    val updated = (listOf(entry) + current).take(MAX_ENTRIES)
    updateFlowAndDisk(context, userId, updated)
  }

  fun deleteHistoryForGoal(context: Context, userId: String, goalId: String) {
    val current = observeEntries(context, userId).value
    val updated = current.filterNot { it.goalId == goalId }
    if (updated.size != current.size) {
      updateFlowAndDisk(context, userId, updated)
    }
  }

  fun estimateCompletion(
    goal: SavingsGoal,
    goalEntries: List<SavingsHistoryEntry>,
    today: LocalDate = LocalDate.now(),
  ): GoalCompletionEstimate? {
    val remaining = (goal.targetAmount - goal.savedAmount).coerceAtLeast(0L)
    if (remaining <= 0L) return null

    val deposits = goalEntries.filter { it.type == SavingsHistoryType.DEPOSIT && it.amount > 0L }
    if (deposits.isEmpty()) return null

    val zone = ZoneId.systemDefault()
    val totalDeposited = deposits.sumOf { it.amount }
    val earliestDate = deposits.minOf {
      Instant.ofEpochMilli(it.timestampMillis).atZone(zone).toLocalDate()
    }
    val spanDays = ChronoUnit.DAYS.between(earliestDate, today).toInt().coerceAtLeast(1)
    // Use at least a 30-day window when only 1 deposit exists so same-day deposit implies monthly cadence
    val effectiveDays = if (deposits.size == 1 && spanDays < 30) 30.0 else spanDays.coerceAtLeast(7).toDouble()
    val dailyPace = totalDeposited.toDouble() / effectiveDays
    if (dailyPace <= 0.0) return null

    val estimatedDays = ceil(remaining.toDouble() / dailyPace).toInt().coerceIn(1, 3650)
    val targetDate = today.plusDays(estimatedDays.toLong())
    val monthlyPace = (dailyPace * 30.0).toLong().coerceAtLeast(1L)

    return GoalCompletionEstimate(
      estimatedDaysLeft = estimatedDays,
      estimatedTargetDate = targetDate,
      averageMonthlyPace = monthlyPace,
    )
  }

  private fun updateFlowAndDisk(
    context: Context,
    userId: String,
    entries: List<SavingsHistoryEntry>,
  ) {
    synchronized(flowsByUser) {
      flowsByUser.getOrPut(userId) { MutableStateFlow(entries) }.value = entries
    }
    saveToDisk(context, userId, entries)
  }

  private fun prefs(context: Context, userId: String) =
    context.applicationContext.getSharedPreferences(PREF_PREFIX + userId, Context.MODE_PRIVATE)

  private fun loadFromDisk(context: Context, userId: String): List<SavingsHistoryEntry> {
    val raw = prefs(context, userId).getString(KEY_ENTRIES, null) ?: return emptyList()
    return runCatching {
      val arr = JSONArray(raw)
      buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.optJSONObject(i) ?: continue
          val type = runCatching {
            SavingsHistoryType.valueOf(obj.optString("type", SavingsHistoryType.DEPOSIT.name))
          }.getOrDefault(SavingsHistoryType.DEPOSIT)
          add(
            SavingsHistoryEntry(
              id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
              goalId = obj.optString("goalId"),
              type = type,
              amount = obj.optLong("amount", 0L),
              balanceAfter = obj.optLong("balanceAfter", 0L),
              timestampMillis = obj.optLong("timestampMillis", System.currentTimeMillis()),
              note = obj.optString("note", ""),
            )
          )
        }
      }
    }.getOrDefault(emptyList())
  }

  private fun saveToDisk(context: Context, userId: String, entries: List<SavingsHistoryEntry>) {
    val arr = JSONArray()
    entries.forEach { e ->
      arr.put(
        JSONObject().apply {
          put("id", e.id)
          put("goalId", e.goalId)
          put("type", e.type.name)
          put("amount", e.amount)
          put("balanceAfter", e.balanceAfter)
          put("timestampMillis", e.timestampMillis)
          put("note", e.note)
        }
      )
    }
    prefs(context, userId).edit().putString(KEY_ENTRIES, arr.toString()).apply()
  }
}
