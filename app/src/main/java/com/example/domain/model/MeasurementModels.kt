package com.example.domain.model

import java.util.UUID
import kotlin.math.atan2
import kotlin.math.hypot

data class Point2D(
  val x: Float,
  val y: Float,
)

data class MeasurementLine(
  val id: String = UUID.randomUUID().toString(),
  val name: String,
  val pointA: Point2D,
  val pointB: Point2D,
  val createdAt: Long = System.currentTimeMillis(),
) {
  val deltaX: Float
    get() = pointB.x - pointA.x

  val deltaY: Float
    get() = pointB.y - pointA.y

  val pixelDistance: Float
    get() = hypot(deltaX.toDouble(), deltaY.toDouble()).toFloat()

  val angleDegrees: Float
    get() {
      val deg = Math.toDegrees(atan2(-deltaY.toDouble(), deltaX.toDouble())).toFloat()
      return ((deg % 360f) + 360f) % 360f
    }
}

enum class CalibrationMethod(val displayName: String) {
  PIXEL_MODE("Pixel Mode"),
  KNOWN_OBJECT("Known Object"),
  REFERENCE_RULER("Reference Ruler"),
}

enum class LengthUnit(val symbol: String, val cmFactor: Double) {
  CENTIMETERS("cm", 1.0),
  MILLIMETERS("mm", 0.1),
  METERS("m", 100.0),
  INCHES("in", 2.54);

  fun toCentimeters(value: Double): Double = value * cmFactor

  fun fromCentimeters(cm: Double): Double = cm / cmFactor
}

data class KnownObjectPreset(
  val id: String,
  val name: String,
  val lengthCm: Double,
  val subtitle: String,
)

val DefaultKnownObjects = listOf(
  KnownObjectPreset("credit_card_w", "Credit / ID Card (Width)", 8.56, "ISO/IEC 7810 ID-1 • 8.56 cm"),
  KnownObjectPreset("credit_card_h", "Credit / ID Card (Height)", 5.398, "ISO/IEC 7810 ID-1 • 5.40 cm"),
  KnownObjectPreset("us_quarter", "US Quarter Dollar", 2.426, "Diameter • 2.43 cm"),
  KnownObjectPreset("euro_1", "1 Euro Coin", 2.325, "Diameter • 2.33 cm"),
  KnownObjectPreset("a4_short", "A4 Paper (Short Edge)", 21.0, "ISO 216 • 21.00 cm"),
  KnownObjectPreset("a4_long", "A4 Paper (Long Edge)", 29.7, "ISO 216 • 29.70 cm"),
  KnownObjectPreset("aa_battery", "AA Battery (Length)", 5.05, "Standard IEC R6 • 5.05 cm"),
)

data class CalibrationState(
  val method: CalibrationMethod = CalibrationMethod.PIXEL_MODE,
  val pixelsPerCm: Double? = null,
  val referenceLabel: String = "",
  val referenceLengthCm: Double? = null,
  val referencePointA: Point2D? = null,
  val referencePointB: Point2D? = null,
) {
  val isCalibrated: Boolean
    get() = method != CalibrationMethod.PIXEL_MODE && (pixelsPerCm ?: 0.0) > 0.0

  val referencePixelLength: Float?
    get() {
      val a = referencePointA ?: return null
      val b = referencePointB ?: return null
      return hypot((b.x - a.x).toDouble(), (b.y - a.y).toDouble()).toFloat()
    }
}

data class CalibratedDistance(
  val pixels: Double,
  val centimeters: Double,
  val meters: Double,
  val inches: Double,
)

enum class ThemeMode(val label: String) {
  DARK("Dark"),
  LIGHT("Light"),
  SYSTEM("System"),
}

enum class InteractionTool {
  MEASURE,
  CALIBRATE_SPAN,
}

enum class WorkspaceMode {
  LIVE_CAMERA,
  PHOTO_CANVAS,
}
