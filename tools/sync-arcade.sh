#!/bin/sh
# Copies the Matix Arcade from the website into the app, so games added on the website
# (Gha15/Gha15.github.io: assets/arcade.js, assets/arcade.css, arcade/img/) are built into the app.
#
#   tools/sync-arcade.sh [path to a website checkout]
#
# Without a path it uses ../Gha15.github.io, or clones the website if that folder is missing.
# The CI build runs this before every build, and the app also loads a newer arcade straight
# from the website at runtime (js/matix-arcade-sync.js), so new games reach installed apps
# without waiting for a Play update. Scores sync through the club database (/arcade/best, /arcade/top).
set -e
cd "$(dirname "$0")/.."
SITE="${1:-../Gha15.github.io}"
if [ ! -f "$SITE/assets/arcade.js" ]; then
  SITE="$(mktemp -d)/site"
  git clone --depth 1 https://github.com/Gha15/Gha15.github.io.git "$SITE" >/dev/null 2>&1
fi
A=app/src/main/assets
mkdir -p "$A/arcade/img" "$A/js" "$A/css"
cp "$SITE/assets/arcade.js" "$A/js/matix-games.js"
# the website uses absolute /arcade/img/ paths; inside the app the css lives in css/, so go one folder up
sed 's#url(/arcade/img/#url(../arcade/img/#g' "$SITE/assets/arcade.css" > "$A/css/matix-games.css"
rsync -a --delete "$SITE/arcade/img/" "$A/arcade/img/"
cp "$SITE/arcade/version.json" "$A/arcade/version.json"
V=$(sed -n 's/.*"version": *\([0-9]*\).*/\1/p' "$A/arcade/version.json")
echo "Arcade synced from $SITE (version $V, $(ls "$A/arcade/img" | wc -l | tr -d ' ') images)."
