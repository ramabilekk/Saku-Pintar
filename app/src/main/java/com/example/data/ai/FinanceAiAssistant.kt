package com.example.data.ai

import android.util.Log
import com.example.data.budget.DailyBudgetInsight
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.MonthlyBudget
import com.example.data.model.PaymentMethod
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
      "headline" to Schema.string("Judul singkat 1 kalimat dalam Bahasa Indonesia"),
      "statusLevel" to Schema.enumeration(
        values = listOf("AMAN", "PERHATIAN", "BOROS"),
        description = "Status kesehatan pengeluaran bulan ini",
      ),
      "summary" to Schema.string("Ringkasan analisis keuangan 2-3 kalimat dalam Bahasa Indonesia"),
      "topCategoryHighlight" to Schema.string("Sorotan kategori pengeluaran terbesar atau yang perlu diawasi"),
      "actionTips" to Schema.array(
        items = Schema.string("Saran praktis dan spesifik dalam Bahasa Indonesia"),
        description = "Tepat 3 saran penghematan atau pengelolaan anggaran yang realistis",
      ),
    ),
  )

  private val expenseParserSchema = Schema.obj(
    mapOf(
      "title" to Schema.string("Nama pengeluaran yang ringkas dan jelas dalam Bahasa Indonesia"),
      "amount" to Schema.integer("Total nominal pengeluaran dalam Rupiah (bilangan bulat positif, contoh: 35000)"),
      "category" to Schema.enumeration(
        values = ExpenseCategory.entries.map { it.name },
        description = "Kategori pengeluaran yang paling sesuai",
      ),
      "paymentMethod" to Schema.enumeration(
        values = PaymentMethod.entries.map { it.name },
        description = "Metode pembayaran yang terdeteksi (default QRIS jika tidak disebut)",
      ),
      "note" to Schema.string("Catatan detail singkat jika ada rincian tambahan, atau string kosong"),
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
          "Anda adalah penasihat keuangan pribadi SakuPintar berbahasa Indonesia yang ramah, objektif, dan praktis. " +
            "Analisis data pengeluaran dan anggaran bulanan pengguna menggunakan mata uang Rupiah (Rp). " +
            "Berikan saran yang konkret berdasarkan angka nyata yang diberikan tanpa mengarang angka baru."
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
          "Ekstrak satu transaksi pengeluaran dari kalimat Bahasa Indonesia pengguna. " +
            "Konversi singkatan nominal Indonesia seperti 'rb'/'ribu' (* 1000) dan 'jt'/'juta' (* 1000000). " +
            "Jika pengguna menyebut beberapa item dalam satu transaksi (misal 'soto 28rb dan es teh 7rb'), jumlahkan totalnya (35000) dan rangkum di judul/catatan. " +
            "Pilih kategori yang paling tepat dari daftar enum dan metode pembayaran dari enum (default QRIS bila tidak disebutkan)."
        )
      },
    )
  }

  suspend fun analyzeMonthlyFinances(
    monthKey: String,
    expenses: List<Expense>,
    budget: MonthlyBudget?,
    dailyInsight: DailyBudgetInsight,
  ): AiFinancialInsight {
    val categoryTotals = ExpenseCategory.entries.mapNotNull { cat ->
      val spent = expenses.filter { it.categoryEnum == cat }.sumOf { it.amount }
      val allocated = budget?.limitFor(cat) ?: 0L
      if (spent > 0L || allocated > 0L) {
        "- ${cat.labelId} (${cat.name}): Terpakai ${Formatters.formatRupiah(spent)}, Batas ${if (allocated > 0L) Formatters.formatRupiah(allocated) else "Belum diatur"}"
      } else {
        null
      }
    }.joinToString("\n").ifBlank { "- Belum ada transaksi kategori tercatat." }

    val recentItems = expenses.take(8).joinToString("\n") { item ->
      "- ${item.title}: ${Formatters.formatRupiah(item.amount)} (${item.categoryEnum.labelId}, ${Formatters.formatShortDate(item.dateEpochDay)})"
    }.ifBlank { "- Belum ada transaksi." }

    val prompt = buildString {
      appendLine("Analisis keuangan pribadi untuk bulan ${Formatters.formatMonthTitle(monthKey)} ($monthKey):")
      appendLine("- Total pengeluaran bulan ini: ${Formatters.formatRupiah(dailyInsight.totalSpent)} (${expenses.size} transaksi)")
      if (budget != null) {
        appendLine("- Pendapatan bulanan: ${Formatters.formatRupiah(budget.monthlyIncome)}")
        appendLine("- Total anggaran belanja: ${Formatters.formatRupiah(dailyInsight.totalBudget)}")
        appendLine("- Target tabungan: ${Formatters.formatRupiah(budget.savingsTarget)} (${budget.savingsPercent}%)")
        appendLine("- Sisa anggaran bulan ini: ${Formatters.formatRupiah(dailyInsight.remainingBudget)}")
        appendLine("- Batas aman harian tersisa: ${Formatters.formatRupiah(dailyInsight.dailySafeLimit)} (${dailyInsight.daysRemaining} hari tersisa)")
      } else {
        appendLine("- Anggaran bulanan: Belum disusun.")
      }
      appendLine("- Pengeluaran hari ini: ${Formatters.formatRupiah(dailyInsight.todaySpent)}")
      appendLine()
      appendLine("Rincian per kategori:")
      appendLine(categoryTotals)
      appendLine()
      appendLine("Transaksi terbaru:")
      appendLine(recentItems)
    }

    return try {
      val response = withTimeout(AI_TIMEOUT_MS) {
        insightModel.generateContent(prompt)
      }
      val raw = response.text?.trim()
        ?: throw FinanceAiException("Gemini AI tidak mengembalikan teks analisis.")
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
        headline = json.optString("headline").ifBlank { "Ringkasan Keuangan Bulan Ini" },
        statusLevel = json.optString("statusLevel").ifBlank { "AMAN" },
        summary = json.optString("summary").ifBlank { "Analisis berhasil dibuat untuk bulan ini." },
        topCategoryHighlight = json.optString("topCategoryHighlight").ifBlank { "Pantau kategori pengeluaran terbesar Anda." },
        actionTips = tips.ifEmpty { listOf("Catat setiap transaksi harian agar kontrol anggaran tetap akurat.") },
      )
    } catch (e: TimeoutCancellationException) {
      Log.w(TAG, "AI insight timed out", e)
      throw FinanceAiException("Analisis AI melebihi batas waktu. Pastikan koneksi internet stabil lalu coba lagi.", e)
    } catch (e: CancellationException) {
      throw e
    } catch (e: FinanceAiException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "AI insight failed", e)
      throw FinanceAiException("Gagal menghubungi Gemini AI: ${e.localizedMessage ?: "Terjadi kesalahan"}", e)
    }
  }

  suspend fun parseNaturalExpense(rawInput: String): AiParsedExpense {
    val trimmed = rawInput.trim()
    if (trimmed.isEmpty()) {
      throw FinanceAiException("Tuliskan deskripsi pengeluaran terlebih dahulu.")
    }

    return try {
      val response = withTimeout(AI_TIMEOUT_MS) {
        expenseParserModel.generateContent(
          "Teks transaksi pengguna: \"$trimmed\""
        )
      }
      val raw = response.text?.trim()
        ?: throw FinanceAiException("Gemini AI tidak mengembalikan hasil pembacaan.")
      val json = JSONObject(raw)
      val title = json.optString("title").trim()
      val amount = json.optLong("amount", 0L)
      val category = ExpenseCategory.fromKey(json.optString("category"))
      val paymentMethod = PaymentMethod.fromKey(json.optString("paymentMethod"))
      val note = json.optString("note").trim()

      if (title.isEmpty() || amount <= 0L) {
        throw FinanceAiException("Nominal atau nama pengeluaran tidak terbaca. Contoh: \"Kopi susu 25rb QRIS\".")
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
      throw FinanceAiException("Pembacaan AI melebihi batas waktu. Coba lagi sebentar.", e)
    } catch (e: CancellationException) {
      throw e
    } catch (e: FinanceAiException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "AI expense parse failed", e)
      throw FinanceAiException("Gagal memproses teks dengan Gemini AI: ${e.localizedMessage ?: "Terjadi kesalahan"}", e)
    }
  }

  private companion object {
    const val TAG = "FinanceAiAssistant"
    const val MODEL_NAME = "gemini-3.8-flash"
    const val AI_TIMEOUT_MS = 60_000L
  }
}
