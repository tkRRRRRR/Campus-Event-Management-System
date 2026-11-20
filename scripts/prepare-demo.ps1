# Prepare a near-now activity for the live check-in demo.
# Usage: powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\prepare-demo.ps1 [-Port 8080]
param([int]$Port = 8080)

$ErrorActionPreference = "Stop"
$base = "http://127.0.0.1:$Port"
$utf8 = New-Object System.Text.UTF8Encoding($false)

function PostJson($url, $token, $obj) {
    $headers = @{}
    if ($token) { $headers["Authorization"] = "Bearer $token" }
    if ($null -eq $obj) {
        $resp = Invoke-WebRequest -Uri $url -Method Post -Headers $headers -ContentType "application/json" -Body ([byte[]]@()) -UseBasicParsing
    } else {
        $json = $obj | ConvertTo-Json -Compress -Depth 5
        $resp = Invoke-WebRequest -Uri $url -Method Post -Headers $headers -ContentType "application/json" -Body $utf8.GetBytes([string]$json) -UseBasicParsing
    }
    return $resp.Content | ConvertFrom-Json
}

function PutJson($url, $token, $obj) {
    $json = $obj | ConvertTo-Json -Compress -Depth 5
    $headers = @{}
    if ($token) { $headers["Authorization"] = "Bearer $token" }
    $resp = Invoke-WebRequest -Uri $url -Method Put -Headers $headers -ContentType "application/json" -Body $utf8.GetBytes($json) -UseBasicParsing
    return $resp.Content | ConvertFrom-Json
}

function Login($username, $password) {
    $r = PostJson "$base/api/auth/login" $null @{ username = $username; password = $password }
    return $r.data.token
}

Write-Host "login admin / organizer / students ..."
$admin = Login "admin" "123456"
$org = Login "2022010102" "123456"
$s1 = Login "2023000203" "123456"
$s2 = Login "2023000204" "123456"

$now = Get-Date
$start = $now.AddMinutes(4).ToString("yyyy-MM-dd HH:mm:ss")
$deadline = $now.AddMinutes(3).ToString("yyyy-MM-dd HH:mm:ss")
$end = $now.AddMinutes(94).ToString("yyyy-MM-dd HH:mm:ss")

Write-Host "create activity ..."
$body = @{
    title           = "演示活动：校园摄影技巧分享会"
    categoryId      = 3
    summary         = "构图、光线与后期基础，现场提供相机体验"
    content         = "由摄影社指导老师讲解构图、光线与后期基础，现场提供相机体验，适合零基础同学参加。"
    location        = "艺术楼 302 摄影教室"
    startTime       = $start
    endTime         = $end
    signupDeadline  = $deadline
    capacity        = 30
    needAudit       = 0
    draft           = $false
}
$r = PostJson "$base/api/activity" $org $body
if ($r.code -ne 200) { Write-Host ("create failed: " + $r.msg); exit 1 }
$activityId = $r.data

Write-Host "approve ..."
$r = PutJson "$base/api/activity/$activityId/audit" $admin @{ pass = $true; remark = "" }
if ($r.code -ne 200) { Write-Host ("approve failed: " + $r.msg); exit 1 }

Write-Host "students register ..."
$r1 = PostJson "$base/api/registration/$activityId" $s1 $null
$r2 = PostJson "$base/api/registration/$activityId" $s2 $null
Write-Host ("  s1: " + $r1.code + " " + $r1.msg)
Write-Host ("  s2: " + $r2.code + " " + $r2.msg)

Write-Host "generate check-in code ..."
$r = PostJson "$base/api/checkin/$activityId/code" $org $null
$code = $null
if ($r.code -eq 200) { $code = $r.data.code }
$stat = Invoke-RestMethod -Uri "$base/api/checkin/$activityId/stat" -Headers @{ Authorization = "Bearer $org" }

Write-Host ""
Write-Host "================ DEMO READY ================"
Write-Host ("activityId : " + $activityId)
Write-Host ("title      : 演示活动：校园摄影技巧分享会")
Write-Host ("start      : " + $start + "   (check-in opens " + $now.AddMinutes(-26).ToString("HH:mm") + ")")
Write-Host ("end        : " + $end)
Write-Host ("students   : 2023000203 / 2023000204   (password 123456)")
Write-Host ("checkinCode: " + $code)
Write-Host ("signed     : " + $stat.data.signedCount + " / " + $stat.data.capacity)
Write-Host "==========================================="
