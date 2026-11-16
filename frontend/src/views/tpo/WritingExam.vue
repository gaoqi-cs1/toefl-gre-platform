<template>
  <div class="exam-page">
    <header class="exam-header">
      <div class="header-left">
        <span class="type-badge writing">WRITING</span>
        <span class="progress-text">第 {{ currentIndex + 1 }} / {{ questionList.length }} 题</span>
      </div>
      <div class="header-right">
        <div class="timer-box">
          <span class="timer-label">用时</span>
          <span class="timer-value">{{ timeText }}</span>
        </div>
        <button class="btn ghost" @click="toggleTimer">{{ timerRunning ? '暂停' : '开始' }}</button>
        <button class="btn soft" @click="addWrong">加入错题本</button>
        <button class="btn ghost" @click="goTpo">返回 TPO</button>
      </div>
    </header>

    <main class="exam-body single">
      <section class="panel">
        <div class="panel-head">
          <h3>Writing Task</h3>
          <div class="word-count">
            <span :class="{ ok: wordCount >= 150 }">{{ wordCount }}</span> words
          </div>
        </div>

        <div v-if="currentQuestion" class="question-inner">
          <p class="q-title">{{ currentQuestion.questionTitle }}</p>
          <div v-if="currentQuestion.analysis" class="hint-box">
            <span class="hint-tag">提示</span>{{ currentQuestion.analysis }}
          </div>

          <el-input v-model="userAnswer" type="textarea" :rows="14" resize="none"
                    placeholder="在这里撰写你的作文" class="essay-input" />

          <div v-if="result" class="result-box" :class="result.correct ? 'ok' : 'bad'">
            <div v-if="!result.answered" class="result-line">
              你未作答，参考答案为 <b>{{ result.correctAnswer }}</b>
            </div>
            <template v-else>
              <div v-if="result.correct" class="result-line">✅ 答案正确</div>
              <div v-else class="result-line">
                ❌ 与参考答案不一致，正确答案为 <b>{{ result.correctAnswer }}</b>
              </div>
              <div class="result-sub">你的答案：{{ result.userAnswer }}</div>
            </template>
          </div>
        </div>
        <div v-else class="empty">暂无写作题目数据</div>

        <div class="action-bar">
          <button class="btn ghost" :disabled="currentIndex === 0" @click="prevQuestion">上一题</button>
          <button class="btn primary" @click="submitAnswer">提交作文</button>
          <button class="btn ghost" :disabled="currentIndex >= questionList.length - 1" @click="nextQuestion">下一题</button>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
// 统一走封装 service（自动带 token、统一错误提示），原来是裸 axios
import service from '@/utils/request'

const route = useRoute()
const router = useRouter()

const sectionId = ref(null)
const jumpQid = ref(null)
const questionList = ref([])
const currentIndex = ref(0)
const userAnswer = ref('')
const result = ref(null)
const answerCache = ref({})

const readUserId = () => {
  try {
    const info = JSON.parse(localStorage.getItem('userInfo') || '{}')
    return Number(info.userId) || 1
  } catch (e) {
    return 1
  }
}
const userId = readUserId()

const timerRunning = ref(false)
const timeText = ref('00:00')
let timer = null
let second = 0

const currentQuestion = computed(() => questionList.value[currentIndex.value] || null)
const wordCount = computed(() => {
  const t = userAnswer.value.trim()
  return t ? t.split(/\s+/).length : 0
})

const toggleTimer = () => {
  timerRunning.value = !timerRunning.value
  if (timerRunning.value) {
    timer = setInterval(() => {
      second++
      timeText.value = `${String(Math.floor(second / 60)).padStart(2, '0')}:${String(second % 60).padStart(2, '0')}`
    }, 1000)
  } else {
    clearInterval(timer)
  }
}

const goTpo = () => router.push('/tpo')

const getPageData = async () => {
  if (!sectionId.value) {
    ElMessage.warning('缺少篇章编号')
    return
  }
  try {
    const [qRes, ansRes] = await Promise.all([
      service.get('/api/question/list', { params: { sectionId: sectionId.value } }),
      service.get('/api/answer/list', { params: { userId } })
    ])
    questionList.value = qRes.data || []

    const map = {}
    ;(ansRes.data || []).forEach(a => { map[a.questionId] = a })
    answerCache.value = map

    if (jumpQid.value) {
      const idx = questionList.value.findIndex(q => Number(q.id) === Number(jumpQid.value))
      currentIndex.value = idx >= 0 ? idx : 0
    } else {
      currentIndex.value = 0
    }
    loadCurrent()
  } catch (err) {
    ElMessage.error('加载写作题目失败：' + (err?.response?.data?.msg || '网络异常'))
    console.error(err)
  }
}

const loadCurrent = () => {
  if (!currentQuestion.value) return
  const saved = answerCache.value[currentQuestion.value.id]
  userAnswer.value = saved && saved.userAnswer ? saved.userAnswer : ''
  result.value = null
}

const submitAnswer = async () => {
  if (!currentQuestion.value) return
  try {
    const res = await service.post('/api/answer/submit', {
      userId,
      questionId: currentQuestion.value.id,
      sectionId: sectionId.value,
      userAnswer: userAnswer.value
    })
    if (res.code !== 200) {
      ElMessage.warning(res.msg || '提交失败')
      return
    }
    result.value = res.data
    answerCache.value[result.value.questionId] = {
      questionId: result.value.questionId,
      userAnswer: result.value.userAnswer,
      isCorrect: result.value.correct ? 1 : 0
    }

    if (!result.value.answered) {
      ElMessage.warning('你还没有写任何内容')
    } else {
      ElMessage.success('作文已提交（' + wordCount.value + ' 词），主观题由人工/AI 评分')
    }
  } catch (err) {
    ElMessage.error('提交失败：' + (err?.response?.data?.msg || '网络异常'))
    console.error(err)
  }
}

const prevQuestion = () => {
  currentIndex.value--
  loadCurrent()
}
const nextQuestion = () => {
  currentIndex.value++
  loadCurrent()
}

const addWrong = async () => {
  if (!currentQuestion.value) return
  try {
    const res = await service.post('/api/wrong/add', {
      userId,
      questionId: currentQuestion.value.id,
      sectionId: sectionId.value
    })
    if (res.code === 200) ElMessage.success('已加入错题本')
    else ElMessage.warning(res.msg)
  } catch (err) {
    ElMessage.error('加入错题本失败')
  }
}

// 只加载一次（原来 watch + onMounted 会重复请求两遍）
watch(route, async () => {
  sectionId.value = route.params.sectionId
  jumpQid.value = route.query.jumpQid
  await getPageData()
}, { immediate: true })
</script>


<style scoped>
.exam-page {
  min-height: 100vh; background: #f4f6fb; padding: 18px;
  box-sizing: border-box; font-family: -apple-system, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}
.exam-header {
  display: flex; align-items: center; justify-content: space-between;
  background: #fff; border-radius: 14px; padding: 12px 18px;
  box-shadow: 0 2px 12px rgba(28, 50, 92, 0.06); margin-bottom: 16px;
}
.header-left { display: flex; align-items: center; gap: 12px; }
.type-badge {
  font-size: 12px; letter-spacing: 1px; padding: 4px 10px;
  border-radius: 6px; font-weight: 600; color: #fff;
}
.type-badge.writing { background: linear-gradient(135deg, #8a7ef0, #6b5be0); }
.progress-text { color: #6b7a90; font-size: 14px; }
.header-right { display: flex; align-items: center; gap: 10px; }
.timer-box {
  display: flex; align-items: baseline; gap: 6px;
  background: #f2f6ff; border-radius: 8px; padding: 6px 12px;
}
.timer-label { font-size: 12px; color: #8a99ad; }
.timer-value { font-size: 18px; font-weight: 700; color: #2b6ef2; font-variant-numeric: tabular-nums; }
.btn {
  border: none; cursor: pointer; border-radius: 9px; font-size: 13px;
  padding: 8px 14px; transition: all 0.18s; font-family: inherit;
}
.btn:disabled { opacity: 0.45; cursor: not-allowed; }
.btn.primary { background: #2b6ef2; color: #fff; }
.btn.primary:hover { background: #1b5cd8; }
.btn.soft { background: #fff4e5; color: #d98b1c; }
.btn.soft:hover { background: #ffe9cc; }
.btn.ghost { background: #f2f4f8; color: #5a6b82; }
.btn.ghost:hover { background: #e6eaf2; }

.exam-body { display: grid; grid-template-columns: 1fr; gap: 16px; }
.panel {
  background: #fff; border-radius: 14px; box-shadow: 0 2px 12px rgba(28, 50, 92, 0.06);
  padding: 20px; display: flex; flex-direction: column; min-height: 74vh;
}
.panel-head {
  display: flex; align-items: center; justify-content: space-between;
  border-bottom: 1px solid #eef1f6; padding-bottom: 10px; margin-bottom: 16px;
}
.panel-head h3 { margin: 0; font-size: 15px; color: #1f2d3d; font-weight: 600; }
.word-count { font-size: 13px; color: #8a99ad; }
.word-count span { font-weight: 700; font-size: 15px; color: #b8c2d1; font-variant-numeric: tabular-nums; }
.word-count span.ok { color: #22a06b; }
.q-title { font-size: 16.5px; line-height: 1.75; color: #1f2d3d; margin: 0 0 16px; font-weight: 500; }
.hint-box {
  background: #f6f4ff; border-left: 3px solid #6b5be0; border-radius: 8px;
  padding: 11px 14px; margin-bottom: 16px; font-size: 13.5px; color: #5f5580; line-height: 1.7;
}
.hint-tag {
  display: inline-block; background: #6b5be0; color: #fff;
  font-size: 11px; padding: 1px 6px; border-radius: 4px; margin-right: 8px;
}
.essay-input :deep(.el-textarea__inner) {
  border-radius: 11px; border: 1px solid #e6ebf3; padding: 14px;
  font-size: 14.5px; line-height: 1.85; box-shadow: none;
}
.essay-input :deep(.el-textarea__inner:focus) {
  border-color: #2b6ef2; box-shadow: 0 0 0 3px rgba(43, 110, 242, 0.1);
}
.question-inner { flex: 1; display: flex; flex-direction: column; }
.result-box {
  margin-top: 14px; border-radius: 10px; padding: 12px 14px;
  border-left: 3px solid #2b6ef2; background: #f8fbff;
}
.result-box.ok { border-left-color: #22a06b; background: #f1fbf6; }
.result-box.bad { border-left-color: #e04f4f; background: #fef5f5; }
.result-line { font-size: 14px; color: #1f2d3d; margin-bottom: 6px; font-weight: 500; }
.result-line b { color: #e04f4f; }
.result-box.ok .result-line b { color: #22a06b; }
.result-sub { font-size: 13px; color: #5a6b82; line-height: 1.7; }
.action-bar {
  display: flex; gap: 10px; justify-content: center;
  padding-top: 16px; margin-top: 16px; border-top: 1px solid #eef1f6;
}
.empty { color: #a3aec1; text-align: center; padding: 60px 0; font-size: 14px; }
</style>
