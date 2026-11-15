package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.util.ImageUtils

@Composable
fun ProfileAvatar(
  displayName: String,
  customPhotoBase64: String?,
  googlePhotoUrl: String?,
  size: Dp,
  modifier: Modifier = Modifier,
) {
  val customBitmap = remember(customPhotoBase64) {
    customPhotoBase64?.takeIf { it.isNotBlank() }?.let { ImageUtils.decodeAvatarBitmap(it) }
  }
  val borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
  val avatarModifier = modifier
    .size(size)
    .clip(CircleShape)
    .border(1.5.dp, borderColor, CircleShape)

  when {
    customBitmap != null -> {
      Image(
        bitmap = customBitmap.asImageBitmap(),
        contentDescription = "Foto profil $displayName",
        contentScale = ContentScale.Crop,
        modifier = avatarModifier,
      )
    }

    !googlePhotoUrl.isNullOrBlank() -> {
      val context = LocalContext.current
      val request = remember(googlePhotoUrl) {
        ImageRequest.Builder(context)
          .data(googlePhotoUrl)
          .crossfade(true)
          .build()
      }
      SubcomposeAsyncImage(
        model = request,
        contentDescription = "Foto profil $displayName",
        contentScale = ContentScale.Crop,
        modifier = avatarModifier,
        loading = { MonogramAvatar(displayName = displayName, size = size) },
        error = { MonogramAvatar(displayName = displayName, size = size) },
      )
    }

    else -> {
      MonogramAvatar(
        displayName = displayName,
        size = size,
        modifier = avatarModifier,
      )
    }
  }
}

@Composable
private fun MonogramAvatar(
  displayName: String,
  size: Dp,
  modifier: Modifier = Modifier,
) {
  val initialChar = displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "S"
  val textStyle = when {
    size >= 56.dp -> MaterialTheme.typography.headlineSmall
    size >= 40.dp -> MaterialTheme.typography.titleMedium
    else -> MaterialTheme.typography.labelLarge
  }
  Surface(
    shape = CircleShape,
    color = MaterialTheme.colorScheme.primaryContainer,
    modifier = modifier.size(size),
  ) {
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = initialChar,
        style = textStyle,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
      )
    }
  }
}
