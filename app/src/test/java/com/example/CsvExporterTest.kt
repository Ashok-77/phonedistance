package com.example

import com.example.data.local.CsvExporter
import com.example.data.local.MeasurementLineEntity
import com.example.data.local.MeasurementSessionEntity
import com.example.data.local.SessionWithLines
import com.example.domain.model.CalibrationMethod
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExporterTest {

  @Test
  fun `generateCsv outputs header, calibrated values, and escapes commas`() {
    val session = SessionWithLines(
      session = MeasurementSessionEntity(
        id = "s1",
        title = "Desk, Top",
        createdAt = 1700000000000L,
        calibrationMethod = CalibrationMethod.REFERENCE_RULER.name,
        referenceLabel = "Steel Ruler",
        pixelsPerCm = 50.0,
        referenceLengthCm = 10.0,
        refAX = 0f,
        refAY = 0f,
        refBX = 500f,
        refBY = 0f,
        imageWidth = 1600,
        imageHeight = 1200,
        imagePath = null,
      ),
      lines = listOf(
        MeasurementLineEntity(
          id = "l1",
          sessionId = "s1",
          name = "Width \"A\"",
          ax = 100f,
          ay = 100f,
          bx = 600f,
          by = 100f,
          sortOrder = 0,
        ),
      ),
    )

    val csv = CsvExporter.generateCsv(listOf(session))
    assertTrue(csv.contains("SessionID,SessionTitle,TimestampISO,CalibrationMode"))
    assertTrue(csv.contains("\"Desk, Top\""))
    assertTrue(csv.contains("\"Width \"\"A\"\"\""))
    assertTrue(csv.contains("500.00"))
    assertTrue(csv.contains("10.000")) // 500 px / 50 px/cm = 10.000 cm
  }

  @Test
  fun `generateCsv leaves metric columns blank in Pixel Mode`() {
    val session = SessionWithLines(
      session = MeasurementSessionEntity(
        id = "s2",
        title = "Raw Pixels",
        createdAt = 1700000000000L,
        calibrationMethod = CalibrationMethod.PIXEL_MODE.name,
        referenceLabel = "",
        pixelsPerCm = null,
        referenceLengthCm = null,
        refAX = null,
        refAY = null,
        refBX = null,
        refBY = null,
        imageWidth = 1080,
        imageHeight = 1920,
        imagePath = null,
      ),
      lines = listOf(
        MeasurementLineEntity(
          id = "l2",
          sessionId = "s2",
          name = "Line 1",
          ax = 0f,
          ay = 0f,
          bx = 300f,
          by = 400f,
          sortOrder = 0,
        ),
      ),
    )

    val csv = CsvExporter.generateCsv(listOf(session))
    assertTrue(csv.contains("500.00,,,"))
  }
}
