package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementDao {

  @Transaction
  @Query("SELECT * FROM measurement_sessions ORDER BY createdAt DESC")
  fun getAllSessionsWithLines(): Flow<List<SessionWithLines>>

  @Transaction
  @Query("SELECT * FROM measurement_sessions WHERE id = :sessionId")
  suspend fun getSessionWithLinesById(sessionId: String): SessionWithLines?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSession(session: MeasurementSessionEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertLines(lines: List<MeasurementLineEntity>)

  @Query("DELETE FROM measurement_lines WHERE sessionId = :sessionId")
  suspend fun deleteLinesForSession(sessionId: String)

  @Transaction
  suspend fun upsertSessionWithLines(
    session: MeasurementSessionEntity,
    lines: List<MeasurementLineEntity>,
  ) {
    insertSession(session)
    deleteLinesForSession(session.id)
    if (lines.isNotEmpty()) {
      insertLines(lines)
    }
  }

  @Query("DELETE FROM measurement_sessions WHERE id = :sessionId")
  suspend fun deleteSessionById(sessionId: String)

  @Query("DELETE FROM measurement_sessions")
  suspend fun deleteAllSessions()
}
