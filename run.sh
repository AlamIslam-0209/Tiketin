#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"

JAR_PATH="target/tiketin-desktop-2.0.0-jar-with-dependencies.jar"
if [ -f "$JAR_PATH" ]; then
    echo "Menjalankan Tiketin Desktop..."
    exec java -jar "$JAR_PATH"
fi

MVN_BIN="mvn"
if ! command -v mvn >/dev/null 2>&1; then
    if [ -x "/usr/local/bin/mvn" ]; then
        MVN_BIN="/usr/local/bin/mvn"
    elif [ -x "/opt/homebrew/bin/mvn" ]; then
        MVN_BIN="/opt/homebrew/bin/mvn"
    elif [ -x "/usr/local/var/homebrew/tmp/.cellar/maven/3.10.0/libexec/bin/mvn" ]; then
        MVN_BIN="/usr/local/var/homebrew/tmp/.cellar/maven/3.10.0/libexec/bin/mvn"
    fi
fi

echo "Menjalankan Tiketin Desktop via Maven..."
"$MVN_BIN" compile exec:java
