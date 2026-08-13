#!/usr/bin/env bash
#
# Checks the companion APKs in src/official/assets before they are packaged.
# They are not built here, so ask apksigner what each one is and whether it
# carries the certificate we expect.
#
# Certificates rather than file hashes. A version bump changes every byte but
# keeps the key, so the pin survives an update and still catches a binary signed
# by anyone else. Each digest below is the one on the publisher's own release.
#
# tor-browser.apk is pinned by SHA-256 where it is fetched, so only its
# signature is checked here.

set -euo pipefail

POSTBOX_CERT="0c42c2a2e9be889eef2e7da10e8cd9cc71e6e6bc0e676fc994a8066d1766cb8d"
MONERUJO_CERT="967ca1930c19f383a43729a059cd21727855f78cd5e0d9882f780189fe1d8cf1"
ORBOT_CERT="a454b87a1847a89ed7f5e70fba6bba96f3ef29c26e0981204fe347bf231dfd5b"

assets="anonomi-android/src/official/assets"

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [ -z "$SDK" ]; then
	echo "::error::neither ANDROID_HOME nor ANDROID_SDK_ROOT is set"
	exit 1
fi
APKSIGNER="$(ls -d "$SDK"/build-tools/*/apksigner | sort -V | tail -1)"
echo "using $APKSIGNER"

fail=0
report="$(mktemp)"

# The hotspot hides the button for anything missing, so an absent APK would not
# show up at runtime.
for required in anonomi-postbox.apk monerujo.apk orbot.apk tor-browser.apk; do
	if [ ! -f "$assets/$required" ]; then
		echo "::error::missing $assets/$required"
		fail=1
	fi
done

shopt -s nullglob
for apk in "$assets"/*.apk; do
	name="$(basename "$apk")"
	if ! "$APKSIGNER" verify --print-certs "$apk" > "$report" 2>&1; then
		echo "::error::unsigned, corrupt or not an APK: $name ($(wc -c < "$apk") bytes)"
		cat "$report"
		fail=1
		continue
	fi
	sha="$(grep -m1 -i 'certificate SHA-256 digest' "$report" | awk '{print $NF}')"
	case "$name" in
		anonomi-postbox.apk) expected="$POSTBOX_CERT" ;;
		monerujo.apk)        expected="$MONERUJO_CERT" ;;
		orbot.apk)           expected="$ORBOT_CERT" ;;
		tor-browser.apk)     expected="" ;;
		*)
			expected=""
			echo "::warning::$name is bundled but not pinned, and the hotspot will not offer it" ;;
	esac
	if [ -n "$expected" ] && [ "$sha" != "$expected" ]; then
		echo "::error::$name is signed by $sha, expected $expected"
		fail=1
	else
		echo "signed OK: $name ($sha)"
	fi
done

if [ "$fail" -ne 0 ]; then
	echo "Bundled companion APKs failed verification."
	exit 1
fi
