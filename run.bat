@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

set "JAR_PATH=target\tiketin-desktop-2.0.0-jar-with-dependencies.jar"

where java >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Java tidak ditemukan di PATH. Pastikan JDK/JRE 17 atau lebih baru sudah terpasang.
    pause
    exit /b 1
)

if exist "%JAR_PATH%" (
    echo Menjalankan Tiketin Desktop...
    java -jar "%JAR_PATH%"
    if errorlevel 1 (
        echo [ERROR] Aplikasi berhenti dengan kode error !errorlevel!.
        pause
        exit /b !errorlevel!
    )
    exit /b 0
)

where mvn >nul 2>nul
if errorlevel 1 (
    echo [ERROR] File JAR belum ditemukan dan Maven ^(mvn^) tidak tersedia di PATH.
    echo Jalankan "mvn package" terlebih dahulu atau pastikan Maven terpasang.
    pause
    exit /b 1
)

echo Menjalankan Tiketin Desktop via Maven...
call mvn compile exec:java
if errorlevel 1 (
    echo [ERROR] Gagal menjalankan aplikasi via Maven.
    pause
    exit /b !errorlevel!
)
