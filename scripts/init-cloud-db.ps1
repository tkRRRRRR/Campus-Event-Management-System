# 初始化云端 MySQL：建表 + 索引 + 演示数据（会 DROP 重建，谨慎执行）
# 用法：
#   powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\init-cloud-db.ps1 `
#     -DbHost mysql6.sqlpub.com -DbPort 3311 -DbName tkrrrrrr -DbUser tkrrrrrr -DbPassword ****
#   加 -SchemaOnly 只建表，不灌演示数据
param(
    [string]$DbHost = "mysql6.sqlpub.com",
    [int]$DbPort = 3311,
    [string]$DbName = "tkrrrrrr",
    [string]$DbUser = "tkrrrrrr",
    [string]$DbPassword = "",
    [switch]$SchemaOnly
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$jdk = Join-Path $root "tools\jdk8"
if (-not (Test-Path $jdk)) { Write-Host "missing tools\jdk8, run scripts\setup-env.ps1 first"; exit 1 }

$driver = Join-Path $env:USERPROFILE ".m2\repository\com\mysql\mysql-connector-j\8.0.33\mysql-connector-j-8.0.33.jar"
if (-not (Test-Path $driver)) {
    $driver = (Get-ChildItem "$env:USERPROFILE\.m2" -Recurse -Filter "mysql-connector-j-*.jar" | Select-Object -First 1).FullName
}
if (-not $driver) { Write-Host "mysql driver jar not found in ~/.m2"; exit 1 }

$out = Join-Path $env:TEMP "dbinit-out"
New-Item -ItemType Directory -Force -Path $out | Out-Null

Write-Host "compiling DbInit ..."
& (Join-Path $jdk "bin\javac.exe") -encoding UTF-8 -cp $driver -d $out "scripts\DbInit.java"
if ($LASTEXITCODE -ne 0) { Write-Host "compile failed"; exit 1 }

$extra = @()
if ($SchemaOnly) { $extra += "--schema-only" }

Write-Host "importing into $DbHost`:$DbPort/$DbName ..."
& (Join-Path $jdk "bin\java.exe") '-Dfile.encoding=UTF-8' -cp "$driver;$out" DbInit $DbHost $DbPort $DbName $DbUser $DbPassword @extra
exit $LASTEXITCODE
