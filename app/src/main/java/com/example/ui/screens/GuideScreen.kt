package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.repository.UserPreferences
import com.example.domain.model.LengthUnit
import com.example.domain.model.ThemeMode
import com.example.ui.theme.CalibrationAmber

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GuideScreen(
  preferences: UserPreferences,
  onThemeModeChanged: (ThemeMode) -> Unit,
  onPreferredUnitChanged: (LengthUnit) -> Unit,
  onToggleGrid: () -> Unit,
  onToggleLoupe: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 16.dp, vertical = 14.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Text(
      text = "Instrument Guide & Settings",
      style = MaterialTheme.typography.headlineSmall,
    )

    Image(
      painter = painterResource(id = R.drawable.img_calibration_guide),
      contentDescription = "Overhead flat plane measurement setup diagram",
      contentScale = ContentScale.Crop,
      modifier = Modifier
        .fillMaxWidth()
        .height(170.dp)
        .clip(RoundedCornerShape(18.dp)),
    )

    // Appearance & Instrument Preferences
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Text("Display & Theme", style = MaterialTheme.typography.titleMedium)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
          ThemeMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
              selected = preferences.themeMode == mode,
              onClick = { onThemeModeChanged(mode) },
              shape = SegmentedButtonDefaults.itemShape(index = index, count = ThemeMode.entries.size),
              modifier = Modifier.testTag("guide_theme_${mode.name.lowercase()}"),
            ) {
              Text(mode.label)
            }
          }
        }

        Text("Primary Calibrated Unit", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          LengthUnit.entries.forEach { unit ->
            FilterChip(
              selected = preferences.preferredUnit == unit,
              onClick = { onPreferredUnitChanged(unit) },
              label = { Text(unit.symbol) },
              modifier = Modifier.testTag("guide_unit_${unit.symbol}"),
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Precision Reticle Grid", style = MaterialTheme.typography.titleSmall)
            Text(
              "Display alignment grid and center optical crosshair",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Switch(
            checked = preferences.showGridOverlay,
            onCheckedChange = { onToggleGrid() },
            modifier = Modifier.testTag("guide_grid_switch"),
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Drag Magnifier Loupe", style = MaterialTheme.typography.titleSmall)
            Text(
              "2.6× zoom loupe above your finger when dragging handles on photos",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Switch(
            checked = preferences.enableMagnifierLoupe,
            onCheckedChange = { onToggleLoupe() },
            modifier = Modifier.testTag("guide_loupe_switch"),
          )
        }
      }
    }

    // Calibration Methods Explained
    InfoCard(
      icon = Icons.Outlined.Straighten,
      iconTint = MaterialTheme.colorScheme.primary,
      title = "Three Calibration Modes",
      bullets = listOf(
        "Known Object: Select an ISO ID/Credit Card (8.56 cm), coin, A4 paper, or enter any custom object length.",
        "Reference Ruler: Place a physical metric or imperial ruler in frame, tap two markings, and enter the real span.",
        "Pixel Mode: Default uncalibrated mode showing exact pixel distance (px) without guessing real-world units.",
      ),
    )

    // Optical Physics Disclaimer
    InfoCard(
      icon = Icons.Outlined.WarningAmber,
      iconTint = CalibrationAmber,
      title = "Perspective, Tilt & Plane Geometry",
      bullets = listOf(
        stringResource(R.string.accuracy_disclaimer),
        "Hold the camera parallel to the surface (90° overhead) to minimize keystone perspective distortion.",
        "Place the calibration reference at the exact same distance/depth from the camera as the object being measured.",
      ),
    )

    // Privacy & Local Security
    InfoCard(
      icon = Icons.Outlined.Lock,
      iconTint = MaterialTheme.colorScheme.tertiary,
      title = "Local-First Privacy Architecture",
      bullets = listOf(
        "All camera frames and imported photos are processed 100% locally in app-private storage.",
        "Zero cloud uploads, zero analytics, and zero background video recording.",
        "Camera permission is requested only when you choose to activate the camera.",
      ),
    )
  }
}

@Composable
private fun InfoCard(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  iconTint: androidx.compose.ui.graphics.Color,
  title: String,
  bullets: List<String>,
) {
  Surface(
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surface,
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
        Icon(imageVector = icon, contentDescription = null, tint = iconTint)
        Text(text = title, style = MaterialTheme.typography.titleMedium)
      }
      bullets.forEach { bullet ->
        Text(
          text = "• $bullet",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}
