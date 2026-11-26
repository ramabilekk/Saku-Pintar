package com.example.data.ai

import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import com.example.data.budget.DailyBudgetInsight
import com.example.data.budget.PaydayCycleManager
import com.example.data.local.CashflowEntry
import com.example.data.local.CashflowType
import com.example.data.local.CustomWallet
import com.example.data.local.DebtRecord
import com.example.data.local.DebtType
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.MonthlyBudget
import com.example.data.model.PaymentMethod
import com.example.data.model.SavingsGoal
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
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
  val detectedEpochDay: Long? = null,
)

data class AiChatMessage(
  val id: String = UUID.randomUUID().toString(),
  val isUser: Boolean,
  val text: String,
  val isError: Boolean = false,
)

data class AiFinanceContextSnapshot(
  val monthKey: String,
  val paydayStartDay: Int = 1,
  val periodExpenses: List<Expense>,
  val allHistoricalExpenses: List<Expense>,
  val budget: MonthlyBudget?,
  val dailyInsight: DailyBudgetInsight,
  val walletBalances: Map<PaymentMethod, Long> = emptyMap(),
  val savingsGoals: List<SavingsGoal> = emptyList(),
  val cashflowEntries: List<CashflowEntry> = emptyList(),
  val customWallets: List<CustomWallet> = emptyList(),
  val debtRecords: List<DebtRecord> = emptyList(),
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
    properties = mapOf(
      "title" to Schema.string("Concise, clear merchant or expense name in the user's language"),
      "amount" to Schema.integer("Final grand total expense amount paid in the active currency (positive whole integer, e.g., 35000 or 25)"),
      "category" to Schema.string("Best matching expense category: one of FOOD, BILLS, TRANSPORT, HEALTH, SHOPPING, ENTERTAINMENT, OTHER"),
      "paymentMethod" to Schema.string("Detected payment method: one of QRIS, CASH, TRANSFER, CARD (default QRIS if unspecified)"),
      "note" to Schema.string("Brief itemized summary or details if present, or empty string"),
      "dateIso" to Schema.string("Transaction date printed on receipt or mentioned in text in YYYY-MM-DD format, or empty string if not visible"),
    ),
    optionalProperties = listOf("dateIso"),
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

  private val chatSystemInstruction = content {
    text(
      "You are SakuPintar AI, a sharp, warm, practical personal finance coach inside the SakuPintar Android app. " +
        "Answer questions concisely in the EXACT target language specified in the context block (Indonesian if Indonesian, English if English, etc.). " +
        "Base EVERY number, category comparison, wallet balance, and savings recommendation strictly on the LIVE USER FINANCIAL SNAPSHOT provided. " +
        "Never invent transactions or amounts that are not in the snapshot. " +
        "Keep answers well-structured with short paragraphs or clean bullet points using plain dashes (-), avoiding heavy markdown tables."
    )
  }

  private val primaryChatModel by lazy {
    Firebase.ai.generativeModel(
      modelName = PRIMARY_MODEL_NAME,
      generationConfig = generationConfig {
        temperature = 0.55f
      },
      systemInstruction = chatSystemInstruction,
    )
  }

  private val fallbackChatModel by lazy {
    Firebase.ai.generativeModel(
      modelName = FALLBACK_MODEL_NAME,
      generationConfig = generationConfig {
        temperature = 0.55f
      },
      systemInstruction = chatSystemInstruction,
    )
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
      val detectedEpochDay = parseOptionalDateEpochDay(json.optString("dateIso"))

      if (title.isEmpty() || amount <= 0L) {
        throw FinanceAiException("Could not detect title or amount from your text.")
      }

      AiParsedExpense(
        title = title.take(80),
        amount = amount,
        category = category,
        paymentMethod = paymentMethod,
        note = note.take(200),
        detectedEpochDay = detectedEpochDay,
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
    val promptText = "Read this shopping receipt, invoice, or digital payment proof image carefully. " +
      "Target language for note/title: $targetLangName. Expected currency: ${currency.code} (${currency.symbol}). " +
      "Extract: (1) merchant/store name as title, (2) final GRAND TOTAL paid after tax/discount as integer amount (ignore change/kembalian), " +
      "(3) best matching expense category (FOOD|BILLS|TRANSPORT|HEALTH|SHOPPING|ENTERTAINMENT|OTHER), " +
      "(4) payment method (QRIS|TRANSFER|CASH|CARD), " +
      "(5) concise item list summary as note (up to 4 main items), and " +
      "(6) transaction date printed on the receipt in YYYY-MM-DD format as dateIso (empty string if unreadable). " +
      "Return JSON with keys: title, amount, category, paymentMethod, note, dateIso."

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
      val detectedEpochDay = parseOptionalDateEpochDay(json.optString("dateIso"))

      if (amount <= 0L) {
        throw FinanceAiException("Could not read total amount clearly from receipt image. Try a clearer photo.")
      }

      AiParsedExpense(
        title = title.take(80),
        amount = amount,
        category = category,
        paymentMethod = paymentMethod,
        note = note.take(200),
        detectedEpochDay = detectedEpochDay,
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

  fun streamFinanceChatReply(
    userQuestion: String,
    conversationHistory: List<AiChatMessage>,
    snapshot: AiFinanceContextSnapshot,
    language: AppLanguage = LanguagePreferenceManager.currentLanguage,
    currency: AppCurrency = CurrencyPreferenceManager.currentCurrency,
  ): Flow<String> = flow {
    val cleanQuestion = userQuestion.trim()
    if (cleanQuestion.isEmpty()) {
      throw FinanceAiException("Please enter a question first.")
    }

    val contextBlock = buildLiveContextPrompt(snapshot, language, currency)
    val priorTurns = conversationHistory
      .filter { !it.isError && it.text.isNotBlank() }
      .takeLast(8)
      .map { msg ->
        content(if (msg.isUser) "user" else "model") {
          text(msg.text)
        }
      }

    val seededHistory = buildList {
      add(content("user") { text(contextBlock) })
      add(
        content("model") {
          text("Understood. I have your live SakuPintar financial snapshot ready and will answer strictly from your real data.")
        }
      )
      addAll(priorTurns)
    }

    val models = listOf(primaryChatModel, fallbackChatModel)
    var emittedAnyToken = false
    var lastError: Exception? = null

    for ((index, model) in models.withIndex()) {
      try {
        val chat = model.startChat(history = seededHistory)
        val accumulated = StringBuilder()
        withTimeout(AI_TIMEOUT_MS) {
          chat.sendMessageStream(cleanQuestion).collect { chunk ->
            val piece = chunk.text.orEmpty()
            if (piece.isNotEmpty()) {
              emittedAnyToken = true
              accumulated.append(piece)
              emit(accumulated.toString())
            }
          }
        }
        if (accumulated.isNotBlank()) {
          return@flow
        }
      } catch (e: TimeoutCancellationException) {
        Log.w(TAG, "Chat stream attempt ${index + 1} timed out", e)
        lastError = e
        if (emittedAnyToken) break
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "Chat stream attempt ${index + 1} failed", e)
        lastError = e
        if (emittedAnyToken) break
      }
    }

    if (lastError is TimeoutCancellationException) {
      throw FinanceAiException("AI response timed out after 60 seconds. Please try again.", lastError)
    }
    throw FinanceAiException(
      lastError?.localizedMessage ?: "Failed to reach Gemini AI. Please check your connection.",
      lastError,
    )
  }

  private fun buildLiveContextPrompt(
    snapshot: AiFinanceContextSnapshot,
    language: AppLanguage,
    currency: AppCurrency,
  ): String {
    val locale = language.toLocale()
    val cycle = PaydayCycleManager.resolveCycle(snapshot.monthKey, snapshot.paydayStartDay)
    val periodLabel = if (cycle.isCustomCycle) {
      "${Formatters.formatMonthTitle(snapshot.monthKey, locale)} (Payday Cycle: ${cycle.formatRangeLabel(locale)})"
    } else {
      Formatters.formatMonthTitle(snapshot.monthKey, locale)
    }

    val categorySummary = ExpenseCategory.entries.mapNotNull { cat ->
      val spent = snapshot.periodExpenses.filter { it.categoryEnum == cat }.sumOf { it.amount }
      val limit = snapshot.budget?.limitFor(cat) ?: 0L
      if (spent > 0L || limit > 0L) {
        "- ${cat.name}: Spent ${Formatters.formatMoney(spent, currency)}" +
          if (limit > 0L) " / Budget ${Formatters.formatMoney(limit, currency)}" else ""
      } else null
    }.joinToString("\n").ifBlank { "- No spending recorded in this period yet." }

    val recentExpenses = snapshot.periodExpenses.take(12).joinToString("\n") { e ->
      "- ${e.title}: ${Formatters.formatMoney(e.amount, currency)} (${e.categoryEnum.name}, ${e.paymentMethodEnum.name}, ${Formatters.formatShortDate(e.dateEpochDay, locale)})"
    }.ifBlank { "- None yet." }

    val walletSummary = PaymentMethod.entries.joinToString(", ") { method ->
      "${method.name}: ${Formatters.formatMoney(snapshot.walletBalances[method] ?: 0L, currency)}"
    }

    val totalWallet = PaymentMethod.entries.sumOf { snapshot.walletBalances[it] ?: 0L }
    val savingsSummary = snapshot.savingsGoals.joinToString("\n") { g ->
      "- ${g.title} (${g.categoryEnum.name}): Saved ${Formatters.formatMoney(g.savedAmount, currency)} / Target ${Formatters.formatMoney(g.targetAmount, currency)} (${(g.progressFraction * 100).toInt()}%)"
    }.ifBlank { "- No savings goals set yet." }

    val incomeEntries = snapshot.cashflowEntries.filter { it.type == CashflowType.INCOME }
    val periodIncome = if (cycle.isCustomCycle) {
      incomeEntries.filter { cycle.contains(it.dateEpochDay) }.sumOf { it.amount }
    } else {
      incomeEntries.filter { it.monthKey == snapshot.monthKey }.sumOf { it.amount }
    }

    val endYm = runCatching { YearMonth.parse(snapshot.monthKey) }.getOrElse { YearMonth.now() }
    val sixMonthTrend = (5 downTo 0).joinToString("; ") { offset ->
      val ym = endYm.minusMonths(offset.toLong())
      val key = ym.toString()
      val c = PaydayCycleManager.resolveCycle(key, snapshot.paydayStartDay)
      val spent = if (c.isCustomCycle) {
        snapshot.allHistoricalExpenses.filter { c.contains(it.dateEpochDay) }.sumOf { it.amount }
      } else {
        snapshot.allHistoricalExpenses.filter { it.monthKey == key }.sumOf { it.amount }
      }
      "$key: ${Formatters.formatMoney(spent, currency)}"
    }

    return buildString {
      appendLine("LIVE USER FINANCIAL SNAPSHOT (USE AS GROUND TRUTH):")
      appendLine("- Target Reply Language: ${language.aiLanguageInstructionName()}")
      appendLine("- Active Currency: ${currency.code} (${currency.symbol})")
      appendLine("- Active Period: $periodLabel")
      appendLine("- Total Spent This Period: ${Formatters.formatMoney(snapshot.dailyInsight.totalSpent, currency)} (${snapshot.periodExpenses.size} transactions)")
      appendLine("- Recorded Cashflow Income This Period: ${Formatters.formatMoney(periodIncome, currency)}")
      if (snapshot.budget != null) {
        appendLine("- Budget Monthly Income: ${Formatters.formatMoney(snapshot.budget.monthlyIncome, currency)}")
        appendLine("- Total Spend Budget: ${Formatters.formatMoney(snapshot.dailyInsight.totalBudget, currency)}")
        if (snapshot.dailyInsight.rolloverAmount > 0L) {
          appendLine("- Includes Rollover Bonus from Previous Period: +${Formatters.formatMoney(snapshot.dailyInsight.rolloverAmount, currency)}")
        }
        appendLine("- Remaining Budget: ${Formatters.formatMoney(snapshot.dailyInsight.remainingBudget, currency)}")
        appendLine("- Daily Safe Limit: ${Formatters.formatMoney(snapshot.dailyInsight.dailySafeLimit, currency)} (${snapshot.dailyInsight.daysRemaining} days remaining)")
        appendLine("- Spent Today: ${Formatters.formatMoney(snapshot.dailyInsight.todaySpent, currency)}")
      } else {
        appendLine("- Monthly Budget: Not configured yet")
      }
      appendLine("- Total Wallet Balance: ${Formatters.formatMoney(totalWallet, currency)} ($walletSummary)")
      if (snapshot.customWallets.isNotEmpty()) {
        val customTotal = snapshot.customWallets.sumOf { it.balance }
        val customDetails = snapshot.customWallets.joinToString(", ") { w ->
          "${w.name} (${w.walletType.name}): ${Formatters.formatMoney(w.balance, currency)}"
        }
        appendLine("- Custom Wallets Total: ${Formatters.formatMoney(customTotal, currency)} ($customDetails)")
      }
      appendLine()
      appendLine("Category Breakdown:")
      appendLine(categorySummary)
      appendLine()
      appendLine("Recent Period Expenses:")
      appendLine(recentExpenses)
      appendLine()
      appendLine("Savings Goals:")
      appendLine(savingsSummary)
      if (snapshot.debtRecords.isNotEmpty()) {
        val unpaid = snapshot.debtRecords.filter { !it.settled }
        val totalOwedToUser = unpaid.filter { it.type == DebtType.OWED_TO_ME }.sumOf { it.amount }
        val totalUserOwes = unpaid.filter { it.type == DebtType.I_OWE }.sumOf { it.amount }
        appendLine()
        appendLine("Debts & Split Bills:")
        appendLine("- Unpaid Receivables (Owed to User): ${Formatters.formatMoney(totalOwedToUser, currency)}")
        appendLine("- Unpaid Payables (User Owes): ${Formatters.formatMoney(totalUserOwes, currency)}")
        unpaid.take(10).forEach { d ->
          val direction = if (d.type == DebtType.OWED_TO_ME) "Owed by" else "Owe to"
          val dueStr = if (d.dueDateEpochDay > 0L) ", due ${Formatters.formatShortDate(d.dueDateEpochDay, locale)}" else ""
          appendLine("- $direction ${d.personName}: ${Formatters.formatMoney(d.amount, currency)}$dueStr" + if (d.note.isNotBlank()) " (${d.note})" else "")
        }
      }
      appendLine()
      appendLine("Last 6 Months Spending Trend: $sixMonthTrend")
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

  private fun parseOptionalDateEpochDay(rawIso: String?): Long? {
    val cleaned = rawIso?.trim().orEmpty()
    if (cleaned.length < 8) return null
    val match = Regex("""\d{4}-\d{2}-\d{2}""").find(cleaned)?.value ?: return null
    return runCatching {
      val parsed = LocalDate.parse(match)
      val now = LocalDate.now()
      if (parsed.year in 2020..(now.year + 1)) parsed.toEpochDay() else null
    }.getOrNull()
  }

  private companion object {
    const val TAG = "FinanceAiAssistant"
    const val PRIMARY_MODEL_NAME = "gemini-3.8-flash"
    const val FALLBACK_MODEL_NAME = "gemini-3.5-flash-lite"
    const val AI_TIMEOUT_MS = 60_000L
  }
}
