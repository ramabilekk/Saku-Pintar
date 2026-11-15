package com.example.i18n

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class AppCurrency(
  val code: String,
  val symbol: String,
  val localeTag: String,
  val displayName: String,
  val defaultMonthlyIncome: Long,
  val budgetStepAmount: Long,
  val budgetStepCompactLabel: String,
  val roundingUnit: Long,
  val incomePresets: List<Pair<Long, String>>,
  val expenseQuickDeltas: List<Pair<Long, String>>,
  val savingsTargetPresets: List<Pair<Long, String>>,
  val savingsTxPresets: List<Pair<Long, String>>,
  val walletQuickDeltas: List<Pair<Long, String>>,
  val chartMinMax: Long,
) {
  IDR(
    code = "IDR",
    symbol = "Rp",
    localeTag = "id-ID",
    displayName = "IDR (Rp)",
    defaultMonthlyIncome = 8_000_000L,
    budgetStepAmount = 10_000L,
    budgetStepCompactLabel = "10rb",
    roundingUnit = 10_000L,
    incomePresets = listOf(5_000_000L to "5 jt", 8_000_000L to "8 jt", 10_000_000L to "10 jt", 15_000_000L to "15 jt"),
    expenseQuickDeltas = listOf(15_000L to "+15rb", 25_000L to "+25rb", 50_000L to "+50rb", 100_000L to "+100rb"),
    savingsTargetPresets = listOf(1_000_000L to "1 Jt", 3_000_000L to "3 Jt", 5_000_000L to "5 Jt", 10_000_000L to "10 Jt"),
    savingsTxPresets = listOf(50_000L to "50rb", 100_000L to "100rb", 250_000L to "250rb", 500_000L to "500rb", 1_000_000L to "1 Jt"),
    walletQuickDeltas = listOf(50_000L to "+50rb", 100_000L to "+100rb", 500_000L to "+500rb"),
    chartMinMax = 100_000L,
  ),
  USD(
    code = "USD",
    symbol = "$",
    localeTag = "en-US",
    displayName = "USD ($)",
    defaultMonthlyIncome = 3_500L,
    budgetStepAmount = 10L,
    budgetStepCompactLabel = "$10",
    roundingUnit = 10L,
    incomePresets = listOf(2_000L to "$2k", 3_500L to "$3.5k", 5_000L to "$5k", 8_000L to "$8k"),
    expenseQuickDeltas = listOf(10L to "+$10", 25L to "+$25", 50L to "+$50", 100L to "+$100"),
    savingsTargetPresets = listOf(500L to "$500", 1_000L to "$1k", 3_000L to "$3k", 5_000L to "$5k"),
    savingsTxPresets = listOf(25L to "$25", 50L to "$50", 100L to "$100", 250L to "$250", 500L to "$500"),
    walletQuickDeltas = listOf(50L to "+$50", 100L to "+$100", 500L to "+$500"),
    chartMinMax = 100L,
  ),
  EUR(
    code = "EUR",
    symbol = "€",
    localeTag = "fr-FR",
    displayName = "EUR (€)",
    defaultMonthlyIncome = 3_000L,
    budgetStepAmount = 10L,
    budgetStepCompactLabel = "€10",
    roundingUnit = 10L,
    incomePresets = listOf(2_000L to "€2k", 3_000L to "€3k", 4_500L to "€4.5k", 7_000L to "€7k"),
    expenseQuickDeltas = listOf(10L to "+€10", 25L to "+€25", 50L to "+€50", 100L to "+€100"),
    savingsTargetPresets = listOf(500L to "€500", 1_000L to "€1k", 3_000L to "€3k", 5_000L to "€5k"),
    savingsTxPresets = listOf(25L to "€25", 50L to "€50", 100L to "€100", 250L to "€250", 500L to "€500"),
    walletQuickDeltas = listOf(50L to "+€50", 100L to "+€100", 500L to "+€500"),
    chartMinMax = 100L,
  ),
  GBP(
    code = "GBP",
    symbol = "£",
    localeTag = "en-GB",
    displayName = "GBP (£)",
    defaultMonthlyIncome = 2_800L,
    budgetStepAmount = 10L,
    budgetStepCompactLabel = "£10",
    roundingUnit = 10L,
    incomePresets = listOf(1_800L to "£1.8k", 2_800L to "£2.8k", 4_000L to "£4k", 6_000L to "£6k"),
    expenseQuickDeltas = listOf(10L to "+£10", 20L to "+£20", 50L to "+£50", 100L to "+£100"),
    savingsTargetPresets = listOf(500L to "£500", 1_000L to "£1k", 2_500L to "£2.5k", 5_000L to "£5k"),
    savingsTxPresets = listOf(20L to "£20", 50L to "£50", 100L to "£100", 250L to "£250", 500L to "£500"),
    walletQuickDeltas = listOf(50L to "+£50", 100L to "+£100", 500L to "+£500"),
    chartMinMax = 100L,
  ),
  SAR(
    code = "SAR",
    symbol = "SAR",
    localeTag = "ar-SA",
    displayName = "SAR",
    defaultMonthlyIncome = 10_000L,
    budgetStepAmount = 50L,
    budgetStepCompactLabel = "50",
    roundingUnit = 50L,
    incomePresets = listOf(6_000L to "6k", 10_000L to "10k", 15_000L to "15k", 25_000L to "25k"),
    expenseQuickDeltas = listOf(20L to "+20", 50L to "+50", 100L to "+100", 250L to "+250"),
    savingsTargetPresets = listOf(2_000L to "2k", 5_000L to "5k", 10_000L to "10k", 20_000L to "20k"),
    savingsTxPresets = listOf(50L to "50", 100L to "100", 250L to "250", 500L to "500", 1_000L to "1k"),
    walletQuickDeltas = listOf(100L to "+100", 500L to "+500", 1_000L to "+1k"),
    chartMinMax = 300L,
  ),
  CNY(
    code = "CNY",
    symbol = "¥",
    localeTag = "zh-CN",
    displayName = "CNY (¥)",
    defaultMonthlyIncome = 12_000L,
    budgetStepAmount = 50L,
    budgetStepCompactLabel = "¥50",
    roundingUnit = 50L,
    incomePresets = listOf(8_000L to "¥8k", 12_000L to "¥12k", 20_000L to "¥20k", 30_000L to "¥30k"),
    expenseQuickDeltas = listOf(20L to "+¥20", 50L to "+¥50", 100L to "+¥100", 200L to "+¥200"),
    savingsTargetPresets = listOf(2_000L to "¥2k", 5_000L to "¥5k", 10_000L to "¥10k", 20_000L to "¥20k"),
    savingsTxPresets = listOf(50L to "¥50", 100L to "¥100", 300L to "¥300", 500L to "¥500", 1_000L to "¥1k"),
    walletQuickDeltas = listOf(100L to "+¥100", 500L to "+¥500", 1_000L to "+¥1k"),
    chartMinMax = 500L,
  ),
  RUB(
    code = "RUB",
    symbol = "₽",
    localeTag = "ru-RU",
    displayName = "RUB (₽)",
    defaultMonthlyIncome = 120_000L,
    budgetStepAmount = 500L,
    budgetStepCompactLabel = "500₽",
    roundingUnit = 500L,
    incomePresets = listOf(60_000L to "60k₽", 120_000L to "120k₽", 200_000L to "200k₽", 300_000L to "300k₽"),
    expenseQuickDeltas = listOf(200L to "+200₽", 500L to "+500₽", 1_000L to "+1k₽", 2_500L to "+2.5k₽"),
    savingsTargetPresets = listOf(20_000L to "20k₽", 50_000L to "50k₽", 100_000L to "100k₽", 250_000L to "250k₽"),
    savingsTxPresets = listOf(500L to "500₽", 1_000L to "1k₽", 2_500L to "2.5k₽", 5_000L to "5k₽", 10_000L to "10k₽"),
    walletQuickDeltas = listOf(1_000L to "+1k₽", 5_000L to "+5k₽", 10_000L to "+10k₽"),
    chartMinMax = 3_000L,
  ),
  SGD(
    code = "SGD",
    symbol = "S$",
    localeTag = "en-SG",
    displayName = "SGD (S$)",
    defaultMonthlyIncome = 4_500L,
    budgetStepAmount = 10L,
    budgetStepCompactLabel = "S$10",
    roundingUnit = 10L,
    incomePresets = listOf(2_500L to "S$2.5k", 4_500L to "S$4.5k", 6_500L to "S$6.5k", 10_000L to "S$10k"),
    expenseQuickDeltas = listOf(10L to "+S$10", 25L to "+S$25", 50L to "+S$50", 100L to "+S$100"),
    savingsTargetPresets = listOf(1_000L to "S$1k", 3_000L to "S$3k", 5_000L to "S$5k", 10_000L to "S$10k"),
    savingsTxPresets = listOf(25L to "S$25", 50L to "S$50", 100L to "S$100", 250L to "S$250", 500L to "S$500"),
    walletQuickDeltas = listOf(50L to "+S$50", 100L to "+S$100", 500L to "+S$500"),
    chartMinMax = 100L,
  ),
  MYR(
    code = "MYR",
    symbol = "RM",
    localeTag = "ms-MY",
    displayName = "MYR (RM)",
    defaultMonthlyIncome = 5_000L,
    budgetStepAmount = 20L,
    budgetStepCompactLabel = "RM20",
    roundingUnit = 20L,
    incomePresets = listOf(3_000L to "RM3k", 5_000L to "RM5k", 8_000L to "RM8k", 12_000L to "RM12k"),
    expenseQuickDeltas = listOf(10L to "+RM10", 25L to "+RM25", 50L to "+RM50", 100L to "+RM100"),
    savingsTargetPresets = listOf(1_000L to "RM1k", 3_000L to "RM3k", 5_000L to "RM5k", 10_000L to "RM10k"),
    savingsTxPresets = listOf(50L to "RM50", 100L to "RM100", 250L to "RM250", 500L to "RM500", 1_000L to "RM1k"),
    walletQuickDeltas = listOf(50L to "+RM50", 100L to "+RM100", 500L to "+RM500"),
    chartMinMax = 150L,
  );

  val locale: Locale
    get() = Locale.forLanguageTag(localeTag)

  val prefixWithSpace: String
    get() = when (this) {
      IDR, SAR, MYR -> "$symbol "
      else -> symbol
    }

  companion object {
    fun fromCode(code: String?): AppCurrency =
      entries.firstOrNull { it.code == code || it.name == code } ?: IDR
  }
}

object CurrencyPreferenceManager {
  private const val PREFS_NAME = "sakupintar_currency_prefs"
  private const val KEY_CURRENCY = "app_currency"

  private val _currency = MutableStateFlow(AppCurrency.IDR)
  @Volatile
  private var initialized = false

  val currentCurrency: AppCurrency
    get() = _currency.value

  fun observeCurrency(context: Context): StateFlow<AppCurrency> {
    if (!initialized) {
      val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      _currency.value = AppCurrency.fromCode(prefs.getString(KEY_CURRENCY, null))
      initialized = true
    }
    return _currency.asStateFlow()
  }

  fun setCurrency(context: Context, currency: AppCurrency) {
    val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putString(KEY_CURRENCY, currency.code).apply()
    initialized = true
    _currency.value = currency
  }
}
