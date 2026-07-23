@echo off
setlocal EnableDelayedExpansion
title Work Management and Billing
cd /d "%~dp0"

REM Prefer portable mode if available (no Maven needed)
if exist "%~dp0portable\lib\work-management-billing-1.0.0.jar" (
    call "%~dp0Run App (Portable).bat"
    exit /b %ERRORLEVEL%
)

REM ---- Find Java from PATH or common locations ----
set "JAVA_HOME="
where java >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    for /f "delims=" %%J in ('where java 2^>nul') do (
        if not defined JAVA_HOME (
            for %%K in ("%%~dpJ..") do set "JAVA_HOME=%%~fK"
        )
    )
)
if not defined JAVA_HOME if exist "C:\Program Files\Java\jdk-21" set "JAVA_HOME=C:\Program Files\Java\jdk-21"
if not defined JAVA_HOME if exist "C:\Program Files\Java\jdk-17" set "JAVA_HOME=C:\Program Files\Java\jdk-17"
if not defined JAVA_HOME if exist "C:\Program Files\Eclipse Adoptium\jdk-21" set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21"
if not defined JAVA_HOME if exist "C:\Program Files\Eclipse Adoptium\jdk-17" set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17"
if not defined JAVA_HOME if exist "C:\jdk-17.0.2" set "JAVA_HOME=C:\jdk-17.0.2"

REM ---- Find Maven from PATH or common locations ----
set "MAVEN_HOME="
set "MVN_CMD="
where mvn >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    for /f "delims=" %%M in ('where mvn 2^>nul') do (
        if not defined MVN_CMD set "MVN_CMD=%%M"
    )
)
if not defined MVN_CMD if exist "C:\apache-maven-3.8.5\bin\mvn.cmd" set "MVN_CMD=C:\apache-maven-3.8.5\bin\mvn.cmd"
if not defined MVN_CMD if exist "C:\apache-maven-3.9.9\bin\mvn.cmd" set "MVN_CMD=C:\apache-maven-3.9.9\bin\mvn.cmd"

if not defined JAVA_HOME (
    echo ERROR: Java JDK not found.
    echo Install JDK 17 or 21 from https://adoptium.net
    echo Then run Check-Requirements.bat
    pause
    exit /b 1
)

if not defined MVN_CMD (
    echo ERROR: Maven not found.
    echo.
    echo OPTION A - Easier: On main PC run Prepare-For-Other-PC.bat
    echo              then copy folder and use Run App (Portable).bat
    echo.
    echo OPTION B - Install Maven from https://maven.apache.org
    pause
    exit /b 1
)

for %%K in ("%MVN_CMD%") do set "MAVEN_HOME=%%~dpK.."
set "PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%PATH%"

echo ============================================
echo   Work Management and Billing
echo ============================================
echo Java : %JAVA_HOME%
echo Maven: %MVN_CMD%
echo.
echo Starting... please wait 30-60 seconds.
echo.

call "%MVN_CMD%" javafx:run > "%~dp0launch-log.txt" 2>&1
set ERR=%ERRORLEVEL%

if %ERR% NEQ 0 (
    echo *** FAILED TO START ***
    type "%~dp0launch-log.txt"
    echo.
    echo Log: %~dp0launch-log.txt
    pause
    exit /b %ERR%
)

echo App closed.
pause
