package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.LengthUnit
import com.example.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vision_measure_prefs")

data class UserPreferences(
  val themeMode: ThemeMode = ThemeMode.DARK,
  val hasSeenOnboarding: Boolean = false,
  val preferredUnit: LengthUnit = LengthUnit.CENTIMETERS,
  val showGridOverlay: Boolean = true,
  val enableMagnifierLoupe: Boolean = true,
)

class PreferencesRepository(private val context: Context) {

  private object Keys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val HAS_SEEN_ONBOARDING = booleanPreferencesKey("has_seen_onboarding")
    val PREFERRED_UNIT = stringPreferencesKey("preferred_unit")
    val SHOW_GRID = booleanPreferencesKey("show_grid")
    val ENABLE_LOUPE = booleanPreferencesKey("enable_loupe")
  }

  val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
    UserPreferences(
      themeMode = prefs[Keys.THEME_MODE]?.let { name ->
        runCatching { ThemeMode.valueOf(name) }.getOrNull()
      } ?: ThemeMode.DARK,
      hasSeenOnboarding = prefs[Keys.HAS_SEEN_ONBOARDING] ?: false,
      preferredUnit = prefs[Keys.PREFERRED_UNIT]?.let { name ->
        runCatching { LengthUnit.valueOf(name) }.getOrNull()
      } ?: LengthUnit.CENTIMETERS,
      showGridOverlay = prefs[Keys.SHOW_GRID] ?: true,
      enableMagnifierLoupe = prefs[Keys.ENABLE_LOUPE] ?: true,
    )
  }

  suspend fun setThemeMode(mode: ThemeMode) {
    context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
  }

  suspend fun markOnboardingSeen() {
    context.dataStore.edit { it[Keys.HAS_SEEN_ONBOARDING] = true }
  }

  suspend fun setPreferredUnit(unit: LengthUnit) {
    context.dataStore.edit { it[Keys.PREFERRED_UNIT] = unit.name }
  }

  suspend fun setShowGridOverlay(enabled: Boolean) {
    context.dataStore.edit { it[Keys.SHOW_GRID] = enabled }
  }

  suspend fun setEnableMagnifierLoupe(enabled: Boolean) {
    context.dataStore.edit { it[Keys.ENABLE_LOUPE] = enabled }
  }
}
