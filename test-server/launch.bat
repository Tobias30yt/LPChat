@echo off
setlocal
cd /d "%~dp0"
powershell.exe -NoLogo -NoProfile -ExecutionPolicy RemoteSigned -File "%~dp0start.ps1" %*
set "LPCHAT_EXIT=%ERRORLEVEL%"
if /I "%~1"=="-ValidateOnly" exit /b %LPCHAT_EXIT%
echo.
if not "%LPCHAT_EXIT%"=="0" echo Start fehlgeschlagen. Hinweise stehen oben.
pause
exit /b %LPCHAT_EXIT%
