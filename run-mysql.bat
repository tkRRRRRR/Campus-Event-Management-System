@echo off
chcp 65001 >nul
setlocal

rem ============================================================
rem  MySQL 模式：连接本地或云端（SQLPub）数据库
rem  连接参数通过环境变量传入，不写死在代码里
rem
rem  首次使用前请先导入数据库脚本：
rem    mysql -h 主机 -P 端口 -u 用户 -p 库名 < db\schema.sql
rem    mysql -h 主机 -P 端口 -u 用户 -p 库名 < db\indexes.sql
rem    mysql -h 主机 -P 端口 -u 用户 -p 库名 < db\data.sql
rem ============================================================

set "ROOT=%~dp0"
set "JAVA_EXE=java"

if exist "%ROOT%jre8\bin\java.exe" set "JAVA_EXE=%ROOT%jre8\bin\java.exe"
if exist "%ROOT%tools\jdk8\bin\java.exe" set "JAVA_EXE=%ROOT%tools\jdk8\bin\java.exe"

set "JAR=%ROOT%campus-activity.jar"
if not exist "%JAR%" set "JAR=%ROOT%target\campus-activity.jar"

if not exist "%JAR%" (
    echo [错误] 没找到 campus-activity.jar
    pause
    exit /b 1
)

rem ---------- 数据库连接参数（按需修改这三行）----------
if "%DB_HOST%"==""     set "DB_HOST=localhost"
if "%DB_PORT%"==""     set "DB_PORT=3306"
if "%DB_NAME%"==""     set "DB_NAME=campus_activity"
if "%DB_USER%"==""     set "DB_USER=root"
if "%DB_PASSWORD%"=="" set "DB_PASSWORD=root"
rem SQLPub 等云端数据库需要 SSL，改成 true
if "%DB_SSL%"==""      set "DB_SSL=true"

echo.
echo   校园活动管理系统 - MySQL 模式
echo   数据库: %DB_HOST%:%DB_PORT%/%DB_NAME%
echo   访问地址: http://localhost:8080
echo.

"%JAVA_EXE%" -jar "%JAR%"

pause
