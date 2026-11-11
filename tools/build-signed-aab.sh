#!/bin/zsh
# Builds a signed release AAB with the upload key and copies it to ~/Documents/app-release.aab.
# Passwords are typed here (hidden) and only passed to Gradle through environment variables.
# usage: tools/build-signed-aab.sh [keystore path]
set -e
cd "$(dirname "$0")/.."
KS="${1:-$HOME/Documents/matixthemathclub}"
JBR="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
[ -d "$JBR" ] && export JAVA_HOME="$JBR"
[ -f "$KS" ] || { echo "Keystore not found: $KS"; exit 1; }

printf "Keystore password: "; read -rs SP; echo
ALIASES=$("$JAVA_HOME/bin/keytool" -list -keystore "$KS" -storepass "$SP" 2>/dev/null | awk -F, '/PrivateKeyEntry/{print $1}')
[ -n "$ALIASES" ] || { echo "Wrong keystore password (or no key inside)."; exit 1; }
ALIAS=$(echo "$ALIASES" | head -1)
echo "Key alias: $ALIAS"
printf "Key password (just press Enter if it is the same): "; read -rs KP; echo
[ -z "$KP" ] && KP="$SP"

KEYSTORE_PATH="$KS" KEYSTORE_PASSWORD="$SP" KEY_ALIAS="$ALIAS" KEY_PASSWORD="$KP" ./gradlew --no-daemon :app:bundleRelease
OUT=app/build/outputs/bundle/release/app-release.aab
"$JAVA_HOME/bin/jarsigner" -verify "$OUT" >/dev/null && echo "Signature OK"
cp "$OUT" "$HOME/Documents/app-release.aab"
ls -lh "$HOME/Documents/app-release.aab"
echo "Done: ~/Documents/app-release.aab"
