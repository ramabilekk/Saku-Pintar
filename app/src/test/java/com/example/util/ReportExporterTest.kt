package com.example.util

import com.example.data.budget.BudgetPaceStatus
import com.example.data.budget.DailyBudgetInsight
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.MonthlyBudget
import com.example.data.model.PaymentMethod
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.util.zip.ZipInputStream

class ReportExporterTest {

  @Test
  fun buildMonthlyCsv_includesBomEscapesQuotesAndTotals() {
    val day = LocalDate.of(2026, 11, 10).toEpochDay()
    val expenses = listOf(
      Expense(
        id = "exp-1",
        userId = "user-1",
        title = "Kopi \"Spesial\", Susu",
        amount = 28_000L,
        category = ExpenseCategory.FOOD.name,
        paymentMethod = PaymentMethod.QRIS.name,
        dateEpochDay = day,
        monthKey = "2026-11",
        note = "Baris1\nBaris2",
      ),
    )
    val budget = MonthlyBudget(
      userId = "user-1",
      monthKey = "2026-11",
      monthlyIncome = 5_000_000L,
      savingsTarget = 1_000_000L,
      categoryBudgets = mapOf(ExpenseCategory.FOOD.name to 1_500_000L),
    )

    val csv = ReportExporter.buildMonthlyCsv("2026-11", expenses, budget)

    assertTrue(csv.startsWith("\uFEFFTanggal,Judul,Kategori,Metode Pembayaran,Nominal (Rp),Catatan"))
    assertTrue(csv.contains("\"Kopi \"\"Spesial\"\", Susu\""))
    assertTrue(csv.contains("\"Baris1 Baris2\""))
    assertTrue(csv.contains("Total Pengeluaran,28000"))
    assertTrue(csv.contains("Pendapatan Bulanan,5000000"))
  }

  @Test
  fun buildMonthlyShareText_includesCategoryBreakdownAndRecentTransactions() {
    val day = LocalDate.of(2026, 11, 10).toEpochDay()
    val expenses = listOf(
      Expense(
        id = "exp-1",
        userId = "user-1",
        title = "Makan Siang",
        amount = 35_000L,
        category = ExpenseCategory.FOOD.name,
        paymentMethod = PaymentMethod.CASH.name,
        dateEpochDay = day,
        monthKey = "2026-11",
      ),
    )
    val insight = DailyBudgetInsight(
      totalBudget = 2_000_000L,
      totalSpent = 35_000L,
      remainingBudget = 1_965_000L,
      daysInMonth = 30,
      daysRemaining = 20,
      dailySafeLimit = 98_250L,
      todaySpent = 35_000L,
      todayRemaining = 63_250L,
      paceStatus = BudgetPaceStatus.SAFE,
    )

    val summary = ReportExporter.buildMonthlyShareText("2026-11", expenses, null, insight)

    assertTrue(summary.contains("Rekap Keuangan SakuPintar"))
    assertTrue(summary.contains("Makan Siang"))
    assertTrue(summary.contains("Rincian per Kategori"))
  }

  @Test
  fun buildMonthlyExcelBytes_createsValidTwoSheetOoxmlWorkbook() {
    val day = LocalDate.of(2026, 11, 12).toEpochDay()
    val expenses = listOf(
      Expense(
        id = "exp-1",
        userId = "user-1",
        title = "Makan & Kopi <Spesial>",
        amount = 45_000L,
        category = ExpenseCategory.FOOD.name,
        paymentMethod = PaymentMethod.QRIS.name,
        dateEpochDay = day,
        monthKey = "2026-11",
        note = "Catatan \"uji\"",
      ),
      Expense(
        id = "exp-2",
        userId = "user-1",
        title = "Ojek Online",
        amount = 20_000L,
        category = ExpenseCategory.TRANSPORT.name,
        paymentMethod = PaymentMethod.TRANSFER.name,
        dateEpochDay = day,
        monthKey = "2026-11",
      ),
    )
    val budget = MonthlyBudget(
      userId = "user-1",
      monthKey = "2026-11",
      monthlyIncome = 6_000_000L,
      savingsTarget = 1_200_000L,
      categoryBudgets = mapOf(
        ExpenseCategory.FOOD.name to 1_800_000L,
        ExpenseCategory.TRANSPORT.name to 700_000L,
      ),
    )
    val insight = DailyBudgetInsight(
      totalBudget = 2_500_000L,
      totalSpent = 65_000L,
      remainingBudget = 2_435_000L,
      daysInMonth = 30,
      daysRemaining = 18,
      dailySafeLimit = 135_277L,
      todaySpent = 65_000L,
      todayRemaining = 70_277L,
      paceStatus = BudgetPaceStatus.SAFE,
    )

    val bytes = ReportExporter.buildMonthlyExcelBytes("2026-11", expenses, budget, insight)
    val entries = mutableMapOf<String, String>()
    ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
      var entry = zip.nextEntry
      while (entry != null) {
        entries[entry.name] = String(zip.readBytes(), StandardCharsets.UTF_8)
        entry = zip.nextEntry
      }
    }

    assertTrue(entries.containsKey("[Content_Types].xml"))
    assertTrue(entries.containsKey("_rels/.rels"))
    assertTrue(entries.containsKey("xl/_rels/workbook.xml.rels"))
    assertTrue(entries.containsKey("xl/workbook.xml"))
    assertTrue(entries.containsKey("xl/styles.xml"))
    assertTrue(entries.containsKey("xl/worksheets/sheet1.xml"))
    assertTrue(entries.containsKey("xl/worksheets/sheet2.xml"))

    val workbook = entries.getValue("xl/workbook.xml")
    assertTrue(workbook.contains("Ringkasan Bulanan"))
    assertTrue(workbook.contains("Rincian Transaksi"))

    val sheet1 = entries.getValue("xl/worksheets/sheet1.xml")
    assertTrue(sheet1.contains("Rekap Keuangan Bulanan SakuPintar"))
    assertTrue(sheet1.contains("TOTAL KATEGORI"))
    assertTrue(sheet1.contains("<v>65000</v>"))

    val sheet2 = entries.getValue("xl/worksheets/sheet2.xml")
    assertTrue(sheet2.contains("Makan &amp; Kopi &lt;Spesial&gt;"))
    assertTrue(sheet2.contains("<f>SUM(E2:E3)</f><v>65000</v>"))
  }

  @Test
  fun defaultFileName_matchesExpectedExtensionsAndMimeTypes() {
    assertTrue(ReportExporter.defaultFileName("2026-11", ReportFormat.EXCEL) == "SakuPintar_2026-11.xlsx")
    assertTrue(ReportExporter.defaultFileName("2026-11", ReportFormat.PDF) == "SakuPintar_2026-11.pdf")
    assertTrue(ReportExporter.defaultFileName("2026-11", ReportFormat.CSV) == "SakuPintar_2026-11.csv")
    assertTrue(ReportFormat.EXCEL.mimeType.contains("spreadsheetml.sheet"))
    assertTrue(ReportFormat.PDF.mimeType == "application/pdf")
  }
}
