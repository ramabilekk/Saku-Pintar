package com.example.ui.theme

import android.content.Context
import androidx.annotation.StringRes
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode(
  val labelId: String,
  @StringRes val labelRes: Int,
) {
  SYSTEM("Ikuti Perangkat", R.string.theme_mode_system),
  LIGHT("Terang", R.string.theme_mode_light),
  DARK("Gelap", R.string.theme_mode_dark);

  companion object {
    fun fromKey(key: String?): AppThemeMode =
      entries.firstOrNull { it.name == key } ?: SYSTEM
  }
}

object ThemePreferenceManager {
  private const val PREFS_NAME = "sakupintar_theme_prefs"
  private const val KEY_THEME_MODE = "theme_mode"

  private val _themeMode = MutableStateFlow(AppThemeMode.SYSTEM)
  private var initialized = false

  fun observeThemeMode(context: Context): StateFlow<AppThemeMode> {
    if (!initialized) {
      val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      _themeMode.value = AppThemeMode.fromKey(prefs.getString(KEY_THEME_MODE, null))
      initialized = true
    }
    return _themeMode.asStateFlow()
  }

  fun setThemeMode(context: Context, mode: AppThemeMode) {
    val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    initialized = true
    _themeMode.value = mode
  }
}
