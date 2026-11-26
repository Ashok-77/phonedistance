package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.CalibrationEngine
import com.example.domain.model.CalibrationState
import com.example.domain.model.MeasurementLine
import com.example.domain.model.Point2D
import com.example.ui.theme.CalibrationAmber
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReadoutPanel(
  lines: List<MeasurementLine>,
  selectedLine: MeasurementLine?,
  pendingPointA: Point2D?,
  calibration: CalibrationState,
  onSelectLine: (String) -> Unit,
  onRenameLine: (MeasurementLine) -> Unit,
  onDeleteLine: (String) -> Unit,
  onOpenCalibration: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val activeLine = selectedLine
  val calibrated = activeLine?.let {
    CalibrationEngine.measure(it.pixelDistance.toDouble(), calibration)
  }

  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 3.dp,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      // Status Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = if (calibration.isCalibrated) Icons.Filled.Straighten else Icons.Outlined.WarningAmber,
            contentDescription = null,
            tint = if (calibration.isCalibrated) MaterialTheme.colorScheme.tertiary else CalibrationAmber,
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = if (calibration.isCalibrated) {
              "${calibration.method.displayName} • ${String.format(Locale.US, "%.1f", calibration.pixelsPerCm)} px/cm"
            } else {
              "Pixel Mode (Uncalibrated)"
            },
            style = MaterialTheme.typography.labelMedium,
            color = if (calibration.isCalibrated) MaterialTheme.colorScheme.tertiary else CalibrationAmber,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        TextButton(
          onClick = onOpenCalibration,
          modifier = Modifier.testTag("readout_calibrate_button"),
        ) {
          Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text(if (calibration.isCalibrated) "Recalibrate" else "Calibrate")
        }
      }

      if (activeLine == null) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Text(
              text = if (pendingPointA == null) {
                "Tap Point A on the viewport to begin measuring"
              } else {
                "Point A set at (${pendingPointA.x.toInt()}, ${pendingPointA.y.toInt()}) — tap Point B"
              },
              style = MaterialTheme.typography.titleSmall,
            )
            Text(
              text = "Drag any handle after placing points to fine-tune with sub-pixel precision.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      } else {
        // Primary Digital Readout Card
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = activeLine.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
              )
              Text(
                text = String.format(Locale.US, "θ %.1f°", activeLine.angleDegrees),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }

            if (calibrated != null) {
              Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Text(
                  text = String.format(Locale.US, "%.2f cm", calibrated.centimeters),
                  style = MaterialTheme.typography.headlineMedium,
                  color = MaterialTheme.colorScheme.primary,
                )
                Text(
                  text = String.format(Locale.US, "%.1f px", calibrated.pixels),
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(bottom = 4.dp),
                )
              }

              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                MetricChip("Meters", String.format(Locale.US, "%.3f m", calibrated.meters))
                MetricChip("Inches", String.format(Locale.US, "%.2f in", calibrated.inches))
                MetricChip("Pixels", String.format(Locale.US, "%.1f px", calibrated.pixels))
              }
            } else {
              Text(
                text = String.format(Locale.US, "%.1f px", activeLine.pixelDistance),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
              )
              Text(
                text = "Calibrate with a known object or ruler to unlock cm, m, and inches.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }

            Text(
              text = String.format(
                Locale.US,
                "A(%.0f, %.0f) → B(%.0f, %.0f)  •  Δ(%.0f, %.0f)",
                activeLine.pointA.x,
                activeLine.pointA.y,
                activeLine.pointB.x,
                activeLine.pointB.y,
                activeLine.deltaX,
                activeLine.deltaY,
              ),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      // Multi-Line List
      if (lines.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "Measurements (${lines.size})",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          lines.forEach { line ->
            val isSelected = line.id == activeLine?.id
            val distText = CalibrationEngine.formatDistanceLabel(
              pixelDistance = line.pixelDistance.toDouble(),
              calibration = calibration,
            )
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
              } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
              },
              border = if (isSelected) {
                BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
              } else {
                null
              },
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelectLine(line.id) }
                .testTag("line_row_${line.id}"),
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = line.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text = "$distText  (${String.format(Locale.US, "%.0f px", line.pixelDistance)})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Row {
                  IconButton(
                    onClick = { onRenameLine(line) },
                    modifier = Modifier.testTag("rename_line_${line.id}"),
                  ) {
                    Icon(
                      imageVector = Icons.Filled.Edit,
                      contentDescription = "Rename ${line.name}",
                      modifier = Modifier.size(18.dp),
                    )
                  }
                  IconButton(
                    onClick = { onDeleteLine(line.id) },
                    modifier = Modifier.testTag("delete_line_${line.id}"),
                  ) {
                    Icon(
                      imageVector = Icons.Filled.Delete,
                      contentDescription = "Delete ${line.name}",
                      tint = MaterialTheme.colorScheme.error,
                      modifier = Modifier.size(18.dp),
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun MetricChip(label: String, value: String) {
  AssistChip(
    onClick = {},
    label = {
      Text(
        text = "$label: $value",
        style = MaterialTheme.typography.labelSmall,
      )
    },
    colors = AssistChipDefaults.assistChipColors(
      containerColor = MaterialTheme.colorScheme.surface,
    ),
  )
}
