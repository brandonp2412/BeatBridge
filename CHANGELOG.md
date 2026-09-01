# Changelog

All notable changes to BeatBridge are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [1.0.46] - 2026-09-01

### Changed
- fix: restore reproducible F-Droid releases
- ci: preserve secure Play metadata credentials
- Fix linting errors
- ci: sync Play metadata automatically
- ci: move release pipeline to main

## [1.0.44] - 2026-08-27

### Changed
- Harden release pipeline and Bluetooth monitor state
- Sync Google Play store metadata automatically (#10)
- Setup automated e2e and screenshot tests

## [Unreleased]

### Changed
- Allow multi-select and configure delay
- Add adi-registration.properties

## [1.0.26] - 2026-04-26

### Changed
- Add "Play on any bluetooth device" toggle
- Fix music app not launching

## [1.0.25] - 2026-04-04

### Changed
- Fix music app not launching

## [1.0.24] - 2026-04-04

### Changed
- Fix pixelated looking notification icon
- Improve list performance
- Upgrade kotlin to 9.0.1
- Remove redundant qualifiers
- Suppress warnings for query all apps & foreground service
- Clean up BluetoothMonitorService.kt

## [1.0.23] - 2026-04-02

### Changed
- Update screenshots

## [1.0.22] - 2026-04-01

### Changed
- Add FUNDING.yml

## [1.0.21] - 2026-04-01

### Changed
- Fix f-droid issue

## [1.0.20] - 2026-04-01

### Changed
- Automatically start beatbridge on reboot
- Change persistent notification
- Allow selecting music app
- Sign release APKs with keystore from GitHub secrets
- Trigger tag and release automatically

## [1.0.0] - 2024-01-01

### Added
- Select any paired Bluetooth device as the auto-play trigger
- Foreground service monitors for Bluetooth ACL connection events
- Automatically dispatches `MEDIA_PLAY` key event to the active media session on connect
- Persistent selected-device preference across app restarts
- Status label shows the currently watched device name
- Empty-state message when no paired devices are found
- Supports Android 8.0 (API 26) through Android 15 (API 36)
- Handles runtime Bluetooth permissions for Android 12+ and notification permission for Android 13+
