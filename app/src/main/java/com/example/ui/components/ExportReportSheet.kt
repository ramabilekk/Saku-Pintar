package com.example.ui.components

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.i18n.CurrencyPreferenceManager
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.FinanceDashboardData
import com.example.util.Formatters
import com.example.util.ReportExporter
import com.example.util.ReportFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportReportSheet(
  data: FinanceDashboardData,
  onStatusMessage: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val activeCurrency by CurrencyPreferenceManager.observeCurrency(context).collectAsStateWithLifecycle()
  val scope = rememberCoroutineScope()
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val latestData by rememberUpdatedState(data)
  var isWorking by remember { mutableStateOf(false) }

  fun launchSave(format: ReportFormat, uri: android.net.Uri) {
    val snapshot = latestData
    val formatLabel = format.label(context)
    scope.launch {
      isWorking = true
      val message = withContext(Dispatchers.IO) {
        runCatching {
          context.contentResolver.openOutputStream(uri)?.use { stream ->
            ReportExporter.writeMonthlyReport(
              outputStream = stream,
              format = format,
              monthKey = snapshot.monthKey,
              expenses = snapshot.expenses,
              budget = snapshot.budget,
              dailyInsight = snapshot.dailyInsight,
              context = context,
              currency = activeCurrency,
            )
          } ?: error(context.getString(R.string.error_generic))
        }.fold(
          onSuccess = { context.getString(R.string.export_saved_success, formatLabel) },
          onFailure = { err ->
            context.getString(
              R.string.export_saved_error,
              formatLabel,
              err.localizedMessage ?: context.getString(R.string.error_generic),
            )
          },
        )
      }
      isWorking = false
      onStatusMessage(message)
      onDismiss()
    }
  }

  val excelLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument(ReportFormat.EXCEL.mimeType),
  ) { uri ->
    if (uri != null) launchSave(ReportFormat.EXCEL, uri)
  }

  val pdfLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument(ReportFormat.PDF.mimeType),
  ) { uri ->
    if (uri != null) launchSave(ReportFormat.PDF, uri)
  }

  val csvLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument(ReportFormat.CSV.mimeType),
  ) { uri ->
    if (uri != null) launchSave(ReportFormat.CSV, uri)
  }

  fun saveWithPicker(format: ReportFormat) {
    val fileName = ReportExporter.defaultFileName(latestData.monthKey, format)
    when (format) {
      ReportFormat.EXCEL -> excelLauncher.launch(fileName)
      ReportFormat.PDF -> pdfLauncher.launch(fileName)
      ReportFormat.CSV -> csvLauncher.launch(fileName)
    }
  }

  fun shareReportFile(format: ReportFormat) {
    val snapshot = latestData
    val formatLabel = format.label(context)
    scope.launch {
      isWorking = true
      val uriResult = withContext(Dispatchers.IO) {
        runCatching {
          ReportExporter.createShareableReportUri(
            context = context,
            format = format,
            monthKey = snapshot.monthKey,
            expenses = snapshot.expenses,
            budget = snapshot.budget,
            dailyInsight = snapshot.dailyInsight,
            currency = activeCurrency,
          )
        }
      }
      isWorking = false
      uriResult.onSuccess { uri ->
        val monthTitle = Formatters.formatMonthTitle(snapshot.monthKey)
        val subject = context.getString(R.string.export_subject, monthTitle)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
          type = format.mimeType
          putExtra(Intent.EXTRA_SUBJECT, subject)
          putExtra(Intent.EXTRA_STREAM, uri)
          addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching {
          context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.export_share_action)))
          onDismiss()
        }.onFailure {
          onStatusMessage(context.getString(R.string.export_no_app_file, formatLabel))
        }
      }.onFailure { err ->
        onStatusMessage(
          context.getString(
            R.string.export_saved_error,
            formatLabel,
            err.localizedMessage ?: context.getString(R.string.error_generic),
          )
        )
      }
    }
  }

  fun shareSummaryText() {
    val snapshot = latestData
    val summaryText = ReportExporter.buildMonthlyShareText(
      monthKey = snapshot.monthKey,
      expenses = snapshot.expenses,
      budget = snapshot.budget,
      dailyInsight = snapshot.dailyInsight,
      context = context,
      currency = activeCurrency,
    )
    val monthTitle = Formatters.formatMonthTitle(snapshot.monthKey)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      putExtra(
        Intent.EXTRA_SUBJECT,
        context.getString(R.string.export_subject, monthTitle),
      )
      putExtra(Intent.EXTRA_TEXT, summaryText)
    }
    runCatching {
      context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.export_share_action)))
      onDismiss()
    }.onFailure {
      onStatusMessage(context.getString(R.string.export_no_app_text))
    }
  }

  ModalBottomSheet(
    onDismissRequest = { if (!isWorking) onDismiss() },
    sheetState = sheetState,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp)
        .navigationBarsPadding()
        .padding(bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = stringResource(R.string.export_sheet_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = Formatters.formatMonthTitle(data.monthKey),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.primaryContainer,
        ) {
          Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.End,
          ) {
            Text(
              text = stringResource(R.string.export_tx_count, data.expenses.size),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
              text = Formatters.formatMoneyCompact(data.expenses.sumOf { it.amount }, activeCurrency),
              style = MaterialTheme.typography.labelLarge,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
        }
      }

      Text(
        text = stringResource(R.string.export_save_section),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
      )

      ExportFormatCard(
        icon = Icons.Rounded.TableChart,
        title = stringResource(R.string.export_excel_title),
        subtitle = stringResource(R.string.export_excel_subtitle),
        badge = stringResource(R.string.export_primary_badge),
        highlighted = true,
        enabled = !isWorking,
        onClick = { saveWithPicker(ReportFormat.EXCEL) },
        modifier = Modifier.testTag("export_option_excel"),
      )

      ExportFormatCard(
        icon = Icons.Rounded.PictureAsPdf,
        title = stringResource(R.string.export_pdf_title),
        subtitle = stringResource(R.string.export_pdf_subtitle),
        badge = null,
        highlighted = false,
        enabled = !isWorking,
        onClick = { saveWithPicker(ReportFormat.PDF) },
        modifier = Modifier.testTag("export_option_pdf"),
      )

      ExportFormatCard(
        icon = Icons.Rounded.Description,
        title = stringResource(R.string.export_csv_title),
        subtitle = stringResource(R.string.export_csv_subtitle),
        badge = null,
        highlighted = false,
        enabled = !isWorking,
        onClick = { saveWithPicker(ReportFormat.CSV) },
        modifier = Modifier.testTag("export_option_csv"),
      )

      HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(
          imageVector = Icons.Rounded.Share,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(18.dp),
        )
        Text(
          text = stringResource(R.string.export_share_section),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        FilledTonalButton(
          onClick = { shareReportFile(ReportFormat.EXCEL) },
          enabled = !isWorking,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .testTag("share_option_excel"),
        ) {
          Icon(
            imageVector = Icons.Rounded.TableChart,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = stringResource(R.string.export_btn_excel),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        OutlinedButton(
          onClick = { shareReportFile(ReportFormat.PDF) },
          enabled = !isWorking,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .testTag("share_option_pdf"),
        ) {
          Icon(
            imageVector = Icons.Rounded.PictureAsPdf,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = stringResource(R.string.export_btn_pdf),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        OutlinedButton(
          onClick = { shareSummaryText() },
          enabled = !isWorking,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .testTag("share_option_text"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Share,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = stringResource(R.string.export_btn_text),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}

@Composable
private fun ExportFormatCard(
  icon: ImageVector,
  title: String,
  subtitle: String,
  badge: String?,
  highlighted: Boolean,
  enabled: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    onClick = onClick,
    enabled = enabled,
    shape = RoundedCornerShape(16.dp),
    color = if (highlighted) {
      MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.52f)
    } else {
      MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    },
    border = if (highlighted) {
      BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
    } else {
      BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    },
    modifier = modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (highlighted) {
          MaterialTheme.colorScheme.primary
        } else {
          MaterialTheme.colorScheme.secondaryContainer
        },
        modifier = Modifier.size(44.dp),
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = if (highlighted) {
            MaterialTheme.colorScheme.onPrimary
          } else {
            MaterialTheme.colorScheme.onSecondaryContainer
          },
          modifier = Modifier
            .padding(10.dp)
            .size(24.dp),
        )
      }
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
          )
          if (badge != null) {
            Surface(
              shape = RoundedCornerShape(50),
              color = MaterialTheme.colorScheme.primary,
            ) {
              Text(
                text = badge,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
              )
            }
          }
        }
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Icon(
        imageVector = Icons.Rounded.FileDownload,
        contentDescription = title,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(20.dp),
      )
    }
  }
}
