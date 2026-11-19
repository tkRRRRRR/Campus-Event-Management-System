# Admin / notice / config / user-center smoke test (ASCII output only).
$ErrorActionPreference = 'Continue'
$base = 'http://127.0.0.1:8080'
$script:pass = 0
$script:fail = 0

function Login([string]$u, [string]$p) {
    $body = @{ username = $u; password = $p } | ConvertTo-Json
    try {
        $r = Invoke-RestMethod -Uri "$base/api/auth/login" -Method Post -ContentType 'application/json' -Body ([Text.Encoding]::UTF8.GetBytes($body))
        return $r.data.token
    } catch { return $null }
}

function Call([string]$method, [string]$path, [string]$token, $obj) {
    $h = @{ Authorization = "Bearer $token" }
    try {
        if ($obj -ne $null) {
            $json = $obj | ConvertTo-Json
            $resp = Invoke-WebRequest -Uri "$base$path" -Method $method -Headers $h -ContentType 'application/json' -Body ([Text.Encoding]::UTF8.GetBytes($json)) -UseBasicParsing
        } else {
            $resp = Invoke-WebRequest -Uri "$base$path" -Method $method -Headers $h -UseBasicParsing
        }
        return ($resp.Content | ConvertFrom-Json)
    } catch {
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        $text = $reader.ReadToEnd()
        return ($text | ConvertFrom-Json)
    }
}

function Check([string]$name, [bool]$ok, [string]$detail) {
    if ($ok) { $script:pass++; Write-Host ("[PASS] " + $name) }
    else { $script:fail++; Write-Host ("[FAIL] " + $name + "  -> " + $detail) }
}

$adm = Login 'admin' '123456'
$org = Login '2022010102' '123456'
$stu = Login '2023000201' '123456'
Check 'login admin/org/stu' ($adm -ne $null -and $org -ne $null -and $stu -ne $null) 'token missing'

Write-Host '--- announcement ---'
$list = Call 'GET' '/api/announcement/list' $stu
Check 'student sees announcement list' ($list.code -eq 200) ($list | ConvertTo-Json -Compress)
$cntBefore = 0
if ($list.data) { $cntBefore = @($list.data).Count }

$n = Call 'POST' '/api/announcement' $stu @{ title = 'x'; content = 'y'; status = 1 }
Check 'student cannot create announcement (403)' ($n.code -eq 403) ("code=" + $n.code + " msg=" + $n.msg)

$created = Call 'POST' '/api/announcement' $adm @{ title = 'SYSTEM TEST NOTICE'; content = 'created by smoke test'; status = 1 }
Check 'admin creates announcement' ($created.code -eq 200) ($created | ConvertTo-Json -Compress)
$aid = $created.data
if (-not $aid) { $p = Call 'GET' '/api/announcement/page?pageNum=1&pageSize=5&keyword=SYSTEM TEST' $adm; $aid = @($p.data.records)[0].id }
Write-Host ("  announcement id = " + $aid)

$paged = Call 'GET' '/api/announcement/page?pageNum=1&pageSize=5&keyword=SYSTEM' $adm
Check 'admin page filter by keyword' ($paged.code -eq 200 -and $paged.data.total -ge 1) ($paged | ConvertTo-Json -Compress)

$off = Call 'PUT' ("/api/announcement/$aid/status?status=0") $adm $null
Check 'offline announcement' ($off.code -eq 200) ($off | ConvertTo-Json -Compress)
$after = Call 'GET' '/api/announcement/list' $stu
$found = $false
foreach ($x in @($after.data)) { if ($x.id -eq $aid) { $found = $true } }
Check 'offline announcement hidden from students' (-not $found) ('still visible, list=' + $cntBefore)

$bad = Call 'PUT' ("/api/announcement/$aid/status?status=9") $adm $null
Check 'invalid announcement status rejected' ($bad.code -eq 400) ($bad | ConvertTo-Json -Compress)

$del = Call 'DELETE' ("/api/announcement/$aid") $adm $null
Check 'delete announcement' ($del.code -eq 200) ($del | ConvertTo-Json -Compress)

Write-Host '--- personal notice ---'
$my = Call 'GET' '/api/notice/my?pageNum=1&pageSize=5' $stu
Check 'student notice page' ($my.code -eq 200) ($my | ConvertTo-Json -Compress)
$unread0 = Call 'GET' '/api/notice/unread-count' $stu
Write-Host ("  unread before = " + $unread0.data.count)

$first = $null
foreach ($r in @($my.data.records)) { if ($r.isRead -eq 0) { $first = $r; break } }
if ($first) {
    $rd = Call 'PUT' ("/api/notice/" + $first.id + "/read") $stu $null
    Check 'mark one notice read' ($rd.code -eq 200) ($rd | ConvertTo-Json -Compress)
    $unread1 = Call 'GET' '/api/notice/unread-count' $stu
    Check 'unread count dropped' ($unread1.data.count -lt $unread0.data.count) ("before=" + $unread0.data.count + " after=" + $unread1.data.count)
} else {
    Write-Host '  (no unread notice, skip single read)'
}

$all = Call 'PUT' '/api/notice/read-all' $stu $null
Check 'mark all read' ($all.code -eq 200) ($all | ConvertTo-Json -Compress)
$unread2 = Call 'GET' '/api/notice/unread-count' $stu
Check 'unread is zero after read-all' ($unread2.data.count -eq 0) ("count=" + $unread2.data.count)

$other = Call 'PUT' '/api/notice/999999/read' $stu $null
Check 'read someone else notice -> 404' ($other.code -eq 404) ($other | ConvertTo-Json -Compress)

Write-Host '--- sys config ---'
$cfg = Call 'GET' '/api/config/list' $adm
Check 'admin config list' ($cfg.code -eq 200 -and @($cfg.data).Count -ge 4) ($cfg | ConvertTo-Json -Compress)
$violation = $null
foreach ($c in @($cfg.data)) { if ($c.configKey -eq 'violation_limit') { $violation = $c } }
Check 'violation_limit exists' ($violation -ne $null) 'key not found'

$stuCfg = Call 'GET' '/api/config/list' $stu
Check 'student cannot read config (403)' ($stuCfg.code -eq 403) ("code=" + $stuCfg.code)

$upd = Call 'PUT' ("/api/config/" + $violation.id) $adm @{ configValue = '3' }
Check 'update violation_limit=3' ($upd.code -eq 200) ($upd | ConvertTo-Json -Compress)
$notInt = Call 'PUT' ("/api/config/" + $violation.id) $adm @{ configValue = 'abc' }
Check 'non-integer rejected' ($notInt.code -eq 400) ($notInt | ConvertTo-Json -Compress)
$zero = Call 'PUT' ("/api/config/" + $violation.id) $adm @{ configValue = '0' }
Check 'zero rejected' ($zero.code -eq 400) ($zero | ConvertTo-Json -Compress)
$restore = Call 'PUT' ("/api/config/" + $violation.id) $adm @{ configValue = '2' }
Check 'restore violation_limit=2' ($restore.code -eq 200) ($restore | ConvertTo-Json -Compress)

Write-Host '--- user center ---'
$prof = Call 'PUT' '/api/user/profile' $stu @{ realName = 'Smoke Student'; phone = '13800001111'; email = 'smoke@test.com'; avatar = '' }
Check 'update profile' ($prof.code -eq 200) ($prof | ConvertTo-Json -Compress)
$info = Call 'GET' '/api/user/info' $stu
Check 'profile reflected in info' ($info.data.realName -eq 'Smoke Student' -and $info.data.phone -eq '13800001111') ($info | ConvertTo-Json -Compress)

$badPhone = Call 'PUT' '/api/user/profile' $stu @{ realName = 'Smoke Student'; phone = '123' }
Check 'invalid phone rejected' ($badPhone.code -eq 400) ($badPhone | ConvertTo-Json -Compress)

$badPwd = Call 'PUT' '/api/user/password' $stu @{ oldPassword = 'wrong'; newPassword = '654321' }
Check 'wrong old password rejected' ($badPwd.code -eq 400) ($badPwd | ConvertTo-Json -Compress)
$samePwd = Call 'PUT' '/api/user/password' $stu @{ oldPassword = '123456'; newPassword = '123456' }
Check 'same password rejected' ($samePwd.code -eq 400) ($samePwd | ConvertTo-Json -Compress)
$okPwd = Call 'PUT' '/api/user/password' $stu @{ oldPassword = '123456'; newPassword = '654321' }
Check 'change password' ($okPwd.code -eq 200) ($okPwd | ConvertTo-Json -Compress)
$newLogin = Login '2023000201' '654321'
Check 'login with new password' ($newLogin -ne $null) 'login failed'
$backPwd = Call 'PUT' '/api/user/password' $newLogin @{ oldPassword = '654321'; newPassword = '123456' }
Check 'restore password 123456' ($backPwd.code -eq 200) ($backPwd | ConvertTo-Json -Compress)

Write-Host '--- admin user management ---'
$page = Call 'GET' '/api/user/page?pageNum=1&pageSize=5' $adm
Check 'admin user page' ($page.code -eq 200 -and $page.data.total -gt 0) ($page | ConvertTo-Json -Compress)
$kw = Call 'GET' '/api/user/page?pageNum=1&pageSize=5&keyword=2023000202' $adm
Check 'keyword search by username' ($kw.code -eq 200 -and $kw.data.total -ge 1) ($kw | ConvertTo-Json -Compress)
$roleFilter = Call 'GET' '/api/user/page?pageNum=1&pageSize=10&role=ADMIN' $adm
Check 'role filter' ($roleFilter.code -eq 200 -and $roleFilter.data.total -ge 1) ($roleFilter | ConvertTo-Json -Compress)

$stuPage = Call 'GET' '/api/user/page?pageNum=1&pageSize=5' $stu
Check 'student cannot list users (403)' ($stuPage.code -eq 403) ("code=" + $stuPage.code)

$targetId = $null
foreach ($u in @($kw.data.records)) { if ($u.username -eq '2023000202') { $targetId = $u.id } }
Write-Host ("  target user id = " + $targetId)

$disable = Call 'PUT' ("/api/user/$targetId/status?status=0") $adm $null
Check 'disable user' ($disable.code -eq 200) ($disable | ConvertTo-Json -Compress)
$disabledLogin = Login '2023000202' '123456'
Check 'disabled user cannot login' ($disabledLogin -eq $null) 'login succeeded unexpectedly'
$enable = Call 'PUT' ("/api/user/$targetId/status?status=1") $adm $null
Check 'enable user' ($enable.code -eq 200) ($enable | ConvertTo-Json -Compress)

$toOrg = Call 'PUT' ("/api/user/$targetId/role?role=ORGANIZER") $adm $null
Check 'promote to organizer' ($toOrg.code -eq 200) ($toOrg | ConvertTo-Json -Compress)
$orgLogin = Login '2023000202' '123456'
$orgInfo = Call 'GET' '/api/user/info' $orgLogin
Check 'role changed visible' ($orgInfo.data.role -eq 'ORGANIZER') ($orgInfo | ConvertTo-Json -Compress)
$toStu = Call 'PUT' ("/api/user/$targetId/role?role=STUDENT") $adm $null
Check 'demote back to student' ($toStu.code -eq 200) ($toStu | ConvertTo-Json -Compress)
$badRole = Call 'PUT' ("/api/user/$targetId/role?role=SUPER") $adm $null
Check 'invalid role rejected' ($badRole.code -eq 400) ($badRole | ConvertTo-Json -Compress)

$reset = Call 'PUT' ("/api/user/$targetId/reset-password") $adm $null
Check 'reset password' ($reset.code -eq 200) ($reset | ConvertTo-Json -Compress)
$resetLogin = Login '2023000202' '123456'
Check 'login after reset' ($resetLogin -ne $null) 'login failed'

$admInfo = Call 'GET' '/api/user/info' $adm
$selfDisable = Call 'PUT' ("/api/user/" + $admInfo.data.id + "/status?status=0") $adm $null
Check 'admin cannot disable self' ($selfDisable.code -eq 400) ($selfDisable | ConvertTo-Json -Compress)
$selfRole = Call 'PUT' ("/api/user/" + $admInfo.data.id + "/role?role=STUDENT") $adm $null
Check 'admin cannot change own role' ($selfRole.code -eq 400) ($selfRole | ConvertTo-Json -Compress)

Write-Host ''
Write-Host ("RESULT: pass=" + $script:pass + " fail=" + $script:fail)
