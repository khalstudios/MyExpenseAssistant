#!/usr/bin/env bash
#
# Flips the Pro entitlement on the attached device, for testing the paywall.
#
#   scripts/pro-mode.sh locked    lock everything - the gates and the paywall
#   scripts/pro-mode.sh pro       unlock everything, as a grandfathered user
#   scripts/pro-mode.sh notice    locked, and replay the "Pro is yours, free" dialog
#   scripts/pro-mode.sh status    what the device currently believes
#
# Debug builds only: this works through run-as, which release builds refuse.
#
# Deleting the preferences file instead of editing it does NOT lock the app.
# EntitlementStore recomputes "grandfathered" from the install date whenever the
# key is missing, so a device installed before the cutoff lands back on true.

set -euo pipefail
cd "$(dirname "$0")/.."

# Git Bash rewrites an argument that looks like a Unix path, so an adb push to
# /data/local/tmp would otherwise arrive as C:/Program Files/Git/data/local/tmp.
export MSYS_NO_PATHCONV=1

if [ -n "${ADB:-}" ]; then
    adb_bin="$ADB"
elif command -v adb >/dev/null 2>&1; then
    adb_bin=adb
else
    adb_bin="$HOME/AppData/Local/Android/Sdk/platform-tools/adb.exe"
fi

if [ ! -x "$adb_bin" ] && ! command -v "$adb_bin" >/dev/null 2>&1; then
    echo "ERROR: adb not found. Put it on PATH or set ADB=/path/to/adb.exe" >&2
    exit 1
fi

PKG=com.khaltech.expenseassistant
PREFS=shared_prefs/pro-entitlement.xml
MODE="${1:-status}"

run_as() { "$adb_bin" shell "run-as $PKG sh -c '$1'"; }

if [ "$MODE" = "status" ]; then
    run_as "cat $PREFS" 2>/dev/null | grep -E 'grandfathered|purchased|notice' \
        || echo "no entitlement stored yet"
    exit 0
fi

case "$MODE" in
    locked) grandfathered=false; seen=true  ;;
    notice) grandfathered=false; seen=false ;;
    pro)    grandfathered=true;  seen=true  ;;
    *)
        echo "ERROR: unknown mode '$MODE' (expected locked, pro, notice or status)" >&2
        exit 1
        ;;
esac

# Relative on purpose. MSYS_NO_PATHCONV stops Git Bash rewriting the remote
# /data/local/tmp argument, but it also stops it turning an absolute local path
# like /tmp/xyz into one adb.exe can open. A relative path sidesteps both.
tmp=.pro-mode.tmp.xml
trap 'rm -f "$tmp"' EXIT
cat > "$tmp" <<XML
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <boolean name="grandfather_notice_seen" value="$seen" />
    <boolean name="grandfathered" value="$grandfathered" />
    <boolean name="purchased" value="false" />
</map>
XML

# Force-stop first: SharedPreferences are cached in memory, and a running
# process writes its own stale copy back over this one on the way out.
"$adb_bin" shell am force-stop $PKG
"$adb_bin" push "$tmp" /data/local/tmp/pe.xml >/dev/null 2>&1
run_as "cat /data/local/tmp/pe.xml > $PREFS"
"$adb_bin" shell rm /data/local/tmp/pe.xml

"$adb_bin" shell am start -n $PKG/.ui.MainActivity >/dev/null
echo "==> $MODE mode set, app relaunched"
run_as "cat $PREFS" | grep -E 'grandfathered|purchased'
