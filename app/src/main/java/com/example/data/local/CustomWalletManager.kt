package com.example.data.local

import android.content.Context
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

import com.example.data.model.PaymentMethod

enum class CustomWalletType {
  BANK,
  EWALLET,
  CASH,
  CARD,
  INVESTMENT;

  fun defaultPaymentMethod(): PaymentMethod = when (this) {
    BANK, INVESTMENT -> PaymentMethod.TRANSFER
    EWALLET -> PaymentMethod.QRIS
    CASH -> PaymentMethod.CASH
    CARD -> PaymentMethod.CARD
  }
}

data class CustomWallet(
  val id: String = UUID.randomUUID().toString(),
  val userId: String = "",
  val name: String = "",
  val walletType: CustomWalletType = CustomWalletType.BANK,
  val balance: Long = 0L,
  val colorHex: Long = 0xFF0D8A68L,
) {
  val accentColor: Color
    get() = Color(colorHex)
}

/**
 * Manages user-defined custom wallets/accounts (e.g., BCA, GoPay, OVO, Dana, SeaBank)
 * locally per user ID, with seamless Firestore cloud sync support.
 */
object CustomWalletManager {
  private const val PREF_PREFIX = "sakupintar_custom_wallets_"
  private const val KEY_WALLETS = "wallets_json"
  private const val MAX_WALLETS = 30

  val presetColorPalette: List<Long> = listOf(
    0xFF0D8A68L, // Emerald
    0xFF0284C7L, // Ocean Blue (BCA/Mandiri)
    0xFF0E7490L, // Deep Teal
    0xFF7C3AEDL, // Violet (OVO)
    0xFF059669L, // Mint (GoPay)
    0xFFF59E0BL, // Amber (SeaBank/Gold)
    0xFFEA580CL, // Orange (ShopeePay)
    0xFFDC2626L, // Crimson
  )

  private val flowsByUser = mutableMapOf<String, MutableStateFlow<List<CustomWallet>>>()

  fun observeWallets(context: Context, userId: String): StateFlow<List<CustomWallet>> {
    val flow = synchronized(flowsByUser) {
      flowsByUser.getOrPut(userId) {
        MutableStateFlow(loadFromDisk(context, userId))
      }
    }
    return flow.asStateFlow()
  }

  fun upsertWallet(
    context: Context,
    userId: String,
    name: String,
    walletType: CustomWalletType,
    balance: Long,
    colorHex: Long = presetColorPalette.first(),
    id: String = UUID.randomUUID().toString(),
  ): CustomWallet? {
    val cleanName = name.trim().take(40)
    if (cleanName.isEmpty()) return null
    val current = observeWallets(context, userId).value
    val exists = current.any { it.id == id }
    if (!exists && current.size >= MAX_WALLETS) return null

    val wallet = CustomWallet(
      id = id,
      userId = userId,
      name = cleanName,
      walletType = walletType,
      balance = balance.coerceIn(0L, 100_000_000_000L),
      colorHex = colorHex,
    )
    val updated = if (exists) {
      current.map { if (it.id == id) wallet else it }
    } else {
      current + wallet
    }
    updateFlowAndDisk(context, userId, updated)
    return wallet
  }

  fun adjustBalance(
    context: Context,
    userId: String,
    walletId: String,
    delta: Long,
  ): CustomWallet? {
    if (delta == 0L) return observeWallets(context, userId).value.firstOrNull { it.id == walletId }
    val current = observeWallets(context, userId).value
    val target = current.firstOrNull { it.id == walletId } ?: return null
    val updatedWallet = target.copy(
      balance = (target.balance + delta).coerceIn(0L, 100_000_000_000L),
    )
    val updatedList = current.map { if (it.id == walletId) updatedWallet else it }
    updateFlowAndDisk(context, userId, updatedList)
    return updatedWallet
  }

  fun deleteWallet(context: Context, userId: String, walletId: String) {
    val current = observeWallets(context, userId).value
    val updated = current.filterNot { it.id == walletId }
    if (updated.size != current.size) {
      updateFlowAndDisk(context, userId, updated)
    }
  }

  fun replaceAll(context: Context, userId: String, wallets: List<CustomWallet>) {
    updateFlowAndDisk(context, userId, wallets.take(MAX_WALLETS))
  }

  fun clearAll(context: Context, userId: String) {
    synchronized(flowsByUser) {
      flowsByUser[userId]?.value = emptyList()
    }
    prefs(context, userId).edit().clear().apply()
  }

  private fun updateFlowAndDisk(context: Context, userId: String, wallets: List<CustomWallet>) {
    synchronized(flowsByUser) {
      flowsByUser.getOrPut(userId) { MutableStateFlow(wallets) }.value = wallets
    }
    saveToDisk(context, userId, wallets)
  }

  private fun prefs(context: Context, userId: String) =
    context.applicationContext.getSharedPreferences(PREF_PREFIX + userId, Context.MODE_PRIVATE)

  private fun loadFromDisk(context: Context, userId: String): List<CustomWallet> {
    val raw = prefs(context, userId).getString(KEY_WALLETS, null) ?: return emptyList()
    return runCatching {
      val arr = JSONArray(raw)
      buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.optJSONObject(i) ?: continue
          val name = obj.optString("name", "").trim()
          if (name.isEmpty()) continue
          val type = CustomWalletType.entries.firstOrNull { it.name == obj.optString("walletType") }
            ?: CustomWalletType.BANK
          add(
            CustomWallet(
              id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
              userId = userId,
              name = name,
              walletType = type,
              balance = obj.optLong("balance", 0L).coerceAtLeast(0L),
              colorHex = obj.optLong("colorHex", presetColorPalette.first()),
            )
          )
        }
      }
    }.getOrDefault(emptyList())
  }

  private fun saveToDisk(context: Context, userId: String, wallets: List<CustomWallet>) {
    val arr = JSONArray()
    wallets.forEach { w ->
      arr.put(
        JSONObject().apply {
          put("id", w.id)
          put("name", w.name)
          put("walletType", w.walletType.name)
          put("balance", w.balance)
          put("colorHex", w.colorHex)
        }
      )
    }
    prefs(context, userId).edit().putString(KEY_WALLETS, arr.toString()).apply()
  }
}
