package com.example.data.repository

import com.example.data.local.MeasurementDao
import com.example.data.local.MeasurementLineEntity
import com.example.data.local.MeasurementSessionEntity
import com.example.data.local.SessionWithLines
import com.example.domain.model.CalibrationState
import com.example.domain.model.MeasurementLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class MeasurementRepository(
  private val dao: MeasurementDao,
) {

  val allSessions: Flow<List<SessionWithLines>> = dao.getAllSessionsWithLines()

  suspend fun saveSession(
    existingSessionId: String?,
    title: String,
    calibration: CalibrationState,
    lines: List<MeasurementLine>,
    imageWidth: Int,
    imageHeight: Int,
    imagePath: String?,
  ): String {
    val sessionId = existingSessionId ?: UUID.randomUUID().toString()
    val sessionEntity = MeasurementSessionEntity(
      id = sessionId,
      title = title.trim().ifEmpty { "Measurement Session" },
      createdAt = System.currentTimeMillis(),
      calibrationMethod = calibration.method.name,
      referenceLabel = calibration.referenceLabel,
      pixelsPerCm = calibration.pixelsPerCm,
      referenceLengthCm = calibration.referenceLengthCm,
      refAX = calibration.referencePointA?.x,
      refAY = calibration.referencePointA?.y,
      refBX = calibration.referencePointB?.x,
      refBY = calibration.referencePointB?.y,
      imageWidth = imageWidth,
      imageHeight = imageHeight,
      imagePath = imagePath,
    )
    val lineEntities = lines.mapIndexed { idx, line ->
      MeasurementLineEntity(
        id = line.id,
        sessionId = sessionId,
        name = line.name,
        ax = line.pointA.x,
        ay = line.pointA.y,
        bx = line.pointB.x,
        by = line.pointB.y,
        sortOrder = idx,
      )
    }
    dao.upsertSessionWithLines(sessionEntity, lineEntities)
    return sessionId
  }

  suspend fun deleteSession(session: SessionWithLines) {
    val imagePath = session.session.imagePath
    dao.deleteSessionById(session.session.id)
    if (!imagePath.isNullOrBlank()) {
      withContext(Dispatchers.IO) {
        runCatching { File(imagePath).delete() }
      }
    }
  }

  suspend fun clearAllSessions(sessions: List<SessionWithLines>) {
    dao.deleteAllSessions()
    withContext(Dispatchers.IO) {
      sessions.forEach { item ->
        val path = item.session.imagePath
        if (!path.isNullOrBlank()) {
          runCatching { File(path).delete() }
        }
      }
    }
  }
}
