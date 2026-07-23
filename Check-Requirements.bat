@echo off
setlocal EnableDelayedExpansion
title Check Requirements
cd /d "%~dp0"

echo ============================================
echo   Work Billing - System Check
echo ============================================
echo.

set OK=1

REM ---- Check Java ----
echo [1] Checking Java...
set JAVA_FOUND=0
where java >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    for /f "delims=" %%J in ('where java 2^>nul') do (
        if !JAVA_FOUND! EQU 0 (
            echo     Found: %%J
            set JAVA_FOUND=1
            "%%J" -version 2>&1 | findstr /i "version"
        )
    )
) else (
    echo     NOT FOUND in PATH
    set OK=0
)

if exist "C:\Program Files\Java" (
    echo     Installed JDK folders:
    dir /b "C:\Program Files\Java" 2>nul
)
echo.

REM ---- Check Maven ----
echo [2] Checking Maven...
set MVN_FOUND=0
where mvn >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    for /f "delims=" %%M in ('where mvn 2^>nul') do (
        if !MVN_FOUND! EQU 0 (
            echo     Found: %%M
            set MVN_FOUND=1
            call "%%M" -version 2>nul | findstr /i "Apache Maven"
        )
    )
) else (
    echo     NOT FOUND in PATH
)

if exist "%~dp0portable\lib\work-management-billing-1.0.0.jar" (
    echo     Portable package found - Maven NOT required to run
) else if !MVN_FOUND! EQU 0 (
    echo     Maven required unless you use portable package
    set OK=0
)
echo.

REM ---- Check portable package ----
echo [3] Checking portable package...
if exist "%~dp0portable\lib\work-management-billing-1.0.0.jar" (
    echo     OK - portable\lib folder is ready
    dir /b "%~dp0portable\lib\javafx-*-win.jar" 2>nul | findstr javafx >nul
    if %ERRORLEVEL% EQU 0 (
        echo     OK - JavaFX Windows libraries found
    ) else (
        echo     WARNING - JavaFX win jars missing. Run Prepare-For-Other-PC.bat on main computer.
        set OK=0
    )
) else (
    echo     NOT FOUND - run Prepare-For-Other-PC.bat on your main computer first
    echo     OR install Maven and use Launch App.bat
)
echo.

REM ---- Internet ----
echo [4] Note: First Maven run needs internet to download libraries.
echo     Portable package works offline after Prepare-For-Other-PC.bat
echo.

echo ============================================
if %OK% EQU 1 (
    echo   RESULT: Ready to run
    echo   Use: Run App (Portable).bat  OR  Launch App.bat
) else (
    echo   RESULT: Something is missing - see above
    echo.
    echo   FIX on this computer:
    echo   1. Install JDK 17 or 21 from https://adoptium.net
    echo   2. Get portable package from main PC ^(Prepare-For-Other-PC.bat^)
    echo      OR install Maven from https://maven.apache.org
)
echo ============================================
echo.
pause
