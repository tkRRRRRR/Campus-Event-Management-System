# Assemble a portable release folder: app jar + bundled JRE 8 + launcher scripts + db scripts.
# ASCII-only script body (PS 5.1 reads non-ASCII scripts without BOM as ANSI).
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$out = Join-Path $root 'release\CampusActivitySystem'
$jar = Join-Path $root 'target\campus-activity.jar'
$jre = Join-Path $root 'tools\jdk8\jre'

if (!(Test-Path $jar)) { throw "jar not found: $jar (run mvn clean package first)" }
if (!(Test-Path $jre)) { throw "jre not found: $jre" }

Write-Host "Preparing $out"
if (Test-Path $out) { Remove-Item $out -Recurse -Force }
New-Item -ItemType Directory -Path $out | Out-Null
New-Item -ItemType Directory -Path (Join-Path $out 'app') | Out-Null
New-Item -ItemType Directory -Path (Join-Path $out 'db') | Out-Null

Copy-Item $jar (Join-Path $out 'app\campus-activity.jar')
Write-Host "Copying JRE ..."
Copy-Item $jre (Join-Path $out 'runtime') -Recurse
Copy-Item (Join-Path $root 'db\*.sql') (Join-Path $out 'db') -Force

$nl = "`r`n"

$startH2 = @(
    '@echo off',
    'chcp 65001 >nul',
    'setlocal',
    'cd /d "%~dp0"',
    'set "JAVA=%~dp0runtime\bin\java.exe"',
    'if not exist "%JAVA%" (',
    '  echo [ERROR] runtime folder missing, please extract the whole package again',
    '  pause',
    '  exit /b 1',
    ')',
    'set "PORT=8080"',
    'if not "%~1"=="" set "PORT=%~1"',
    'echo ============================================',
    'echo  Campus Activity Management System (H2 demo)',
    'echo  URL : http://127.0.0.1:%PORT%',
    'echo  Keep this window open. Press Ctrl+C to stop.',
    'echo ============================================',
    'start "" http://127.0.0.1:%PORT%',
    '"%JAVA%" -Dfile.encoding=UTF-8 -jar "app\campus-activity.jar" --spring.profiles.active=h2 --server.port=%PORT%',
    'pause'
) -join $nl
[System.IO.File]::WriteAllText((Join-Path $out 'start-demo.bat'), $startH2, (New-Object System.Text.UTF8Encoding($false)))

$startCloud = @(
    '@echo off',
    'chcp 65001 >nul',
    'setlocal',
    'cd /d "%~dp0"',
    'set "JAVA=%~dp0runtime\bin\java.exe"',
    'if not exist "%JAVA%" (',
    '  echo [ERROR] runtime folder missing',
    '  pause',
    '  exit /b 1',
    ')',
    'rem ---- cloud MySQL connection, edit config-cloud.bat if needed ----',
    'if exist config-cloud.bat call config-cloud.bat',
    'if "%DB_HOST%"=="" set "DB_HOST=localhost"',
    'if "%DB_PORT%"=="" set "DB_PORT=3306"',
    'if "%DB_NAME%"=="" set "DB_NAME=campus_activity"',
    'if "%DB_USER%"=="" set "DB_USER=root"',
    'if "%DB_PASSWORD%"=="" set "DB_PASSWORD=root"',
    'set "PORT=8080"',
    'if not "%~1"=="" set "PORT=%~1"',
    'echo ============================================',
    'echo  Campus Activity Management System (MySQL)',
    'echo  DB   : %DB_HOST%:%DB_PORT%/%DB_NAME%',
    'echo  URL  : http://127.0.0.1:%PORT%',
    'echo ============================================',
    'start "" http://127.0.0.1:%PORT%',
    '"%JAVA%" -Dfile.encoding=UTF-8 -jar "app\campus-activity.jar" --spring.profiles.active=mysql --server.port=%PORT%',
    'pause'
) -join $nl
[System.IO.File]::WriteAllText((Join-Path $out 'start-cloud.bat'), $startCloud, (New-Object System.Text.UTF8Encoding($false)))

$config = @(
    '@echo off',
    'rem Fill in your own MySQL / SQLPub connection here.',
    'rem Keep this file next to start-cloud.bat. Do not commit it to a public repo.',
    'set "DB_HOST=mysql6.sqlpub.com"',
    'set "DB_PORT=3311"',
    'set "DB_NAME=tkrrrrrr"',
    'set "DB_USER=tkrrrrrr"',
    'set "DB_PASSWORD=your-password-here"',
    'set "DB_SSL=true"'
) -join $nl
[System.IO.File]::WriteAllText((Join-Path $out 'config-cloud.bat'), $config, (New-Object System.Text.UTF8Encoding($false)))

$readme = @(
    '校园活动管理系统 便携版使用说明',
    '================================',
    '',
    '一、运行环境',
    '  本包自带 Java 运行时（runtime 目录），目标电脑不需要安装 JDK、Maven、MySQL。',
    '  支持 Windows 7 SP1 及以上的 64 位系统。',
    '',
    '二、启动方式',
    '  1. 演示模式（推荐，零配置、不联网）',
    '     双击 start-demo.bat，浏览器会自动打开 http://127.0.0.1:8080',
    '     数据存放在内存数据库中，每次启动都是全新的演示数据，关闭窗口即清空。',
    '     如 8080 端口被占用，可在命令行执行：start-demo.bat 8090',
    '',
    '  2. 云端数据库模式',
    '     双击 start-cloud.bat（需先按第三步配置数据库连接）。',
    '',
    '三、连接云端 MySQL（SQLPub）',
    '  1. 打开 config-cloud.bat，把 DB_HOST / DB_PORT / DB_NAME / DB_USER / DB_PASSWORD',
    '     改成自己数据库的信息（SQLPub 的控制台里可以看到）。',
    '  2. 用任意 MySQL 客户端按顺序执行 db 目录下的脚本：',
    '     schema.sql（建表） -> indexes.sql（索引，可选） -> data.sql（演示数据，可选）',
    '  3. 双击 start-cloud.bat 启动。',
    '',
    '四、演示账号（密码均为 123456）',
    '  管理员    admin',
    '  组织者    2022010102',
    '  学生      2023000201 ~ 2023000210（以及其他 2023 开头的学号）',
    '',
    '五、常见问题',
    '  1. 双击后窗口一闪而过：右键 start-demo.bat -> 属性 -> 目标末尾不要有多余空格，',
    '     或在命令行里执行以查看报错信息。',
    '  2. 8080 端口被占用：start-demo.bat 8090 换端口启动。',
    '  3. 浏览器没有自动打开：手动访问窗口里提示的地址。',
    '  4. 上传的头像保存在程序目录下的 upload 文件夹。',
    '',
    '六、目录结构',
    '  app\campus-activity.jar   后端 + 前端打包成的可执行 jar',
    '  runtime\                  内置 Java 8 运行时',
    '  db\                       建表与演示数据脚本',
    '  start-demo.bat            演示模式启动脚本',
    '  start-cloud.bat           云端数据库模式启动脚本',
    '  config-cloud.bat          云端数据库连接配置'
) -join $nl
[System.IO.File]::WriteAllText((Join-Path $out '使用说明.txt'), $readme, (New-Object System.Text.UTF8Encoding($true)))

$zip = Join-Path $root 'release\CampusActivitySystem.zip'
if (Test-Path $zip) { Remove-Item $zip -Force }
Write-Host "Compressing ..."
Compress-Archive -Path $out -DestinationPath $zip -CompressionLevel Optimal
"zip size MB: " + [math]::Round((Get-Item $zip).Length / 1MB, 1)
"folder size MB: " + [math]::Round(((Get-ChildItem $out -Recurse -File | Measure-Object -Property Length -Sum).Sum / 1MB), 1)
