package com.example.auth

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.LayoutDirection
import com.example.i18n.AppLanguage
import com.example.i18n.LanguagePreferenceManager
import com.example.ui.auth.AuthScreen
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.SakuPintarTheme
import com.example.ui.theme.ThemePreferenceManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AccountSecurityOtpTest {

  @get:Rule
  val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun authScreen_onlyOffersGoogleAndGuestSignInWithoutPhoneOrOtp() {
    AccountSecurityOtpManager.clearAltSession(composeRule.activity)
    ThemePreferenceManager.setThemeMode(composeRule.activity, AppThemeMode.LIGHT)

    var authSucceeded = false

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
            AuthScreen(onAuthSuccess = { authSucceeded = true })
          }
        }
      }
    }

    composeRule.waitForIdle()
    composeRule.onNodeWithTag("auth_landing_enter_button").performScrollTo().assertIsDisplayed().performClick()
    composeRule.waitForIdle()

    // Verify Google & Guest are displayed while Phone, Email, and OTP are absent
    composeRule.onNodeWithTag("google_sign_in_button").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("auth_guest_method_button").performScrollTo().assertIsDisplayed()
    assertEquals(0, composeRule.onAllNodesWithTag("auth_phone_method_button").fetchSemanticsNodes().size)
    assertEquals(0, composeRule.onAllNodesWithTag("auth_email_method_button").fetchSemanticsNodes().size)
    assertEquals(0, composeRule.onAllNodesWithTag("otp_verification_dialog").fetchSemanticsNodes().size)

    // Clicking Guest enters immediately without any OTP gate
    composeRule.onNodeWithTag("auth_guest_method_button").performScrollTo().performClick()
    composeRule.waitForIdle()

    assertTrue(authSucceeded)
    assertEquals(0, composeRule.onAllNodesWithTag("otp_verification_dialog").fetchSemanticsNodes().size)
    val session = AccountSecurityOtpManager.observeAltSession(composeRule.activity).value
    assertNotNull(session)
    assertEquals(AuthMethodType.GUEST, session?.method)

    AccountSecurityOtpManager.clearAltSession(composeRule.activity)
    assertNull(AccountSecurityOtpManager.observeAltSession(composeRule.activity).value)
  }
}
