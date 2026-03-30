# Changelog

All notable changes to BeatBridge are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

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
