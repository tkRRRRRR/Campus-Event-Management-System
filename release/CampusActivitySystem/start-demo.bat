@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0"
set "JAVA=%~dp0runtime\bin\java.exe"
if not exist "%JAVA%" (
  echo [ERROR] runtime folder missing, please extract the whole package again
  pause
  exit /b 1
)
set "PORT=8080"
if not "%~1"=="" set "PORT=%~1"
echo ============================================
echo  Campus Activity Management System (H2 demo)
echo  URL : http://127.0.0.1:%PORT%
echo  Keep this window open. Press Ctrl+C to stop.
echo ============================================
start "" http://127.0.0.1:%PORT%
"%JAVA%" -Dfile.encoding=UTF-8 -jar "app\campus-activity.jar" --spring.profiles.active=h2 --server.port=%PORT%
pause