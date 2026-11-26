package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOff
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.repository.UserPreferences
import com.example.domain.model.InteractionTool
import com.example.domain.model.MeasurementLine
import com.example.domain.model.ThemeMode
import com.example.domain.model.WorkspaceMode
import com.example.ui.components.CalibrationSheet
import com.example.ui.components.CameraControllerHandle
import com.example.ui.components.CameraPreview
import com.example.ui.components.MeasureCanvas
import com.example.ui.components.ReadoutPanel
import com.example.ui.theme.CalibrationAmber
import com.example.ui.viewmodel.MeasureUiState
import com.example.ui.viewmodel.MeasureViewModel

@Composable
fun MeasureScreen(
  uiState: MeasureUiState,
  preferences: UserPreferences,
  viewModel: MeasureViewModel,
  onOpenOnboarding: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var showCalibrationSheet by remember { mutableStateOf(false) }
  var showSaveDialog by remember { mutableStateOf(false) }
  var renamingLine by remember { mutableStateOf<MeasurementLine?>(null) }
  var cameraErrorText by remember { mutableStateOf<String?>(null) }

  // Check initial permission without prompting automatically
  LaunchedEffect(Unit) {
    val granted = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.CAMERA,
    ) == PackageManager.PERMISSION_GRANTED
    viewModel.onCameraPermissionChanged(granted)
  }

  val cameraPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
  ) { granted ->
    cameraErrorText = null
    viewModel.onCameraPermissionChanged(granted)
    if (!granted) {
      viewModel.emitMessage("Camera permission denied. You can import a photo or use the Practice Blueprint.")
    }
  }

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri ->
    if (uri != null) {
      viewModel.onPhotoPicked(uri)
    }
  }

  val cameraController = remember {
    CameraControllerHandle(
      context = context,
      onCaptureStarted = { viewModel.setCapturingPhoto(true) },
      onPhotoCaptured = { file -> viewModel.onPhotoCaptured(file) },
      onCaptureError = { msg ->
        viewModel.setCapturingPhoto(false)
        viewModel.emitMessage(msg)
      },
    )
  }

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
  ) {
    val isExpandedLayout = maxWidth >= 720.dp

    if (isExpandedLayout) {
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Column(
          modifier = Modifier
            .weight(1.35f)
            .fillMaxHeight(),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          TopInstrumentBar(
            uiState = uiState,
            preferences = preferences,
            onToggleTheme = { viewModel.cycleThemeMode() },
            onToggleGrid = { viewModel.toggleGridOverlay() },
            onOpenHelp = onOpenOnboarding,
          )
          ViewportContainer(
            uiState = uiState,
            preferences = preferences,
            cameraErrorText = cameraErrorText,
            cameraController = cameraController,
            viewModel = viewModel,
            onRequestCameraPermission = {
              cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            },
            onPickPhoto = {
              photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            onCameraError = { err -> cameraErrorText = err },
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth(),
          )
          FloatingMeasurementToolbar(
            uiState = uiState,
            onToggleCalibrateSpan = {
              if (uiState.interactionTool == InteractionTool.CALIBRATE_SPAN) {
                viewModel.setInteractionTool(InteractionTool.MEASURE)
              } else {
                showCalibrationSheet = true
              }
            },
            onClearPending = { viewModel.clearPendingPoint() },
            onResetCanvas = { viewModel.resetCanvas() },
            onSaveSession = { showSaveDialog = true },
            onRetakeOrCamera = {
              cameraErrorText = null
              viewModel.activateLiveCamera()
            },
            onImportPhoto = {
              photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
          )
        }

        Column(
          modifier = Modifier
            .weight(0.85f)
            .fillMaxHeight()
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          ReadoutPanel(
            lines = uiState.lines,
            selectedLine = uiState.selectedLine,
            pendingPointA = uiState.pendingPointA,
            calibration = uiState.calibration,
            onSelectLine = { viewModel.selectLine(it) },
            onRenameLine = { renamingLine = it },
            onDeleteLine = { viewModel.deleteLine(it) },
            onOpenCalibration = { showCalibrationSheet = true },
          )
        }
      }
    } else {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        TopInstrumentBar(
          uiState = uiState,
          preferences = preferences,
          onToggleTheme = { viewModel.cycleThemeMode() },
          onToggleGrid = { viewModel.toggleGridOverlay() },
          onOpenHelp = onOpenOnboarding,
        )

        ViewportContainer(
          uiState = uiState,
          preferences = preferences,
          cameraErrorText = cameraErrorText,
          cameraController = cameraController,
          viewModel = viewModel,
          onRequestCameraPermission = {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
          },
          onPickPhoto = {
            photoPickerLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
          },
          onCameraError = { err -> cameraErrorText = err },
          modifier = Modifier
            .fillMaxWidth()
            .height(370.dp),
        )

        FloatingMeasurementToolbar(
          uiState = uiState,
          onToggleCalibrateSpan = { showCalibrationSheet = true },
          onClearPending = { viewModel.clearPendingPoint() },
          onResetCanvas = { viewModel.resetCanvas() },
          onSaveSession = { showSaveDialog = true },
          onRetakeOrCamera = {
            cameraErrorText = null
            viewModel.activateLiveCamera()
          },
          onImportPhoto = {
            photoPickerLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
          },
        )

        ReadoutPanel(
          lines = uiState.lines,
          selectedLine = uiState.selectedLine,
          pendingPointA = uiState.pendingPointA,
          calibration = uiState.calibration,
          onSelectLine = { viewModel.selectLine(it) },
          onRenameLine = { renamingLine = it },
          onDeleteLine = { viewModel.deleteLine(it) },
          onOpenCalibration = { showCalibrationSheet = true },
        )
      }
    }
  }

  if (showCalibrationSheet) {
    CalibrationSheet(
      calibration = uiState.calibration,
      pendingCalPointA = uiState.pendingCalPointA,
      pendingCalPointB = uiState.pendingCalPointB,
      selectedLine = uiState.selectedLine,
      onDismiss = { showCalibrationSheet = false },
      onStartPickSpanOnCanvas = {
        viewModel.setInteractionTool(InteractionTool.CALIBRATE_SPAN)
        viewModel.emitMessage("Tap 2 points on your reference object or ruler.")
      },
      onUseSelectedLineAsSpan = { viewModel.useSelectedLineForCalibrationSpan() },
      onApplyCalibration = { method, label, length, unit, pA, pB ->
        viewModel.applyCalibration(method, label, length, unit, pA, pB)
      },
      onResetToPixelMode = { viewModel.resetToPixelMode() },
    )
  }

  if (showSaveDialog) {
    SaveSessionDialog(
      initialTitle = uiState.activeSessionTitle.orEmpty(),
      onDismiss = { showSaveDialog = false },
      onConfirm = { title ->
        viewModel.saveCurrentSession(title)
        showSaveDialog = false
      },
    )
  }

  renamingLine?.let { targetLine ->
    RenameLineDialog(
      currentName = targetLine.name,
      onDismiss = { renamingLine = null },
      onConfirm = { newName ->
        viewModel.renameLine(targetLine.id, newName)
        renamingLine = null
      },
    )
  }
}

@Composable
private fun TopInstrumentBar(
  uiState: MeasureUiState,
  preferences: UserPreferences,
  onToggleTheme: () -> Unit,
  onToggleGrid: () -> Unit,
  onOpenHelp: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "VisionMeasure Ruler",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = if (uiState.interactionTool == InteractionTool.CALIBRATE_SPAN) {
            "Calibration Mode • Tap 2 reference points"
          } else if (uiState.workspaceMode == WorkspaceMode.PHOTO_CANVAS) {
            "Photo Canvas • ${uiState.canvasWidth}×${uiState.canvasHeight} px"
          } else {
            "Live Instrument • Local Processing"
          },
          style = MaterialTheme.typography.labelSmall,
          color = if (uiState.interactionTool == InteractionTool.CALIBRATE_SPAN) {
            CalibrationAmber
          } else {
            MaterialTheme.colorScheme.onSurfaceVariant
          },
        )
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = onToggleGrid,
          modifier = Modifier.testTag("toggle_grid_button"),
        ) {
          Icon(
            imageVector = if (preferences.showGridOverlay) Icons.Filled.GridOn else Icons.Filled.GridOff,
            contentDescription = "Toggle precision grid",
            tint = if (preferences.showGridOverlay) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        IconButton(
          onClick = onToggleTheme,
          modifier = Modifier.testTag("toggle_theme_button"),
        ) {
          Icon(
            imageVector = if (preferences.themeMode == ThemeMode.LIGHT) Icons.Filled.DarkMode else Icons.Filled.LightMode,
            contentDescription = "Switch dark or light theme",
          )
        }
        IconButton(
          onClick = onOpenHelp,
          modifier = Modifier.testTag("open_help_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
            contentDescription = "Open measurement guide",
          )
        }
      }
    }
  }
}

@Composable
private fun ViewportContainer(
  uiState: MeasureUiState,
  preferences: UserPreferences,
  cameraErrorText: String?,
  cameraController: CameraControllerHandle,
  viewModel: MeasureViewModel,
  onRequestCameraPermission: () -> Unit,
  onPickPhoto: () -> Unit,
  onCameraError: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.clip(RoundedCornerShape(22.dp)),
    shape = RoundedCornerShape(22.dp),
    color = Color(0xFF070B10),
    border = BorderStroke(
      width = 1.5.dp,
      color = if (uiState.interactionTool == InteractionTool.CALIBRATE_SPAN) CalibrationAmber else MaterialTheme.colorScheme.outline,
    ),
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      val showLiveFeed = uiState.workspaceMode == WorkspaceMode.LIVE_CAMERA &&
        uiState.hasCameraPermission &&
        cameraErrorText == null

      if (uiState.workspaceMode == WorkspaceMode.PHOTO_CANVAS && uiState.photoBitmap != null) {
        MeasureCanvas(
          bitmap = uiState.photoBitmap,
          canvasWidth = uiState.canvasWidth,
          canvasHeight = uiState.canvasHeight,
          lines = uiState.lines,
          pendingPointA = uiState.pendingPointA,
          selectedLineId = uiState.selectedLine?.id,
          calibration = uiState.calibration,
          pendingCalPointA = uiState.pendingCalPointA,
          pendingCalPointB = uiState.pendingCalPointB,
          interactionTool = uiState.interactionTool,
          preferredUnit = preferences.preferredUnit,
          showGridOverlay = preferences.showGridOverlay,
          enableMagnifierLoupe = preferences.enableMagnifierLoupe,
          onCanvasTapped = { viewModel.onCanvasTap(it) },
          onHandleDragged = { id, isA, pt -> viewModel.onHandleDrag(id, isA, pt) },
          onCalibrationHandleDragged = { isA, pt -> viewModel.onCalibrationHandleDrag(isA, pt) },
          onViewSizeChanged = {},
          modifier = Modifier.fillMaxSize(),
        )
      } else if (showLiveFeed) {
        CameraPreview(
          lensFacing = uiState.lensFacing,
          flashEnabled = uiState.isFlashEnabled,
          controllerHandle = cameraController,
          onCapabilitiesDetected = { back, front -> viewModel.updateCameraCapabilities(back, front) },
          onCameraError = onCameraError,
          modifier = Modifier.fillMaxSize(),
        )

        MeasureCanvas(
          bitmap = null,
          canvasWidth = uiState.canvasWidth,
          canvasHeight = uiState.canvasHeight,
          lines = uiState.lines,
          pendingPointA = uiState.pendingPointA,
          selectedLineId = uiState.selectedLine?.id,
          calibration = uiState.calibration,
          pendingCalPointA = uiState.pendingCalPointA,
          pendingCalPointB = uiState.pendingCalPointB,
          interactionTool = uiState.interactionTool,
          preferredUnit = preferences.preferredUnit,
          showGridOverlay = preferences.showGridOverlay,
          enableMagnifierLoupe = false,
          onCanvasTapped = { viewModel.onCanvasTap(it) },
          onHandleDragged = { id, isA, pt -> viewModel.onHandleDrag(id, isA, pt) },
          onCalibrationHandleDragged = { isA, pt -> viewModel.onCalibrationHandleDrag(isA, pt) },
          onViewSizeChanged = { size -> viewModel.updateLiveCanvasDimensions(size.width, size.height) },
          modifier = Modifier.fillMaxSize(),
        )

        // Floating Camera Controls Bar
        Surface(
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 12.dp),
          shape = RoundedCornerShape(28.dp),
          color = Color(0xFF0A1119).copy(alpha = 0.85f),
          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            IconButton(
              onClick = onPickPhoto,
              modifier = Modifier.testTag("camera_bar_import_button"),
            ) {
              Icon(Icons.Filled.PhotoLibrary, contentDescription = "Import photo", tint = Color.White)
            }

            if (uiState.hasFrontCamera && uiState.hasBackCamera) {
              IconButton(
                onClick = { viewModel.toggleCameraLens() },
                modifier = Modifier.testTag("switch_camera_button"),
              ) {
                Icon(Icons.Filled.Cameraswitch, contentDescription = "Switch front or rear camera", tint = Color.White)
              }
            }

            FilledIconButton(
              onClick = { cameraController.capturePhoto() },
              enabled = !uiState.isCapturingPhoto,
              colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
              ),
              modifier = Modifier
                .size(52.dp)
                .testTag("capture_photo_button"),
            ) {
              if (uiState.isCapturingPhoto) {
                CircularProgressIndicator(
                  modifier = Modifier.size(22.dp),
                  strokeWidth = 2.5.dp,
                  color = MaterialTheme.colorScheme.onPrimary,
                )
              } else {
                Icon(Icons.Filled.CameraAlt, contentDescription = "Capture photo to measure")
              }
            }

            IconButton(
              onClick = { viewModel.toggleFlash() },
              enabled = uiState.lensFacing == androidx.camera.core.CameraSelector.LENS_FACING_BACK,
              modifier = Modifier.testTag("toggle_flash_button"),
            ) {
              Icon(
                imageVector = if (uiState.isFlashEnabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                contentDescription = "Toggle flash",
                tint = if (uiState.isFlashEnabled) CalibrationAmber else Color.White,
              )
            }

            IconButton(
              onClick = { viewModel.loadPracticeBlueprint() },
              modifier = Modifier.testTag("camera_bar_blueprint_button"),
            ) {
              Icon(Icons.Filled.Straighten, contentDescription = "Load practice blueprint", tint = Color.White)
            }
          }
        }
      } else {
        // Privacy-First Camera Access / No-Camera Fallback Card
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
        ) {
          Image(
            painter = painterResource(id = R.drawable.img_calibration_guide),
            contentDescription = "VisionMeasure calibration setup",
            contentScale = ContentScale.Crop,
            modifier = Modifier
              .fillMaxWidth()
              .height(92.dp)
              .clip(RoundedCornerShape(14.dp)),
          )
          Spacer(Modifier.height(10.dp))
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Outlined.Lock,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp),
            )
            Text(
              text = stringResource(R.string.camera_permission_title),
              style = MaterialTheme.typography.titleMedium,
              color = Color.White,
            )
          }
          Spacer(Modifier.height(4.dp))
          Text(
            text = cameraErrorText ?: stringResource(R.string.camera_permission_body),
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFB4C5D6),
          )
          Spacer(Modifier.height(10.dp))
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Button(
              onClick = onRequestCameraPermission,
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("enable_camera_button"),
            ) {
              Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text("Open Camera", maxLines = 1)
            }
            FilledTonalButton(
              onClick = onPickPhoto,
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("import_photo_button"),
            ) {
              Icon(Icons.Filled.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text("Import", maxLines = 1)
            }
          }
          Spacer(Modifier.height(8.dp))
          OutlinedButton(
            onClick = { viewModel.loadPracticeBlueprint() },
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("practice_blueprint_button"),
          ) {
            Icon(Icons.Filled.Straighten, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Load Practice Workbench Target")
          }
        }
      }

      // Calibration Span Banner when picking calibration points
      AnimatedVisibility(
        visible = uiState.interactionTool == InteractionTool.CALIBRATE_SPAN,
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(10.dp),
      ) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = CalibrationAmber,
          contentColor = Color(0xFF1F1300),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = "Tap 2 points on your reference",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
            )
            IconButton(
              onClick = { viewModel.setInteractionTool(InteractionTool.MEASURE) },
              modifier = Modifier.size(24.dp),
            ) {
              Icon(Icons.Filled.Close, contentDescription = "Exit calibration span mode", modifier = Modifier.size(16.dp))
            }
          }
        }
      }
    }
  }
}

@Composable
private fun FloatingMeasurementToolbar(
  uiState: MeasureUiState,
  onToggleCalibrateSpan: () -> Unit,
  onClearPending: () -> Unit,
  onResetCanvas: () -> Unit,
  onSaveSession: () -> Unit,
  onRetakeOrCamera: () -> Unit,
  onImportPhoto: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    tonalElevation = 2.dp,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      FilledTonalButton(
        onClick = onToggleCalibrateSpan,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        modifier = Modifier.testTag("toolbar_calibrate_button"),
      ) {
        Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text("Calibrate", style = MaterialTheme.typography.labelLarge)
      }

      if (uiState.pendingPointA != null) {
        FilledTonalIconButton(
          onClick = onClearPending,
          modifier = Modifier.testTag("toolbar_undo_point_button"),
        ) {
          Icon(Icons.Filled.Close, contentDescription = "Cancel pending point A")
        }
      }

      IconButton(
        onClick = onResetCanvas,
        enabled = uiState.lines.isNotEmpty() || uiState.pendingPointA != null || uiState.calibration.isCalibrated,
        modifier = Modifier.testTag("toolbar_reset_button"),
      ) {
        Icon(Icons.Filled.DeleteSweep, contentDescription = "Reset measurement canvas")
      }

      IconButton(
        onClick = onSaveSession,
        enabled = uiState.lines.isNotEmpty(),
        modifier = Modifier.testTag("toolbar_save_button"),
      ) {
        Icon(
          imageVector = Icons.Filled.BookmarkAdd,
          contentDescription = "Save measurement session",
          tint = if (uiState.lines.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      if (uiState.workspaceMode == WorkspaceMode.PHOTO_CANVAS) {
        IconButton(
          onClick = onRetakeOrCamera,
          modifier = Modifier.testTag("toolbar_retake_button"),
        ) {
          Icon(Icons.Filled.Refresh, contentDescription = "Retake photo with camera")
        }
      } else {
        IconButton(
          onClick = onImportPhoto,
          modifier = Modifier.testTag("toolbar_import_button"),
        ) {
          Icon(Icons.Filled.PhotoLibrary, contentDescription = "Import photo")
        }
      }
    }
  }
}

@Composable
private fun SaveSessionDialog(
  initialTitle: String,
  onDismiss: () -> Unit,
  onConfirm: (String) -> Unit,
) {
  var title by remember { mutableStateOf(initialTitle) }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Save Measurement Session") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "Saved strictly to local on-device storage with all lines and calibration data.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Session Title") },
          placeholder = { Text("e.g., Kitchen Countertop") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("save_session_title_input"),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(title) },
        modifier = Modifier.testTag("confirm_save_session_button"),
      ) {
        Text("Save")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}

@Composable
private fun RenameLineDialog(
  currentName: String,
  onDismiss: () -> Unit,
  onConfirm: (String) -> Unit,
) {
  var name by remember { mutableStateOf(currentName) }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Rename Measurement") },
    text = {
      OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Measurement Name") },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("rename_line_input"),
      )
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(name) },
        enabled = name.isNotBlank(),
        modifier = Modifier.testTag("confirm_rename_line_button"),
      ) {
        Text("Update")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}
