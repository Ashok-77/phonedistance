package com.example.ui.components

import android.content.Context
import android.view.ViewGroup
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.local.ImageProcessor
import java.io.File

class CameraControllerHandle(
  private val context: Context,
  private val onCaptureStarted: () -> Unit,
  private val onPhotoCaptured: (File) -> Unit,
  private val onCaptureError: (String) -> Unit,
) {
  internal var imageCapture: ImageCapture? = null
  internal var boundCamera: Camera? = null

  fun capturePhoto() {
    val capture = imageCapture ?: run {
      onCaptureError("Camera is still initializing.")
      return
    }
    onCaptureStarted()
    val outputFile = ImageProcessor.createCaptureFile(context)
    val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
    capture.takePicture(
      outputOptions,
      ContextCompat.getMainExecutor(context),
      object : ImageCapture.OnImageSavedCallback {
        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
          onPhotoCaptured(outputFile)
        }

        override fun onError(exception: ImageCaptureException) {
          outputFile.delete()
          onCaptureError(exception.message ?: "Failed to capture image.")
        }
      },
    )
  }
}

@Composable
fun CameraPreview(
  lensFacing: Int,
  flashEnabled: Boolean,
  controllerHandle: CameraControllerHandle,
  onCapabilitiesDetected: (hasBack: Boolean, hasFront: Boolean) -> Unit,
  onCameraError: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val currentCapabilitiesCallback by rememberUpdatedState(onCapabilitiesDetected)
  val currentErrorCallback by rememberUpdatedState(onCameraError)

  var previewView by remember { mutableStateOf<PreviewView?>(null) }

  LaunchedEffect(controllerHandle.boundCamera, flashEnabled) {
    val camera = controllerHandle.boundCamera ?: return@LaunchedEffect
    if (camera.cameraInfo.hasFlashUnit()) {
      camera.cameraControl.enableTorch(flashEnabled)
    }
  }

  DisposableEffect(previewView, lensFacing, lifecycleOwner) {
    val view = previewView ?: return@DisposableEffect onDispose {}
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    val executor = ContextCompat.getMainExecutor(context)

    val listener = Runnable {
      try {
        val cameraProvider = cameraProviderFuture.get()
        val hasBack = runCatching {
          cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
        }.getOrDefault(false)
        val hasFront = runCatching {
          cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
        }.getOrDefault(false)

        currentCapabilitiesCallback(hasBack, hasFront)

        if (!hasBack && !hasFront) {
          currentErrorCallback("No camera hardware detected on this device. You can import a photo or use the Practice Blueprint.")
          return@Runnable
        }

        val selector = when {
          lensFacing == CameraSelector.LENS_FACING_FRONT && hasFront -> CameraSelector.DEFAULT_FRONT_CAMERA
          hasBack -> CameraSelector.DEFAULT_BACK_CAMERA
          else -> CameraSelector.DEFAULT_FRONT_CAMERA
        }

        val preview = Preview.Builder().build().also {
          it.surfaceProvider = view.surfaceProvider
        }
        val imageCapture = ImageCapture.Builder()
          .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
          .build()

        cameraProvider.unbindAll()
        val camera = cameraProvider.bindToLifecycle(
          lifecycleOwner,
          selector,
          preview,
          imageCapture,
        )
        controllerHandle.imageCapture = imageCapture
        controllerHandle.boundCamera = camera
        if (camera.cameraInfo.hasFlashUnit()) {
          camera.cameraControl.enableTorch(flashEnabled)
        }
      } catch (t: Throwable) {
        currentErrorCallback(t.message ?: "Unable to start camera preview.")
      }
    }

    cameraProviderFuture.addListener(listener, executor)

    onDispose {
      runCatching {
        cameraProviderFuture.get().unbindAll()
      }
      controllerHandle.boundCamera = null
      controllerHandle.imageCapture = null
    }
  }

  AndroidView(
    factory = { ctx ->
      PreviewView(ctx).apply {
        layoutParams = ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT,
        )
        scaleType = PreviewView.ScaleType.FIT_CENTER
        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
      }.also { previewView = it }
    },
    modifier = modifier,
  )
}
