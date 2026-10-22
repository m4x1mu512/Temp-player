#!/usr/bin/env sh
# Lightweight Gradle bootstrap for repositories where the binary wrapper JAR
# is not present. Uses the distribution pinned in gradle/wrapper/gradle-wrapper.properties.
set -eu

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROPERTIES="$APP_HOME/gradle/wrapper/gradle-wrapper.properties"

if [ ! -f "$PROPERTIES" ]; then
  echo "Gradle wrapper properties not found: $PROPERTIES" >&2
  exit 1
fi

DISTRIBUTION_URL=$(sed -n 's/^distributionUrl=//p' "$PROPERTIES" | sed 's/\\:/\:/g' | tail -n 1)
if [ -z "$DISTRIBUTION_URL" ]; then
  echo "distributionUrl is missing from $PROPERTIES" >&2
  exit 1
fi

ARCHIVE_NAME=${DISTRIBUTION_URL##*/}
DIST_NAME=${ARCHIVE_NAME%.zip}
GRADLE_VERSION=${DIST_NAME#gradle-}
GRADLE_VERSION=${GRADLE_VERSION%-bin}
GRADLE_USER_HOME=${GRADLE_USER_HOME:-"$HOME/.gradle"}
DIST_ROOT="$GRADLE_USER_HOME/wrapper/dists/$DIST_NAME-bootstrap"
GRADLE_HOME="$DIST_ROOT/$DIST_NAME"

if [ ! -x "$GRADLE_HOME/bin/gradle" ]; then
  mkdir -p "$DIST_ROOT"
  TMP_DIR="$DIST_ROOT/.download-$$"
  mkdir -p "$TMP_DIR"
  trap 'rm -rf "$TMP_DIR"' EXIT HUP INT TERM

  echo "Downloading Gradle $GRADLE_VERSION..."
  if command -v curl >/dev/null 2>&1; then
    curl --fail --location --retry 3 --connect-timeout 20 "$DISTRIBUTION_URL" -o "$TMP_DIR/gradle.zip"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$TMP_DIR/gradle.zip" "$DISTRIBUTION_URL"
  else
    echo "Neither curl nor wget is available to download Gradle." >&2
    exit 1
  fi

  unzip -q "$TMP_DIR/gradle.zip" -d "$TMP_DIR"
  if [ ! -x "$TMP_DIR/$DIST_NAME/bin/gradle" ]; then
    echo "Gradle distribution archive did not contain $DIST_NAME/bin/gradle." >&2
    exit 1
  fi
  rm -rf "$GRADLE_HOME"
  mv "$TMP_DIR/$DIST_NAME" "$GRADLE_HOME"
  rm -rf "$TMP_DIR"
  trap - EXIT HUP INT TERM
fi

exec "$GRADLE_HOME/bin/gradle" "$@"
