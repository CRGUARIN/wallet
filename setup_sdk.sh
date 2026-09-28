#!/usr/bin/env bash
# Instala el Android SDK necesario para compilar Wallet. Idempotente.
# Uso: source ./setup_sdk.sh   (con "source" las variables quedan en la sesión actual)
set -euo pipefail

export ANDROID_HOME="${ANDROID_HOME:-$HOME/android-sdk}"
TOOLS_ZIP="commandlinetools-linux-16111833_latest.zip"

if [ ! -x "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" ]; then
    mkdir -p "$ANDROID_HOME/cmdline-tools"
    tmp="$(mktemp -d)"
    curl -sSLo "$tmp/tools.zip" "https://dl.google.com/android/repository/$TOOLS_ZIP"
    unzip -q "$tmp/tools.zip" -d "$tmp"
    rm -rf "$ANDROID_HOME/cmdline-tools/latest"
    mv "$tmp/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
    rm -rf "$tmp"
fi

export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
yes | sdkmanager --licenses > /dev/null || true
sdkmanager "platform-tools" "platforms;android-37.0" "build-tools;37.0.0"

# Gradle lee la ruta del SDK desde local.properties (ignorado por git).
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
echo "sdk.dir=$ANDROID_HOME" > "$SCRIPT_DIR/local.properties"
