# Contract regression after the organizer/admin gap fixes.
# ASCII-only output so it can run in a cp936 console.
param([string]$Base = 'http://127.0.0.1:8080')

$ErrorActionPreference = 'Stop'
$script:pass = 0
$script:fail = 0

function Check($name, $ok, $detail) {
    if ($ok) { $script:pass++; Write-Host ("  PASS  " + $name) }
    else { $script:fail++; Write-Host ("  FAIL  " + $name + "  -> " + $detail) }
}

function Login($u, $p) {
    $body = @{ username = $u; password = $p } | ConvertTo-Json
    $r = Invoke-RestMethod -Uri "$Base/api/auth/login" -Method Post -ContentType 'application/json' `
        -Body ([System.Text.Encoding]::UTF8.GetBytes($body))
    return $r.data.token
}

function Call($method, $path, $token, $obj) {
    $h = @{ Authorization = "Bearer $token" }
    try {
        if ($null -ne $obj) {
            $json = $obj | ConvertTo-Json -Depth 6
            return (Invoke-WebRequest -Uri "$Base$path" -Method $method -Headers $h -ContentType 'application/json' `
                -Body ([System.Text.Encoding]::UTF8.GetBytes($json)) -UseBasicParsing).Content
        }
        return (Invoke-WebRequest -Uri "$Base$path" -Method $method -Headers $h -UseBasicParsing).Content
    } catch {
        $resp = $_.Exception.Response
        if ($null -eq $resp) { return '{"code":-1,"msg":"no response"}' }
        $sr = New-Object System.IO.StreamReader($resp.GetResponseStream())
        return $sr.ReadToEnd()
    }
}

function J($method, $path, $token, $obj) {
    return (Call $method $path $token $obj) | ConvertFrom-Json
}

$org = Login '2022010102' '123456'
$adm = Login 'admin' '123456'
$s1 = Login '2023000201' '123456'
$s2 = Login '2023000202' '123456'
$s3 = Login '2023000203' '123456'
Check 'login organizer/admin/students' ($org -and $adm -and $s1 -and $s2 -and $s3) 'token missing'

Write-Host "`n[1] category /all"
$all = J 'GET' '/api/category/all' $adm $null
Check 'admin sees all categories' ($all.code -eq 200 -and $all.data.Count -ge 5) ($all.code)
$stuAll = J 'GET' '/api/category/all' $s1 $null
Check 'student forbidden on /all' ($stuAll.code -eq 403) ($stuAll.code)

Write-Host "`n[2] activity list carries auditRemark"
$page = J 'GET' '/api/activity/page?pageNum=1&pageSize=3' $s1 $null
$first = $page.data.records[0]
$hasField = ($first.PSObject.Properties.Name -contains 'auditRemark')
Check 'list item has auditRemark' ($page.code -eq 200 -and $hasField) 'field missing'

Write-Host "`n[3] pending activity can be edited"
$tomorrow = (Get-Date).AddDays(1)
$f = 'yyyy-MM-dd HH:mm:ss'
$actA = @{
    title = 'Contract Test A'; categoryId = 1; summary = 'contract'; content = 'contract';
    location = 'Room 101'; startTime = $tomorrow.ToString('yyyy-MM-dd') + ' 09:00:00';
    endTime = $tomorrow.ToString('yyyy-MM-dd') + ' 11:00:00';
    signupDeadline = $tomorrow.ToString('yyyy-MM-dd') + ' 08:00:00';
    capacity = 10; needAudit = 1; draft = $false
}
$created = J 'POST' '/api/activity' $org $actA
$actAId = $created.data
Check 'publish activity (pending)' ($created.code -eq 200) $created.code
$pendingEdit = $actA.Clone()
$pendingEdit.title = 'Contract Test A edited'
$upd = J 'PUT' "/api/activity/$actAId" $org $pendingEdit
Check 'edit while pending allowed' ($upd.code -eq 200) $upd.msg
$audit = J 'PUT' "/api/activity/$actAId/audit" $adm @{ pass = $true }
Check 'admin audit pass' ($audit.code -eq 200) $audit.msg

Write-Host "`n[4] registration audit with reason"
$reg = J 'POST' "/api/registration/$actAId" $s1 $null
Check 'student signup goes pending' ($reg.code -eq 200) $reg.msg
$beforeUnread = (J 'GET' '/api/notice/unread-count' $s1 $null).data.count
$members = J 'GET' "/api/registration/activity/${actAId}?pageNum=1&pageSize=10" $org $null
$regId = $members.data.records[0].id
$rej = J 'PUT' "/api/registration/$regId/audit" $org @{ pass = $false; reason = 'quota full, please join next time' }
Check 'audit reject with reason' ($rej.code -eq 200) $rej.msg
$afterUnread = (J 'GET' '/api/notice/unread-count' $s1 $null).data.count
Check 'reject reason notifies student' ($afterUnread -gt $beforeUnread) "$beforeUnread -> $afterUnread"

Write-Host "`n[5] finish returns absent count as data"
$actB = @{
    title = 'Contract Test B'; categoryId = 1; summary = 'b'; content = 'b';
    location = 'Room 102'; startTime = $tomorrow.ToString('yyyy-MM-dd') + ' 14:00:00';
    endTime = $tomorrow.ToString('yyyy-MM-dd') + ' 16:00:00';
    signupDeadline = $tomorrow.ToString('yyyy-MM-dd') + ' 13:00:00';
    capacity = 5; needAudit = 0; draft = $false
}
$actBId = (J 'POST' '/api/activity' $org $actB).data
J 'PUT' "/api/activity/$actBId/audit" $adm @{ pass = $true } | Out-Null
J 'POST' "/api/registration/$actBId" $s1 $null | Out-Null
J 'POST' "/api/registration/$actBId" $s2 $null | Out-Null
$fin = J 'POST' "/api/checkin/$actBId/finish" $org $null
Check 'finish returns absent count' ($fin.code -eq 200 -and $fin.data -eq 2) ("code=" + $fin.code + " data=" + $fin.data)

Write-Host "`n[6] my registrations filter by activityStatus"
$my = J 'GET' '/api/registration/my?pageNum=1&pageSize=50&activityStatus=5' $s1 $null
$allFinished = $true
if ($my.data.records.Count -gt 0) {
    foreach ($r in $my.data.records) { if ($r.activityStatus -ne 5) { $allFinished = $false } }
}
Check 'activityStatus=5 filter works' ($my.code -eq 200 -and $allFinished) 'mixed statuses'
$myAll = J 'GET' '/api/registration/my?pageNum=1&pageSize=5' $s1 $null
$hasReviewed = ($myAll.data.records[0].PSObject.Properties.Name -contains 'reviewed')
Check 'my registration has reviewed flag' $hasReviewed 'field missing'

Write-Host "`n[7] checkin list keyword matches studentNo"
$list = J 'GET' "/api/checkin/${actBId}/list?pageNum=1&pageSize=10" $org $null
$studentNo = $list.data.records[0].studentNo
$filtered = J 'GET' "/api/checkin/${actBId}/list?pageNum=1&pageSize=10&keyword=$studentNo" $org $null
Check 'keyword by studentNo returns 1' ($filtered.data.records.Count -eq 1) ("count=" + $filtered.data.records.Count)
$stat = J 'GET' "/api/checkin/$actBId/stat" $org $null
$hasCancel = ($stat.data.PSObject.Properties.Name -contains 'cancelCount')
Check 'stat has cancelCount' $hasCancel 'field missing'

Write-Host "`n===== $script:pass passed, $script:fail failed ====="
if ($script:fail -gt 0) { exit 1 }
