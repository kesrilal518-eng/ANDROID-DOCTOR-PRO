# ANDROID DOCTOR PRO

**Professional software-based Android device diagnostics**

Premium black UI with neon green / blue accents. Built with **Kotlin**, **Jetpack Compose**, and **Material 3**.

## Features

- Device detection (manufacturer, model, Android version, RAM, storage, display, battery, CPU)
- Full device scan with live progress (PASS / WARNING / FAILED / UNAVAILABLE / INCONCLUSIVE)
- Diagnostic modules: CPU/RAM, Battery, Storage, Display, Touch, Camera, Mic, Speaker, Vibration, Wi-Fi, Bluetooth, Mobile Network, Sensors, Thermal, USB
- Dashboard with health score, history, PDF export
- No root required · No fake results · No motherboard/IC claims from software alone

## Build

```bash
./gradlew assembleDebug
```

Requires JDK 17, Android SDK. Open in Android Studio for best experience.

## CI

`.github/workflows/android.yml` builds debug APK on push to main and uploads it as an artifact.

## License

MIT
