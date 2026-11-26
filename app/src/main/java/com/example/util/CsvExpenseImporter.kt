package com.example.util

import android.content.Context
import android.net.Uri
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.PaymentMethod
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Zero-permission SAF CSV expense importer supporting SakuPintar CSV exports
 * as well as standard comma/semicolon-separated personal expense spreadsheets.
 */
object CsvExpenseImporter {

  private val DATE_FORMATTERS = listOf(
    DateTimeFormatter.ISO_LOCAL_DATE,
    DateTimeFormatter.ofPattern("dd/MM/yyyy"),
    DateTimeFormatter.ofPattern("d/M/yyyy"),
    DateTimeFormatter.ofPattern("dd-MM-yyyy"),
    DateTimeFormatter.ofPattern("d-M-yyyy"),
    DateTimeFormatter.ofPattern("yyyy/MM/dd"),
    DateTimeFormatter.ofPattern("MM/dd/yyyy"),
  )

  fun parseCsvFromUri(
    context: Context,
    uri: Uri,
    userId: String,
  ): Result<List<Expense>> = runCatching {
    val rawText = context.contentResolver.openInputStream(uri)?.use { stream ->
      BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).readText()
    } ?: error("Unable to open selected CSV file")
    parseCsvText(rawText, userId)
  }

  fun parseCsvText(
    rawCsv: String,
    userId: String,
  ): List<Expense> {
    val cleaned = rawCsv.removePrefix("\uFEFF").trim()
    if (cleaned.isEmpty()) return emptyList()

    val delimiter = detectDelimiter(cleaned)
    val rows = parseRfc4180Rows(cleaned, delimiter)
    if (rows.isEmpty()) return emptyList()

    var headerRowIndex = -1
    var dateCol = -1
    var titleCol = -1
    var categoryCol = -1
    var paymentCol = -1
    var amountCol = -1
    var noteCol = -1

    for (i in 0 until minOf(rows.size, 8)) {
      val row = rows[i].map { it.trim().lowercase() }
      val foundTitle = row.indexOfFirst {
        it.contains("judul") || it.contains("title") || it.contains("deskrip") ||
          it.contains("description") || it.contains("keterangan") || it.contains("merchant") || it == "item"
      }
      val foundAmount = row.indexOfFirst {
        it.contains("nominal") || it.contains("amount") || it.contains("jumlah") ||
          it.contains("harga") || it.contains("price") || it.contains("total") || it.contains("biaya")
      }
      if (foundTitle >= 0 && foundAmount >= 0) {
        headerRowIndex = i
        titleCol = foundTitle
        amountCol = foundAmount
        dateCol = row.indexOfFirst { it.contains("tanggal") || it.contains("date") || it.contains("tgl") }
        categoryCol = row.indexOfFirst { it.contains("kategori") || it.contains("category") }
        paymentCol = row.indexOfFirst {
          it.contains("metode") || it.contains("payment") || it.contains("bayar") || it.contains("wallet") || it.contains("dompet")
        }
        noteCol = row.indexOfFirst { it.contains("catatan") || it.contains("note") || it.contains("memo") }
        break
      }
    }

    // Fallback positional mapping if no header row detected: Date, Title, Category, Payment, Amount, Note
    val startIndex = if (headerRowIndex >= 0) {
      headerRowIndex + 1
    } else {
      dateCol = 0
      titleCol = 1
      categoryCol = 2
      paymentCol = 3
      amountCol = 4
      noteCol = 5
      0
    }

    val imported = mutableListOf<Expense>()
    val today = LocalDate.now()

    for (i in startIndex until rows.size) {
      val row = rows[i]
      if (row.all { it.isBlank() }) continue

      val firstCell = row.firstOrNull()?.trim()?.lowercase().orEmpty()
      if (
        firstCell.startsWith("ringkasan") ||
        firstCell.startsWith("summary") ||
        firstCell.startsWith("monthly summary") ||
        firstCell.startsWith("total pengeluaran") ||
        firstCell.startsWith("total expense") ||
        firstCell.startsWith("total spent")
      ) {
        break
      }

      val rawTitle = row.getOrNull(titleCol)?.trim().orEmpty()
      val rawAmount = row.getOrNull(amountCol)?.trim().orEmpty()
      val amount = parseAmount(rawAmount)
      if (rawTitle.isBlank() || amount <= 0L) continue

      val parsedDate = parseDate(row.getOrNull(dateCol)?.trim()) ?: today
      val epochDay = parsedDate.toEpochDay()
      val monthKey = Formatters.currentMonthKey(parsedDate)
      val category = parseCategory(row.getOrNull(categoryCol)?.trim())
      val payment = parsePaymentMethod(row.getOrNull(paymentCol)?.trim())
      val note = row.getOrNull(noteCol)?.trim().orEmpty().take(200)

      imported.add(
        Expense(
          id = UUID.randomUUID().toString(),
          userId = userId,
          title = rawTitle.take(80),
          amount = amount,
          category = category.name,
          paymentMethod = payment.name,
          dateEpochDay = epochDay,
          monthKey = monthKey,
          note = note,
        ),
      )
    }

    return imported
  }

  private fun detectDelimiter(text: String): Char {
    val firstLine = text.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty()
    val commaCount = firstLine.count { it == ',' }
    val semicolonCount = firstLine.count { it == ';' }
    return if (semicolonCount > commaCount) ';' else ','
  }

  private fun parseRfc4180Rows(text: String, delimiter: Char): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    var currentRow = mutableListOf<String>()
    val cell = StringBuilder()
    var inQuotes = false
    var i = 0
    val len = text.length

    while (i < len) {
      val ch = text[i]
      if (inQuotes) {
        if (ch == '"') {
          if (i + 1 < len && text[i + 1] == '"') {
            cell.append('"')
            i += 2
            continue
          } else {
            inQuotes = false
          }
        } else {
          cell.append(ch)
        }
      } else {
        when (ch) {
          '"' -> inQuotes = true
          delimiter -> {
            currentRow.add(cell.toString())
            cell.clear()
          }
          '\r' -> {
            if (i + 1 < len && text[i + 1] == '\n') i++
            currentRow.add(cell.toString())
            cell.clear()
            rows.add(currentRow)
            currentRow = mutableListOf()
          }
          '\n' -> {
            currentRow.add(cell.toString())
            cell.clear()
            rows.add(currentRow)
            currentRow = mutableListOf()
          }
          else -> cell.append(ch)
        }
      }
      i++
    }

    if (cell.isNotEmpty() || currentRow.isNotEmpty()) {
      currentRow.add(cell.toString())
      rows.add(currentRow)
    }

    return rows
  }

  private fun parseDate(raw: String?): LocalDate? {
    val cleaned = raw?.trim().orEmpty()
    if (cleaned.isBlank()) return null

    // Handle ISO date prefix inside timestamp (e.g. "2026-11-22 10:30:00")
    val dateToken = cleaned.substringBefore(' ').substringBefore('T')
    for (formatter in DATE_FORMATTERS) {
      val parsed = runCatching { LocalDate.parse(dateToken, formatter) }.getOrNull()
      if (parsed != null && parsed.year in 2015..2040) {
        return parsed
      }
    }
    return null
  }

  private fun parseAmount(raw: String): Long {
    if (raw.isBlank()) return 0L
    // Strip currency labels & spaces, keep digits, dot, comma
    var s = raw.trim()
      .replace("Rp", "", ignoreCase = true)
      .replace("IDR", "", ignoreCase = true)
      .replace("USD", "", ignoreCase = true)
      .replace("$", "")
      .replace("€", "")
      .replace("£", "")
      .replace("¥", "")
      .replace(" ", "")

    // If string ends with decimal cents like ".00" or ",00" (2 digits after last separator)
    // and is not a thousands separator (which has 3 digits), drop fractional cents
    val decimalDot = Regex("""^[\d,]+\.\d{1,2}$""")
    val decimalComma = Regex("""^[\d.]+,\d{1,2}$""")
    s = when {
      decimalDot.matches(s) -> s.substringBeforeLast('.')
      decimalComma.matches(s) -> s.substringBeforeLast(',')
      else -> s
    }

    val digitsOnly = s.filter { it.isDigit() }
    return digitsOnly.toLongOrNull() ?: 0L
  }

  private fun parseCategory(raw: String?): ExpenseCategory {
    val s = raw?.trim()?.lowercase().orEmpty()
    if (s.isBlank()) return ExpenseCategory.OTHER
    ExpenseCategory.entries.firstOrNull {
      it.name.equals(s, ignoreCase = true) || it.labelId.equals(s, ignoreCase = true)
    }?.let { return it }

    return when {
      s.contains("makan") || s.contains("minum") || s.contains("food") || s.contains("drink") ||
        s.contains("kopi") || s.contains("coffee") || s.contains("resto") || s.contains("grocer") -> ExpenseCategory.FOOD
      s.contains("tagihan") || s.contains("bill") || s.contains("util") || s.contains("listrik") ||
        s.contains("internet") || s.contains("wifi") || s.contains("sewa") || s.contains("rent") -> ExpenseCategory.BILLS
      s.contains("transport") || s.contains("bensin") || s.contains("fuel") || s.contains("ojek") ||
        s.contains("gojek") || s.contains("grab") || s.contains("parkir") || s.contains("tol") -> ExpenseCategory.TRANSPORT
      s.contains("sehat") || s.contains("health") || s.contains("obat") || s.contains("dokter") ||
        s.contains("med") || s.contains("apotek") -> ExpenseCategory.HEALTH
      s.contains("belanja") || s.contains("shop") || s.contains("baju") || s.contains("fashion") ||
        s.contains("mart") -> ExpenseCategory.SHOPPING
      s.contains("hiburan") || s.contains("entertain") || s.contains("nonton") || s.contains("game") ||
        s.contains("musik") || s.contains("film") -> ExpenseCategory.ENTERTAINMENT
      else -> ExpenseCategory.OTHER
    }
  }

  private fun parsePaymentMethod(raw: String?): PaymentMethod {
    val s = raw?.trim()?.lowercase().orEmpty()
    if (s.isBlank()) return PaymentMethod.QRIS
    PaymentMethod.entries.firstOrNull {
      it.name.equals(s, ignoreCase = true) || it.labelId.equals(s, ignoreCase = true)
    }?.let { return it }

    return when {
      s.contains("tunai") || s.contains("cash") -> PaymentMethod.CASH
      s.contains("transfer") || s.contains("bank") || s.contains("bca") || s.contains("mandiri") ||
        s.contains("bri") || s.contains("bni") || s.contains("seabank") -> PaymentMethod.TRANSFER
      s.contains("kartu") || s.contains("card") || s.contains("debit") || s.contains("kredit") ||
        s.contains("credit") || s.contains("visa") || s.contains("master") -> PaymentMethod.CARD
      else -> PaymentMethod.QRIS
    }
  }
}
