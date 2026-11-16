package com.example.i18n

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.budget.BudgetPaceStatus
import com.example.data.budget.DailyBudgetInsight
import com.example.notifications.ReminderSettings
import com.example.ui.components.AccountProfileDialog
import com.example.ui.components.ExpenseEditorSheet
import com.example.ui.components.ExportReportSheet
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.SakuPintarTheme
import com.example.ui.viewmodel.AiExpenseParseState
import com.example.ui.viewmodel.FinanceDashboardData
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ModalLocalizationCrashTest {

  @get:Rule
  val composeRule = createAndroidComposeRule<ComponentActivity>()

  private fun unwrapActivity(context: Context): Activity? {
    var current: Context? = context
    while (current is ContextWrapper) {
      if (current is Activity) return current
      current = current.baseContext
    }
    return current as? Activity
  }

  @Test
  fun wrappedActivityContextPreservesActivityIdentityAndLocalizedResources() {
    val wrapped = LanguagePreferenceManager.wrapActivityContext(composeRule.activity, AppLanguage.ARABIC)
    assertTrue(unwrapActivity(wrapped) === composeRule.activity)
    assertTrue(wrapped.getString(com.example.R.string.nav_home) == "الرئيسية")
  }

  @Test
  fun accountProfileDialogOpensAndShowsControlsUnderWrappedLocalizedContext() {
    composeRule.activity.runOnUiThread {
      composeRule.activity.setContent {
        val localizedContext = LanguagePreferenceManager.wrapActivityContext(
          composeRule.activity,
          AppLanguage.INDONESIAN,
        )
        CompositionLocalProvider(
          LocalContext provides localizedContext,
          LocalConfiguration provides localizedContext.resources.configuration,
          LocalLayoutDirection provides LayoutDirection.Ltr,
          LocalActivityResultRegistryOwner provides composeRule.activity,
        ) {
          SakuPintarTheme {
            AccountProfileDialog(
              displayName = "Budi",
              email = "budi@example.com",
              customPhotoBase64 = null,
              googlePhotoUrl = null,
              isSavingProfile = false,
              themeMode = AppThemeMode.SYSTEM,
              reminderSettings = ReminderSettings(),
              onReminderSettingsChange = {},
              onSaveDisplayName = {},
              onPickPhotoUri = {},
              onSyncGooglePhoto = {},
              onStatusMessage = {},
              onSignOut = {},
              onDismiss = {},
            )
          }
        }
      }
    }

    composeRule.waitForIdle()
    composeRule.onNodeWithTag("profile_language_button").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("pick_profile_photo_button").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("profile_name_row").performScrollTo().performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("profile_name_input").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("profile_currency_button").performScrollTo().performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("currency_chip_USD").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("profile_theme_button").performScrollTo().performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("theme_mode_DARK").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("profile_language_button").performScrollTo().performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("language_option_ENGLISH").assertIsDisplayed()
  }

  @Test
  fun expenseEditorSheetOpensUnderWrappedLocalizedContextWithoutCrash() {
    composeRule.activity.runOnUiThread {
      composeRule.activity.setContent {
        val localizedContext = LanguagePreferenceManager.wrapActivityContext(
          composeRule.activity,
          AppLanguage.FRENCH,
        )
        CompositionLocalProvider(
          LocalContext provides localizedContext,
          LocalConfiguration provides localizedContext.resources.configuration,
          LocalLayoutDirection provides LayoutDirection.Ltr,
          LocalActivityResultRegistryOwner provides composeRule.activity,
        ) {
          SakuPintarTheme {
            ExpenseEditorSheet(
              existingExpense = null,
              aiParseState = AiExpenseParseState.Idle,
              onParseWithAi = {},
              onScanReceiptUri = {},
              onClearAiParse = {},
              onDismiss = {},
              onSave = { _, _, _, _, _, _ -> },
            )
          }
        }
      }
    }

    composeRule.waitForIdle()
    composeRule.onNodeWithTag("ai_scan_receipt_button").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("expense_amount_input").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("expense_title_input").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("save_expense_button").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun exportReportSheetOpensUnderWrappedLocalizedContextWithoutCrash() {
    composeRule.activity.runOnUiThread {
      composeRule.activity.setContent {
        val localizedContext = LanguagePreferenceManager.wrapActivityContext(
          composeRule.activity,
          AppLanguage.SPANISH,
        )
        CompositionLocalProvider(
          LocalContext provides localizedContext,
          LocalConfiguration provides localizedContext.resources.configuration,
          LocalLayoutDirection provides LayoutDirection.Ltr,
          LocalActivityResultRegistryOwner provides composeRule.activity,
        ) {
          SakuPintarTheme {
            ExportReportSheet(
              data = FinanceDashboardData(
                monthKey = "2026-11",
                expenses = emptyList(),
                filteredExpenses = emptyList(),
                allHistoricalExpenses = emptyList(),
                recurringTemplates = emptyList(),
                budget = null,
                dailyInsight = DailyBudgetInsight(
                  totalBudget = 1_000_000L,
                  totalSpent = 0L,
                  remainingBudget = 1_000_000L,
                  daysInMonth = 30,
                  daysRemaining = 20,
                  dailySafeLimit = 50_000L,
                  todaySpent = 0L,
                  todayRemaining = 50_000L,
                  paceStatus = BudgetPaceStatus.SAFE,
                ),
                categoryBreakdown = emptyList(),
                selectedCategoryFilter = null,
                searchQuery = "",
              ),
              onStatusMessage = {},
              onDismiss = {},
            )
          }
        }
      }
    }

    composeRule.waitForIdle()
    composeRule.onNodeWithTag("export_option_excel").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("export_option_pdf").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("export_option_csv").performScrollTo().assertIsDisplayed()
  }
}
