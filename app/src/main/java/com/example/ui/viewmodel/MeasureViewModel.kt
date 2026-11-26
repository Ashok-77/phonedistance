package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.CsvExporter
import com.example.data.local.ImageProcessor
import com.example.data.local.SessionWithLines
import com.example.data.repository.MeasurementRepository
import com.example.data.repository.PreferencesRepository
import com.example.data.repository.UserPreferences
import com.example.domain.model.CalibrationEngine
import com.example.domain.model.CalibrationMethod
import com.example.domain.model.CalibrationState
import com.example.domain.model.InteractionTool
import com.example.domain.model.LengthUnit
import com.example.domain.model.MeasurementLine
import com.example.domain.model.Point2D
import com.example.domain.model.ThemeMode
import com.example.domain.model.WorkspaceMode
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MeasureUiState(
  val workspaceMode: WorkspaceMode = WorkspaceMode.LIVE_CAMERA,
  val hasCameraPermission: Boolean = false,
  val isCameraActive: Boolean = false,
  val lensFacing: Int = CameraSelector.LENS_FACING_BACK,
  val isFlashEnabled: Boolean = false,
  val hasFrontCamera: Boolean = true,
  val hasBackCamera: Boolean = true,
  val isCapturingPhoto: Boolean = false,
  val photoBitmap: Bitmap? = null,
  val photoFilePath: String? = null,
  val canvasWidth: Int = 1080,
  val canvasHeight: Int = 1440,
  val interactionTool: InteractionTool = InteractionTool.MEASURE,
  val lines: List<MeasurementLine> = emptyList(),
  val pendingPointA: Point2D? = null,
  val selectedLineId: String? = null,
  val calibration: CalibrationState = CalibrationState(),
  val pendingCalPointA: Point2D? = null,
  val pendingCalPointB: Point2D? = null,
  val activeSessionId: String? = null,
  val activeSessionTitle: String? = null,
) {
  val selectedLine: MeasurementLine?
    get() = lines.firstOrNull { it.id == selectedLineId } ?: lines.lastOrNull()
}

sealed interface UiEvent {
  data class ShowMessage(val message: String) : UiEvent
  data class ShareCsv(val uri: Uri, val fileName: String) : UiEvent
  data class PromptSaveCsv(val csvContent: String, val suggestedFileName: String) : UiEvent
}

class MeasureViewModel(
  application: Application,
  private val measurementRepository: MeasurementRepository,
  private val preferencesRepository: PreferencesRepository,
) : AndroidViewModel(application) {

  private val _uiState = MutableStateFlow(MeasureUiState())
  val uiState: StateFlow<MeasureUiState> = _uiState.asStateFlow()

  private val _events = MutableSharedFlow<UiEvent>()
  val events: SharedFlow<UiEvent> = _events.asSharedFlow()

  val preferences: StateFlow<UserPreferences> = preferencesRepository.preferencesFlow
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = UserPreferences(),
    )

  val savedSessions: StateFlow<List<SessionWithLines>> = measurementRepository.allSessions
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = emptyList(),
    )

  private var nextLineCounter = 1

  fun onCameraPermissionChanged(granted: Boolean) {
    _uiState.update { state ->
      state.copy(
        hasCameraPermission = granted,
        isCameraActive = granted && state.workspaceMode == WorkspaceMode.LIVE_CAMERA,
      )
    }
  }

  fun activateLiveCamera() {
    _uiState.update { state ->
      state.copy(
        workspaceMode = WorkspaceMode.LIVE_CAMERA,
        isCameraActive = state.hasCameraPermission,
        photoBitmap = null,
        photoFilePath = null,
        lines = emptyList(),
        pendingPointA = null,
        selectedLineId = null,
        activeSessionId = null,
        activeSessionTitle = null,
        calibration = CalibrationState(),
        pendingCalPointA = null,
        pendingCalPointB = null,
        interactionTool = InteractionTool.MEASURE,
      )
    }
    nextLineCounter = 1
  }

  fun updateLiveCanvasDimensions(width: Int, height: Int) {
    if (width <= 0 || height <= 0) return
    val current = _uiState.value
    if (current.workspaceMode == WorkspaceMode.LIVE_CAMERA &&
      (current.canvasWidth != width || current.canvasHeight != height)
    ) {
      _uiState.update { it.copy(canvasWidth = width, canvasHeight = height) }
    }
  }

  fun updateCameraCapabilities(hasBack: Boolean, hasFront: Boolean) {
    _uiState.update { state ->
      val facing = when {
        state.lensFacing == CameraSelector.LENS_FACING_BACK && hasBack -> CameraSelector.LENS_FACING_BACK
        state.lensFacing == CameraSelector.LENS_FACING_FRONT && hasFront -> CameraSelector.LENS_FACING_FRONT
        hasBack -> CameraSelector.LENS_FACING_BACK
        else -> CameraSelector.LENS_FACING_FRONT
      }
      state.copy(
        hasBackCamera = hasBack,
        hasFrontCamera = hasFront,
        lensFacing = facing,
      )
    }
  }

  fun toggleCameraLens() {
    _uiState.update { state ->
      val nextFacing = if (state.lensFacing == CameraSelector.LENS_FACING_BACK && state.hasFrontCamera) {
        CameraSelector.LENS_FACING_FRONT
      } else if (state.hasBackCamera) {
        CameraSelector.LENS_FACING_BACK
      } else {
        state.lensFacing
      }
      state.copy(
        lensFacing = nextFacing,
        isFlashEnabled = if (nextFacing == CameraSelector.LENS_FACING_FRONT) false else state.isFlashEnabled,
      )
    }
  }

  fun toggleFlash() {
    _uiState.update { state ->
      if (state.lensFacing == CameraSelector.LENS_FACING_FRONT) state
      else state.copy(isFlashEnabled = !state.isFlashEnabled)
    }
  }

  fun setCapturingPhoto(capturing: Boolean) {
    _uiState.update { it.copy(isCapturingPhoto = capturing) }
  }

  fun onPhotoCaptured(file: File) {
    viewModelScope.launch {
      val decoded = ImageProcessor.decodeCaptureFile(file)
      _uiState.update { it.copy(isCapturingPhoto = false) }
      if (decoded == null) {
        emitMessage("Could not decode captured photo. Please try again.")
        return@launch
      }
      loadPhotoIntoWorkspace(
        bitmap = decoded.bitmap,
        filePath = decoded.filePath,
        width = decoded.width,
        height = decoded.height,
        resetMeasurements = true,
      )
      emitMessage("Photo frozen locally. Tap two points to measure.")
    }
  }

  fun onPhotoPicked(uri: Uri) {
    viewModelScope.launch {
      val decoded = ImageProcessor.copyAndDecodeFromUri(getApplication(), uri)
      if (decoded == null) {
        emitMessage("Could not load selected image.")
        return@launch
      }
      loadPhotoIntoWorkspace(
        bitmap = decoded.bitmap,
        filePath = decoded.filePath,
        width = decoded.width,
        height = decoded.height,
        resetMeasurements = true,
      )
      emitMessage("Photo imported locally. Tap A and B to measure.")
    }
  }

  fun loadPracticeBlueprint() {
    viewModelScope.launch {
      val decoded = ImageProcessor.createPracticeBlueprint(getApplication())
      loadPhotoIntoWorkspace(
        bitmap = decoded.bitmap,
        filePath = decoded.filePath,
        width = decoded.width,
        height = decoded.height,
        resetMeasurements = true,
      )
      emitMessage("Practice workbench loaded. Try calibrating on the 8.56 cm ID card or steel ruler.")
    }
  }

  private fun loadPhotoIntoWorkspace(
    bitmap: Bitmap,
    filePath: String?,
    width: Int,
    height: Int,
    resetMeasurements: Boolean,
  ) {
    _uiState.update { state ->
      state.copy(
        workspaceMode = WorkspaceMode.PHOTO_CANVAS,
        isCameraActive = false,
        photoBitmap = bitmap,
        photoFilePath = filePath,
        canvasWidth = width,
        canvasHeight = height,
        lines = if (resetMeasurements) emptyList() else state.lines,
        pendingPointA = if (resetMeasurements) null else state.pendingPointA,
        selectedLineId = if (resetMeasurements) null else state.selectedLineId,
        calibration = if (resetMeasurements) CalibrationState() else state.calibration,
        pendingCalPointA = if (resetMeasurements) null else state.pendingCalPointA,
        pendingCalPointB = if (resetMeasurements) null else state.pendingCalPointB,
        activeSessionId = if (resetMeasurements) null else state.activeSessionId,
        activeSessionTitle = if (resetMeasurements) null else state.activeSessionTitle,
        interactionTool = InteractionTool.MEASURE,
      )
    }
    if (resetMeasurements) nextLineCounter = 1
  }

  fun setInteractionTool(tool: InteractionTool) {
    _uiState.update { state ->
      if (tool == InteractionTool.CALIBRATE_SPAN) {
        // Pre-populate calibration points from existing calibration or currently selected line
        val existingA = state.pendingCalPointA ?: state.calibration.referencePointA
        val existingB = state.pendingCalPointB ?: state.calibration.referencePointB
        state.copy(
          interactionTool = tool,
          pendingPointA = null,
          pendingCalPointA = existingA,
          pendingCalPointB = existingB,
        )
      } else {
        state.copy(interactionTool = tool)
      }
    }
  }

  fun onCanvasTap(point: Point2D) {
    val current = _uiState.value
    val clamped = clampPoint(point, current.canvasWidth, current.canvasHeight)

    if (current.interactionTool == InteractionTool.CALIBRATE_SPAN) {
      handleCalibrationTap(clamped)
      return
    }

    val pointA = current.pendingPointA
    if (pointA == null) {
      _uiState.update { it.copy(pendingPointA = clamped) }
    } else {
      if (CalibrationEngine.pixelDistance(pointA, clamped) < 4.0) {
        emitMessage("Tap farther from Point A to create a measurable span.")
        return
      }
      val newLine = MeasurementLine(
        name = "Line ${nextLineCounter++}",
        pointA = pointA,
        pointB = clamped,
      )
      _uiState.update { state ->
        state.copy(
          lines = state.lines + newLine,
          pendingPointA = null,
          selectedLineId = newLine.id,
        )
      }
    }
  }

  private fun handleCalibrationTap(clamped: Point2D) {
    val current = _uiState.value
    val calA = current.pendingCalPointA
    val calB = current.pendingCalPointB
    if (calA == null || calB != null) {
      _uiState.update {
        it.copy(
          pendingCalPointA = clamped,
          pendingCalPointB = null,
        )
      }
    } else {
      if (CalibrationEngine.pixelDistance(calA, clamped) < 6.0) {
        emitMessage("Calibration span is too short. Select two distinct points on your reference.")
        return
      }
      _uiState.update { state ->
        val updatedCalibration = if (state.calibration.isCalibrated && state.calibration.referenceLengthCm != null) {
          val newPxPerCm = CalibrationEngine.computePixelsPerCm(
            calA,
            clamped,
            state.calibration.referenceLengthCm,
            LengthUnit.CENTIMETERS,
          )
          state.calibration.copy(
            pixelsPerCm = newPxPerCm ?: state.calibration.pixelsPerCm,
            referencePointA = calA,
            referencePointB = clamped,
          )
        } else {
          state.calibration
        }
        state.copy(
          pendingCalPointB = clamped,
          calibration = updatedCalibration,
        )
      }
    }
  }

  fun onHandleDrag(lineId: String, isPointA: Boolean, newPoint: Point2D) {
    val current = _uiState.value
    val clamped = clampPoint(newPoint, current.canvasWidth, current.canvasHeight)
    _uiState.update { state ->
      state.copy(
        selectedLineId = lineId,
        lines = state.lines.map { line ->
          if (line.id != lineId) line
          else if (isPointA) line.copy(pointA = clamped)
          else line.copy(pointB = clamped)
        },
      )
    }
  }

  fun onCalibrationHandleDrag(isPointA: Boolean, newPoint: Point2D) {
    val current = _uiState.value
    val clamped = clampPoint(newPoint, current.canvasWidth, current.canvasHeight)
    _uiState.update { state ->
      val a = if (isPointA) clamped else (state.pendingCalPointA ?: state.calibration.referencePointA)
      val b = if (!isPointA) clamped else (state.pendingCalPointB ?: state.calibration.referencePointB)
      val updatedCalibration = if (a != null && b != null && state.calibration.isCalibrated && state.calibration.referenceLengthCm != null) {
        val newPxPerCm = CalibrationEngine.computePixelsPerCm(
          a,
          b,
          state.calibration.referenceLengthCm,
          LengthUnit.CENTIMETERS,
        )
        state.calibration.copy(
          pixelsPerCm = newPxPerCm ?: state.calibration.pixelsPerCm,
          referencePointA = a,
          referencePointB = b,
        )
      } else {
        state.calibration
      }
      state.copy(
        pendingCalPointA = a,
        pendingCalPointB = b,
        calibration = updatedCalibration,
      )
    }
  }

  fun useSelectedLineForCalibrationSpan() {
    val line = _uiState.value.selectedLine ?: return
    _uiState.update {
      it.copy(
        pendingCalPointA = line.pointA,
        pendingCalPointB = line.pointB,
      )
    }
  }

  fun applyCalibration(
    method: CalibrationMethod,
    referenceLabel: String,
    actualLength: Double,
    unit: LengthUnit,
    pointA: Point2D?,
    pointB: Point2D?,
  ): Boolean {
    if (method == CalibrationMethod.PIXEL_MODE) {
      resetToPixelMode()
      return true
    }
    if (pointA == null || pointB == null) {
      emitMessage("Place two calibration points on the reference object or ruler first.")
      return false
    }
    val pxPerCm = CalibrationEngine.computePixelsPerCm(pointA, pointB, actualLength, unit)
    if (pxPerCm == null || pxPerCm <= 0.0) {
      emitMessage("Enter a valid positive length and ensure the two points are separated.")
      return false
    }
    val lengthCm = unit.toCentimeters(actualLength)
    _uiState.update { state ->
      state.copy(
        calibration = CalibrationState(
          method = method,
          pixelsPerCm = pxPerCm,
          referenceLabel = referenceLabel.trim().ifEmpty { method.displayName },
          referenceLengthCm = lengthCm,
          referencePointA = pointA,
          referencePointB = pointB,
        ),
        pendingCalPointA = pointA,
        pendingCalPointB = pointB,
        interactionTool = InteractionTool.MEASURE,
      )
    }
    emitMessage("Calibrated: ${String.format(Locale.US, "%.1f", pxPerCm)} px/cm")
    return true
  }

  fun resetToPixelMode() {
    _uiState.update { state ->
      state.copy(
        calibration = CalibrationState(method = CalibrationMethod.PIXEL_MODE),
        pendingCalPointA = null,
        pendingCalPointB = null,
        interactionTool = InteractionTool.MEASURE,
      )
    }
    emitMessage("Switched to Pixel Mode (uncalibrated).")
  }

  fun clearPendingPoint() {
    _uiState.update { it.copy(pendingPointA = null) }
  }

  fun selectLine(lineId: String) {
    _uiState.update { it.copy(selectedLineId = lineId) }
  }

  fun renameLine(lineId: String, newName: String) {
    val trimmed = newName.trim()
    if (trimmed.isEmpty()) return
    _uiState.update { state ->
      state.copy(
        lines = state.lines.map { if (it.id == lineId) it.copy(name = trimmed) else it },
      )
    }
  }

  fun deleteLine(lineId: String) {
    _uiState.update { state ->
      val remaining = state.lines.filterNot { it.id == lineId }
      val nextSelected = if (state.selectedLineId == lineId) remaining.lastOrNull()?.id else state.selectedLineId
      state.copy(
        lines = remaining,
        selectedLineId = nextSelected,
      )
    }
  }

  fun resetCanvas() {
    _uiState.update {
      it.copy(
        lines = emptyList(),
        pendingPointA = null,
        selectedLineId = null,
        pendingCalPointA = null,
        pendingCalPointB = null,
        calibration = CalibrationState(),
        interactionTool = InteractionTool.MEASURE,
      )
    }
    nextLineCounter = 1
    emitMessage("Canvas cleared.")
  }

  fun saveCurrentSession(customTitle: String) {
    val current = _uiState.value
    if (current.lines.isEmpty()) {
      emitMessage("Add at least one measurement line before saving.")
      return
    }
    viewModelScope.launch {
      val defaultTitle = SimpleDateFormat("MMM d • HH:mm", Locale.getDefault()).format(Date())
      val title = customTitle.trim().ifEmpty { "Session $defaultTitle" }
      val savedId = measurementRepository.saveSession(
        existingSessionId = current.activeSessionId,
        title = title,
        calibration = current.calibration,
        lines = current.lines,
        imageWidth = current.canvasWidth,
        imageHeight = current.canvasHeight,
        imagePath = current.photoFilePath,
      )
      _uiState.update { it.copy(activeSessionId = savedId, activeSessionTitle = title) }
      emitMessage("Session \"$title\" saved locally.")
    }
  }

  fun loadSessionIntoWorkspace(item: SessionWithLines) {
    viewModelScope.launch {
      val session = item.session
      val decoded = session.imagePath?.let { path ->
        ImageProcessor.decodeCaptureFile(File(path))
      }
      val method = runCatching { CalibrationMethod.valueOf(session.calibrationMethod) }
        .getOrDefault(CalibrationMethod.PIXEL_MODE)
      val refA = if (session.refAX != null && session.refAY != null) Point2D(session.refAX, session.refAY) else null
      val refB = if (session.refBX != null && session.refBY != null) Point2D(session.refBX, session.refBY) else null
      val loadedLines = item.lines.sortedBy { it.sortOrder }.map { entity ->
        MeasurementLine(
          id = entity.id,
          name = entity.name,
          pointA = Point2D(entity.ax, entity.ay),
          pointB = Point2D(entity.bx, entity.by),
        )
      }
      nextLineCounter = loadedLines.size + 1

      _uiState.update { state ->
        state.copy(
          workspaceMode = if (decoded != null) WorkspaceMode.PHOTO_CANVAS else WorkspaceMode.LIVE_CAMERA,
          isCameraActive = decoded == null && state.hasCameraPermission,
          photoBitmap = decoded?.bitmap,
          photoFilePath = decoded?.filePath,
          canvasWidth = decoded?.width ?: session.imageWidth.coerceAtLeast(100),
          canvasHeight = decoded?.height ?: session.imageHeight.coerceAtLeast(100),
          lines = loadedLines,
          pendingPointA = null,
          selectedLineId = loadedLines.lastOrNull()?.id,
          calibration = CalibrationState(
            method = method,
            pixelsPerCm = session.pixelsPerCm,
            referenceLabel = session.referenceLabel,
            referenceLengthCm = session.referenceLengthCm,
            referencePointA = refA,
            referencePointB = refB,
          ),
          pendingCalPointA = refA,
          pendingCalPointB = refB,
          activeSessionId = session.id,
          activeSessionTitle = session.title,
          interactionTool = InteractionTool.MEASURE,
        )
      }
      emitMessage("Loaded \"${session.title}\" into workspace.")
    }
  }

  fun deleteSession(item: SessionWithLines) {
    viewModelScope.launch {
      measurementRepository.deleteSession(item)
      emitMessage("Deleted \"${item.session.title}\".")
    }
  }

  fun clearAllHistory() {
    viewModelScope.launch {
      measurementRepository.clearAllSessions(savedSessions.value)
      emitMessage("All local measurement history cleared.")
    }
  }

  fun shareSessionsCsv(sessions: List<SessionWithLines>, prefix: String) {
    if (sessions.isEmpty()) {
      emitMessage("No measurements available to export.")
      return
    }
    viewModelScope.launch {
      val csv = CsvExporter.generateCsv(sessions)
      val uri = CsvExporter.writeTempCsvForShare(getApplication(), csv, prefix)
      _events.emit(UiEvent.ShareCsv(uri = uri, fileName = "${prefix}.csv"))
    }
  }

  fun requestSaveSessionsCsv(sessions: List<SessionWithLines>, suggestedName: String) {
    if (sessions.isEmpty()) {
      emitMessage("No measurements available to export.")
      return
    }
    viewModelScope.launch {
      val csv = CsvExporter.generateCsv(sessions)
      _events.emit(UiEvent.PromptSaveCsv(csvContent = csv, suggestedFileName = suggestedName))
    }
  }

  fun setThemeMode(mode: ThemeMode) {
    viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
  }

  fun cycleThemeMode() {
    val current = preferences.value.themeMode
    val next = when (current) {
      ThemeMode.DARK -> ThemeMode.LIGHT
      ThemeMode.LIGHT -> ThemeMode.DARK
      ThemeMode.SYSTEM -> ThemeMode.DARK
    }
    setThemeMode(next)
  }

  fun setPreferredUnit(unit: LengthUnit) {
    viewModelScope.launch { preferencesRepository.setPreferredUnit(unit) }
  }

  fun toggleGridOverlay() {
    viewModelScope.launch {
      preferencesRepository.setShowGridOverlay(!preferences.value.showGridOverlay)
    }
  }

  fun toggleMagnifierLoupe() {
    viewModelScope.launch {
      preferencesRepository.setEnableMagnifierLoupe(!preferences.value.enableMagnifierLoupe)
    }
  }

  fun markOnboardingSeen() {
    viewModelScope.launch { preferencesRepository.markOnboardingSeen() }
  }

  private fun clampPoint(point: Point2D, width: Int, height: Int): Point2D {
    return Point2D(
      x = point.x.coerceIn(0f, width.toFloat().coerceAtLeast(1f)),
      y = point.y.coerceIn(0f, height.toFloat().coerceAtLeast(1f)),
    )
  }

  fun emitMessage(message: String) {
    viewModelScope.launch {
      _events.emit(UiEvent.ShowMessage(message))
    }
  }

  class Factory(
    private val application: Application,
    private val measurementRepository: MeasurementRepository,
    private val preferencesRepository: PreferencesRepository,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return MeasureViewModel(application, measurementRepository, preferencesRepository) as T
    }
  }
}
