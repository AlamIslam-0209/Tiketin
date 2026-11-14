#!/bin/bash
cd "$(dirname "$0")"

if ! command -v java &> /dev/null; then
    echo "Error: Java tidak ditemukan. Pastikan JDK 17+ terinstal."
    exit 1
fi

if ! command -v mvn &> /dev/null; then
    echo "Error: Maven tidak ditemukan. Pastikan Maven terinstal."
    exit 1
fi

mvn javafx:run "$@"
