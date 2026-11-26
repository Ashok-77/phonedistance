package com.example.data.local

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.domain.model.CalibrationEngine
import com.example.domain.model.CalibrationMethod
import com.example.domain.model.CalibrationState
import com.example.domain.model.Point2D
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

  private const val HEADER =
    "SessionID,SessionTitle,TimestampISO,CalibrationMode,CalibrationReference,PixelsPerCm,LineName,PointA_X,PointA_Y,PointB_X,PointB_Y,Distance_Px,Distance_Cm,Distance_M,Distance_In"

  fun generateCsv(sessions: List<SessionWithLines>): String {
    val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
    val sb = StringBuilder()
    sb.appendLine(HEADER)

    sessions.forEach { item ->
      val session = item.session
      val method = runCatching { CalibrationMethod.valueOf(session.calibrationMethod) }
        .getOrDefault(CalibrationMethod.PIXEL_MODE)
      val calibration = CalibrationState(
        method = method,
        pixelsPerCm = session.pixelsPerCm,
        referenceLabel = session.referenceLabel,
        referenceLengthCm = session.referenceLengthCm,
      )
      val timestampStr = isoFormat.format(Date(session.createdAt))
      val pxPerCmStr = session.pixelsPerCm?.let { String.format(Locale.US, "%.4f", it) } ?: ""

      val sortedLines = item.lines.sortedBy { it.sortOrder }
      if (sortedLines.isEmpty()) {
        sb.appendLine(
          listOf(
            session.id,
            session.title,
            timestampStr,
            method.displayName,
            session.referenceLabel,
            pxPerCmStr,
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
          ).joinToString(",") { escapeCsv(it) }
        )
      } else {
        sortedLines.forEach { line ->
          val pxDist = CalibrationEngine.pixelDistance(
            Point2D(line.ax, line.ay),
            Point2D(line.bx, line.by),
          )
          val calibrated = CalibrationEngine.measure(pxDist, calibration)
          sb.appendLine(
            listOf(
              session.id,
              session.title,
              timestampStr,
              method.displayName,
              session.referenceLabel,
              pxPerCmStr,
              line.name,
              String.format(Locale.US, "%.1f", line.ax),
              String.format(Locale.US, "%.1f", line.ay),
              String.format(Locale.US, "%.1f", line.bx),
              String.format(Locale.US, "%.1f", line.by),
              String.format(Locale.US, "%.2f", pxDist),
              calibrated?.let { String.format(Locale.US, "%.3f", it.centimeters) } ?: "",
              calibrated?.let { String.format(Locale.US, "%.4f", it.meters) } ?: "",
              calibrated?.let { String.format(Locale.US, "%.3f", it.inches) } ?: "",
            ).joinToString(",") { escapeCsv(it) }
          )
        }
      }
    }
    return sb.toString()
  }

  fun writeTempCsvForShare(
    context: Context,
    csvContent: String,
    fileNamePrefix: String,
  ): Uri {
    val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
    val safePrefix = fileNamePrefix.replace(Regex("[^A-Za-z0-9_-]"), "_").ifBlank { "vision_measure" }
    val file = File(exportDir, "${safePrefix}_${System.currentTimeMillis()}.csv")
    file.writeText(csvContent, Charsets.UTF_8)
    return FileProvider.getUriForFile(
      context,
      "${context.packageName}.fileprovider",
      file,
    )
  }

  private fun escapeCsv(value: String): String {
    val needsQuotes = value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')
    return if (needsQuotes) {
      "\"${value.replace("\"", "\"\"")}\""
    } else {
      value
    }
  }
}
