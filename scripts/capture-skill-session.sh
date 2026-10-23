#!/usr/bin/env bash
set -euo pipefail

skill_id="${1:-skill-session}"
adb_bin="${ADB_BIN:-$(command -v adb || true)}"

if [[ -z "${adb_bin}" && -x "/opt/homebrew/share/android-commandlinetools/platform-tools/adb" ]]; then
  adb_bin="/opt/homebrew/share/android-commandlinetools/platform-tools/adb"
fi

if [[ -z "${adb_bin}" ]]; then
  echo "adb not found. Set ADB_BIN to the Android platform-tools adb path." >&2
  exit 1
fi

device="${ANDROID_SERIAL:-$("${adb_bin}" devices | awk 'NR > 1 && $2 == "device" { print $1; exit }')}"
if [[ -z "${device}" ]]; then
  echo "No authorized Android device found." >&2
  exit 1
fi

session_root="${SKILL_CAPTURE_ROOT:-artifacts/skill-recordings}"
session_dir="${session_root}/$(date +%Y%m%d-%H%M%S)-${skill_id//[^A-Za-z0-9._-]/-}"
mkdir -p "${session_dir}/screens" "${session_dir}/ui"

cat > "${session_dir}/metadata.txt" <<META
skill_id=${skill_id}
device=${device}
started_at=$(date '+%Y-%m-%d %H:%M:%S %z')
capture_interval_ms=${CAPTURE_INTERVAL_MS:-800}
META

"${adb_bin}" -s "${device}" shell am broadcast \
  -a com.personaljarvis.DEBUG_SKILL_TRACE \
  -n com.personaljarvis/.debug.SkillTraceReceiver \
  --ez reset true >/dev/null 2>&1 || true

cleanup() {
  "${adb_bin}" -s "${device}" shell rm -f /sdcard/xiaozhu-skill-window.xml >/dev/null 2>&1 || true
  "${adb_bin}" -s "${device}" exec-out \
    run-as com.personaljarvis cat files/skill-trace/latest.jsonl \
    > "${session_dir}/accessibility.jsonl" 2>/dev/null || true
  echo
  echo "Recording saved to: ${session_dir}"
  echo "Frames: $(find "${session_dir}/screens" -type f | wc -l | tr -d ' ')"
}
trap cleanup EXIT

echo "Recording ${skill_id} on ${device}"
echo "Perform the workflow slowly from its starting screen."
echo "Pause briefly after each tap. Press Ctrl+C to finish."

counter=0
interval_seconds="$(
  awk -v ms="${CAPTURE_INTERVAL_MS:-800}" 'BEGIN { printf "%.3f", ms / 1000 }'
)"

while true; do
  frame="$(printf '%05d' "${counter}")"
  "${adb_bin}" -s "${device}" exec-out screencap -p > "${session_dir}/screens/${frame}.png"
  "${adb_bin}" -s "${device}" shell uiautomator dump /sdcard/xiaozhu-skill-window.xml >/dev/null
  "${adb_bin}" -s "${device}" exec-out cat /sdcard/xiaozhu-skill-window.xml > "${session_dir}/ui/${frame}.xml"
  "${adb_bin}" -s "${device}" shell am broadcast \
    -a com.personaljarvis.DEBUG_SKILL_TRACE \
    -n com.personaljarvis/.debug.SkillTraceReceiver >/dev/null 2>&1 || true
  counter=$((counter + 1))
  printf '\rCaptured %s frames' "${counter}"
  sleep "${interval_seconds}"
done
