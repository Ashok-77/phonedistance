package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.OnboardingDialog
import com.example.ui.screens.GuideScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.MeasureScreen
import com.example.ui.theme.VisionMeasureTheme
import com.example.ui.viewmodel.MeasureViewModel
import com.example.ui.viewmodel.UiEvent

private enum class AppDestination(
  val route: String,
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
) {
  MEASURE("measure", "Measure", Icons.Filled.Straighten, Icons.Outlined.Straighten),
  HISTORY("history", "History", Icons.Filled.History, Icons.Outlined.History),
  GUIDE("guide", "Guide", Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook),
}

class MainActivity : ComponentActivity() {

  private val viewModel: MeasureViewModel by viewModels {
    val app = application as VisionMeasureApplication
    MeasureViewModel.Factory(
      application = app,
      measurementRepository = app.measurementRepository,
      preferencesRepository = app.preferencesRepository,
    )
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      VisionMeasureApp(viewModel = viewModel)
    }
  }
}

@Composable
fun VisionMeasureApp(viewModel: MeasureViewModel) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val preferences by viewModel.preferences.collectAsStateWithLifecycle()
  val sessions by viewModel.savedSessions.collectAsStateWithLifecycle()

  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }
  val navController = rememberNavController()
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = backStackEntry?.destination?.route ?: AppDestination.MEASURE.route

  var pendingCsvContent by remember { mutableStateOf<String?>(null) }
  var showOnboardingManual by remember { mutableStateOf(false) }

  val createCsvLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("text/csv"),
  ) { uri ->
    val content = pendingCsvContent
    pendingCsvContent = null
    if (uri != null && content != null) {
      runCatching {
        context.contentResolver.openOutputStream(uri)?.use { out ->
          out.write(content.toByteArray(Charsets.UTF_8))
        }
      }.onSuccess {
        viewModel.emitMessage("CSV exported successfully.")
      }.onFailure {
        viewModel.emitMessage("Failed to write CSV file.")
      }
    }
  }

  LaunchedEffect(viewModel) {
    viewModel.events.collect { event ->
      when (event) {
        is UiEvent.ShowMessage -> {
          snackbarHostState.showSnackbar(event.message)
        }
        is UiEvent.ShareCsv -> {
          val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, event.uri)
            putExtra(Intent.EXTRA_SUBJECT, "VisionMeasure Export (${event.fileName})")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
          }
          context.startActivity(Intent.createChooser(intent, "Share Measurement CSV"))
        }
        is UiEvent.PromptSaveCsv -> {
          pendingCsvContent = event.csvContent
          createCsvLauncher.launch(event.suggestedFileName)
        }
      }
    }
  }

  VisionMeasureTheme(themeMode = preferences.themeMode) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val useNavigationRail = maxWidth >= 720.dp

      Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
          if (!useNavigationRail) {
            NavigationBar(windowInsets = WindowInsets.navigationBars) {
              AppDestination.entries.forEach { dest ->
                val selected = currentRoute == dest.route
                NavigationBarItem(
                  selected = selected,
                  onClick = {
                    navController.navigate(dest.route) {
                      popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                      launchSingleTop = true
                      restoreState = true
                    }
                  },
                  icon = {
                    Icon(
                      imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                      contentDescription = dest.label,
                    )
                  },
                  label = { Text(dest.label) },
                  modifier = Modifier.testTag("nav_${dest.route}"),
                )
              }
            }
          }
        },
      ) { innerPadding ->
        Row(
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        ) {
          if (useNavigationRail) {
            NavigationRail {
              AppDestination.entries.forEach { dest ->
                val selected = currentRoute == dest.route
                NavigationRailItem(
                  selected = selected,
                  onClick = {
                    navController.navigate(dest.route) {
                      popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                      launchSingleTop = true
                      restoreState = true
                    }
                  },
                  icon = {
                    Icon(
                      imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                      contentDescription = dest.label,
                    )
                  },
                  label = { Text(dest.label) },
                  modifier = Modifier.testTag("rail_${dest.route}"),
                )
              }
            }
          }

          Box(modifier = Modifier.weight(1f).fillMaxSize()) {
            NavHost(
              navController = navController,
              startDestination = AppDestination.MEASURE.route,
            ) {
              composable(AppDestination.MEASURE.route) {
                MeasureScreen(
                  uiState = uiState,
                  preferences = preferences,
                  viewModel = viewModel,
                  onOpenOnboarding = { showOnboardingManual = true },
                )
              }
              composable(AppDestination.HISTORY.route) {
                HistoryScreen(
                  sessions = sessions,
                  onOpenSession = { item ->
                    viewModel.loadSessionIntoWorkspace(item)
                    navController.navigate(AppDestination.MEASURE.route) {
                      popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                      launchSingleTop = true
                      restoreState = true
                    }
                  },
                  onDeleteSession = { viewModel.deleteSession(it) },
                  onClearAll = { viewModel.clearAllHistory() },
                  onShareCsv = { list, prefix -> viewModel.shareSessionsCsv(list, prefix) },
                  onSaveCsvToDevice = { list, name -> viewModel.requestSaveSessionsCsv(list, name) },
                  onGoToMeasure = {
                    navController.navigate(AppDestination.MEASURE.route) {
                      popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                      launchSingleTop = true
                      restoreState = true
                    }
                  },
                )
              }
              composable(AppDestination.GUIDE.route) {
                GuideScreen(
                  preferences = preferences,
                  onThemeModeChanged = { viewModel.setThemeMode(it) },
                  onPreferredUnitChanged = { viewModel.setPreferredUnit(it) },
                  onToggleGrid = { viewModel.toggleGridOverlay() },
                  onToggleLoupe = { viewModel.toggleMagnifierLoupe() },
                )
              }
            }
          }
        }
      }

      if (!preferences.hasSeenOnboarding || showOnboardingManual) {
        OnboardingDialog(
          onDismiss = {
            showOnboardingManual = false
            if (!preferences.hasSeenOnboarding) {
              viewModel.markOnboardingSeen()
            }
          },
        )
      }
    }
  }
}
