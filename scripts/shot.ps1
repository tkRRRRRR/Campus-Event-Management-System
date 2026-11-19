# Crop the app window from the desktop and save it as a figure.
# Usage: powershell -File scripts/shot.ps1 -Name 03-student-home
param(
    [Parameter(Mandatory = $true)][string]$Name,
    [int]$X = 60, [int]$Y = 30, [int]$W = 1800, [int]$H = 1126
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$dir = Join-Path $root 'doc\figures'
if (!(Test-Path $dir)) { New-Item -ItemType Directory -Path $dir | Out-Null }

$bmp = New-Object System.Drawing.Bitmap($W, $H)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.CopyFromScreen($X, $Y, 0, 0, (New-Object System.Drawing.Size($W, $H)))
$out = Join-Path $dir "$Name.png"
$bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose(); $bmp.Dispose()
"$out"
