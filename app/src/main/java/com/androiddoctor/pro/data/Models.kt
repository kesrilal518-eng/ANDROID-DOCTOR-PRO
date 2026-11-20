package com.androiddoctor.pro.data

import java.util.UUID

enum class TestStatus {
    PASS,
    WARNING,
    FAILED,
    UNAVAILABLE,
    INCONCLUSIVE,
    PENDING,
    RUNNING
}

data class DiagnosticResult(
    val id: String = UUID.randomUUID().toString(),
    val category: String,
    val name: String,
    val status: TestStatus,
    val score: Int = 0,
    val message: String,
    val details: String = "",
    val recommendation: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class DeviceInfo(
    val manufacturer: String,
    val brand: String,
    val model: String,
    val device: String,
    val product: String,
    val androidVersion: String,
    val sdkInt: Int,
    val securityPatch: String,
    val totalRamMb: Long,
    val availableRamMb: Long,
    val totalStorageGb: Double,
    val availableStorageGb: Double,
    val screenWidthPx: Int,
    val screenHeightPx: Int,
    val densityDpi: Int,
    val refreshRateHz: Float,
    val cpuAbi: String,
    val cpuCores: Int,
    val batteryLevel: Int,
    val batteryTemperatureC: Float,
    val isCharging: Boolean,
    val batteryHealth: String,
    val batteryTechnology: String
)

data class ScanSession(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val deviceInfo: DeviceInfo,
    val results: List<DiagnosticResult>,
    val overallScore: Int,
    val passCount: Int,
    val warningCount: Int,
    val failedCount: Int,
    val unavailableCount: Int
)

data class ScanProgress(
    val currentTest: String = "",
    val completed: Int = 0,
    val total: Int = 0,
    val percentage: Float = 0f,
    val isRunning: Boolean = false,
    val results: List<DiagnosticResult> = emptyList()
)
