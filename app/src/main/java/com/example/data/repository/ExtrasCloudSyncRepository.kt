package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.local.CashflowEntry
import com.example.data.local.CashflowLedgerManager
import com.example.data.local.CashflowType
import com.example.data.local.CustomCategory
import com.example.data.local.CustomCategoryManager
import com.example.data.local.CustomWallet
import com.example.data.local.CustomWalletManager
import com.example.data.local.CustomWalletType
import com.example.data.local.DebtLedgerManager
import com.example.data.local.DebtRecord
import com.example.data.local.DebtType
import com.example.data.local.NetWorthSnapshot
import com.example.data.local.NetWorthSnapshotManager
import com.example.data.local.SavingsHistoryEntry
import com.example.data.local.SavingsHistoryManager
import com.example.data.local.SavingsHistoryType
import com.example.data.local.SubscriptionCycle
import com.example.data.local.SubscriptionItem
import com.example.data.local.SubscriptionManager
import com.example.data.model.ExpenseCategory
import com.example.data.model.PaymentMethod
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.example.notifications.BillDueReminderManager
import com.example.notifications.BillDueSchedule
import com.example.util.Formatters
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate

/**
 * Real-time bi-directional Firestore synchronization for companion & extended personal finance data:
 * - cashflowEntries (Income & Transfers)
 * - customCategories
 * - savingsHistory
 * - debtRecords (Utang / Piutang / Split Bill)
 * - customWallets (BCA, GoPay, SeaBank, etc.)
 *
 * Always uses the named database from R.string.firestore_database_id and strictly matches firestore.rules.
 */
class ExtrasCloudSyncRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    db = FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    auth = Firebase.auth,
  )

  private fun requireUserId(): String =
    auth.currentUser?.uid
      ?: throw IllegalStateException("Pengguna harus masuk dengan Google terlebih dahulu.")

  private fun sanitizeDocId(raw: String): String =
    raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128).ifBlank { "item_${System.currentTimeMillis()}" }

  fun startRealtimeSync(
    scope: CoroutineScope,
    context: Context,
    userId: String,
  ): List<Job> {
    if (userId.isBlank() || auth.currentUser?.uid != userId) return emptyList()
    val appCtx = context.applicationContext
    val userDoc = db.collection("users").document(userId)

    val j1 = scope.launch {
      userDoc.collection("cashflowEntries").snapshots()
        .catch { err -> logSnapshotError(err, "users/$userId/cashflowEntries", userId) }
        .collect { snap ->
          val items = snap.documents.mapNotNull { doc ->
            val d = doc.data ?: return@mapNotNull null
            val title = (d["title"] as? String)?.trim().orEmpty()
            val amount = (d["amount"] as? Number)?.toLong() ?: 0L
            if (title.isEmpty() || amount <= 0L) return@mapNotNull null
            val type = CashflowType.entries.firstOrNull { it.name == d["type"] } ?: CashflowType.INCOME
            val sourceKey = (d["sourceWallet"] as? String).orEmpty()
            CashflowEntry(
              id = doc.id,
              userId = userId,
              type = type,
              title = title,
              amount = amount,
              targetWallet = PaymentMethod.fromKey((d["targetWallet"] as? String).orEmpty()),
              sourceWallet = sourceKey.takeIf { it.isNotBlank() }?.let { PaymentMethod.fromKey(it) },
              dateEpochDay = (d["dateEpochDay"] as? Number)?.toLong() ?: LocalDate.now().toEpochDay(),
              monthKey = (d["monthKey"] as? String)?.takeIf { it.length == 7 } ?: Formatters.currentMonthKey(),
              note = (d["note"] as? String).orEmpty(),
            )
          }.sortedByDescending { it.dateEpochDay }
          CashflowLedgerManager.replaceAll(appCtx, userId, items)
        }
    }

    val j2 = scope.launch {
      userDoc.collection("customCategories").snapshots()
        .catch { err -> logSnapshotError(err, "users/$userId/customCategories", userId) }
        .collect { snap ->
          val items = snap.documents.mapNotNull { doc ->
            val d = doc.data ?: return@mapNotNull null
            val name = (d["name"] as? String)?.trim().orEmpty()
            if (name.isEmpty()) return@mapNotNull null
            CustomCategory(
              id = doc.id,
              name = name,
              parentCategory = ExpenseCategory.fromKey((d["parentCategory"] as? String).orEmpty()),
              colorHex = (d["colorHex"] as? Number)?.toLong() ?: CustomCategoryManager.presetColorPalette.first(),
            )
          }
          CustomCategoryManager.replaceAll(appCtx, items)
        }
    }

    val j3 = scope.launch {
      userDoc.collection("savingsHistory").snapshots()
        .catch { err -> logSnapshotError(err, "users/$userId/savingsHistory", userId) }
        .collect { snap ->
          val items = snap.documents.mapNotNull { doc ->
            val d = doc.data ?: return@mapNotNull null
            val goalId = (d["goalId"] as? String)?.trim().orEmpty()
            val amount = (d["amount"] as? Number)?.toLong() ?: 0L
            if (goalId.isEmpty() || amount <= 0L) return@mapNotNull null
            val type = SavingsHistoryType.entries.firstOrNull { it.name == d["type"] }
              ?: SavingsHistoryType.DEPOSIT
            SavingsHistoryEntry(
              id = doc.id,
              goalId = goalId,
              type = type,
              amount = amount,
              balanceAfter = ((d["balanceAfter"] as? Number)?.toLong() ?: 0L).coerceAtLeast(0L),
              timestampMillis = (d["timestampMillis"] as? Number)?.toLong() ?: System.currentTimeMillis(),
              note = (d["note"] as? String).orEmpty(),
            )
          }.sortedByDescending { it.timestampMillis }
          SavingsHistoryManager.replaceAll(appCtx, userId, items)
        }
    }

    val j4 = scope.launch {
      userDoc.collection("debtRecords").snapshots()
        .catch { err -> logSnapshotError(err, "users/$userId/debtRecords", userId) }
        .collect { snap ->
          val items = snap.documents.mapNotNull { doc ->
            val d = doc.data ?: return@mapNotNull null
            val personName = (d["personName"] as? String)?.trim().orEmpty()
            val amount = (d["amount"] as? Number)?.toLong() ?: 0L
            if (personName.isEmpty() || amount <= 0L) return@mapNotNull null
            val type = DebtType.entries.firstOrNull { it.name == d["type"] } ?: DebtType.OWED_TO_ME
            DebtRecord(
              id = doc.id,
              userId = userId,
              personName = personName,
              type = type,
              amount = amount,
              dueDateEpochDay = (d["dueDateEpochDay"] as? Number)?.toLong() ?: LocalDate.now().toEpochDay(),
              settled = (d["settled"] as? Boolean) ?: false,
              note = (d["note"] as? String).orEmpty(),
            )
          }
          DebtLedgerManager.replaceAll(appCtx, userId, items)
        }
    }

    val j5 = scope.launch {
      userDoc.collection("customWallets").snapshots()
        .catch { err -> logSnapshotError(err, "users/$userId/customWallets", userId) }
        .collect { snap ->
          val items = snap.documents.mapNotNull { doc ->
            val d = doc.data ?: return@mapNotNull null
            val name = (d["name"] as? String)?.trim().orEmpty()
            if (name.isEmpty()) return@mapNotNull null
            val type = CustomWalletType.entries.firstOrNull { it.name == d["walletType"] }
              ?: CustomWalletType.BANK
            CustomWallet(
              id = doc.id,
              userId = userId,
              name = name,
              walletType = type,
              balance = ((d["balance"] as? Number)?.toLong() ?: 0L).coerceAtLeast(0L),
              colorHex = (d["colorHex"] as? Number)?.toLong() ?: CustomWalletManager.presetColorPalette.first(),
            )
          }
          CustomWalletManager.replaceAll(appCtx, userId, items)
        }
    }

    val j6 = scope.launch {
      userDoc.collection("subscriptions").snapshots()
        .catch { err -> logSnapshotError(err, "users/$userId/subscriptions", userId) }
        .collect { snap ->
          val items = snap.documents.mapNotNull { doc ->
            val d = doc.data ?: return@mapNotNull null
            val name = (d["name"] as? String)?.trim().orEmpty()
            val amount = (d["amount"] as? Number)?.toLong() ?: 0L
            if (name.isEmpty() || amount <= 0L) return@mapNotNull null
            SubscriptionItem(
              id = doc.id,
              userId = userId,
              name = name,
              amount = amount,
              billingCycle = SubscriptionCycle.fromKey((d["billingCycle"] as? String).orEmpty()),
              renewalDay = ((d["renewalDay"] as? Number)?.toInt() ?: 1).coerceIn(1, 31),
              category = ExpenseCategory.fromKey((d["category"] as? String).orEmpty()),
              paymentMethod = PaymentMethod.fromKey((d["paymentMethod"] as? String).orEmpty()),
              active = (d["active"] as? Boolean) ?: true,
              note = (d["note"] as? String).orEmpty(),
            )
          }
          SubscriptionManager.replaceAll(appCtx, userId, items)
        }
    }

    val j7 = scope.launch {
      userDoc.collection("billSchedules").snapshots()
        .catch { err -> logSnapshotError(err, "users/$userId/billSchedules", userId) }
        .collect { snap ->
          val map = buildMap<String, BillDueSchedule> {
            snap.documents.forEach { doc ->
              val d = doc.data ?: return@forEach
              val day = (d["dueDayOfMonth"] as? Number)?.toInt() ?: return@forEach
              if (day !in 1..31) return@forEach
              put(
                doc.id,
                BillDueSchedule(
                  templateId = doc.id,
                  title = (d["title"] as? String)?.trim().orEmpty(),
                  dueDayOfMonth = day,
                  reminderEnabled = (d["reminderEnabled"] as? Boolean) ?: true,
                  autoLogEnabled = (d["autoLogEnabled"] as? Boolean) ?: false,
                  lastAutoLoggedMonthKey = (d["lastAutoLoggedMonthKey"] as? String).orEmpty(),
                )
              )
            }
          }
          BillDueReminderManager.replaceAll(appCtx, map)
        }
    }

    val j8 = scope.launch {
      userDoc.collection("netWorthSnapshots").snapshots()
        .catch { err -> logSnapshotError(err, "users/$userId/netWorthSnapshots", userId) }
        .collect { snap ->
          val items = snap.documents.mapNotNull { doc ->
            val d = doc.data ?: return@mapNotNull null
            val monthKey = (d["monthKey"] as? String)?.takeIf { it.length == 7 } ?: doc.id
            val assets = ((d["totalAssets"] as? Number)?.toLong() ?: 0L).coerceAtLeast(0L)
            val liabilities = ((d["totalLiabilities"] as? Number)?.toLong() ?: 0L).coerceAtLeast(0L)
            NetWorthSnapshot(
              monthKey = monthKey,
              totalAssets = assets,
              totalLiabilities = liabilities,
              netWorth = assets - liabilities,
            )
          }
          NetWorthSnapshotManager.replaceAll(appCtx, userId, items)
        }
    }

    return listOf(j1, j2, j3, j4, j5, j6, j7, j8)
  }

  suspend fun upsertCashflowEntry(entry: CashflowEntry): Result<Unit> = runMutation("cashflowEntries") { uid ->
    val docId = sanitizeDocId(entry.id)
    val docRef = db.collection("users").document(uid).collection("cashflowEntries").document(docId)
    val payload = buildMap<String, Any> {
      put("userId", uid)
      put("type", entry.type.name)
      put("title", entry.title.trim().take(80).ifEmpty { "Cashflow" })
      put("amount", entry.amount.coerceIn(1L, 100_000_000_000L))
      put("targetWallet", entry.targetWallet.name)
      if (entry.sourceWallet != null) {
        put("sourceWallet", entry.sourceWallet.name)
      }
      put("dateEpochDay", entry.dateEpochDay.coerceIn(0L, 100_000L))
      put("monthKey", entry.monthKey)
      if (entry.note.isNotBlank()) {
        put("note", entry.note.trim().take(120))
      }
      put("createdAt", FieldValue.serverTimestamp())
      put("updatedAt", FieldValue.serverTimestamp())
    }
    docRef.set(payload).await()
  }

  suspend fun deleteCashflowEntry(entryId: String): Result<Unit> = runMutation("cashflowEntries") { uid ->
    val docId = sanitizeDocId(entryId)
    db.collection("users").document(uid).collection("cashflowEntries").document(docId).delete().await()
  }

  suspend fun upsertCustomCategory(category: CustomCategory): Result<Unit> = runMutation("customCategories") { uid ->
    val docId = sanitizeDocId(category.id)
    val docRef = db.collection("users").document(uid).collection("customCategories").document(docId)
    val payload = mapOf(
      "userId" to uid,
      "name" to category.name.trim().take(24),
      "parentCategory" to category.parentCategory.name,
      "colorHex" to category.colorHex,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    docRef.set(payload).await()
  }

  suspend fun deleteCustomCategory(categoryId: String): Result<Unit> = runMutation("customCategories") { uid ->
    val docId = sanitizeDocId(categoryId)
    db.collection("users").document(uid).collection("customCategories").document(docId).delete().await()
  }

  suspend fun addSavingsHistoryEntry(entry: SavingsHistoryEntry): Result<Unit> = runMutation("savingsHistory") { uid ->
    val docId = sanitizeDocId(entry.id)
    val docRef = db.collection("users").document(uid).collection("savingsHistory").document(docId)
    val payload = buildMap<String, Any> {
      put("userId", uid)
      put("goalId", entry.goalId.trim().take(128))
      put("type", entry.type.name)
      put("amount", entry.amount.coerceIn(1L, 100_000_000_000L))
      put("balanceAfter", entry.balanceAfter.coerceIn(0L, 100_000_000_000L))
      put("timestampMillis", entry.timestampMillis.coerceAtLeast(1L))
      if (entry.note.isNotBlank()) {
        put("note", entry.note.trim().take(120))
      }
      put("createdAt", FieldValue.serverTimestamp())
      put("updatedAt", FieldValue.serverTimestamp())
    }
    docRef.set(payload).await()
  }

  suspend fun upsertDebtRecord(record: DebtRecord): Result<Unit> = runMutation("debtRecords") { uid ->
    val docId = sanitizeDocId(record.id)
    val docRef = db.collection("users").document(uid).collection("debtRecords").document(docId)
    val existing = docRef.get().await()
    if (existing.exists()) {
      val updates = buildMap<String, Any> {
        put("personName", record.personName.trim().take(60))
        put("type", record.type.name)
        put("amount", record.amount.coerceIn(1L, 100_000_000_000L))
        put("dueDateEpochDay", record.dueDateEpochDay.coerceIn(0L, 100_000L))
        put("settled", record.settled)
        put("note", record.note.trim().take(160))
        put("updatedAt", FieldValue.serverTimestamp())
      }
      docRef.update(updates).await()
    } else {
      val payload = buildMap<String, Any> {
        put("userId", uid)
        put("personName", record.personName.trim().take(60))
        put("type", record.type.name)
        put("amount", record.amount.coerceIn(1L, 100_000_000_000L))
        put("dueDateEpochDay", record.dueDateEpochDay.coerceIn(0L, 100_000L))
        put("settled", record.settled)
        if (record.note.isNotBlank()) {
          put("note", record.note.trim().take(160))
        }
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
      }
      docRef.set(payload).await()
    }
  }

  suspend fun deleteDebtRecord(debtId: String): Result<Unit> = runMutation("debtRecords") { uid ->
    val docId = sanitizeDocId(debtId)
    db.collection("users").document(uid).collection("debtRecords").document(docId).delete().await()
  }

  suspend fun upsertCustomWallet(wallet: CustomWallet): Result<Unit> = runMutation("customWallets") { uid ->
    val docId = sanitizeDocId(wallet.id)
    val docRef = db.collection("users").document(uid).collection("customWallets").document(docId)
    val existing = docRef.get().await()
    if (existing.exists()) {
      docRef.update(
        mapOf(
          "name" to wallet.name.trim().take(40),
          "walletType" to wallet.walletType.name,
          "balance" to wallet.balance.coerceIn(0L, 100_000_000_000L),
          "colorHex" to wallet.colorHex,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } else {
      docRef.set(
        mapOf(
          "userId" to uid,
          "name" to wallet.name.trim().take(40),
          "walletType" to wallet.walletType.name,
          "balance" to wallet.balance.coerceIn(0L, 100_000_000_000L),
          "colorHex" to wallet.colorHex,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    }
  }

  suspend fun deleteCustomWallet(walletId: String): Result<Unit> = runMutation("customWallets") { uid ->
    val docId = sanitizeDocId(walletId)
    db.collection("users").document(uid).collection("customWallets").document(docId).delete().await()
  }

  suspend fun upsertSubscription(item: SubscriptionItem): Result<Unit> = runMutation("subscriptions") { uid ->
    val docId = sanitizeDocId(item.id)
    val docRef = db.collection("users").document(uid).collection("subscriptions").document(docId)
    val existing = docRef.get().await()
    if (existing.exists()) {
      docRef.update(
        mapOf(
          "name" to item.name.trim().take(60),
          "amount" to item.amount.coerceIn(1L, 100_000_000_000L),
          "billingCycle" to item.billingCycle.name,
          "renewalDay" to item.renewalDay.coerceIn(1, 31),
          "category" to item.category.name,
          "paymentMethod" to item.paymentMethod.name,
          "active" to item.active,
          "note" to item.note.trim().take(160),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } else {
      val payload = buildMap<String, Any> {
        put("userId", uid)
        put("name", item.name.trim().take(60))
        put("amount", item.amount.coerceIn(1L, 100_000_000_000L))
        put("billingCycle", item.billingCycle.name)
        put("renewalDay", item.renewalDay.coerceIn(1, 31))
        put("category", item.category.name)
        put("paymentMethod", item.paymentMethod.name)
        put("active", item.active)
        if (item.note.isNotBlank()) {
          put("note", item.note.trim().take(160))
        }
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
      }
      docRef.set(payload).await()
    }
  }

  suspend fun deleteSubscription(subId: String): Result<Unit> = runMutation("subscriptions") { uid ->
    val docId = sanitizeDocId(subId)
    db.collection("users").document(uid).collection("subscriptions").document(docId).delete().await()
  }

  suspend fun upsertBillSchedule(schedule: BillDueSchedule): Result<Unit> = runMutation("billSchedules") { uid ->
    val docId = sanitizeDocId(schedule.templateId)
    val docRef = db.collection("users").document(uid).collection("billSchedules").document(docId)
    val existing = docRef.get().await()
    val safeMonthKey = schedule.lastAutoLoggedMonthKey.takeIf {
      it.isEmpty() || Regex("^[0-9]{4}-(0[1-9]|1[0-2])$").matches(it)
    }.orEmpty()
    if (existing.exists()) {
      docRef.update(
        mapOf(
          "title" to schedule.title.trim().take(80).ifEmpty { "Bill" },
          "dueDayOfMonth" to schedule.dueDayOfMonth.coerceIn(1, 31),
          "reminderEnabled" to schedule.reminderEnabled,
          "autoLogEnabled" to schedule.autoLogEnabled,
          "lastAutoLoggedMonthKey" to safeMonthKey,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } else {
      val payload = buildMap<String, Any> {
        put("userId", uid)
        put("title", schedule.title.trim().take(80).ifEmpty { "Bill" })
        put("dueDayOfMonth", schedule.dueDayOfMonth.coerceIn(1, 31))
        put("reminderEnabled", schedule.reminderEnabled)
        put("autoLogEnabled", schedule.autoLogEnabled)
        if (safeMonthKey.isNotEmpty()) {
          put("lastAutoLoggedMonthKey", safeMonthKey)
        }
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
      }
      docRef.set(payload).await()
    }
  }

  suspend fun deleteBillSchedule(templateId: String): Result<Unit> = runMutation("billSchedules") { uid ->
    val docId = sanitizeDocId(templateId)
    db.collection("users").document(uid).collection("billSchedules").document(docId).delete().await()
  }

  suspend fun upsertNetWorthSnapshot(snapshot: NetWorthSnapshot): Result<Unit> = runMutation("netWorthSnapshots") { uid ->
    val monthKey = snapshot.monthKey
    if (!Regex("^[0-9]{4}-(0[1-9]|1[0-2])$").matches(monthKey)) return@runMutation
    val safeAssets = snapshot.totalAssets.coerceIn(0L, 1_000_000_000_000L)
    val safeLiabilities = snapshot.totalLiabilities.coerceIn(0L, 1_000_000_000_000L)
    val safeNetWorth = safeAssets - safeLiabilities
    val docRef = db.collection("users").document(uid).collection("netWorthSnapshots").document(monthKey)
    val existing = docRef.get().await()
    if (existing.exists()) {
      docRef.update(
        mapOf(
          "totalAssets" to safeAssets,
          "totalLiabilities" to safeLiabilities,
          "netWorth" to safeNetWorth,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } else {
      docRef.set(
        mapOf(
          "userId" to uid,
          "monthKey" to monthKey,
          "totalAssets" to safeAssets,
          "totalLiabilities" to safeLiabilities,
          "netWorth" to safeNetWorth,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    }
  }

  private suspend fun runMutation(
    subcollection: String,
    block: suspend (uid: String) -> Unit,
  ): Result<Unit> {
    return try {
      val uid = requireUserId()
      block(uid)
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      val uid = auth.currentUser?.uid.orEmpty()
      handleFirestoreError(e, OperationType.WRITE, "users/$uid/$subcollection", auth)
      Result.failure(e)
    }
  }

  private fun logSnapshotError(error: Throwable, path: String, userId: String) {
    val firestoreError = generateSequence(error) { it.cause }
      .filterIsInstance<FirebaseFirestoreException>()
      .firstOrNull()
    if (firestoreError == null && error is CancellationException) throw error
    val toLog = firestoreError ?: (error as? Exception)
    if (toLog != null && auth.currentUser?.uid == userId) {
      handleFirestoreError(toLog, OperationType.LIST, path, auth)
    } else {
      Log.w(TAG, "Extras snapshot closed on $path: ${error.message}")
    }
  }

  private companion object {
    const val TAG = "ExtrasCloudSync"
  }
}
