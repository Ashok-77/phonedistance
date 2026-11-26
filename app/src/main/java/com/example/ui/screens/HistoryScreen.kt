package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.SessionWithLines
import com.example.domain.model.CalibrationEngine
import com.example.domain.model.CalibrationMethod
import com.example.domain.model.CalibrationState
import com.example.domain.model.Point2D
import com.example.ui.theme.CalibrationAmber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
  sessions: List<SessionWithLines>,
  onOpenSession: (SessionWithLines) -> Unit,
  onDeleteSession: (SessionWithLines) -> Unit,
  onClearAll: () -> Unit,
  onShareCsv: (List<SessionWithLines>, String) -> Unit,
  onSaveCsvToDevice: (List<SessionWithLines>, String) -> Unit,
  onGoToMeasure: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var searchQuery by remember { mutableStateOf("") }
  var showClearConfirm by remember { mutableStateOf(false) }
  var inspectingSession by remember { mutableStateOf<SessionWithLines?>(null) }

  val filteredSessions = remember(sessions, searchQuery) {
    if (searchQuery.isBlank()) sessions
    else sessions.filter { item ->
      item.session.title.contains(searchQuery, ignoreCase = true) ||
        item.lines.any { it.name.contains(searchQuery, ignoreCase = true) }
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column {
        Text(
          text = "Measurement History",
          style = MaterialTheme.typography.headlineSmall,
        )
        Text(
          text = "${sessions.size} local session(s) stored on-device",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      if (sessions.isNotEmpty()) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          IconButton(
            onClick = { onSaveCsvToDevice(sessions, "vision_measure_all_${System.currentTimeMillis()}.csv") },
            modifier = Modifier.testTag("export_all_save_csv_button"),
          ) {
            Icon(Icons.Filled.Download, contentDescription = "Save all sessions as CSV")
          }
          IconButton(
            onClick = { onShareCsv(sessions, "vision_measure_all") },
            modifier = Modifier.testTag("export_all_share_csv_button"),
          ) {
            Icon(Icons.Filled.Share, contentDescription = "Share all sessions CSV")
          }
          IconButton(
            onClick = { showClearConfirm = true },
            modifier = Modifier.testTag("clear_all_history_button"),
          ) {
            Icon(
              Icons.Filled.DeleteSweep,
              contentDescription = "Clear all history",
              tint = MaterialTheme.colorScheme.error,
            )
          }
        }
      }
    }

    if (sessions.isNotEmpty()) {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        placeholder = { Text("Search sessions or measurement names...") },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("history_search_input"),
      )
    }

    if (filteredSessions.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(24.dp),
        contentAlignment = Alignment.Center,
      ) {
        Column(
          modifier = Modifier.verticalScroll(rememberScrollState()),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(52.dp),
          )
          Text(
            text = if (sessions.isEmpty()) "No Saved Sessions Yet" else "No Matching Sessions",
            style = MaterialTheme.typography.titleMedium,
          )
          Text(
            text = if (sessions.isEmpty()) {
              "Measure an object in the workspace and tap the Save icon to store sessions locally and export CSV reports."
            } else {
              "Try a different search keyword."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          if (sessions.isEmpty()) {
            Button(
              onClick = onGoToMeasure,
              modifier = Modifier.testTag("empty_history_measure_button"),
            ) {
              Text("Go to Workspace")
            }
          }
        }
      }
    } else {
      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
      ) {
        items(filteredSessions, key = { it.session.id }) { item ->
          SessionHistoryCard(
            item = item,
            onInspect = { inspectingSession = item },
            onOpen = { onOpenSession(item) },
            onShareCsv = { onShareCsv(listOf(item), item.session.title) },
            onSaveCsv = { onSaveCsvToDevice(listOf(item), "${item.session.title.replace(" ", "_")}.csv") },
            onDelete = { onDeleteSession(item) },
          )
        }
      }
    }
  }

  if (showClearConfirm) {
    AlertDialog(
      onDismissRequest = { showClearConfirm = false },
      title = { Text("Clear All History?") },
      text = { Text("This permanently deletes all saved local sessions and captured measurement photos.") },
      confirmButton = {
        Button(
          onClick = {
            onClearAll()
            showClearConfirm = false
          },
          modifier = Modifier.testTag("confirm_clear_history_button"),
        ) {
          Text("Delete All")
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
      },
    )
  }

  inspectingSession?.let { item ->
    SessionDetailDialog(
      item = item,
      onDismiss = { inspectingSession = null },
      onOpenInWorkspace = {
        inspectingSession = null
        onOpenSession(item)
      },
      onExportCsv = {
        onShareCsv(listOf(item), item.session.title)
      },
    )
  }
}

@Composable
private fun SessionHistoryCard(
  item: SessionWithLines,
  onInspect: () -> Unit,
  onOpen: () -> Unit,
  onShareCsv: () -> Unit,
  onSaveCsv: () -> Unit,
  onDelete: () -> Unit,
) {
  val session = item.session
  val method = runCatching { CalibrationMethod.valueOf(session.calibrationMethod) }
    .getOrDefault(CalibrationMethod.PIXEL_MODE)
  val isCalibrated = method != CalibrationMethod.PIXEL_MODE && (session.pixelsPerCm ?: 0.0) > 0.0
  val dateLabel = remember(session.createdAt) {
    SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault()).format(Date(session.createdAt))
  }

  val thumbnail by produceState<Bitmap?>(initialValue = null, session.imagePath) {
    val path = session.imagePath
    if (!path.isNullOrBlank()) {
      value = withContext(Dispatchers.IO) {
        runCatching {
          val file = File(path)
          if (!file.exists()) return@runCatching null
          val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
          BitmapFactory.decodeFile(file.absolutePath, opts)
        }.getOrNull()
      }
    }
  }

  Card(
    onClick = onInspect,
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("session_card_${session.id}"),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (thumbnail != null) {
          Image(
            bitmap = thumbnail!!.asImageBitmap(),
            contentDescription = "Thumbnail for ${session.title}",
            contentScale = ContentScale.Crop,
            modifier = Modifier
              .size(64.dp)
              .clip(RoundedCornerShape(12.dp)),
          )
        } else {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(64.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Filled.Straighten,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
            }
          }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
          Text(
            text = session.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = dateLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "${item.lines.size} line(s)",
              style = MaterialTheme.typography.labelSmall,
            )
            Text(
              text = if (isCalibrated) method.displayName else "Pixel Mode",
              style = MaterialTheme.typography.labelSmall,
              color = if (isCalibrated) MaterialTheme.colorScheme.tertiary else CalibrationAmber,
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        FilledTonalButton(
          onClick = onOpen,
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          modifier = Modifier.testTag("open_session_${session.id}"),
        ) {
          Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(6.dp))
          Text("Open")
        }

        Row {
          IconButton(
            onClick = onSaveCsv,
            modifier = Modifier.testTag("save_csv_${session.id}"),
          ) {
            Icon(Icons.Filled.Download, contentDescription = "Save session CSV")
          }
          IconButton(
            onClick = onShareCsv,
            modifier = Modifier.testTag("share_csv_${session.id}"),
          ) {
            Icon(Icons.Filled.Share, contentDescription = "Share session CSV")
          }
          IconButton(
            onClick = onDelete,
            modifier = Modifier.testTag("delete_session_${session.id}"),
          ) {
            Icon(
              Icons.Filled.Delete,
              contentDescription = "Delete session",
              tint = MaterialTheme.colorScheme.error,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SessionDetailDialog(
  item: SessionWithLines,
  onDismiss: () -> Unit,
  onOpenInWorkspace: () -> Unit,
  onExportCsv: () -> Unit,
) {
  val session = item.session
  val method = runCatching { CalibrationMethod.valueOf(session.calibrationMethod) }
    .getOrDefault(CalibrationMethod.PIXEL_MODE)
  val calibration = CalibrationState(
    method = method,
    pixelsPerCm = session.pixelsPerCm,
    referenceLabel = session.referenceLabel,
    referenceLengthCm = session.referenceLengthCm,
  )

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(session.title) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = if (calibration.isCalibrated) {
            "Calibration: ${method.displayName} (${session.referenceLabel} • ${String.format(Locale.US, "%.1f", session.pixelsPerCm)} px/cm)"
          } else {
            "Calibration: Uncalibrated (Pixel Mode)"
          },
          style = MaterialTheme.typography.labelMedium,
          color = if (calibration.isCalibrated) MaterialTheme.colorScheme.tertiary else CalibrationAmber,
        )
        item.lines.sortedBy { it.sortOrder }.forEach { line ->
          val px = CalibrationEngine.pixelDistance(Point2D(line.ax, line.ay), Point2D(line.bx, line.by))
          val cal = CalibrationEngine.measure(px, calibration)
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text(line.name, style = MaterialTheme.typography.titleSmall)
              if (cal != null) {
                Text(
                  text = String.format(Locale.US, "%.2f cm  •  %.3f m  •  %.2f in  (%.1f px)", cal.centimeters, cal.meters, cal.inches, cal.pixels),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.primary,
                )
              } else {
                Text(
                  text = String.format(Locale.US, "%.1f px", px),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
              Text(
                text = String.format(Locale.US, "A(%.0f, %.0f) → B(%.0f, %.0f)", line.ax, line.ay, line.bx, line.by),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    },
    confirmButton = {
      Button(onClick = onOpenInWorkspace) { Text("Open in Canvas") }
    },
    dismissButton = {
      Row {
        OutlinedButton(onClick = onExportCsv) { Text("Share CSV") }
        Spacer(Modifier.width(6.dp))
        TextButton(onClick = onDismiss) { Text("Close") }
      }
    },
  )
}
