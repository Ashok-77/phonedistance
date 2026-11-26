package com.example.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class DecodedPhoto(
  val filePath: String,
  val bitmap: Bitmap,
  val width: Int,
  val height: Int,
)

object ImageProcessor {

  private const val MAX_IMAGE_DIM = 2048

  fun capturesDir(context: Context): File {
    return File(context.filesDir, "captures").apply { mkdirs() }
  }

  fun createCaptureFile(context: Context): File {
    return File(capturesDir(context), "capture_${UUID.randomUUID()}.jpg")
  }

  suspend fun copyAndDecodeFromUri(context: Context, uri: Uri): DecodedPhoto? =
    withContext(Dispatchers.IO) {
      val destFile = createCaptureFile(context)
      val orientation = context.contentResolver.openInputStream(uri)?.use { stream ->
        runCatching {
          ExifInterface(stream).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
          )
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
      } ?: ExifInterface.ORIENTATION_NORMAL

      val rawBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
        decodeSampledBitmap(stream.readBytes())
      } ?: return@withContext null

      val oriented = applyExifOrientation(rawBitmap, orientation)
      if (oriented !== rawBitmap) rawBitmap.recycle()

      FileOutputStream(destFile).use { out ->
        oriented.compress(Bitmap.CompressFormat.JPEG, 92, out)
      }
      DecodedPhoto(
        filePath = destFile.absolutePath,
        bitmap = oriented,
        width = oriented.width,
        height = oriented.height,
      )
    }

  suspend fun decodeCaptureFile(file: File): DecodedPhoto? =
    withContext(Dispatchers.IO) {
      if (!file.exists()) return@withContext null
      val orientation = runCatching {
        ExifInterface(file.absolutePath).getAttributeInt(
          ExifInterface.TAG_ORIENTATION,
          ExifInterface.ORIENTATION_NORMAL,
        )
      }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

      val bytes = file.readBytes()
      val rawBitmap = decodeSampledBitmap(bytes) ?: return@withContext null
      val oriented = applyExifOrientation(rawBitmap, orientation)
      if (oriented !== rawBitmap) {
        rawBitmap.recycle()
        FileOutputStream(file).use { out ->
          oriented.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
      }
      DecodedPhoto(
        filePath = file.absolutePath,
        bitmap = oriented,
        width = oriented.width,
        height = oriented.height,
      )
    }

  suspend fun createPracticeBlueprint(context: Context): DecodedPhoto =
    withContext(Dispatchers.IO) {
      val width = 1600
      val height = 1200
      val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(bitmap)

      // Dark technical cutting mat background
      canvas.drawColor(Color.rgb(19, 28, 38))

      val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(38, 56, 189, 248)
        strokeWidth = 1.5f
      }
      val majorGridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(72, 56, 189, 248)
        strokeWidth = 2.5f
      }
      for (x in 0..width step 50) {
        canvas.drawLine(x.toFloat(), 0f, x.toFloat(), height.toFloat(), if (x % 200 == 0) majorGridPaint else gridPaint)
      }
      for (y in 0..height step 50) {
        canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), if (y % 200 == 0) majorGridPaint else gridPaint)
      }

      // Draw a realistic ISO/IEC 7810 ID-1 Credit/ID Card (8.56 cm x 5.40 cm at 80 px/cm => 685 px x 432 px)
      val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(226, 232, 240)
      }
      val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(14, 165, 233)
        style = Paint.Style.STROKE
        strokeWidth = 4f
      }
      val cardRect = RectF(160f, 180f, 160f + 685f, 180f + 432f)
      canvas.drawRoundRect(cardRect, 28f, 28f, cardPaint)
      canvas.drawRoundRect(cardRect, 28f, 28f, cardBorderPaint)

      // Card chip & label
      val chipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(217, 169, 64) }
      canvas.drawRoundRect(RectF(230f, 330f, 325f, 405f), 12f, 12f, chipPaint)

      val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(15, 23, 42)
        textSize = 32f
        isFakeBoldText = true
      }
      canvas.drawText("ISO ID-1 CARD (8.56 cm x 5.40 cm)", 220f, 255f, textPaint)

      // Draw a metric steel ruler along the bottom (10 cm = 800 px at 80 px/cm)
      val rulerRect = RectF(160f, 920f, 160f + 960f, 1060f)
      val rulerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(203, 213, 225) }
      canvas.drawRect(rulerRect, rulerPaint)
      val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(15, 23, 42)
        strokeWidth = 3f
        textSize = 26f
      }
      for (mm in 0..110) {
        val x = 200f + mm * 8f
        val tickLen = when {
          mm % 10 == 0 -> 58f
          mm % 5 == 0 -> 40f
          else -> 24f
        }
        canvas.drawLine(x, 920f, x, 920f + tickLen, tickPaint)
        if (mm % 10 == 0) {
          canvas.drawText("${mm / 10}", x - 8f, 1020f, tickPaint)
        }
      }
      canvas.drawText("cm", 1105f, 1020f, tickPaint)

      // Draw an aluminum machined block to measure (e.g. 6.00 cm x 3.50 cm => 480 px x 280 px)
      val partPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(148, 163, 184) }
      val partStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(241, 245, 249)
        style = Paint.Style.STROKE
        strokeWidth = 4f
      }
      val partRect = RectF(960f, 260f, 960f + 480f, 260f + 280f)
      canvas.drawRoundRect(partRect, 14f, 14f, partPaint)
      canvas.drawRoundRect(partRect, 14f, 14f, partStroke)

      val holePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(19, 28, 38) }
      canvas.drawCircle(1040f, 400f, 34f, holePaint)
      canvas.drawCircle(1360f, 400f, 34f, holePaint)

      val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(148, 163, 184)
        textSize = 28f
      }
      canvas.drawText("PRACTICE WORKBENCH TARGET (FLAT PLANE)", 160f, 110f, labelPaint)

      val destFile = createCaptureFile(context)
      FileOutputStream(destFile).use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
      }
      DecodedPhoto(
        filePath = destFile.absolutePath,
        bitmap = bitmap,
        width = bitmap.width,
        height = bitmap.height,
      )
    }

  private fun decodeSampledBitmap(bytes: ByteArray): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sampleSize = 1
    while (bounds.outWidth / sampleSize > MAX_IMAGE_DIM || bounds.outHeight / sampleSize > MAX_IMAGE_DIM) {
      sampleSize *= 2
    }
    val decodeOpts = BitmapFactory.Options().apply {
      inSampleSize = sampleSize
      inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
  }

  private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
    val matrix = Matrix()
    when (orientation) {
      ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
      ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
      ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
      ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
      ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
      else -> return bitmap
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
  }
}
