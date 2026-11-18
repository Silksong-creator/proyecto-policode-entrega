@echo off
setlocal
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0iniciar.ps1"
set "CODIGO=%ERRORLEVEL%"
echo.
pause
exit /b %CODIGO%
