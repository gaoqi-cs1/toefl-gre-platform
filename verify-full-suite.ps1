param(
  [string]$baseUrl = "http://localhost:8081",
  [switch]$KeepRecord
)
$ErrorActionPreference = "Stop"
$script:fail = 0

function Check([string]$name, $cond, $detail) {
  if ($cond) { "  [PASS] $name" } else { "  [FAIL] $name :: $detail"; $script:fail++ }
}
function Post-Json([string]$url, $obj) {
  $json = $obj | ConvertTo-Json -Depth 8 -Compress
  $bytes = [Text.Encoding]::UTF8.GetBytes($json)
  Invoke-RestMethod -Method Post -Uri $url -Body $bytes -ContentType "application/json; charset=utf-8"
}

"== 1. 套题目录 =="
$list = (Invoke-RestMethod "$baseUrl/api/fullSuite/list").data
Check "目录至少有 2 套题（含用户随机生成的）" ($list.Count -ge 2) "count=$($list.Count)"
Check "套题1 阅读8题" ($list[0].readingCount -eq 8) "reading=$($list[0].readingCount)"
Check "套题1 听力5题" ($list[0].listeningCount -eq 5) "listening=$($list[0].listeningCount)"
Check "套题1 口语2题" ($list[0].speakingCount -eq 2) "speaking=$($list[0].speakingCount)"
Check "套题1 写作2题" ($list[0].writingCount -eq 2) "writing=$($list[0].writingCount)"

"== 2. 套题详情（含脱敏校验） =="
$detail = (Invoke-RestMethod "$baseUrl/api/fullSuite/1").data
Check "4 个模块" ($detail.modules.Count -eq 4) "modules=$($detail.modules.Count)"
$mods = @{}
foreach ($m in $detail.modules) { $mods[$m.moduleType] = $m }
$q0 = $mods["reading"].sections[0].questions[0]
Check "题目不下发 answer" ($null -eq $q0.answer) "answer 泄露"
Check "题目不下发 analysis" ($null -eq $q0.analysis) "analysis 泄露"
Check "阅读正文已下发" ($mods["reading"].sections[0].contentText.Length -gt 400) "正文太短"
Check "听力原文不下发" ($null -eq $mods["listening"].sections[0].contentText) "听力原文泄露"
Check "听力音频地址" ($mods["listening"].sections[0].audioUrl -like "*/audio/*") "$($mods["listening"].sections[0].audioUrl)"
Check "口语题面存在" ($mods["speaking"].sections[0].questions[0].prompt.Length -gt 50) "prompt 缺失"
Check "写作限时 1200s" ($mods["writing"].sections[0].questions[0].limitSecond -eq 1200) "$($mods["writing"].sections[0].questions[0].limitSecond)"

"== 3. 开始模考 =="
$rec = (Post-Json "$baseUrl/api/fullExam/start" @{ suiteId = 1; userId = 1 }).data
Check "拿到记录id" ($rec.id -gt 0) "recordId=$($rec.id)"
Check "状态 doing" ($rec.status -eq "doing") "$($rec.status)"
$rid = $rec.id

"== 4. 组装答案（阅读全对 / 听力对3题 / 口语+写作给文本） =="
$readingKey = @{ 1 = "B"; 2 = "C"; 3 = "B"; 4 = "B"; 5 = "B"; 6 = "D"; 7 = "C"; 8 = "A" }
$listeningKey = @{ 1 = "B"; 2 = "B"; 3 = "B"; 4 = "A"; 5 = "B" }
$speakingText = "I prefer to study in the library because the environment keeps me focused. First, other students around me are also working, so I feel pressure to stay on task. For example, last semester I tried to write an essay at home and I spent two hours cleaning my room instead. Second, the library has reference books and a quiet reading room, and when I need a source I can find it in five minutes. Therefore, although home is more comfortable, the library is more efficient for deep work."
$writingText = "The lecture challenges the reading in three ways. First, the reading claims that small parks take up space that could house many families. The lecture replies that high density buildings need ventilation and shade, and that removing parks raises summer temperatures in the whole district, which increases cooling costs for every resident. Second, the reading argues that residents prefer apartments because they are cheaper. The professor points out that the construction cost per unit in a dense block is higher, so the rent is not necessarily lower, and families with children still need open space within walking distance. Third, the reading says that the city can build a larger central park instead. The lecture answers that a single large park is far from most homes, and that daily use depends on small parks that people can reach in five minutes. Therefore, the reading overestimates the benefit of density and underestimates the cost of losing local green space, and the professor concludes that cities should improve small parks instead of replacing them."

$answers = New-Object System.Collections.ArrayList
foreach ($m in $detail.modules) {
  foreach ($s in $m.sections) {
    foreach ($q in $s.questions) {
      switch ($q.moduleType) {
        "reading" { [void]$answers.Add(@{ questionId = $q.id; userAnswer = $readingKey[[int]$q.sortIndex] }) }
        "listening" {
          $letter = if ($q.sortIndex -le 3) { $listeningKey[[int]$q.sortIndex] } else { "C" }
          [void]$answers.Add(@{ questionId = $q.id; userAnswer = $letter })
        }
        "speaking" { [void]$answers.Add(@{ questionId = $q.id; userAnswer = $speakingText }) }
        "writing" { [void]$answers.Add(@{ questionId = $q.id; userAnswer = $writingText }) }
      }
    }
  }
}
"  答案条数 = $($answers.Count)"
Check "答案条数 17（套题1 共 8+5+2+2）" ($answers.Count -eq 17) "$($answers.Count)"

"== 5. 一次性提交判分 =="
$sub = (Post-Json "$baseUrl/api/fullExam/submit" @{ recordId = $rid; costSeconds = 5400; answers = $answers }).data
Check "阅读 8/8" ($sub.readingCorrect -eq 8 -and $sub.readingTotal -eq 8) "$($sub.readingCorrect)/$($sub.readingTotal)"
Check "听力 3/5" ($sub.listeningCorrect -eq 3 -and $sub.listeningTotal -eq 5) "$($sub.listeningCorrect)/$($sub.listeningTotal)"
Check "阅读档 6.0" ($sub.readingBand -eq 6.0) "$($sub.readingBand)"
Check "听力档 4.0" ($sub.listeningBand -eq 4.0) "$($sub.listeningBand)"
Check "口语档 1-6" ([double]$sub.speakingBand -ge 1 -and [double]$sub.speakingBand -le 6) "$($sub.speakingBand)"
Check "写作档 1-6" ([double]$sub.writingBand -ge 1 -and [double]$sub.writingBand -le 6) "$($sub.writingBand)"
Check "总分 = 四项平均" ([math]::Abs([double]$sub.totalBand - ((([double]$sub.readingBand + [double]$sub.listeningBand + [double]$sub.speakingBand + [double]$sub.writingBand) / 4))) -le 0.26)
Check "兼容分 0-120" ($sub.legacyScore -ge 0 -and $sub.legacyScore -le 120) "$($sub.legacyScore)"
Check "口语反馈 2 条" ($sub.speakingFeedback.Count -eq 2) "$($sub.speakingFeedback.Count)"
Check "写作反馈 2 条" ($sub.writingFeedback.Count -eq 2) "$($sub.writingFeedback.Count)"
$fb = $sub.writingFeedback[0].feedback
Check "写作反馈有 4 个维度" ($fb.dimensions.Count -eq 4) "$($fb.dimensions.Count)"
Check "写作反馈有强弱点" (($fb.strengths.Count + $fb.improvements.Count) -gt 0) "空"
"  分档：阅读 $($sub.readingBand) / 听力 $($sub.listeningBand) / 口语 $($sub.speakingBand) / 写作 $($sub.writingBand) / 总分 $($sub.totalBand) / 兼容分 $($sub.legacyScore)"

"== 6. 成绩详情 =="
$rd = (Invoke-RestMethod "$baseUrl/api/fullExam/record/$rid").data
Check "状态 finished" ($rd.record.status -eq "finished") "$($rd.record.status)"
Check "用时 5400s" ($rd.record.costSeconds -eq 5400) "$($rd.record.costSeconds)"
$rw = ($rd.modules | Where-Object { $_.moduleType -eq "reading" }).answers[0]
Check "客观题有正确答案" ($rw.correctAnswer -ne $null -and $rw.correctAnswer.Length -gt 0) "缺失"
Check "客观题有解析" ($rw.analysis.Length -gt 10) "缺失"
Check "客观题判对" ($rw.isCorrect -eq 1) "$($rw.isCorrect)"
$ww = ($rd.modules | Where-Object { $_.moduleType -eq "writing" }).answers[0]
Check "主观题有 AI 反馈对象" ($null -ne $ww.aiFeedback.dimensions) "aiFeedback 未解析成对象"
Check "听力原文在成绩页可见" ((($rd.sections | Where-Object { $_.moduleType -eq "listening" })[0].contentText).Length -gt 100) "缺失"
Check "成绩页含 CEFR" ($rd.cefr.Length -gt 0) "$($rd.cefr)"

"== 7. 记录列表 =="
$rl = (Invoke-RestMethod "$baseUrl/api/fullExam/records?userId=1").data
$matched = @($rl | Where-Object { [long]$_.id -eq [long]$rid })
Check "列表含本次记录" ($matched.Count -eq 1) "matched=$($matched.Count) total=$($rl.Count) rid=$rid"

"== 8. 空答案提交（未作答模块按 1.0 档兜底） =="
$rec2 = (Post-Json "$baseUrl/api/fullExam/start" @{ suiteId = 2; userId = 1 }).data
$sub2 = (Post-Json "$baseUrl/api/fullExam/submit" @{ recordId = $rec2.id; answers = @() }).data
Check "空卷口语 1.0" ([double]$sub2.speakingBand -eq 1.0) "$($sub2.speakingBand)"
Check "空卷写作 1.0" ([double]$sub2.writingBand -eq 1.0) "$($sub2.writingBand)"
Check "空卷总分 1.0" ([double]$sub2.totalBand -eq 1.0) "$($sub2.totalBand)"

"== 9. 清理测试数据 =="
if (-not $KeepRecord) {
  Invoke-RestMethod -Method Delete -Uri "$baseUrl/api/fullExam/record/$rid" | Out-Null
  Invoke-RestMethod -Method Delete -Uri "$baseUrl/api/fullExam/record/$($rec2.id)" | Out-Null
  $after = (Invoke-RestMethod "$baseUrl/api/fullExam/records?userId=1").data
  $left = ($after | Where-Object { $_.id -eq $rid }).Count
  Check "测试记录已删除" ($left -eq 0) "仍有残留"
}

""
if ($script:fail -eq 0) { "ALL CHECKS PASSED" } else { "FAILED: $($script:fail)" }
