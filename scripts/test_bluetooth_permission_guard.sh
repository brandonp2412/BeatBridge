#!/usr/bin/env bash
set -euo pipefail

source_file="$(dirname "$0")/../app/src/main/java/com/beatbridge/BluetoothMonitorService.kt"

grep -q 'Manifest.permission.BLUETOOTH_CONNECT' "$source_file"
grep -q 'checkSelfPermission' "$source_file"
