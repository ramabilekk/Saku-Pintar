package com.example.util

import android.content.Context
import android.graphics.BitmapFactory
import com.example.R
import com.example.data.budget.DailyBudgetInsight
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.MonthlyBudget
import com.example.data.model.PaymentMethod
import com.example.i18n.AppCurrency
import com.example.i18n.CurrencyPreferenceManager
import com.example.i18n.LanguagePreferenceManager
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.util.Base64
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.min

/**
 * Generates a valid OOXML (.xlsx) workbook in pure Kotlin with zero external dependencies,
 * compatible with both Microsoft Excel and Google Sheets (Spreadsheet).
 *
 * Sheet 1: Summary (Financial summary, Category breakdown, Payment method breakdown, Receipt count)
 * Sheet 2: Transactions (Full chronological ledger with live Excel =SUM formula, Receipt status, & inline cell thumbnails)
 * Sheet 3 (when receipts exist): Lampiran Struk (Dedicated Receipt & Invoice table with aspect-ratio-locked photos centered inside each row's cell)
 */
internal object ExcelReportWriter {

  private data class DecodedReceiptAttachment(
    val index: Int,
    val expense: Expense,
    val txRowIndex0: Int,
    val receiptSheetRowIndex0: Int,
    val imageBytes: ByteArray,
    val widthPx: Int,
    val heightPx: Int,
  )

  fun buildWorkbookBytes(
    monthKey: String,
    expenses: List<Expense>,
    budget: MonthlyBudget?,
    dailyInsight: DailyBudgetInsight,
    context: Context? = null,
    currency: AppCurrency = CurrencyPreferenceManager.currentCurrency,
  ): ByteArray {
    val summarySheetName = sanitizeSheetName(
      context?.getString(R.string.report_sheet_summary) ?: "Ringkasan Bulanan",
      fallback = "Summary",
    )
    val txSheetName = sanitizeSheetName(
      context?.getString(R.string.report_sheet_transactions) ?: "Rincian Transaksi",
      fallback = "Transactions",
    )
    val receiptSheetName = sanitizeSheetName(
      context?.getString(R.string.report_sheet_receipts) ?: "Lampiran Struk",
      fallback = "Receipts",
    )

    val sorted = expenses.sortedBy { it.dateEpochDay }
    val decodedReceipts = mutableListOf<DecodedReceiptAttachment>()
    val receiptIndexByExpenseId = LinkedHashMap<String, Int>()
    var nextIdx = 1
    var receiptRowCursor0 = 2 // Row 3 in 1-indexed Excel (0-indexed = 2)
    sorted.forEachIndexed { txIdx, item ->
      if (item.hasReceiptPhoto) {
        val bytes = decodeBase64Safely(item.receiptPhotoBase64)
        val assigned = nextIdx++
        receiptIndexByExpenseId[item.id] = assigned
        val currentReceiptRow0 = receiptRowCursor0++
        if (bytes != null && bytes.isNotEmpty()) {
          val (w, h) = extractImageDimensions(bytes)
          decodedReceipts.add(
            DecodedReceiptAttachment(
              index = assigned,
              expense = item,
              txRowIndex0 = txIdx + 1, // Row (txIdx + 2) in 1-indexed Sheet 2 -> 0-indexed = txIdx + 1
              receiptSheetRowIndex0 = currentReceiptRow0,
              imageBytes = bytes,
              widthPx = w,
              heightPx = h,
            )
          )
        }
      }
    }

    val hasReceiptSheet = receiptIndexByExpenseId.isNotEmpty()
    val hasEmbeddedImages = decodedReceipts.isNotEmpty()

    val baos = ByteArrayOutputStream()
    ZipOutputStream(baos).use { zip ->
      writeZipEntry(
        zip,
        "[Content_Types].xml",
        contentTypesXml(hasReceiptSheet = hasReceiptSheet, hasEmbeddedImages = hasEmbeddedImages),
      )
      writeZipEntry(zip, "_rels/.rels", rootRelsXml())
      writeZipEntry(
        zip,
        "xl/_rels/workbook.xml.rels",
        workbookRelsXml(hasReceiptSheet = hasReceiptSheet),
      )
      writeZipEntry(
        zip,
        "xl/workbook.xml",
        workbookXml(
          summarySheetName = summarySheetName,
          txSheetName = txSheetName,
          receiptSheetName = if (hasReceiptSheet) receiptSheetName else null,
        ),
      )
      writeZipEntry(zip, "xl/styles.xml", stylesXml(currency))
      writeZipEntry(
        zip,
        "xl/worksheets/sheet1.xml",
        buildSummarySheetXml(
          monthKey = monthKey,
          expenses = expenses,
          budget = budget,
          dailyInsight = dailyInsight,
          receiptCount = receiptIndexByExpenseId.size,
          context = context,
          currency = currency,
        ),
      )
      writeZipEntry(
        zip,
        "xl/worksheets/sheet2.xml",
        buildTransactionsSheetXml(
          expenses = sorted,
          receiptIndexByExpenseId = receiptIndexByExpenseId,
          hasDrawing = hasEmbeddedImages,
          context = context,
          currency = currency,
        ),
      )

      if (hasEmbeddedImages) {
        writeZipEntry(
          zip,
          "xl/worksheets/_rels/sheet2.xml.rels",
          sheetDrawingRelsXml(drawingFileName = "drawing2.xml"),
        )
        writeZipEntry(
          zip,
          "xl/drawings/drawing2.xml",
          buildTransactionsDrawingXml(decodedReceipts),
        )
        writeZipEntry(
          zip,
          "xl/drawings/_rels/drawing2.xml.rels",
          buildDrawingRelsXml(decodedReceipts),
        )
      }

      if (hasReceiptSheet) {
        writeZipEntry(
          zip,
          "xl/worksheets/sheet3.xml",
          buildReceiptsSheetXml(
            expensesWithReceipts = sorted.filter { it.hasReceiptPhoto },
            receiptIndexByExpenseId = receiptIndexByExpenseId,
            hasDrawing = hasEmbeddedImages,
            context = context,
            currency = currency,
          ),
        )
        if (hasEmbeddedImages) {
          writeZipEntry(
            zip,
            "xl/worksheets/_rels/sheet3.xml.rels",
            sheetDrawingRelsXml(drawingFileName = "drawing1.xml"),
          )
          writeZipEntry(
            zip,
            "xl/drawings/drawing1.xml",
            buildReceiptsDrawingXml(decodedReceipts),
          )
          writeZipEntry(
            zip,
            "xl/drawings/_rels/drawing1.xml.rels",
            buildDrawingRelsXml(decodedReceipts),
          )
          decodedReceipts.forEach { attachment ->
            writeZipBinaryEntry(
              zip,
              "xl/media/receipt_${attachment.index}.jpg",
              attachment.imageBytes,
            )
          }
        }
      }
    }
    return baos.toByteArray()
  }

  /**
   * Reads JPEG SOF / PNG IHDR dimensions directly from byte array (works in both Android runtime and JVM unit tests),
   * falling back to Android BitmapFactory or a balanced 3:4 receipt aspect ratio.
   */
  internal fun extractImageDimensions(bytes: ByteArray): Pair<Int, Int> {
    // 1. Check PNG IHDR
    if (bytes.size >= 24 &&
      bytes[0] == 0x89.toByte() &&
      bytes[1] == 0x50.toByte() &&
      bytes[2] == 0x4E.toByte() &&
      bytes[3] == 0x47.toByte()
    ) {
      val w = ((bytes[16].toInt() and 0xFF) shl 24) or
        ((bytes[17].toInt() and 0xFF) shl 16) or
        ((bytes[18].toInt() and 0xFF) shl 8) or
        (bytes[19].toInt() and 0xFF)
      val h = ((bytes[20].toInt() and 0xFF) shl 24) or
        ((bytes[21].toInt() and 0xFF) shl 16) or
        ((bytes[22].toInt() and 0xFF) shl 8) or
        (bytes[23].toInt() and 0xFF)
      if (w > 0 && h > 0) return w to h
    }

    // 2. Scan JPEG SOF markers (0xFFC0 .. 0xFFCF, excluding DHT/DAC/RST)
    if (bytes.size > 4 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()) {
      var pos = 2
      while (pos + 8 < bytes.size) {
        if (bytes[pos] != 0xFF.toByte()) {
          pos++
          continue
        }
        while (pos < bytes.size && bytes[pos] == 0xFF.toByte()) {
          pos++
        }
        if (pos >= bytes.size) break
        val marker = bytes[pos].toInt() and 0xFF
        pos++
        if (marker == 0xD8 || marker == 0xD9 || (marker in 0xD0..0xD7)) continue
        if (pos + 1 >= bytes.size) break
        val segmentLength = ((bytes[pos].toInt() and 0xFF) shl 8) or (bytes[pos + 1].toInt() and 0xFF)
        if (segmentLength < 2) break
        val isSofMarker = marker in 0xC0..0xCF && marker != 0xC4 && marker != 0xC8 && marker != 0xCC
        if (isSofMarker && pos + 6 < bytes.size) {
          val height = ((bytes[pos + 3].toInt() and 0xFF) shl 8) or (bytes[pos + 4].toInt() and 0xFF)
          val width = ((bytes[pos + 5].toInt() and 0xFF) shl 8) or (bytes[pos + 6].toInt() and 0xFF)
          if (width > 0 && height > 0) return width to height
        }
        pos += segmentLength
      }
    }

    // 3. Fallback to Android BitmapFactory bounds decoding when running on device
    runCatching {
      val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
      if (opts.outWidth > 0 && opts.outHeight > 0) {
        return opts.outWidth to opts.outHeight
      }
    }

    return 600 to 800
  }

  private fun decodeBase64Safely(rawBase64: String): ByteArray? {
    val clean = rawBase64.substringAfter("base64,", rawBase64).trim()
    if (clean.isEmpty()) return null
    return try {
      Base64.getMimeDecoder().decode(clean)
    } catch (_: Exception) {
      try {
        android.util.Base64.decode(clean, android.util.Base64.DEFAULT)
      } catch (_: Throwable) {
        null
      }
    }
  }

  private fun sanitizeSheetName(raw: String, fallback: String): String {
    val cleaned = raw
      .replace(':', ' ')
      .replace('\\', ' ')
      .replace('/', ' ')
      .replace('?', ' ')
      .replace('*', ' ')
      .replace('[', '(')
      .replace(']', ')')
      .trim()
      .take(31)
    return cleaned.ifBlank { fallback }
  }

  private fun writeZipEntry(zip: ZipOutputStream, path: String, xml: String) {
    zip.putNextEntry(ZipEntry(path))
    zip.write(xml.toByteArray(StandardCharsets.UTF_8))
    zip.closeEntry()
  }

  private fun writeZipBinaryEntry(zip: ZipOutputStream, path: String, bytes: ByteArray) {
    zip.putNextEntry(ZipEntry(path))
    zip.write(bytes)
    zip.closeEntry()
  }

  private fun buildSummarySheetXml(
    monthKey: String,
    expenses: List<Expense>,
    budget: MonthlyBudget?,
    dailyInsight: DailyBudgetInsight,
    receiptCount: Int,
    context: Context?,
    currency: AppCurrency,
  ): String {
    val locale: Locale = if (context != null) LanguagePreferenceManager.currentLocale() else Locale.forLanguageTag("id-ID")
    val monthTitle = Formatters.formatMonthTitle(monthKey, locale)
    val totalSpent = expenses.sumOf { it.amount }
    val spentByCategory = expenses.groupBy { it.categoryEnum }
      .mapValues { (_, items) -> items.sumOf { it.amount } }
    val byMethod = PaymentMethod.entries.map { method ->
      val matching = expenses.filter { it.paymentMethodEnum == method }
      Triple(method, matching.size, matching.sumOf { it.amount })
    }

    val bannerTitle = context?.getString(R.string.report_banner_title) ?: "Rekap Keuangan Bulanan SakuPintar"
    val sheetSummaryTitle = context?.getString(R.string.report_sheet_summary) ?: "Ringkasan Bulanan"
    val totalSpentLabel = context?.getString(R.string.hero_total_spent_month) ?: "Total Pengeluaran"
    val txLabel = context?.getString(R.string.report_sheet_transactions) ?: "Jumlah Transaksi"
    val receiptLabel = context?.getString(R.string.report_col_receipt) ?: "Bukti Struk"
    val incomeLabel = context?.getString(R.string.budget_income_label, currency.symbol) ?: "Pendapatan Bulanan"
    val budgetLabel = context?.getString(R.string.budget_shopping_allocation) ?: "Total Anggaran Belanja"
    val savingsLabel = context?.getString(R.string.hero_savings_target) ?: "Target Tabungan"
    val remainingLabel = context?.getString(R.string.hero_remaining_budget) ?: "Sisa Anggaran"
    val colCategory = context?.getString(R.string.report_col_category) ?: "Kategori"
    val colMethod = context?.getString(R.string.report_col_method) ?: "Metode Pembayaran"
    val colAmount = context?.getString(R.string.report_col_amount, currency.symbol) ?: "Nominal (Rp)"

    val rows = StringBuilder()
    var r = 1

    rows.append(rowXml(r++, listOf(textCell("A", r - 1, bannerTitle, style = 1)), heightPt = 28))
    rows.append(rowXml(r++, listOf(textCell("A", r - 1, monthTitle, style = 0)), heightPt = 20))
    r++ // Blank row

    rows.append(
      rowXml(
        r++,
        listOf(
          textCell("A", r - 1, sheetSummaryTitle, style = 2),
          textCell("B", r - 1, colAmount, style = 2),
        ),
        heightPt = 25,
      ),
    )
    rows.append(
      rowXml(
        r++,
        listOf(
          textCell("A", r - 1, totalSpentLabel, style = 3),
          numberCell("B", r - 1, totalSpent, style = 4),
        ),
        heightPt = 22,
      ),
    )
    rows.append(
      rowXml(
        r++,
        listOf(
          textCell("A", r - 1, txLabel, style = 3),
          numberCell("B", r - 1, expenses.size.toLong(), style = 8),
        ),
        heightPt = 22,
      ),
    )
    rows.append(
      rowXml(
        r++,
        listOf(
          textCell("A", r - 1, receiptLabel, style = 3),
          numberCell("B", r - 1, receiptCount.toLong(), style = 8),
        ),
        heightPt = 22,
      ),
    )
    if (budget != null) {
      rows.append(
        rowXml(
          r++,
          listOf(
            textCell("A", r - 1, incomeLabel, style = 3),
            numberCell("B", r - 1, budget.monthlyIncome, style = 4),
          ),
          heightPt = 22,
        ),
      )
      rows.append(
        rowXml(
          r++,
          listOf(
            textCell("A", r - 1, budgetLabel, style = 3),
            numberCell("B", r - 1, dailyInsight.totalBudget, style = 4),
          ),
          heightPt = 22,
        ),
      )
      rows.append(
        rowXml(
          r++,
          listOf(
            textCell("A", r - 1, savingsLabel, style = 3),
            numberCell("B", r - 1, budget.savingsTarget, style = 4),
          ),
          heightPt = 22,
        ),
      )
      rows.append(
        rowXml(
          r++,
          listOf(
            textCell("A", r - 1, remainingLabel, style = 5),
            numberCell("B", r - 1, dailyInsight.remainingBudget, style = 6),
          ),
          heightPt = 24,
        ),
      )
    }

    r++ // Blank row

    // Section 2: Category Breakdown
    rows.append(
      rowXml(
        r++,
        listOf(
          textCell("A", r - 1, colCategory, style = 2),
          textCell("B", r - 1, if (context != null) "$budgetLabel (${currency.symbol})" else "Anggaran (Rp)", style = 2),
          textCell("C", r - 1, if (context != null) "$totalSpentLabel (${currency.symbol})" else "Terpakai (Rp)", style = 2),
          textCell("D", r - 1, if (context != null) "$remainingLabel (${currency.symbol})" else "Sisa (Rp)", style = 2),
          textCell("E", r - 1, "%", style = 2),
        ),
        heightPt = 25,
      ),
    )
    var sumAllocated = 0L
    for (category in ExpenseCategory.entries) {
      val allocated = budget?.limitFor(category) ?: 0L
      val spent = spentByCategory[category] ?: 0L
      val remaining = allocated - spent
      val ratio = if (allocated > 0L) spent.toDouble() / allocated.toDouble() else 0.0
      sumAllocated += allocated
      rows.append(
        rowXml(
          r++,
          listOf(
            textCell("A", r - 1, category.label(context), style = 3),
            numberCell("B", r - 1, allocated, style = 4),
            numberCell("C", r - 1, spent, style = 4),
            numberCell("D", r - 1, remaining, style = 4),
            decimalCell("E", r - 1, ratio, style = 7),
          ),
          heightPt = 22,
        ),
      )
    }
    val totalRatio = if (sumAllocated > 0L) totalSpent.toDouble() / sumAllocated.toDouble() else 0.0
    rows.append(
      rowXml(
        r++,
        listOf(
          textCell("A", r - 1, if (context != null) "TOTAL ($colCategory)" else "TOTAL KATEGORI", style = 5),
          numberCell("B", r - 1, sumAllocated, style = 6),
          numberCell("C", r - 1, totalSpent, style = 6),
          numberCell("D", r - 1, sumAllocated - totalSpent, style = 6),
          decimalCell("E", r - 1, totalRatio, style = 7),
        ),
        heightPt = 24,
      ),
    )

    r++ // Blank row

    // Section 3: Payment Method Breakdown
    rows.append(
      rowXml(
        r++,
        listOf(
          textCell("A", r - 1, colMethod, style = 2),
          textCell("B", r - 1, txLabel, style = 2),
          textCell("C", r - 1, colAmount, style = 2),
        ),
        heightPt = 25,
      ),
    )
    for ((method, count, sum) in byMethod) {
      rows.append(
        rowXml(
          r++,
          listOf(
            textCell("A", r - 1, method.label(context), style = 3),
            numberCell("B", r - 1, count.toLong(), style = 8),
            numberCell("C", r - 1, sum, style = 4),
          ),
          heightPt = 22,
        ),
      )
    }

    val colsXml = """
      <cols>
        <col min="1" max="1" width="28" customWidth="1"/>
        <col min="2" max="4" width="20" customWidth="1"/>
        <col min="5" max="5" width="14" customWidth="1"/>
      </cols>
    """.trimIndent()

    return wrapWorksheetXml(colsXml, rows.toString())
  }

  private fun buildTransactionsSheetXml(
    expenses: List<Expense>,
    receiptIndexByExpenseId: Map<String, Int>,
    hasDrawing: Boolean,
    context: Context?,
    currency: AppCurrency,
  ): String {
    val colDate = context?.getString(R.string.report_col_date) ?: "Tanggal"
    val colTitle = context?.getString(R.string.report_col_title) ?: "Judul Transaksi"
    val colCategory = context?.getString(R.string.report_col_category) ?: "Kategori"
    val colMethod = context?.getString(R.string.report_col_method) ?: "Metode Pembayaran"
    val colAmount = context?.getString(R.string.report_col_amount, currency.symbol) ?: "Nominal (Rp)"
    val colNote = context?.getString(R.string.report_col_note) ?: "Catatan"
    val colReceipt = context?.getString(R.string.report_col_receipt) ?: "Bukti Struk"
    val colReceiptPhoto = context?.getString(R.string.report_col_receipt_photo) ?: "Foto Struk"
    val totalLabel = context?.getString(R.string.hero_total_spent_month)?.uppercase(Locale.ROOT) ?: "TOTAL PENGELUARAN"
    val txCountText = context?.getString(R.string.export_tx_count, expenses.size) ?: "${expenses.size} transaksi"
    val receiptCountText = context?.getString(R.string.report_receipt_attached_count, receiptIndexByExpenseId.size)
      ?: "${receiptIndexByExpenseId.size} foto struk terlampir"

    val rows = StringBuilder()

    val headerCells = buildList {
      add(textCell("A", 1, colDate, style = 2))
      add(textCell("B", 1, colTitle, style = 2))
      add(textCell("C", 1, colCategory, style = 2))
      add(textCell("D", 1, colMethod, style = 2))
      add(textCell("E", 1, colAmount, style = 2))
      add(textCell("F", 1, colNote, style = 2))
      add(textCell("G", 1, colReceipt, style = 2))
      if (hasDrawing) {
        add(textCell("H", 1, colReceiptPhoto, style = 2))
      }
    }
    rows.append(rowXml(1, headerCells, heightPt = 26))

    expenses.forEachIndexed { index, item ->
      val rowNum = index + 2
      val dateStr = LocalDate.ofEpochDay(item.dateEpochDay).toString()
      val receiptIdx = receiptIndexByExpenseId[item.id]
      val receiptStatus = if (receiptIdx != null) {
        context?.getString(R.string.report_receipt_yes, receiptIdx) ?: "Terlampir (#$receiptIdx)"
      } else {
        context?.getString(R.string.report_receipt_no) ?: "-"
      }
      val rowCells = buildList {
        add(textCell("A", rowNum, dateStr, style = 10))
        add(textCell("B", rowNum, item.title, style = 11))
        add(textCell("C", rowNum, item.categoryEnum.label(context), style = 10))
        add(textCell("D", rowNum, item.paymentMethodEnum.label(context), style = 10))
        add(numberCell("E", rowNum, item.amount, style = 12))
        add(textCell("F", rowNum, item.note, style = 11))
        add(textCell("G", rowNum, receiptStatus, style = if (receiptIdx != null) 9 else 10))
        if (hasDrawing) {
          add(textCell("H", rowNum, "", style = 10))
        }
      }
      val rowHeight = if (hasDrawing && receiptIdx != null) 92 else 24
      rows.append(rowXml(rowNum, rowCells, heightPt = rowHeight))
    }

    if (expenses.isNotEmpty()) {
      val lastDataRow = expenses.size + 1
      val totalRow = lastDataRow + 1
      val totalSpent = expenses.sumOf { it.amount }
      val totalCells = buildList {
        add(textCell("A", totalRow, totalLabel, style = 5))
        add(textCell("B", totalRow, "", style = 5))
        add(textCell("C", totalRow, "", style = 5))
        add(textCell("D", totalRow, "", style = 5))
        add(formulaNumberCell("E", totalRow, "SUM(E2:E$lastDataRow)", totalSpent, style = 6))
        add(textCell("F", totalRow, txCountText, style = 5))
        add(textCell("G", totalRow, receiptCountText, style = 5))
        if (hasDrawing) {
          add(textCell("H", totalRow, "", style = 5))
        }
      }
      rows.append(rowXml(totalRow, totalCells, heightPt = 26))
    }

    val colsXml = """
      <cols>
        <col min="1" max="1" width="14" customWidth="1"/>
        <col min="2" max="2" width="30" customWidth="1"/>
        <col min="3" max="4" width="20" customWidth="1"/>
        <col min="5" max="5" width="18" customWidth="1"/>
        <col min="6" max="6" width="30" customWidth="1"/>
        <col min="7" max="7" width="18" customWidth="1"/>
        <col min="8" max="8" width="22" customWidth="1"/>
      </cols>
    """.trimIndent()

    val drawingTag = if (hasDrawing) "\n  <drawing r:id=\"rId1\"/>" else ""
    return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  $colsXml
  <sheetData>
    $rows
  </sheetData>$drawingTag
</worksheet>"""
  }

  private fun buildReceiptsSheetXml(
    expensesWithReceipts: List<Expense>,
    receiptIndexByExpenseId: Map<String, Int>,
    hasDrawing: Boolean,
    context: Context?,
    currency: AppCurrency,
  ): String {
    val sectionTitle = context?.getString(R.string.report_receipt_section_title)
      ?: "Lampiran Foto Struk & Invoice"
    val colDate = context?.getString(R.string.report_col_date) ?: "Tanggal"
    val colTitle = context?.getString(R.string.report_col_title) ?: "Judul Transaksi"
    val colCategory = context?.getString(R.string.report_col_category) ?: "Kategori"
    val colMethod = context?.getString(R.string.report_col_method) ?: "Metode Pembayaran"
    val colAmount = context?.getString(R.string.report_col_amount, currency.symbol) ?: "Nominal (Rp)"
    val colNote = context?.getString(R.string.report_col_note) ?: "Catatan"
    val colReceipt = context?.getString(R.string.report_col_receipt) ?: "Bukti Struk"
    val colReceiptPhoto = context?.getString(R.string.report_col_receipt_photo) ?: "Foto Struk"

    val rows = StringBuilder()
    rows.append(rowXml(1, listOf(textCell("A", 1, sectionTitle, style = 1)), heightPt = 30))
    rows.append(
      rowXml(
        2,
        listOf(
          textCell("A", 2, "#", style = 2),
          textCell("B", 2, colDate, style = 2),
          textCell("C", 2, colTitle, style = 2),
          textCell("D", 2, colCategory, style = 2),
          textCell("E", 2, colMethod, style = 2),
          textCell("F", 2, colAmount, style = 2),
          textCell("G", 2, colNote, style = 2),
          textCell("H", 2, colReceipt, style = 2),
          textCell("I", 2, colReceiptPhoto, style = 2),
        ),
        heightPt = 26,
      ),
    )

    // Each receipt is a single clean, bordered table row with 138pt height;
    // the photo is anchored strictly inside cell I{rowNum} with aspect-ratio-preserved margins.
    expensesWithReceipts.forEachIndexed { idx, item ->
      val receiptNum = receiptIndexByExpenseId[item.id] ?: (idx + 1)
      val rowNum = 3 + idx
      val dateStr = LocalDate.ofEpochDay(item.dateEpochDay).toString()
      val label = context?.getString(R.string.report_receipt_yes, receiptNum) ?: "Terlampir (#$receiptNum)"
      rows.append(
        rowXml(
          rowNum,
          listOf(
            textCell("A", rowNum, "#$receiptNum", style = 9),
            textCell("B", rowNum, dateStr, style = 10),
            textCell("C", rowNum, item.title, style = 11),
            textCell("D", rowNum, item.categoryEnum.label(context), style = 10),
            textCell("E", rowNum, item.paymentMethodEnum.label(context), style = 10),
            numberCell("F", rowNum, item.amount, style = 12),
            textCell("G", rowNum, item.note.ifBlank { "-" }, style = 11),
            textCell("H", rowNum, label, style = 9),
            textCell("I", rowNum, "", style = 10),
          ),
          heightPt = 138,
        ),
      )
    }

    if (expensesWithReceipts.isNotEmpty()) {
      val lastReceiptRow = 2 + expensesWithReceipts.size
      val totalRow = lastReceiptRow + 1
      val totalReceiptAmount = expensesWithReceipts.sumOf { it.amount }
      val receiptCountText = context?.getString(R.string.report_receipt_attached_count, expensesWithReceipts.size)
        ?: "${expensesWithReceipts.size} foto struk terlampir"
      rows.append(
        rowXml(
          totalRow,
          listOf(
            textCell("A", totalRow, "TOTAL", style = 5),
            textCell("B", totalRow, "", style = 5),
            textCell("C", totalRow, receiptCountText, style = 5),
            textCell("D", totalRow, "", style = 5),
            textCell("E", totalRow, "", style = 5),
            formulaNumberCell("F", totalRow, "SUM(F3:F$lastReceiptRow)", totalReceiptAmount, style = 6),
            textCell("G", totalRow, "", style = 5),
            textCell("H", totalRow, "", style = 5),
            textCell("I", totalRow, "", style = 5),
          ),
          heightPt = 26,
        ),
      )
    }

    val colsXml = """
      <cols>
        <col min="1" max="1" width="9" customWidth="1"/>
        <col min="2" max="2" width="14" customWidth="1"/>
        <col min="3" max="3" width="28" customWidth="1"/>
        <col min="4" max="5" width="18" customWidth="1"/>
        <col min="6" max="6" width="18" customWidth="1"/>
        <col min="7" max="7" width="26" customWidth="1"/>
        <col min="8" max="8" width="18" customWidth="1"/>
        <col min="9" max="9" width="28" customWidth="1"/>
      </cols>
    """.trimIndent()

    val drawingTag = if (hasDrawing) "\n  <drawing r:id=\"rId1\"/>" else ""
    return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  $colsXml
  <sheetData>
    $rows
  </sheetData>$drawingTag
</worksheet>"""
  }

  /**
   * Builds `xl/drawings/drawing1.xml` for Sheet 3 (`Lampiran Struk`), placing each receipt photo
   * centered inside Column I (`col0 = 8`) on its single row (`row0 = attachment.receiptSheetRowIndex0`)
   * with aspect ratio preserved.
   */
  private fun buildReceiptsDrawingXml(attachments: List<DecodedReceiptAttachment>): String {
    // Column I width = 28 chars (~196px = 1,866,900 EMU), Row height = 138pt (184px = 1,752,600 EMU)
    val cellWidthEmu = 1_866_900L
    val cellHeightEmu = 1_752_600L
    val padEmu = 66_675L // 7px inner padding from cell borders
    return buildCenteredSingleCellDrawingXml(
      attachments = attachments,
      targetCol0 = 8,
      cellWidthEmu = cellWidthEmu,
      cellHeightEmu = cellHeightEmu,
      padEmu = padEmu,
      rowIndexSelector = { it.receiptSheetRowIndex0 },
    )
  }

  /**
   * Builds `xl/drawings/drawing2.xml` for Sheet 2 (`Rincian Transaksi`), placing a neat thumbnail
   * inside Column H (`col0 = 7`) on each transaction row that has a receipt photo.
   */
  private fun buildTransactionsDrawingXml(attachments: List<DecodedReceiptAttachment>): String {
    // Column H width = 22 chars (~154px = 1,466,850 EMU), Row height = 92pt (~123px = 1,168,400 EMU)
    val cellWidthEmu = 1_466_850L
    val cellHeightEmu = 1_168_400L
    val padEmu = 47_625L // 5px inner padding from cell borders
    return buildCenteredSingleCellDrawingXml(
      attachments = attachments,
      targetCol0 = 7,
      cellWidthEmu = cellWidthEmu,
      cellHeightEmu = cellHeightEmu,
      padEmu = padEmu,
      rowIndexSelector = { it.txRowIndex0 },
    )
  }

  private fun buildCenteredSingleCellDrawingXml(
    attachments: List<DecodedReceiptAttachment>,
    targetCol0: Int,
    cellWidthEmu: Long,
    cellHeightEmu: Long,
    padEmu: Long,
    rowIndexSelector: (DecodedReceiptAttachment) -> Int,
  ): String {
    val anchors = StringBuilder()
    val maxBoxW = (cellWidthEmu - padEmu * 2L).coerceAtLeast(200_000L)
    val maxBoxH = (cellHeightEmu - padEmu * 2L).coerceAtLeast(200_000L)

    attachments.forEach { attachment ->
      val row0 = rowIndexSelector(attachment)
      val safeW = attachment.widthPx.coerceAtLeast(1).toDouble()
      val safeH = attachment.heightPx.coerceAtLeast(1).toDouble()
      val scale = min(maxBoxW.toDouble() / safeW, maxBoxH.toDouble() / safeH)
      val drawW = (safeW * scale).toLong().coerceIn(150_000L, maxBoxW)
      val drawH = (safeH * scale).toLong().coerceIn(150_000L, maxBoxH)
      val fromColOff = ((cellWidthEmu - drawW) / 2L).coerceAtLeast(padEmu)
      val fromRowOff = ((cellHeightEmu - drawH) / 2L).coerceAtLeast(padEmu)
      val toColOff = (fromColOff + drawW).coerceAtMost(cellWidthEmu - padEmu / 2L)
      val toRowOff = (fromRowOff + drawH).coerceAtMost(cellHeightEmu - padEmu / 2L)

      anchors.append(
        """
  <xdr:twoCellAnchor editAs="oneCell">
    <xdr:from>
      <xdr:col>$targetCol0</xdr:col>
      <xdr:colOff>$fromColOff</xdr:colOff>
      <xdr:row>$row0</xdr:row>
      <xdr:rowOff>$fromRowOff</xdr:rowOff>
    </xdr:from>
    <xdr:to>
      <xdr:col>$targetCol0</xdr:col>
      <xdr:colOff>$toColOff</xdr:colOff>
      <xdr:row>$row0</xdr:row>
      <xdr:rowOff>$toRowOff</xdr:rowOff>
    </xdr:to>
    <xdr:pic>
      <xdr:nvPicPr>
        <xdr:cNvPr id="${attachment.index + 1}" name="Receipt_${attachment.index}" descr="${escapeXml(attachment.expense.title)}"/>
        <xdr:cNvPicPr>
          <a:picLocks noChangeAspect="1"/>
        </xdr:cNvPicPr>
      </xdr:nvPicPr>
      <xdr:blipFill>
        <a:blip r:embed="rId${attachment.index}"/>
        <a:stretch>
          <a:fillRect/>
        </a:stretch>
      </xdr:blipFill>
      <xdr:spPr>
        <a:xfrm>
          <a:off x="0" y="0"/>
          <a:ext cx="$drawW" cy="$drawH"/>
        </a:xfrm>
        <a:prstGeom prst="rect">
          <a:avLst/>
        </a:prstGeom>
      </xdr:spPr>
    </xdr:pic>
    <xdr:clientData/>
  </xdr:twoCellAnchor>
        """.trimIndent()
      ).append('\n')
    }

    return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<xdr:wsDr xmlns:xdr="http://schemas.openxmlformats.org/drawingml/2006/spreadsheetDrawing" xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
$anchors</xdr:wsDr>"""
  }

  private fun buildDrawingRelsXml(attachments: List<DecodedReceiptAttachment>): String {
    val rels = attachments.joinToString("\n") { attachment ->
      "  <Relationship Id=\"rId${attachment.index}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/image\" Target=\"../media/receipt_${attachment.index}.jpg\"/>"
    }
    return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
$rels
</Relationships>"""
  }

  private fun sheetDrawingRelsXml(drawingFileName: String): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/drawing" Target="../drawings/$drawingFileName"/>
</Relationships>"""

  private fun wrapWorksheetXml(colsXml: String, rowsXml: String): String =
    """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  $colsXml
  <sheetData>
    $rowsXml
  </sheetData>
</worksheet>"""

  private fun rowXml(rowNumber: Int, cells: List<String>, heightPt: Int? = null): String {
    val htAttr = if (heightPt != null) " ht=\"$heightPt\" customHeight=\"1\"" else ""
    return "<row r=\"$rowNumber\"$htAttr>${cells.joinToString("")}</row>\n"
  }

  private fun textCell(col: String, row: Int, value: String, style: Int): String =
    "<c r=\"$col$row\" t=\"inlineStr\" s=\"$style\"><is><t>${escapeXml(value)}</t></is></c>"

  private fun numberCell(col: String, row: Int, value: Long, style: Int): String =
    "<c r=\"$col$row\" s=\"$style\"><v>$value</v></c>"

  private fun decimalCell(col: String, row: Int, value: Double, style: Int): String {
    val safe = if (value.isFinite()) String.format(Locale.US, "%.4f", value) else "0"
    return "<c r=\"$col$row\" s=\"$style\"><v>$safe</v></c>"
  }

  private fun formulaNumberCell(
    col: String,
    row: Int,
    formula: String,
    cachedValue: Long,
    style: Int,
  ): String = "<c r=\"$col$row\" s=\"$style\"><f>$formula</f><v>$cachedValue</v></c>"

  private fun escapeXml(input: String): String {
    val sb = StringBuilder(input.length)
    for (ch in input) {
      val code = ch.code
      val validXmlChar = code == 0x9 || code == 0xA || code == 0xD || (code >= 0x20 && code <= 0xD7FF) || (code >= 0xE000 && code <= 0xFFFD)
      if (!validXmlChar) continue
      when (ch) {
        '&' -> sb.append("&amp;")
        '<' -> sb.append("&lt;")
        '>' -> sb.append("&gt;")
        '"' -> sb.append("&quot;")
        '\'' -> sb.append("&apos;")
        else -> sb.append(ch)
      }
    }
    return sb.toString()
  }

  private fun contentTypesXml(
    hasReceiptSheet: Boolean,
    hasEmbeddedImages: Boolean,
  ): String {
    val jpgDefault = if (hasEmbeddedImages) {
      "\n  <Default Extension=\"jpg\" ContentType=\"image/jpeg\"/>"
    } else {
      ""
    }
    val sheet3Override = if (hasReceiptSheet) {
      "\n  <Override PartName=\"/xl/worksheets/sheet3.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
    } else {
      ""
    }
    val drawingOverrides = if (hasEmbeddedImages) {
      "\n  <Override PartName=\"/xl/drawings/drawing1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.drawing+xml\"/>" +
        "\n  <Override PartName=\"/xl/drawings/drawing2.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.drawing+xml\"/>"
    } else {
      ""
    }
    return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>$jpgDefault
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>$sheet3Override$drawingOverrides
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""
  }

  private fun rootRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

  private fun workbookRelsXml(hasReceiptSheet: Boolean): String {
    val sheet3Rel = if (hasReceiptSheet) {
      "\n  <Relationship Id=\"rId4\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet3.xml\"/>"
    } else {
      ""
    }
    return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>$sheet3Rel
</Relationships>"""
  }

  private fun workbookXml(
    summarySheetName: String,
    txSheetName: String,
    receiptSheetName: String?,
  ): String {
    val sheet3Node = if (receiptSheetName != null) {
      "\n    <sheet name=\"${escapeXml(receiptSheetName)}\" sheetId=\"3\" r:id=\"rId4\"/>"
    } else {
      ""
    }
    return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="${escapeXml(summarySheetName)}" sheetId="1" r:id="rId1"/>
    <sheet name="${escapeXml(txSheetName)}" sheetId="2" r:id="rId2"/>$sheet3Node
  </sheets>
</workbook>"""
  }

  private fun stylesXml(currency: AppCurrency): String {
    val prefix = escapeXml(currency.prefixWithSpace)
    val formatCode = "&quot;$prefix&quot;#,##0;-&quot;$prefix&quot;#,##0;&quot;$prefix&quot;0"
    return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <numFmts count="1">
    <numFmt numFmtId="164" formatCode="$formatCode"/>
  </numFmts>
  <fonts count="5">
    <font><sz val="11"/><color rgb="FF111827"/><name val="Calibri"/></font>
    <font><b/><sz val="14"/><color rgb="FF3730A3"/><name val="Calibri"/></font>
    <font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>
    <font><b/><sz val="11"/><color rgb="FF1E1B4B"/><name val="Calibri"/></font>
    <font><b/><sz val="10"/><color rgb="FF065F46"/><name val="Calibri"/></font>
  </fonts>
  <fills count="5">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FF4338CA"/></patternFill></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFE0E7FF"/></patternFill></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFD1FAE5"/></patternFill></fill>
  </fills>
  <borders count="2">
    <border><left/><right/><top/><bottom/><diagonal/></border>
    <border>
      <left style="thin"><color rgb="FFCBD5E1"/></left>
      <right style="thin"><color rgb="FFCBD5E1"/></right>
      <top style="thin"><color rgb="FFCBD5E1"/></top>
      <bottom style="thin"><color rgb="FFCBD5E1"/></bottom>
      <diagonal/>
    </border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="13">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1" applyAlignment="1"><alignment vertical="center"/></xf>
    <xf numFmtId="0" fontId="2" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1"><alignment vertical="center"/></xf>
    <xf numFmtId="164" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1" applyAlignment="1"><alignment vertical="center"/></xf>
    <xf numFmtId="0" fontId="3" fillId="3" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment vertical="center"/></xf>
    <xf numFmtId="164" fontId="3" fillId="3" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment vertical="center"/></xf>
    <xf numFmtId="9" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1" applyAlignment="1"><alignment vertical="center"/></xf>
    <xf numFmtId="3" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1" applyAlignment="1"><alignment vertical="center"/></xf>
    <xf numFmtId="0" fontId="4" fillId="4" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1"><alignment vertical="center" wrapText="1"/></xf>
    <xf numFmtId="164" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>
  </cellXfs>
</styleSheet>"""
  }
}
