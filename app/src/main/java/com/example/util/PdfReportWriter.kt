package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
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
import kotlin.math.min

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
  private val receiptCardFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = Color.rgb(255, 255, 255)
    style = Paint.Style.FILL
  }
  private val receiptHeaderFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = Color.rgb(238, 242, 255)
    style = Paint.Style.FILL
  }
  private val receiptImageBgFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = Color.rgb(241, 245, 249)
    style = Paint.Style.FILL
  }
  private val whiteMatteFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = Color.WHITE
    style = Paint.Style.FILL
  }
  private val emeraldBadgeFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = Color.rgb(209, 250, 229)
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
  private val photoFrameBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = Color.rgb(148, 163, 184)
    style = Paint.Style.STROKE
    strokeWidth = 0.7f
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
    textSize = 11.5f
    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
  }
  private val cardTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = Color.rgb(15, 23, 42)
    textSize = 10f
    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
  }
  private val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = Color.WHITE
    textSize = 9.2f
    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
  }
  private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = Color.rgb(17, 24, 39)
    textSize = 9.0f
  }
  private val bodyBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = Color.rgb(17, 24, 39)
    textSize = 9.5f
    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
  }
  private val accentMetaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = Color.rgb(67, 56, 202)
    textSize = 8.4f
    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
  }
  private val emeraldTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = Color.rgb(6, 95, 70)
    textSize = 8.2f
    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
  }
  private val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = Color.rgb(100, 116, 139)
    textSize = 8.3f
  }
  private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

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

      val sorted = expenses.sortedBy { it.dateEpochDay }
      val receiptIndexByExpenseId = LinkedHashMap<String, Int>()
      val decodedBitmapByExpenseId = LinkedHashMap<String, Bitmap>()
      var nextReceiptIdx = 1
      for (item in sorted) {
        if (item.hasReceiptPhoto) {
          receiptIndexByExpenseId[item.id] = nextReceiptIdx++
          ImageUtils.decodeAvatarBitmap(item.receiptPhotoBase64)?.let { bmp ->
            if (bmp.width > 0 && bmp.height > 0) {
              decodedBitmapByExpenseId[item.id] = bmp
            }
          }
        }
      }
      val totalReceipts = receiptIndexByExpenseId.size

      val bannerTitle = context?.getString(R.string.report_banner_title) ?: "SakuPintar — Rekap Keuangan Bulanan"
      val periodLine = context?.getString(R.string.report_period_printed, monthTitle, printedDate)
        ?: "Periode: $monthTitle  •  Dicetak: $printedDate"
      val summaryTitle = context?.getString(R.string.report_sheet_summary) ?: "Ringkasan Bulan Ini"
      val totalSpentLabel = context?.getString(R.string.hero_total_spent_month) ?: "Total Pengeluaran"
      val txCountLabel = context?.getString(R.string.report_sheet_transactions) ?: "Jumlah Transaksi"
      val txCountVal = context?.getString(R.string.export_tx_count, expenses.size) ?: "${expenses.size} transaksi"
      val receiptSummaryLabel = context?.getString(R.string.report_col_receipt) ?: "Bukti Struk"
      val receiptSummaryVal = context?.getString(R.string.report_receipt_attached_count, totalReceipts)
        ?: "$totalReceipts foto struk terlampir"
      val incomeLabel = context?.getString(R.string.budget_income_label, currency.symbol) ?: "Pendapatan Bulanan"
      val budgetLabel = context?.getString(R.string.budget_shopping_allocation) ?: "Anggaran Belanja"
      val savingsLabel = context?.getString(R.string.hero_savings_target) ?: "Target Tabungan"
      val remainingLabel = context?.getString(R.string.hero_remaining_budget) ?: "Sisa Anggaran"
      val categoryTitle = context?.getString(R.string.dashboard_category_allocation) ?: "Rekap per Kategori"
      val txSectionTitle = context?.getString(R.string.report_sheet_transactions) ?: "Rincian Transaksi"
      val receiptGalleryTitle = context?.getString(R.string.report_receipt_section_title)
        ?: "Lampiran Foto Struk & Invoice"
      val emptyTxText = context?.getString(R.string.empty_expenses_title) ?: "Belum ada transaksi tercatat pada bulan ini."

      val colCategory = context?.getString(R.string.report_col_category) ?: "Kategori"
      val colBudget = context?.getString(R.string.nav_budget) ?: "Anggaran"
      val colSpent = context?.getString(R.string.hero_spent_label, "")?.trim() ?: "Terpakai"
      val colRem = context?.getString(R.string.cat_remaining, "")?.trim() ?: "Sisa"
      val colDate = context?.getString(R.string.report_col_date) ?: "Tanggal"
      val colTitle = context?.getString(R.string.report_col_title) ?: "Judul & Catatan"
      val colMethod = context?.getString(R.string.report_col_method) ?: "Metode"
      val colReceipt = context?.getString(R.string.report_col_receipt) ?: "Struk"
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
        add(txCountLabel to "$txCountVal • $receiptSummaryVal")
        if (budget != null) {
          add(incomeLabel to Formatters.formatMoney(budget.monthlyIncome, currency))
          add(budgetLabel to Formatters.formatMoney(dailyInsight.totalBudget, currency))
          add(savingsLabel to Formatters.formatMoney(budget.savingsTarget, currency))
          add(remainingLabel to Formatters.formatMoney(dailyInsight.remainingBudget, currency))
        } else {
          add(receiptSummaryLabel to receiptSummaryVal)
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
      if (y + 56f > BOTTOM_LIMIT) {
        drawFooter(canvas, monthTitle, pageNumber, context)
        pdf.finishPage(page)
        pageNumber++
        page = pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        canvas = page.canvas
        y = MARGIN
      }

      canvas.drawText("$txSectionTitle (${expenses.size})", MARGIN, y, sectionPaint)
      y += 8f
      y = drawTransactionTableHeader(canvas, y, colDate, colTitle, colCategory, colMethod, colReceipt, colAmount)

      if (sorted.isEmpty()) {
        canvas.drawText(emptyTxText, MARGIN + 6f, y + 16f, mutedPaint)
        y += 24f
      } else {
        sorted.forEachIndexed { idx, item ->
          val receiptIdx = receiptIndexByExpenseId[item.id]
          val thumbBitmap = decodedBitmapByExpenseId[item.id]
          val rowH = if (receiptIdx != null) 30f else 20f

          if (y + rowH > BOTTOM_LIMIT) {
            drawFooter(canvas, monthTitle, pageNumber, context)
            pdf.finishPage(page)
            pageNumber++
            page = pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            canvas = page.canvas
            y = MARGIN
            canvas.drawText(txSectionTitle, MARGIN, y, sectionPaint)
            y += 8f
            y = drawTransactionTableHeader(canvas, y, colDate, colTitle, colCategory, colMethod, colReceipt, colAmount)
          }

          if (idx % 2 == 1) {
            canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowH, zebraFill)
          }
          canvas.drawLine(MARGIN, y + rowH, PAGE_WIDTH - MARGIN, y + rowH, borderPaint)

          val textBaselineY = y + (rowH / 2f) + 3.5f
          val dateStr = LocalDate.ofEpochDay(item.dateEpochDay)
            .format(DateTimeFormatter.ofPattern("dd/MM/yy"))
          val titleLabel = fitText(
            if (item.note.isBlank()) item.title else "${item.title} (${item.note})",
            bodyPaint,
            150f,
          )

          canvas.drawText(dateStr, MARGIN + 6f, textBaselineY, bodyPaint)
          canvas.drawText(titleLabel, MARGIN + 56f, textBaselineY, bodyPaint)
          canvas.drawText(fitText(item.categoryEnum.shortLabel(context), bodyPaint, 66f), MARGIN + 212f, textBaselineY, bodyPaint)
          canvas.drawText(fitText(item.paymentMethodEnum.label(context), bodyPaint, 60f), MARGIN + 282f, textBaselineY, bodyPaint)

          if (receiptIdx != null) {
            drawInlineReceiptCell(
              canvas = canvas,
              cellLeft = MARGIN + 348f,
              rowTop = y,
              rowHeight = rowH,
              receiptNumber = receiptIdx,
              bitmap = thumbBitmap,
            )
          } else {
            val noReceiptText = context?.getString(R.string.report_receipt_no) ?: "-"
            canvas.drawText(noReceiptText, MARGIN + 356f, textBaselineY, mutedPaint)
          }

          drawRightText(canvas, Formatters.formatMoney(item.amount, currency), PAGE_WIDTH - MARGIN - 6f, textBaselineY, bodyBoldPaint)
          y += rowH
        }
      }

      // 2-Column Executive Receipt / Invoice Photo Gallery Section
      val expensesWithReceipts = sorted.filter { it.hasReceiptPhoto }
      if (expensesWithReceipts.isNotEmpty()) {
        y += 22f
        val gridCardHeight = 208f
        val gridGap = 12f
        val gridCardWidth = (CONTENT_WIDTH - gridGap) / 2f

        if (y + 28f + gridCardHeight > BOTTOM_LIMIT) {
          drawFooter(canvas, monthTitle, pageNumber, context)
          pdf.finishPage(page)
          pageNumber++
          page = pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
          canvas = page.canvas
          y = MARGIN
        }

        canvas.drawText("$receiptGalleryTitle (${expensesWithReceipts.size})", MARGIN, y, sectionPaint)
        y += 10f

        expensesWithReceipts.chunked(2).forEach { rowPair ->
          if (y + gridCardHeight > BOTTOM_LIMIT) {
            drawFooter(canvas, monthTitle, pageNumber, context)
            pdf.finishPage(page)
            pageNumber++
            page = pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            canvas = page.canvas
            y = MARGIN
            canvas.drawText("$receiptGalleryTitle (${expensesWithReceipts.size})", MARGIN, y, sectionPaint)
            y += 10f
          }

          rowPair.forEachIndexed { colIdx, item ->
            val receiptIdx = receiptIndexByExpenseId[item.id] ?: 1
            val cardLeft = MARGIN + colIdx * (gridCardWidth + gridGap)
            drawReceiptGridCard(
              canvas = canvas,
              leftX = cardLeft,
              topY = y,
              cardWidth = gridCardWidth,
              cardHeight = gridCardHeight,
              receiptNumber = receiptIdx,
              expense = item,
              bitmap = decodedBitmapByExpenseId[item.id],
              context = context,
              currency = currency,
            )
          }
          y += gridCardHeight + gridGap
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

  /**
   * Draws a compact inline thumbnail + `#N` badge inside the transaction ledger table row.
   */
  private fun drawInlineReceiptCell(
    canvas: Canvas,
    cellLeft: Float,
    rowTop: Float,
    rowHeight: Float,
    receiptNumber: Int,
    bitmap: Bitmap?,
  ) {
    var badgeLeft = cellLeft
    if (bitmap != null && bitmap.width > 0 && bitmap.height > 0) {
      val thumbSize = rowHeight - 6f
      val thumbTop = rowTop + 3f
      val thumbRect = RectF(cellLeft, thumbTop, cellLeft + thumbSize, thumbTop + thumbSize)
      canvas.drawRoundRect(thumbRect, 4f, 4f, whiteMatteFill)
      val scale = min((thumbSize - 2f) / bitmap.width.toFloat(), (thumbSize - 2f) / bitmap.height.toFloat())
      val dw = bitmap.width * scale
      val dh = bitmap.height * scale
      val dx = cellLeft + (thumbSize - dw) / 2f
      val dy = thumbTop + (thumbSize - dh) / 2f
      canvas.drawBitmap(
        bitmap,
        Rect(0, 0, bitmap.width, bitmap.height),
        RectF(dx, dy, dx + dw, dy + dh),
        bitmapPaint,
      )
      canvas.drawRoundRect(thumbRect, 4f, 4f, photoFrameBorderPaint)
      badgeLeft = cellLeft + thumbSize + 5f
    }

    val badgeText = "#$receiptNumber"
    val pillW = emeraldTextPaint.measureText(badgeText) + 12f
    val pillH = 15f
    val pillTop = rowTop + (rowHeight - pillH) / 2f
    val pillRect = RectF(badgeLeft, pillTop, badgeLeft + pillW, pillTop + pillH)
    canvas.drawRoundRect(pillRect, 5f, 5f, emeraldBadgeFill)
    canvas.drawText(badgeText, badgeLeft + 6f, pillTop + 10.5f, emeraldTextPaint)
  }

  /**
   * Draws a structured 2-column executive receipt card with a header bar,
   * aspect-ratio-centered framed photo viewport, and 3-line transaction metadata footer.
   */
  private fun drawReceiptGridCard(
    canvas: Canvas,
    leftX: Float,
    topY: Float,
    cardWidth: Float,
    cardHeight: Float,
    receiptNumber: Int,
    expense: Expense,
    bitmap: Bitmap?,
    context: Context?,
    currency: AppCurrency,
  ) {
    val cardRect = RectF(leftX, topY, leftX + cardWidth, topY + cardHeight)
    canvas.drawRoundRect(cardRect, 10f, 10f, receiptCardFill)

    // 1. Top Header Bar (26pt)
    val headerHeight = 26f
    val headerPath = Path().apply {
      addRoundRect(
        RectF(leftX, topY, leftX + cardWidth, topY + headerHeight),
        floatArrayOf(10f, 10f, 10f, 10f, 0f, 0f, 0f, 0f),
        Path.Direction.CW,
      )
    }
    canvas.drawPath(headerPath, receiptHeaderFill)
    canvas.drawLine(leftX, topY + headerHeight, leftX + cardWidth, topY + headerHeight, borderPaint)

    val dateStr = LocalDate.ofEpochDay(expense.dateEpochDay)
      .format(DateTimeFormatter.ofPattern("dd MMM yy"))
    val badgeText = "STRUK #$receiptNumber • $dateStr"
    val badgeW = (emeraldTextPaint.measureText(badgeText) + 12f).coerceAtMost(cardWidth * 0.56f)
    val badgeRect = RectF(leftX + 8f, topY + 5f, leftX + 8f + badgeW, topY + 21f)
    canvas.drawRoundRect(badgeRect, 5f, 5f, emeraldBadgeFill)
    canvas.drawText(fitText(badgeText, emeraldTextPaint, badgeW - 8f), leftX + 14f, topY + 15.8f, emeraldTextPaint)

    val amountText = Formatters.formatMoney(expense.amount, currency)
    drawRightText(
      canvas,
      fitText(amountText, bodyBoldPaint, cardWidth * 0.40f),
      leftX + cardWidth - 8f,
      topY + 16.5f,
      bodyBoldPaint,
    )

    // 2. Framed Photo Viewport (124pt)
    val viewportLeft = leftX + 8f
    val viewportTop = topY + headerHeight + 6f
    val viewportWidth = cardWidth - 16f
    val viewportHeight = 122f
    val viewportRect = RectF(viewportLeft, viewportTop, viewportLeft + viewportWidth, viewportTop + viewportHeight)
    canvas.drawRoundRect(viewportRect, 7f, 7f, receiptImageBgFill)

    val resolvedBitmap = bitmap ?: ImageUtils.decodeAvatarBitmap(expense.receiptPhotoBase64)
    if (resolvedBitmap != null && resolvedBitmap.width > 0 && resolvedBitmap.height > 0) {
      val innerPad = 6f
      val availW = viewportWidth - innerPad * 2f
      val availH = viewportHeight - innerPad * 2f
      val scale = min(availW / resolvedBitmap.width.toFloat(), availH / resolvedBitmap.height.toFloat())
      val drawW = resolvedBitmap.width * scale
      val drawH = resolvedBitmap.height * scale
      val drawLeft = viewportLeft + (viewportWidth - drawW) / 2f
      val drawTop = viewportTop + (viewportHeight - drawH) / 2f

      // White matte frame around actual photo dimensions
      val matteRect = RectF(drawLeft - 2.5f, drawTop - 2.5f, drawLeft + drawW + 2.5f, drawTop + drawH + 2.5f)
      canvas.drawRoundRect(matteRect, 4f, 4f, whiteMatteFill)

      val srcRect = Rect(0, 0, resolvedBitmap.width, resolvedBitmap.height)
      val dstRect = RectF(drawLeft, drawTop, drawLeft + drawW, drawTop + drawH)
      canvas.drawBitmap(resolvedBitmap, srcRect, dstRect, bitmapPaint)
      canvas.drawRoundRect(matteRect, 4f, 4f, photoFrameBorderPaint)
    } else {
      val fallbackLabel = context?.getString(R.string.receipt_attached_badge) ?: "Struk Terlampir"
      canvas.drawText(
        fitText(fallbackLabel, mutedPaint, viewportWidth - 20f),
        viewportLeft + 12f,
        viewportTop + viewportHeight / 2f,
        mutedPaint,
      )
    }
    canvas.drawRoundRect(viewportRect, 7f, 7f, borderPaint)

    // 3. Structured 3-Line Metadata Footer
    val metaLeft = leftX + 10f
    val maxMetaW = cardWidth - 20f
    val metaTop = viewportTop + viewportHeight + 15f

    canvas.drawText(
      fitText(expense.title, cardTitlePaint, maxMetaW),
      metaLeft,
      metaTop,
      cardTitlePaint,
    )

    val catAndMethod = "${expense.categoryEnum.label(context)}  •  ${expense.paymentMethodEnum.label(context)}"
    canvas.drawText(
      fitText(catAndMethod, accentMetaPaint, maxMetaW),
      metaLeft,
      metaTop + 14f,
      accentMetaPaint,
    )

    val noteLine = expense.note.takeIf { it.isNotBlank() }
      ?: (context?.getString(R.string.receipt_attached_badge) ?: "Bukti struk terverifikasi")
    canvas.drawText(
      fitText(noteLine, mutedPaint, maxMetaW),
      metaLeft,
      metaTop + 27f,
      mutedPaint,
    )

    // Outer Card Border
    canvas.drawRoundRect(cardRect, 10f, 10f, borderPaint)
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
    colReceipt: String,
    colAmount: String,
  ): Float {
    canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 22f, indigoFill)
    canvas.drawText(fitText(colDate, tableHeaderPaint, 48f), MARGIN + 6f, y + 15f, tableHeaderPaint)
    canvas.drawText(fitText(colTitle, tableHeaderPaint, 150f), MARGIN + 56f, y + 15f, tableHeaderPaint)
    canvas.drawText(fitText(colCategory, tableHeaderPaint, 66f), MARGIN + 212f, y + 15f, tableHeaderPaint)
    canvas.drawText(fitText(colMethod, tableHeaderPaint, 60f), MARGIN + 282f, y + 15f, tableHeaderPaint)
    canvas.drawText(fitText(colReceipt, tableHeaderPaint, 72f), MARGIN + 348f, y + 15f, tableHeaderPaint)
    drawRightText(canvas, fitText(colAmount, tableHeaderPaint, 95f), PAGE_WIDTH - MARGIN - 6f, y + 15f, tableHeaderPaint)
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
