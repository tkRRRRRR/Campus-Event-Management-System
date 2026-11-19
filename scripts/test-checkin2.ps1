# Check-in module: edge cases (ASCII source, no BOM needed)
param([string]$Base = 'http://127.0.0.1:8080')

function Login($u, $p) {
    $b = @{ username = $u; password = $p } | ConvertTo-Json
    (Invoke-RestMethod -Uri "$Base/api/auth/login" -Method Post -ContentType 'application/json' `
        -Body ([System.Text.Encoding]::UTF8.GetBytes($b))).data.token
}

function Call($method, $path, $token, $obj) {
    $h = @{ Authorization = "Bearer $token" }
    try {
        if ($obj) {
            $json = $obj | ConvertTo-Json
            $resp = Invoke-WebRequest -Uri "$Base$path" -Method $method -Headers $h -ContentType 'application/json' `
                -Body ([System.Text.Encoding]::UTF8.GetBytes($json)) -UseBasicParsing
        } else {
            $resp = Invoke-WebRequest -Uri "$Base$path" -Method $method -Headers $h -UseBasicParsing
        }
        [pscustomobject]@{ code = 200; body = ($resp.Content | ConvertFrom-Json) }
    } catch {
        $status = $_.Exception.Response.StatusCode.value__
        $body = $null
        try {
            $stream = $_.Exception.Response.GetResponseStream()
            $reader = New-Object System.IO.StreamReader($stream, [System.Text.Encoding]::UTF8)
            $body = $reader.ReadToEnd() | ConvertFrom-Json
        } catch { }
        [pscustomobject]@{ code = $status; body = $body }
    }
}

function Show($label, $r) {
    $code = if ($r.body) { $r.body.code } else { 'HTTP ' + $r.code }
    $msg = if ($r.body) { $r.body.msg } else { '' }
    Write-Host ("{0,-40} code={1,-6} msg={2}" -f $label, $code, $msg)
}

function NewActivity($org, $title, $startOffsetMin, $endOffsetMin) {
    $now = Get-Date
    $r = Call 'POST' '/api/activity' $org @{
        title = $title; categoryId = 2; summary = 't'; content = 't'; location = 'gym'
        startTime = $now.AddMinutes($startOffsetMin).ToString('yyyy-MM-dd HH:mm:ss')
        endTime = $now.AddMinutes($endOffsetMin).ToString('yyyy-MM-dd HH:mm:ss')
        signupDeadline = $now.AddMinutes(5).ToString('yyyy-MM-dd HH:mm:ss')
        capacity = 5; needAudit = 0
    }
    $r.body.data
}

function Approve($adm, $aid) {
    Call 'PUT' "/api/activity/$aid/audit" $adm @{ pass = $true } | Out-Null
}

function RegId($org, $aid, $username) {
    $r = Call 'GET' "/api/registration/activity/$aid?pageSize=100" $org $null
    ($r.body.data.records | Where-Object { $_.username -eq $username }).id
}

$org = Login '2022010102' '123456'
$adm = Login 'admin' '123456'
$s1 = Login '2023000201' '123456'
$s3 = Login '2023000203' '123456'
$s5 = Login '2023000205' '123456'
$s6 = Login '2023000206' '123456'

$A1 = 8
Write-Host "=== 1. manual check-in restores an absent record ==="
$stat = (Call 'GET' "/api/checkin/$A1/stat" $org $null).body.data
Write-Host "before: checked=$($stat.checkedCount) absent=$($stat.absentCount)"
$rid = RegId $org $A1 '2023000203'
Write-Host "s3 registrationId=$rid"
Show 'manual check-in absent -> checked' (Call 'POST' "/api/checkin/manual/$rid" $org $null)
Show 'manual again (expect 400)' (Call 'POST' "/api/checkin/manual/$rid" $org $null)
$stat = (Call 'GET' "/api/checkin/$A1/stat" $org $null).body.data
Write-Host "after : checked=$($stat.checkedCount) absent=$($stat.absentCount) rate=$($stat.checkinRate)"

Write-Host "`n=== 2. second absence triggers ban (violation_limit=2) ==="
$A2 = NewActivity $org 'Ban-Test-1' 20 180; Approve $adm $A2
$A3 = NewActivity $org 'Ban-Test-2' 20 180; Approve $adm $A3
foreach ($a in @($A2, $A3)) {
    Call 'POST' "/api/registration/$a" $s3 $null | Out-Null
    Call 'POST' "/api/registration/$a" $s5 $null | Out-Null
    $f = Call 'POST' "/api/checkin/$a/finish" $org $null
    Show "finish act $a" $f
}

$A4 = NewActivity $org 'Ban-Test-3' 20 180; Approve $adm $A4
Show 's3 register while banned' (Call 'POST' "/api/registration/$A4" $s3 $null)
Show 's5 register while banned' (Call 'POST' "/api/registration/$A4" $s5 $null)
Show 's6 (clean) register' (Call 'POST' "/api/registration/$A4" $s6 $null)

Write-Host "`n=== 3. check-in window not open yet ==="
$A5 = NewActivity $org 'Future-Activity' 300 420; Approve $adm $A5
Call 'POST' "/api/registration/$A5" $s6 $null | Out-Null
Show 'generate code too early' (Call 'POST' "/api/checkin/$A5/code" $org $null)
Show 's6 checkin too early' (Call 'POST' "/api/checkin/$A5" $s6 @{ code = '123456' })
