@echo off
chcp 65001 >nul
setlocal

rem ============================================================
rem  H2 演示模式：不需要 MySQL，不需要联网，双击即可
rem  数据在内存中，每次启动都是干净的演示数据
rem ============================================================

set "ROOT=%~dp0"
set "JAVA_EXE=java"

if exist "%ROOT%jre8\bin\java.exe" set "JAVA_EXE=%ROOT%jre8\bin\java.exe"
if exist "%ROOT%tools\jdk8\bin\java.exe" set "JAVA_EXE=%ROOT%tools\jdk8\bin\java.exe"

set "JAR=%ROOT%campus-activity.jar"
if not exist "%JAR%" set "JAR=%ROOT%target\campus-activity.jar"

if not exist "%JAR%" (
    echo [错误] 没找到 campus-activity.jar
    echo        请先执行： mvn -s scripts\settings.xml clean package -DskipTests
    pause
    exit /b 1
)

echo.
echo   校园活动管理系统 - 演示模式
echo   访问地址: http://localhost:8080
echo   演示账号: admin / 2022010102 / 2023010115   密码都是 123456
echo   关闭窗口即停止服务
echo.

"%JAVA_EXE%" -jar "%JAR%" --spring.profiles.active=h2

pause
