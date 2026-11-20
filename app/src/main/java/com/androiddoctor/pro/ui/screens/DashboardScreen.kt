package com.androiddoctor.pro.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.androiddoctor.pro.ui.components.DeviceInfoCard
import com.androiddoctor.pro.ui.components.DiagnosticResultCard
import com.androiddoctor.pro.ui.components.HealthScoreRing
import com.androiddoctor.pro.ui.components.NeonButton
import com.androiddoctor.pro.ui.components.ScanProgressBar
import com.androiddoctor.pro.ui.theme.DeepBlack
import com.androiddoctor.pro.ui.theme.NeonBlue
import com.androiddoctor.pro.ui.theme.NeonGreen
import com.androiddoctor.pro.ui.theme.TextSecondary
import com.androiddoctor.pro.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateHistory: () -> Unit,
    onNavigateInteractive: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val info = state.deviceInfo

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("ANDROID DOCTOR PRO", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                actions = {
                    IconButton(onClick = onNavigateHistory) {
                        Icon(Icons.Default.History, contentDescription = "History", tint = NeonBlue)
                    }
                    IconButton(onClick = {
                        viewModel.exportPdf { file ->
                            if (file != null) {
                                val uri = FileProvider.getUriForFile(
                                    context, "${context.packageName}.fileprovider", file
                                )
                                val share = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(share, "Share Diagnostic Report"))
                            }
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Export PDF", tint = NeonBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepBlack, titleContentColor = NeonGreen)
            )
        },
        containerColor = DeepBlack
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).background(DeepBlack),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                if (info != null) {
                    DeviceInfoCard(
                        manufacturer = info.manufacturer,
                        model = info.model,
                        androidVersion = "${info.androidVersion} (SDK ${info.sdkInt})",
                        ram = "${info.availableRamMb} MB",
                        storage = "${"%.1f".format(info.availableStorageGb)} GB",
                        battery = "${info.batteryLevel}%"
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HealthScoreRing(score = state.overallScore)
                    Column {
                        Text("Overall Health", color = TextSecondary, fontSize = 13.sp)
                        Text(
                            text = when {
                                state.overallScore >= 80 -> "Good"
                                state.overallScore >= 60 -> "Fair"
                                state.overallScore > 0 -> "Needs Attention"
                                else -> "Not scanned"
                            },
                            color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("Based only on available tests", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
            item {
                if (state.scanProgress.isRunning || state.scanProgress.percentage > 0f) {
                    ScanProgressBar(
                        progress = state.scanProgress.percentage,
                        currentTest = state.scanProgress.currentTest,
                        isRunning = state.scanProgress.isRunning
                    )
                }
            }
            item {
                NeonButton(
                    text = if (state.scanProgress.isRunning) "SCANNING..." else "FULL DEVICE SCAN",
                    onClick = { viewModel.startFullScan() },
                    enabled = !state.scanProgress.isRunning
                )
            }
            item {
                NeonButton(
                    text = "INTERACTIVE TESTS",
                    onClick = onNavigateInteractive,
                    enabled = !state.scanProgress.isRunning
                )
            }
            if (state.lastResults.isNotEmpty()) {
                item {
                    Text("Latest Results", color = NeonBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                items(state.lastResults) { result ->
                    DiagnosticResultCard(result)
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Disclaimer: Software diagnostics only. Cannot detect motherboard or IC faults. No root required.",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}
