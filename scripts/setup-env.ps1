<#
    便携开发环境一键准备（不写注册表、不装系统、删目录即卸载）
    用法：
        pwsh -File scripts/setup-env.ps1             # 缺什么下什么
        pwsh -File scripts/setup-env.ps1 -Force      # 全部重下
    产物：
        tools/jdk8      Adoptium Temurin 8 JDK（编译用）
        tools/maven     Maven 3.9.9
        tools/node      Node.js 16.20.2
#>
param([switch]$Force)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

# Windows PowerShell 5.1 需要显式加载压缩程序集（PowerShell 7 已内置）
Add-Type -AssemblyName System.IO.Compression.FileSystem -ErrorAction SilentlyContinue

$root  = Split-Path -Parent $PSScriptRoot
$tools = Join-Path $root 'tools'
$down  = Join-Path $root 'downloads'
New-Item -ItemType Directory -Force -Path $tools, $down | Out-Null

$items = @(
    @{
        Name = 'jdk8'; Dir = 'jdk8'; Exe = 'bin\java.exe'; Probe = '-version'
        Urls = @(
            'https://api.adoptium.net/v3/binary/latest/8/ga/windows/x64/jdk/hotspot/normal/eclipse',
            'https://cdn.azul.com/zulu/bin/zulu8.78.0.19-ca-jdk8.0.412-win_x64.zip'
        )
    },
    @{
        Name = 'maven'; Dir = 'maven'; Exe = 'bin\mvn.cmd'; Probe = '-v'
        Urls = @(
            'https://maven.aliyun.com/repository/public/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip',
            'https://dlcdn.apache.org/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip'
        )
    },
    @{
        Name = 'node'; Dir = 'node'; Exe = 'node.exe'; Probe = '-v'
        Urls = @(
            'https://npmmirror.com/mirrors/node/v16.20.2/node-v16.20.2-win-x64.zip'
        )
    }
)

function Get-FirstWorking {
    param($urls, $outFile)
    foreach ($u in $urls) {
        try {
            Write-Host "  尝试下载: $u"
            $sw = [System.Diagnostics.Stopwatch]::StartNew()
            Invoke-WebRequest -Uri $u -OutFile $outFile -TimeoutSec 900 -UseBasicParsing
            $sw.Stop()
            $mb = [math]::Round((Get-Item $outFile).Length / 1MB, 1)
            Write-Host "  成功: $mb MB，耗时 $([math]::Round($sw.Elapsed.TotalSeconds,1))s" -ForegroundColor Green
            return $true
        } catch {
            Write-Warning "  失败: $($_.Exception.Message)"
            if (Test-Path $outFile) { Remove-Item $outFile -Force }
        }
    }
    return $false
}

foreach ($it in $items) {
    $target = Join-Path $tools $it.Dir
    $exe    = Join-Path $target $it.Exe

    if ((Test-Path $exe) -and -not $Force) {
        Write-Host "[$($it.Name)] 已存在，跳过" -ForegroundColor DarkGray
        continue
    }

    Write-Host "[$($it.Name)] 开始准备 ..." -ForegroundColor Cyan
    $zip = Join-Path $down "$($it.Name).zip"

    if ((Test-Path $zip) -and $Force) { Remove-Item $zip -Force }
    if (-not (Test-Path $zip)) {
        if (-not (Get-FirstWorking -urls $it.Urls -outFile $zip)) {
            throw "[$($it.Name)] 所有下载源均失败，请检查网络或代理"
        }
    }

    $tmp = Join-Path $down "tmp-$($it.Name)"
    if (Test-Path $tmp) { Remove-Item $tmp -Recurse -Force }
    Write-Host "  解压中 ..." -ForegroundColor DarkGray
    [System.IO.Compression.ZipFile]::ExtractToDirectory($zip, $tmp)

    $inner = Get-ChildItem $tmp -Directory | Select-Object -First 1
    if (-not $inner) { throw "[$($it.Name)] 压缩包结构异常，没有顶层目录" }

    if (Test-Path $target) { Remove-Item $target -Recurse -Force }
    Move-Item $inner.FullName $target
    Remove-Item $tmp -Recurse -Force

    if (-not (Test-Path $exe)) { throw "[$($it.Name)] 解压后找不到 $exe" }
    Write-Host "[$($it.Name)] 完成" -ForegroundColor Green
}

# ---- 验证 ----
Write-Host "`n===== 版本验证 =====" -ForegroundColor Cyan
$ErrorActionPreference = 'Continue'
$env:JAVA_HOME = Join-Path $tools 'jdk8'
$env:PATH = "$env:JAVA_HOME\bin;$(Join-Path $tools 'maven\bin');$(Join-Path $tools 'node');$env:PATH"

$javaExe = Join-Path $tools 'jdk8\bin\java.exe'
$nodeExe = Join-Path $tools 'node\node.exe'
$mvnExe  = Join-Path $tools 'maven\bin\mvn.cmd'

# java -version 输出到 stderr，用 cmd 合并避免 PowerShell 把它当成错误
$javaOut = (& cmd /c "`"$javaExe`" -version 2>&1" | Out-String)
$nodeOut = (& $nodeExe -v | Out-String)
$mvnOut  = (& cmd /c "`"$mvnExe`" -v 2>&1" | Out-String)

Write-Host ("  java  : " + (($javaOut -split "`n")[0]).Trim())
Write-Host ("  node  : " + $nodeOut.Trim())
Write-Host ("  maven : " + (($mvnOut -split "`n")[0]).Trim())

Write-Host "`n全部就绪。后续会话请先执行: . .\scripts\env.ps1" -ForegroundColor Green
