package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.CalibrationEngine
import com.example.domain.model.CalibrationState
import com.example.domain.model.InteractionTool
import com.example.domain.model.LengthUnit
import com.example.domain.model.MeasurementLine
import com.example.domain.model.Point2D
import com.example.ui.theme.CalibrationAmber
import com.example.ui.theme.InstrumentBlueActive
import com.example.ui.theme.InstrumentCyanGlow
import com.example.ui.theme.InstrumentCyanLine
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

private sealed interface DragTarget {
  data class LineEndpoint(val lineId: String, val isPointA: Boolean) : DragTarget
  data class CalibrationEndpoint(val isPointA: Boolean) : DragTarget
}

private data class ViewportTransform(
  val imageWidth: Float,
  val imageHeight: Float,
  val viewWidth: Float,
  val viewHeight: Float,
  val zoom: Float,
  val pan: Offset,
) {
  val baseScale: Float = if (imageWidth > 0f && imageHeight > 0f && viewWidth > 0f && viewHeight > 0f) {
    minOf(viewWidth / imageWidth, viewHeight / imageHeight)
  } else {
    1f
  }
  val effectiveScale: Float = baseScale * zoom
  val drawnWidth: Float = imageWidth * effectiveScale
  val drawnHeight: Float = imageHeight * effectiveScale
  val originX: Float = (viewWidth - drawnWidth) / 2f + pan.x
  val originY: Float = (viewHeight - drawnHeight) / 2f + pan.y

  fun imageToScreen(p: Point2D): Offset {
    return Offset(
      x = originX + p.x * effectiveScale,
      y = originY + p.y * effectiveScale,
    )
  }

  fun screenToImage(offset: Offset): Point2D {
    val ix = ((offset.x - originX) / effectiveScale).coerceIn(0f, imageWidth)
    val iy = ((offset.y - originY) / effectiveScale).coerceIn(0f, imageHeight)
    return Point2D(ix, iy)
  }
}

@Composable
fun MeasureCanvas(
  bitmap: Bitmap?,
  canvasWidth: Int,
  canvasHeight: Int,
  lines: List<MeasurementLine>,
  pendingPointA: Point2D?,
  selectedLineId: String?,
  calibration: CalibrationState,
  pendingCalPointA: Point2D?,
  pendingCalPointB: Point2D?,
  interactionTool: InteractionTool,
  preferredUnit: LengthUnit,
  showGridOverlay: Boolean,
  enableMagnifierLoupe: Boolean,
  onCanvasTapped: (Point2D) -> Unit,
  onHandleDragged: (lineId: String, isPointA: Boolean, Point2D) -> Unit,
  onCalibrationHandleDragged: (isPointA: Boolean, Point2D) -> Unit,
  onViewSizeChanged: (IntSize) -> Unit,
  modifier: Modifier = Modifier,
) {
  val density = LocalDensity.current
  val hitRadiusPx = with(density) { 28.dp.toPx() }
  val tapSlopPx = with(density) { 10.dp.toPx() }
  val loupeRadiusPx = with(density) { 56.dp.toPx() }

  var viewSize by remember { mutableStateOf(IntSize.Zero) }
  var zoom by remember(bitmap) { mutableFloatStateOf(1f) }
  var pan by remember(bitmap) { mutableStateOf(Offset.Zero) }
  var activeDragPointScreen by remember { mutableStateOf<Offset?>(null) }
  var activeDragImagePoint by remember { mutableStateOf<Point2D?>(null) }

  val imageBitmap = remember(bitmap) { bitmap?.asImageBitmap() }

  val currentLines by rememberUpdatedState(lines)
  val currentCalA by rememberUpdatedState(pendingCalPointA ?: calibration.referencePointA)
  val currentCalB by rememberUpdatedState(pendingCalPointB ?: calibration.referencePointB)
  val currentTool by rememberUpdatedState(interactionTool)
  val currentTapCallback by rememberUpdatedState(onCanvasTapped)
  val currentDragCallback by rememberUpdatedState(onHandleDragged)
  val currentCalDragCallback by rememberUpdatedState(onCalibrationHandleDragged)
  val currentCanvasWidth by rememberUpdatedState(canvasWidth)
  val currentCanvasHeight by rememberUpdatedState(canvasHeight)

  val infiniteTransition = rememberInfiniteTransition(label = "reticlePulse")
  val pulseProgress by infiniteTransition.animateFloat(
    initialValue = 0.7f,
    targetValue = 1.25f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 900),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "pulse",
  )

  val labelTextPaint = remember {
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = android.graphics.Color.WHITE
      textAlign = Paint.Align.CENTER
      typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }
  }
  val labelBgPaint = remember {
    Paint(Paint.ANTI_ALIAS_FLAG)
  }

  val transform = remember(
    currentCanvasWidth,
    currentCanvasHeight,
    viewSize,
    zoom,
    pan,
  ) {
    ViewportTransform(
      imageWidth = currentCanvasWidth.toFloat().coerceAtLeast(1f),
      imageHeight = currentCanvasHeight.toFloat().coerceAtLeast(1f),
      viewWidth = viewSize.width.toFloat().coerceAtLeast(1f),
      viewHeight = viewSize.height.toFloat().coerceAtLeast(1f),
      zoom = zoom,
      pan = pan,
    )
  }
  val currentTransform by rememberUpdatedState(transform)

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(if (bitmap != null) Color.Black else Color.Transparent)
      .onSizeChanged { newSize ->
        viewSize = newSize
        onViewSizeChanged(newSize)
      }
      .testTag("measure_canvas")
      .pointerInput(bitmap != null) {
        awaitEachGesture {
          val firstDown = awaitFirstDown(requireUnconsumed = false)
          val startPos = firstDown.position
          val t = currentTransform

          // Hit-test existing handles in screen space
          var dragTarget: DragTarget? = null
          var bestDist = hitRadiusPx

          if (currentTool == InteractionTool.CALIBRATE_SPAN || currentCalA != null || currentCalB != null) {
            currentCalA?.let { pt ->
              val d = hypot(t.imageToScreen(pt).x - startPos.x, t.imageToScreen(pt).y - startPos.y)
              if (d < bestDist) {
                bestDist = d
                dragTarget = DragTarget.CalibrationEndpoint(isPointA = true)
              }
            }
            currentCalB?.let { pt ->
              val d = hypot(t.imageToScreen(pt).x - startPos.x, t.imageToScreen(pt).y - startPos.y)
              if (d < bestDist) {
                bestDist = d
                dragTarget = DragTarget.CalibrationEndpoint(isPointA = false)
              }
            }
          }

          currentLines.forEach { line ->
            val dA = hypot(t.imageToScreen(line.pointA).x - startPos.x, t.imageToScreen(line.pointA).y - startPos.y)
            if (dA < bestDist) {
              bestDist = dA
              dragTarget = DragTarget.LineEndpoint(line.id, isPointA = true)
            }
            val dB = hypot(t.imageToScreen(line.pointB).x - startPos.x, t.imageToScreen(line.pointB).y - startPos.y)
            if (dB < bestDist) {
              bestDist = dB
              dragTarget = DragTarget.LineEndpoint(line.id, isPointA = false)
            }
          }

          var totalMoved = 0f
          var multiTouchUsed = false

          while (true) {
            val event = awaitPointerEvent()
            val pressedChanges = event.changes.filter { it.pressed }

            if (pressedChanges.isEmpty()) {
              // Gesture released
              if (dragTarget == null && !multiTouchUsed && totalMoved < tapSlopPx) {
                val tappedImagePt = currentTransform.screenToImage(startPos)
                currentTapCallback(tappedImagePt)
              }
              activeDragPointScreen = null
              activeDragImagePoint = null
              break
            }

            if (pressedChanges.size >= 2 && bitmap != null) {
              multiTouchUsed = true
              activeDragPointScreen = null
              activeDragImagePoint = null
              val zoomChange = event.calculateZoom()
              val panChange = event.calculatePan()
              val nextZoom = (zoom * zoomChange).coerceIn(1f, 5f)
              val maxPanX = ((currentTransform.imageWidth * currentTransform.baseScale * nextZoom) - viewSize.width * 0.5f).coerceAtLeast(0f)
              val maxPanY = ((currentTransform.imageHeight * currentTransform.baseScale * nextZoom) - viewSize.height * 0.5f).coerceAtLeast(0f)
              zoom = nextZoom
              pan = if (nextZoom <= 1.01f) {
                Offset.Zero
              } else {
                Offset(
                  x = (pan.x + panChange.x).coerceIn(-maxPanX, maxPanX),
                  y = (pan.y + panChange.y).coerceIn(-maxPanY, maxPanY),
                )
              }
              event.changes.forEach { it.consume() }
            } else if (pressedChanges.size == 1) {
              val change = pressedChanges.first()
              val delta = change.position - change.previousPosition
              totalMoved += delta.getDistance()

              val target = dragTarget
              if (target != null) {
                if (change.positionChanged()) change.consume()
                val imgPt = currentTransform.screenToImage(change.position)
                activeDragPointScreen = change.position
                activeDragImagePoint = imgPt
                when (target) {
                  is DragTarget.LineEndpoint -> {
                    currentDragCallback(target.lineId, target.isPointA, imgPt)
                  }
                  is DragTarget.CalibrationEndpoint -> {
                    currentCalDragCallback(target.isPointA, imgPt)
                  }
                }
              } else if (bitmap != null && zoom > 1.02f && totalMoved > tapSlopPx) {
                // Single-finger pan while zoomed in
                val maxPanX = ((currentTransform.drawnWidth) - viewSize.width * 0.5f).coerceAtLeast(0f)
                val maxPanY = ((currentTransform.drawnHeight) - viewSize.height * 0.5f).coerceAtLeast(0f)
                pan = Offset(
                  x = (pan.x + delta.x).coerceIn(-maxPanX, maxPanX),
                  y = (pan.y + delta.y).coerceIn(-maxPanY, maxPanY),
                )
                change.consume()
              }
            }
          }
        }
      },
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      // 1. Draw photo bitmap if frozen/imported
      if (imageBitmap != null) {
        drawImage(
          image = imageBitmap,
          dstOffset = IntOffset(transform.originX.roundToInt(), transform.originY.roundToInt()),
          dstSize = IntSize(
            transform.drawnWidth.roundToInt().coerceAtLeast(1),
            transform.drawnHeight.roundToInt().coerceAtLeast(1),
          ),
        )
      }

      // 2. Subtle precision drafting grid & center reticle
      if (showGridOverlay) {
        drawInstrumentGrid()
      }

      // 3. Draw Calibration Reference Span (Amber dashed)
      val calA = pendingCalPointA ?: calibration.referencePointA
      val calB = pendingCalPointB ?: calibration.referencePointB
      if (calA != null && calB != null) {
        val sA = transform.imageToScreen(calA)
        val sB = transform.imageToScreen(calB)
        drawCalibrationSpan(
          screenA = sA,
          screenB = sB,
          label = if (calibration.isCalibrated && calibration.referenceLengthCm != null) {
            "REF ${String.format(java.util.Locale.US, "%.2f cm", calibration.referenceLengthCm)}"
          } else {
            "CAL REF"
          },
          textPaint = labelTextPaint,
          bgPaint = labelBgPaint,
        )
      } else if (calA != null) {
        val sA = transform.imageToScreen(calA)
        drawEndpointHandle(sA, CalibrationAmber, "REF A", pulseProgress, labelTextPaint)
      }

      // 4. Draw all measurement lines
      lines.forEach { line ->
        val isSelected = line.id == selectedLineId
        val sA = transform.imageToScreen(line.pointA)
        val sB = transform.imageToScreen(line.pointB)
        val formatted = CalibrationEngine.formatDistanceLabel(
          pixelDistance = line.pixelDistance.toDouble(),
          calibration = calibration,
          preferredUnit = preferredUnit,
        )
        drawMeasurementVector(
          screenA = sA,
          screenB = sB,
          label = "${line.name}: $formatted",
          isSelected = isSelected,
          textPaint = labelTextPaint,
          bgPaint = labelBgPaint,
        )
      }

      // 5. Draw pending Point A (awaiting Point B)
      if (pendingPointA != null) {
        val sA = transform.imageToScreen(pendingPointA)
        drawCircle(
          color = InstrumentCyanGlow.copy(alpha = 0.25f),
          radius = 22.dp.toPx() * pulseProgress,
          center = sA,
        )
        drawEndpointHandle(sA, InstrumentCyanGlow, "A", 1f, labelTextPaint)
      }

      // 6. Precision Magnifier Loupe when dragging a handle over a photo
      val dragScreen = activeDragPointScreen
      val dragImg = activeDragImagePoint
      if (enableMagnifierLoupe && imageBitmap != null && dragScreen != null && dragImg != null) {
        drawMagnifierLoupe(
          imageBitmap = imageBitmap,
          imagePoint = dragImg,
          fingerScreen = dragScreen,
          loupeRadius = loupeRadiusPx,
        )
      }
    }

    if (bitmap != null && zoom > 1.05f) {
      Surface(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(12.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        tonalElevation = 4.dp,
      ) {
        TextButton(
          onClick = {
            zoom = 1f
            pan = Offset.Zero
          },
          modifier = Modifier.testTag("reset_zoom_button"),
        ) {
          Text(
            text = "${String.format(java.util.Locale.US, "%.1fx", zoom)} • Reset",
            style = MaterialTheme.typography.labelMedium,
          )
        }
      }
    }
  }
}

private fun DrawScope.drawInstrumentGrid() {
  val step = 48.dp.toPx()
  val gridColor = Color.White.copy(alpha = 0.10f)
  var x = step
  while (x < size.width) {
    drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
    x += step
  }
  var y = step
  while (y < size.height) {
    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
    y += step
  }
  // Center optical reticle
  val cx = size.width / 2f
  val cy = size.height / 2f
  val r = 14.dp.toPx()
  val reticleColor = InstrumentCyanGlow.copy(alpha = 0.35f)
  drawCircle(reticleColor, radius = r, center = Offset(cx, cy), style = Stroke(1.5.dp.toPx()))
  drawLine(reticleColor, Offset(cx - r * 1.4f, cy), Offset(cx + r * 1.4f, cy), strokeWidth = 1.2.dp.toPx())
  drawLine(reticleColor, Offset(cx, cy - r * 1.4f), Offset(cx, cy + r * 1.4f), strokeWidth = 1.2.dp.toPx())
}

private fun DrawScope.drawMeasurementVector(
  screenA: Offset,
  screenB: Offset,
  label: String,
  isSelected: Boolean,
  textPaint: Paint,
  bgPaint: Paint,
) {
  val mainColor = if (isSelected) InstrumentCyanGlow else InstrumentCyanLine
  val glowColor = if (isSelected) InstrumentBlueActive.copy(alpha = 0.35f) else InstrumentCyanLine.copy(alpha = 0.22f)

  // Soft outer glow
  drawLine(
    color = glowColor,
    start = screenA,
    end = screenB,
    strokeWidth = if (isSelected) 9.dp.toPx() else 6.dp.toPx(),
    cap = StrokeCap.Round,
  )
  // Core line
  drawLine(
    color = mainColor,
    start = screenA,
    end = screenB,
    strokeWidth = if (isSelected) 3.dp.toPx() else 2.2.dp.toPx(),
    cap = StrokeCap.Round,
  )

  // Perpendicular caliper end-caps
  val angle = atan2((screenB.y - screenA.y).toDouble(), (screenB.x - screenA.x).toDouble())
  val perpX = -sin(angle).toFloat() * 10.dp.toPx()
  val perpY = cos(angle).toFloat() * 10.dp.toPx()
  drawLine(mainColor, Offset(screenA.x - perpX, screenA.y - perpY), Offset(screenA.x + perpX, screenA.y + perpY), strokeWidth = 2.dp.toPx())
  drawLine(mainColor, Offset(screenB.x - perpX, screenB.y - perpY), Offset(screenB.x + perpX, screenB.y + perpY), strokeWidth = 2.dp.toPx())

  drawEndpointHandle(screenA, mainColor, "A", 1f, textPaint)
  drawEndpointHandle(screenB, mainColor, "B", 1f, textPaint)

  // Pill label at midpoint
  val mid = Offset((screenA.x + screenB.x) / 2f, (screenA.y + screenB.y) / 2f - 18.dp.toPx())
  drawPillLabel(mid, label, if (isSelected) Color(0xFF0A2233) else Color(0xFF0E1722), mainColor, textPaint, bgPaint)
}

private fun DrawScope.drawCalibrationSpan(
  screenA: Offset,
  screenB: Offset,
  label: String,
  textPaint: Paint,
  bgPaint: Paint,
) {
  drawLine(
    color = CalibrationAmber,
    start = screenA,
    end = screenB,
    strokeWidth = 2.8.dp.toPx(),
    cap = StrokeCap.Round,
    pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 10f), 0f),
  )
  drawEndpointHandle(screenA, CalibrationAmber, "R1", 1f, textPaint)
  drawEndpointHandle(screenB, CalibrationAmber, "R2", 1f, textPaint)
  val mid = Offset((screenA.x + screenB.x) / 2f, (screenA.y + screenB.y) / 2f + 22.dp.toPx())
  drawPillLabel(mid, label, Color(0xFF2B1D05), CalibrationAmber, textPaint, bgPaint)
}

private fun DrawScope.drawEndpointHandle(
  center: Offset,
  color: Color,
  tag: String,
  scale: Float,
  textPaint: Paint,
) {
  val outerR = 11.dp.toPx() * scale
  val innerR = 4.5.dp.toPx()
  drawCircle(color = Color(0xFF09111A).copy(alpha = 0.85f), radius = outerR, center = center)
  drawCircle(color = color, radius = outerR, center = center, style = Stroke(width = 2.5.dp.toPx()))
  drawCircle(color = Color.White, radius = innerR, center = center)

  textPaint.textSize = 10.sp.toPx()
  textPaint.color = color.toArgb()
  drawContext.canvas.nativeCanvas.drawText(
    tag,
    center.x,
    center.y - outerR - 6.dp.toPx(),
    textPaint,
  )
}

private fun DrawScope.drawPillLabel(
  center: Offset,
  text: String,
  bgColor: Color,
  borderColor: Color,
  textPaint: Paint,
  bgPaint: Paint,
) {
  textPaint.textSize = 11.5.sp.toPx()
  textPaint.color = Color.White.toArgb()
  val bounds = Rect()
  textPaint.getTextBounds(text, 0, text.length, bounds)
  val padH = 10.dp.toPx()
  val padV = 5.dp.toPx()
  val halfW = bounds.width() / 2f + padH
  val halfH = bounds.height() / 2f + padV

  val clampedX = center.x.coerceIn(halfW + 6f, size.width - halfW - 6f)
  val clampedY = center.y.coerceIn(halfH + 6f, size.height - halfH - 6f)
  val rect = RectF(clampedX - halfW, clampedY - halfH, clampedX + halfW, clampedY + halfH)

  bgPaint.style = Paint.Style.FILL
  bgPaint.color = bgColor.copy(alpha = 0.92f).toArgb()
  drawContext.canvas.nativeCanvas.drawRoundRect(rect, 12.dp.toPx(), 12.dp.toPx(), bgPaint)

  bgPaint.style = Paint.Style.STROKE
  bgPaint.strokeWidth = 1.4.dp.toPx()
  bgPaint.color = borderColor.toArgb()
  drawContext.canvas.nativeCanvas.drawRoundRect(rect, 12.dp.toPx(), 12.dp.toPx(), bgPaint)

  drawContext.canvas.nativeCanvas.drawText(
    text,
    clampedX,
    clampedY + bounds.height() / 2f - 1.dp.toPx(),
    textPaint,
  )
}

private fun DrawScope.drawMagnifierLoupe(
  imageBitmap: androidx.compose.ui.graphics.ImageBitmap,
  imagePoint: Point2D,
  fingerScreen: Offset,
  loupeRadius: Float,
) {
  val offsetAbove = loupeRadius + 46.dp.toPx()
  val loupeCenter = Offset(
    x = fingerScreen.x.coerceIn(loupeRadius + 12f, size.width - loupeRadius - 12f),
    y = if (fingerScreen.y - offsetAbove > loupeRadius + 12f) {
      fingerScreen.y - offsetAbove
    } else {
      (fingerScreen.y + offsetAbove).coerceAtMost(size.height - loupeRadius - 12f)
    },
  )

  val clip = androidx.compose.ui.graphics.Path().apply {
    addOval(
      androidx.compose.ui.geometry.Rect(
        loupeCenter.x - loupeRadius,
        loupeCenter.y - loupeRadius,
        loupeCenter.x + loupeRadius,
        loupeCenter.y + loupeRadius,
      )
    )
  }

  val zoomScale = 2.6f * (size.width / imageBitmap.width.toFloat().coerceAtLeast(1f)).coerceAtLeast(1f)
  val srcW = imageBitmap.width * zoomScale
  val srcH = imageBitmap.height * zoomScale
  val dstX = loupeCenter.x - imagePoint.x * zoomScale
  val dstY = loupeCenter.y - imagePoint.y * zoomScale

  drawCircle(Color.Black, radius = loupeRadius + 2.dp.toPx(), center = loupeCenter)
  clipPath(clip) {
    drawRect(Color(0xFF0A0F14), topLeft = Offset(loupeCenter.x - loupeRadius, loupeCenter.y - loupeRadius), size = androidx.compose.ui.geometry.Size(loupeRadius * 2, loupeRadius * 2))
    drawImage(
      image = imageBitmap,
      dstOffset = IntOffset(dstX.roundToInt(), dstY.roundToInt()),
      dstSize = IntSize(srcW.roundToInt().coerceAtLeast(1), srcH.roundToInt().coerceAtLeast(1)),
    )
    // Loupe precision crosshair
    drawLine(
      InstrumentCyanGlow,
      Offset(loupeCenter.x - loupeRadius, loupeCenter.y),
      Offset(loupeCenter.x + loupeRadius, loupeCenter.y),
      strokeWidth = 1.5.dp.toPx(),
    )
    drawLine(
      InstrumentCyanGlow,
      Offset(loupeCenter.x, loupeCenter.y - loupeRadius),
      Offset(loupeCenter.x, loupeCenter.y + loupeRadius),
      strokeWidth = 1.5.dp.toPx(),
    )
  }
  drawCircle(
    color = InstrumentCyanGlow,
    radius = loupeRadius,
    center = loupeCenter,
    style = Stroke(width = 2.5.dp.toPx()),
  )
}
