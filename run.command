#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

if [ -f "$SCRIPT_DIR/desktop/pom.xml" ]; then
    APP_DIR="$SCRIPT_DIR/desktop"
elif [ -f "$SCRIPT_DIR/pom.xml" ]; then
    APP_DIR="$SCRIPT_DIR"
else
    echo "Error: Direktori project 'desktop/pom.xml' tidak ditemukan."
    read -r -p "Tekan Enter untuk menutup..."
    exit 1
fi

cd "$APP_DIR" || exit 1

# Pastikan PATH mencakup Homebrew & lokasi umum macOS saat dibuka lewat Finder
export PATH="/opt/homebrew/bin:/usr/local/bin:/opt/homebrew/opt/openjdk@21/bin:/opt/homebrew/opt/openjdk@17/bin:/usr/local/opt/openjdk@21/bin:/usr/local/opt/openjdk@17/bin:$PATH"

if [ -d "$HOME/.sdkman/candidates/maven/current/bin" ]; then
    export PATH="$HOME/.sdkman/candidates/maven/current/bin:$PATH"
fi

if [ -z "${JAVA_HOME:-}" ] && [ -x /usr/libexec/java_home ]; then
    DETECTED_JAVA_HOME="$(/usr/libexec/java_home -v 17+ 2>/dev/null || true)"
    if [ -n "$DETECTED_JAVA_HOME" ]; then
        export JAVA_HOME="$DETECTED_JAVA_HOME"
        export PATH="$JAVA_HOME/bin:$PATH"
    fi
fi

pause_on_error() {
    echo ""
    read -r -p "Tekan Enter untuk menutup..."
}

if ! command -v java &> /dev/null; then
    echo "Error: Java tidak ditemukan. Pastikan JDK 17+ sudah terinstal."
    pause_on_error
    exit 1
fi

JAVA_VER="$(java -version 2>&1 | awk -F'"' '/version/ {print $2}' | awk -F'.' '{print ($1 == "1" ? $2 : $1)}')"
if [ -z "$JAVA_VER" ] || ! [[ "$JAVA_VER" =~ ^[0-9]+$ ]] || [ "$JAVA_VER" -lt 17 ]; then
    echo "Error: Membutuhkan JDK 17 atau lebih baru (terdeteksi: ${JAVA_VER:-tidak diketahui})."
    pause_on_error
    exit 1
fi

if ! command -v mvn &> /dev/null; then
    echo "Error: Maven (mvn) tidak ditemukan. Instal via Homebrew: brew install maven"
    pause_on_error
    exit 1
fi

DB_HOST="127.0.0.1"
DB_PORT="3306"
if [ -f "src/main/resources/database.properties" ]; then
    PROP_HOST="$(grep -E '^db\.host=' src/main/resources/database.properties | head -n 1 | cut -d'=' -f2- | tr -d '[:space:]')"
    PROP_PORT="$(grep -E '^db\.port=' src/main/resources/database.properties | head -n 1 | cut -d'=' -f2- | tr -d '[:space:]')"
    [ -n "$PROP_HOST" ] && DB_HOST="$PROP_HOST"
    [ -n "$PROP_PORT" ] && DB_PORT="$PROP_PORT"
fi

echo "========================================"
echo "  Tiketin Desktop — JavaFX 21 Launcher"
echo "========================================"
echo "Direktori : $APP_DIR"
echo "Java      : JDK $JAVA_VER"

if command -v nc &> /dev/null; then
    if nc -z -w 1 "$DB_HOST" "$DB_PORT" &> /dev/null; then
        echo "MySQL     : Aktif ($DB_HOST:$DB_PORT)"
    else
        echo "Peringatan: MySQL di $DB_HOST:$DB_PORT belum merespons."
        echo "            Pastikan MySQL / XAMPP sudah aktif agar data termuat."
    fi
fi

echo ""
echo "Menjalankan Tiketin Desktop..."
mvn javafx:run "$@"
EXIT_CODE=$?

if [ $EXIT_CODE -ne 0 ]; then
    echo ""
    echo "Aplikasi berhenti dengan error (kode: $EXIT_CODE)."
    pause_on_error
fi

exit $EXIT_CODE
