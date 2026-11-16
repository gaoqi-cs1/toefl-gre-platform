<template>
  <div class="exam-page">
    <header class="exam-header">
      <div class="header-left">
        <span class="title">整卷结果</span>
      </div>
      <div class="header-right">
        <button class="btn ghost" @click="$router.push('/suite')">返回套题列表</button>
      </div>
    </header>

    <div v-if="record" class="score-card">
      <div class="score-main">
        <div class="score-num">{{ record.correctCount }}<span class="score-total">/{{ record.totalCount }}</span></div>
        <div class="score-label">客观题答对</div>
      </div>
      <div class="score-detail">
        <div class="detail-item">
          <span class="d-name">阅读</span>
          <span class="d-val">{{ record.readingCorrect }} / {{ record.readingTotal }}</span>
        </div>
        <div class="detail-item">
          <span class="d-name">听力</span>
          <span class="d-val">{{ record.listeningCorrect }} / {{ record.listeningTotal }}</span>
        </div>
        <div class="detail-item">
          <span class="d-name">口语已答</span>
          <span class="d-val">{{ record.speakingCount }} 题</span>
        </div>
        <div class="detail-item">
          <span class="d-name">写作已答</span>
          <span class="d-val">{{ record.writingCount }} 题</span>
        </div>
        <div class="detail-item">
          <span class="d-name">整卷用时</span>
          <span class="d-val">{{ costText }}</span>
        </div>
      </div>
    </div>

    <div class="answer-wrap">
      <div v-for="group in grouped" :key="group.type" class="group">
        <div class="group-head">
          <span class="dot-mark" :class="group.type"></span>
          <span class="group-name">{{ group.name }}</span>
          <span class="group-num">{{ group.list.length }} 题</span>
        </div>

        <div v-for="a in group.list" :key="a.questionId" class="a-card"
             :class="{ ok: a.isCorrect === 1, bad: a.isCorrect !== 1 }">
          <div class="a-head">
            <span class="a-title">{{ a.questionTitle }}</span>
            <span class="a-state">{{ a.isCorrect === 1 ? '答对' : '答错' }}</span>
          </div>

          <ul v-if="hasOption(a)" class="option-list">
            <li v-for="opt in buildOption(a)" :key="opt.key" class="option-item"
                :class="{ correct: opt.key === a.correctAnswer, mine: opt.key === a.userAnswer }">
              <span class="opt-key">{{ opt.key }}</span>
              <span class="opt-text">{{ opt.text || '暂无内容' }}</span>
              <span v-if="opt.key === a.correctAnswer" class="mark correct-mark">正确答案</span>
              <span v-if="opt.key === a.userAnswer" class="mark mine-mark">我的答案</span>
            </li>
          </ul>

          <div class="answer-line">
            <span>我的答案：<b>{{ a.userAnswer || '空' }}</b></span>
            <span>正确答案：<b class="right">{{ a.correctAnswer || '暂无' }}</b></span>
          </div>

          <div v-if="a.analysis" class="analysis">解析：{{ a.analysis }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
// 统一走封装 service；接口也从原来后端不存在的 /api/suite/record/detail
// 换成真实存在的 /api/examRecord/detail（返回 { record, answers }）
import service from '@/utils/request'

const route = useRoute()
const recordId = ref(route.query.recordId)
const record = ref(null)
const answers = ref([])

const costText = computed(() => {
  const r = record.value
  if (!r) return '--'
  let s = Number(r.costSeconds) || 0
  if (!s && r.startTime && r.endTime) {
    const st = new Date(String(r.startTime).replace(' ', 'T'))
    const et = new Date(String(r.endTime).replace(' ', 'T'))
    const diff = Math.floor((et - st) / 1000)
    if (!isNaN(diff) && diff > 0) s = diff
  }
  if (!s) return '--'
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  const sec = s % 60
  return h > 0 ? `${h}小时${m}分` : `${m}分${sec}秒`
})

const nameOf = (type) => {
  if (type === 'reading') return '阅读 Reading'
  if (type === 'listening') return '听力 Listening'
  if (type === 'speaking') return '口语 Speaking'
  if (type === 'writing') return '写作 Writing'
  return type
}

const grouped = computed(() => {
  const types = ['reading', 'listening', 'speaking', 'writing']
  return types
    .map(t => ({ type: t, name: nameOf(t), list: answers.value.filter(a => a.moduleType === t) }))
    .filter(g => g.list.length > 0)
})

const hasOption = (a) => !!(a.optionA || a.optionB || a.optionC || a.optionD)
const buildOption = (a) => [
  { key: 'A', text: a.optionA },
  { key: 'B', text: a.optionB },
  { key: 'C', text: a.optionC },
  { key: 'D', text: a.optionD }
]

const getDetail = async () => {
  if (!recordId.value) {
    ElMessage.warning('缺少记录编号')
    return
  }
  try {
    const res = await service.get('/api/examRecord/detail', { params: { recordId: recordId.value } })
    if (res.code !== 200) {
      ElMessage.error(res.msg || '加载结果失败')
      return
    }
    record.value = res.data.record
    answers.value = res.data.answers || []
  } catch (err) {
    ElMessage.error('加载结果失败：' + (err?.response?.data?.msg || '网络异常'))
    console.error(err)
  }
}

onMounted(getDetail)
</script>


<style scoped>
.exam-page { min-height: 100vh; background: #f4f6fb; padding: 18px; box-sizing: border-box; }
.exam-header {
  display: flex; justify-content: space-between; align-items: center;
  background: #fff; border-radius: 14px; padding: 12px 18px;
  box-shadow: 0 2px 12px rgba(28, 50, 92, 0.06); margin-bottom: 16px;
}
.title { font-size: 16px; font-weight: 600; color: #1f2d3d; }
.btn { border: none; cursor: pointer; border-radius: 9px; font-size: 13px; padding: 8px 14px; transition: all 0.18s; }
.btn.ghost { background: #f2f4f8; color: #5a6b82; }
.btn.ghost:hover { background: #e6eaf2; }

.score-card {
  background: #fff; border-radius: 14px; padding: 20px;
  box-shadow: 0 2px 12px rgba(28, 50, 92, 0.06); margin-bottom: 16px;
  display: flex; align-items: center; gap: 40px; flex-wrap: wrap;
}
.score-main { display: flex; flex-direction: column; align-items: center; }
.score-num { font-size: 40px; font-weight: 700; color: #2b6ef2; line-height: 1; }
.score-total { font-size: 18px; color: #8a99ad; font-weight: 500; }
.score-label { font-size: 12px; color: #8a99ad; margin-top: 6px; }
.score-detail { display: flex; gap: 28px; flex-wrap: wrap; }
.detail-item { display: flex; flex-direction: column; gap: 4px; }
.d-name { font-size: 12px; color: #8a99ad; }
.d-val { font-size: 15px; font-weight: 600; color: #1f2d3d; }

.answer-wrap { display: grid; grid-template-columns: repeat(auto-fit, minmax(460px, 1fr)); gap: 14px; }
.group {
  background: #fff; border-radius: 14px; padding: 16px;
  box-shadow: 0 2px 12px rgba(28, 50, 92, 0.06);
  display: flex; flex-direction: column; gap: 12px;
}
.group-head { display: flex; align-items: center; gap: 8px; padding-bottom: 8px; border-bottom: 1px solid #eef1f6; }
.dot-mark { width: 8px; height: 8px; border-radius: 50%; }
.dot-mark.reading { background: #2b6ef2; }
.dot-mark.listening { background: #14a094; }
.dot-mark.speaking { background: #ec8a1f; }
.dot-mark.writing { background: #6b5be0; }
.group-name { font-size: 14px; font-weight: 600; color: #1f2d3d; }
.group-num { margin-left: auto; font-size: 12px; color: #8a99ad; }

.a-card { border: 1px solid #eef1f6; border-radius: 10px; padding: 12px; background: #fbfcfe; }
.a-card.ok { border-color: #cdeee0; background: #f6fdfa; }
.a-card.bad { border-color: #f7dada; background: #fefafa; }
.a-head { display: flex; justify-content: space-between; gap: 10px; margin-bottom: 10px; }
.a-title { font-size: 13.5px; color: #1f2d3d; line-height: 1.6; flex: 1; }
.a-state { font-size: 11px; padding: 1px 7px; border-radius: 4px; align-self: flex-start; white-space: nowrap; }
.a-card.ok .a-state { color: #22a06b; background: #e7f8f0; }
.a-card.bad .a-state { color: #dd5252; background: #fdecec; }

.option-list { list-style: none; margin: 0 0 10px; padding: 0; }
.option-item {
  display: flex; align-items: flex-start; gap: 8px; padding: 7px 10px;
  margin-bottom: 5px; border: 1px solid #eef1f6; border-radius: 8px; background: #fff;
}
.option-item.correct { border-color: #22a06b; background: #f1fbf6; }
.option-item.mine { border-color: #f0a13a; background: #fff9f0; }
.option-item.correct.mine { border-color: #22a06b; background: #f1fbf6; }
.opt-key {
  width: 19px; height: 19px; border-radius: 5px; background: #eef2f9; color: #5a6b82;
  font-size: 12px; font-weight: 700; display: inline-flex; align-items: center;
  justify-content: center; flex: none;
}
.option-item.correct .opt-key { background: #22a06b; color: #fff; }
.option-item.mine:not(.correct) .opt-key { background: #f0a13a; color: #fff; }
.opt-text { font-size: 13px; line-height: 1.6; color: #35404f; flex: 1; }
.mark { font-size: 11px; padding: 1px 6px; border-radius: 4px; white-space: nowrap; align-self: center; }
.correct-mark { color: #22a06b; background: #e7f8f0; }
.mine-mark { color: #d98b1c; background: #fff2dd; }

.answer-line { display: flex; gap: 16px; font-size: 12.5px; color: #5a6b82; flex-wrap: wrap; }
.answer-line b { color: #1f2d3d; }
.answer-line b.right { color: #22a06b; }
.analysis { font-size: 12.5px; color: #6b7a90; line-height: 1.7; margin-top: 8px; }
</style>
