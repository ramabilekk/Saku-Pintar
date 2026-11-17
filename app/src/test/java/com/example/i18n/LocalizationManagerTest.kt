package com.example.i18n

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.R
import com.example.data.budget.BudgetAutoComposer
import com.example.data.model.BudgetStrategy
import com.example.util.Formatters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LocalizationManagerTest {

  @Test
  fun allSevenLanguagesResolveDistinctLocalizedStrings() {
    val baseContext = ApplicationProvider.getApplicationContext<Context>()

    val idCtx = LanguagePreferenceManager.localizedContext(baseContext, AppLanguage.INDONESIAN)
    val enCtx = LanguagePreferenceManager.localizedContext(baseContext, AppLanguage.ENGLISH)
    val arCtx = LanguagePreferenceManager.localizedContext(baseContext, AppLanguage.ARABIC)
    val zhCtx = LanguagePreferenceManager.localizedContext(baseContext, AppLanguage.CHINESE)
    val frCtx = LanguagePreferenceManager.localizedContext(baseContext, AppLanguage.FRENCH)
    val ruCtx = LanguagePreferenceManager.localizedContext(baseContext, AppLanguage.RUSSIAN)
    val esCtx = LanguagePreferenceManager.localizedContext(baseContext, AppLanguage.SPANISH)

    assertEquals("Beranda", idCtx.getString(R.string.nav_home))
    assertEquals("Home", enCtx.getString(R.string.nav_home))
    assertEquals("الرئيسية", arCtx.getString(R.string.nav_home))
    assertEquals("首页", zhCtx.getString(R.string.nav_home))
    assertEquals("Accueil", frCtx.getString(R.string.nav_home))
    assertEquals("Главная", ruCtx.getString(R.string.nav_home))
    assertEquals("Inicio", esCtx.getString(R.string.nav_home))

    val signInStrings = listOf(idCtx, enCtx, arCtx, zhCtx, frCtx, ruCtx, esCtx)
      .map { it.getString(R.string.auth_sign_in_google) }
    assertEquals(7, signInStrings.distinct().size)
  }

  @Test
  fun rtlFlagMatchesArabicAndExcludesLtrLanguages() {
    assertTrue(AppLanguage.ARABIC.isRtl)
    assertTrue(LanguagePreferenceManager.isRtl(AppLanguage.ARABIC))
    assertFalse(LanguagePreferenceManager.isRtl(AppLanguage.INDONESIAN))
    assertFalse(LanguagePreferenceManager.isRtl(AppLanguage.ENGLISH))
    assertFalse(LanguagePreferenceManager.isRtl(AppLanguage.CHINESE))
    assertFalse(LanguagePreferenceManager.isRtl(AppLanguage.FRENCH))
    assertFalse(LanguagePreferenceManager.isRtl(AppLanguage.RUSSIAN))
    assertFalse(LanguagePreferenceManager.isRtl(AppLanguage.SPANISH))
  }

  @Test
  fun formattersAndAutoComposerRespectSelectedCurrency() {
    val usdFormatted = Formatters.formatMoney(3500L, AppCurrency.USD)
    val sarFormatted = Formatters.formatMoney(10000L, AppCurrency.SAR)
    val cnyFormatted = Formatters.formatMoney(12000L, AppCurrency.CNY)
    val rubFormatted = Formatters.formatMoney(120000L, AppCurrency.RUB)
    val idrFormatted = Formatters.formatMoney(8000000L, AppCurrency.IDR)

    assertTrue(usdFormatted.startsWith("$"))
    assertTrue(sarFormatted.startsWith("SAR "))
    assertTrue(cnyFormatted.startsWith("¥"))
    assertTrue(rubFormatted.startsWith("₽"))
    assertTrue(idrFormatted.startsWith("Rp "))

    val usdBudget = BudgetAutoComposer.composeBudget(
      userId = "u1",
      monthKey = "2026-06",
      monthlyIncome = 3_500L,
      strategy = BudgetStrategy.BALANCED_50_30_20,
      roundingUnit = AppCurrency.USD.roundingUnit,
    )
    assertEquals(3_500L, usdBudget.monthlyIncome)
    assertEquals(700L, usdBudget.savingsTarget)
    assertNotEquals(0L, usdBudget.totalAllocatedSpendBudget)
    assertEquals(3_500L, usdBudget.savingsTarget + usdBudget.totalAllocatedSpendBudget)
  }

  @Test
  fun languagePickerRemovesUnOfficialWordingAcrossAllLocales() {
    val baseContext = ApplicationProvider.getApplicationContext<Context>()
    val forbiddenMarkers = listOf("PBB", "UN ", "UN-", "United Nations", "الأمم", "ONU", "ООН", "联合国")

    val supportedLanguages = AppLanguage.entries.filter { it != AppLanguage.SYSTEM }
    assertEquals(7, supportedLanguages.size)

    supportedLanguages.forEach { lang ->
      val ctx = LanguagePreferenceManager.localizedContext(baseContext, lang)
      val subtitle = ctx.getString(R.string.language_section_subtitle)
      forbiddenMarkers.forEach { marker ->
        assertFalse(
          "Expected no UN/PBB reference '$marker' in ${lang.name} subtitle: $subtitle",
          subtitle.contains(marker, ignoreCase = true),
        )
      }
      val customReminder = ctx.getString(R.string.reminder_custom_time_action)
      val calendarTitle = ctx.getString(R.string.calendar_section_title)
      assertTrue(customReminder.isNotBlank())
      assertTrue(calendarTitle.isNotBlank())
    }
  }
}
