package com.example.util

import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.budget.DailyBudgetInsight
import com.example.data.model.Expense
import com.example.data.model.MonthlyBudget
import com.example.i18n.AppCurrency
import com.example.i18n.CurrencyPreferenceManager
import com.example.i18n.LanguagePreferenceManager
import java.io.File
import java.io.OutputStream
import java.time.LocalDate
import java.util.Locale

enum class ReportFormat(
  val extension: String,
  val mimeType: String,
  val labelId: String,
  @StringRes val labelRes: Int,
) {
  EXCEL(
    extension = "xlsx",
    mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    labelId = "Excel (.xlsx)",
    labelRes = R.string.export_btn_excel,
  ),
  PDF(
    extension = "pdf",
    mimeType = "application/pdf",
    labelId = "Dokumen PDF (.pdf)",
    labelRes = R.string.export_pdf_title,
  ),
  CSV(
    extension = "csv",
    mimeType = "text/csv",
    labelId = "CSV (.csv)",
    labelRes = R.string.export_csv_title,
  );

  fun label(context: Context?): String = context?.getString(labelRes) ?: labelId
}

object ReportExporter {

  fun defaultFileName(monthKey: String, format: ReportFormat): String =
    "SakuPintar_${monthKey}.${format.extension}"

  fun buildMonthlyExcelBytes(
    monthKey: String,
    expenses: List<Expense>,
    budget: MonthlyBudget?,
    dailyInsight: DailyBudgetInsight,
    context: Context? = null,
    currency: AppCurrency = CurrencyPreferenceManager.currentCurrency,
  ): ByteArray = ExcelReportWriter.buildWorkbookBytes(
    monthKey = monthKey,
    expenses = expenses,
    budget = budget,
    dailyInsight = dailyInsight,
    context = context,
    currency = currency,
  )

  fun writeMonthlyPdf(
    outputStream: OutputStream,
    monthKey: String,
    expenses: List<Expense>,
    budget: MonthlyBudget?,
    dailyInsight: DailyBudgetInsight,
    context: Context? = null,
    currency: AppCurrency = CurrencyPreferenceManager.currentCurrency,
  ) {
    PdfReportWriter.writeReportPdf(
      outputStream = outputStream,
      monthKey = monthKey,
      expenses = expenses,
      budget = budget,
      dailyInsight = dailyInsight,
      context = context,
      currency = currency,
    )
  }

  fun writeMonthlyReport(
    outputStream: OutputStream,
    format: ReportFormat,
    monthKey: String,
    expenses: List<Expense>,
    budget: MonthlyBudget?,
    dailyInsight: DailyBudgetInsight,
    context: Context? = null,
    currency: AppCurrency = CurrencyPreferenceManager.currentCurrency,
  ) {
    when (format) {
      ReportFormat.EXCEL -> {
        outputStream.write(buildMonthlyExcelBytes(monthKey, expenses, budget, dailyInsight, context, currency))
        outputStream.flush()
      }
      ReportFormat.PDF -> {
        writeMonthlyPdf(outputStream, monthKey, expenses, budget, dailyInsight, context, currency)
      }
      ReportFormat.CSV -> {
        val csv = buildMonthlyCsv(monthKey, expenses, budget, context, currency)
        outputStream.write(csv.toByteArray(Charsets.UTF_8))
        outputStream.flush()
      }
    }
  }

  fun createShareableReportUri(
    context: Context,
    format: ReportFormat,
    monthKey: String,
    expenses: List<Expense>,
    budget: MonthlyBudget?,
    dailyInsight: DailyBudgetInsight,
    currency: AppCurrency = CurrencyPreferenceManager.currentCurrency,
  ): Uri {
    val reportsDir = File(context.cacheDir, "reports").apply {
      if (!exists()) mkdirs()
    }
    reportsDir.listFiles()?.forEach { oldFile ->
      if (oldFile.isFile) oldFile.delete()
    }
    val targetFile = File(reportsDir, defaultFileName(monthKey, format))
    targetFile.outputStream().use { stream ->
      writeMonthlyReport(
        outputStream = stream,
        format = format,
        monthKey = monthKey,
        expenses = expenses,
        budget = budget,
        dailyInsight = dailyInsight,
        context = context,
        currency = currency,
      )
    }
    return FileProvider.getUriForFile(
      context,
      "${context.packageName}.fileprovider",
      targetFile,
    )
  }

  fun buildMonthlyCsv(
    monthKey: String,
    expenses: List<Expense>,
    budget: MonthlyBudget?,
    context: Context? = null,
    currency: AppCurrency = CurrencyPreferenceManager.currentCurrency,
  ): String {
    val locale: Locale = if (context != null) LanguagePreferenceManager.currentLocale() else Locale.forLanguageTag("id-ID")
    val colDate = context?.getString(R.string.report_col_date) ?: "Tanggal"
    val colTitle = context?.getString(R.string.report_col_title) ?: "Judul"
    val colCategory = context?.getString(R.string.report_col_category) ?: "Kategori"
    val colMethod = context?.getString(R.string.report_col_method) ?: "Metode Pembayaran"
    val colAmount = context?.getString(R.string.report_col_amount, currency.symbol) ?: "Nominal (Rp)"
    val colNote = context?.getString(R.string.report_col_note) ?: "Catatan"
    val colReceipt = context?.getString(R.string.report_col_receipt) ?: "Bukti Struk"
    val summaryLabel = context?.getString(R.string.report_sheet_summary) ?: "Ringkasan Bulan"
    val totalSpentLabel = context?.getString(R.string.hero_total_spent_month) ?: "Total Pengeluaran"
    val incomeLabel = context?.getString(R.string.budget_income_label, currency.symbol) ?: "Pendapatan Bulanan"
    val budgetLabel = context?.getString(R.string.budget_shopping_allocation) ?: "Total Anggaran Belanja"
    val savingsLabel = context?.getString(R.string.hero_savings_target) ?: "Target Tabungan"

    val sb = StringBuilder()
    sb.append('\uFEFF')
    sb.appendLine("$colDate,$colTitle,$colCategory,$colMethod,$colAmount,$colNote,$colReceipt")
    val sorted = expenses.sortedBy { it.dateEpochDay }
    val receiptItems = mutableListOf<Pair<Int, Expense>>()
    var receiptIndex = 1
    var totalReceipts = 0
    for (item in sorted) {
      val dateStr = LocalDate.ofEpochDay(item.dateEpochDay).toString()
      val receiptStatus = if (item.hasReceiptPhoto) {
        val currentIdx = receiptIndex++
        totalReceipts++
        receiptItems.add(currentIdx to item)
        context?.getString(R.string.report_receipt_yes, currentIdx) ?: "Terlampir (#$currentIdx)"
      } else {
        context?.getString(R.string.report_receipt_no) ?: "-"
      }
      sb.append(csvCell(dateStr)).append(',')
      sb.append(csvCell(item.title)).append(',')
      sb.append(csvCell(item.categoryEnum.label(context))).append(',')
      sb.append(csvCell(item.paymentMethodEnum.label(context))).append(',')
      sb.append(item.amount).append(',')
      sb.append(csvCell(item.note)).append(',')
      sb.appendLine(csvCell(receiptStatus))
    }
    sb.appendLine()
    sb.appendLine("$summaryLabel,${csvCell(Formatters.formatMonthTitle(monthKey, locale))}")
    sb.appendLine("$totalSpentLabel,${expenses.sumOf { it.amount }}")
    sb.appendLine("$colReceipt,$totalReceipts")
    if (budget != null) {
      sb.appendLine("$incomeLabel,${budget.monthlyIncome}")
      sb.appendLine("$budgetLabel,${budget.totalAllocatedSpendBudget}")
      sb.appendLine("$savingsLabel,${budget.savingsTarget}")
    }
    if (receiptItems.isNotEmpty()) {
      val receiptSectionTitle = context?.getString(R.string.report_receipt_section_title)
        ?: "Lampiran Foto Struk & Invoice"
      val colReceiptPhoto = context?.getString(R.string.report_col_receipt_photo) ?: "Foto Struk"
      sb.appendLine()
      sb.appendLine(csvCell(receiptSectionTitle))
      sb.appendLine("#,$colDate,$colTitle,$colCategory,$colMethod,$colAmount,$colNote,$colReceiptPhoto")
      for ((idx, item) in receiptItems) {
        val dateStr = LocalDate.ofEpochDay(item.dateEpochDay).toString()
        val refLabel = "Struk #$idx (xl/media/receipt_$idx.jpg)"
        sb.append(csvCell("#$idx")).append(',')
        sb.append(csvCell(dateStr)).append(',')
        sb.append(csvCell(item.title)).append(',')
        sb.append(csvCell(item.categoryEnum.label(context))).append(',')
        sb.append(csvCell(item.paymentMethodEnum.label(context))).append(',')
        sb.append(item.amount).append(',')
        sb.append(csvCell(item.note.ifBlank { "-" })).append(',')
        sb.appendLine(csvCell(refLabel))
      }
    }
    return sb.toString()
  }

  fun buildMonthlyShareText(
    monthKey: String,
    expenses: List<Expense>,
    budget: MonthlyBudget?,
    dailyInsight: DailyBudgetInsight,
    context: Context? = null,
    currency: AppCurrency = CurrencyPreferenceManager.currentCurrency,
  ): String {
    val locale: Locale = if (context != null) LanguagePreferenceManager.currentLocale() else Locale.forLanguageTag("id-ID")
    val monthTitle = Formatters.formatMonthTitle(monthKey, locale)
    val totalSpent = expenses.sumOf { it.amount }
    val receiptCount = expenses.count { it.hasReceiptPhoto }
    val byCategory = expenses.groupBy { it.categoryEnum }
      .mapValues { (_, items) -> items.sumOf { it.amount } }
      .entries
      .sortedByDescending { it.value }

    val subjectHeader = context?.getString(R.string.export_subject, monthTitle)
      ?: "Rekap Keuangan SakuPintar - $monthTitle"
    val totalLabel = context?.getString(R.string.hero_total_spent_month) ?: "Total Pengeluaran"
    val txLabel = context?.getString(R.string.export_tx_count, expenses.size) ?: "${expenses.size} transaksi"
    val receiptSummary = context?.getString(R.string.report_receipt_attached_count, receiptCount)
      ?: "$receiptCount foto struk terlampir"
    val budgetLabel = context?.getString(R.string.budget_shopping_allocation) ?: "Anggaran Belanja"
    val remainingLabel = context?.getString(R.string.hero_remaining_budget) ?: "Sisa Anggaran"
    val savingsLabel = context?.getString(R.string.hero_savings_target) ?: "Target Tabungan"
    val categoryHeader = context?.getString(R.string.dashboard_category_allocation) ?: "Rincian per Kategori"
    val recentHeader = context?.getString(R.string.dashboard_recent_expenses) ?: "5 Transaksi Terbaru"

    return buildString {
      appendLine(subjectHeader)
      appendLine("$totalLabel: ${Formatters.formatMoney(totalSpent, currency)} ($txLabel • $receiptSummary)")
      if (budget != null) {
        appendLine("$budgetLabel: ${Formatters.formatMoney(dailyInsight.totalBudget, currency)}")
        appendLine("$remainingLabel: ${Formatters.formatMoney(dailyInsight.remainingBudget, currency)}")
        appendLine("$savingsLabel: ${Formatters.formatMoney(budget.savingsTarget, currency)}")
      }
      if (byCategory.isNotEmpty()) {
        appendLine()
        appendLine("$categoryHeader:")
        byCategory.forEach { (cat, sum) ->
          appendLine("- ${cat.label(context)}: ${Formatters.formatMoney(sum, currency)}")
        }
      }
      if (expenses.isNotEmpty()) {
        appendLine()
        appendLine("$recentHeader:")
        expenses.take(5).forEach { item ->
          val receiptTag = if (item.hasReceiptPhoto) " [Struk]" else ""
          appendLine("- ${Formatters.formatShortDate(item.dateEpochDay, locale)}: ${item.title} (${Formatters.formatMoney(item.amount, currency)})$receiptTag")
        }
      }
    }.trim()
  }

  private fun csvCell(value: String): String {
    val sanitized = value.replace("\"", "\"\"").replace("\r", " ").replace("\n", " ")
    return "\"$sanitized\""
  }
}
