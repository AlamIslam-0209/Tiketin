@echo off
chcp 65001 >nul
cd /d "%~dp0"

where java >nul 2>&1
if %errorlevel% neq 0 (
    echo Error: Java tidak ditemukan. Pastikan JDK 17+ terinstal dan ada di PATH.
    pause
    exit /b 1
)

where mvn >nul 2>&1
if %errorlevel% neq 0 (
    echo Error: Maven tidak ditemukan. Pastikan Maven terinstal dan ada di PATH.
    pause
    exit /b 1
)

mvn javafx:run %*
