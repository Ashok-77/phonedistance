package com.example

import android.app.Application
import com.example.data.local.VisionMeasureDatabase
import com.example.data.repository.MeasurementRepository
import com.example.data.repository.PreferencesRepository

class VisionMeasureApplication : Application() {

  val database: VisionMeasureDatabase by lazy {
    VisionMeasureDatabase.getInstance(this)
  }

  val measurementRepository: MeasurementRepository by lazy {
    MeasurementRepository(database.measurementDao())
  }

  val preferencesRepository: PreferencesRepository by lazy {
    PreferencesRepository(this)
  }
}
