package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  var draftName by remember(displayName) { mutableStateOf(displayName) }
  var showLanguagePicker by remember { mutableStateOf(false) }
  var expandNameEditor by remember { mutableStateOf(false) }
  var expandCurrencyPicker by remember { mutableStateOf(false) }
  var expandThemePicker by remember { mutableStateOf(false) }

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

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    tonalElevation = 0.dp,
  ) {
    Box(
      modifier = Modifier.fillMaxWidth(),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier = Modifier
          .widthIn(max = 520.dp)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp)
          .navigationBarsPadding()
          .padding(bottom = 24.dp),
      ) {
        // Minimalist Profile Identity Header
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Box(contentAlignment = Alignment.BottomEnd) {
            ProfileAvatar(
              displayName = displayName,
              customPhotoBase64 = customPhotoBase64,
              googlePhotoUrl = googlePhotoUrl,
              size = 60.dp,
            )
            Surface(
              onClick = {
                if (!isSavingProfile) {
                  photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                  )
                }
              },
              shape = CircleShape,
              color = MaterialTheme.colorScheme.surfaceContainerHigh,
              border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.surface,
              ),
              modifier = Modifier.size(24.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Outlined.PhotoCamera,
                  contentDescription = stringResource(R.string.profile_photo_title),
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(13.dp),
                )
              }
            }
          }

          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
          ) {
            Text(
              text = displayName,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = email ?: stringResource(R.string.profile_connected_google),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = when {
                hasCustomPhoto -> stringResource(R.string.profile_photo_custom_desc)
                hasGooglePhoto -> stringResource(R.string.profile_photo_google_desc)
                else -> stringResource(R.string.profile_default_desc)
              },
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier
              .size(48.dp)
              .testTag("profile_close_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Close,
              contentDescription = stringResource(R.string.action_close),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(20.dp),
            )
          }
        }

        // Section 1: Account
        ProfileGroupHeader(title = stringResource(R.string.profile_group_account))
        ProfileGroupCard {
          ProfileSettingRow(
            icon = Icons.Outlined.Edit,
            title = stringResource(R.string.profile_edit_name_title),
            trailingValue = displayName,
            trailingIcon = if (expandNameEditor) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
            onClick = { expandNameEditor = !expandNameEditor },
            modifier = Modifier.testTag("profile_name_row"),
          )

          AnimatedVisibility(
            visible = expandNameEditor,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
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
                onClick = {
                  onSaveDisplayName(trimmedDraft)
                  expandNameEditor = false
                },
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

          ProfileSubtleDivider()

          ProfileSettingRow(
            icon = Icons.Outlined.PhotoCamera,
            title = if (hasCustomPhoto) {
              stringResource(R.string.profile_photo_change)
            } else {
              stringResource(R.string.profile_photo_pick)
            },
            trailingIcon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            enabled = !isSavingProfile,
            onClick = {
              photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
              )
            },
            modifier = Modifier.testTag("pick_profile_photo_button"),
          )

          if (hasCustomPhoto) {
            ProfileSubtleDivider()
            ProfileSettingRow(
              icon = Icons.Outlined.Sync,
              title = if (hasGooglePhoto) {
                stringResource(R.string.profile_photo_use_google)
              } else {
                stringResource(R.string.profile_photo_remove_custom)
              },
              trailingIcon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
              enabled = !isSavingProfile,
              onClick = onSyncGooglePhoto,
              modifier = Modifier.testTag("sync_google_photo_button"),
            )
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Section 2: Preferences
        ProfileGroupHeader(title = stringResource(R.string.profile_group_preferences))
        ProfileGroupCard {
          val languageValue = if (activeLanguage == AppLanguage.SYSTEM) {
            "${stringResource(R.string.language_auto)} (${activeLanguage.resolveEffectiveLanguage().shortCode})"
          } else {
            activeLanguage.nativeName
          }
          ProfileSettingRow(
            icon = Icons.Outlined.Language,
            title = stringResource(R.string.language_section_title),
            trailingValue = languageValue,
            trailingIcon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            onClick = { showLanguagePicker = true },
            modifier = Modifier.testTag("profile_language_button"),
          )

          ProfileSubtleDivider()

          ProfileSettingRow(
            icon = Icons.Outlined.AttachMoney,
            title = stringResource(R.string.currency_section_title),
            trailingValue = activeCurrency.displayName,
            trailingIcon = if (expandCurrencyPicker) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
            onClick = { expandCurrencyPicker = !expandCurrencyPicker },
            modifier = Modifier.testTag("profile_currency_button"),
          )

          AnimatedVisibility(
            visible = expandCurrencyPicker,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
          ) {
            FlowRow(
              modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              AppCurrency.entries.forEach { currency ->
                FilterChip(
                  selected = activeCurrency == currency,
                  onClick = {
                    CurrencyPreferenceManager.setCurrency(context, currency)
                    onStatusMessage(context.getString(R.string.currency_changed_status, currency.displayName))
                    expandCurrencyPicker = false
                  },
                  label = { Text(currency.displayName) },
                  modifier = Modifier.testTag("currency_chip_${currency.code}"),
                )
              }
            }
          }

          ProfileSubtleDivider()

          ProfileSettingRow(
            icon = Icons.Outlined.Palette,
            title = stringResource(R.string.theme_section_title),
            trailingValue = stringResource(themeMode.labelRes),
            trailingIcon = if (expandThemePicker) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
            onClick = { expandThemePicker = !expandThemePicker },
            modifier = Modifier.testTag("profile_theme_button"),
          )

          AnimatedVisibility(
            visible = expandThemePicker,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
          ) {
            SingleChoiceSegmentedButtonRow(
              modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
            ) {
              AppThemeMode.entries.forEachIndexed { index, mode ->
                val modeLabel = stringResource(mode.labelRes)
                SegmentedButton(
                  selected = themeMode == mode,
                  onClick = {
                    ThemePreferenceManager.setThemeMode(context, mode)
                    onStatusMessage(context.getString(R.string.theme_changed_status, modeLabel))
                  },
                  shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = AppThemeMode.entries.size,
                  ),
                  modifier = Modifier.testTag("theme_mode_${mode.name}"),
                ) {
                  Text(
                    text = modeLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                  )
                }
              }
            }
          }

          ProfileSubtleDivider()

          // Daily Reminder Switch Row
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(min = 54.dp)
              .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Icon(
              imageVector = Icons.Outlined.Notifications,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = stringResource(R.string.reminder_title),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = if (reminderSettings.enabled) {
                  "%02d:00".format(reminderSettings.hour)
                } else {
                  stringResource(R.string.reminder_subtitle)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
              modifier = Modifier
                .scale(0.9f)
                .testTag("reminder_toggle_switch"),
            )
          }

          AnimatedVisibility(
            visible = reminderSettings.enabled,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(start = 48.dp, end = 16.dp, bottom = 12.dp),
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

        Spacer(modifier = Modifier.height(18.dp))

        // Subtle Sign Out Footer
        TextButton(
          onClick = onSignOut,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("sign_out_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.Logout,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = stringResource(R.string.action_sign_out),
            color = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
            style = MaterialTheme.typography.labelLarge,
          )
        }
      }
    }
  }

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

@Composable
private fun ProfileGroupHeader(title: String) {
  Text(
    text = title,
    style = MaterialTheme.typography.labelMedium,
    fontWeight = FontWeight.SemiBold,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.padding(start = 6.dp, bottom = 6.dp),
  )
}

@Composable
private fun ProfileGroupCard(
  content: @Composable () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    border = BorderStroke(
      width = 0.5.dp,
      color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
    ),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      content()
    }
  }
}

@Composable
private fun ProfileSubtleDivider() {
  HorizontalDivider(
    thickness = 0.5.dp,
    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
    modifier = Modifier.padding(horizontal = 16.dp),
  )
}

@Composable
private fun ProfileSettingRow(
  icon: ImageVector,
  title: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  trailingValue: String? = null,
  trailingIcon: ImageVector? = null,
  enabled: Boolean = true,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .heightIn(min = 52.dp)
      .clickable(enabled = enabled, onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(20.dp),
    )
    Text(
      text = title,
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.Medium,
      color = MaterialTheme.colorScheme.onSurface,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f),
    )
    if (!trailingValue.isNullOrBlank()) {
      Text(
        text = trailingValue,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.widthIn(max = 150.dp),
      )
    }
    if (trailingIcon != null) {
      Icon(
        imageVector = trailingIcon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.size(18.dp),
      )
    }
  }
}
