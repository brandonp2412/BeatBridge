<div align="center">
  <img src="assets/logo.svg" alt="BeatBridge" width="520"/>

  <br/>

[![CI](https://github.com/brandonp2412/BeatBridge/actions/workflows/ci.yml/badge.svg)](https://github.com/brandonp2412/BeatBridge/actions/workflows/ci.yml)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blueviolet.svg)](LICENSE)
[![Min SDK](https://img.shields.io/badge/Android-8.0%2B-brightgreen.svg)](https://developer.android.com/about/versions/oreo)
[![F-Droid](https://img.shields.io/f-droid/v/com.beatbridge?color=blueviolet)](https://f-droid.org/packages/com.beatbridge)

</div>

## What it does

BeatBridge runs a lightweight background service that watches for your chosen Bluetooth device to connect. The instant it does — your car speakers, headphones, or any paired device — BeatBridge fires a **media play** event to resume whatever you were last listening to in Spotify, YouTube Music, Pocket Casts, or any other media app.

No logins. No accounts. No internet. Just music.

## Features

- **One-tap setup** — pick your device from the paired devices list and you're done
- **Works with any media app** — dispatches a standard Android media key event so any player responds
- **Persistent monitoring** — foreground service keeps watching even after you close the app
- **Battery-conscious** — no polling; wakes only on Bluetooth ACL connection events
- **Fully offline** — zero network permissions, zero tracking
- **Open source** — GPL-3.0, available on F-Droid

## Screenshots

<div align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/04_full_list.png" alt="BeatBridge main screen" width="270"/>
</div>

### Build from source

```bash
git clone https://github.com/brandonp2412/BeatBridge.git
cd BeatBridge
./gradlew assembleRelease
# APK → app/build/outputs/apk/release/app-release-unsigned.apk
```

Requirements: JDK 17, Android SDK with API 36.

## How it works

```
User selects device
        │
        ▼
BluetoothMonitorService starts (foreground)
        │
        └─ registers BroadcastReceiver for ACTION_ACL_CONNECTED
                    │
                    ▼
          Device connects? ──No──▶ keep waiting
                    │
                   Yes
                    │
                    ▼
          Address matches selected? ──No──▶ ignore
                    │
                   Yes
                    │
                    ▼
          AudioManager.dispatchMediaKeyEvent(MEDIA_PLAY)
                    │
                    ▼
          Music resumes in active media session ♪
```

## Build & test

```bash
# Unit tests
./gradlew test

# Instrumented tests (requires connected device or emulator)
./gradlew connectedAndroidTest

# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

## Contributing

Pull requests are welcome. Please:

1. Fork the repo and create a branch: `git checkout -b feature/my-change`
2. Make your changes and add/update tests as needed
3. Ensure `./gradlew test` passes
4. Open a PR with a clear description

## F-Droid

BeatBridge is designed to meet F-Droid inclusion requirements:

- GPL-3.0 licensed
- No proprietary dependencies (only AndroidX + Google Material)
- No network access, no tracking
- Reproducible builds from source via `./gradlew assembleRelease`
- Fastlane metadata in [`fastlane/metadata/android/`](fastlane/metadata/android/)

## License

```
BeatBridge — Auto-play music when your Bluetooth device connects
Copyright (C) 2026 BeatBridge Contributors

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.
```

Full text: [LICENSE](LICENSE)
