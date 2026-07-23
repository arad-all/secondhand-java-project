@echo off
REM ===========================================================================
REM SecondHand - Sample Data Setup Script (Windows CMD)
REM ===========================================================================
REM Run this from the project root AFTER creating the database with Hibernate's
REM ddl-auto=update (i.e., after starting the backend at least once).
REM
REM Usage:
REM   docs\setup-sample-data.bat
REM
REM Note: This script requires sqlite3.exe to be in your PATH. If you don't
REM have it, download from https://sqlite.org/download.html and place
REM sqlite3.exe somewhere in your PATH, or update the SQLITE variable below.
REM ===========================================================================

setlocal enabledelayedexpansion

set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%SCRIPT_DIR%.."

where sqlite3 >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo Error: sqlite3 is not installed or not in PATH.
    echo Download it from https://sqlite.org/download.html and try again.
    pause
    exit /b 1
)

if not exist "%PROJECT_ROOT%\secondhand.db" (
    echo Error: secondhand.db not found.
    echo Start the backend at least once so Hibernate creates the database,
    echo then run this script.
    pause
    exit /b 1
)

echo ==^> 1. Populating database with sample data...
sqlite3 "%PROJECT_ROOT%\secondhand.db" < "%SCRIPT_DIR%sample-data.sql"
echo     Done.

echo ==^> 2. Copying sample photos to uploads/advertisements/...
if not exist "%PROJECT_ROOT%\uploads\advertisements" mkdir "%PROJECT_ROOT%\uploads\advertisements"

for /d %%d in ("%SCRIPT_DIR%sample-photos\*") do (
    set "ad_id=%%~nxd"
    if not exist "%PROJECT_ROOT%\uploads\advertisements\!ad_id!" mkdir "%PROJECT_ROOT%\uploads\advertisements\!ad_id!"
    copy "%%d\*.*" "%PROJECT_ROOT%\uploads\advertisements\!ad_id!\" >nul
)
echo     Done.

echo.
echo Sample data is ready! Start the backend and frontend to use the app.
echo Test accounts:  admin / admin_pass  ^|  arad / 1234  ^|  amirmohammad / 1234
