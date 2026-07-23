@echo off
title Create Desktop Shortcut
cd /d "%~dp0"

set VBS_LAUNCHER=%~dp0Start Work Billing.vbs
for /f "delims=" %%D in ('powershell -NoProfile -Command "[Environment]::GetFolderPath('Desktop')"') do set DESKTOP=%%D
set SHORTCUT_NAME=Work Billing.lnk

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ws = New-Object -ComObject WScript.Shell; ^
   $s = $ws.CreateShortcut('%DESKTOP%\%SHORTCUT_NAME%'); ^
   $s.TargetPath = '%~dp0Start Work Billing.vbs'; ^
   $s.WorkingDirectory = '%~dp0'; ^
   $s.Description = 'Work Management and Billing'; ^
   $s.Save()"

echo.
echo Desktop shortcut created:
echo %DESKTOP%\%SHORTCUT_NAME%
echo.
echo Double-click "Work Billing" on your Desktop to launch the GUI app.
pause
