package com.example.util

import com.example.i18n.AppCurrency
import com.example.i18n.CurrencyPreferenceManager
import com.example.i18n.LanguagePreferenceManager
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

object Formatters {

  fun formatRupiah(amount: Long): String =
    formatMoney(amount, CurrencyPreferenceManager.currentCurrency)

  fun formatMoney(
    amount: Long,
    currency: AppCurrency = CurrencyPreferenceManager.currentCurrency,
  ): String {
    val nf = NumberFormat.getIntegerInstance(currency.locale)
    val sign = if (amount < 0L) "-" else ""
    return "$sign${currency.prefixWithSpace}${nf.format(abs(amount))}"
  }

  fun formatRupiahCompact(amount: Long): String =
    formatMoneyCompact(
      amount = amount,
      currency = CurrencyPreferenceManager.currentCurrency,
      locale = LanguagePreferenceManager.currentLocale(),
    )

  fun formatMoneyCompact(
    amount: Long,
    currency: AppCurrency = CurrencyPreferenceManager.currentCurrency,
    locale: Locale = LanguagePreferenceManager.currentLocale(),
  ): String {
    val absVal = abs(amount)
    val sign = if (amount < 0L) "-" else ""
    val prefix = currency.prefixWithSpace
    val isIndonesian = locale.language.equals("in", ignoreCase = true) ||
      locale.language.equals("id", ignoreCase = true)

    return if (currency == AppCurrency.IDR && isIndonesian) {
      when {
        absVal >= 1_000_000_000L ->
          "${sign}${prefix}${String.format(locale, "%.1f", absVal / 1_000_000_000.0)} M"
        absVal >= 1_000_000L -> {
          val m = absVal / 1_000_000.0
          val str = if (absVal % 1_000_000L == 0L) "${absVal / 1_000_000L}" else String.format(locale, "%.1f", m)
          "${sign}${prefix}$str jt"
        }
        absVal >= 1_000L -> "${sign}${prefix}${absVal / 1_000L} rb"
        else -> "${sign}${prefix}$absVal"
      }
    } else {
      when {
        absVal >= 1_000_000_000L ->
          "${sign}${prefix}${String.format(Locale.US, "%.1f", absVal / 1_000_000_000.0)}B"
        absVal >= 1_000_000L -> {
          val m = absVal / 1_000_000.0
          val str = if (absVal % 1_000_000L == 0L) "${absVal / 1_000_000L}" else String.format(Locale.US, "%.1f", m)
          "${sign}${prefix}${str}M"
        }
        absVal >= 10_000L -> {
          val k = absVal / 1_000.0
          val str = if (absVal % 1_000L == 0L) "${absVal / 1_000L}" else String.format(Locale.US, "%.1f", k)
          "${sign}${prefix}${str}K"
        }
        else -> formatMoney(amount, currency)
      }
    }
  }

  fun currentMonthKey(today: LocalDate = LocalDate.now()): String =
    YearMonth.from(today).toString()

  fun shiftMonthKey(monthKey: String, deltaMonths: Long): String {
    val ym = runCatching { YearMonth.parse(monthKey) }.getOrElse { YearMonth.now() }
    return ym.plusMonths(deltaMonths).toString()
  }

  fun formatMonthTitle(
    monthKey: String,
    locale: Locale = LanguagePreferenceManager.currentLocale(),
  ): String {
    val ym = runCatching { YearMonth.parse(monthKey) }.getOrElse { YearMonth.now() }
    val formatter = DateTimeFormatter.ofPattern("LLLL yyyy", locale)
    val raw = ym.atDay(1).format(formatter)
    return raw.replaceFirstChar { ch ->
      if (ch.isLowerCase()) ch.titlecase(locale) else ch.toString()
    }
  }

  fun formatDateGroupLabel(
    epochDay: Long,
    today: LocalDate = LocalDate.now(),
    todayLabel: String = "Hari Ini",
    yesterdayLabel: String = "Kemarin",
    locale: Locale = LanguagePreferenceManager.currentLocale(),
  ): String {
    val date = LocalDate.ofEpochDay(epochDay)
    val shortFmt = DateTimeFormatter.ofPattern("d MMM yyyy", locale)
    val fullFmt = DateTimeFormatter.ofPattern("EEEE, d MMM yyyy", locale)
    return when (date) {
      today -> "$todayLabel • ${date.format(shortFmt)}"
      today.minusDays(1) -> "$yesterdayLabel • ${date.format(shortFmt)}"
      else -> date.format(fullFmt)
    }
  }

  fun formatShortDate(
    epochDay: Long,
    locale: Locale = LanguagePreferenceManager.currentLocale(),
  ): String = LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern("d MMM yyyy", locale))
}
