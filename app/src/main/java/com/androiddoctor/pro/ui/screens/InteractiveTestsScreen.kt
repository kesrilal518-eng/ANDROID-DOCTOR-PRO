package com.androiddoctor.pro.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.androiddoctor.pro.ui.components.NeonButton
import com.androiddoctor.pro.ui.theme.CardDark
import com.androiddoctor.pro.ui.theme.DeepBlack
import com.androiddoctor.pro.ui.theme.NeonBlue
import com.androiddoctor.pro.ui.theme.NeonGreen
import com.androiddoctor.pro.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractiveTestsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var statusMessage by remember { mutableStateOf("Select a test below.") }
    val touchPoints = remember { mutableStateListOf<Offset>() }
    var showTouchOverlay by remember { mutableStateOf(false) }
    var displayColor by remember { mutableStateOf(Color.Black) }
    var showDisplayTest by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        statusMessage = if (results.values.all { it }) {
            "Permissions granted. You can run audio/camera tests."
        } else {
            "Some permissions were denied. Related tests may be limited."
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Interactive Tests", color = NeonGreen, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = NeonBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepBlack)
            )
        },
        containerColor = DeepBlack
    ) { padding ->
        if (showDisplayTest) {
            Box(modifier = Modifier.fillMaxSize().background(displayColor).padding(padding)) {
                Column(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
                    NeonButton(text = "White", onClick = { displayColor = Color.White })
                    Spacer(Modifier.height(8.dp))
                    NeonButton(text = "Red", onClick = { displayColor = Color.Red })
                    Spacer(Modifier.height(8.dp))
                    NeonButton(text = "Green", onClick = { displayColor = Color.Green })
                    Spacer(Modifier.height(8.dp))
                    NeonButton(text = "Blue", onClick = { displayColor = Color.Blue })
                    Spacer(Modifier.height(8.dp))
                    NeonButton(text = "Black / Exit", onClick = {
                        displayColor = Color.Black
                        showDisplayTest = false
                    })
                }
            }
            return@Scaffold
        }

        if (showTouchOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DeepBlack)
                    .padding(padding)
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ -> touchPoints.add(change.position) }
                    }
            ) {
                Column(modifier = Modifier.align(Alignment.Center).padding(24.dp)) {
                    Text(
                        "Touch / drag anywhere.\nPoints recorded: ${touchPoints.size}",
                        color = NeonGreen, fontSize = 16.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    NeonButton(text = "Clear & Exit", onClick = {
                        touchPoints.clear()
                        showTouchOverlay = false
                        statusMessage = "Touch test finished. Review for dead zones visually."
                    })
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                statusMessage,
                color = TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth().background(CardDark, RoundedCornerShape(8.dp)).padding(12.dp)
            )
            Spacer(Modifier.height(16.dp))

            NeonButton(text = "Request Runtime Permissions", onClick = {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                )
            })
            Spacer(Modifier.height(10.dp))
            NeonButton(text = "Display Color Test", onClick = {
                showDisplayTest = true
                statusMessage = "Visually inspect for dead pixels, discoloration, or backlight issues."
            })
            Spacer(Modifier.height(10.dp))
            NeonButton(text = "Touchscreen Test", onClick = {
                touchPoints.clear()
                showTouchOverlay = true
                statusMessage = "Draw across the entire screen."
            })
            Spacer(Modifier.height(10.dp))
            NeonButton(text = "Vibration Test", onClick = {
                try {
                    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val vm = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                        vm.defaultVibrator
                    } else {
                        @Suppress("DEPRECATION")
                        context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as Vibrator
                    }
                    if (vibrator.hasVibrator()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200), -1))
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator.vibrate(longArrayOf(0, 200, 100, 200), -1)
                        }
                        statusMessage = "Vibration pattern triggered. Did you feel it?"
                    } else {
                        statusMessage = "No vibrator available on this device."
                    }
                } catch (e: Exception) {
                    statusMessage = "Vibration error: ${e.message}"
                }
            })
            Spacer(Modifier.height(10.dp))
            NeonButton(text = "Microphone Quick Check", onClick = {
                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                statusMessage = if (!granted) "RECORD_AUDIO permission required."
                else "Microphone permission OK. For full test, record and playback."
            })
            Spacer(Modifier.height(10.dp))
            NeonButton(text = "Camera Presence Check", onClick = {
                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                if (!granted) {
                    statusMessage = "CAMERA permission required."
                    return@NeonButton
                }
                try {
                    val cm = context.getSystemService(android.content.Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
                    statusMessage = "Cameras found: ${cm.cameraIdList.joinToString()}. Open system camera for quality check."
                } catch (e: Exception) {
                    statusMessage = "Camera error: ${e.message}"
                }
            })
            Spacer(Modifier.height(24.dp))
            Text(
                "These interactive tests require your observation. Software cannot fully certify hardware without user feedback and cannot detect internal IC faults.",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}
