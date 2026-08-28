#!/usr/bin/env bash
set -euo pipefail

layout_file="$(dirname "$0")/../app/src/main/res/layout/item_device.xml"
grep -q 'app:tint="@color/bb_on_surface_dim"' "$layout_file"
! grep -q 'android:tint="@color/bb_on_surface_dim"' "$layout_file"
