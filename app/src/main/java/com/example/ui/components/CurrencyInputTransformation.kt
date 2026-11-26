package com.example.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.example.i18n.AppCurrency
import java.text.DecimalFormatSymbols

/**
 * Formats digit-only numeric input with thousand separators (e.g. `8000000` -> `8.000.000` for IDR)
 * while keeping underlying state as raw digits and maintaining exact cursor offset mapping.
 */
class ThousandSeparatorVisualTransformation(
  private val separator: Char = '.',
) : VisualTransformation {

  override fun filter(text: AnnotatedString): TransformedText {
    val raw = text.text
    if (raw.isEmpty()) {
      return TransformedText(AnnotatedString(""), OffsetMapping.Identity)
    }

    val n = raw.length
    val separatorCount = (n - 1) / 3
    val formattedLength = n + separatorCount
    val sb = StringBuilder(formattedLength)
    val origToTrans = IntArray(n + 1)
    val transToOrig = IntArray(formattedLength + 1)

    origToTrans[0] = 0
    transToOrig[0] = 0

    var t = 0
    for (i in 0 until n) {
      sb.append(raw[i])
      t++
      origToTrans[i + 1] = t
      transToOrig[t] = i + 1

      val remaining = n - 1 - i
      if (remaining > 0 && remaining % 3 == 0) {
        sb.append(separator)
        t++
        transToOrig[t] = i + 1
      }
    }

    val formatted = sb.toString()
    val offsetMapping = object : OffsetMapping {
      override fun originalToTransformed(offset: Int): Int =
        origToTrans[offset.coerceIn(0, n)]

      override fun transformedToOriginal(offset: Int): Int =
        transToOrig[offset.coerceIn(0, formatted.length)]
    }

    return TransformedText(AnnotatedString(formatted), offsetMapping)
  }

  companion object {
    fun groupingSeparatorFor(currency: AppCurrency): Char = when (currency) {
      AppCurrency.IDR -> '.'
      else -> {
        val sym = DecimalFormatSymbols.getInstance(currency.locale).groupingSeparator
        if (sym.isWhitespace()) '.' else sym
      }
    }
  }
}
