package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.util.ImageUtils

@Composable
fun ReceiptThumbnailBadge(
  receiptPhotoBase64: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val bitmap = remember(receiptPhotoBase64) {
    ImageUtils.decodeAvatarBitmap(receiptPhotoBase64)
  }

  Surface(
    shape = RoundedCornerShape(10.dp),
    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
    modifier = modifier
      .clip(RoundedCornerShape(10.dp))
      .clickable(onClick = onClick),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      if (bitmap != null) {
        Image(
          bitmap = bitmap.asImageBitmap(),
          contentDescription = stringResource(R.string.receipt_view_full),
          contentScale = ContentScale.Crop,
          modifier = Modifier
            .size(20.dp)
            .clip(RoundedCornerShape(5.dp)),
        )
      } else {
        Icon(
          imageVector = Icons.Rounded.Receipt,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(14.dp),
        )
      }
      Text(
        text = stringResource(R.string.receipt_badge_short),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
      )
      Icon(
        imageVector = Icons.Rounded.ZoomIn,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
        modifier = Modifier.size(13.dp),
      )
    }
  }
}

@Composable
fun ReceiptPhotoViewerDialog(
  receiptPhotoBase64: String,
  title: String,
  subtitle: String,
  amountFormatted: String,
  onDismiss: () -> Unit,
) {
  val bitmap = remember(receiptPhotoBase64) {
    ImageUtils.decodeAvatarBitmap(receiptPhotoBase64)
  }
  var scale by remember { mutableFloatStateOf(1f) }
  var offset by remember { mutableStateOf(Offset.Zero) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.82f))
        .padding(16.dp),
      contentAlignment = Alignment.Center,
    ) {
      Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier
          .widthIn(max = 520.dp)
          .fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.primaryContainer,
              modifier = Modifier.size(40.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Rounded.Receipt,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp),
                )
              }
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = title.ifBlank { stringResource(R.string.receipt_viewer_title) },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              if (subtitle.isNotBlank()) {
                Text(
                  text = subtitle,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
            if (amountFormatted.isNotBlank()) {
              Text(
                text = amountFormatted,
                style = MaterialTheme.typography.titleSmall,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
              )
            }
            IconButton(
              onClick = onDismiss,
              modifier = Modifier.size(40.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = stringResource(R.string.action_close),
              )
            }
          }

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(min = 260.dp, max = 460.dp)
              .clip(RoundedCornerShape(16.dp))
              .background(MaterialTheme.colorScheme.surfaceContainerLow)
              .pointerInput(Unit) {
                detectTapGestures(
                  onDoubleTap = {
                    if (scale > 1.1f) {
                      scale = 1f
                      offset = Offset.Zero
                    } else {
                      scale = 2.2f
                    }
                  }
                )
              }
              .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                  val newScale = (scale * zoom).coerceIn(1f, 3.5f)
                  scale = newScale
                  offset = if (newScale > 1f) {
                    Offset(
                      x = (offset.x + pan.x).coerceIn(-360f, 360f),
                      y = (offset.y + pan.y).coerceIn(-480f, 480f),
                    )
                  } else {
                    Offset.Zero
                  }
                }
              },
            contentAlignment = Alignment.Center,
          ) {
            if (bitmap != null) {
              Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = stringResource(R.string.receipt_viewer_title),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                  .fillMaxWidth()
                  .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y,
                  ),
              )
            } else {
              Spacer(modifier = Modifier.height(200.dp))
            }
          }
        }
      }
    }
  }
}
