<template>
  <div class="exam-page">
    <header class="exam-header">
      <div class="header-left">
        <span class="type-badge listening">LISTENING</span>
        <span class="progress-text">第 {{ currentIndex + 1 }} / {{ questionList.length }} 题</span>
      </div>
      <div class="header-right">
        <div class="timer-box">
          <span class="timer-label">用时</span>
          <span class="timer-value">{{ timeText }}</span>
        </div>
        <button class="btn soft" @click="addWrong">加入错题本</button>
        <button class="btn ghost" @click="goTpo">返回 TPO</button>
      </div>
    </header>

    <main class="exam-body">
      <section class="panel audio-panel">
        <div class="panel-head">
          <h3>Audio</h3>
          <span class="track-time">{{ currentTime }} / {{ duration }}</span>
        </div>
        <div class="audio-wrap">
          <audio ref="audioRef" :src="sectionInfo.audioUrl" @timeupdate="onTimeUpdate" @ended="onEnded"></audio>
          <button class="play-btn" @click="playAudio">{{ isPlaying ? '暂停' : '播放' }}</button>
          <div class="audio-tip">点击播放听力音频，可反复收听</div>
        </div>
        <!-- 听力原文按题控制：**本题**提交答案后才显示，换到下一题又会重新锁上 -->
        <div v-if="article.length && currentSubmitted" class="article-text">
          <div class="article-head">
            <span>听力原文 Transcript</span>
            <!-- 译文按钮同样只在本题提交后出现，换题自动收起 -->
            <button v-if="hasSubmitted" class="btn ghost btn-translate" type="button" @click="showTranslation = !showTranslation">
              {{ showTranslation ? '隐藏中文译文' : '查看中文译文' }}
            </button>
          </div>
          <p v-for="(p, idx) in article" :key="idx">{{ p }}</p>
          <div v-if="showTranslation" class="translation-box">
            <div class="translation-head">中文译文</div>
            <p v-for="(p, idx) in translationParagraphs" :key="idx">{{ p }}</p>
            <p v-if="translationParagraphs.length === 0" class="translation-empty">
              本篇暂无中文译文。可在导入模板里补 [TRANSLATION] 段重新导入，或直接给 tpo_section.content_translation 填内容。
            </p>
          </div>
        </div>
        <div v-else-if="article.length" class="article-locked">
          听力原文在你提交本题答案后才显示
        </div>
      </section>

      <section class="panel question-panel">
        <div class="panel-head">
          <h3>Question</h3>
          <span class="q-index">{{ currentIndex + 1 }}</span>
        </div>

        <div v-if="currentQuestion" class="question-inner">
          <p class="q-title">{{ currentQuestion.questionTitle }}</p>

          <el-radio-group v-model="userAnswer" class="option-group" @change="onPick">
            <el-radio value="A" class="option-card" :class="optionClass('A')">
              <span class="opt-key">A</span>
              <span class="opt-text">{{ currentQuestion.optionA || '暂无内容' }}</span>
              <span v-if="isCorrectOption('A')" class="opt-tag">正确答案</span>
            </el-radio>
            <el-radio value="B" class="option-card" :class="optionClass('B')">
              <span class="opt-key">B</span>
              <span class="opt-text">{{ currentQuestion.optionB || '暂无内容' }}</span>
              <span v-if="isCorrectOption('B')" class="opt-tag">正确答案</span>
            </el-radio>
            <el-radio value="C" class="option-card" :class="optionClass('C')">
              <span class="opt-key">C</span>
              <span class="opt-text">{{ currentQuestion.optionC || '暂无内容' }}</span>
              <span v-if="isCorrectOption('C')" class="opt-tag">正确答案</span>
            </el-radio>
            <el-radio value="D" class="option-card" :class="optionClass('D')">
              <span class="opt-key">D</span>
              <span class="opt-text">{{ currentQuestion.optionD || '暂无内容' }}</span>
              <span v-if="isCorrectOption('D')" class="opt-tag">正确答案</span>
            </el-radio>
          </el-radio-group>

          <div v-if="result" class="result-box" :class="result.correct ? 'ok' : 'bad'">
            <div v-if="!result.answered" class="result-line">
              你未作答，正确答案为 <b>{{ result.correctAnswer }}</b>
            </div>
            <template v-else>
              <div v-if="result.correct" class="result-line">✅ 答案正确</div>
              <div v-else class="result-line">
                ❌ 回答错误，正确答案为 <b>{{ result.correctAnswer }}</b>
              </div>
              <div class="result-sub">你的答案：{{ result.userAnswer }}</div>
            </template>
            <div v-if="result.analysis" class="result-sub">解析：{{ result.analysis }}</div>
          </div>
        </div>
        <div v-else class="empty">暂无题目数据</div>

        <div class="action-bar">
          <button class="btn ghost" :disabled="currentIndex === 0" @click="prevQuestion">上一题</button>
          <button class="btn primary" @click="submitAnswer">提交答案</button>
          <button class="btn ghost" :disabled="currentIndex >= questionList.length - 1" @click="nextQuestion">下一题</button>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, watch, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
// 统一走封装的 service：自动带 token、统一错误提示（原来是裸 axios，登录态不生效）
import service from '@/utils/request'

const route = useRoute()
const router = useRouter()

const sectionId = ref(null)
const jumpQid = ref(null)
const sectionInfo = ref({})
const article = ref([])
const questionList = ref([])
const currentIndex = ref(0)
const userAnswer = ref('')
const result = ref(null)
const answerCache = ref({})
/**
 * 本次会话里考生自己点过的选项：{ 题目id: 'A' }
 * **不再从服务端历史作答回填**：一进页面就有选项显示「已选中」，等于提前暗示答案。
 */
const pickedMap = ref({})
/** 本次会话里已经提交过的题目判分结果：{ 题目id: 判分对象 } */
const resultMap = ref({})

// 用户id 从登录信息里取，不再写死 1
const readUserId = () => {
  try {
    const info = JSON.parse(localStorage.getItem('userInfo') || '{}')
    return Number(info.userId) || 1
  } catch (e) {
    return 1
  }
}
const userId = readUserId()

const audioRef = ref(null)
const isPlaying = ref(false)
const currentTime = ref('00:00')
const duration = ref('00:00')

// 计时：进入页面即自动开始（和真实考试一样，不需要手动点「开始」，也不能暂停）
const timeText = ref('00:00')
let timer = null
let second = 0

const startTimer = () => {
  if (timer) clearInterval(timer)
  second = 0
  timeText.value = '00:00'
  timer = setInterval(() => {
    second++
    timeText.value = `${String(Math.floor(second / 60)).padStart(2, '0')}:${String(second % 60).padStart(2, '0')}`
  }, 1000)
}

const currentQuestion = computed(() => questionList.value[currentIndex.value] || null)
// 中文译文开关（仅本题提交后可用，换题收起）
const showTranslation = ref(false)
const hasSubmitted = computed(() => Object.keys(resultMap.value).length > 0)
const translationParagraphs = computed(() => {
  const text = String(sectionInfo.value.contentTranslation || "").trim()
  return text ? text.split("\n").filter(t => t.trim()) : []
})

/**
 * 当前这道题是否已提交：决定听力原文能不能显示。
 * 用 computed 按题判断：换题自动重新锁上，避免拿原文做后面还没做的题。
 */
const currentSubmitted = computed(() => {
  const q = currentQuestion.value
  return !!(q && resultMap.value[q.id])
})

/**
 * 选项对错样式：做题过程中（result 为空）一律中性，不标出哪个是正确答案；
 * 提交后正确项标绿、选错项标红。
 */
const optionClass = (key) => {
  if (!result.value) return ''
  if (result.value.correctAnswer === key) return 'opt-correct'
  if (result.value.userAnswer === key && result.value.correctAnswer !== key) return 'opt-wrong'
  return ''
}
const isCorrectOption = (key) => !!(result.value && result.value.correctAnswer === key)

const goTpo = () => router.push('/tpo')

const fmt = s => {
  if (!s || isNaN(s)) return '00:00'
  return `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(Math.floor(s % 60)).padStart(2, '0')}`
}
const playAudio = () => {
  if (!audioRef.value) return
  if (isPlaying.value) audioRef.value.pause()
  else audioRef.value.play()
  isPlaying.value = !isPlaying.value
}
const onEnded = () => (isPlaying.value = false)
const onTimeUpdate = () => {
  const a = audioRef.value
  if (!a) return
  currentTime.value = fmt(a.currentTime)
  duration.value = fmt(a.duration)
}

const getPageData = async () => {
  if (!sectionId.value) {
    ElMessage.warning('缺少篇章编号')
    return
  }
  try {
    const [secRes, qRes, ansRes] = await Promise.all([
      service.get('/api/section', { params: { id: sectionId.value } }),
      service.get('/api/question/list', { params: { sectionId: sectionId.value } }),
      service.get('/api/answer/list', { params: { userId } })
    ])

    sectionInfo.value = secRes.data || {}
    article.value = sectionInfo.value.contentText
      ? sectionInfo.value.contentText.split('\n').filter(t => t.trim())
      : []
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
    ElMessage.error('加载听力数据失败：' + (err?.response?.data?.msg || '网络异常'))
    console.error(err)
  }
}

const loadCurrent = () => {
  if (!currentQuestion.value) return
  const qid = currentQuestion.value.id
  // ① 选项：只回显本次会话点过的选择，其余一律「未选择」
  userAnswer.value = pickedMap.value[qid] || ''
  // ② 判分结果：只恢复本次会话提交过的题，历史作答不回填
  result.value = resultMap.value[qid] || null
  // 换题收起译文，避免拿译文做还没做的题
  showTranslation.value = false
}

/** 点击选项：记录本次会话的选择（提交前不算已作答） */
const onPick = () => {
  if (!currentQuestion.value) return
  pickedMap.value[currentQuestion.value.id] = userAnswer.value
}

const submitAnswer = async () => {
  if (!currentQuestion.value) return
  // 未选任何选项先提醒：避免空提交后直接看到正确答案
  if (!userAnswer.value) {
    ElMessage.warning('请先选择一个选项再提交')
    return
  }
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
    resultMap.value[res.data.questionId] = res.data
    answerCache.value[result.value.questionId] = {
      questionId: result.value.questionId,
      userAnswer: result.value.userAnswer,
      isCorrect: result.value.correct ? 1 : 0
    }

    if (!result.value.answered) {
      ElMessage.warning('你未作答，正确答案为 ' + result.value.correctAnswer)
    } else if (result.value.correct) {
      ElMessage.success('答案正确')
    } else {
      ElMessage.error('回答错误，正确答案为 ' + result.value.correctAnswer)
    }
  } catch (err) {
    ElMessage.error('提交答案失败')
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

// 只在路由变化时加载一次。原来 watch(immediate) + onMounted 会各请求一遍，
// 页面一进来就打两次接口（日志里能看到重复请求）
watch(route, async () => {
  sectionId.value = route.params.sectionId
  jumpQid.value = route.query.jumpQid
  // 进入页面（或换篇）立刻开始计时
  startTimer()
  await getPageData()
}, { immediate: true })

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
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
.type-badge.listening { background: linear-gradient(135deg, #34c5b5, #14a094); }
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

.exam-body { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.panel {
  background: #fff; border-radius: 14px; box-shadow: 0 2px 12px rgba(28, 50, 92, 0.06);
  padding: 18px; display: flex; flex-direction: column; min-height: 74vh;
}
.panel-head {
  display: flex; align-items: center; justify-content: space-between;
  border-bottom: 1px solid #eef1f6; padding-bottom: 10px; margin-bottom: 14px;
}
.panel-head h3 { margin: 0; font-size: 15px; color: #1f2d3d; font-weight: 600; }
.track-time { font-size: 13px; color: #8a99ad; font-variant-numeric: tabular-nums; }
.q-index {
  width: 24px; height: 24px; border-radius: 50%; background: #eaf1ff; color: #2b6ef2;
  font-size: 13px; display: flex; align-items: center; justify-content: center; font-weight: 700;
}
.audio-wrap {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 16px; background: linear-gradient(135deg, #f3f8ff, #eefaf8);
  border-radius: 12px; padding: 34px 20px; margin-bottom: 14px;
}
.play-btn {
  width: 78px; height: 78px; border-radius: 50%; border: none; cursor: pointer;
  color: #fff; font-size: 15px; background: linear-gradient(135deg, #34c5b5, #14a094);
  box-shadow: 0 8px 20px rgba(20, 160, 148, 0.32); transition: transform 0.18s;
}
.play-btn:hover { transform: scale(1.05); }
.audio-tip { font-size: 13px; color: #7d8ca1; }
.article-text { max-height: 26vh; overflow-y: auto; line-height: 1.8; font-size: 14.5px; color: #5a6b82; }
.article-text p { margin: 0 0 12px; }
.article-head {
  font-size: 12px; font-weight: 700; color: #14a094;
  letter-spacing: 0.4px; margin-bottom: 8px;
}
.article-locked {
  padding: 12px 14px; border-radius: 10px; background: #f4f7fb;
  color: #8a99ad; font-size: 13px;
}

.question-inner { flex: 1; }
.q-title { font-size: 16px; line-height: 1.7; color: #1f2d3d; margin: 4px 0 18px; font-weight: 500; }
.option-group { display: flex; flex-direction: column; width: 100%; }
.option-card {
  display: flex !important; align-items: flex-start; width: 100%;
  margin: 0 0 10px !important; padding: 13px 15px; border: 1px solid #e6ebf3;
  border-radius: 11px; background: #fff; transition: all 0.18s; box-sizing: border-box;
}
.option-card:hover { border-color: #a9c6ff; background: #f7faff; }
.option-card.is-checked { border-color: #2b6ef2; background: #f2f7ff; box-shadow: 0 2px 10px rgba(43, 110, 242, 0.12); }
.opt-key {
  display: inline-flex; width: 22px; height: 22px; border-radius: 6px; background: #eef2f9;
  color: #5a6b82; font-size: 12px; font-weight: 700; align-items: center;
  justify-content: center; margin-right: 10px; flex: none;
}
.option-card.is-checked .opt-key { background: #2b6ef2; color: #fff; }

/* 提交答案后的对错标记（做题时不出现） */
.option-card.opt-correct {
  border-color: #22a06b !important;
  background: #f1fbf6 !important;
}
.option-card.opt-correct .opt-key { background: #22a06b !important; color: #fff !important; }
.option-card.opt-wrong {
  border-color: #e04f4f !important;
  background: #fef5f5 !important;
}
.option-card.opt-wrong .opt-key { background: #e04f4f !important; color: #fff !important; }
.opt-tag {
  margin-left: 8px; font-size: 11px; font-weight: 600; color: #157a51;
  background: #d9f5e7; border-radius: 999px; padding: 1px 8px; flex: none;
}
.opt-text { font-size: 14.5px; line-height: 1.6; color: #35404f; white-space: normal; }

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
  padding-top: 14px; margin-top: auto; border-top: 1px solid #eef1f6;
}
.empty { color: #a3aec1; text-align: center; padding: 40px 0; font-size: 14px; }

@media (max-width: 1100px) {
  .exam-body { grid-template-columns: 1fr; }
  .panel { min-height: auto; }
}

/* ===== 中文译文区块（本题提交后才出现） ===== */
.article-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.btn-translate {
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 8px;
  border: 1px solid #d6e0f2;
  background: #f7f9ff;
  color: #2a4a9c;
  cursor: pointer;
}
.translation-box {
  margin-top: 12px;
  padding: 12px 14px;
  border: 1px solid #e6ebf5;
  border-left: 3px solid #2a4a9c;
  border-radius: 10px;
  background: #fbfcff;
}
.translation-head { font-size: 12px; font-weight: 700; color: #2a4a9c; letter-spacing: 0.4px; margin-bottom: 8px; }
.translation-box p { margin: 0 0 8px; font-size: 13px; line-height: 1.85; color: #3a4a63; }
.translation-empty { color: #8a94a6 !important; }
</style>
