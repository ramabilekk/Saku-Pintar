package com.example.security

import android.app.KeyguardManager
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest

/**
 * Centralized preferences & security manager for:
 * - Privacy Eye mode (masking all currency figures on screen)
 * - App Lock (4-digit salted SHA-256 PIN + Android Device Credential / Biometric fallback)
 * - Auto-deduct wallet balance when recording expenses
 * - Starter onboarding checklist dismissal
 */
object PrivacySecurityManager {
  private const val PREFS_NAME = "sakupintar_privacy_security"
  private const val KEY_HIDE_BALANCES = "hide_balances"
  private const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
  private const val KEY_PIN_HASH = "pin_hash"
  private const val KEY_AUTO_DEDUCT_WALLET = "auto_deduct_wallet"
  private const val KEY_ONBOARDING_DISMISSED = "onboarding_dismissed"
  private const val KEY_HAPTIC_FEEDBACK_ENABLED = "haptic_feedback_enabled"
  private const val KEY_BUDGET_ALERTS_ENABLED = "budget_alerts_enabled"
  private const val PIN_SALT = "sakupintar_v2_pin_salt_2026"

  private val _hideBalances = MutableStateFlow(false)
  private val _appLockEnabled = MutableStateFlow(false)
  private val _autoDeductWallet = MutableStateFlow(true)
  private val _onboardingDismissed = MutableStateFlow(false)
  private val _hapticFeedbackEnabled = MutableStateFlow(true)
  private val _budgetAlertsEnabled = MutableStateFlow(true)

  @Volatile
  private var isLoaded = false

  fun observeHideBalances(context: Context): StateFlow<Boolean> {
    ensureLoaded(context)
    return _hideBalances.asStateFlow()
  }

  fun observeAppLockEnabled(context: Context): StateFlow<Boolean> {
    ensureLoaded(context)
    return _appLockEnabled.asStateFlow()
  }

  fun observeAutoDeductWallet(context: Context): StateFlow<Boolean> {
    ensureLoaded(context)
    return _autoDeductWallet.asStateFlow()
  }

  fun observeOnboardingDismissed(context: Context): StateFlow<Boolean> {
    ensureLoaded(context)
    return _onboardingDismissed.asStateFlow()
  }

  fun observeHapticFeedbackEnabled(context: Context): StateFlow<Boolean> {
    ensureLoaded(context)
    return _hapticFeedbackEnabled.asStateFlow()
  }

  fun observeBudgetAlertsEnabled(context: Context): StateFlow<Boolean> {
    ensureLoaded(context)
    return _budgetAlertsEnabled.asStateFlow()
  }

  fun isHideBalances(context: Context): Boolean {
    ensureLoaded(context)
    return _hideBalances.value
  }

  fun isAppLockEnabled(context: Context): Boolean {
    ensureLoaded(context)
    return _appLockEnabled.value
  }

  fun isAutoDeductWalletEnabled(context: Context): Boolean {
    ensureLoaded(context)
    return _autoDeductWallet.value
  }

  fun isHapticFeedbackEnabled(context: Context): Boolean {
    ensureLoaded(context)
    return _hapticFeedbackEnabled.value
  }

  fun isBudgetAlertsEnabled(context: Context): Boolean {
    ensureLoaded(context)
    return _budgetAlertsEnabled.value
  }

  fun toggleHideBalances(context: Context): Boolean {
    ensureLoaded(context)
    val next = !_hideBalances.value
    setHideBalances(context, next)
    return next
  }

  fun setHideBalances(context: Context, hide: Boolean) {
    ensureLoaded(context)
    _hideBalances.value = hide
    prefs(context).edit().putBoolean(KEY_HIDE_BALANCES, hide).apply()
  }

  fun setAutoDeductWallet(context: Context, enabled: Boolean) {
    ensureLoaded(context)
    _autoDeductWallet.value = enabled
    prefs(context).edit().putBoolean(KEY_AUTO_DEDUCT_WALLET, enabled).apply()
  }

  fun setHapticFeedbackEnabled(context: Context, enabled: Boolean) {
    ensureLoaded(context)
    _hapticFeedbackEnabled.value = enabled
    prefs(context).edit().putBoolean(KEY_HAPTIC_FEEDBACK_ENABLED, enabled).apply()
  }

  fun setBudgetAlertsEnabled(context: Context, enabled: Boolean) {
    ensureLoaded(context)
    _budgetAlertsEnabled.value = enabled
    prefs(context).edit().putBoolean(KEY_BUDGET_ALERTS_ENABLED, enabled).apply()
  }

  fun dismissOnboarding(context: Context) {
    ensureLoaded(context)
    _onboardingDismissed.value = true
    prefs(context).edit().putBoolean(KEY_ONBOARDING_DISMISSED, true).apply()
  }

  fun hasPinConfigured(context: Context): Boolean {
    ensureLoaded(context)
    return !prefs(context).getString(KEY_PIN_HASH, null).isNullOrBlank()
  }

  fun savePinAndEnableLock(context: Context, pin: String): Boolean {
    val clean = pin.filter { it.isDigit() }
    if (clean.length != 4) return false
    ensureLoaded(context)
    val hashed = sha256("$PIN_SALT:$clean")
    prefs(context).edit()
      .putString(KEY_PIN_HASH, hashed)
      .putBoolean(KEY_APP_LOCK_ENABLED, true)
      .apply()
    _appLockEnabled.value = true
    return true
  }

  fun verifyPin(context: Context, pin: String): Boolean {
    ensureLoaded(context)
    val stored = prefs(context).getString(KEY_PIN_HASH, null) ?: return false
    val clean = pin.filter { it.isDigit() }
    if (clean.length != 4) return false
    return stored == sha256("$PIN_SALT:$clean")
  }

  fun setAppLockEnabled(context: Context, enabled: Boolean) {
    ensureLoaded(context)
    _appLockEnabled.value = enabled
    prefs(context).edit().putBoolean(KEY_APP_LOCK_ENABLED, enabled).apply()
  }

  fun isDeviceCredentialAvailable(context: Context): Boolean {
    val km = context.applicationContext.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
    return km?.isDeviceSecure == true
  }

  private fun prefs(context: Context) =
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  private fun ensureLoaded(context: Context) {
    if (isLoaded) return
    synchronized(this) {
      if (isLoaded) return
      val p = prefs(context)
      _hideBalances.value = p.getBoolean(KEY_HIDE_BALANCES, false)
      _appLockEnabled.value = p.getBoolean(KEY_APP_LOCK_ENABLED, false)
      _autoDeductWallet.value = p.getBoolean(KEY_AUTO_DEDUCT_WALLET, true)
      _onboardingDismissed.value = p.getBoolean(KEY_ONBOARDING_DISMISSED, false)
      _hapticFeedbackEnabled.value = p.getBoolean(KEY_HAPTIC_FEEDBACK_ENABLED, true)
      _budgetAlertsEnabled.value = p.getBoolean(KEY_BUDGET_ALERTS_ENABLED, true)
      isLoaded = true
    }
  }

  private fun sha256(value: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
  }
}
