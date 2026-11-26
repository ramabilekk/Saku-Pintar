package com.example.util

import com.example.data.model.Expense

/**
 * Utility for lightweight #hashtag Event/Project tags embedded cleanly inside [Expense.note].
 * Examples: #liburan, #mudik, #kerja, #renovasi, #proyek.
 */
object EventTagHelper {
  private val TAG_REGEX = Regex("#[\\p{L}\\p{N}_]{2,24}")

  val presetEventTags: List<String> = listOf(
    "#liburan",
    "#mudik",
    "#kerja",
    "#renovasi",
    "#acara",
    "#proyek",
  )

  /** Normalizes user input (with or without leading '#') into a clean lowercase #tag, or null if invalid. */
  fun normalizeTag(raw: String): String? {
    val cleaned = raw
      .trim()
      .removePrefix("#")
      .replace(Regex("[^\\p{L}\\p{N}_]"), "")
      .lowercase()
      .take(24)
    return if (cleaned.length >= 2) "#$cleaned" else null
  }

  /** Extracts distinct lowercase #tags from a note string. */
  fun extractTags(note: String): List<String> {
    if (note.isBlank() || !note.contains('#')) return emptyList()
    return TAG_REGEX.findAll(note)
      .map { it.value.lowercase() }
      .distinct()
      .toList()
  }

  /** Checks whether [note] contains [tag] (case-insensitive). */
  fun hasTag(note: String, tag: String): Boolean {
    val normalized = normalizeTag(tag) ?: return false
    return extractTags(note).any { it.equals(normalized, ignoreCase = true) }
  }

  /** Toggles [tag] inside [note], keeping total length safely within the 200-char Firestore note limit. */
  fun toggleTag(note: String, tag: String, maxNoteLength: Int = 200): String {
    val normalized = normalizeTag(tag) ?: return note
    return if (hasTag(note, normalized)) {
      val removeRegex = Regex("(?i)(^|\\s)${Regex.escape(normalized)}(?=\\s|$)")
      note.replace(removeRegex, " ").replace(Regex("\\s+"), " ").trim()
    } else {
      val trimmed = note.trim()
      val candidate = if (trimmed.isEmpty()) normalized else "$trimmed $normalized"
      if (candidate.length <= maxNoteLength) candidate else trimmed.take((maxNoteLength - normalized.length - 1).coerceAtLeast(0)).trim() + " " + normalized
    }
  }

  /**
   * Collects all distinct #event tags found across [expenses], excluding custom-category tags if supplied,
   * ordered by usage frequency descending.
   */
  fun collectAllTags(
    expenses: List<Expense>,
    excludedTags: Set<String> = emptySet(),
  ): List<Pair<String, Int>> {
    val excludedLower = excludedTags.map { it.lowercase() }.toSet()
    val counts = linkedMapOf<String, Int>()
    expenses.forEach { expense ->
      extractTags(expense.note).forEach { tag ->
        if (tag !in excludedLower) {
          counts[tag] = (counts[tag] ?: 0) + 1
        }
      }
    }
    return counts.entries
      .sortedByDescending { it.value }
      .map { it.key to it.value }
  }
}
