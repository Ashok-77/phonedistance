package com.example.domain.model

import java.util.Locale
import kotlin.math.hypot

object CalibrationEngine {

  private const val MIN_PIXEL_SPAN = 1.0

  fun pixelDistance(a: Point2D, b: Point2D): Double {
    return hypot((b.x - a.x).toDouble(), (b.y - a.y).toDouble())
  }

  /**
   * Computes pixels-per-centimeter given two image-space points and the real-world length
   * between them. Returns null if inputs are invalid or span is too tiny to calibrate safely.
   */
  fun computePixelsPerCm(
    pointA: Point2D,
    pointB: Point2D,
    actualLength: Double,
    unit: LengthUnit,
  ): Double? {
    if (!actualLength.isFinite() || actualLength <= 0.0) return null
    val px = pixelDistance(pointA, pointB)
    if (!px.isFinite() || px < MIN_PIXEL_SPAN) return null
    val cm = unit.toCentimeters(actualLength)
    if (!cm.isFinite() || cm <= 0.0) return null
    return px / cm
  }

  /**
   * Returns calibrated real-world distances ONLY if calibration is valid and active.
   * Never fabricates or guesses real-world values in Pixel Mode.
   */
  fun measure(
    pixelDistance: Double,
    calibration: CalibrationState,
  ): CalibratedDistance? {
    if (!calibration.isCalibrated) return null
    val pxPerCm = calibration.pixelsPerCm ?: return null
    if (!pxPerCm.isFinite() || pxPerCm <= 0.0) return null

    val cm = pixelDistance / pxPerCm
    val meters = cm / 100.0
    val inches = cm / 2.54
    return CalibratedDistance(
      pixels = pixelDistance,
      centimeters = cm,
      meters = meters,
      inches = inches,
    )
  }

  fun formatDistanceLabel(
    pixelDistance: Double,
    calibration: CalibrationState,
    preferredUnit: LengthUnit = LengthUnit.CENTIMETERS,
  ): String {
    val calibrated = measure(pixelDistance, calibration)
    return if (calibrated == null) {
      String.format(Locale.US, "%.1f px", pixelDistance)
    } else {
      when (preferredUnit) {
        LengthUnit.CENTIMETERS -> String.format(Locale.US, "%.2f cm", calibrated.centimeters)
        LengthUnit.MILLIMETERS -> String.format(Locale.US, "%.1f mm", calibrated.centimeters * 10.0)
        LengthUnit.METERS -> String.format(Locale.US, "%.3f m", calibrated.meters)
        LengthUnit.INCHES -> String.format(Locale.US, "%.2f in", calibrated.inches)
      }
    }
  }
}
