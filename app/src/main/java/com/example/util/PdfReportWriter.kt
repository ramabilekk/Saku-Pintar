package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.R
import com.example.data.budget.DailyBudgetInsight
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.MonthlyBudget
import com.example.i18n.AppCurrency
import com.example.i18n.CurrencyPreferenceManager
import com.example.i18n.LanguagePreferenceManager
import java.io.OutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

internal object PdfReportWriter {

  private const val PAGE_WIDTH = 595
  private const val PAGE_HEIGHT = 842
  private const val MARGIN = 36f
  private const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN * 2f
  private const val FOOTER_Y = PAGE_HEIGHT - 24f
  private const val BOTTOM_LIMIT = PAGE_HEIGHT - 46f

  private val indigoFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = Color.rgb(67, 56, 202)
    style = Paint.Style.FILL
  }
  private val softIndigoFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = Color.rgb(238, 242, 255)
    style = Paint.Style.FILL
  }
  private val zebraFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = Color.rgb(248, 250, 252)
    style = Paint.Style.FILL
  }
  private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = Color.rgb(203, 213, 225)
    style = Paint.Style.STROKE
    strokeWidth = 0.8f
  }
  private val headerTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = Color.WHITE
    textSize = 16f
    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
  }
  private val headerSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = Color.argb(230, 255, 255, 255)
    textSize = 10f
  }
  private val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = Color.rgb(49, 46, 129)
    textSize = 12f
    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
  }
  private val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = Color.WHITE
    textSize = 9.5f
    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
  }
  private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = Color.rgb(17, 24, 39)
    textSize = 9.5f
  }
  private val bodyBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = Color.rgb(17, 24, 39)
    textSize = 9.5f
    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
  }
  private val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = Color.rgb(100, 116, 139)
    textSize = 8.5f
  }

  fun writeReportPdf(
    outputStream: OutputStream,
    monthKey: String,
    expenses: List<Expense>,
    budget: MonthlyBudget?,
    dailyInsight: DailyBudgetInsight,
    context: Context? = null,
    currency: AppCurrency = CurrencyPreferenceManager.currentCurrency,
  ) {
    val pdf = PdfDocument()
    try {
      val locale = if (context != null) LanguagePreferenceManager.currentLocale() else Locale.forLanguageTag("id-ID")
      val monthTitle = Formatters.formatMonthTitle(monthKey, locale)
      val printedDate = LocalDate.now()
        .format(DateTimeFormatter.ofPattern("d MMM yyyy", locale))

      val bannerTitle = context?.getString(R.string.report_banner_title) ?: "SakuPintar — Rekap Keuangan Bulanan"
      val periodLine = context?.getString(R.string.report_period_printed, monthTitle, printedDate)
        ?: "Periode: $monthTitle  •  Dicetak: $printedDate"
      val summaryTitle = context?.getString(R.string.report_sheet_summary) ?: "Ringkasan Bulan Ini"
      val totalSpentLabel = context?.getString(R.string.hero_total_spent_month) ?: "Total Pengeluaran"
      val txCountLabel = context?.getString(R.string.report_sheet_transactions) ?: "Jumlah Transaksi"
      val txCountVal = context?.getString(R.string.export_tx_count, expenses.size) ?: "${expenses.size} transaksi"
      val incomeLabel = context?.getString(R.string.budget_income_label, currency.symbol) ?: "Pendapatan Bulanan"
      val budgetLabel = context?.getString(R.string.budget_shopping_allocation) ?: "Anggaran Belanja"
      val savingsLabel = context?.getString(R.string.hero_savings_target) ?: "Target Tabungan"
      val remainingLabel = context?.getString(R.string.hero_remaining_budget) ?: "Sisa Anggaran"
      val categoryTitle = context?.getString(R.string.dashboard_category_allocation) ?: "Rekap per Kategori"
      val txSectionTitle = context?.getString(R.string.report_sheet_transactions) ?: "Rincian Transaksi"
      val emptyTxText = context?.getString(R.string.empty_expenses_title) ?: "Belum ada transaksi tercatat pada bulan ini."

      val colCategory = context?.getString(R.string.report_col_category) ?: "Kategori"
      val colBudget = context?.getString(R.string.nav_budget) ?: "Anggaran"
      val colSpent = context?.getString(R.string.hero_spent_label, "")?.trim() ?: "Terpakai"
      val colRem = context?.getString(R.string.cat_remaining, "")?.trim() ?: "Sisa"
      val colDate = context?.getString(R.string.report_col_date) ?: "Tanggal"
      val colTitle = context?.getString(R.string.report_col_title) ?: "Judul & Catatan"
      val colMethod = context?.getString(R.string.report_col_method) ?: "Metode"
      val colAmount = context?.getString(R.string.report_col_amount, currency.symbol) ?: "Nominal"

      var pageNumber = 1
      var page = pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
      var canvas = page.canvas
      var y = MARGIN

      // Banner header
      val bannerRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 64f)
      canvas.drawRoundRect(bannerRect, 12f, 12f, indigoFill)
      canvas.drawText(fitText(bannerTitle, headerTitlePaint, CONTENT_WIDTH - 32f), MARGIN + 16f, y + 26f, headerTitlePaint)
      canvas.drawText(fitText(periodLine, headerSubPaint, CONTENT_WIDTH - 32f), MARGIN + 16f, y + 46f, headerSubPaint)
      y += 82f

      // Summary cards
      canvas.drawText(summaryTitle, MARGIN, y, sectionPaint)
      y += 10f
      val totalSpent = expenses.sumOf { it.amount }
      val summaryItems = buildList {
        add(totalSpentLabel to Formatters.formatMoney(totalSpent, currency))
        add(txCountLabel to txCountVal)
        if (budget != null) {
          add(incomeLabel to Formatters.formatMoney(budget.monthlyIncome, currency))
          add(budgetLabel to Formatters.formatMoney(dailyInsight.totalBudget, currency))
          add(savingsLabel to Formatters.formatMoney(budget.savingsTarget, currency))
          add(remainingLabel to Formatters.formatMoney(dailyInsight.remainingBudget, currency))
        }
      }
      val colW = (CONTENT_WIDTH - 10f) / 2f
      summaryItems.chunked(2).forEach { pair ->
        pair.forEachIndexed { idx, (label, value) ->
          val left = MARGIN + idx * (colW + 10f)
          val box = RectF(left, y, left + colW, y + 40f)
          canvas.drawRoundRect(box, 8f, 8f, softIndigoFill)
          canvas.drawText(fitText(label, mutedPaint, colW - 20f), left + 10f, y + 15f, mutedPaint)
          canvas.drawText(fitText(value, bodyBoldPaint, colW - 20f), left + 10f, y + 31f, bodyBoldPaint)
        }
        y += 46f
      }

      y += 8f

      // Category breakdown table
      canvas.drawText(categoryTitle, MARGIN, y, sectionPaint)
      y += 8f
      y = drawCategoryTableHeader(canvas, y, colCategory, colBudget, colSpent, colRem)
      val spentByCategory = expenses.groupBy { it.categoryEnum }
        .mapValues { (_, items) -> items.sumOf { it.amount } }
      ExpenseCategory.entries.forEachIndexed { index, category ->
        val allocated = budget?.limitFor(category) ?: 0L
        val spent = spentByCategory[category] ?: 0L
        val remaining = allocated - spent
        val pct = if (allocated > 0L) "${((spent * 100) / allocated)}%" else "-"
        if (index % 2 == 1) {
          canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 20f, zebraFill)
        }
        canvas.drawLine(MARGIN, y + 20f, PAGE_WIDTH - MARGIN, y + 20f, borderPaint)
        canvas.drawText(fitText(category.label(context), bodyPaint, 145f), MARGIN + 6f, y + 14f, bodyPaint)
        drawRightText(canvas, Formatters.formatMoney(allocated, currency), MARGIN + 255f, y + 14f, bodyPaint)
        drawRightText(canvas, Formatters.formatMoney(spent, currency), MARGIN + 355f, y + 14f, bodyPaint)
        drawRightText(canvas, Formatters.formatMoney(remaining, currency), MARGIN + 455f, y + 14f, bodyPaint)
        drawRightText(canvas, pct, PAGE_WIDTH - MARGIN - 6f, y + 14f, bodyPaint)
        y += 20f
      }

      y += 18f

      // Transactions Ledger
      if (y + 52f > BOTTOM_LIMIT) {
        drawFooter(canvas, monthTitle, pageNumber, context)
        pdf.finishPage(page)
        pageNumber++
        page = pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        canvas = page.canvas
        y = MARGIN
      }

      canvas.drawText("$txSectionTitle (${expenses.size})", MARGIN, y, sectionPaint)
      y += 8f
      y = drawTransactionTableHeader(canvas, y, colDate, colTitle, colCategory, colMethod, colAmount)

      val sorted = expenses.sortedBy { it.dateEpochDay }
      if (sorted.isEmpty()) {
        canvas.drawText(emptyTxText, MARGIN + 6f, y + 16f, mutedPaint)
      } else {
        sorted.forEachIndexed { idx, item ->
          if (y + 20f > BOTTOM_LIMIT) {
            drawFooter(canvas, monthTitle, pageNumber, context)
            pdf.finishPage(page)
            pageNumber++
            page = pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            canvas = page.canvas
            y = MARGIN
            canvas.drawText(txSectionTitle, MARGIN, y, sectionPaint)
            y += 8f
            y = drawTransactionTableHeader(canvas, y, colDate, colTitle, colCategory, colMethod, colAmount)
          }

          if (idx % 2 == 1) {
            canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 20f, zebraFill)
          }
          canvas.drawLine(MARGIN, y + 20f, PAGE_WIDTH - MARGIN, y + 20f, borderPaint)

          val dateStr = LocalDate.ofEpochDay(item.dateEpochDay)
            .format(DateTimeFormatter.ofPattern("dd/MM/yy"))
          val titleLabel = fitText(
            if (item.note.isBlank()) item.title else "${item.title} (${item.note})",
            bodyPaint,
            175f,
          )
          canvas.drawText(dateStr, MARGIN + 6f, y + 14f, bodyPaint)
          canvas.drawText(titleLabel, MARGIN + 64f, y + 14f, bodyPaint)
          canvas.drawText(fitText(item.categoryEnum.shortLabel(context), bodyPaint, 78f), MARGIN + 248f, y + 14f, bodyPaint)
          canvas.drawText(fitText(item.paymentMethodEnum.label(context), bodyPaint, 78f), MARGIN + 334f, y + 14f, bodyPaint)
          drawRightText(canvas, Formatters.formatMoney(item.amount, currency), PAGE_WIDTH - MARGIN - 6f, y + 14f, bodyBoldPaint)
          y += 20f
        }
      }

      drawFooter(canvas, monthTitle, pageNumber, context)
      pdf.finishPage(page)
      pdf.writeTo(outputStream)
      outputStream.flush()
    } finally {
      pdf.close()
    }
  }

  private fun drawCategoryTableHeader(
    canvas: Canvas,
    y: Float,
    colCategory: String,
    colBudget: String,
    colSpent: String,
    colRem: String,
  ): Float {
    canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 22f, indigoFill)
    canvas.drawText(fitText(colCategory, tableHeaderPaint, 140f), MARGIN + 6f, y + 15f, tableHeaderPaint)
    drawRightText(canvas, fitText(colBudget, tableHeaderPaint, 85f), MARGIN + 255f, y + 15f, tableHeaderPaint)
    drawRightText(canvas, fitText(colSpent, tableHeaderPaint, 85f), MARGIN + 355f, y + 15f, tableHeaderPaint)
    drawRightText(canvas, fitText(colRem, tableHeaderPaint, 85f), MARGIN + 455f, y + 15f, tableHeaderPaint)
    drawRightText(canvas, "%", PAGE_WIDTH - MARGIN - 6f, y + 15f, tableHeaderPaint)
    return y + 22f
  }

  private fun drawTransactionTableHeader(
    canvas: Canvas,
    y: Float,
    colDate: String,
    colTitle: String,
    colCategory: String,
    colMethod: String,
    colAmount: String,
  ): Float {
    canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 22f, indigoFill)
    canvas.drawText(fitText(colDate, tableHeaderPaint, 52f), MARGIN + 6f, y + 15f, tableHeaderPaint)
    canvas.drawText(fitText(colTitle, tableHeaderPaint, 175f), MARGIN + 64f, y + 15f, tableHeaderPaint)
    canvas.drawText(fitText(colCategory, tableHeaderPaint, 78f), MARGIN + 248f, y + 15f, tableHeaderPaint)
    canvas.drawText(fitText(colMethod, tableHeaderPaint, 78f), MARGIN + 334f, y + 15f, tableHeaderPaint)
    drawRightText(canvas, fitText(colAmount, tableHeaderPaint, 100f), PAGE_WIDTH - MARGIN - 6f, y + 15f, tableHeaderPaint)
    return y + 22f
  }

  private fun drawFooter(canvas: Canvas, monthTitle: String, pageNumber: Int, context: Context?) {
    canvas.drawLine(MARGIN, FOOTER_Y - 12f, PAGE_WIDTH - MARGIN, FOOTER_Y - 12f, borderPaint)
    val leftFooter = context?.getString(R.string.export_subject, monthTitle) ?: "SakuPintar • Rekap $monthTitle"
    val pageText = context?.getString(R.string.report_page_footer, pageNumber) ?: "Halaman $pageNumber"
    canvas.drawText(leftFooter, MARGIN, FOOTER_Y, mutedPaint)
    drawRightText(canvas, pageText, PAGE_WIDTH - MARGIN, FOOTER_Y, mutedPaint)
  }

  private fun drawRightText(canvas: Canvas, text: String, rightX: Float, baselineY: Float, paint: Paint) {
    val width = paint.measureText(text)
    canvas.drawText(text, rightX - width, baselineY, paint)
  }

  private fun fitText(text: String, paint: Paint, maxWidth: Float): String {
    val clean = text.replace('\n', ' ').trim()
    if (paint.measureText(clean) <= maxWidth) return clean
    val ellipsis = "..."
    val targetWidth = (maxWidth - paint.measureText(ellipsis)).coerceAtLeast(10f)
    val count = paint.breakText(clean, true, targetWidth, null)
    return clean.take(count.coerceAtLeast(1)) + ellipsis
  }
}
