package com.example.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "measurement_sessions")
data class MeasurementSessionEntity(
  @PrimaryKey val id: String,
  val title: String,
  val createdAt: Long,
  val calibrationMethod: String,
  val referenceLabel: String,
  val pixelsPerCm: Double?,
  val referenceLengthCm: Double?,
  val refAX: Float?,
  val refAY: Float?,
  val refBX: Float?,
  val refBY: Float?,
  val imageWidth: Int,
  val imageHeight: Int,
  val imagePath: String?,
)

@Entity(
  tableName = "measurement_lines",
  foreignKeys = [
    ForeignKey(
      entity = MeasurementSessionEntity::class,
      parentColumns = ["id"],
      childColumns = ["sessionId"],
      onDelete = ForeignKey.CASCADE,
    )
  ],
  indices = [Index(value = ["sessionId"])],
)
data class MeasurementLineEntity(
  @PrimaryKey val id: String,
  val sessionId: String,
  val name: String,
  val ax: Float,
  val ay: Float,
  val bx: Float,
  val by: Float,
  val sortOrder: Int,
)

data class SessionWithLines(
  @Embedded val session: MeasurementSessionEntity,
  @Relation(
    parentColumn = "id",
    entityColumn = "sessionId",
  )
  val lines: List<MeasurementLineEntity>,
)
