package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object ImageUtils {

  suspend fun decodeScaledBitmap(
    context: Context,
    uri: Uri,
    maxDimensionPx: Int = 1280,
  ): Bitmap = withContext(Dispatchers.IO) {
    val rawBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      val source = ImageDecoder.createSource(context.contentResolver, uri)
      ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        val size = info.size
        val longest = max(size.width, size.height)
        if (longest > maxDimensionPx) {
          val scale = maxDimensionPx.toFloat() / longest.toFloat()
          decoder.setTargetSize(
            (size.width * scale).roundToInt().coerceAtLeast(1),
            (size.height * scale).roundToInt().coerceAtLeast(1),
          )
        }
        decoder.isMutableRequired = false
      }
    } else {
      val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, boundsOptions)
      }
      var sampleSize = 1
      val longest = max(boundsOptions.outWidth, boundsOptions.outHeight)
      while (longest / sampleSize > maxDimensionPx * 2) {
        sampleSize *= 2
      }
      val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
      val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, decodeOptions)
      } ?: error("Gagal membaca gambar dari galeri.")
      val decodedLongest = max(decoded.width, decoded.height)
      if (decodedLongest > maxDimensionPx) {
        val scale = maxDimensionPx.toFloat() / decodedLongest.toFloat()
        Bitmap.createScaledBitmap(
          decoded,
          (decoded.width * scale).roundToInt().coerceAtLeast(1),
          (decoded.height * scale).roundToInt().coerceAtLeast(1),
          true,
        )
      } else {
        decoded
      }
    }
    rawBitmap
  }

  suspend fun encodeAvatarBase64(
    context: Context,
    uri: Uri,
    targetSizePx: Int = 256,
  ): String = withContext(Dispatchers.IO) {
    val decoded = decodeScaledBitmap(context, uri, maxDimensionPx = 512)
    encodeAvatarBitmapBase64(decoded, targetSizePx)
  }

  fun encodeAvatarBitmapBase64(
    bitmap: Bitmap,
    targetSizePx: Int = 256,
  ): String {
    val softwareBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bitmap.config == Bitmap.Config.HARDWARE) {
      bitmap.copy(Bitmap.Config.ARGB_8888, false)
    } else {
      bitmap
    }
    val squareSize = min(softwareBitmap.width, softwareBitmap.height).coerceAtLeast(1)
    val offsetX = ((softwareBitmap.width - squareSize) / 2).coerceAtLeast(0)
    val offsetY = ((softwareBitmap.height - squareSize) / 2).coerceAtLeast(0)
    val cropped = Bitmap.createBitmap(softwareBitmap, offsetX, offsetY, squareSize, squareSize)
    val avatar = if (cropped.width != targetSizePx) {
      Bitmap.createScaledBitmap(cropped, targetSizePx, targetSizePx, true)
    } else {
      cropped
    }

    val output = ByteArrayOutputStream()
    avatar.compress(Bitmap.CompressFormat.JPEG, 78, output)
    if (output.size() > 95_000) {
      output.reset()
      avatar.compress(Bitmap.CompressFormat.JPEG, 55, output)
    }
    val encoded = Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    require(encoded.length <= 145_000) { "Ukuran foto profil terlalu besar." }
    return encoded
  }

  fun decodeAvatarBitmap(base64: String): Bitmap? {
    if (base64.isBlank()) return null
    return runCatching {
      val bytes = Base64.decode(base64, Base64.DEFAULT)
      BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()
  }

  suspend fun encodeReceiptBase64(
    context: Context,
    uri: Uri,
    maxDimensionPx: Int = 820,
  ): String = withContext(Dispatchers.IO) {
    val decoded = decodeScaledBitmap(context, uri, maxDimensionPx = maxDimensionPx)
    encodeReceiptBitmapBase64(decoded, maxDimensionPx)
  }

  fun encodeReceiptBitmapBase64(
    bitmap: Bitmap,
    maxDimensionPx: Int = 820,
  ): String {
    val softwareBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bitmap.config == Bitmap.Config.HARDWARE) {
      bitmap.copy(Bitmap.Config.ARGB_8888, false)
    } else {
      bitmap
    }
    val longest = max(softwareBitmap.width, softwareBitmap.height).coerceAtLeast(1)
    val scaled = if (longest > maxDimensionPx) {
      val scale = maxDimensionPx.toFloat() / longest.toFloat()
      Bitmap.createScaledBitmap(
        softwareBitmap,
        (softwareBitmap.width * scale).roundToInt().coerceAtLeast(1),
        (softwareBitmap.height * scale).roundToInt().coerceAtLeast(1),
        true,
      )
    } else {
      softwareBitmap
    }

    val output = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, 76, output)
    if (output.size() > 130_000) {
      output.reset()
      scaled.compress(Bitmap.CompressFormat.JPEG, 56, output)
    }
    if (output.size() > 130_000) {
      output.reset()
      val smaller = Bitmap.createScaledBitmap(
        scaled,
        (scaled.width * 0.72f).roundToInt().coerceAtLeast(1),
        (scaled.height * 0.72f).roundToInt().coerceAtLeast(1),
        true,
      )
      smaller.compress(Bitmap.CompressFormat.JPEG, 52, output)
    }
    val encoded = Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    require(encoded.length <= 195_000) { "Ukuran foto struk terlalu besar." }
    return encoded
  }
}
