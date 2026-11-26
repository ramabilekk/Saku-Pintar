package com.example.util

import com.example.data.budget.BudgetAutoComposer
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.MonthlyBudget
import com.example.data.model.PaymentMethod
import com.example.i18n.AppCurrency
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportExporterTest {

  private fun sampleExpenses(): List<Expense> {
    val baseDay = LocalDate.of(2026, 11, 1).toEpochDay()
    return (1..7).map { idx ->
      Expense(
        id = "exp-$idx",
        title = if (idx == 1) "Kopi, Roti \"Pagi\"" else "Pengeluaran $idx",
        amount = idx * 15_000L,
        category = if (idx % 2 == 0) ExpenseCategory.TRANSPORT.name else ExpenseCategory.FOOD.name,
        paymentMethod = PaymentMethod.QRIS.name,
        dateEpochDay = baseDay + idx - 1,
        monthKey = "2026-11",
        note = if (idx == 1) "Catatan baris 1\nbaris 2" else "",
      )
    }
  }

  @Test
  fun defaultFileName_usesExpectedExtensionForCsvAndTxt() {
    assertEquals("SakuPintar_2026-11.csv", ReportExporter.defaultFileName("2026-11", ReportFormat.CSV))
    assertEquals("SakuPintar_2026-11.txt", ReportExporter.defaultFileName("2026-11", ReportFormat.TXT))
  }

  @Test
  fun buildMonthlyPlainText_includesAllTransactionsAndSummary() {
    val expenses = sampleExpenses()
    val budget = MonthlyBudget(
      monthKey = "2026-11",
      monthlyIncome = 5_000_000L,
      savingsPercent = 20,
      savingsTarget = 1_000_000L,
      categoryBudgets = mapOf(
        ExpenseCategory.FOOD.name to 2_000_000L,
        ExpenseCategory.TRANSPORT.name to 2_000_000L,
      ),
    )
    val insight = BudgetAutoComposer.calculateDailyInsight(
      monthKey = "2026-11",
      budget = budget,
      monthExpenses = expenses,
      today = LocalDate.of(2026, 11, 10),
    )

    val text = ReportExporter.buildMonthlyPlainText(
      monthKey = "2026-11",
      expenses = expenses,
      budget = budget,
      dailyInsight = insight,
      context = null,
      currency = AppCurrency.IDR,
    )

    assertTrue(text.contains("CADANGAN PENGELUARAN BULANAN SAKUPINTAR"))
    assertTrue(text.contains("2026-11-01"))
    assertTrue(text.contains("Kopi, Roti \"Pagi\""))
    assertTrue(text.contains("Pengeluaran 7"))
    assertTrue(text.contains("Catatan: Catatan baris 1 baris 2"))
  }

  @Test
  fun buildMonthlyCsv_escapesSpecialCharactersAndWritesUtf8Output() {
    val expenses = sampleExpenses()
    val insight = BudgetAutoComposer.calculateDailyInsight(
      monthKey = "2026-11",
      budget = null,
      monthExpenses = expenses,
      today = LocalDate.of(2026, 11, 10),
    )

    val csvOut = ByteArrayOutputStream()
    ReportExporter.writeMonthlyReport(
      outputStream = csvOut,
      format = ReportFormat.CSV,
      monthKey = "2026-11",
      expenses = expenses,
      budget = null,
      dailyInsight = insight,
      context = null,
      currency = AppCurrency.IDR,
    )
    val csvString = csvOut.toString(Charsets.UTF_8.name())
    assertTrue(csvString.startsWith("\uFEFF"))
    assertTrue(csvString.contains("\"Kopi, Roti \"\"Pagi\"\"\""))
    assertTrue(csvString.contains("\"Catatan baris 1 baris 2\""))
    assertTrue(csvString.contains("\"Pengeluaran 7\""))

    val txtOut = ByteArrayOutputStream()
    ReportExporter.writeMonthlyReport(
      outputStream = txtOut,
      format = ReportFormat.TXT,
      monthKey = "2026-11",
      expenses = expenses,
      budget = null,
      dailyInsight = insight,
      context = null,
      currency = AppCurrency.IDR,
    )
    assertTrue(txtOut.toString(Charsets.UTF_8.name()).contains("Pengeluaran 7"))
  }
}
