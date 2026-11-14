package com.example.util

import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

object Formatters {
  private val indonesianLocale = Locale.forLanguageTag("id-ID")
  private val numberFormat = NumberFormat.getIntegerInstance(indonesianLocale)
  private val dayDateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMM yyyy", indonesianLocale)
  private val shortDateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", indonesianLocale)

  private val monthNamesId = listOf(
    "Januari", "Februari", "Maret", "April", "Mei", "Juni",
    "Juli", "Agustus", "September", "Oktober", "November", "Desember"
  )

  fun formatRupiah(amount: Long): String {
    val prefix = if (amount < 0L) "-Rp " else "Rp "
    return prefix + numberFormat.format(abs(amount))
  }

  fun formatRupiahCompact(amount: Long): String {
    val absVal = abs(amount)
    val prefix = if (amount < 0L) "-" else ""
    return when {
      absVal >= 1_000_000_000L -> "${prefix}Rp ${String.format(indonesianLocale, "%.1f", absVal / 1_000_000_000.0)} M"
      absVal >= 1_000_000L -> {
        val m = absVal / 1_000_000.0
        val str = if (absVal % 1_000_000L == 0L) "${absVal / 1_000_000L}" else String.format(indonesianLocale, "%.1f", m)
        "${prefix}Rp $str jt"
      }
      absVal >= 1_000L -> "${prefix}Rp ${absVal / 1_000L} rb"
      else -> "${prefix}Rp $absVal"
    }
  }

  fun currentMonthKey(today: LocalDate = LocalDate.now()): String =
    YearMonth.from(today).toString()

  fun shiftMonthKey(monthKey: String, deltaMonths: Long): String {
    val ym = runCatching { YearMonth.parse(monthKey) }.getOrElse { YearMonth.now() }
    return ym.plusMonths(deltaMonths).toString()
  }

  fun formatMonthTitle(monthKey: String): String {
    val ym = runCatching { YearMonth.parse(monthKey) }.getOrElse { YearMonth.now() }
    val monthName = monthNamesId.getOrElse(ym.monthValue - 1) { ym.month.name }
    return "$monthName ${ym.year}"
  }

  fun formatDateGroupLabel(epochDay: Long, today: LocalDate = LocalDate.now()): String {
    val date = LocalDate.ofEpochDay(epochDay)
    return when (date) {
      today -> "Hari Ini • ${date.format(shortDateFormatter)}"
      today.minusDays(1) -> "Kemarin • ${date.format(shortDateFormatter)}"
      else -> date.format(dayDateFormatter)
    }
  }

  fun formatShortDate(epochDay: Long): String =
    LocalDate.ofEpochDay(epochDay).format(shortDateFormatter)
}
