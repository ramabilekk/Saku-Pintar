package com.example.data.local

import android.content.Context
import com.example.util.Formatters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class NetWorthSnapshot(
  val monthKey: String,
  val totalAssets: Long,
  val totalLiabilities: Long,
  val netWorth: Long = totalAssets - totalLiabilities,
)

/**
 * Tracks monthly net worth snapshots (Assets - Liabilities) for trend charting over time.
 * Hydrated from Firestore via [replaceAll] in Google mode, and stored locally per user.
 */
object NetWorthSnapshotManager {
  private const val PREFS_NAME = "sakupintar_net_worth_snapshots"
  private const val KEY_PREFIX = "nw_"
  private val MONTH_REGEX = Regex("^[0-9]{4}-(0[1-9]|1[0-2])$")

  private val flows = mutableMapOf<String, MutableStateFlow<List<NetWorthSnapshot>>>()

  fun observeSnapshots(context: Context, userId: String): StateFlow<List<NetWorthSnapshot>> {
    val key = userKey(userId)
    return synchronized(flows) {
      flows.getOrPut(key) {
        MutableStateFlow(load(context.applicationContext, key))
      }
    }.asStateFlow()
  }

  /**
   * Records or updates the snapshot for [monthKey] (defaults to current month).
   * Returns the saved [NetWorthSnapshot] ONLY if the values actually changed (to avoid redundant cloud writes).
   */
  fun recordSnapshotIfChanged(
    context: Context,
    userId: String,
    totalAssets: Long,
    totalLiabilities: Long,
    monthKey: String = Formatters.currentMonthKey(),
  ): NetWorthSnapshot? {
    if (!MONTH_REGEX.matches(monthKey)) return null
    val safeAssets = totalAssets.coerceIn(0L, 1_000_000_000_000L)
    val safeLiabilities = totalLiabilities.coerceIn(0L, 1_000_000_000_000L)
    val snapshot = NetWorthSnapshot(
      monthKey = monthKey,
      totalAssets = safeAssets,
      totalLiabilities = safeLiabilities,
      netWorth = safeAssets - safeLiabilities,
    )
    val key = userKey(userId)
    val flow = synchronized(flows) {
      flows.getOrPut(key) { MutableStateFlow(load(context.applicationContext, key)) }
    }
    val existing = flow.value.firstOrNull { it.monthKey == monthKey }
    if (existing == snapshot) return null

    val updated = (flow.value.filterNot { it.monthKey == monthKey } + snapshot)
      .sortedBy { it.monthKey }
      .takeLast(12)
    flow.value = updated
    save(context.applicationContext, key, updated)
    return snapshot
  }

  fun replaceAll(context: Context, userId: String, items: List<NetWorthSnapshot>) {
    val key = userKey(userId)
    val normalized = items
      .filter { MONTH_REGEX.matches(it.monthKey) }
      .associateBy { it.monthKey }
      .values
      .sortedBy { it.monthKey }
      .takeLast(12)
    val flow = synchronized(flows) {
      flows.getOrPut(key) { MutableStateFlow(normalized) }
    }
    flow.value = normalized
    save(context.applicationContext, key, normalized)
  }

  private fun userKey(userId: String): String = KEY_PREFIX + userId.ifBlank { "local_guest" }

  private fun load(context: Context, key: String): List<NetWorthSnapshot> {
    val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .getString(key, null) ?: return emptyList()
    return runCatching {
      val arr = JSONArray(raw)
      buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.optJSONObject(i) ?: continue
          val monthKey = obj.optString("monthKey", "")
          if (!MONTH_REGEX.matches(monthKey)) continue
          val assets = obj.optLong("totalAssets", 0L).coerceAtLeast(0L)
          val liabilities = obj.optLong("totalLiabilities", 0L).coerceAtLeast(0L)
          add(
            NetWorthSnapshot(
              monthKey = monthKey,
              totalAssets = assets,
              totalLiabilities = liabilities,
              netWorth = assets - liabilities,
            )
          )
        }
      }.sortedBy { it.monthKey }
    }.getOrDefault(emptyList())
  }

  private fun save(context: Context, key: String, items: List<NetWorthSnapshot>) {
    val arr = JSONArray()
    items.forEach { item ->
      arr.put(
        JSONObject().apply {
          put("monthKey", item.monthKey)
          put("totalAssets", item.totalAssets)
          put("totalLiabilities", item.totalLiabilities)
          put("netWorth", item.netWorth)
        }
      )
    }
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putString(key, arr.toString())
      .apply()
  }
}
