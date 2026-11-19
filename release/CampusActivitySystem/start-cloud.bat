@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0"
set "JAVA=%~dp0runtime\bin\java.exe"
if not exist "%JAVA%" (
  echo [ERROR] runtime folder missing
  pause
  exit /b 1
)
rem ---- cloud MySQL connection, edit config-cloud.bat if needed ----
if exist config-cloud.bat call config-cloud.bat
if "%DB_HOST%"=="" set "DB_HOST=localhost"
if "%DB_PORT%"=="" set "DB_PORT=3306"
if "%DB_NAME%"=="" set "DB_NAME=campus_activity"
if "%DB_USER%"=="" set "DB_USER=root"
if "%DB_PASSWORD%"=="" set "DB_PASSWORD=root"
set "PORT=8080"
if not "%~1"=="" set "PORT=%~1"
echo ============================================
echo  Campus Activity Management System (MySQL)
echo  DB   : %DB_HOST%:%DB_PORT%/%DB_NAME%
echo  URL  : http://127.0.0.1:%PORT%
echo ============================================
start "" http://127.0.0.1:%PORT%
"%JAVA%" -Dfile.encoding=UTF-8 -jar "app\campus-activity.jar" --spring.profiles.active=mysql --server.port=%PORT%
pause