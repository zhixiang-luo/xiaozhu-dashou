#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "$0")" && pwd)"
sdk="${MACOS_SDK_PATH:-/Library/Developer/CommandLineTools/SDKs/MacOSX26.5.sdk}"
cache="${CLANG_MODULE_CACHE_PATH:-/private/tmp/xiaozhu-swift-cache}"

if [[ ! -d "${sdk}" ]]; then
  echo "macOS SDK not found: ${sdk}" >&2
  echo "Set MACOS_SDK_PATH to a Swift-compatible macOS SDK." >&2
  exit 1
fi

mkdir -p "${cache}"
CLANG_MODULE_CACHE_PATH="${cache}" xcrun swift \
  -sdk "${sdk}" \
  "${script_dir}/ocr-frame.swift" \
  "$@"
