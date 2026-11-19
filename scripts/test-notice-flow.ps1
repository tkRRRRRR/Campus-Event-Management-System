# Verify personal notice unread flow (audit notification) - ASCII output only.
$base = 'http://127.0.0.1:8080'

function Login([string]$u, [string]$p) {
    $b = @{ username = $u; password = $p } | ConvertTo-Json
    (Invoke-RestMethod -Uri "$base/api/auth/login" -Method Post -ContentType 'application/json' -Body ([Text.Encoding]::UTF8.GetBytes($b))).data.token
}

function Call([string]$m, [string]$p, [string]$t, $o) {
    $h = @{ Authorization = "Bearer $t" }
    if ($o -ne $null) {
        $j = $o | ConvertTo-Json
        (Invoke-WebRequest -Uri "$base$p" -Method $m -Headers $h -ContentType 'application/json' -Body ([Text.Encoding]::UTF8.GetBytes($j)) -UseBasicParsing).Content | ConvertFrom-Json
    } else {
        (Invoke-WebRequest -Uri "$base$p" -Method $m -Headers $h -UseBasicParsing).Content | ConvertFrom-Json
    }
}

$adm = Login 'admin' '123456'
$org = Login '2022010102' '123456'

$before = Call 'GET' '/api/notice/unread-count' $org
Write-Host ("unread before = " + $before.data.count)

$r = Call 'POST' '/api/activity' $org @{ title = 'Notice Flow Test'; categoryId = 1; summary = 's'; content = 'c'; location = 'L'; startTime = '2027-05-01 10:00:00'; endTime = '2027-05-01 12:00:00'; signupDeadline = '2027-04-28 18:00:00'; capacity = 5; needAudit = 0 }
Write-Host ("create code=" + $r.code + " id=" + $r.data)

$au = Call 'PUT' ('/api/activity/' + $r.data + '/audit') $adm @{ pass = $true }
Write-Host ("audit code=" + $au.code + " msg=" + $au.msg)

$my = Call 'GET' '/api/notice/my?pageNum=1&pageSize=5' $org
Write-Host ("org notices total=" + $my.data.total)
$u = Call 'GET' '/api/notice/unread-count' $org
Write-Host ("org unread after audit = " + $u.data.count)

$first = $null
foreach ($x in @($my.data.records)) { if ($x.isRead -eq 0) { $first = $x } }
if ($first -ne $null) {
    Write-Host ("first unread id=" + $first.id + " type=" + $first.noticeType + " title=" + $first.title)
    $rd = Call 'PUT' ('/api/notice/' + $first.id + '/read') $org $null
    $u2 = Call 'GET' '/api/notice/unread-count' $org
    Write-Host ("after read code=" + $rd.code + " unread=" + $u2.data.count)
} else {
    Write-Host 'NO UNREAD NOTICE FOUND - notification not created'
}
