# Check-in module end-to-end test (ASCII output to avoid console encoding issues)
param([string]$Base = 'http://127.0.0.1:8080')

$ErrorActionPreference = 'Continue'

function Login($u, $p) {
    $b = @{ username = $u; password = $p } | ConvertTo-Json
    $r = Invoke-RestMethod -Uri "$Base/api/auth/login" -Method Post -ContentType 'application/json' `
        -Body ([System.Text.Encoding]::UTF8.GetBytes($b))
    return $r.data.token
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
        return [pscustomobject]@{ code = 200; body = ($resp.Content | ConvertFrom-Json) }
    } catch {
        $status = $_.Exception.Response.StatusCode.value__
        $body = $null
        try {
            $stream = $_.Exception.Response.GetResponseStream()
            $reader = New-Object System.IO.StreamReader($stream, [System.Text.Encoding]::UTF8)
            $text = $reader.ReadToEnd()
            $body = $text | ConvertFrom-Json
        } catch { }
        return [pscustomobject]@{ code = $status; body = $body }
    }
}

function Show($label, $r) {
    $code = if ($r.body) { $r.body.code } else { 'HTTP ' + $r.code }
    $msg = if ($r.body) { $r.body.msg } else { '' }
    $data = if ($r.body -and $r.body.data) { $r.body.data } else { '' }
    Write-Host ("{0,-42} code={1,-6} msg={2} {3}" -f $label, $code, $msg, $data)
}

$org = Login '2022010102' '123456'
$adm = Login 'admin' '123456'
$s1 = Login '2023000201' '123456'
$s2 = Login '2023000202' '123456'
$s3 = Login '2023000203' '123456'
Write-Host "login ok: org/admin/s1/s2/s3"

# ---------- create activity (starts in 20 min, ends in 3h) ----------
$now = Get-Date
$start = $now.AddMinutes(20).ToString('yyyy-MM-dd HH:mm:ss')
$end = $now.AddHours(3).ToString('yyyy-MM-dd HH:mm:ss')
$deadline = $now.AddMinutes(15).ToString('yyyy-MM-dd HH:mm:ss')

$r = Call 'POST' '/api/activity' $org @{
    title = 'Checkin-E2E-活动'; categoryId = 2; summary = 'checkin test'; content = 'checkin test'
    location = '体育馆'; startTime = $start; endTime = $end; signupDeadline = $deadline
    capacity = 5; needAudit = 0
}
Show 'POST /api/activity' $r
$aid = $r.body.data
Call 'PUT' "/api/activity/$aid/audit" $adm @{ pass = $true } | Out-Null
Write-Host "activityId=$aid start=$start end=$end"

# ---------- register 3 students ----------
foreach ($t in @($s1, $s2, $s3)) {
    $rr = Call 'POST' "/api/registration/$aid" $t $null
    Show 'register' $rr
}

# ---------- generate code ----------
$r = Call 'POST' "/api/checkin/$aid/code" $org $null
Show 'POST /code (generate)' $r
$code = $r.body.data.code
$expire = $r.body.data.expireTime
$remain = $r.body.data.remainSeconds
Write-Host "code=$code expire=$expire remainSeconds=$remain"

# ---------- idempotent code reuse ----------
$r2 = Call 'POST' "/api/checkin/$aid/code" $org $null
Write-Host "reuse same code? $(if ($r2.body.data.code -eq $code) { 'YES' } else { 'NO' })"

# ---------- student cannot generate code ----------
Show 'student POST /code (expect 403)' (Call 'POST' "/api/checkin/$aid/code" $s1 $null)

# ---------- wrong code ----------
Show 's1 checkin wrong code' (Call 'POST' "/api/checkin/$aid" $s1 @{ code = '000000' })

# ---------- right code, not registered student ----------
$s4 = Login '2023000204' '123456'
Show 's4 (not registered) checkin' (Call 'POST' "/api/checkin/$aid" $s4 @{ code = $code })

# ---------- s1 checkin ok ----------
Show 's1 checkin ok' (Call 'POST' "/api/checkin/$aid" $s1 @{ code = $code })

# ---------- s1 checkin again ----------
Show 's1 checkin again' (Call 'POST' "/api/checkin/$aid" $s1 @{ code = $code })

# ---------- stat ----------
$r = Call 'GET' "/api/checkin/$aid/stat" $org $null
Show 'GET /stat' $r

# ---------- list: checked only ----------
$r = Call 'GET' "/api/checkin/$aid/list?status=2" $org $null
Show 'GET /list?status=2 checked' ([pscustomobject]@{ code = 200; body = [pscustomobject]@{ code = $r.body.code; msg = $r.body.msg; data = ('records=' + $r.body.data.records.Count) } })

# ---------- manual checkin for s2 ----------
$r = Call 'GET' "/api/registration/activity/$aid" $org $null
$target = ($r.body.data.records | Where-Object { $_.username -eq '2023000202' })
Write-Host "s2 registrationId=$($target.id) status=$($target.status)"
Show 'manual checkin s2' (Call 'POST' "/api/checkin/manual/$($target.id)" $org $null)

# ---------- finish activity -> s3 absent ----------
Show 'finish activity' (Call 'POST' "/api/checkin/$aid/finish" $org $null)
Show 'finish again (expect 400)' (Call 'POST' "/api/checkin/$aid/finish" $org $null)

# ---------- final stat ----------
$r = Call 'GET' "/api/checkin/$aid/stat" $adm $null
Show 'final /stat' $r

# ---------- s3 tries checkin after finish ----------
Show 's3 checkin after finish' (Call 'POST' "/api/checkin/$aid" $s3 @{ code = $code })

# ---------- activity status ----------
$r = Call 'GET' "/api/activity/$aid" $s1 $null
Write-Host "activity status now = $($r.body.data.status) (5=finished)"

# ---------- s3 my registrations ----------
$r = Call 'GET' '/api/registration/my?status=4' $s3 $null
Show 's3 my absent list' ([pscustomobject]@{ code = 200; body = [pscustomobject]@{ code = $r.body.code; msg = ''; data = ('records=' + $r.body.data.records.Count) } })
