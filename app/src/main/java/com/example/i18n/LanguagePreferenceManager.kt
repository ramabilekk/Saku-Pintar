package com.example.i18n

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.view.View
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class AppLanguage(
  val code: String,
  val nativeName: String,
  val shortCode: String,
  val isUnOfficial: Boolean,
  val isRtl: Boolean = false,
) {
  SYSTEM(
    code = "system",
    nativeName = "Auto",
    shortCode = "AUTO",
    isUnOfficial = false,
  ),
  INDONESIAN(
    code = "id",
    nativeName = "Bahasa Indonesia",
    shortCode = "ID",
    isUnOfficial = false,
  ),
  ENGLISH(
    code = "en",
    nativeName = "English",
    shortCode = "EN",
    isUnOfficial = true,
  ),
  ARABIC(
    code = "ar",
    nativeName = "العربية",
    shortCode = "AR",
    isUnOfficial = true,
    isRtl = true,
  ),
  CHINESE(
    code = "zh-CN",
    nativeName = "中文 (简体)",
    shortCode = "ZH",
    isUnOfficial = true,
  ),
  FRENCH(
    code = "fr",
    nativeName = "Français",
    shortCode = "FR",
    isUnOfficial = true,
  ),
  RUSSIAN(
    code = "ru",
    nativeName = "Русский",
    shortCode = "RU",
    isUnOfficial = true,
  ),
  SPANISH(
    code = "es",
    nativeName = "Español",
    shortCode = "ES",
    isUnOfficial = true,
  );

  fun resolveEffectiveLanguage(systemLocale: Locale = defaultSystemLocale()): AppLanguage {
    if (this != SYSTEM) return this
    val lang = systemLocale.language.lowercase(Locale.ROOT)
    return when (lang) {
      "in", "id" -> INDONESIAN
      "ar" -> ARABIC
      "zh" -> CHINESE
      "fr" -> FRENCH
      "ru" -> RUSSIAN
      "es" -> SPANISH
      else -> ENGLISH
    }
  }

  fun toLocale(systemLocale: Locale = defaultSystemLocale()): Locale {
    return when (resolveEffectiveLanguage(systemLocale)) {
      INDONESIAN -> Locale("in", "ID")
      ENGLISH -> Locale("en", "US")
      ARABIC -> Locale("ar", "SA")
      CHINESE -> Locale.SIMPLIFIED_CHINESE
      FRENCH -> Locale("fr", "FR")
      RUSSIAN -> Locale("ru", "RU")
      SPANISH -> Locale("es", "ES")
      SYSTEM -> Locale("en", "US")
    }
  }

  fun aiLanguageInstructionName(systemLocale: Locale = defaultSystemLocale()): String {
    return when (resolveEffectiveLanguage(systemLocale)) {
      INDONESIAN -> "Indonesian (Bahasa Indonesia)"
      ENGLISH -> "English"
      ARABIC -> "Arabic"
      CHINESE -> "Simplified Chinese"
      FRENCH -> "French"
      RUSSIAN -> "Russian"
      SPANISH -> "Spanish"
      SYSTEM -> "English"
    }
  }

  companion object {
    private val initialDeviceLocale: Locale = Locale.getDefault()

    fun defaultSystemLocale(): Locale = initialDeviceLocale

    fun fromCode(code: String?): AppLanguage =
      entries.firstOrNull { it.code == code || it.name == code } ?: INDONESIAN
  }
}

object LanguagePreferenceManager {
  private const val PREFS_NAME = "sakupintar_locale_prefs"
  private const val KEY_LANGUAGE = "app_language"

  private val _language = MutableStateFlow(AppLanguage.INDONESIAN)
  @Volatile
  private var initialized = false

  val currentLanguage: AppLanguage
    get() = _language.value

  fun currentLocale(): Locale = _language.value.toLocale()

  fun observeLanguage(context: Context): StateFlow<AppLanguage> {
    if (!initialized) {
      val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      val saved = prefs.getString(KEY_LANGUAGE, null)
      val resolved = if (saved == null) AppLanguage.SYSTEM else AppLanguage.fromCode(saved)
      _language.value = resolved
      Locale.setDefault(resolved.toLocale())
      initialized = true
    }
    return _language.asStateFlow()
  }

  fun setLanguage(context: Context, language: AppLanguage) {
    val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putString(KEY_LANGUAGE, language.code).apply()
    initialized = true
    _language.value = language
    Locale.setDefault(language.toLocale())
  }

  fun localizedContext(context: Context, language: AppLanguage = observeLanguage(context).value): Context {
    val locale = language.toLocale()
    val config = Configuration(context.resources.configuration)
    config.setLocale(locale)
    config.setLayoutDirection(locale)
    return context.createConfigurationContext(config)
  }

  fun isRtl(language: AppLanguage = _language.value): Boolean {
    val effective = language.resolveEffectiveLanguage()
    if (effective.isRtl) return true
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
      android.text.TextUtils.getLayoutDirectionFromLocale(language.toLocale()) == View.LAYOUT_DIRECTION_RTL
    } else {
      false
    }
  }
}
