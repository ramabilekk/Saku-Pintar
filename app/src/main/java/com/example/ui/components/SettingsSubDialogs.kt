package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.Coil
import com.example.R
import com.example.ui.theme.CoralDanger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

internal object AppCacheManager {
  suspend fun calculateCacheSizeBytes(context: Context): Long = withContext(Dispatchers.IO) {
    val internal = directorySizeBytes(context.cacheDir)
    val external = context.externalCacheDir?.let { directorySizeBytes(it) } ?: 0L
    internal + external
  }

  suspend fun clearAppCache(context: Context): Long = withContext(Dispatchers.IO) {
    val before = calculateCacheSizeBytes(context)
    runCatching { Coil.imageLoader(context.applicationContext).memoryCache?.clear() }
    runCatching { deleteChildren(context.cacheDir) }
    runCatching { context.externalCacheDir?.let { deleteChildren(it) } }
    before
  }

  fun formatCacheSize(bytes: Long): String {
    if (bytes <= 0L) return "0 KB"
    val kb = bytes / 1024.0
    if (kb < 1024.0) {
      return String.format(Locale.getDefault(), "%.0f KB", kb.coerceAtLeast(1.0))
    }
    val mb = kb / 1024.0
    return String.format(Locale.getDefault(), "%.1f MB", mb)
  }

  private fun directorySizeBytes(dir: File?): Long {
    if (dir == null || !dir.exists()) return 0L
    var total = 0L
    val children = dir.listFiles() ?: return 0L
    for (file in children) {
      total += if (file.isDirectory) directorySizeBytes(file) else file.length()
    }
    return total
  }

  private fun deleteChildren(dir: File?) {
    if (dir == null || !dir.exists()) return
    val children = dir.listFiles() ?: return
    for (file in children) {
      runCatching {
        if (file.isDirectory) {
          file.deleteRecursively()
        } else {
          file.delete()
        }
      }
    }
  }
}

internal fun launchSupportEmailOrCopy(
  context: Context,
  email: String,
  subject: String,
  onCopiedMessage: (String) -> Unit,
) {
  val intent = Intent(Intent.ACTION_SENDTO).apply {
    data = Uri.parse("mailto:$email")
    putExtra(Intent.EXTRA_SUBJECT, subject)
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
  }
  val launched = runCatching {
    context.startActivity(intent)
    true
  }.getOrDefault(false)

  if (!launched) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("support_email", email))
    onCopiedMessage(context.getString(R.string.settings_contact_copied_status, email))
  }
}

@Composable
internal fun EditProfileQuickDialog(
  currentDisplayName: String,
  isSavingProfile: Boolean,
  onSaveDisplayName: (String) -> Unit,
  onChangePhotoClick: () -> Unit,
  onDismiss: () -> Unit,
) {
  var nameDraft by remember(currentDisplayName) { mutableStateOf(currentDisplayName) }
  val trimmed = nameDraft.trim()

  LiquidGlassAlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = stringResource(R.string.settings_edit_profile_dialog_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        OutlinedTextField(
          value = nameDraft,
          onValueChange = { nameDraft = it.take(50) },
          label = { Text(stringResource(R.string.profile_display_name_label)) },
          placeholder = { Text(stringResource(R.string.profile_display_name_hint)) },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("quick_edit_profile_name_input"),
        )
        OutlinedButton(
          onClick = {
            onDismiss()
            onChangePhotoClick()
          },
          enabled = !isSavingProfile,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("quick_edit_profile_photo_button"),
        ) {
          Icon(
            imageVector = Icons.Outlined.PhotoCamera,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = stringResource(R.string.settings_quick_change_photo),
            fontWeight = FontWeight.SemiBold,
          )
        }
      }
    },
    confirmButton = {
      FilledTonalButton(
        onClick = {
          onSaveDisplayName(trimmed)
          onDismiss()
        },
        enabled = !isSavingProfile && trimmed.isNotEmpty() && trimmed != currentDisplayName,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.testTag("quick_edit_profile_save_button"),
      ) {
        Icon(
          imageVector = Icons.Rounded.Save,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(stringResource(R.string.action_save))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.action_cancel))
      }
    },
  )
}

@Composable
internal fun ClearCacheConfirmDialog(
  formattedSize: String,
  onConfirmClear: () -> Unit,
  onDismiss: () -> Unit,
) {
  LiquidGlassAlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = stringResource(R.string.settings_cache_dialog_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Text(
        text = stringResource(R.string.settings_cache_dialog_body, formattedSize),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    },
    confirmButton = {
      FilledTonalButton(
        onClick = onConfirmClear,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.testTag("confirm_clear_cache_button"),
      ) {
        Text(
          text = stringResource(R.string.settings_cache_clear_action),
          fontWeight = FontWeight.SemiBold,
        )
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.action_cancel))
      }
    },
  )
}

@Composable
internal fun SignOutConfirmDialog(
  isGuestMode: Boolean,
  onConfirmSignOut: () -> Unit,
  onDismiss: () -> Unit,
) {
  LiquidGlassAlertDialog(
    onDismissRequest = onDismiss,
    accentColor = CoralDanger,
    title = {
      Text(
        text = stringResource(R.string.settings_sign_out_confirm_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Text(
        text = stringResource(
          if (isGuestMode) {
            R.string.settings_sign_out_confirm_body_guest
          } else {
            R.string.settings_sign_out_confirm_body_google
          }
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    },
    confirmButton = {
      Button(
        onClick = onConfirmSignOut,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = CoralDanger,
          contentColor = Color.White,
        ),
        modifier = Modifier.testTag("confirm_sign_out_button"),
      ) {
        Text(
          text = stringResource(R.string.action_sign_out),
          fontWeight = FontWeight.Bold,
        )
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.action_cancel))
      }
    },
  )
}

@Composable
internal fun DeleteAccountConfirmDialog(
  isGuestMode: Boolean,
  isDeleting: Boolean,
  onConfirmDelete: () -> Unit,
  onDismiss: () -> Unit,
) {
  var understoodChecked by remember { mutableStateOf(false) }

  LiquidGlassAlertDialog(
    onDismissRequest = { if (!isDeleting) onDismiss() },
    accentColor = CoralDanger,
    icon = {
      Icon(
        imageVector = Icons.Rounded.DeleteForever,
        contentDescription = null,
        tint = CoralDanger,
        modifier = Modifier.size(28.dp),
      )
    },
    title = {
      Text(
        text = stringResource(R.string.settings_delete_account_dialog_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = CoralDanger,
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text = stringResource(
            if (isGuestMode) {
              R.string.settings_delete_account_dialog_body_guest
            } else {
              R.string.settings_delete_account_dialog_body_google
            }
          ),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isDeleting) { understoodChecked = !understoodChecked }
            .padding(vertical = 4.dp)
            .testTag("delete_account_confirm_checkbox"),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Checkbox(
            checked = understoodChecked,
            onCheckedChange = { if (!isDeleting) understoodChecked = it },
          )
          Text(
            text = stringResource(R.string.settings_delete_account_checkbox),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onConfirmDelete,
        enabled = understoodChecked && !isDeleting,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = CoralDanger,
          contentColor = Color.White,
        ),
        modifier = Modifier.testTag("confirm_delete_account_button"),
      ) {
        if (isDeleting) {
          CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
            color = Color.White,
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(stringResource(R.string.settings_delete_account_in_progress))
        } else {
          Text(
            text = stringResource(R.string.settings_delete_account_confirm),
            fontWeight = FontWeight.Bold,
          )
        }
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        enabled = !isDeleting,
      ) {
        Text(stringResource(R.string.action_cancel))
      }
    },
  )
}

@Composable
internal fun LegalInfoDialog(
  title: String,
  body: String,
  onDismiss: () -> Unit,
) {
  LiquidGlassAlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 380.dp)
          .verticalScroll(rememberScrollState()),
      ) {
        Text(
          text = body,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
        )
      }
    },
    confirmButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("legal_dialog_close_button"),
      ) {
        Text(
          text = stringResource(R.string.action_close),
          fontWeight = FontWeight.SemiBold,
        )
      }
    },
  )
}
