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

echo "==> Building play + direct release artifacts"
./gradlew.bat bundlePlayRelease assemblePlayRelease assembleDirectRelease

echo "==> Copying to repo root"
cp app/build/outputs/bundle/playRelease/app-play-release.aab MyExpenseAssistant-play-release.aab
cp app/build/outputs/apk/play/release/app-play-release.apk MyExpenseAssistant-play-test.apk
cp app/build/outputs/apk/direct/release/app-direct-release.apk MyExpenseAssistant-direct-release.apk

echo
grep -E "versionCode|versionName" app/build.gradle.kts | sed 's/^ */  /'
echo
echo "  Upload to Play:      MyExpenseAssistant-play-release.aab"
echo "  Share with testers:  MyExpenseAssistant-play-test.apk"
echo "  Full-capability:     MyExpenseAssistant-direct-release.apk"
echo
# Not reproducible build-to-build - use it to check a transfer, not to identify a build.
echo "==> SHA-256 of this build"
sha256sum MyExpenseAssistant-play-test.apk
