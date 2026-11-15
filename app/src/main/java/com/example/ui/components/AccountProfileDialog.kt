package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.i18n.AppCurrency
import com.example.i18n.AppLanguage
import com.example.i18n.CurrencyPreferenceManager
import com.example.i18n.LanguagePreferenceManager
import com.example.notifications.DailyReminderManager
import com.example.notifications.ReminderSettings
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.ThemePreferenceManager

@Composable
fun AccountProfileDialog(
  displayName: String,
  email: String?,
  customPhotoBase64: String?,
  googlePhotoUrl: String?,
  isSavingProfile: Boolean,
  themeMode: AppThemeMode,
  reminderSettings: ReminderSettings,
  onReminderSettingsChange: (ReminderSettings) -> Unit,
  onSaveDisplayName: (String) -> Unit,
  onPickPhotoUri: (Uri) -> Unit,
  onSyncGooglePhoto: () -> Unit,
  onStatusMessage: (String) -> Unit,
  onSignOut: () -> Unit,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  var draftName by remember(displayName) { mutableStateOf(displayName) }
  var showLanguagePicker by remember { mutableStateOf(false) }

  val activeLanguage by LanguagePreferenceManager.observeLanguage(context).collectAsStateWithLifecycle()
  val activeCurrency by CurrencyPreferenceManager.observeCurrency(context).collectAsStateWithLifecycle()

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri ->
    if (uri != null) {
      onPickPhotoUri(uri)
    }
  }

  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
  ) { granted ->
    if (granted) {
      val updated = DailyReminderManager.updateSettings(
        context,
        reminderSettings.copy(enabled = true),
      )
      onReminderSettingsChange(updated)
      onStatusMessage(context.getString(R.string.reminder_enabled_status, updated.hour))
    } else {
      val updated = DailyReminderManager.updateSettings(
        context,
        reminderSettings.copy(enabled = false),
      )
      onReminderSettingsChange(updated)
      onStatusMessage(context.getString(R.string.reminder_denied_status))
    }
  }

  val hasCustomPhoto = !customPhotoBase64.isNullOrBlank()
  val hasGooglePhoto = !googlePhotoUrl.isNullOrBlank()

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      ProfileAvatar(
        displayName = displayName,
        customPhotoBase64 = customPhotoBase64,
        googlePhotoUrl = googlePhotoUrl,
        size = 72.dp,
      )
    },
    title = {
      Text(text = displayName)
    },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = email ?: stringResource(R.string.profile_connected_google),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = when {
            hasCustomPhoto -> stringResource(R.string.profile_photo_custom_desc)
            hasGooglePhoto -> stringResource(R.string.profile_photo_google_desc)
            else -> stringResource(R.string.profile_default_desc)
          },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Language Settings (Indonesian & 6 UN Official Languages)
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.Language,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
              )
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = stringResource(R.string.language_section_title),
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = stringResource(R.string.language_section_subtitle),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            OutlinedButton(
              onClick = { showLanguagePicker = true },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("profile_language_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Language,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = if (activeLanguage == AppLanguage.SYSTEM) {
                  "${stringResource(R.string.language_auto)} (${activeLanguage.resolveEffectiveLanguage().shortCode})"
                } else {
                  "${activeLanguage.nativeName} (${activeLanguage.shortCode})"
                },
                fontWeight = FontWeight.SemiBold,
              )
            }
          }
        }

        // Currency Settings
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.AttachMoney,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
              )
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = stringResource(R.string.currency_section_title),
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = stringResource(R.string.currency_section_subtitle),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              AppCurrency.entries.forEach { currency ->
                FilterChip(
                  selected = activeCurrency == currency,
                  onClick = {
                    CurrencyPreferenceManager.setCurrency(context, currency)
                    onStatusMessage(context.getString(R.string.currency_changed_status, currency.displayName))
                  },
                  label = { Text(currency.displayName) },
                  modifier = Modifier.testTag("currency_chip_${currency.code}"),
                )
              }
            }
          }
        }

        // Profile Photo Controls
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.PhotoCamera,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
              )
              Text(
                text = stringResource(R.string.profile_photo_title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
              )
            }
            OutlinedButton(
              onClick = {
                photoPickerLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
              },
              enabled = !isSavingProfile,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("pick_profile_photo_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.PhotoCamera,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                if (hasCustomPhoto) {
                  stringResource(R.string.profile_photo_change)
                } else {
                  stringResource(R.string.profile_photo_pick)
                }
              )
            }
            if (hasCustomPhoto) {
              TextButton(
                onClick = onSyncGooglePhoto,
                enabled = !isSavingProfile,
                modifier = Modifier
                  .fillMaxWidth()
                  .height(48.dp)
                  .testTag("sync_google_photo_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Sync,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  if (hasGooglePhoto) {
                    stringResource(R.string.profile_photo_use_google)
                  } else {
                    stringResource(R.string.profile_photo_remove_custom)
                  }
                )
              }
            }
          }
        }

        // Edit Profile Display Name
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
              )
              Text(
                text = stringResource(R.string.profile_edit_name_title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
              )
            }
            OutlinedTextField(
              value = draftName,
              onValueChange = { draftName = it.take(50) },
              label = { Text(stringResource(R.string.profile_display_name_label)) },
              placeholder = { Text(stringResource(R.string.profile_display_name_hint)) },
              singleLine = true,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_name_input"),
            )
            val trimmedDraft = draftName.trim()
            FilledTonalButton(
              onClick = { onSaveDisplayName(trimmedDraft) },
              enabled = !isSavingProfile && trimmedDraft.isNotEmpty() && trimmedDraft != displayName,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("save_profile_name_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Save,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                if (isSavingProfile) {
                  stringResource(R.string.action_saving)
                } else {
                  stringResource(R.string.profile_save_name_button)
                }
              )
            }
          }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

        // Daily Reminder Settings
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.NotificationsActive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
              )
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = stringResource(R.string.reminder_title),
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = stringResource(R.string.reminder_subtitle),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Switch(
                checked = reminderSettings.enabled,
                onCheckedChange = { enable ->
                  if (enable) {
                    val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                      ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS,
                      ) != PackageManager.PERMISSION_GRANTED
                    if (needsPermission) {
                      notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                      val updated = DailyReminderManager.updateSettings(
                        context,
                        reminderSettings.copy(enabled = true),
                      )
                      onReminderSettingsChange(updated)
                      onStatusMessage(context.getString(R.string.reminder_enabled_status, updated.hour))
                    }
                  } else {
                    val updated = DailyReminderManager.updateSettings(
                      context,
                      reminderSettings.copy(enabled = false),
                    )
                    onReminderSettingsChange(updated)
                    onStatusMessage(context.getString(R.string.reminder_disabled_status))
                  }
                },
                modifier = Modifier.testTag("reminder_toggle_switch"),
              )
            }
            if (reminderSettings.enabled) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                listOf(19, 20, 21).forEach { hourOption ->
                  FilterChip(
                    selected = reminderSettings.hour == hourOption,
                    onClick = {
                      val updated = DailyReminderManager.updateSettings(
                        context,
                        reminderSettings.copy(hour = hourOption, minute = 0),
                      )
                      onReminderSettingsChange(updated)
                      onStatusMessage(context.getString(R.string.reminder_time_changed, hourOption))
                    },
                    label = { Text("%02d:00".format(hourOption)) },
                    modifier = Modifier.testTag("reminder_hour_$hourOption"),
                  )
                }
              }
            }
          }
        }

        // Theme Mode Settings
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.Palette,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
              )
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = stringResource(R.string.theme_section_title),
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = stringResource(R.string.theme_section_subtitle),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              AppThemeMode.entries.forEach { mode ->
                val modeLabel = stringResource(mode.labelRes)
                FilterChip(
                  selected = themeMode == mode,
                  onClick = {
                    ThemePreferenceManager.setThemeMode(context, mode)
                    onStatusMessage(context.getString(R.string.theme_changed_status, modeLabel))
                  },
                  label = { Text(modeLabel) },
                  modifier = Modifier.testTag("theme_mode_${mode.name}"),
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(
        onClick = onSignOut,
        modifier = Modifier.testTag("sign_out_button"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.Logout,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(stringResource(R.string.action_sign_out))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.action_close))
      }
    },
  )

  if (showLanguagePicker) {
    LanguagePickerDialog(
      selectedLanguage = activeLanguage,
      onLanguageSelected = { picked ->
        val updatedCtx = LanguagePreferenceManager.localizedContext(context, picked)
        val pickedLabel = if (picked == AppLanguage.SYSTEM) {
          updatedCtx.getString(R.string.language_auto)
        } else {
          picked.nativeName
        }
        onStatusMessage(updatedCtx.getString(R.string.language_changed_status, pickedLabel))
      },
      onDismiss = { showLanguagePicker = false },
    )
  }
}
