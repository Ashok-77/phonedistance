package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.R

@Composable
fun OnboardingDialog(
  onDismiss: () -> Unit,
) {
  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
    ) {
      Column(
        modifier = Modifier
          .verticalScroll(rememberScrollState())
          .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Image(
          painter = painterResource(id = R.drawable.img_calibration_guide),
          contentDescription = "Flat plane camera measurement calibration diagram",
          contentScale = ContentScale.Crop,
          modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(16.dp)),
        )

        Text(
          text = "Welcome to VisionMeasure",
          style = MaterialTheme.typography.headlineSmall,
        )
        Text(
          text = "A precision local camera instrument for manual 2D surface measurements.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        StepRow("1", "Capture or Import", "Hold your device flat and parallel over the surface, then freeze a photo or measure live.")
        StepRow("2", "Tap A → Tap B", "Tap two endpoints and drag handles using the precision magnifier loupe.")
        StepRow("3", "Calibrate on Same Plane", "Use a credit card, coin, or physical ruler in the same plane to unlock cm, m, and inches.")

        Button(
          onClick = onDismiss,
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("onboarding_start_button"),
        ) {
          Text("Start Measuring")
        }
      }
    }
  }
}

@Composable
private fun StepRow(number: String, title: String, body: String) {
  Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    Surface(
      shape = RoundedCornerShape(8.dp),
      color = MaterialTheme.colorScheme.primaryContainer,
    ) {
      Text(
        text = number,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
      )
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(title, style = MaterialTheme.typography.titleSmall)
      Text(
        text = body,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
