package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.PaymentMethod
import com.example.data.model.SavingsGoal
import com.example.data.model.SavingsGoalCategory
import com.example.data.model.WalletBalance
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class SavingsRepository(
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

  fun observeSavingsGoals(userId: String): Flow<List<SavingsGoal>> {
    val path = "users/$userId/savingsGoals"
    return db.collection("users")
      .document(userId)
      .collection("savingsGoals")
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(
          SavingsGoal::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        ).sortedWith(
          compareBy<SavingsGoal> { it.isCompleted }
            .thenByDescending { it.createdAt?.seconds ?: 0L }
            .thenBy { it.title.lowercase() }
        )
      }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val toLog = firestoreError ?: (error as? Exception)
        if (toLog != null && auth.currentUser?.uid == userId) {
          handleFirestoreError(toLog, OperationType.LIST, path, auth)
        }
        throw error
      }
  }

  fun observeWalletBalances(userId: String): Flow<List<WalletBalance>> {
    val path = "users/$userId/walletBalances"
    return db.collection("users")
      .document(userId)
      .collection("walletBalances")
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(
          WalletBalance::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        )
      }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val toLog = firestoreError ?: (error as? Exception)
        if (toLog != null && auth.currentUser?.uid == userId) {
          handleFirestoreError(toLog, OperationType.LIST, path, auth)
        }
        throw error
      }
  }

  suspend fun createSavingsGoal(
    title: String,
    targetAmount: Long,
    initialSavedAmount: Long = 0L,
    category: SavingsGoalCategory = SavingsGoalCategory.EMERGENCY,
    note: String = "",
  ): String {
    val uid = requireUserId()
    val cleanTitle = title.trim().take(60)
    require(cleanTitle.isNotEmpty()) { "Nama target tabungan harus diisi." }
    require(targetAmount > 0L) { "Target nominal harus lebih dari 0." }
    val safeSaved = initialSavedAmount.coerceAtLeast(0L)

    val docRef = db.collection("users").document(uid).collection("savingsGoals").document()
    val payload = mapOf(
      "userId" to uid,
      "title" to cleanTitle,
      "targetAmount" to targetAmount,
      "savedAmount" to safeSaved,
      "category" to category.name,
      "note" to note.trim().take(120),
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    try {
      docRef.set(payload).await()
      return docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
      throw e
    }
  }

  suspend fun adjustSavingsGoalAmount(goalId: String, deltaAmount: Long): Long {
    val uid = requireUserId()
    require(goalId.isNotBlank()) { "ID target tabungan tidak boleh kosong." }
    val docRef = db.collection("users").document(uid).collection("savingsGoals").document(goalId)
    try {
      return db.runTransaction { tx ->
        val snap = tx.get(docRef)
        val currentSaved = snap.getLong("savedAmount") ?: 0L
        val nextSaved = (currentSaved + deltaAmount).coerceAtLeast(0L)
        tx.update(
          docRef,
          mapOf(
            "savedAmount" to nextSaved,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
        nextSaved
      }.await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
      throw e
    }
  }

  suspend fun updateSavingsGoal(
    goalId: String,
    title: String,
    targetAmount: Long,
    category: SavingsGoalCategory,
    note: String = "",
  ) {
    val uid = requireUserId()
    require(goalId.isNotBlank()) { "ID target tabungan tidak boleh kosong." }
    val cleanTitle = title.trim().take(60)
    require(cleanTitle.isNotEmpty()) { "Nama target tabungan harus diisi." }
    require(targetAmount > 0L) { "Target nominal harus lebih dari 0." }

    val docRef = db.collection("users").document(uid).collection("savingsGoals").document(goalId)
    val updates = mapOf(
      "title" to cleanTitle,
      "targetAmount" to targetAmount,
      "category" to category.name,
      "note" to note.trim().take(120),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    try {
      docRef.update(updates).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
      throw e
    }
  }

  suspend fun deleteSavingsGoal(goalId: String) {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("savingsGoals").document(goalId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
      throw e
    }
  }

  suspend fun setWalletBalance(paymentMethod: PaymentMethod, newBalance: Long) {
    val uid = requireUserId()
    val safeBalance = newBalance.coerceAtLeast(0L)
    val docRef = db.collection("users").document(uid)
      .collection("walletBalances").document(paymentMethod.name)
    try {
      val existingSnapshot = docRef.get().await()
      if (!existingSnapshot.exists()) {
        docRef.set(
          mapOf(
            "userId" to uid,
            "paymentMethod" to paymentMethod.name,
            "balance" to safeBalance,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      } else {
        docRef.update(
          mapOf(
            "balance" to safeBalance,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
      throw e
    }
  }
}
