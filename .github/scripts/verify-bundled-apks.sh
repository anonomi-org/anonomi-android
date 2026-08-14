#!/usr/bin/env bash
#
# Checks the companion APKs in src/official/assets before they are packaged.
# They are not built here, so ask apksigner what each one is and whether it
# carries the certificate we expect.
#
# Certificates rather than file hashes. Every one of these is already pinned by
# SHA-256 in fetch-companion-apks.sh, which fixes the bytes; the certificate is
# the check that survives the next version bump, when the hash has to change but
# the key must not. Each digest below is the one on the publisher's own release.
#
# tor-browser.apk publishes no certificate we pin, so for it the SHA-256 at
# fetch time is the whole guarantee and only signature validity is checked here.

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
AAPT="$(ls -d "$SDK"/build-tools/*/aapt2 | sort -V | tail -1)"
echo "using $APKSIGNER"
echo "using $AAPT"

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

# A release states the Postbox version twice: release.yml builds the standalone
# APK from the anonomi-postbox submodule, while the official build packages the
# APK that fetch-companion-apks.sh pins. Bump one and not the other and the
# release page offers a Postbox the hotspot in that same build will not hand
# out. Compare the two rather than trust them to be edited together.
postbox_gradle="anonomi-postbox/postbox-android/build.gradle"
if [ ! -f "$postbox_gradle" ]; then
	# release.yml and check-bundled-apks.yml both check the submodule out, so in
	# CI its absence is a broken workflow, not a maintainer running this bare.
	if [ -n "${CI:-}" ]; then
		echo "::error::$postbox_gradle not found - check the submodule out"
		fail=1
	else
		echo "warning: $postbox_gradle not found, skipping the version check"
	fi
elif [ -f "$assets/anonomi-postbox.apk" ]; then
	sub_ver="$(sed -n 's/.*versionName[[:space:]]*"\([^"]*\)".*/\1/p' "$postbox_gradle" | head -1)"
	apk_ver="$("$AAPT" dump badging "$assets/anonomi-postbox.apk" 2>/dev/null |
		sed -n "s/.*versionName='\([^']*\)'.*/\1/p" | head -1)"
	if [ -z "$sub_ver" ] || [ -z "$apk_ver" ]; then
		echo "::error::could not read the Postbox version (submodule '$sub_ver', APK '$apk_ver')"
		fail=1
	elif [ "$sub_ver" != "$apk_ver" ]; then
		echo "::error::bundled Postbox is $apk_ver but the submodule builds $sub_ver"
		echo "::error::bump POSTBOX_VERSION in fetch-companion-apks.sh and the submodule together"
		fail=1
	else
		echo "Postbox version matches the submodule: $apk_ver"
	fi
fi

if [ "$fail" -ne 0 ]; then
	echo "Bundled companion APKs failed verification."
	exit 1
fi
