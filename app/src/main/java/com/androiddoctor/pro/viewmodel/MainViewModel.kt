package com.androiddoctor.pro.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.androiddoctor.pro.AndroidDoctorApp
import com.androiddoctor.pro.data.DeviceInfo
import com.androiddoctor.pro.data.DiagnosticResult
import com.androiddoctor.pro.data.ScanProgress
import com.androiddoctor.pro.data.ScanSession
import com.androiddoctor.pro.data.TestStatus
import com.androiddoctor.pro.diagnostics.DeviceInfoCollector
import com.androiddoctor.pro.diagnostics.DiagnosticEngine
import com.androiddoctor.pro.utils.PdfReportGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class UiState(
    val deviceInfo: DeviceInfo? = null,
    val scanProgress: ScanProgress = ScanProgress(),
    val lastResults: List<DiagnosticResult> = emptyList(),
    val overallScore: Int = 0,
    val history: List<ScanSession> = emptyList(),
    val isExporting: Boolean = false,
    val exportMessage: String? = null,
    val error: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val engine = DiagnosticEngine(application)
    private val repository = (application as AndroidDoctorApp).scanHistoryRepository

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        refreshDeviceInfo()
        viewModelScope.launch {
            repository.historyFlow.collect { sessions ->
                _uiState.update { it.copy(history = sessions) }
            }
        }
    }

    fun refreshDeviceInfo() {
        viewModelScope.launch {
            val info = DeviceInfoCollector.collect(getApplication())
            _uiState.update { it.copy(deviceInfo = info) }
        }
    }

    fun startFullScan() {
        if (_uiState.value.scanProgress.isRunning) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    scanProgress = ScanProgress(isRunning = true, total = 15, completed = 0),
                    lastResults = emptyList(),
                    overallScore = 0,
                    error = null
                )
            }
            try {
                val results = engine.runFullScan { name, completed, total, result ->
                    _uiState.update { state ->
                        val newResults = if (result != null) state.scanProgress.results + result else state.scanProgress.results
                        state.copy(
                            scanProgress = ScanProgress(
                                currentTest = name,
                                completed = completed,
                                total = total,
                                percentage = if (total > 0) completed.toFloat() / total else 0f,
                                isRunning = true,
                                results = newResults
                            )
                        )
                    }
                }
                val score = DiagnosticEngine.calculateOverallScore(results)
                val session = ScanSession(
                    deviceInfo = _uiState.value.deviceInfo ?: DeviceInfoCollector.collect(getApplication()),
                    results = results,
                    overallScore = score,
                    passCount = results.count { it.status == TestStatus.PASS },
                    warningCount = results.count { it.status == TestStatus.WARNING },
                    failedCount = results.count { it.status == TestStatus.FAILED },
                    unavailableCount = results.count {
                        it.status == TestStatus.UNAVAILABLE || it.status == TestStatus.INCONCLUSIVE
                    }
                )
                repository.addSession(session)
                _uiState.update {
                    it.copy(
                        lastResults = results,
                        overallScore = score,
                        scanProgress = ScanProgress(
                            isRunning = false,
                            completed = results.size,
                            total = results.size,
                            percentage = 1f,
                            results = results,
                            currentTest = "Completed"
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        scanProgress = it.scanProgress.copy(isRunning = false),
                        error = e.message
                    )
                }
            }
        }
    }

    fun exportPdf(onComplete: (File?) -> Unit) {
        val state = _uiState.value
        val info = state.deviceInfo ?: return
        val results = state.lastResults
        if (results.isEmpty()) {
            onComplete(null)
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, exportMessage = null) }
            try {
                val file = PdfReportGenerator.generate(
                    context = getApplication(),
                    deviceInfo = info,
                    results = results,
                    overallScore = state.overallScore
                )
                _uiState.update { it.copy(isExporting = false, exportMessage = "Report saved: ${file.name}") }
                onComplete(file)
            } catch (e: Exception) {
                _uiState.update { it.copy(isExporting = false, exportMessage = "Export failed: ${e.message}") }
                onComplete(null)
            }
        }
    }
}
