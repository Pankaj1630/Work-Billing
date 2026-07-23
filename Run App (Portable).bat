@echo off
setlocal EnableDelayedExpansion
title Work Management and Billing
cd /d "%~dp0"

if not exist "%~dp0portable\lib\work-management-billing-1.0.0.jar" (
    echo Portable package not found.
    echo.
    echo On your MAIN computer, run: Prepare-For-Other-PC.bat
    echo Then copy the whole folder to this computer.
    echo.
    pause
    exit /b 1
)

REM ---- Find Java ----
set "JAVA_EXE="
where java >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    for /f "delims=" %%J in ('where java 2^>nul') do (
        if not defined JAVA_EXE set "JAVA_EXE=%%J"
    )
)
if not defined JAVA_EXE if exist "C:\Program Files\Java\jdk-21\bin\java.exe" set "JAVA_EXE=C:\Program Files\Java\jdk-21\bin\java.exe"
if not defined JAVA_EXE if exist "C:\Program Files\Java\jdk-17\bin\java.exe" set "JAVA_EXE=C:\Program Files\Java\jdk-17\bin\java.exe"
if not defined JAVA_EXE if exist "C:\Program Files\Eclipse Adoptium\jdk-17\bin\java.exe" set "JAVA_EXE=C:\Program Files\Eclipse Adoptium\jdk-17\bin\java.exe"
if not defined JAVA_EXE if exist "C:\Program Files\Eclipse Adoptium\jdk-21\bin\java.exe" set "JAVA_EXE=C:\Program Files\Eclipse Adoptium\jdk-21\bin\java.exe"

if not defined JAVA_EXE (
    echo Java JDK not found.
    echo Install JDK 17 or 21 from https://adoptium.net
    pause
    exit /b 1
)

echo Starting Work Management and Billing...
echo Java: %JAVA_EXE%
echo.

REM Build JavaFX module path from win jars
set "MODULE_PATH="
for %%f in ("%~dp0portable\lib\javafx-*-win.jar") do (
    if defined MODULE_PATH (
        set "MODULE_PATH=!MODULE_PATH!;%%f"
    ) else (
        set "MODULE_PATH=%%f"
    )
)

if not defined MODULE_PATH (
    echo JavaFX libraries missing in portable\lib
    echo Run Prepare-For-Other-PC.bat on main computer.
    pause
    exit /b 1
)

REM Build classpath excluding javafx jars
set "CLASSPATH="
for %%f in ("%~dp0portable\lib\*.jar") do (
    echo %%f | findstr /i "javafx" >nul
    if errorlevel 1 (
        if defined CLASSPATH (
            set "CLASSPATH=!CLASSPATH!;%%f"
        ) else (
            set "CLASSPATH=%%f"
        )
    )
)

"%JAVA_EXE%" --module-path "%MODULE_PATH%" --add-modules javafx.controls,javafx.fxml -cp "%CLASSPATH%" com.workbilling.Main > "%~dp0launch-log.txt" 2>&1
set ERR=%ERRORLEVEL%

if %ERR% NEQ 0 (
    echo.
    echo *** FAILED TO START ***
    type "%~dp0launch-log.txt"
    echo.
    pause
    exit /b %ERR%
)
