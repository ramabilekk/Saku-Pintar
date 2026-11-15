package com.example.data.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.data.budget.DailyBudgetInsight
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.MonthlyBudget
import com.example.data.model.PaymentMethod
import com.example.i18n.AppCurrency
import com.example.i18n.AppLanguage
import com.example.i18n.CurrencyPreferenceManager
import com.example.i18n.LanguagePreferenceManager
import com.example.util.Formatters
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class AiFinancialInsight(
  val headline: String,
  val statusLevel: String,
  val summary: String,
  val topCategoryHighlight: String,
  val actionTips: List<String>,
)

data class AiParsedExpense(
  val title: String,
  val amount: Long,
  val category: ExpenseCategory,
  val paymentMethod: PaymentMethod,
  val note: String,
)

class FinanceAiException(message: String, cause: Throwable? = null) : Exception(message, cause)

class FinanceAiAssistant {

  private val insightSchema = Schema.obj(
    mapOf(
      "headline" to Schema.string("Short 1-sentence financial summary headline in the requested target language"),
      "statusLevel" to Schema.enumeration(
        values = listOf("AMAN", "PERHATIAN", "BOROS"),
        description = "Monthly spending health status: AMAN (on track), PERHATIAN (near limit), BOROS (over budget)",
      ),
      "summary" to Schema.string("2-3 sentence financial analysis summary in the requested target language"),
      "topCategoryHighlight" to Schema.string("Highlight of the highest or most notable spending category in the requested target language"),
      "actionTips" to Schema.array(
        items = Schema.string("Practical, concrete saving tip in the requested target language"),
        description = "Exactly 3 realistic saving or budgeting recommendations in the requested target language",
      ),
    ),
  )

  private val expenseParserSchema = Schema.obj(
    mapOf(
      "title" to Schema.string("Concise, clear expense name in the user's language"),
      "amount" to Schema.integer("Total expense amount in the active currency (positive whole integer, e.g., 35000 or 25)"),
      "category" to Schema.enumeration(
        values = ExpenseCategory.entries.map { it.name },
        description = "Best matching expense category",
      ),
      "paymentMethod" to Schema.enumeration(
        values = PaymentMethod.entries.map { it.name },
        description = "Detected payment method (default QRIS if unspecified)",
      ),
      "note" to Schema.string("Brief additional details if present, or empty string"),
    ),
  )

  private val insightModel by lazy {
    Firebase.ai.generativeModel(
      modelName = MODEL_NAME,
      generationConfig = generationConfig {
        temperature = 0.4f
        responseMimeType = "application/json"
        responseSchema = insightSchema
      },
      systemInstruction = content {
        text(
          "You are SakuPintar's friendly, objective, practical personal finance advisor. " +
            "Always respond in the EXACT target language specified in the user prompt. " +
            "Analyze the user's monthly spending and budget data using the specified currency. " +
            "Give concrete, helpful recommendations based solely on the real numbers provided without inventing figures. " +
            "Keep statusLevel strictly one of AMAN, PERHATIAN, or BOROS."
        )
      },
    )
  }

  private val expenseParserModel by lazy {
    Firebase.ai.generativeModel(
      modelName = MODEL_NAME,
      generationConfig = generationConfig {
        temperature = 0.1f
        responseMimeType = "application/json"
        responseSchema = expenseParserSchema
      },
      systemInstruction = content {
        text(
          "Extract a single expense transaction from the user's text or receipt image in ANY language. " +
            "Convert shorthand amounts cleanly (e.g. Indonesian 'rb'/'ribu' * 1000, 'jt'/'juta' * 1000000; English 'k' * 1000; Chinese '万' * 10000). " +
            "Round decimal currency amounts to the nearest whole integer if needed, or preserve whole units for currencies like IDR/JPY/KRW. " +
            "If multiple items are mentioned in one transaction, sum the total amount and summarize items in title/note. " +
            "Pick the best matching category enum and paymentMethod enum (default QRIS if unclear). Keep title and note in the requested target language."
        )
      },
    )
  }

  suspend fun analyzeMonthlyFinances(
    monthKey: String,
    expenses: List<Expense>,
    budget: MonthlyBudget?,
    dailyInsight: DailyBudgetInsight,
    language: AppLanguage = LanguagePreferenceManager.currentLanguage,
    currency: AppCurrency = CurrencyPreferenceManager.currentCurrency,
  ): AiFinancialInsight {
    val locale = language.toLocale()
    val targetLangName = language.aiLanguageInstructionName()

    val categoryTotals = ExpenseCategory.entries.mapNotNull { cat ->
      val spent = expenses.filter { it.categoryEnum == cat }.sumOf { it.amount }
      val allocated = budget?.limitFor(cat) ?: 0L
      if (spent > 0L || allocated > 0L) {
        "- ${cat.name}: Spent ${Formatters.formatMoney(spent, currency)}, Limit ${if (allocated > 0L) Formatters.formatMoney(allocated, currency) else "Not set"}"
      } else {
        null
      }
    }.joinToString("\n").ifBlank { "- No category transactions recorded yet." }

    val recentItems = expenses.take(8).joinToString("\n") { item ->
      "- ${item.title}: ${Formatters.formatMoney(item.amount, currency)} (${item.categoryEnum.name}, ${Formatters.formatShortDate(item.dateEpochDay, locale)})"
    }.ifBlank { "- No transactions yet." }

    val prompt = buildString {
      appendLine("TARGET RESPONSE LANGUAGE: $targetLangName")
      appendLine("ACTIVE CURRENCY: ${currency.code} (${currency.symbol})")
      appendLine("Analyze personal finances for ${Formatters.formatMonthTitle(monthKey, locale)} ($monthKey):")
      appendLine("- Total spent this month: ${Formatters.formatMoney(dailyInsight.totalSpent, currency)} (${expenses.size} transactions)")
      if (budget != null) {
        appendLine("- Monthly income: ${Formatters.formatMoney(budget.monthlyIncome, currency)}")
        appendLine("- Total spend budget: ${Formatters.formatMoney(dailyInsight.totalBudget, currency)}")
        appendLine("- Savings target: ${Formatters.formatMoney(budget.savingsTarget, currency)} (${budget.savingsPercent}%)")
        appendLine("- Remaining budget this month: ${Formatters.formatMoney(dailyInsight.remainingBudget, currency)}")
        appendLine("- Daily safe spending limit: ${Formatters.formatMoney(dailyInsight.dailySafeLimit, currency)} (${dailyInsight.daysRemaining} days left)")
      } else {
        appendLine("- Monthly budget: Not configured yet.")
      }
      appendLine("- Spent today: ${Formatters.formatMoney(dailyInsight.todaySpent, currency)}")
      appendLine()
      appendLine("Category breakdown:")
      appendLine(categoryTotals)
      appendLine()
      appendLine("Recent transactions:")
      appendLine(recentItems)
    }

    return try {
      val response = withTimeout(AI_TIMEOUT_MS) {
        insightModel.generateContent(prompt)
      }
      val raw = response.text?.trim()
        ?: throw FinanceAiException("Gemini AI returned empty analysis.")
      val json = JSONObject(raw)
      val tipsArray = json.optJSONArray("actionTips")
      val tips = buildList {
        if (tipsArray != null) {
          for (i in 0 until tipsArray.length()) {
            val tip = tipsArray.optString(i).trim()
            if (tip.isNotEmpty()) add(tip)
          }
        }
      }
      AiFinancialInsight(
        headline = json.optString("headline").ifBlank { "Monthly Finance Summary" },
        statusLevel = json.optString("statusLevel").ifBlank { "AMAN" },
        summary = json.optString("summary").ifBlank { "Analysis generated for this month." },
        topCategoryHighlight = json.optString("topCategoryHighlight").ifBlank { "Monitor your highest spending category." },
        actionTips = tips.ifEmpty { listOf("Record every daily expense to keep your budget on track.") },
      )
    } catch (e: TimeoutCancellationException) {
      Log.w(TAG, "AI insight timed out", e)
      throw FinanceAiException("AI analysis timed out. Please check your connection and try again.", e)
    } catch (e: CancellationException) {
      throw e
    } catch (e: FinanceAiException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "AI insight failed", e)
      throw FinanceAiException("Failed to reach Gemini AI: ${e.localizedMessage ?: "Error"}", e)
    }
  }

  suspend fun parseNaturalExpense(
    rawInput: String,
    language: AppLanguage = LanguagePreferenceManager.currentLanguage,
    currency: AppCurrency = CurrencyPreferenceManager.currentCurrency,
  ): AiParsedExpense {
    val trimmed = rawInput.trim()
    if (trimmed.isEmpty()) {
      throw FinanceAiException("Enter an expense description first.")
    }

    val targetLangName = language.aiLanguageInstructionName()
    return try {
      val response = withTimeout(AI_TIMEOUT_MS) {
        expenseParserModel.generateContent(
          "Target language for title/note: $targetLangName. Active currency: ${currency.code} (${currency.symbol}).\n" +
            "User transaction text: \"$trimmed\""
        )
      }
      val raw = response.text?.trim()
        ?: throw FinanceAiException("Gemini AI did not return a parsed result.")
      val json = JSONObject(raw)
      val title = json.optString("title").trim()
      val amount = json.optLong("amount", 0L)
      val category = ExpenseCategory.fromKey(json.optString("category"))
      val paymentMethod = PaymentMethod.fromKey(json.optString("paymentMethod"))
      val note = json.optString("note").trim()

      if (title.isEmpty() || amount <= 0L) {
        throw FinanceAiException("Could not detect title or amount from your text.")
      }

      AiParsedExpense(
        title = title.take(80),
        amount = amount,
        category = category,
        paymentMethod = paymentMethod,
        note = note.take(200),
      )
    } catch (e: TimeoutCancellationException) {
      Log.w(TAG, "AI expense parse timed out", e)
      throw FinanceAiException("AI parsing timed out. Please try again.", e)
    } catch (e: CancellationException) {
      throw e
    } catch (e: FinanceAiException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "AI expense parse failed", e)
      throw FinanceAiException("Failed to process text with Gemini AI: ${e.localizedMessage ?: "Error"}", e)
    }
  }

  suspend fun parseReceiptImage(
    bitmap: Bitmap,
    language: AppLanguage = LanguagePreferenceManager.currentLanguage,
    currency: AppCurrency = CurrencyPreferenceManager.currentCurrency,
  ): AiParsedExpense {
    val targetLangName = language.aiLanguageInstructionName()
    return try {
      val response = withTimeout(AI_TIMEOUT_MS) {
        expenseParserModel.generateContent(
          content {
            image(bitmap)
            text(
              "Read this shopping receipt or payment proof image. " +
                "Target language for note/title: $targetLangName. Expected currency: ${currency.code} (${currency.symbol}). " +
                "Extract the merchant/store name as title, final total paid as integer amount, " +
                "best matching expense category, payment method (QRIS/TRANSFER/CASH/CARD), and a brief item summary as note."
            )
          }
        )
      }
      val raw = response.text?.trim()
        ?: throw FinanceAiException("Gemini AI did not return a receipt result.")
      val json = JSONObject(raw)
      val title = json.optString("title").trim()
      val amount = json.optLong("amount", 0L)
      val category = ExpenseCategory.fromKey(json.optString("category"))
      val paymentMethod = PaymentMethod.fromKey(json.optString("paymentMethod"))
      val note = json.optString("note").trim()

      if (title.isEmpty() || amount <= 0L) {
        throw FinanceAiException("Could not read merchant name or total clearly from receipt image. Try a clearer photo.")
      }

      AiParsedExpense(
        title = title.take(80),
        amount = amount,
        category = category,
        paymentMethod = paymentMethod,
        note = note.take(200),
      )
    } catch (e: TimeoutCancellationException) {
      Log.w(TAG, "AI receipt scan timed out", e)
      throw FinanceAiException("AI receipt scan timed out. Please try again.", e)
    } catch (e: CancellationException) {
      throw e
    } catch (e: FinanceAiException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "AI receipt scan failed", e)
      throw FinanceAiException("Failed to scan receipt with Gemini AI: ${e.localizedMessage ?: "Error"}", e)
    }
  }

  private companion object {
    const val TAG = "FinanceAiAssistant"
    const val MODEL_NAME = "gemini-3.8-flash"
    const val AI_TIMEOUT_MS = 60_000L
  }
}
