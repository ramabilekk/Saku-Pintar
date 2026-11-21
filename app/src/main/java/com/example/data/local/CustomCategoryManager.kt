package com.example.data.local

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.example.data.model.ExpenseCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class CustomCategory(
  val id: String,
  val name: String,
  val parentCategory: ExpenseCategory,
  val colorHex: Long,
) {
  val accentColor: Color
    get() = Color(colorHex)

  /** Tag inserted in expense notes to allow instant search/filtering while preserving Firestore schema. */
  val noteTag: String
    get() = "#${name.trim().replace(" ", "")}"
}

/**
 * Manages user-defined custom categories mapped to core ExpenseCategory groups so Auto-Budget
 * calculations and Firestore security rules remain 100% compatible.
 */
object CustomCategoryManager {
  private const val PREFS_NAME = "sakupintar_custom_categories"
  private const val KEY_LIST = "categories_json"
  private const val MAX_CUSTOM_CATEGORIES = 20

  val presetColorPalette: List<Long> = listOf(
    0xFF0EA5E9L, // Sky
    0xFF10B981L, // Emerald
    0xFF6366F1L, // Indigo
    0xFFEC4899L, // Pink
    0xFFF59E0BL, // Amber
    0xFF8B5CF6L, // Violet
    0xFFEF4444L, // Rose
    0xFF14B8A6L, // Teal
  )

  private val _categories = MutableStateFlow<List<CustomCategory>>(emptyList())
  @Volatile
  private var isLoaded = false

  fun observeCategories(context: Context): StateFlow<List<CustomCategory>> {
    ensureLoaded(context)
    return _categories.asStateFlow()
  }

  fun addCategory(
    context: Context,
    name: String,
    parentCategory: ExpenseCategory,
    colorHex: Long = presetColorPalette.first(),
  ): CustomCategory? {
    ensureLoaded(context)
    val cleanName = name.trim().take(24)
    if (cleanName.isEmpty()) return null
    val current = _categories.value
    if (current.any { it.name.equals(cleanName, ignoreCase = true) }) {
      return current.first { it.name.equals(cleanName, ignoreCase = true) }
    }
    if (current.size >= MAX_CUSTOM_CATEGORIES) return null
    val created = CustomCategory(
      id = UUID.randomUUID().toString(),
      name = cleanName,
      parentCategory = parentCategory,
      colorHex = colorHex,
    )
    val updated = current + created
    _categories.value = updated
    persist(context, updated)
    return created
  }

  fun deleteCategory(context: Context, categoryId: String) {
    ensureLoaded(context)
    val updated = _categories.value.filterNot { it.id == categoryId }
    _categories.value = updated
    persist(context, updated)
  }

  /** Appends the custom category tag into an expense note without duplicating it. */
  fun applyTagToNote(existingNote: String, customCategory: CustomCategory): String {
    val tag = customCategory.noteTag
    val trimmed = existingNote.trim()
    if (trimmed.contains(tag, ignoreCase = true)) return trimmed
    return if (trimmed.isEmpty()) tag else "$tag • $trimmed".take(200)
  }

  private fun ensureLoaded(context: Context) {
    if (isLoaded) return
    synchronized(this) {
      if (isLoaded) return
      val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      val raw = prefs.getString(KEY_LIST, null)
      val parsed = if (raw.isNullOrBlank()) {
        emptyList()
      } else {
        runCatching {
          val arr = JSONArray(raw)
          buildList {
            for (i in 0 until arr.length()) {
              val obj = arr.optJSONObject(i) ?: continue
              val id = obj.optString("id").ifBlank { UUID.randomUUID().toString() }
              val name = obj.optString("name").trim()
              if (name.isEmpty()) continue
              add(
                CustomCategory(
                  id = id,
                  name = name,
                  parentCategory = ExpenseCategory.fromKey(obj.optString("parentCategory")),
                  colorHex = obj.optLong("colorHex", presetColorPalette.first()),
                )
              )
            }
          }
        }.getOrDefault(emptyList())
      }
      _categories.value = parsed
      isLoaded = true
    }
  }

  private fun persist(context: Context, list: List<CustomCategory>) {
    val arr = JSONArray()
    list.forEach { cat ->
      arr.put(
        JSONObject().apply {
          put("id", cat.id)
          put("name", cat.name)
          put("parentCategory", cat.parentCategory.name)
          put("colorHex", cat.colorHex)
        }
      )
    }
    context.applicationContext
      .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putString(KEY_LIST, arr.toString())
      .apply()
  }
}
