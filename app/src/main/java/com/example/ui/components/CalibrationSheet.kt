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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.CalibrationEngine
import com.example.domain.model.CalibrationMethod
import com.example.domain.model.CalibrationState
import com.example.domain.model.DefaultKnownObjects
import com.example.domain.model.LengthUnit
import com.example.domain.model.MeasurementLine
import com.example.domain.model.Point2D
import com.example.ui.theme.CalibrationAmber
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CalibrationSheet(
  calibration: CalibrationState,
  pendingCalPointA: Point2D?,
  pendingCalPointB: Point2D?,
  selectedLine: MeasurementLine?,
  onDismiss: () -> Unit,
  onStartPickSpanOnCanvas: () -> Unit,
  onUseSelectedLineAsSpan: () -> Unit,
  onApplyCalibration: (
    method: CalibrationMethod,
    referenceLabel: String,
    actualLength: Double,
    unit: LengthUnit,
    pointA: Point2D?,
    pointB: Point2D?,
  ) -> Boolean,
  onResetToPixelMode: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var selectedMethod by remember(calibration.method) {
    mutableStateOf(
      if (calibration.method == CalibrationMethod.PIXEL_MODE) CalibrationMethod.KNOWN_OBJECT
      else calibration.method
    )
  }
  var selectedPresetId by remember { mutableStateOf(DefaultKnownObjects.first().id) }
  var isCustomObject by remember { mutableStateOf(false) }
  var customObjectName by remember { mutableStateOf("Reference Object") }
  var lengthInput by remember(calibration) {
    mutableStateOf(
      calibration.referenceLengthCm?.let { String.format(Locale.US, "%.2f", it) }
        ?: String.format(Locale.US, "%.2f", DefaultKnownObjects.first().lengthCm)
    )
  }
  var selectedUnit by remember { mutableStateOf(LengthUnit.CENTIMETERS) }

  val refA = pendingCalPointA ?: calibration.referencePointA
  val refB = pendingCalPointB ?: calibration.referencePointB
  val spanPx = if (refA != null && refB != null) CalibrationEngine.pixelDistance(refA, refB) else null

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp)
        .padding(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.Tune,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
        )
        Column {
          Text(
            text = "Calibration Engine",
            style = MaterialTheme.typography.headlineSmall,
          )
          Text(
            text = "Map image pixels to real-world units on the same flat plane",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      // 3-Mode Selector
      val methods = listOf(
        CalibrationMethod.KNOWN_OBJECT,
        CalibrationMethod.REFERENCE_RULER,
        CalibrationMethod.PIXEL_MODE,
      )
      SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        methods.forEachIndexed { index, method ->
          SegmentedButton(
            selected = selectedMethod == method,
            onClick = {
              selectedMethod = method
              if (method == CalibrationMethod.KNOWN_OBJECT && !isCustomObject) {
                val preset = DefaultKnownObjects.firstOrNull { it.id == selectedPresetId }
                if (preset != null) {
                  selectedUnit = LengthUnit.CENTIMETERS
                  lengthInput = String.format(Locale.US, "%.3f", preset.lengthCm)
                }
              } else if (method == CalibrationMethod.REFERENCE_RULER) {
                selectedUnit = LengthUnit.CENTIMETERS
                if (lengthInput.isBlank()) lengthInput = "10.0"
              }
            },
            shape = SegmentedButtonDefaults.itemShape(index = index, count = methods.size),
            modifier = Modifier.testTag("calibration_tab_${method.name.lowercase()}"),
          ) {
            Text(
              text = when (method) {
                CalibrationMethod.KNOWN_OBJECT -> "Known Object"
                CalibrationMethod.REFERENCE_RULER -> "Ruler"
                CalibrationMethod.PIXEL_MODE -> "Pixel Mode"
              },
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.SemiBold,
            )
          }
        }
      }

      if (selectedMethod == CalibrationMethod.PIXEL_MODE) {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Outlined.GridOn,
                contentDescription = null,
                tint = CalibrationAmber,
              )
              Text(
                text = "Pixel Mode (Uncalibrated)",
                style = MaterialTheme.typography.titleMedium,
              )
            }
            Text(
              text = "Distances are reported strictly in raw image pixels (px). No real-world centimeters, meters, or inches are claimed without a plane reference.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
              onClick = {
                onResetToPixelMode()
                onDismiss()
              },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("confirm_pixel_mode_button"),
            ) {
              Text("Use Pixel Mode")
            }
          }
        }
      } else {
        // Step 1: Span on image
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          border = BorderStroke(
            width = 1.dp,
            color = if (spanPx != null) MaterialTheme.colorScheme.tertiary else CalibrationAmber,
          ),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = if (selectedMethod == CalibrationMethod.KNOWN_OBJECT) {
                  "1. Mark Known Object Span"
                } else {
                  "1. Mark Two Points on Ruler"
                },
                style = MaterialTheme.typography.titleSmall,
              )
              if (spanPx != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                  Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(16.dp),
                  )
                  Text(
                    text = String.format(Locale.US, "%.1f px", spanPx),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                  )
                }
              }
            }
            Text(
              text = if (spanPx == null) {
                "Tap below to select two points on the reference object or ruler in your image."
              } else {
                "Reference span set (${String.format(Locale.US, "%.1f px", spanPx)}). You can adjust R1/R2 directly on the canvas."
              },
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              OutlinedButton(
                onClick = {
                  onStartPickSpanOnCanvas()
                  onDismiss()
                },
                modifier = Modifier
                  .weight(1f)
                  .testTag("pick_calibration_points_button"),
              ) {
                Icon(Icons.Filled.TouchApp, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (spanPx == null) "Pick 2 Points" else "Re-pick Points")
              }
              if (selectedLine != null) {
                OutlinedButton(
                  onClick = onUseSelectedLineAsSpan,
                  modifier = Modifier
                    .weight(1f)
                    .testTag("use_active_line_calibration_button"),
                ) {
                  Icon(Icons.Filled.Straighten, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(Modifier.width(6.dp))
                  Text("Use ${selectedLine.name}")
                }
              }
            }
          }
        }

        // Step 2: Reference dimension
        if (selectedMethod == CalibrationMethod.KNOWN_OBJECT) {
          Text(
            text = "2. Select Reference Object",
            style = MaterialTheme.typography.titleSmall,
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            DefaultKnownObjects.forEach { preset ->
              val selected = !isCustomObject && selectedPresetId == preset.id
              FilterChip(
                selected = selected,
                onClick = {
                  isCustomObject = false
                  selectedPresetId = preset.id
                  selectedUnit = LengthUnit.CENTIMETERS
                  lengthInput = String.format(Locale.US, "%.3f", preset.lengthCm)
                },
                label = { Text(preset.name, style = MaterialTheme.typography.bodySmall) },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Outlined.CreditCard,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                  )
                },
                modifier = Modifier.testTag("preset_${preset.id}"),
              )
            }
            FilterChip(
              selected = isCustomObject,
              onClick = { isCustomObject = true },
              label = { Text("Custom Object", style = MaterialTheme.typography.bodySmall) },
              modifier = Modifier.testTag("preset_custom"),
            )
          }

          if (isCustomObject) {
            OutlinedTextField(
              value = customObjectName,
              onValueChange = { customObjectName = it },
              label = { Text("Object Name") },
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("custom_object_name_input"),
            )
          }
        } else {
          Text(
            text = "2. Enter Ruler Span Distance",
            style = MaterialTheme.typography.titleSmall,
          )
          Text(
            text = "Enter the exact distance between the two ruler tick marks you tapped (for example, 10 cm or 6 in).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        Row(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth(),
        ) {
          OutlinedTextField(
            value = lengthInput,
            onValueChange = { lengthInput = it },
            label = { Text("Actual Length") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier
              .weight(1f)
              .testTag("calibration_length_input"),
          )
          FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            LengthUnit.entries.forEach { unit ->
              FilterChip(
                selected = selectedUnit == unit,
                onClick = { selectedUnit = unit },
                label = { Text(unit.symbol) },
                modifier = Modifier.testTag("unit_chip_${unit.symbol}"),
              )
            }
          }
        }

        Button(
          onClick = {
            val numeric = lengthInput.toDoubleOrNull() ?: -1.0
            val label = when {
              selectedMethod == CalibrationMethod.REFERENCE_RULER ->
                "Ruler ($lengthInput ${selectedUnit.symbol})"
              isCustomObject -> customObjectName.ifBlank { "Custom Object" }
              else -> DefaultKnownObjects.firstOrNull { it.id == selectedPresetId }?.name ?: "Known Object"
            }
            val success = onApplyCalibration(
              selectedMethod,
              label,
              numeric,
              selectedUnit,
              refA,
              refB,
            )
            if (success) onDismiss()
          },
          enabled = spanPx != null && (lengthInput.toDoubleOrNull() ?: 0.0) > 0.0,
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("apply_calibration_button"),
        ) {
          Text("Apply Calibration Scale")
        }
      }

      // Mandatory Optical Plane & Perspective Disclaimer
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.Top,
        ) {
          Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
          )
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = "Single-Plane Accuracy Notice",
              style = MaterialTheme.typography.titleSmall,
            )
            Text(
              text = stringResource(R.string.accuracy_disclaimer),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}
