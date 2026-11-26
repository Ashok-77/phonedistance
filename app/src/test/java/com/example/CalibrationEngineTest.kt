package com.example

import com.example.domain.model.CalibrationEngine
import com.example.domain.model.CalibrationMethod
import com.example.domain.model.CalibrationState
import com.example.domain.model.LengthUnit
import com.example.domain.model.Point2D
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalibrationEngineTest {

  @Test
  fun `pixel mode never fabricates real world measurements`() {
    val uncalibrated = CalibrationState(method = CalibrationMethod.PIXEL_MODE, pixelsPerCm = null)
    assertFalse(uncalibrated.isCalibrated)
    assertNull(CalibrationEngine.measure(500.0, uncalibrated))
    assertEquals("500.0 px", CalibrationEngine.formatDistanceLabel(500.0, uncalibrated))
  }

  @Test
  fun `known object calibration computes accurate scale and converts cm m inches`() {
    // Credit card width = 8.56 cm spanning 856 pixels => 100 px/cm
    val pxPerCm = CalibrationEngine.computePixelsPerCm(
      pointA = Point2D(100f, 200f),
      pointB = Point2D(956f, 200f),
      actualLength = 8.56,
      unit = LengthUnit.CENTIMETERS,
    )
    assertNotNull(pxPerCm)
    assertEquals(100.0, pxPerCm!!, 0.001)

    val cal = CalibrationState(
      method = CalibrationMethod.KNOWN_OBJECT,
      pixelsPerCm = pxPerCm,
      referenceLabel = "Credit Card",
      referenceLengthCm = 8.56,
    )
    assertTrue(cal.isCalibrated)

    // 254 pixels at 100 px/cm => 2.54 cm = 0.0254 m = 1.0 in
    val result = CalibrationEngine.measure(254.0, cal)
    assertNotNull(result)
    assertEquals(2.54, result!!.centimeters, 0.001)
    assertEquals(0.0254, result.meters, 0.0001)
    assertEquals(1.0, result.inches, 0.001)
  }

  @Test
  fun `reference ruler in inches converts to pixels per cm accurately`() {
    // 10 inches = 25.4 cm spanning 1270 px => 50 px/cm
    val pxPerCm = CalibrationEngine.computePixelsPerCm(
      pointA = Point2D(0f, 0f),
      pointB = Point2D(1270f, 0f),
      actualLength = 10.0,
      unit = LengthUnit.INCHES,
    )
    assertNotNull(pxPerCm)
    assertEquals(50.0, pxPerCm!!, 0.001)
  }

  @Test
  fun `zero or negative length or identical points return null`() {
    assertNull(CalibrationEngine.computePixelsPerCm(Point2D(10f, 10f), Point2D(10f, 10f), 5.0, LengthUnit.CENTIMETERS))
    assertNull(CalibrationEngine.computePixelsPerCm(Point2D(0f, 0f), Point2D(100f, 0f), 0.0, LengthUnit.CENTIMETERS))
    assertNull(CalibrationEngine.computePixelsPerCm(Point2D(0f, 0f), Point2D(100f, 0f), -4.0, LengthUnit.MILLIMETERS))
  }
}
