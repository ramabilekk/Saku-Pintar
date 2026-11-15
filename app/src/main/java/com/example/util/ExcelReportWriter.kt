package com.example.util

import android.content.Context
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
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Generates a valid 2-sheet OOXML (.xlsx) workbook in pure Kotlin with zero external dependencies.
 * Sheet 1: Summary (Financial summary, Category breakdown, Payment method breakdown)
 * Sheet 2: Transactions (Full chronological ledger with live Excel =SUM formula)
 */
internal object ExcelReportWriter {

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

    val baos = ByteArrayOutputStream()
    ZipOutputStream(baos).use { zip ->
      writeZipEntry(zip, "[Content_Types].xml", contentTypesXml())
      writeZipEntry(zip, "_rels/.rels", rootRelsXml())
      writeZipEntry(zip, "xl/_rels/workbook.xml.rels", workbookRelsXml())
      writeZipEntry(zip, "xl/workbook.xml", workbookXml(summarySheetName, txSheetName))
      writeZipEntry(zip, "xl/styles.xml", stylesXml(currency))
      writeZipEntry(
        zip,
        "xl/worksheets/sheet1.xml",
        buildSummarySheetXml(monthKey, expenses, budget, dailyInsight, context, currency),
      )
      writeZipEntry(
        zip,
        "xl/worksheets/sheet2.xml",
        buildTransactionsSheetXml(expenses, context, currency),
      )
    }
    return baos.toByteArray()
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

  private fun buildSummarySheetXml(
    monthKey: String,
    expenses: List<Expense>,
    budget: MonthlyBudget?,
    dailyInsight: DailyBudgetInsight,
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
    val incomeLabel = context?.getString(R.string.budget_income_label, currency.symbol) ?: "Pendapatan Bulanan"
    val budgetLabel = context?.getString(R.string.budget_shopping_allocation) ?: "Total Anggaran Belanja"
    val savingsLabel = context?.getString(R.string.hero_savings_target) ?: "Target Tabungan"
    val remainingLabel = context?.getString(R.string.hero_remaining_budget) ?: "Sisa Anggaran"
    val colCategory = context?.getString(R.string.report_col_category) ?: "Kategori"
    val colMethod = context?.getString(R.string.report_col_method) ?: "Metode Pembayaran"
    val colAmount = context?.getString(R.string.report_col_amount, currency.symbol) ?: "Nominal (Rp)"

    val rows = StringBuilder()
    var r = 1

    rows.append(rowXml(r++, listOf(textCell("A", r - 1, bannerTitle, style = 1))))
    rows.append(rowXml(r++, listOf(textCell("A", r - 1, monthTitle, style = 0))))
    r++ // Blank row

    rows.append(
      rowXml(
        r++,
        listOf(
          textCell("A", r - 1, sheetSummaryTitle, style = 2),
          textCell("B", r - 1, colAmount, style = 2),
        ),
      ),
    )
    rows.append(
      rowXml(
        r++,
        listOf(
          textCell("A", r - 1, totalSpentLabel, style = 3),
          numberCell("B", r - 1, totalSpent, style = 4),
        ),
      ),
    )
    rows.append(
      rowXml(
        r++,
        listOf(
          textCell("A", r - 1, txLabel, style = 3),
          numberCell("B", r - 1, expenses.size.toLong(), style = 8),
        ),
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
        ),
      )
      rows.append(
        rowXml(
          r++,
          listOf(
            textCell("A", r - 1, budgetLabel, style = 3),
            numberCell("B", r - 1, dailyInsight.totalBudget, style = 4),
          ),
        ),
      )
      rows.append(
        rowXml(
          r++,
          listOf(
            textCell("A", r - 1, savingsLabel, style = 3),
            numberCell("B", r - 1, budget.savingsTarget, style = 4),
          ),
        ),
      )
      rows.append(
        rowXml(
          r++,
          listOf(
            textCell("A", r - 1, remainingLabel, style = 5),
            numberCell("B", r - 1, dailyInsight.remainingBudget, style = 6),
          ),
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
    context: Context?,
    currency: AppCurrency,
  ): String {
    val colDate = context?.getString(R.string.report_col_date) ?: "Tanggal"
    val colTitle = context?.getString(R.string.report_col_title) ?: "Judul Transaksi"
    val colCategory = context?.getString(R.string.report_col_category) ?: "Kategori"
    val colMethod = context?.getString(R.string.report_col_method) ?: "Metode Pembayaran"
    val colAmount = context?.getString(R.string.report_col_amount, currency.symbol) ?: "Nominal (Rp)"
    val colNote = context?.getString(R.string.report_col_note) ?: "Catatan"
    val totalLabel = context?.getString(R.string.hero_total_spent_month)?.uppercase(Locale.ROOT) ?: "TOTAL PENGELUARAN"
    val txCountText = context?.getString(R.string.export_tx_count, expenses.size) ?: "${expenses.size} transaksi"

    val sorted = expenses.sortedBy { it.dateEpochDay }
    val rows = StringBuilder()

    rows.append(
      rowXml(
        1,
        listOf(
          textCell("A", 1, colDate, style = 2),
          textCell("B", 1, colTitle, style = 2),
          textCell("C", 1, colCategory, style = 2),
          textCell("D", 1, colMethod, style = 2),
          textCell("E", 1, colAmount, style = 2),
          textCell("F", 1, colNote, style = 2),
        ),
      ),
    )

    sorted.forEachIndexed { index, item ->
      val rowNum = index + 2
      val dateStr = LocalDate.ofEpochDay(item.dateEpochDay).toString()
      rows.append(
        rowXml(
          rowNum,
          listOf(
            textCell("A", rowNum, dateStr, style = 3),
            textCell("B", rowNum, item.title, style = 3),
            textCell("C", rowNum, item.categoryEnum.label(context), style = 3),
            textCell("D", rowNum, item.paymentMethodEnum.label(context), style = 3),
            numberCell("E", rowNum, item.amount, style = 4),
            textCell("F", rowNum, item.note, style = 3),
          ),
        ),
      )
    }

    if (sorted.isNotEmpty()) {
      val lastDataRow = sorted.size + 1
      val totalRow = lastDataRow + 1
      val totalSpent = sorted.sumOf { it.amount }
      rows.append(
        rowXml(
          totalRow,
          listOf(
            textCell("A", totalRow, totalLabel, style = 5),
            textCell("B", totalRow, "", style = 5),
            textCell("C", totalRow, "", style = 5),
            textCell("D", totalRow, "", style = 5),
            formulaNumberCell("E", totalRow, "SUM(E2:E$lastDataRow)", totalSpent, style = 6),
            textCell("F", totalRow, txCountText, style = 5),
          ),
        ),
      )
    }

    val colsXml = """
      <cols>
        <col min="1" max="1" width="14" customWidth="1"/>
        <col min="2" max="2" width="30" customWidth="1"/>
        <col min="3" max="4" width="20" customWidth="1"/>
        <col min="5" max="5" width="18" customWidth="1"/>
        <col min="6" max="6" width="32" customWidth="1"/>
      </cols>
    """.trimIndent()

    return wrapWorksheetXml(colsXml, rows.toString())
  }

  private fun wrapWorksheetXml(colsXml: String, rowsXml: String): String =
    """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  $colsXml
  <sheetData>
    $rowsXml
  </sheetData>
</worksheet>"""

  private fun rowXml(rowNumber: Int, cells: List<String>): String =
    "<row r=\"$rowNumber\">${cells.joinToString("")}</row>\n"

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

  private fun contentTypesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""

  private fun rootRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

  private fun workbookRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

  private fun workbookXml(summarySheetName: String, txSheetName: String): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="${escapeXml(summarySheetName)}" sheetId="1" r:id="rId1"/>
    <sheet name="${escapeXml(txSheetName)}" sheetId="2" r:id="rId2"/>
  </sheets>
</workbook>"""

  private fun stylesXml(currency: AppCurrency): String {
    val prefix = escapeXml(currency.prefixWithSpace)
    val formatCode = "&quot;$prefix&quot;#,##0;-&quot;$prefix&quot;#,##0;&quot;$prefix&quot;0"
    return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <numFmts count="1">
    <numFmt numFmtId="164" formatCode="$formatCode"/>
  </numFmts>
  <fonts count="4">
    <font><sz val="11"/><color rgb="FF111827"/><name val="Calibri"/></font>
    <font><b/><sz val="14"/><color rgb="FF3730A3"/><name val="Calibri"/></font>
    <font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>
    <font><b/><sz val="11"/><color rgb="FF1E1B4B"/><name val="Calibri"/></font>
  </fonts>
  <fills count="4">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FF4338CA"/></patternFill></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFE0E7FF"/></patternFill></fill>
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
  <cellXfs count="9">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>
    <xf numFmtId="0" fontId="2" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment vertical="center"/></xf>
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1"/>
    <xf numFmtId="164" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1"/>
    <xf numFmtId="0" fontId="3" fillId="3" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"/>
    <xf numFmtId="164" fontId="3" fillId="3" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1"/>
    <xf numFmtId="9" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1"/>
    <xf numFmtId="3" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1"/>
  </cellXfs>
</styleSheet>"""
  }
}
