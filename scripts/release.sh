#!/usr/bin/env bash
#
# Builds the signed release artifacts and copies them to the repo root.
#
#   scripts/release.sh
#
# Bump versionCode in app/build.gradle.kts BEFORE running this if the build is
# going to Play - the Console rejects an upload whose versionCode it has seen.

set -euo pipefail
cd "$(dirname "$0")/.."

if [ ! -f keystore.properties ]; then
    echo "ERROR: keystore.properties not found." >&2
    echo "Release builds would be silently unsigned. Restore it and app-upload.jks first." >&2
    exit 1
fi

echo "==> Building release artifacts"
./gradlew.bat bundleRelease assembleRelease

echo "==> Copying to repo root"
cp app/build/outputs/bundle/release/app-release.aab KahanGayaPaisa-release.aab
cp app/build/outputs/apk/release/app-release.apk KahanGayaPaisa-release.apk

echo
grep -E "versionCode|versionName" app/build.gradle.kts | sed 's/^ */  /'
echo
echo "  Upload to Play:      KahanGayaPaisa-release.aab"
echo "  Share with testers:  KahanGayaPaisa-release.apk"
echo
# Not reproducible build-to-build - use it to check a transfer, not to identify a build.
echo "==> SHA-256 of this build"
sha256sum KahanGayaPaisa-release.apk
