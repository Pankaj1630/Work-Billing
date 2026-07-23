@echo off
title Prepare Portable Package (run on MAIN computer)
cd /d "%~dp0"

REM Find Maven
set "MVN=mvn"
where mvn >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    if exist "C:\apache-maven-3.8.5\bin\mvn.cmd" set "MVN=C:\apache-maven-3.8.5\bin\mvn.cmd"
    if exist "C:\apache-maven-3.9.9\bin\mvn.cmd" set "MVN=C:\apache-maven-3.9.9\bin\mvn.cmd"
)

if not exist "%MVN%" if "%MVN%"=="mvn" (
    echo Maven not found. Install Maven first.
    pause
    exit /b 1
)

echo Building portable package...
echo This copies all libraries into portable\lib
echo Other computer will only need Java - no Maven needed.
echo.

call "%MVN%" clean package -DskipTests
if errorlevel 1 (
    echo Build failed.
    pause
    exit /b 1
)

echo.
echo ============================================
echo   Portable package ready!
echo ============================================
echo.
echo Folder: %~dp0portable\lib
echo.
echo TO SEND TO OTHER COMPUTER:
echo   1. Zip the whole "work-management-billing" folder
echo   2. On other PC: unzip anywhere
echo   3. Install JDK 17 or 21 only
echo   4. Run: Check-Requirements.bat
echo   5. Run: Run App (Portable).bat
echo.
pause
