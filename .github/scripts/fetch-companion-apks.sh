#!/usr/bin/env bash
#
# Downloads the companion apps the official flavour hands out over the hotspot
# into src/official/assets, checking each against a pinned SHA-256.
#
# They are fetched rather than committed so the tree stays free of large
# binaries and a version change is an edit to two lines here. Run it by hand to
# populate the assets for a local official build; release.yml runs it in CI.
#
# When you change a version, get the new SHA-256 from the publisher and, where
# one is published, check the signature before you write the hash down. The hash
# covers every build after that.

set -euo pipefail

# Anonomi Postbox. Built from the anonomi-postbox submodule by that project's
# own release workflow and signed with our release key, so the certificate is
# pinned in verify-bundled-apks.sh as well. Keep POSTBOX_VERSION in step with
# the submodule: release.yml builds the standalone Postbox from the submodule
# and bundles this APK inside the official build, and verify-bundled-apks.sh
# fails if the two disagree.
POSTBOX_VERSION="1.0.6"
POSTBOX_SHA256="3c29c0bee86c33fee92d2c7e0a8e07a6190a46e38d3b6f2fe6dc8b1748751f25"

# Tor Browser. Read from the archive host rather than dist, which serves only
# the current release, so a pinned version there stops resolving as soon as the
# next one ships. Signature checked against the Tor Browser Developers key
# EF6E286DDA85EA2A4BA7DE684E2C6E8793298290.
TB_VERSION="15.0.19"
TB_ABI="aarch64"
TB_SHA256="4982ff6e9eb1075f6035acae84aafcd70e4b9a70e72499af2476e6c1852516d0"

# Orbot. arm64-v8a, matching what has shipped; the universal build covers every
# architecture at 82 MB against 34 MB. Signature checked against
# BBE20FD6DA48A3DD4CC7DF41A801183E69B37AA9.
ORBOT_VERSION="17.9.5-RC-4-tor-0.4.9.11"
ORBOT_ABI="arm64-v8a"
ORBOT_SHA256="9cfacb21eaf5ad9e397f80d4f48b9d913ef585c5c6cd96ac254d008052305cbb"

# Monerujo. One universal build, so no architecture to choose. The tag and the
# file name spell the version differently, hence the substitution below. No
# signature is published alongside it.
MONERUJO_VERSION="4.1.7"
MONERUJO_SHA256="0b31ad7f0b1f677daa6dfeff95730241222467c33c4465469c7a0d820beb260a"

dest="anonomi-android/src/official/assets"

# GNU coreutils on the runner, BSD tools on a maintainer's Mac.
check_sha256() {
	if command -v sha256sum > /dev/null 2>&1; then
		sha256sum -c -
	else
		shasum -a 256 -c -
	fi
}

fetch() {
	local url="$1" name="$2" sha="$3" tmp
	tmp="$(mktemp)"
	echo "fetching $name"
	curl -fsSL --retry 3 --retry-delay 5 -o "$tmp" "$url"
	echo "${sha}  ${tmp}" | check_sha256
	mv "$tmp" "${dest}/${name}"
	chmod 644 "${dest}/${name}"
}

# Every APK here is fetched and ignored, so nothing in this directory is
# tracked and a fresh checkout does not have it. Check for something that is
# tracked to catch being run from the wrong place, then create it.
if [ ! -f "settings.gradle" ]; then
	echo "::error::settings.gradle not found - run this from the repository root"
	exit 1
fi
mkdir -p "$dest"

fetch "https://github.com/anonomi-org/anonomi-postbox/releases/download/v${POSTBOX_VERSION}/anonomi-postbox-release.apk" \
	"anonomi-postbox.apk" "$POSTBOX_SHA256"

fetch "https://archive.torproject.org/tor-package-archive/torbrowser/${TB_VERSION}/tor-browser-android-${TB_ABI}-${TB_VERSION}.apk" \
	"tor-browser.apk" "$TB_SHA256"

fetch "https://github.com/guardianproject/orbot/releases/download/${ORBOT_VERSION}/Orbot-${ORBOT_VERSION}-fullperm-${ORBOT_ABI}-release.apk" \
	"orbot.apk" "$ORBOT_SHA256"

monerujo_named="$(echo "$MONERUJO_VERSION" | tr . x)"
fetch "https://github.com/m2049r/xmrwallet/releases/download/v${MONERUJO_VERSION}/monerujo-${monerujo_named}_universal.apk" \
	"monerujo.apk" "$MONERUJO_SHA256"

ls -l "$dest"
