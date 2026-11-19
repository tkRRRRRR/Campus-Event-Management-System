# Build web (Vue) then package Spring Boot jar with dist copied into static/
# Usage: powershell -NoProfile -ExecutionPolicy Bypass -File scripts\build-all.ps1
$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$jdk = Join-Path $root 'tools\jdk8'
$mvn = Join-Path $root 'tools\maven\bin\mvn.cmd'
$node = Join-Path $root 'tools\node'
$npm = Join-Path $node 'npm.cmd'

if (-not (Test-Path $jdk)) { throw "JDK not found: $jdk (run scripts/setup-env.ps1 first)" }
if (-not (Test-Path $mvn)) { throw "Maven not found: $mvn" }
if (-not (Test-Path $npm)) { throw "Node not found: $npm" }

$env:JAVA_HOME = $jdk
$env:PATH = "$jdk\bin;$node;" + $env:PATH

# ---------- 1. build web ----------
Write-Host "[1/3] npm run build ..." -ForegroundColor Cyan
Push-Location (Join-Path $root 'web')
& $npm run build
if ($LASTEXITCODE -ne 0) { Pop-Location; throw 'web build failed' }
Pop-Location

# ---------- 2. copy dist -> static ----------
Write-Host "[2/3] copy dist -> src/main/resources/static ..." -ForegroundColor Cyan
$static = Join-Path $root 'src\main\resources\static'
if (Test-Path $static) { Remove-Item $static -Recurse -Force }
New-Item -ItemType Directory -Path $static -Force | Out-Null
Copy-Item (Join-Path $root 'web\dist\*') $static -Recurse -Force

# ---------- 3. package jar ----------
Write-Host "[3/3] mvn clean package ..." -ForegroundColor Cyan
& $mvn -s scripts\settings.xml -B -q -DskipTests clean package
if ($LASTEXITCODE -ne 0) { throw 'maven package failed' }

$jar = Join-Path $root 'target\campus-activity.jar'
$size = [math]::Round((Get-Item $jar).Length / 1MB, 1)
Write-Host "OK -> $jar ($size MB)" -ForegroundColor Green
Write-Host "Run: run-h2.bat  (or java -jar target\campus-activity.jar --spring.profiles.active=h2)"
