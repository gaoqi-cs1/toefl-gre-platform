<template>
  <div class="exam-page">
    <header class="exam-header">
      <div class="header-left">
        <span class="type-badge reading">READING</span>
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
      <section class="panel article-panel">
        <div class="panel-head">
          <h3>Passage</h3>
          <!-- 译文按钮只在提交过至少一题后出现：做题前不给任何中文提示 -->
          <button
            v-if="hasSubmitted"
            class="btn ghost btn-translate"
            type="button"
            @click="showTranslation = !showTranslation"
          >
            {{ showTranslation ? '隐藏中文译文' : '查看中文译文' }}
          </button>
        </div>
        <div class="article-text">
          <p v-for="(p, idx) in article" :key="idx">{{ p }}</p>
          <p v-if="article.length === 0" class="empty">文章加载中...</p>
        </div>

        <!-- 提交后才允许展开中文译文，未作答前整块不渲染 -->
        <el-collapse-transition>
          <div v-if="hasSubmitted && showTranslation" class="translation-box">
            <div class="translation-head">中文译文</div>
            <p v-for="(p, idx) in translationParagraphs" :key="idx">{{ p }}</p>
            <p v-if="translationParagraphs.length === 0" class="translation-empty">
              本篇暂无中文译文。可在导入模板里补 [TRANSLATION] 段重新导入，或直接给 tpo_section.content_translation 填内容。
            </p>
          </div>
        </el-collapse-transition>
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
          </div>

          <!-- 解析必须先提交答案才出现：做题时不提前泄露正确答案 -->
          <div v-if="result" class="analysis-action" style="margin-top:12px;">
            <el-button size="small" @click="showAnalysis = !showAnalysis">
              {{ showAnalysis ? 'Hide Analysis' : 'Show Analysis' }}
            </el-button>
          </div>
          <el-collapse-transition>
            <div v-if="result && showAnalysis && currentQuestion.analysis" class="analysis-en-box">
              <div class="analysis-title">Analysis · 解析（中英对照）</div>
              <div class="analysis-text">{{ currentQuestion.analysis }}</div>
            </div>
          </el-collapse-transition>
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
// ✅ 替换成封装好的request，自动携带token
import service from '@/utils/request'

const route = useRoute()
const router = useRouter()

const sectionId = ref(null)
const jumpQid = ref(null)
const article = ref([])
const questionList = ref([])
const currentIndex = ref(0)
const userAnswer = ref('')
const result = ref(null)
const answerCache = ref({})
/**
 * 本次会话里考生自己点过的选项：{ 题目id: 'A' }
 * 只记这次打开页面之后的点击，**不再从服务端历史作答回填**：
 * 一进来就有个选项是「已选中」的样子，等于提前暗示答案，这是之前的问题根源。
 */
const pickedMap = ref({})
/**
 * 本次会话里已经提交过的题目：{ 题目id: 后端判分对象 }
 * 已提交过的题回看时保留对错和解析；没提交过的题保持干净——无选中、无标记、无解析。
 */
const resultMap = ref({})
const userId = 1
// 解析显示开关
const showAnalysis = ref(false)
// 中文译文开关（只在提交过至少一题后可用）
const showTranslation = ref(false)
const sectionInfo = ref({})
const hasSubmitted = computed(() => Object.keys(resultMap.value).length > 0)
const translationParagraphs = computed(() => {
  const text = String(sectionInfo.value.contentTranslation || "").trim()
  return text ? text.split("\n").filter(t => t.trim()) : []
})

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

/**
 * 提交后才标出选项对错：
 *   没提交（result 为空）→ 不返回任何样式，选项一律中性，不显示哪个是正确答案；
 *   提交后 → 正确选项标绿（opt-correct），选错的那一项标红（opt-wrong）。
 */
const optionClass = (key) => {
  if (!result.value) return ''
  if (result.value.correctAnswer === key) return 'opt-correct'
  if (result.value.userAnswer === key && result.value.correctAnswer !== key) return 'opt-wrong'
  return ''
}

/** 提交后才在正确选项后面挂「正确答案」小标签 */
const isCorrectOption = (key) => !!(result.value && result.value.correctAnswer === key)

const goTpo = () => router.push('/tpo')

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

    const sec = secRes.data || {}
    sectionInfo.value = sec
    article.value = sec.contentText ? sec.contentText.split('\n').filter(t => t.trim()) : []
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
    ElMessage.error('加载数据失败')
    console.error(err)
  }
}

const loadCurrent = () => {
  if (!currentQuestion.value) return
  const qid = currentQuestion.value.id
  // ① 选项：只有本次会话点过的才回显，否则一律「未选择」
  userAnswer.value = pickedMap.value[qid] || ''
  // ② 判分结果：只有本次会话提交过的题才恢复，历史作答不回填
  result.value = resultMap.value[qid] || null
  // 切换题目自动关闭解析面板
  showAnalysis.value = false
}

/** 点击选项：记住本次会话的选择（未提交前不算已作答） */
const onPick = () => {
  if (!currentQuestion.value) return
  pickedMap.value[currentQuestion.value.id] = userAnswer.value
}

const saveToCache = (vo) => {
  answerCache.value[vo.questionId] = {
    questionId: vo.questionId,
    userAnswer: vo.userAnswer,
    isCorrect: vo.correct ? 1 : 0
  }
}

const submitAnswer = async () => {
  if (!currentQuestion.value) return
  // 没点任何选项就提交：先提醒选择，避免"空卷提交 → 直接看到正确答案"
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
    saveToCache(res.data)

    if (!result.value.answered) {
      ElMessage.warning('你未作答，正确答案为 ' + result.value.correctAnswer)
    } else if (result.value.correct) {
      ElMessage.success('答案正确')
    } else {
      ElMessage.error('回答错误，正确答案为 ' + result.value.correctAnswer)
    }
  } catch (err) {
    ElMessage.error('提交答案失败')
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

// 说明：这里原来还有一个 onMounted 再调一次 getPageData()，
// 和上面 watch 的 immediate 重复，页面一进来就发两遍请求（还会把当前题状态重置两次）。
// 已删掉，只保留 watch 这一处。
</script>

<style scoped>
.exam-page {
  min-height: 100vh;
  background: #f4f6fb;
  padding: 18px;
  box-sizing: border-box;
  font-family: -apple-system, 'PingFang SC', 'Microsoft YaHei', sans-serif;
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
.type-badge.reading { background: linear-gradient(135deg, #4f8ef7, #2b6ef2); }
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
.q-index {
  width: 24px; height: 24px; border-radius: 50%; background: #eaf1ff; color: #2b6ef2;
  font-size: 13px; display: flex; align-items: center; justify-content: center; font-weight: 700;
}
.article-text { overflow-y: auto; line-height: 1.9; font-size: 15.5px; color: #35404f; padding-right: 6px; }
.article-text p { margin: 0 0 16px; text-indent: 2em; }
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
  margin-left: 8px;
  font-size: 11px;
  font-weight: 600;
  color: #157a51;
  background: #d9f5e7;
  border-radius: 999px;
  padding: 1px 8px;
  flex: none;
}
.opt-text { font-size: 14.5px; line-height: 1.6; color: #35404f; white-space: normal; }

.result-box {
  margin-top: 14px; border-radius: 10px; padding: 12px 14px; border-left: 3px solid #2b6ef2;
  background: #f8fbff;
}
.result-box.ok { border-left-color: #22a06b; background: #f1fbf6; }
.result-box.bad { border-left-color: #e04f4f; background: #fef5f5; }
.result-line { font-size: 14px; color: #1f2d3d; margin-bottom: 6px; font-weight: 500; }
.result-line b { color: #2b6ef2; }
.result-box.bad .result-line b { color: #e04f4f; }
.result-sub { font-size: 13px; color: #5a6b82; line-height: 1.7; }

/* 英文解析面板样式，和页面风格统一 */
.analysis-en-box {
  margin-top:10px;
  padding:14px;
  background:#f0f6ff;
  border-radius:10px;
  border-left:3px solid #4f8ef7;
}
.analysis-title {
  font-weight:600;
  color:#2b6ef2;
  margin-bottom:6px;
  font-size:14px;
}
.analysis-text {
  font-size:14px;
  line-height:1.7;
  color:#2c3b52;
}

.action-bar {
  display: flex; gap: 10px; justify-content: center;
  padding-top: 14px; margin-top: auto; border-top: 1px solid #eef1f6;
}
.empty { color: #a3aec1; text-align: center; padding: 40px 0; font-size: 14px; }

@media (max-width: 1100px) {
  .exam-body { grid-template-columns: 1fr; }
  .panel { min-height: auto; }
}

/* ===== 中文译文区块（提交后才出现） ===== */
.btn-translate {
  font-size: 12px;
  padding: 5px 12px;
  border-radius: 8px;
  border: 1px solid #d6e0f2;
  background: #f7f9ff;
  color: #2a4a9c;
  cursor: pointer;
}
.translation-box {
  margin-top: 14px;
  padding: 14px 16px;
  border: 1px solid #e6ebf5;
  border-left: 3px solid #2a4a9c;
  border-radius: 10px;
  background: #fbfcff;
}
.translation-head {
  font-size: 12px;
  font-weight: 700;
  color: #2a4a9c;
  letter-spacing: 0.4px;
  margin-bottom: 8px;
}
.translation-box p {
  margin: 0 0 8px;
  font-size: 13px;
  line-height: 1.85;
  color: #3a4a63;
}
.translation-empty { color: #8a94a6 !important; font-style: normal; }
</style>
