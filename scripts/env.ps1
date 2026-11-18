<#
    在当前 PowerShell 会话里启用便携环境（不修改系统环境变量）
    用法：  . .\scripts\env.ps1        ← 注意前面有一个点，表示在当前会话执行
#>
$repoRoot = Split-Path -Parent $PSScriptRoot
$toolsDir = Join-Path $repoRoot 'tools'

foreach ($need in @('jdk8', 'maven', 'node')) {
    if (-not (Test-Path (Join-Path $toolsDir $need))) {
        Write-Warning "缺少 tools/$need，请先运行 scripts/setup-env.ps1"
    }
}

$env:JAVA_HOME  = Join-Path $toolsDir 'jdk8'
$env:M2_HOME    = Join-Path $toolsDir 'maven'
$env:NODE_HOME  = Join-Path $toolsDir 'node'
$env:PATH = "$env:JAVA_HOME\bin;$env:M2_HOME\bin;$env:NODE_HOME;$env:PATH"
$env:NODE_OPTIONS = ''

Write-Host "便携环境已启用:" -ForegroundColor Green
Write-Host "  JAVA_HOME = $env:JAVA_HOME"
Write-Host "  MAVEN     = $env:M2_HOME"
Write-Host "  NODE      = $env:NODE_HOME"
