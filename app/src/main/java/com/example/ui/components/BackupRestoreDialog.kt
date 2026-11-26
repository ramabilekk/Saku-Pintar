package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.BackupRestoreManager
import com.example.data.local.FullAppBackup
import com.example.data.model.Expense
import com.example.ui.theme.EmeraldSafe
import com.example.util.CsvExpenseImporter
import kotlinx.coroutines.launch

/**
 * Zero-permission Storage Access Framework (SAF) Backup & Restore JSON dialog.
 * Works seamlessly for both Google Sign-In (Firestore) and Guest Mode accounts.
 */
@Composable
fun BackupRestoreDialog(
  userId: String,
  onBuildBackup: suspend () -> FullAppBackup,
  onRestoreBackup: suspend (FullAppBackup) -> Result<Int>,
  onStatusMessage: (String) -> Unit,
  onDismiss: () -> Unit,
  onImportCsvExpenses: (suspend (List<Expense>) -> Result<Int>)? = null,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val isDarkTheme = rememberIsEffectiveDarkTheme()

  var isExporting by remember { mutableStateOf(false) }
  var isRestoring by remember { mutableStateOf(false) }
  var isImportingCsv by remember { mutableStateOf(false) }
  var pendingRestoreBackup by remember { mutableStateOf<FullAppBackup?>(null) }
  var pendingCsvExpenses by remember { mutableStateOf<List<Expense>?>(null) }

  val exportLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/json"),
  ) { uri ->
    if (uri != null) {
      scope.launch {
        isExporting = true
        try {
          val backup = onBuildBackup()
          val result = BackupRestoreManager.writeBackupToUri(context, uri, backup)
          result.onSuccess { count ->
            onStatusMessage(context.getString(R.string.backup_export_success, count))
            onDismiss()
          }.onFailure { err ->
            onStatusMessage(
              context.getString(
                R.string.backup_error_generic,
                err.localizedMessage ?: context.getString(R.string.error_generic),
              )
            )
          }
        } catch (e: Exception) {
          onStatusMessage(
            context.getString(
              R.string.backup_error_generic,
              e.localizedMessage ?: context.getString(R.string.error_generic),
            )
          )
        } finally {
          isExporting = false
        }
      }
    }
  }

  val importLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
  ) { uri ->
    if (uri != null) {
      scope.launch {
        isRestoring = true
        try {
          val parsed = BackupRestoreManager.readBackupFromUri(context, uri, userId)
          parsed.onSuccess { backup ->
            pendingRestoreBackup = backup
          }.onFailure { err ->
            onStatusMessage(
              context.getString(
                R.string.backup_error_generic,
                err.localizedMessage ?: context.getString(R.string.error_generic),
              )
            )
          }
        } finally {
          isRestoring = false
        }
      }
    }
  }

  val csvImportLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
  ) { uri ->
    if (uri != null) {
      scope.launch {
        isImportingCsv = true
        try {
          val parsed = CsvExpenseImporter.parseCsvFromUri(context, uri, userId)
          parsed.onSuccess { list ->
            if (list.isEmpty()) {
              onStatusMessage(context.getString(R.string.csv_import_empty_error))
            } else {
              pendingCsvExpenses = list
            }
          }.onFailure { err ->
            onStatusMessage(
              context.getString(
                R.string.backup_error_generic,
                err.localizedMessage ?: context.getString(R.string.error_generic),
              )
            )
          }
        } finally {
          isImportingCsv = false
        }
      }
    }
  }

  val isBusy = isExporting || isRestoring || isImportingCsv

  LiquidGlassAlertDialog(
    onDismissRequest = { if (!isBusy) onDismiss() },
    title = {
      Text(
        text = stringResource(R.string.backup_dialog_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text = stringResource(R.string.backup_dialog_desc),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Export Backup Card
        SculptedSurfaceCard(
          accentColor = EmeraldSafe,
          cornerRadius = 18,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = EmeraldSafe.copy(alpha = 0.14f),
                modifier = Modifier.size(36.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Rounded.CloudUpload,
                    contentDescription = null,
                    tint = EmeraldSafe,
                    modifier = Modifier.size(20.dp),
                  )
                }
              }
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = stringResource(R.string.backup_export_title),
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = stringResource(R.string.backup_export_subtitle),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            Button(
              onClick = {
                exportLauncher.launch(BackupRestoreManager.suggestedBackupFileName())
              },
              enabled = !isExporting && !isRestoring,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("backup_export_json_button"),
            ) {
              if (isExporting) {
                CircularProgressIndicator(
                  modifier = Modifier.size(16.dp),
                  strokeWidth = 2.dp,
                  color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.backup_in_progress))
              } else {
                Icon(
                  imageVector = Icons.Rounded.CloudUpload,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.backup_export_button), fontWeight = FontWeight.Bold)
              }
            }
          }
        }

        // Restore Backup Card
        SculptedSurfaceCard(
          accentColor = MaterialTheme.colorScheme.primary,
          cornerRadius = 18,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                modifier = Modifier.size(36.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Rounded.CloudDownload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                  )
                }
              }
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = stringResource(R.string.backup_restore_title),
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = stringResource(R.string.backup_restore_subtitle),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            OutlinedButton(
              onClick = {
                importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
              },
              enabled = !isExporting && !isRestoring,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("backup_restore_json_button"),
            ) {
              if (isRestoring) {
                CircularProgressIndicator(
                  modifier = Modifier.size(16.dp),
                  strokeWidth = 2.dp,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.backup_restore_in_progress))
              } else {
                Icon(
                  imageVector = Icons.Rounded.CloudDownload,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.backup_restore_button), fontWeight = FontWeight.SemiBold)
              }
            }
          }
        }

        // Import CSV Expenses Card
        if (onImportCsvExpenses != null) {
          SculptedSurfaceCard(
            accentColor = MaterialTheme.colorScheme.tertiary,
            cornerRadius = 18,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f),
                  modifier = Modifier.size(36.dp),
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = Icons.Rounded.TableChart,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.tertiary,
                      modifier = Modifier.size(20.dp),
                    )
                  }
                }
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = stringResource(R.string.csv_import_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = stringResource(R.string.csv_import_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
              OutlinedButton(
                onClick = {
                  csvImportLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "*/*"))
                },
                enabled = !isBusy,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(48.dp)
                  .testTag("backup_import_csv_button"),
              ) {
                if (isImportingCsv) {
                  CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(stringResource(R.string.csv_import_in_progress))
                } else {
                  Icon(
                    imageVector = Icons.Rounded.TableChart,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(stringResource(R.string.csv_import_button), fontWeight = FontWeight.SemiBold)
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {},
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        enabled = !isBusy,
        modifier = Modifier.height(48.dp),
      ) {
        Text(stringResource(R.string.action_close))
      }
    },
  )

  pendingRestoreBackup?.let { parsedBackup ->
    LiquidGlassAlertDialog(
      onDismissRequest = { if (!isRestoring) pendingRestoreBackup = null },
      title = {
        Text(
          text = stringResource(R.string.backup_restore_confirm_title),
          fontWeight = FontWeight.Bold,
        )
      },
      text = {
        Text(
          text = stringResource(R.string.backup_restore_confirm_body, parsedBackup.totalItemCount),
          style = MaterialTheme.typography.bodyMedium,
        )
      },
      confirmButton = {
        FilledTonalButton(
          onClick = {
            scope.launch {
              isRestoring = true
              try {
                val res = onRestoreBackup(parsedBackup)
                res.onSuccess { restoredCount ->
                  pendingRestoreBackup = null
                  onStatusMessage(context.getString(R.string.backup_restore_success, restoredCount))
                  onDismiss()
                }.onFailure { err ->
                  pendingRestoreBackup = null
                  onStatusMessage(
                    context.getString(
                      R.string.backup_error_generic,
                      err.localizedMessage ?: context.getString(R.string.error_generic),
                    )
                  )
                }
              } finally {
                isRestoring = false
              }
            }
          },
          enabled = !isRestoring,
          modifier = Modifier
            .height(48.dp)
            .testTag("backup_restore_confirm_button"),
        ) {
          Text(stringResource(R.string.backup_restore_confirm_action), fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(
          onClick = { pendingRestoreBackup = null },
          enabled = !isRestoring,
          modifier = Modifier.height(48.dp),
        ) {
          Text(stringResource(R.string.action_cancel))
        }
      },
    )
  }

  pendingCsvExpenses?.let { parsedExpenses ->
    val importer = onImportCsvExpenses
    if (importer != null) {
      LiquidGlassAlertDialog(
        onDismissRequest = { if (!isImportingCsv) pendingCsvExpenses = null },
        title = {
          Text(
            text = stringResource(R.string.csv_import_confirm_title),
            fontWeight = FontWeight.Bold,
          )
        },
        text = {
          Text(
            text = stringResource(R.string.csv_import_confirm_body, parsedExpenses.size),
            style = MaterialTheme.typography.bodyMedium,
          )
        },
        confirmButton = {
          FilledTonalButton(
            onClick = {
              scope.launch {
                isImportingCsv = true
                try {
                  val res = importer(parsedExpenses)
                  res.onSuccess { count ->
                    pendingCsvExpenses = null
                    onStatusMessage(context.getString(R.string.csv_import_success, count))
                    onDismiss()
                  }.onFailure { err ->
                    pendingCsvExpenses = null
                    onStatusMessage(
                      context.getString(
                        R.string.backup_error_generic,
                        err.localizedMessage ?: context.getString(R.string.error_generic),
                      )
                    )
                  }
                } finally {
                  isImportingCsv = false
                }
              }
            },
            enabled = !isImportingCsv,
            modifier = Modifier
              .height(48.dp)
              .testTag("csv_import_confirm_button"),
          ) {
            Text(stringResource(R.string.csv_import_confirm_action), fontWeight = FontWeight.Bold)
          }
        },
        dismissButton = {
          TextButton(
            onClick = { pendingCsvExpenses = null },
            enabled = !isImportingCsv,
            modifier = Modifier.height(48.dp),
          ) {
            Text(stringResource(R.string.action_cancel))
          }
        },
      )
    }
  }
}
