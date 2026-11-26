package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun `app_name matches VisionMeasure`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("VisionMeasure", appName)
  }

  @Test
  fun `onboarding dismisses and loads practice blueprint`() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("onboarding_start_button")
      .performScrollTo()
      .assertIsDisplayed()
      .performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("practice_blueprint_button")
      .performScrollTo()
      .assertIsDisplayed()
      .performClick()
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule.onAllNodes(androidx.compose.ui.test.hasTestTag("measure_canvas"))
        .fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("measure_canvas").assertIsDisplayed()
    composeTestRule.onNodeWithTag("toolbar_calibrate_button").performScrollTo().assertIsDisplayed()

    composeTestRule.onNodeWithTag("nav_history").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("empty_history_measure_button").performScrollTo().assertIsDisplayed()

    composeTestRule.onNodeWithTag("nav_guide").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("guide_theme_light").performScrollTo().assertIsDisplayed()
  }
}
