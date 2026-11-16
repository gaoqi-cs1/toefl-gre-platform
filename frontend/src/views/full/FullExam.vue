<template>
  <AppShell>
    <div class="exam-wrap">
      <!-- ============ 顶部：标题 / 计时 / 提交 ============ -->
      <div class="exam-top card">
        <div class="exam-top__left">
          <div class="exam-title">{{ suiteTitle || '真题套题' }}</div>
          <div class="exam-meta">
            <span class="pill pill--muted">共 {{ totalQuestions }} 题</span>
            <span class="pill" :class="answeredTotal === totalQuestions ? 'pill--ok' : 'pill--muted'">
              已答 {{ answeredTotal }} / {{ totalQuestions }}
            </span>
            <span class="pill pill--accent">用时 {{ elapsedText }}</span>
          </div>
        </div>
        <div class="exam-top__right">
          <el-button round @click="quitExam">退出</el-button>
          <el-button type="primary" round :loading="submitting" @click="submitExam">提交整卷</el-button>
        </div>
      </div>

      <!-- ============ 四个模块的步骤条 ============ -->
      <div class="steps card">
        <button
          v-for="(m, i) in modules"
          :key="m.moduleType"
          type="button"
          :class="['step', { 'is-active': i === activeIndex, 'is-done': moduleTotal(m) > 0 && moduleAnswered(m) === moduleTotal(m) }]"
          @click="goModule(i)"
        >
          <span class="step__index">{{ i + 1 }}</span>
          <span class="step__text">
            <strong>{{ moduleLabel(m.moduleType) }}</strong>
            <small>{{ moduleAnswered(m) }} / {{ moduleTotal(m) }} 题</small>
          </span>
        </button>
      </div>

      <div v-if="errorText" class="card card-pad exam-error">{{ errorText }}</div>

      <div v-else class="exam-body">
        <!-- ================= 左侧：材料 + 题目 ================= -->
        <main ref="mainRef" class="exam-main">
          <section v-for="section in activeSections" :key="section.sectionId" class="exam-section">
            <!-- 阅读正文 -->
            <article v-if="activeModule === 'reading' && section.contentText" class="material card">
              <div class="material__head">
                <h3 class="material__title">{{ section.title }}</h3>
                <span class="pill pill--muted">Reading Passage</span>
              </div>
              <p
                v-for="(para, pi) in splitParagraphs(section.contentText)"
                :key="pi"
                class="material__para"
              >{{ para }}</p>
            </article>

            <!-- 听力音频 -->
            <article v-if="activeModule === 'listening'" class="material card">
              <div class="material__head">
                <h3 class="material__title">{{ section.title }}</h3>
                <span class="pill pill--accent">Listening</span>
              </div>
              <p class="material__hint">{{ section.hint }}</p>
              <audio
                v-if="section.audioUrl"
                class="audio"
                :src="section.audioUrl"
                controls
                preload="none"
              ></audio>
              <p class="material__note">答题时不显示听力原文，交卷后可在成绩页对照原文。</p>
            </article>

            <!-- 题目（客观题 / 主观题两种渲染） -->
            <article
              v-for="q in section.questions"
              :id="'q' + q.id"
              :key="q.id"
              :class="['question card', { 'is-answered': !!answers[q.id] }]"
            >
              <div class="question__head">
                <span class="question__no">Q{{ numberMap[q.id] }}</span>
                <span class="pill pill--muted">{{ moduleLabel(q.moduleType) }}</span>
                <span v-if="q.limitSecond > 0" class="pill pill--muted">
                  限时 {{ Math.round(q.limitSecond / 60) }} 分钟
                </span>
              </div>

              <!-- 客观题 -->
              <template v-if="isObjective(q)">
                <p class="question__title">{{ q.questionTitle }}</p>
                <div class="options">
                  <button
                    v-for="opt in optionsOf(q)"
                    :key="opt.key"
                    type="button"
                    :class="['option', { 'is-selected': answers[q.id] === opt.key }]"
                    @click="pickOption(q, opt.key)"
                  >
                    <span class="option__key">{{ opt.key }}</span>
                    <span class="option__text">{{ opt.text }}</span>
                  </button>
                </div>
              </template>

              <!-- 口语 / 写作 -->
              <template v-else>
                <p class="question__prompt">{{ q.prompt }}</p>
                <p v-if="q.limitSecond > 0" class="question__tip">
                  建议用时 {{ Math.round(q.limitSecond / 60) > 0 ? Math.round(q.limitSecond / 60) + ' 分钟' : q.limitSecond + ' 秒' }}
                  <template v-if="q.prepareSecond > 0">（准备 {{ q.prepareSecond }} 秒）</template>
                  · {{ q.moduleType === 'speaking'
                    ? '录音作答：录音自动上传，转写文本交给 AI 按新托福 rubric 评分'
                    : '把作文写进下面文本框，AI 按新托福 1-6 档评分' }}
                </p>

                <!-- ============ 口语：录音作答 ============ -->
                <div v-if="q.moduleType === 'speaking'" class="rec">
                  <div class="rec__head">
                    <span class="rec__title">录音作答</span>
                    <span v-if="recState[q.id] === 'recording'" class="pill pill--rec">● 录音中 {{ recLeft[q.id] }}s</span>
                    <span v-else-if="recState[q.id] === 'uploading'" class="pill pill--muted">正在保存录音…</span>
                    <span v-else-if="audioUrls[q.id]" class="pill pill--ok">录音已保存</span>
                    <span v-else class="pill pill--muted">还没录音</span>
                  </div>

                  <div class="rec__actions">
                    <el-button
                      v-if="recState[q.id] !== 'recording'"
                      type="danger"
                      round
                      :disabled="recState[q.id] === 'uploading'"
                      @click="startRecord(q)"
                    >{{ audioUrls[q.id] ? '重新录音' : '开始录音' }}</el-button>
                    <el-button v-else type="warning" round @click="stopRecord(q)">结束录音</el-button>
                    <el-button v-if="audioUrls[q.id]" round plain @click="clearRecord(q)">删除这段录音</el-button>
                  </div>

                  <audio v-if="audioUrls[q.id]" class="rec__audio" :src="audioUrls[q.id]" controls></audio>

                  <p class="rec__hint">
                    点「开始录音」对着麦克风说答案，说到限时结束会自动停；录音会自动上传并保存，成绩页可以回放。
                    下面的「评分依据文本」由浏览器自动转写，转写不准的地方可以直接改。
                  </p>

                  <p v-if="recError[q.id]" class="rec__error">{{ recError[q.id] }}</p>

                  <div v-if="transcripts[q.id]" class="rec__transcript">
                    <div class="rec__transcript-head">
                      <span class="rec__label">自动转写</span>
                      <el-button link type="primary" @click="applyTranscript(q)">填进作答框</el-button>
                    </div>
                    <p class="rec__transcript-text">{{ transcripts[q.id] }}</p>
                  </div>
                  <p v-else-if="audioUrls[q.id] && recNoAsr[q.id]" class="rec__hint">
                    当前浏览器无法自动转写（或语音服务没连上），录音已经保存好了。请把你说过的内容手动写到下面文本框，AI 才能给你评分。
                  </p>
                </div>

                <!-- 写作：作文正文；口语：评分依据文本 -->
                <span v-if="q.moduleType === 'speaking'" class="rec__label">评分依据文本（转写结果，可修改）</span>
                <el-input
                  v-model="answers[q.id]"
                  type="textarea"
                  :rows="q.moduleType === 'writing' ? 14 : 6"
                  :placeholder="q.moduleType === 'writing'
                    ? '在这里写作文（建议 120-225 词，先写立场再给例子）…'
                    : '录音结束后转写内容会自动出现在这里；转写失败就直接手动写（建议 90-140 词）…'"
                />
                <div class="question__foot">
                  <span class="counter">词数：{{ wordCount(answers[q.id]) }}</span>
                  <el-popover v-if="q.scoreStandard" placement="top" :width="440" trigger="click">
                    <template #reference>
                      <span class="rubric-link">查看评分标准</span>
                    </template>
                    <div class="rubric">{{ q.scoreStandard }}</div>
                  </el-popover>
                </div>
              </template>
            </article>
          </section>

          <!-- ============ 底部操作 ============ -->
          <div class="exam-actions">
            <el-button v-if="activeIndex > 0" round @click="goModule(activeIndex - 1)">上一模块</el-button>
            <el-button
              v-if="activeIndex < modules.length - 1"
              type="primary"
              round
              @click="goModule(activeIndex + 1)"
            >下一模块：{{ moduleLabel(modules[activeIndex + 1].moduleType) }}</el-button>
            <el-button v-else type="primary" round :loading="submitting" @click="submitExam">
              提交整卷
            </el-button>
          </div>
        </main>

        <!-- ================= 右侧：答题卡 ================= -->
        <aside class="exam-aside card">
          <div class="aside__head">
            <h4>答题卡</h4>
            <span class="aside__hint">点题号跳转</span>
          </div>

          <div v-for="m in modules" :key="m.moduleType" class="aside-group">
            <div class="aside-group__title">
              <span>{{ moduleLabel(m.moduleType) }}</span>
              <small>{{ moduleAnswered(m) }}/{{ moduleTotal(m) }}</small>
            </div>
            <div class="aside-grid">
              <button
                v-for="q in flatQuestions(m)"
                :key="q.id"
                type="button"
                :class="['dot', { 'is-answered': !!answers[q.id], 'is-current': q.id === currentQuestionId }]"
                @click="jumpTo(m, q)"
              >{{ numberMap[q.id] }}</button>
            </div>
          </div>

          <div class="aside__foot">
            <p>答案只存在本机浏览器（localStorage），刷新、关掉页面再回来都能接着做。</p>
            <el-button type="primary" plain round style="width: 100%" :loading="submitting" @click="submitExam">
              四个模块做完后提交
            </el-button>
          </div>
        </aside>
      </div>
    </div>
  </AppShell>
</template>

<script setup>
/**
 * 真题套题答题页（单页流程）
 *
 * 交互顺序：
 *   1. 进入页面 → 拿到套题结构（GET /api/fullSuite/{suiteId}），四模块按 reading→listening→speaking→writing 顺序排好；
 *   2. 点选项 / 打字 → 答案立刻写进 localStorage（key 带 recordId，多套题互不干扰）；
 *   3. 刷新、关页面再回来 → 从 localStorage 恢复答案与所在的模块；
 *   4. 四个模块都做完 → 点「提交整卷」→ POST /api/fullExam/submit 一次性判分。
 */
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import AppShell from '@/components/AppShell.vue'

const route = useRoute()
const router = useRouter()

const recordId = ref(route.query.recordId ? String(route.query.recordId) : '')
const suiteId = ref(route.query.suiteId ? String(route.query.suiteId) : '')

const suiteTitle = ref('')
const modules = ref([])
const activeIndex = ref(0)
const answers = reactive({})
const submitting = ref(false)
const errorText = ref('')
const elapsed = ref(0)
const currentQuestionId = ref(null)
const mainRef = ref(null)

let startedAt = 0
let timer = null
let submitted = false

/* localStorage key 都带 recordId：同一个人不同一次模考之间不会串答案 */
const keyAnswers = computed(() => 'fullExam:answers:' + recordId.value)
const keyProgress = computed(() => 'fullExam:progress:' + recordId.value)
const keyStart = computed(() => 'fullExam:startedAt:' + recordId.value)

const MODULE_LABELS = { reading: '阅读', listening: '听力', speaking: '口语', writing: '写作' }

function moduleLabel(type) {
  return MODULE_LABELS[type] || type
}

const activeModule = computed(() => (modules.value[activeIndex.value] || {}).moduleType || '')
const activeSections = computed(() => (modules.value[activeIndex.value] || {}).sections || [])

/* 一个模块里所有题目（一个模块可能有多篇 material，这里摊平） */
function flatQuestions(m) {
  const list = []
  for (const section of m.sections || []) {
    for (const q of section.questions || []) {
      list.push(q)
    }
  }
  return list
}

function moduleTotal(m) {
  return flatQuestions(m).length
}

function moduleAnswered(m) {
  return flatQuestions(m).filter(q => hasAnswer(answers[q.id])).length
}

function hasAnswer(v) {
  return v !== undefined && v !== null && String(v).trim() !== ''
}

const totalQuestions = computed(() => modules.value.reduce((sum, m) => sum + moduleTotal(m), 0))
const answeredTotal = computed(() => modules.value.reduce((sum, m) => sum + moduleAnswered(m), 0))

/* 题号：整卷连续编号 Q1…Qn，右侧答题卡与题目卡片共用 */
const numberMap = computed(() => {
  const map = {}
  let n = 0
  for (const m of modules.value) {
    for (const q of flatQuestions(m)) {
      n += 1
      map[q.id] = n
    }
  }
  return map
})

const elapsedText = computed(() => {
  const m = Math.floor(elapsed.value / 60)
  const s = elapsed.value % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
})

/* ---------------- 渲染辅助 ---------------- */
function splitParagraphs(text) {
  return String(text || '')
    .split(/\n+/)
    .map(t => t.trim())
    .filter(Boolean)
}

function isObjective(q) {
  return q.moduleType === 'reading' || q.moduleType === 'listening'
}

function optionsOf(q) {
  return ['A', 'B', 'C', 'D']
    .map(key => ({ key, text: q['option' + key] }))
    .filter(o => o.text)
}

function wordCount(text) {
  if (!text) return 0
  return String(text)
    .trim()
    .split(/\s+/)
    .filter(w => w).length
}

/* ---------------- 作答 ---------------- */
function pickOption(q, key) {
  // 再点一次同一个选项就取消选择，方便改答案
  answers[q.id] = answers[q.id] === key ? '' : key
}

/* ================================================================
 * 口语录音
 *  - MediaRecorder 录 webm（Safari 是 mp4），结束后立刻 POST /api/file/uploadAudio
 *  - 同时用浏览器 SpeechRecognition 做自动转写，转写文字填进作答框
 *    （后端评分引擎吃的是文本；不转写就没法评分）
 *  - 录音 URL 存 localStorage，刷新页面还能回放
 * ================================================================ */
const audioUrls = reactive({})     // questionId -> /upload/audio/xxx.webm
const transcripts = reactive({})   // questionId -> 自动转写文本
const recState = reactive({})      // '' | 'recording' | 'uploading'
const recLeft = reactive({})       // 本题剩余录制秒数
const recError = reactive({})      // 本题录音失败提示
const recNoAsr = reactive({})      // 本题浏览器给不了转写，需要手写
const recHandles = {}              // questionId -> { recorder, stream, chunks, recognition, timer }

const keyAudio = computed(() => 'fullExam:audio:' + recordId.value)

function persistAudio() {
  try {
    localStorage.setItem(keyAudio.value, JSON.stringify({ ...audioUrls }))
  } catch (e) {
    // 浏览器禁用存储时忽略
  }
}

watch(audioUrls, persistAudio, { deep: true })

/* 录音开始：取麦克风 → 起 MediaRecorder → 倒计时 → 顺便开转写 */
async function startRecord(q) {
  if (recState[q.id] === 'recording' || recState[q.id] === 'uploading') return
  if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia || typeof MediaRecorder === 'undefined') {
    ElMessage.error('当前浏览器不支持录音，请用 Chrome 或 Edge 打开')
    return
  }
  recError[q.id] = ''
  transcripts[q.id] = ''
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    // 挑一个浏览器支持的格式；后缀要和内容一致，否则成绩页播放会失败
    const mimeType = MediaRecorder.isTypeSupported('audio/webm')
      ? 'audio/webm'
      : (MediaRecorder.isTypeSupported('audio/mp4') ? 'audio/mp4' : '')
    const recorder = mimeType ? new MediaRecorder(stream, { mimeType }) : new MediaRecorder(stream)
    const chunks = []
    const handle = { recorder, stream, chunks, recognition: null, timer: null }
    recHandles[q.id] = handle

    recorder.ondataavailable = event => {
      if (event.data && event.data.size > 0) {
        chunks.push(event.data)
      }
    }
    // 停止后由 onstop 异步上传，这里传 handle 进去拿 chunks
    recorder.onstop = () => finishRecording(q, handle)

    recorder.start()
    recState[q.id] = 'recording'

    // 倒计时：limitSecond 就是这道题的作答时长，没配就按 60 秒
    const limit = Number(q.limitSecond) > 0 ? Number(q.limitSecond) : 60
    recLeft[q.id] = limit
    handle.timer = setInterval(() => {
      recLeft[q.id] -= 1
      if (recLeft[q.id] <= 0) {
        stopRecord(q)   // 到点自动结束，跟真考一致
      }
    }, 1000)

    startRecognition(q, handle)
  } catch (e) {
    recState[q.id] = ''
    recError[q.id] = '麦克风打不开：请点浏览器地址栏左侧的麦克风图标，允许本页面使用麦克风后重试。'
    ElMessage.error('麦克风权限获取失败')
  }
}

/* 结束录音：停计时、停转写、停 recorder（真正的上传在 onstop 里） */
function stopRecord(q) {
  const handle = recHandles[q.id]
  if (!handle || recState[q.id] !== 'recording') return
  if (handle.timer) {
    clearInterval(handle.timer)
  }
  if (handle.recognition) {
    try {
      handle.recognition.stop()
    } catch (e) {
      // 忽略
    }
  }
  try {
    handle.recorder.stop()
  } catch (e) {
    recState[q.id] = ''
  }
  recState[q.id] = 'uploading'
}

/* 录音片段到手 → 上传 → 存 URL；转写随后填进作答框 */
async function finishRecording(q, handle) {
  // 麦克风轨道必须关掉，否则浏览器一直显示"正在录音"
  handle.stream.getTracks().forEach(track => track.stop())
  const type = handle.recorder.mimeType || 'audio/webm'
  const blob = new Blob(handle.chunks, { type })
  if (blob.size === 0) {
    recState[q.id] = ''
    recError[q.id] = '没有录到声音，请检查麦克风是否被静音后重录。'
    delete recHandles[q.id]
    return
  }
  try {
    const url = await uploadAudio(blob, type)
    audioUrls[q.id] = url
    ElMessage.success('录音已保存')
    // 转写结果比录音晚一点到，等 1.2 秒：作答框还空就自动填进去
    setTimeout(() => {
      const text = (transcripts[q.id] || '').trim()
      if (text && !hasAnswer(answers[q.id])) {
        answers[q.id] = text
      }
    }, 1200)
  } catch (e) {
    recError[q.id] = '录音上传失败，请重试；这不会影响你手写作答。'
  } finally {
    recState[q.id] = ''
    delete recHandles[q.id]
  }
}

/* 上传到后端 /api/file/uploadAudio，返回 /upload/audio/xxx.webm */
async function uploadAudio(blob, mime) {
  const ext = mime.includes('mp4') ? 'mp4' : (mime.includes('ogg') ? 'ogg' : 'webm')
  const form = new FormData()
  form.append('file', blob, 'speaking_' + Date.now() + '.' + ext)
  form.append('ext', ext)
  const res = await request.post('/api/file/uploadAudio', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000
  })
  if (!res || res.code !== 200) {
    throw new Error((res && res.msg) || '上传失败')
  }
  return res.data
}

/* 自动转写：Chrome/Edge 可用；不可用时只提示手写，不影响录音 */
function startRecognition(q, handle) {
  const SR = window.SpeechRecognition || window.webkitSpeechRecognition
  if (!SR) {
    recNoAsr[q.id] = true
    return
  }
  try {
    const recognition = new SR()
    recognition.lang = 'en-US'
    recognition.continuous = true
    recognition.interimResults = true
    let finalText = ''
    recognition.onresult = event => {
      let interim = ''
      for (let i = event.resultIndex; i < event.results.length; i++) {
        const result = event.results[i]
        if (result.isFinal) {
          finalText += result[0].transcript + ' '
        } else {
          interim += result[0].transcript
        }
      }
      transcripts[q.id] = (finalText + interim).trim()
    }
    recognition.onerror = event => {
      // 国内网络下语音服务经常连不上：只标记"要手写"，不打断录音
      if (['network', 'service-not-allowed', 'not-allowed', 'audio-capture'].includes(event.error)) {
        recNoAsr[q.id] = true
      }
    }
    recognition.onend = () => {
      transcripts[q.id] = (transcripts[q.id] || '').trim()
    }
    recognition.start()
    handle.recognition = recognition
  } catch (e) {
    recNoAsr[q.id] = true
  }
}

/* 把转写文本填进评分框（改错了可以再手动改） */
function applyTranscript(q) {
  const text = (transcripts[q.id] || '').trim()
  if (!text) return
  answers[q.id] = text
  ElMessage.success('已填入作答框，可以再修改')
}

/* 删除这段录音（只删本机的引用；服务端文件保留，避免误删别人的） */
function clearRecord(q) {
  delete audioUrls[q.id]
  delete transcripts[q.id]
  delete recError[q.id]
}

function goModule(index) {
  if (index < 0 || index >= modules.value.length) return
  activeIndex.value = index
  currentQuestionId.value = null
  nextTick(() => {
    if (mainRef.value) {
      mainRef.value.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }
  })
}

function jumpTo(m, q) {
  const index = modules.value.findIndex(item => item.moduleType === m.moduleType)
  if (index >= 0) {
    activeIndex.value = index
  }
  currentQuestionId.value = q.id
  nextTick(() => {
    const el = document.getElementById('q' + q.id)
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'center' })
    }
  })
}

/* ---------------- 数据加载 ---------------- */
async function loadData() {
  if (!recordId.value) {
    errorText.value = '缺少模考记录 id，请从「真题套题」目录点开始模考进入。'
    return
  }
  try {
    // 兜底：query 里没带 suiteId 时，用记录列表反查一次
    if (!suiteId.value) {
      const records = await request.get('/api/fullExam/records')
      const list = (records && records.data) || []
      const hit = list.find(item => String(item.id) === String(recordId.value))
      if (hit) {
        suiteId.value = String(hit.suiteId)
      }
    }
    if (!suiteId.value) {
      errorText.value = '找不到这套题对应的套题 id，请回目录重新开始。'
      return
    }
    const res = await request.get('/api/fullSuite/' + suiteId.value)
    if (res.code !== 200) {
      errorText.value = res.msg || '套题加载失败'
      return
    }
    suiteTitle.value = res.data.title || ''
    modules.value = (res.data.modules || []).filter(m => (m.sections || []).length > 0)
    if (modules.value.length === 0) {
      errorText.value = '这套题没有可用题目，请检查 full_question 表数据。'
      return
    }
    restoreCache()
    startTimer()
  } catch (e) {
    errorText.value = '网络或服务异常，请确认后端已启动。'
  }
}

/* ---------------- 本地缓存 ---------------- */
function restoreCache() {
  const raw = localStorage.getItem(keyAnswers.value)
  if (raw) {
    try {
      const obj = JSON.parse(raw)
      Object.keys(obj).forEach(id => {
        answers[id] = obj[id]
      })
    } catch (e) {
      localStorage.removeItem(keyAnswers.value)
    }
  }
  // 已上传的录音地址也恢复，刷新后照样能回放
  const audioRaw = localStorage.getItem(keyAudio.value)
  if (audioRaw) {
    try {
      const obj = JSON.parse(audioRaw)
      Object.keys(obj).forEach(id => {
        if (obj[id]) {
          audioUrls[id] = obj[id]
        }
      })
    } catch (e) {
      localStorage.removeItem(keyAudio.value)
    }
  }
  const progress = Number(localStorage.getItem(keyProgress.value) || 0)
  if (progress > 0 && progress < modules.value.length) {
    activeIndex.value = progress
  }
}

function clearCache() {
  localStorage.removeItem(keyAnswers.value)
  localStorage.removeItem(keyProgress.value)
  localStorage.removeItem(keyStart.value)
  localStorage.removeItem(keyAudio.value)
}

/* 每次答题都落盘（deep watch 只在本页生效，体量很小） */
function persistAnswers() {
  try {
    localStorage.setItem(keyAnswers.value, JSON.stringify({ ...answers }))
  } catch (e) {
    // 浏览器禁用存储时静默跳过，不影响答题
  }
}

/* 每次答题都落盘（deep watch 只在本页生效，体量很小） */
watch(answers, persistAnswers, { deep: true })
watch(activeIndex, value => {
  try {
    localStorage.setItem(keyProgress.value, String(value))
  } catch (e) {
    // 忽略
  }
})

/* ---------------- 计时 ---------------- */
function startTimer() {
  const saved = Number(localStorage.getItem(keyStart.value) || 0)
  startedAt = saved > 0 ? saved : Date.now()
  if (!saved) {
    try {
      localStorage.setItem(keyStart.value, String(startedAt))
    } catch (e) {
      // 忽略
    }
  }
  elapsed.value = Math.max(0, Math.floor((Date.now() - startedAt) / 1000))
  timer = setInterval(() => {
    elapsed.value = Math.max(0, Math.floor((Date.now() - startedAt) / 1000))
  }, 1000)
}

/* ---------------- 提交 ---------------- */
async function submitExam() {
  if (submitting.value) return
  const unanswered = totalQuestions.value - answeredTotal.value
  // 口语只录了音、没转写文本 → 评分引擎拿不到内容，提交前提醒一下
  const speakingNoText = modules.value
    .filter(m => m.moduleType === 'speaking')
    .flatMap(m => flatQuestions(m))
    .filter(q => audioUrls[q.id] && !hasAnswer(answers[q.id])).length
  let message = unanswered > 0
    ? `还有 ${unanswered} 题没作答。未作答的模块会按 1.0 档计算，确定提交吗？`
    : '提交后立即判分且不能修改，确定提交吗？'
  if (speakingNoText > 0) {
    message = `有 ${speakingNoText} 道口语题只录了音、没有转写文本，这些题 AI 无法评分。\n建议回去把转写文本补上。仍然提交吗？`
  }
  const confirmed = await ElMessageBox.confirm(message, '提交整卷', {
    type: 'warning',
    confirmButtonText: '提交',
    cancelButtonText: '再检查一下'
  }).then(() => true).catch(() => false)
  if (!confirmed) return

  submitting.value = true
  try {
    const list = []
    // 录音地址也参与遍历：只录了音、还没转写的题，录音照样提交上去保存
    const allIds = Array.from(new Set([...Object.keys(answers), ...Object.keys(audioUrls)]))
    allIds.forEach(id => {
      const hasText = hasAnswer(answers[id])
      const hasAudio = !!audioUrls[id]
      if (!hasText && !hasAudio) return
      list.push({
        questionId: Number(id),
        userAnswer: hasText ? String(answers[id]) : '',
        // 口语录音一起带上，成绩页能回放；没有录音就是空字符串
        audioUrl: audioUrls[id] || ''
      })
    })
    const res = await request.post('/api/fullExam/submit', {
      recordId: Number(recordId.value),
      costSeconds: elapsed.value,
      answers: list
    })
    if (res.code !== 200) {
      ElMessage.error(res.msg || '提交失败')
      return
    }
    submitted = true
    clearCache()
    ElMessage.success('提交成功，正在打开成绩页')
    router.push({ path: '/full/result', query: { recordId: recordId.value } })
  } catch (e) {
    // 拦截器已经提示过
  } finally {
    submitting.value = false
  }
}

async function quitExam() {
  const confirmed = await ElMessageBox.confirm(
    '退出后已作答内容仍保存在本机，下次从「真题套题」页面点继续作答可以接着做。确定退出吗？',
    '退出模考',
    { type: 'info', confirmButtonText: '退出', cancelButtonText: '继续答题' }
  ).then(() => true).catch(() => false)
  if (confirmed) {
    router.push('/full')
  }
}

/* 关页面/刷新时提醒一次，避免整卷没交就丢进度 */
function handleBeforeUnload(event) {
  if (submitted) return
  if (answeredTotal.value === 0) return
  event.preventDefault()
  event.returnValue = '答案还没提交，确定离开吗？'
  return event.returnValue
}

onMounted(() => {
  loadData()
  window.addEventListener('beforeunload', handleBeforeUnload)
})

onBeforeUnmount(() => {
  if (timer) {
    clearInterval(timer)
  }
  // 离开页面时把还在录的音停掉，避免麦克风一直被占用
  Object.keys(recHandles).forEach(id => {
    const handle = recHandles[id]
    if (!handle) return
    if (handle.timer) clearInterval(handle.timer)
    if (handle.recognition) {
      try {
        handle.recognition.stop()
      } catch (e) {
        // 忽略
      }
    }
    try {
      if (handle.recorder && handle.recorder.state === 'recording') handle.recorder.stop()
    } catch (e) {
      // 忽略
    }
    handle.stream.getTracks().forEach(track => track.stop())
  })
  window.removeEventListener('beforeunload', handleBeforeUnload)
})
</script>

<style scoped>
.exam-wrap {
  max-width: 1180px;
  margin: 0 auto;
  padding: 20px 24px 60px;
}

/* ---------- 顶部条 ---------- */
.exam-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 22px;
  flex-wrap: wrap;
}

.exam-title {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.3px;
  margin-bottom: 8px;
}

.exam-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.exam-top__right {
  display: flex;
  gap: 10px;
}

/* ---------- 模块步骤条 ---------- */
.steps {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0;
  margin: 14px 0 18px;
  padding: 6px;
  overflow: hidden;
}

.step {
  appearance: none;
  border: 0;
  background: transparent;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border-radius: 12px;
  cursor: pointer;
  text-align: left;
  transition: background 0.16s ease;
}

.step:hover { background: var(--brand-50); }

.step.is-active { background: var(--brand-900); }
.step.is-active .step__index { background: var(--accent-500); color: #fff; }
.step.is-active strong,
.step.is-active small { color: #fff; }

.step__index {
  width: 26px;
  height: 26px;
  flex: none;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-size: 13px;
  font-weight: 700;
  background: var(--surface-3);
  color: var(--ink-700);
}

.step.is-done .step__index {
  background: #e7f7ef;
  color: var(--success);
}

.step__text { display: flex; flex-direction: column; line-height: 1.25; min-width: 0; }
.step__text strong { font-size: 14px; color: var(--ink-900); }
.step__text small { font-size: 11px; color: var(--ink-500); margin-top: 2px; }

/* ---------- 主体两栏 ---------- */
.exam-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 268px;
  gap: 18px;
  align-items: start;
}

.exam-main { min-width: 0; }
.exam-section { display: flex; flex-direction: column; gap: 14px; }

/* ---------- 材料区 ---------- */
.material { padding: 22px 24px; }

.material__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.material__title {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
}

.material__para {
  margin: 0 0 14px;
  font-size: 15px;
  line-height: 1.85;
  color: var(--ink-700);
  text-align: justify;
}

.material__hint { margin: 0 0 14px; font-size: 13px; color: var(--ink-500); }
.material__note { margin: 12px 0 0; font-size: 12px; color: var(--ink-300); }

.audio {
  width: 100%;
  height: 44px;
  border-radius: 12px;
  background: var(--surface-2);
  padding: 4px 6px;
}

/* ---------- 题目卡片 ---------- */
.question {
  padding: 20px 22px 18px;
  border-left: 3px solid transparent;
  transition: border-color 0.16s ease, box-shadow 0.16s ease;
}

.question.is-answered { border-left-color: var(--success); }

.question__head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.question__no {
  display: inline-grid;
  place-items: center;
  min-width: 34px;
  height: 24px;
  padding: 0 8px;
  border-radius: 8px;
  background: var(--brand-50);
  color: var(--brand-600);
  font-size: 12px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}

.question__title {
  margin: 0 0 14px;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.6;
  color: var(--ink-900);
}

.options { display: flex; flex-direction: column; gap: 8px; }

.option {
  appearance: none;
  display: flex;
  align-items: flex-start;
  gap: 12px;
  width: 100%;
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid var(--line);
  background: #fff;
  cursor: pointer;
  text-align: left;
  transition: border-color 0.15s ease, background 0.15s ease, box-shadow 0.15s ease;
}

.option:hover { border-color: var(--brand-100); background: var(--brand-50); }

.option.is-selected {
  border-color: var(--brand-600);
  background: var(--brand-50);
  box-shadow: 0 0 0 2px rgba(43, 74, 143, 0.08);
}

.option__key {
  width: 24px;
  height: 24px;
  flex: none;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-size: 12px;
  font-weight: 800;
  background: var(--surface-3);
  color: var(--ink-700);
}

.option.is-selected .option__key { background: var(--brand-600); color: #fff; }

.option__text { font-size: 14px; line-height: 1.6; color: var(--ink-700); }

/* ---------- 主观题 ---------- */
.question__prompt {
  margin: 0 0 12px;
  font-size: 14px;
  line-height: 1.8;
  color: var(--ink-700);
  white-space: pre-wrap;
}

.question__tip { margin: 0 0 12px; font-size: 12px; color: var(--ink-500); }

.question__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
}

/* ---------- 口语录音 ---------- */
.rec {
  margin-bottom: 14px;
  padding: 16px;
  border-radius: var(--radius-md);
  border: 1px solid #ffe0cc;
  background: #fff8f2;
}

.rec__head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.rec__title { font-size: 13px; font-weight: 700; color: #b45a1b; }

.pill--rec {
  background: #fdecec;
  color: #c62828;
  font-variant-numeric: tabular-nums;
}

.rec__actions { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }

.rec__audio {
  width: 100%;
  height: 42px;
  margin-top: 12px;
  border-radius: 10px;
  background: #fff;
  padding: 3px 5px;
}

.rec__hint {
  margin: 12px 0 0;
  font-size: 12px;
  line-height: 1.8;
  color: var(--ink-500);
}

.rec__error {
  margin: 10px 0 0;
  font-size: 12px;
  line-height: 1.8;
  color: var(--danger);
}

.rec__transcript {
  margin-top: 12px;
  padding: 12px 14px;
  border-radius: 10px;
  background: #fff;
  border: 1px dashed #f0c9a8;
}

.rec__transcript-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.rec__label { font-size: 11px; font-weight: 700; color: #b45a1b; }

.rec__transcript-text {
  margin: 0;
  font-size: 13px;
  line-height: 1.8;
  color: var(--ink-700);
  white-space: pre-wrap;
}

.counter { font-size: 12px; color: var(--ink-500); font-variant-numeric: tabular-nums; }
.rubric-link { font-size: 12px; color: var(--brand-600); cursor: pointer; }
.rubric { font-size: 12px; line-height: 1.8; color: var(--ink-700); max-height: 320px; overflow: auto; }

/* ---------- 底部操作 ---------- */
.exam-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 18px;
  padding-bottom: 8px;
}

/* ---------- 右侧答题卡 ---------- */
.exam-aside {
  position: sticky;
  top: 84px;
  padding: 18px;
}

.aside__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 12px;
}

.aside__head h4 { margin: 0; font-size: 14px; font-weight: 700; }
.aside__hint { font-size: 11px; color: var(--ink-300); }

.aside-group { margin-bottom: 12px; }

.aside-group__title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  font-weight: 600;
  color: var(--ink-700);
  margin-bottom: 6px;
}

.aside-group__title small { color: var(--ink-300); font-weight: 500; }

.aside-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 6px;
}

.dot {
  appearance: none;
  border: 1px solid var(--line);
  background: #fff;
  border-radius: 8px;
  height: 28px;
  font-size: 11px;
  font-weight: 700;
  color: var(--ink-500);
  cursor: pointer;
  font-variant-numeric: tabular-nums;
  transition: all 0.15s ease;
}

.dot:hover { border-color: var(--brand-100); color: var(--brand-600); }
.dot.is-answered { background: var(--brand-50); border-color: var(--brand-100); color: var(--brand-700); }
.dot.is-current { background: var(--brand-900); border-color: var(--brand-900); color: #fff; }

.aside__foot {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid var(--line);
}

.aside__foot p {
  margin: 0 0 10px;
  font-size: 11px;
  line-height: 1.7;
  color: var(--ink-300);
}

.exam-error { color: var(--danger); font-size: 14px; }

/* ---------- 窄屏 ---------- */
@media (max-width: 960px) {
  .exam-body { grid-template-columns: minmax(0, 1fr); }
  .exam-aside { position: static; order: -1; }
  .aside-grid { grid-template-columns: repeat(8, 1fr); }
  .steps { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 560px) {
  .exam-wrap { padding: 12px 12px 40px; }
  .exam-top { padding: 14px 16px; }
  .question, .material { padding: 16px; }
}
</style>
