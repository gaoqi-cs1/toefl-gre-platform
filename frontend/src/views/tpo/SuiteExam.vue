<template>
  <div class="suite-page">
    <header class="exam-header">
      <div class="header-left">
        <span class="suite-name">{{ suite.title || '套题模考' }}</span>
        <span class="record-tag">记录 #{{ recordId }}</span>
        <span class="timer">用时 {{ timeText }}</span>
      </div>
      <div class="header-right">
        <button class="btn ghost" @click="goRecords">模考记录</button>
        <button class="btn primary" :disabled="submitting" @click="submitAllExam">
          {{ submitting ? '提交中...' : '提交整卷' }}
        </button>
      </div>
    </header>

    <div v-if="loadError" class="tip-box warn">{{ loadError }}</div>

    <el-tabs v-model="activeTab" class="suite-tabs">
      <el-tab-pane
        v-for="tab in tabs"
        :key="tab.type"
        :label="tab.label + ' (' + (groups[tab.type] ? groups[tab.type].length : 0) + ' 题)'"
        :name="tab.type"
      >
        <div v-if="!groups[tab.type] || groups[tab.type].length === 0" class="tip-box">
          本套题未关联 {{ tab.label }} 篇章，或该篇章没有题目。
        </div>

        <div v-for="sec in groups[tab.type]" :key="sec.sectionId" class="section-block">
          <div class="section-head">
            <span class="sec-title">{{ tab.label }} · 篇章 #{{ sec.sectionId }}</span>
            <span class="sec-meta">{{ sec.questions.length }} 题</span>
          </div>

          <!-- 听力：音频播放器 -->
          <div v-if="tab.type === 'listening'" class="audio-box">
            <audio :ref="el => setAudioRef(sec.sectionId, el)" :src="sec.audioUrl" controls></audio>
            <div v-if="!sec.audioUrl" class="audio-missing">该篇章没有配置音频文件</div>
          </div>

          <!-- 阅读 / 听力：篇章正文（可折叠） -->
          <div v-if="sec.contentText" class="passage-box">
            <div class="passage-toggle" @click="togglePassage(sec.sectionId)">
              {{ openedPassage[sec.sectionId] ? '收起文章' : '展开文章' }}
            </div>
            <div v-if="openedPassage[sec.sectionId]" class="passage-text">
              <p v-for="(p, i) in splitParagraphs(sec.contentText)" :key="i">{{ p }}</p>
            </div>
          </div>

          <div v-for="(q, idx) in sec.questions" :key="q.id" class="question-item">
            <div class="q-head">
              <span class="q-no">{{ idx + 1 }}</span>
              <span class="q-title">{{ q.questionTitle || ('题目 ' + q.id) }}</span>
            </div>

            <!-- 客观题：单选 -->
            <div v-if="isObjective(tab.type)" class="option-list">
              <div
                v-for="opt in buildOptions(q)"
                :key="opt.key"
                class="option-item"
                :class="{ checked: answers[q.id] === opt.key }"
                @click="answers[q.id] = opt.key"
              >
                <span class="opt-key">{{ opt.key }}</span>
                <span class="opt-text">{{ opt.text || '暂无内容' }}</span>
              </div>
            </div>

            <!-- 口语 / 写作：文本作答 -->
            <div v-else class="subjective-box">
              <div v-if="q.prompt" class="prompt-text">{{ q.prompt }}</div>
              <div class="limit-tip">
                建议时长：{{ q.limitSecond ? Math.round(q.limitSecond / 60) + ' 分钟' : '按题目要求' }}
              </div>
              <el-input
                v-model="answers[q.id]"
                type="textarea"
                :rows="tab.type === 'writing' ? 4 : 3"
                placeholder="在此输入你的作答内容（口语可写要点提纲，也可以只记要点）"
              />
              <div v-if="tab.type === 'writing'" class="word-count">
                字数：{{ wordCount(answers[q.id]) }}
              </div>
            </div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import service from '@/utils/request.js'

const route = useRoute()
const router = useRouter()

const suiteId = ref(route.query.suiteId)
const recordId = ref(route.query.recordId ? Number(route.query.recordId) : null)

const suite = ref({})
const groups = reactive({ reading: [], listening: [], speaking: [], writing: [] })
const answers = reactive({})
const openedPassage = reactive({})
const audioRefs = {}

const activeTab = ref('reading')
const loadError = ref('')
const submitting = ref(false)

const tabs = [
  { type: 'reading', label: '阅读 R' },
  { type: 'listening', label: '听力 L' },
  { type: 'speaking', label: '口语 S' },
  { type: 'writing', label: '写作 W' }
]

// 计时器
const timeText = ref('00:00')
let second = 0
let timer = null

const startTimer = () => {
  timer = setInterval(() => {
    second++
    timeText.value =
      String(Math.floor(second / 60)).padStart(2, '0') + ':' + String(second % 60).padStart(2, '0')
  }, 1000)
}

const isObjective = (type) => type === 'reading' || type === 'listening'

const buildOptions = (q) => [
  { key: 'A', text: q.optionA },
  { key: 'B', text: q.optionB },
  { key: 'C', text: q.optionC },
  { key: 'D', text: q.optionD }
]

const splitParagraphs = (text) => (text || '').split('\n').filter(t => t.trim())

const togglePassage = (sectionId) => {
  openedPassage[sectionId] = !openedPassage[sectionId]
}

const setAudioRef = (sectionId, el) => {
  if (el) audioRefs[sectionId] = el
}

const wordCount = (text) => {
  const t = (text || '').trim()
  return t ? t.split(/\s+/).length : 0
}

const goRecords = () => router.push('/tpo/suite/records')

/** 1. 保证有模考记录：没有 recordId 就先 start 一条 doing 记录 */
const ensureRecord = async () => {
  if (recordId.value) return true
  if (!suiteId.value) {
    loadError.value = '缺少套题编号，无法开始模考'
    return false
  }
  const res = await service.post('/api/examRecord/start', { suiteId: Number(suiteId.value) })
  if (res.code !== 200) {
    loadError.value = res.msg || '创建模考记录失败'
    return false
  }
  recordId.value = res.data.id
  return true
}

/** 2. 加载套题详情 + 每个篇章的题目 */
const loadSuite = async () => {
  try {
    const res = await service.get('/api/suite/' + suiteId.value)
    if (res.code !== 200) {
      loadError.value = res.msg || '套题加载失败'
      return
    }
    suite.value = res.data
    const sections = res.data.sectionList || []

    let missing = []
    for (const sec of sections) {
      const type = sec.type
      if (!groups[type]) continue
      if (sec.exist === false) {
        missing.push('#' + sec.sectionId)
        continue
      }

      // 题目
      const qRes = await service.get('/api/question/list', { params: { sectionId: sec.sectionId } })
      const questions = (qRes.data || [])

      // 阅读/听力篇章正文与音频
      let contentText = ''
      let audioUrl = sec.audioUrl || ''
      if (type === 'reading' || type === 'listening') {
        const sRes = await service.get('/api/section', { params: { id: sec.sectionId } })
        if (sRes.code === 200 && sRes.data) {
          contentText = sRes.data.contentText || ''
          audioUrl = audioUrl || sRes.data.audioUrl || ''
        }
      }

      groups[type].push({
        sectionId: sec.sectionId,
        questions,
        contentText,
        audioUrl
      })
    }

    if (missing.length) {
      loadError.value = '套题里关联的篇章 ' + missing.join('、') + ' 在数据库中不存在，请检查 suite_section 表'
    }

    // 默认停在第一个有内容的页签
    const firstWithData = tabs.find(t => groups[t.type].length > 0)
    if (firstWithData) activeTab.value = firstWithData.type
  } catch (err) {
    loadError.value = '加载套题数据失败：' + (err?.response?.data?.msg || err.message || '网络异常')
    console.error(err)
  }
}

/** 3. 整卷提交 */
const submitAllExam = async () => {
  const reading = []
  const listening = []
  const speaking = []
  const writing = []

  groups.reading.forEach(sec => sec.questions.forEach(q => {
    if (answers[q.id]) reading.push({ questionId: q.id, userAns: answers[q.id] })
  }))
  groups.listening.forEach(sec => sec.questions.forEach(q => {
    if (answers[q.id]) listening.push({ questionId: q.id, userAns: answers[q.id] })
  }))
  groups.speaking.forEach(sec => sec.questions.forEach(q => {
    if (answers[q.id]) speaking.push({ questionId: q.id, content: answers[q.id] })
  }))
  groups.writing.forEach(sec => sec.questions.forEach(q => {
    if (answers[q.id]) writing.push({ questionId: q.id, content: answers[q.id] })
  }))

  const total = reading.length + listening.length + speaking.length + writing.length
  if (total === 0) {
    ElMessage.warning('还没有作答任何题目')
    return
  }

  try {
    await ElMessageBox.confirm(
      '确定提交整套模考？提交后本次作答不能再修改，系统会自动判分并生成成绩。',
      '提交确认',
      { type: 'warning', confirmButtonText: '确认提交', cancelButtonText: '再检查一下' }
    )
  } catch (e) {
    return
  }

  submitting.value = true
  try {
    const res = await service.post('/api/examRecord/submit', {
      recordId: recordId.value,
      readingAnswerList: reading,
      listeningAnswerList: listening,
      speakingAnswerList: speaking,
      writingAnswerList: writing
    })
    if (res.code !== 200) {
      ElMessage.warning(res.msg || '提交失败')
      return
    }
    ElMessage.success('提交成功：阅读 ' + res.data.readScore + ' / 听力 ' + res.data.listenScore + ' / 总分 ' + res.data.totalScore)
    router.push({ path: '/tpo/suite/result', query: { recordId: recordId.value } })
  } catch (err) {
    ElMessage.error('提交失败：' + (err?.response?.data?.msg || '网络异常'))
    console.error(err)
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  startTimer()
  const ok = await ensureRecord()
  if (ok) await loadSuite()
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.suite-page {
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
.header-left { display: flex; align-items: center; gap: 14px; flex-wrap: wrap; }
.suite-name { font-size: 17px; font-weight: 600; color: #1f2d3d; }
.record-tag { font-size: 12px; color: #5a6b82; background: #f2f4f8; padding: 3px 9px; border-radius: 6px; }
.timer { font-size: 13px; color: #2b6ef2; font-variant-numeric: tabular-nums; }
.header-right { display: flex; gap: 10px; }
.btn {
  border: none; cursor: pointer; border-radius: 9px; font-size: 13px;
  padding: 8px 14px; transition: all 0.18s; font-family: inherit;
}
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
.btn.primary { background: #2b6ef2; color: #fff; }
.btn.primary:hover { background: #1b5cd8; }
.btn.ghost { background: #f2f4f8; color: #5a6b82; }

.suite-tabs {
  background: #fff; border-radius: 14px; padding: 6px 18px 18px;
  box-shadow: 0 2px 12px rgba(28, 50, 92, 0.06);
}
.section-block { margin-bottom: 22px; }
.section-head {
  display: flex; align-items: center; justify-content: space-between;
  border-bottom: 1px solid #eef1f6; padding-bottom: 8px; margin-bottom: 12px;
}
.sec-title { font-size: 14px; font-weight: 600; color: #1f2d3d; }
.sec-meta { font-size: 12px; color: #8a99ad; }
.audio-box { margin-bottom: 12px; }
.audio-box audio { width: 100%; }
.audio-missing { font-size: 12px; color: #e6a23c; }
.passage-box { margin-bottom: 14px; }
.passage-toggle {
  display: inline-block; font-size: 13px; color: #2b6ef2; cursor: pointer;
  background: #f2f7ff; padding: 5px 12px; border-radius: 8px;
}
.passage-text {
  margin-top: 10px; max-height: 320px; overflow-y: auto;
  background: #fafbfe; border-radius: 10px; padding: 14px 16px;
  line-height: 1.9; font-size: 14.5px; color: #35404f;
}
.passage-text p { margin: 0 0 12px; text-indent: 2em; }
.question-item {
  border: 1px solid #eef1f6; border-radius: 12px; padding: 14px 16px; margin-bottom: 14px;
}
.q-head { display: flex; align-items: flex-start; gap: 10px; margin-bottom: 10px; }
.q-no {
  flex: none; width: 22px; height: 22px; border-radius: 50%;
  background: #eaf1ff; color: #2b6ef2; font-size: 12px; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
}
.q-title { font-size: 14.5px; line-height: 1.7; color: #1f2d3d; }
.option-list { display: flex; flex-direction: column; gap: 8px; }
.option-item {
  display: flex; align-items: flex-start; gap: 10px; cursor: pointer;
  border: 1px solid #e6ebf3; border-radius: 10px; padding: 10px 12px;
  transition: all 0.16s;
}
.option-item:hover { border-color: #a9c6ff; background: #f7faff; }
.option-item.checked { border-color: #2b6ef2; background: #f2f7ff; }
.opt-key {
  flex: none; width: 20px; height: 20px; border-radius: 5px; background: #eef2f9;
  color: #5a6b82; font-size: 12px; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
}
.option-item.checked .opt-key { background: #2b6ef2; color: #fff; }
.opt-text { font-size: 14px; line-height: 1.6; color: #35404f; }
.subjective-box { display: flex; flex-direction: column; gap: 8px; }
.prompt-text {
  font-size: 14px; line-height: 1.8; color: #35404f; background: #fafbfe;
  border-radius: 10px; padding: 12px 14px;
}
.limit-tip { font-size: 12px; color: #8a99ad; }
.word-count { font-size: 12px; color: #8a99ad; text-align: right; }
.tip-box {
  background: #f7f8fa; border-radius: 10px; padding: 16px; color: #8a99ad; font-size: 13px;
}
.tip-box.warn { background: #fdf6ec; color: #e6a23c; margin-bottom: 14px; }
</style>
