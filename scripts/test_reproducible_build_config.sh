#!/usr/bin/env bash
set -euo pipefail

build_file="$(dirname "$0")/../app/build.gradle.kts"

grep -qE '^tasks\.matching \{' "$build_file"
grep -qE '^    \.configureEach \{ enabled = false \}$' "$build_file"
