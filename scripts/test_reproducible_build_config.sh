#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
build_file="$root/app/build.gradle.kts"

# Static guards only. Actual reproducibility requires two clean APK builds
# and binary comparison (scripts/verify_reproducible_builds.sh).
grep -Fq 'lockAllConfigurations()' "$build_file"
grep -Fq 'includeInApk = false' "$build_file"
grep -Fq 'includeInBundle = false' "$build_file"
grep -Fq 'include("arm64-v8a", "armeabi-v7a", "x86_64", "x86")' "$build_file"
grep -Fq 'distributionSha256Sum=' "$root/gradle/wrapper/gradle-wrapper.properties"
test -s "$root/gradle/verification-metadata.xml"
test -s "$root/app/gradle.lockfile"
