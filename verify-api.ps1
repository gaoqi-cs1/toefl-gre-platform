# 接口自检脚本：默认打 http://localhost:8080，可传 -baseUrl 指向别的实例
param([string]$baseUrl = 'http://localhost:8080')
$b = $baseUrl
$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [Text.Encoding]::UTF8
function J($o) { $o | ConvertTo-Json -Depth 8 -Compress }

$fail = @()
function Check($name, $cond, $detail) {
  if ($cond) { Write-Output ("PASS  " + $name) }
  else { Write-Output ("FAIL  " + $name + "  " + $detail); $script:fail += $name }
}

# 1. 登录
$login = Invoke-RestMethod -Uri "$b/api/user/login" -Method Post -ContentType 'application/json' -Body '{"username":"root","password":"123456"}'
Check '登录 root/123456' ($login.code -eq 200 -and $login.data.token) (J $login)
$hdr = @{ Authorization = 'Bearer ' + $login.data.token }

# 2. 用户信息
$info = Invoke-RestMethod -Uri "$b/api/user/info" -Headers $hdr
Check 'GET /api/user/info' ($info.code -eq 200 -and $info.data.username -eq 'root') (J $info)

# 3. 用户列表
$ulist = Invoke-RestMethod -Uri "$b/api/user/list" -Headers $hdr
Check 'GET /api/user/list' ($ulist.code -eq 200 -and $ulist.data.Count -ge 2) (J $ulist)

# 4. 篇章列表（TpoList 依赖）
$secs = Invoke-RestMethod -Uri "$b/api/section/list"
$types = $secs.data | ForEach-Object { $_.type }
Check 'GET /api/section/list 四模块齐全' (($types -contains 'reading') -and ($types -contains 'listening') -and ($types -contains 'speaking') -and ($types -contains 'writing')) (J $secs)

# 5. 题目接口两种形态
$q1 = Invoke-RestMethod -Uri "$b/api/question/list?sectionId=1"
$q2 = Invoke-RestMethod -Uri "$b/api/question/list/1"
Check 'GET /api/question/list?sectionId=1' ($q1.code -eq 200 -and $q1.data.Count -eq 6) (J $q1)
Check 'GET /api/question/list/1' ($q2.code -eq 200 -and $q2.data.Count -eq 6) (J $q2)

$first = $q1.data[0]

# 6. 单题提交（对 / 错）
$ok = Invoke-RestMethod -Uri "$b/api/answer/submit" -Method Post -ContentType 'application/json' -Headers $hdr -Body (@{ userId = 1; questionId = $first.id; sectionId = 1; userAnswer = $first.answer } | ConvertTo-Json)
Check '作答正确 -> correct=true' ($ok.code -eq 200 -and $ok.data.correct -eq $true) (J $ok)
$bad = Invoke-RestMethod -Uri "$b/api/answer/submit" -Method Post -ContentType 'application/json' -Headers $hdr -Body (@{ userId = 1; questionId = $first.id; sectionId = 1; userAnswer = 'C' } | ConvertTo-Json)
Check '作答错误 -> correct=false 且带正确答案' ($bad.code -eq 200 -and $bad.data.correct -eq $false -and $bad.data.correctAnswer) (J $bad)

# 7. 作答列表
$al = Invoke-RestMethod -Uri "$b/api/answer/list?userId=1"
Check 'GET /api/answer/list' ($al.code -eq 200 -and $al.data.Count -ge 1) ("count=" + $al.data.Count)

# 8. 错题本
$add = Invoke-RestMethod -Uri "$b/api/wrong/add" -Method Post -ContentType 'application/json' -Headers $hdr -Body (@{ userId = 1; questionId = $first.id; sectionId = 1 } | ConvertTo-Json)
Check 'POST /api/wrong/add' ($add.code -eq 200) (J $add)
$wl = Invoke-RestMethod -Uri "$b/api/wrong/list?userId=1"
$item = $wl.data | Where-Object { $_.questionId -eq $first.id } | Select-Object -First 1
Check 'GET /api/wrong/list 带题干/选项/我的答案' ($wl.code -eq 200 -and $item -and $item.questionTitle -and $item.optionA -and $item.correctAnswer -and $item.sectionType -eq 'reading') (J $item)
if ($item) {
  $del = Invoke-RestMethod -Uri "$b/api/wrong/del?id=$($item.id)" -Method Delete -Headers $hdr
  Check 'DELETE /api/wrong/del?id=' ($del.code -eq 200) (J $del)
}

# 9. 套题列表 + 详情
$sl = Invoke-RestMethod -Uri "$b/api/suite/list"
Check 'GET /api/suite/list' ($sl.code -eq 200 -and $sl.data.Count -ge 1) ("count=" + $sl.data.Count)
$sd = Invoke-RestMethod -Uri "$b/api/suite/1"
$secList = $sd.data.sectionList
$allExist = ($secList | Where-Object { $_.exist -eq $false }).Count -eq 0
Check 'GET /api/suite/1 关联篇章全部存在' ($sd.code -eq 200 -and $secList.Count -eq 4 -and $allExist) (J $sd)

# 10. 模考 start -> submit -> list -> detail -> 成绩页
$start = Invoke-RestMethod -Uri "$b/api/examRecord/start" -Method Post -ContentType 'application/json' -Headers $hdr -Body '{"suiteId":1}'
Check 'POST /api/examRecord/start' ($start.code -eq 200 -and $start.data.id -and $start.data.examStatus -eq 'doing') (J $start)
$rid = $start.data.id

$readQs = $q1.data
$readAns = @()
foreach ($q in $readQs) { $readAns += @{ questionId = $q.id; userAns = $q.answer } }   # 全对
$listQs = (Invoke-RestMethod -Uri "$b/api/question/list?sectionId=7").data
$listAns = @()
foreach ($q in $listQs) { $listAns += @{ questionId = $q.id; userAns = 'A' } }            # 第一个 A 对，其余可能错
$speakQs = (Invoke-RestMethod -Uri "$b/api/question/list?sectionId=4").data
$speakAns = @()
foreach ($q in $speakQs) { $speakAns += @{ questionId = $q.id; content = '这是自动化测试的口语作答内容' } }
$writeQs = (Invoke-RestMethod -Uri "$b/api/question/list?sectionId=5").data
$writeAns = @()
foreach ($q in $writeQs) { $writeAns += @{ questionId = $q.id; content = 'This is an automated test essay.' } }

$payload = @{
  recordId            = $rid
  readingAnswerList   = $readAns
  listeningAnswerList = $listAns
  speakingAnswerList  = $speakAns
  writingAnswerList   = $writeAns
} | ConvertTo-Json -Depth 6
$sub = Invoke-RestMethod -Uri "$b/api/examRecord/submit" -Method Post -ContentType 'application/json' -Headers $hdr -Body $payload
Check 'POST /api/examRecord/submit 判分' ($sub.code -eq 200 -and $sub.data.readScore -gt 0 -and $sub.data.totalScore -gt 0) (J $sub)
Check '阅读 6/6 全对 -> 阅读分满档' ($sub.data.readingCorrect -eq 6 -and $sub.data.readingTotal -eq 6) (J $sub)

$one = Invoke-RestMethod -Uri "$b/api/examRecord/$rid" -Headers $hdr
Check 'GET /api/examRecord/{id} 回写分项分数' ($one.code -eq 200 -and $one.data.examStatus -eq 'finished' -and $one.data.readScore -eq $sub.data.readScore -and $one.data.endTime) (J $one)

$detail = Invoke-RestMethod -Uri "$b/api/examRecord/detail?recordId=$rid" -Headers $hdr
Check 'GET /api/examRecord/detail 含 record+answers+选项' ($detail.code -eq 200 -and $detail.data.record.id -eq $rid -and $detail.data.answers.Count -gt 0 -and $detail.data.answers[0].optionA -ne $null) ("answers=" + $detail.data.answers.Count)

$rl = Invoke-RestMethod -Uri "$b/api/examRecord/list" -Headers $hdr
Check 'GET /api/examRecord/list' ($rl.code -eq 200 -and $rl.data.Count -ge 1) ("count=" + $rl.data.Count)

# 11. 套题一键生成（只取有题篇章）
$init = Invoke-RestMethod -Uri "$b/api/suite/init" -Method Post -Headers $hdr
Check 'POST /api/suite/init' ($init.code -eq 200) (J $init)
if ($init.code -eq 200) {
  $newSuiteId = [regex]::Match([string]$init.data, '\d+').Value
  $newDetail = Invoke-RestMethod -Uri "$b/api/suite/$newSuiteId"
  $empty = ($newDetail.data.sectionList | Where-Object { $_.questionCount -le 0 }).Count
  Check '新套题四个模块都有题目' ($newDetail.data.sectionList.Count -eq 4 -and $empty -eq 0) (J $newDetail.data.sectionList)
}

Write-Output ''
if ($fail.Count -eq 0) { Write-Output 'ALL CHECKS PASSED' } else { Write-Output ('FAILED: ' + ($fail -join ' | ')) }
