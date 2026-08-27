#!/usr/bin/env bash
set -euo pipefail

./gradlew assembleDebug assembleDebugAndroidTest --stacktrace

DEVICE_ABI="$(adb shell getprop ro.product.cpu.abi | tr -d '\r')"
APP_APK="app/build/outputs/apk/debug/app-${DEVICE_ABI}-debug.apk"
TEST_APK="app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"

test -s "$APP_APK"
test -s "$TEST_APK"

adb install -t -r "$APP_APK"
adb install -t -r "$TEST_APK"
adb shell pm grant com.beatbridge android.permission.BLUETOOTH_CONNECT
adb shell pm grant com.beatbridge android.permission.POST_NOTIFICATIONS

mkdir -p artifacts/e2e artifacts/screenshots
adb shell am instrument -w \
  com.beatbridge.test/androidx.test.runner.AndroidJUnitRunner \
  | tee artifacts/e2e/instrumentation.log

adb pull /sdcard/Android/data/com.beatbridge/files/screenshots/. artifacts/screenshots/

for screenshot in \
  01_empty_state.png \
  02_devices_found.png \
  03_device_selected.png \
  04_full_list.png; do
  test -s "artifacts/screenshots/$screenshot"
done
