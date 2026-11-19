$ErrorActionPreference = 'Stop'
$base = 'http://127.0.0.1:8080'

function Login($u, $p) {
    $b = @{ username = $u; password = $p } | ConvertTo-Json
    (Invoke-RestMethod -Uri "$base/api/auth/login" -Method Post -ContentType 'application/json' -Body ([System.Text.Encoding]::UTF8.GetBytes($b))).data.token
}

function Call($method, $path, $token, $obj) {
    $h = @{ Authorization = "Bearer $token" }
    try {
        if ($obj) {
            $json = $obj | ConvertTo-Json
            (Invoke-WebRequest -Uri "$base$path" -Method $method -Headers $h -ContentType 'application/json' -Body ([System.Text.Encoding]::UTF8.GetBytes($json)) -UseBasicParsing).Content
        } else {
            (Invoke-WebRequest -Uri "$base$path" -Method $method -Headers $h -UseBasicParsing).Content
        }
    } catch {
        $resp = $_.Exception.Response
        $reader = New-Object System.IO.StreamReader($resp.GetResponseStream())
        $reader.ReadToEnd()
    }
}

function Show($label, $json) {
    $o = $json | ConvertFrom-Json
    Write-Host ("{0,-28} code={1} msg={2}" -f $label, $o.code, $o.msg)
    return $o
}

$org = Login '2022010102' '123456'
$adm = Login 'admin' '123456'
$s1 = Login '2023000201' '123456'
$s2 = Login '2023000202' '123456'
$s3 = Login '2023000203' '123456'

$fmt = 'yyyy-MM-dd HH:mm:ss'
$start = (Get-Date).AddMinutes(10).ToString($fmt)
$end = (Get-Date).AddHours(2).ToString($fmt)
$deadline = (Get-Date).AddMinutes(8).ToString($fmt)

# 1. 创建并发布活动
$r = Show '创建活动' (Call 'POST' '/api/activity' $org @{
    title = '评价模块测试活动'; categoryId = 3; summary = '评价测试'; content = '评价测试内容'
    location = '大学生活动中心'; startTime = $start; endTime = $end; signupDeadline = $deadline
    capacity = 20; needAudit = 0
})
$aId = $r.data

# 2. 未结束时评价，预期 400
$r = Show '未结束就评价' (Call 'POST' "/api/review/$aId" $s1 @{ score = 5; content = '太早了' })

$r = Show '审核通过' (Call 'PUT' "/api/activity/$aId/audit" $adm @{ pass = $true })

# 3. 报名
$r = Show 's1 报名' (Call 'POST' "/api/registration/$aId" $s1 $null)
$r = Show 's2 报名' (Call 'POST' "/api/registration/$aId" $s2 $null)

# 4. 生成签到码并签到
$code = (Call 'POST' "/api/checkin/$aId/code" $org $null | ConvertFrom-Json).data.code
Write-Host ("签到码: {0}" -f $code)
$r = Show 's1 签到' (Call 'POST' "/api/checkin/$aId" $s1 @{ code = $code })
$r = Show 's2 签到' (Call 'POST' "/api/checkin/$aId" $s2 @{ code = $code })

# 5. 结束活动（s3 未报名，用来测未参加）
$r = Show '结束活动' (Call 'POST' "/api/checkin/$aId/finish" $org $null)

# 6. 评价
$r = Show 's1 评价 5 分' (Call 'POST' "/api/review/$aId" $s1 @{ score = 5; content = '组织得非常好，签到也很快' })
$r = Show 's1 重复评价' (Call 'POST' "/api/review/$aId" $s1 @{ score = 4; content = '再评一次' })
$r = Show 's2 匿名评价 4 分' (Call 'POST' "/api/review/$aId" $s2 @{ score = 4; content = '现场有点吵'; anonymous = $true })
$r = Show 's3 未参加评价' (Call 'POST' "/api/review/$aId" $s3 @{ score = 5; content = '路人评价' })
$r = Show '分数越界 6 分' (Call 'POST' "/api/review/$aId" $s3 @{ score = 6; content = '越界' })

# 7. 列表 / 统计 / 我的
$list = Call 'GET' "/api/review/activity/$aId?pageNum=1&pageSize=10" $s3
Write-Host "`n--- s3 视角的评价列表 ---"
($list | ConvertFrom-Json).data.records | ForEach-Object {
    "id=$($_.id) score=$($_.score) userName=$($_.userName) userId=$($_.userId) anonymous=$($_.anonymous) mine=$($_.mine) content=$($_.content)"
}

$list = Call 'GET' "/api/review/activity/$aId?pageNum=1&pageSize=10" $s2
Write-Host "`n--- s2（本人）视角 ---"
($list | ConvertFrom-Json).data.records | ForEach-Object {
    "id=$($_.id) userName=$($_.userName) userId=$($_.userId) anonymous=$($_.anonymous) mine=$($_.mine)"
}

$list = Call 'GET' "/api/review/activity/$aId?pageNum=1&pageSize=10" $adm
Write-Host "`n--- 管理员视角 ---"
($list | ConvertFrom-Json).data.records | ForEach-Object {
    "id=$($_.id) userName=$($_.userName) anonymous=$($_.anonymous) mine=$($_.mine)"
}

$stat = (Call 'GET' "/api/review/activity/$aId/stat" $s1 | ConvertFrom-Json).data
Write-Host "`n--- 统计 ---"
"avgScore=$($stat.avgScore) total=$($stat.total) 5星=$($stat.score5) 4星=$($stat.score4) 3星=$($stat.score3) 2星=$($stat.score2) 1星=$($stat.score1)"
"myReview.id=$($stat.myReview.id) myReview.score=$($stat.myReview.score)"

$mine = (Call 'GET' '/api/review/my?pageNum=1&pageSize=10' $s2 | ConvertFrom-Json).data
Write-Host "`n--- 我的评价（s2）---"
$mine.records | ForEach-Object { "id=$($_.id) activityTitle=$($_.activityTitle) score=$($_.score) mine=$($_.mine)" }

# 8. 删除权限
$idS1 = ((Call 'GET' "/api/review/activity/$aId?pageNum=1&pageSize=10" $s1 | ConvertFrom-Json).data.records | Where-Object { $_.userId -eq 2023000201 }).id
$idS2 = ((Call 'GET' "/api/review/activity/$aId?pageNum=1&pageSize=10" $adm | ConvertFrom-Json).data.records | Where-Object { $_.anonymous -eq 1 }).id
$r = Show 's3 删他人评价' (Call 'DELETE' "/api/review/$idS1" $s3 $null)
$r = Show 's1 删自己评价' (Call 'DELETE' "/api/review/$idS1" $s1 $null)
$r = Show '管理员删匿名评价' (Call 'DELETE' "/api/review/$idS2" $adm $null)

$stat = (Call 'GET' "/api/review/activity/$aId/stat" $s1 | ConvertFrom-Json).data
Write-Host "`n--- 删除后统计 ---"
"avgScore=$($stat.avgScore) total=$($stat.total)"

# 9. 活动详情里的质量分
$d = (Call 'GET' "/api/activity/$aId" $s1 | ConvertFrom-Json).data
Write-Host "`n--- 活动详情 ---"
"joinedCount=$($d.joinedCount) checkinCount=$($d.checkinCount) reviewCount=$($d.reviewCount) avgScore=$($d.avgScore) qualityScore=$($d.qualityScore)"
Write-Host "ACTIVITY_ID=$aId"
