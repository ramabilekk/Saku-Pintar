package com.example.auth

import android.content.Context
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

enum class AuthMethodType {
  GOOGLE,
  GUEST,
}

data class AltAuthSession(
  val userId: String,
  val method: AuthMethodType,
  val displayName: String,
  val identifierLabel: String,
  val customPhotoBase64: String? = null,
)

object AccountSecurityOtpManager {
  private const val PREFS_NAME = "sakupintar_account_security_prefs"
  private const val KEY_ACTIVE_ALT_METHOD = "active_alt_method"
  private const val KEY_ACTIVE_ALT_UID = "active_alt_uid"
  private const val KEY_ACTIVE_ALT_NAME = "active_alt_name"
  private const val KEY_ACTIVE_ALT_LABEL = "active_alt_label"
  private const val KEY_ACTIVE_ALT_PHOTO = "active_alt_photo"

  @Volatile
  private var initialized = false

  private val _altSessionFlow = MutableStateFlow<AltAuthSession?>(null)

  fun observeAltSession(context: Context): StateFlow<AltAuthSession?> {
    ensureInitialized(context)
    return _altSessionFlow.asStateFlow()
  }

  private fun ensureInitialized(context: Context) {
    if (initialized) return
    synchronized(this) {
      if (initialized) return
      // Clear any legacy Phone/Email/OTP keys if present; Guest visitor sessions are in-memory only
      context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .remove(KEY_ACTIVE_ALT_METHOD)
        .remove(KEY_ACTIVE_ALT_UID)
        .remove(KEY_ACTIVE_ALT_NAME)
        .remove(KEY_ACTIVE_ALT_LABEL)
        .remove(KEY_ACTIVE_ALT_PHOTO)
        .apply()
      initialized = true
    }
  }

  /**
   * Starts a temporary Guest Visitor session immediately without backing up
   * or saving data to persistent storage.
   */
  fun startGuestVisitorSession(
    context: Context,
    customDisplayName: String? = null,
  ) {
    ensureInitialized(context)
    context.applicationContext
      .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .remove(KEY_ACTIVE_ALT_METHOD)
      .remove(KEY_ACTIVE_ALT_UID)
      .remove(KEY_ACTIVE_ALT_NAME)
      .remove(KEY_ACTIVE_ALT_LABEL)
      .remove(KEY_ACTIVE_ALT_PHOTO)
      .apply()

    val resolvedName = customDisplayName
      ?.trim()
      ?.takeIf { it.isNotEmpty() }
      ?: context.getString(R.string.guest_account_display_name)

    _altSessionFlow.value = AltAuthSession(
      userId = "guest_visitor_${UUID.randomUUID()}",
      method = AuthMethodType.GUEST,
      displayName = resolvedName,
      identifierLabel = context.getString(R.string.guest_account_badge),
      customPhotoBase64 = null,
    )
  }

  fun updateAltSessionProfile(
    context: Context,
    displayName: String? = null,
    customPhotoBase64: String? = null,
  ) {
    ensureInitialized(context)
    val current = _altSessionFlow.value ?: return
    val cleanNewName = displayName?.trim()?.takeIf { it.isNotEmpty() }
    val updated = current.copy(
      displayName = cleanNewName ?: current.displayName,
      customPhotoBase64 = customPhotoBase64 ?: current.customPhotoBase64,
    )
    _altSessionFlow.value = updated
  }

  fun clearAltSession(context: Context) {
    ensureInitialized(context)
    context.applicationContext
      .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .remove(KEY_ACTIVE_ALT_METHOD)
      .remove(KEY_ACTIVE_ALT_UID)
      .remove(KEY_ACTIVE_ALT_NAME)
      .remove(KEY_ACTIVE_ALT_LABEL)
      .remove(KEY_ACTIVE_ALT_PHOTO)
      .apply()
    _altSessionFlow.value = null
  }
}
