package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Expense
import com.example.data.model.MonthlyBudget
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

class FinanceRepository(
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

  fun observeExpensesForMonth(userId: String, monthKey: String): Flow<List<Expense>> {
    val path = "users/$userId/expenses"
    return db.collection("users")
      .document(userId)
      .collection("expenses")
      .whereEqualTo("monthKey", monthKey)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(
          Expense::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        ).sortedWith(
          compareByDescending<Expense> { it.dateEpochDay }
            .thenByDescending { it.createdAt?.seconds ?: 0L }
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

  fun observeAllExpenses(userId: String): Flow<List<Expense>> {
    val path = "users/$userId/expenses"
    return db.collection("users")
      .document(userId)
      .collection("expenses")
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(
          Expense::class.java,
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

  fun observeMonthlyBudget(userId: String, monthKey: String): Flow<MonthlyBudget?> {
    val docRef = db.collection("users")
      .document(userId)
      .collection("budgets")
      .document(monthKey)
    return docRef.snapshots()
      .map { snapshot ->
        if (snapshot.exists()) {
          snapshot.toObject(
            MonthlyBudget::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        } else {
          null
        }
      }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val toLog = firestoreError ?: (error as? Exception)
        if (toLog != null && auth.currentUser?.uid == userId) {
          handleFirestoreError(toLog, OperationType.GET, docRef.path, auth)
        }
        throw error
      }
  }

  suspend fun addExpense(
    title: String,
    amount: Long,
    category: String,
    paymentMethod: String,
    dateEpochDay: Long,
    monthKey: String,
    note: String = "",
  ): String {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("expenses").document()
    val payload = mapOf(
      "userId" to uid,
      "title" to title.trim().take(80),
      "amount" to amount,
      "category" to category,
      "paymentMethod" to paymentMethod,
      "dateEpochDay" to dateEpochDay,
      "monthKey" to monthKey,
      "note" to note.trim().take(200),
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

  suspend fun updateExpense(expense: Expense) {
    val uid = requireUserId()
    require(expense.id.isNotBlank()) { "Expense ID must not be blank for update" }
    requireNotNull(expense.createdAt) { "Expense createdAt must not be null for update" }
    val docRef = db.collection("users").document(uid).collection("expenses").document(expense.id)
    try {
      docRef.set(
        expense.copy(
          userId = uid,
          title = expense.title.trim().take(80),
          note = expense.note.trim().take(200),
          updatedAt = null,
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
      throw e
    }
  }

  suspend fun deleteExpense(expenseId: String) {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("expenses").document(expenseId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
      throw e
    }
  }

  suspend fun saveMonthlyBudget(budget: MonthlyBudget) {
    val uid = requireUserId()
    require(budget.monthKey.isNotBlank()) { "monthKey must not be blank" }
    val docRef = db.collection("users").document(uid).collection("budgets").document(budget.monthKey)
    try {
      val existingSnapshot = docRef.get().await()
      if (!existingSnapshot.exists()) {
        val createPayload = mapOf(
          "userId" to uid,
          "monthKey" to budget.monthKey,
          "monthlyIncome" to budget.monthlyIncome,
          "strategy" to budget.strategy,
          "savingsTarget" to budget.savingsTarget,
          "needsPercent" to budget.needsPercent,
          "wantsPercent" to budget.wantsPercent,
          "savingsPercent" to budget.savingsPercent,
          "categoryBudgets" to budget.categoryBudgets,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        docRef.set(createPayload).await()
      } else {
        val updatePayload = mapOf(
          "monthlyIncome" to budget.monthlyIncome,
          "strategy" to budget.strategy,
          "savingsTarget" to budget.savingsTarget,
          "needsPercent" to budget.needsPercent,
          "wantsPercent" to budget.wantsPercent,
          "savingsPercent" to budget.savingsPercent,
          "categoryBudgets" to budget.categoryBudgets,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        docRef.update(updatePayload).await()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
      throw e
    }
  }
}
