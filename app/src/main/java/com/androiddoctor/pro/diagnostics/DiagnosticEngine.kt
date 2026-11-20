package com.androiddoctor.pro.diagnostics

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.telephony.TelephonyManager
import com.androiddoctor.pro.data.DiagnosticResult
import com.androiddoctor.pro.data.TestStatus
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

class DiagnosticEngine(private val context: Context) {

    suspend fun runFullScan(onProgress: (String, Int, Int, DiagnosticResult?) -> Unit): List<DiagnosticResult> {
        val results = mutableListOf<DiagnosticResult>()
        val tests = listOf(
            "CPU & RAM" to ::testCpuRam,
            "Battery & Charging" to ::testBattery,
            "Storage" to ::testStorage,
            "Display" to ::testDisplay,
            "Touchscreen" to ::testTouchscreen,
            "Camera" to ::testCamera,
            "Microphone" to ::testMicrophone,
            "Speaker" to ::testSpeaker,
            "Vibration" to ::testVibration,
            "Wi-Fi" to ::testWifi,
            "Bluetooth" to ::testBluetooth,
            "Mobile Network" to ::testMobileNetwork,
            "Sensors" to ::testSensors,
            "Thermal" to ::testThermal,
            "USB Connection" to ::testUsb
        )
        val total = tests.size
        tests.forEachIndexed { index, (name, testFn) ->
            onProgress(name, index, total, null)
            delay(350)
            val result = try {
                testFn()
            } catch (e: Exception) {
                DiagnosticResult(
                    category = name, name = name, status = TestStatus.INCONCLUSIVE,
                    message = "Test could not complete: ${e.message}",
                    recommendation = "Retry the test or check app permissions."
                )
            }
            results.add(result)
            onProgress(name, index + 1, total, result)
        }
        return results
    }

    private fun testCpuRam(): DiagnosticResult {
        val info = DeviceInfoCollector.collect(context)
        val usedPct = ((info.totalRamMb - info.availableRamMb).toDouble() / info.totalRamMb * 100).roundToInt()
        val status = when {
            usedPct > 92 -> TestStatus.WARNING
            info.availableRamMb < 200 -> TestStatus.WARNING
            else -> TestStatus.PASS
        }
        return DiagnosticResult(
            category = "Performance", name = "CPU & RAM", status = status,
            score = if (status == TestStatus.PASS) 100 else 70,
            message = "RAM: ${info.availableRamMb} MB free of ${info.totalRamMb} MB ($usedPct% used). Cores: ${info.cpuCores}. ABI: ${info.cpuAbi}",
            details = DeviceInfoCollector.getCpuInfo().take(500),
            recommendation = if (status == TestStatus.WARNING)
                "Close unused apps or restart the device. High RAM usage can cause lag but does not prove hardware failure."
            else "Memory and CPU configuration appear normal."
        )
    }

    private fun testBattery(): DiagnosticResult {
        val info = DeviceInfoCollector.collect(context)
        val status = when {
            info.batteryHealth.contains("Dead", true) || info.batteryHealth.contains("Failure", true) -> TestStatus.FAILED
            info.batteryTemperatureC > 45f -> TestStatus.WARNING
            info.batteryTemperatureC > 40f -> TestStatus.WARNING
            info.batteryHealth != "Good" && info.batteryHealth != "Unknown" -> TestStatus.WARNING
            else -> TestStatus.PASS
        }
        return DiagnosticResult(
            category = "Battery", name = "Battery & Charging", status = status,
            score = when (status) { TestStatus.PASS -> 100; TestStatus.WARNING -> 65; else -> 30 },
            message = "Level: ${info.batteryLevel}% | Temp: ${info.batteryTemperatureC}°C | Health: ${info.batteryHealth} | Charging: ${info.isCharging} | Tech: ${info.batteryTechnology}",
            recommendation = when (status) {
                TestStatus.FAILED -> "Battery health reports a serious issue. Consider professional battery check. Software alone cannot confirm physical damage."
                TestStatus.WARNING -> "Monitor temperature and health. Avoid extreme heat. If swelling or rapid drain occurs, seek service."
                else -> "Battery metrics look normal. Temperature and reported health are within expected ranges."
            }
        )
    }

    private fun testStorage(): DiagnosticResult {
        val info = DeviceInfoCollector.collect(context)
        val freePct = (info.availableStorageGb / info.totalStorageGb * 100).roundToInt()
        val status = when {
            freePct < 5 -> TestStatus.WARNING
            freePct < 10 -> TestStatus.WARNING
            else -> TestStatus.PASS
        }
        return DiagnosticResult(
            category = "Storage", name = "Storage Analysis", status = status,
            score = if (status == TestStatus.PASS) 100 else 60,
            message = "Available: ${"%.2f".format(info.availableStorageGb)} GB of ${"%.2f".format(info.totalStorageGb)} GB ($freePct% free)",
            recommendation = if (status == TestStatus.WARNING)
                "Free up space by removing unused apps, media, or cache. Low storage can cause crashes and slow performance."
            else "Storage capacity is healthy."
        )
    }

    private fun testDisplay(): DiagnosticResult {
        val info = DeviceInfoCollector.collect(context)
        return DiagnosticResult(
            category = "Display", name = "Display Configuration", status = TestStatus.PASS, score = 100,
            message = "${info.screenWidthPx}×${info.screenHeightPx} @ ${info.densityDpi} dpi, ${"%.1f".format(info.refreshRateHz)} Hz",
            details = "Software can report resolution and refresh rate but cannot detect dead pixels, burn-in or backlight issues without visual inspection.",
            recommendation = "For dead pixel or color tests, use the interactive Display Test screen and visually inspect the panel."
        )
    }

    private fun testTouchscreen(): DiagnosticResult {
        return DiagnosticResult(
            category = "Input", name = "Touchscreen", status = TestStatus.INCONCLUSIVE, score = 50,
            message = "Automated scan cannot fully validate touch accuracy. Interactive test required.",
            recommendation = "Open the Touchscreen Test module and draw across the entire screen. Check for unresponsive zones."
        )
    }

    private fun testCamera(): DiagnosticResult {
        return try {
            val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val ids = cm.cameraIdList
            if (ids.isEmpty()) {
                DiagnosticResult(category = "Camera", name = "Camera", status = TestStatus.UNAVAILABLE,
                    message = "No cameras detected by CameraManager.",
                    recommendation = "If the device has cameras, check permissions or hardware.")
            } else {
                DiagnosticResult(category = "Camera", name = "Camera", status = TestStatus.PASS, score = 90,
                    message = "Detected ${ids.size} camera(s): ${ids.joinToString()}",
                    details = "Presence of camera IDs confirms the HAL reports cameras. Image quality requires interactive testing.",
                    recommendation = "Use the Camera Test module to open preview and capture a sample photo.")
            }
        } catch (e: SecurityException) {
            DiagnosticResult(category = "Camera", name = "Camera", status = TestStatus.UNAVAILABLE,
                message = "Camera permission not granted.", recommendation = "Grant CAMERA permission to run this test.")
        } catch (e: Exception) {
            DiagnosticResult(category = "Camera", name = "Camera", status = TestStatus.INCONCLUSIVE,
                message = "Unable to query cameras: ${e.message}")
        }
    }

    private fun testMicrophone(): DiagnosticResult {
        return DiagnosticResult(
            category = "Audio", name = "Microphone", status = TestStatus.INCONCLUSIVE, score = 50,
            message = "Microphone presence cannot be fully verified without recording. Interactive test recommended.",
            recommendation = "Grant RECORD_AUDIO and use the Microphone Test to record and play back a short sample."
        )
    }

    private fun testSpeaker(): DiagnosticResult {
        return DiagnosticResult(
            category = "Audio", name = "Speaker", status = TestStatus.INCONCLUSIVE, score = 50,
            message = "Speaker output requires interactive playback test.",
            recommendation = "Use the Speaker Test module to play a tone and verify both earpiece and loudspeaker."
        )
    }

    private fun testVibration(): DiagnosticResult {
        return try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(80)
                }
                DiagnosticResult(category = "Haptics", name = "Vibration", status = TestStatus.PASS, score = 100,
                    message = "Vibrator hardware reported present and a short pulse was triggered.",
                    recommendation = "If you did not feel vibration, check Do Not Disturb / vibration settings or hardware.")
            } else {
                DiagnosticResult(category = "Haptics", name = "Vibration", status = TestStatus.UNAVAILABLE,
                    message = "No vibrator reported by the system.")
            }
        } catch (e: Exception) {
            DiagnosticResult(category = "Haptics", name = "Vibration", status = TestStatus.INCONCLUSIVE,
                message = "Vibration test error: ${e.message}")
        }
    }

    private fun testWifi(): DiagnosticResult {
        return try {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork
            val caps = cm.getNetworkCapabilities(network)
            val hasWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            val enabled = wifi.isWifiEnabled
            val status = when {
                !enabled -> TestStatus.WARNING
                hasWifi -> TestStatus.PASS
                else -> TestStatus.WARNING
            }
            DiagnosticResult(category = "Connectivity", name = "Wi-Fi", status = status,
                score = if (status == TestStatus.PASS) 100 else 70,
                message = "Wi-Fi enabled: $enabled | Connected: $hasWifi",
                recommendation = if (!enabled) "Enable Wi-Fi to fully test connectivity."
                else if (!hasWifi) "Wi-Fi is on but not connected. Try connecting to a network."
                else "Wi-Fi appears functional.")
        } catch (e: Exception) {
            DiagnosticResult(category = "Connectivity", name = "Wi-Fi", status = TestStatus.INCONCLUSIVE, message = e.message ?: "Error")
        }
    }

    private fun testBluetooth(): DiagnosticResult {
        return try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            if (adapter == null) {
                DiagnosticResult(category = "Connectivity", name = "Bluetooth", status = TestStatus.UNAVAILABLE,
                    message = "No Bluetooth adapter found.")
            } else {
                val enabled = adapter.isEnabled
                DiagnosticResult(category = "Connectivity", name = "Bluetooth",
                    status = if (enabled) TestStatus.PASS else TestStatus.WARNING,
                    score = if (enabled) 100 else 70,
                    message = "Bluetooth adapter present. Enabled: $enabled",
                    recommendation = if (!enabled) "Enable Bluetooth to complete functional testing."
                    else "Adapter is available. Pairing tests require interactive use.")
            }
        } catch (e: SecurityException) {
            DiagnosticResult(category = "Connectivity", name = "Bluetooth", status = TestStatus.UNAVAILABLE,
                message = "Bluetooth permission not granted.",
                recommendation = "Grant BLUETOOTH_CONNECT / BLUETOOTH_SCAN permissions.")
        } catch (e: Exception) {
            DiagnosticResult(category = "Connectivity", name = "Bluetooth", status = TestStatus.INCONCLUSIVE, message = e.message ?: "Error")
        }
    }

    private fun testMobileNetwork(): DiagnosticResult {
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork
            val caps = cm.getNetworkCapabilities(network)
            val hasCellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
            val networkType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                when (tm.dataNetworkType) {
                    TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
                    TelephonyManager.NETWORK_TYPE_NR -> "5G NR"
                    TelephonyManager.NETWORK_TYPE_HSPAP, TelephonyManager.NETWORK_TYPE_HSPA, TelephonyManager.NETWORK_TYPE_UMTS -> "3G"
                    TelephonyManager.NETWORK_TYPE_EDGE, TelephonyManager.NETWORK_TYPE_GPRS -> "2G"
                    else -> "Unknown/Other"
                }
            } else "N/A"
            DiagnosticResult(category = "Connectivity", name = "Mobile Network",
                status = if (hasCellular) TestStatus.PASS else TestStatus.WARNING,
                score = if (hasCellular) 100 else 60,
                message = "Cellular transport: $hasCellular | Reported type: $networkType | Operator: ${tm.networkOperatorName ?: "N/A"}",
                recommendation = if (!hasCellular) "No active cellular data connection detected. Check SIM, airplane mode, or signal."
                else "Mobile data path appears available.")
        } catch (e: SecurityException) {
            DiagnosticResult(category = "Connectivity", name = "Mobile Network", status = TestStatus.UNAVAILABLE,
                message = "READ_PHONE_STATE or location permission required for detailed network info.",
                recommendation = "Grant necessary permissions for deeper network diagnostics.")
        } catch (e: Exception) {
            DiagnosticResult(category = "Connectivity", name = "Mobile Network", status = TestStatus.INCONCLUSIVE, message = e.message ?: "Error")
        }
    }

    private fun testSensors(): DiagnosticResult {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensors = sm.getSensorList(Sensor.TYPE_ALL)
        val important = listOf(
            Sensor.TYPE_ACCELEROMETER to "Accelerometer",
            Sensor.TYPE_GYROSCOPE to "Gyroscope",
            Sensor.TYPE_PROXIMITY to "Proximity",
            Sensor.TYPE_LIGHT to "Light",
            Sensor.TYPE_MAGNETIC_FIELD to "Magnetometer"
        )
        val present = important.map { (type, name) -> name to (sm.getDefaultSensor(type) != null) }
        val missing = present.filter { !it.second }.map { it.first }
        val status = when {
            present.none { it.second } -> TestStatus.WARNING
            missing.isNotEmpty() -> TestStatus.WARNING
            else -> TestStatus.PASS
        }
        return DiagnosticResult(
            category = "Sensors", name = "Sensors", status = status,
            score = if (status == TestStatus.PASS) 100 else 75,
            message = "Total sensors: ${sensors.size}. Key sensors: ${present.joinToString { "${it.first}=${if (it.second) "OK" else "Missing"}" }}",
            details = "Missing key sensors: ${missing.joinToString().ifBlank { "None" }}",
            recommendation = if (missing.isNotEmpty())
                "Some common sensors are not reported. This may be normal for the device model or indicate a hardware limitation."
            else "Common sensors are present."
        )
    }

    private fun testThermal(): DiagnosticResult {
        val info = DeviceInfoCollector.collect(context)
        val status = when {
            info.batteryTemperatureC >= 48f -> TestStatus.WARNING
            info.batteryTemperatureC >= 43f -> TestStatus.WARNING
            else -> TestStatus.PASS
        }
        return DiagnosticResult(
            category = "Thermal", name = "Thermal Status", status = status,
            score = if (status == TestStatus.PASS) 100 else 60,
            message = "Battery temperature: ${info.batteryTemperatureC}°C",
            details = "Android does not expose full SoC thermal zones without system privileges. Battery temperature is used as a proxy.",
            recommendation = if (status == TestStatus.WARNING)
                "Device is warm. Avoid heavy use while charging, remove case if needed, and ensure good ventilation. Persistent high temps warrant professional inspection."
            else "Temperature is within a normal range for the battery sensor."
        )
    }

    private fun testUsb(): DiagnosticResult {
        val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val plugged = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val usb = plugged == BatteryManager.BATTERY_PLUGGED_USB
        val ac = plugged == BatteryManager.BATTERY_PLUGGED_AC
        val wireless = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS
        } else false
        return DiagnosticResult(
            category = "Connectivity", name = "USB / Charging Port", status = TestStatus.INCONCLUSIVE, score = 50,
            message = "USB power: $usb | AC: $ac | Wireless: $wireless",
            details = "Software can detect power source when connected but cannot fully test data lines, USB-C pin integrity, or OTG without additional hardware.",
            recommendation = "Connect a charger or USB cable and re-run. For data transfer issues, try different cables and ports. Physical damage requires visual/hardware inspection."
        )
    }

    companion object {
        fun calculateOverallScore(results: List<DiagnosticResult>): Int {
            val scored = results.filter {
                it.status != TestStatus.UNAVAILABLE &&
                        it.status != TestStatus.INCONCLUSIVE &&
                        it.status != TestStatus.PENDING &&
                        it.status != TestStatus.RUNNING
            }
            if (scored.isEmpty()) return 0
            return scored.map { it.score }.average().roundToInt().coerceIn(0, 100)
        }
    }
}
