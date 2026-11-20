package com.androiddoctor.pro

import android.app.Application
import com.androiddoctor.pro.data.ScanHistoryRepository

class AndroidDoctorApp : Application() {
    lateinit var scanHistoryRepository: ScanHistoryRepository
        private set

    override fun onCreate() {
        super.onCreate()
        scanHistoryRepository = ScanHistoryRepository(this)
    }
}
