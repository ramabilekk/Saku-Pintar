package com.example.data.ai

import android.graphics.Bitmap
import android.os.Build
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
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import kotlin.math.roundToLong

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
      "statusLevel" to Schema.string("Monthly spending health status: must be one of AMAN, PERHATIAN, or BOROS"),
      "summary" to Schema.string("2-3 sentence financial analysis summary in the requested target language"),
      "topCategoryHighlight" to Schema.string("Highlight of the highest or most notable spending category in the requested target language"),
      "actionTips" to Schema.array(
        items = Schema.string("Practical, concrete saving tip in the requested target language"),
        description = "3 realistic saving or budgeting recommendations in the requested target language",
      ),
    ),
  )

  private val expenseParserSchema = Schema.obj(
    mapOf(
      "title" to Schema.string("Concise, clear expense name in the user's language"),
      "amount" to Schema.integer("Total expense amount in the active currency (positive whole integer, e.g., 35000 or 25)"),
      "category" to Schema.string("Best matching expense category: one of FOOD, BILLS, TRANSPORT, HEALTH, SHOPPING, ENTERTAINMENT, OTHER"),
      "paymentMethod" to Schema.string("Detected payment method: one of QRIS, CASH, TRANSFER, CARD (default QRIS if unspecified)"),
      "note" to Schema.string("Brief additional details if present, or empty string"),
    ),
  )

  private val insightSystemInstruction = content {
    text(
      "You are SakuPintar's friendly, objective, practical personal finance advisor. " +
        "Always respond in the EXACT target language specified in the user prompt. " +
        "Analyze the user's monthly spending and budget data using the specified currency. " +
        "Give concrete, helpful recommendations based solely on the real numbers provided without inventing figures. " +
        "Keep statusLevel strictly one of AMAN, PERHATIAN, or BOROS."
    )
  }

  private val expenseSystemInstruction = content {
    text(
      "Extract a single expense transaction from the user's text or receipt image in ANY language. " +
        "Convert shorthand amounts cleanly (e.g. Indonesian 'rb'/'ribu' * 1000, 'jt'/'juta' * 1000000; English 'k' * 1000; Chinese '万' * 10000). " +
        "Round decimal currency amounts to the nearest whole integer if needed, or preserve whole units for currencies like IDR/JPY/KRW. " +
        "If multiple items are mentioned in one transaction, sum the total amount and summarize items in title/note. " +
        "Pick the best matching category (FOOD, BILLS, TRANSPORT, HEALTH, SHOPPING, ENTERTAINMENT, OTHER) and paymentMethod (QRIS, CASH, TRANSFER, CARD). Keep title and note in the requested target language."
    )
  }

  private val primaryInsightModel by lazy {
    createInsightModel(PRIMARY_MODEL_NAME, useSchema = true)
  }

  private val fallbackInsightModel by lazy {
    createInsightModel(FALLBACK_MODEL_NAME, useSchema = true)
  }

  private val plainJsonInsightModel by lazy {
    createInsightModel(FALLBACK_MODEL_NAME, useSchema = false)
  }

  private val primaryExpenseParserModel by lazy {
    createExpenseParserModel(PRIMARY_MODEL_NAME, useSchema = true)
  }

  private val fallbackExpenseParserModel by lazy {
    createExpenseParserModel(FALLBACK_MODEL_NAME, useSchema = true)
  }

  private val plainJsonExpenseParserModel by lazy {
    createExpenseParserModel(FALLBACK_MODEL_NAME, useSchema = false)
  }

  private fun createInsightModel(modelName: String, useSchema: Boolean): GenerativeModel {
    return Firebase.ai.generativeModel(
      modelName = modelName,
      generationConfig = generationConfig {
        temperature = 0.4f
        responseMimeType = "application/json"
        if (useSchema) {
          responseSchema = insightSchema
        }
      },
      systemInstruction = insightSystemInstruction,
    )
  }

  private fun createExpenseParserModel(modelName: String, useSchema: Boolean): GenerativeModel {
    return Firebase.ai.generativeModel(
      modelName = modelName,
      generationConfig = generationConfig {
        temperature = 0.1f
        responseMimeType = "application/json"
        if (useSchema) {
          responseSchema = expenseParserSchema
        }
      },
      systemInstruction = expenseSystemInstruction,
    )
  }

  private suspend fun generateWithFallback(
    models: List<GenerativeModel>,
    action: suspend (GenerativeModel) -> String?,
  ): String {
    var lastError: Exception? = null
    for ((index, model) in models.withIndex()) {
      try {
        val text = withTimeout(AI_TIMEOUT_MS) {
          action(model)
        }?.trim()
        if (!text.isNullOrEmpty()) {
          return text
        }
      } catch (e: CancellationException) {
        if (e is TimeoutCancellationException) {
          Log.w(TAG, "Model attempt ${index + 1} timed out", e)
          lastError = e
        } else {
          throw e
        }
      } catch (e: Exception) {
        Log.w(TAG, "Model attempt ${index + 1} failed: ${e.message}", e)
        lastError = e
      }
    }
    if (lastError is TimeoutCancellationException) {
      throw FinanceAiException("AI request timed out. Please check your connection and try again.", lastError)
    }
    throw FinanceAiException(
      lastError?.localizedMessage ?: "Failed to reach Gemini AI.",
      lastError,
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
      appendLine()
      appendLine("Return JSON with keys: headline (string), statusLevel (AMAN|PERHATIAN|BOROS), summary (string), topCategoryHighlight (string), actionTips (array of 3 strings).")
    }

    return try {
      val raw = generateWithFallback(
        listOf(primaryInsightModel, fallbackInsightModel, plainJsonInsightModel),
      ) { model ->
        model.generateContent(prompt).text
      }
      val json = extractJsonObject(raw)
      val tipsArray = json.optJSONArray("actionTips")
      val tips = buildList {
        if (tipsArray != null) {
          for (i in 0 until tipsArray.length()) {
            val tip = tipsArray.optString(i).trim()
            if (tip.isNotEmpty()) add(tip)
          }
        }
      }
      val rawStatus = json.optString("statusLevel").uppercase().trim()
      val normalizedStatus = when {
        rawStatus.contains("BOROS") || rawStatus.contains("OVER") -> "BOROS"
        rawStatus.contains("PERHATIAN") || rawStatus.contains("CAUTION") -> "PERHATIAN"
        else -> "AMAN"
      }
      AiFinancialInsight(
        headline = json.optString("headline").ifBlank { "Monthly Finance Summary" },
        statusLevel = normalizedStatus,
        summary = json.optString("summary").ifBlank { "Analysis generated for this month." },
        topCategoryHighlight = json.optString("topCategoryHighlight").ifBlank { "Monitor your highest spending category." },
        actionTips = tips.ifEmpty { listOf("Record every daily expense to keep your budget on track.") },
      )
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
    val prompt = "Target language for title/note: $targetLangName. Active currency: ${currency.code} (${currency.symbol}).\n" +
      "User transaction text: \"$trimmed\"\n" +
      "Return JSON with keys: title (string), amount (integer), category (FOOD|BILLS|TRANSPORT|HEALTH|SHOPPING|ENTERTAINMENT|OTHER), paymentMethod (QRIS|CASH|TRANSFER|CARD), note (string)."

    return try {
      val raw = generateWithFallback(
        listOf(primaryExpenseParserModel, fallbackExpenseParserModel, plainJsonExpenseParserModel),
      ) { model ->
        model.generateContent(prompt).text
      }
      val json = extractJsonObject(raw)
      val title = json.optString("title").trim()
      val amount = parseFlexibleAmount(json, "amount")
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
    val safeBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bitmap.config == Bitmap.Config.HARDWARE) {
      bitmap.copy(Bitmap.Config.ARGB_8888, false)
    } else {
      bitmap
    }
    val targetLangName = language.aiLanguageInstructionName()
    val promptText = "Read this shopping receipt or payment proof image. " +
      "Target language for note/title: $targetLangName. Expected currency: ${currency.code} (${currency.symbol}). " +
      "Extract the merchant/store name as title, final total paid as integer amount, " +
      "best matching expense category (FOOD|BILLS|TRANSPORT|HEALTH|SHOPPING|ENTERTAINMENT|OTHER), " +
      "payment method (QRIS|TRANSFER|CASH|CARD), and a brief item summary as note. " +
      "Return JSON with keys: title, amount, category, paymentMethod, note."

    return try {
      val raw = generateWithFallback(
        listOf(primaryExpenseParserModel, fallbackExpenseParserModel, plainJsonExpenseParserModel),
      ) { model ->
        model.generateContent(
          content {
            image(safeBitmap)
            text(promptText)
          }
        ).text
      }
      val json = extractJsonObject(raw)
      val category = ExpenseCategory.fromKey(json.optString("category"))
      val title = json.optString("title").trim().ifEmpty { "Receipt (${category.name})" }
      val amount = parseFlexibleAmount(json, "amount")
      val paymentMethod = PaymentMethod.fromKey(json.optString("paymentMethod"))
      val note = json.optString("note").trim()

      if (amount <= 0L) {
        throw FinanceAiException("Could not read total amount clearly from receipt image. Try a clearer photo.")
      }

      AiParsedExpense(
        title = title.take(80),
        amount = amount,
        category = category,
        paymentMethod = paymentMethod,
        note = note.take(200),
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: FinanceAiException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "AI receipt scan failed", e)
      throw FinanceAiException("Failed to scan receipt with Gemini AI: ${e.localizedMessage ?: "Error"}", e)
    }
  }

  private fun extractJsonObject(raw: String): JSONObject {
    val cleaned = raw.trim()
      .removePrefix("```json")
      .removePrefix("```JSON")
      .removePrefix("```")
      .removeSuffix("```")
      .trim()
    val firstBrace = cleaned.indexOf('{')
    val lastBrace = cleaned.lastIndexOf('}')
    val jsonSlice = if (firstBrace >= 0 && lastBrace > firstBrace) {
      cleaned.substring(firstBrace, lastBrace + 1)
    } else {
      cleaned
    }
    return JSONObject(jsonSlice)
  }

  private fun parseFlexibleAmount(json: JSONObject, key: String): Long {
    val directLong = json.optLong(key, -1L)
    if (directLong > 0L) return directLong
    val directDouble = json.optDouble(key, -1.0)
    if (!directDouble.isNaN() && directDouble > 0.0) return directDouble.roundToLong()
    val rawStr = json.optString(key, "").trim()
    if (rawStr.isNotEmpty()) {
      val digits = rawStr.filter { it.isDigit() }
      val parsed = digits.toLongOrNull()
      if (parsed != null && parsed > 0L) return parsed
    }
    return 0L
  }

  private companion object {
    const val TAG = "FinanceAiAssistant"
    const val PRIMARY_MODEL_NAME = "gemini-3.8-flash"
    const val FALLBACK_MODEL_NAME = "gemini-3.5-flash-lite"
    const val AI_TIMEOUT_MS = 60_000L
  }
}
