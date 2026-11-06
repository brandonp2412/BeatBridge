#!/usr/bin/env bash
set -euo pipefail

# Compare unsigned release APKs from two independent clean copies of HEAD.
# Warm the dependency cache with a normal verified build first. This checks
# repeatability of this source/build environment, not F-Droid binary matching.
root="$(git rev-parse --show-toplevel)"
if [[ -n "$(git -C "$root" status --porcelain)" ]]; then
    echo "Refusing to compare a dirty source tree; commit or stash first." >&2
    exit 1
fi

work_dir="$(mktemp -d -t beatbridge-repro-XXXXXX)"
trap 'rm -rf -- "$work_dir"' EXIT

for copy in first second; do
    source_dir="$work_dir/$copy"
    mkdir -p "$source_dir"
    git -C "$root" archive --format=tar HEAD | tar -xf - -C "$source_dir"
    (
        cd "$source_dir"
        env -u KEYSTORE_PASSWORD -u KEYSTORE_FILE -u KEY_ALIAS -u KEY_PASSWORD \
            ./gradlew --no-daemon --offline :app:assembleRelease
    )
done

for abi in x86_64 armeabi-v7a arm64-v8a x86; do
    name="app-$abi-release-unsigned.apk"
    first="$work_dir/first/app/build/outputs/apk/release/$name"
    second="$work_dir/second/app/build/outputs/apk/release/$name"
    test -s "$first"
    test -s "$second"
    if ! cmp -s "$first" "$second"; then
        echo "FAIL: $name differs between clean builds" >&2
        sha256sum "$first" "$second" >&2
        exit 1
    fi
    sha256sum "$first"
done
echo "PASS: all four unsigned release APKs match byte for byte."
