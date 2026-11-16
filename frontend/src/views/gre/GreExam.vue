<template>
  <AppShell>
    <div class="page exam">
      <!-- 顶部：标题 + 计时 + 提交 -->
      <div class="exam-head card">
        <div class="exam-head__left">
          <el-button text @click="leave">← 返回 GRE 首页</el-button>
          <h1 class="exam-head__title">{{ suite.title || 'GRE 全真模考' }}</h1>
          <p class="exam-head__sub">
            答案随点随存（存在本机缓存），三个部分都做完再一次性提交；提交后由后端统一判分，
            这里不会提前显示对错。
          </p>
        </div>
        <div class="exam-head__right">
          <div class="timer">
            <span>本部分剩余</span>
            <strong :class="{ 'is-urgent': currentUrgent }">{{ moduleCountdownText }}</strong>
            <small class="timer__total">总用时 {{ elapsedText }}</small>
          </div>
          <el-button type="primary" round :loading="submitting" @click="submitExam()">提交整卷</el-button>
        </div>
      </div>

      <!-- 模块切换：每个模块独立倒计时，进入即开始，时间到自动锁定并跳到下一部分 -->
      <div class="module-tabs">
        <button
          v-for="mod in modules"
          :key="mod.moduleType"
          type="button"
          :class="['module-tab', { 'is-active': activeModule === mod.moduleType, 'is-locked': moduleLocked[mod.moduleType] }]"
          @click="switchModule(mod.moduleType)"
        >
          <strong>{{ moduleLabel(mod.moduleType) }}</strong>
          <small>{{ answeredInModule(mod) }} / {{ mod.questionCount }} 已作答</small>
          <small v-if="moduleLocked[mod.moduleType]" class="module-tab__lock">时间到 · 已锁定</small>
          <small v-else-if="moduleLimit[mod.moduleType] > 0" class="module-tab__left">
            剩余 {{ formatClock(moduleRemain[mod.moduleType]) }}
          </small>
        </button>
      </div>

      <!-- 当前模块内容 -->
      <section v-loading="loading" class="module-body">
        <template v-for="mod in modules" :key="mod.moduleType">
          <div v-if="activeModule === mod.moduleType" class="module-content">
            <div v-if="moduleLocked[mod.moduleType]" class="time-up card">
              <div class="time-up__text">
                <strong>本部分计时已结束</strong>
                <span>这一部分已锁定，不能再改答案；已作答的内容会随整卷一起提交。</span>
              </div>
            </div>
            <div v-for="section in mod.sections" :key="section.sectionId" class="section-block card">
              <header class="section-head">
                <h3>{{ section.title }}</h3>
                <span class="muted small">{{ section.questionCount }} 题 · {{ section.hint }}</span>
              </header>

              <div v-if="section.contentText" class="passage">
                <p v-for="(para, i) in paragraphs(section.contentText)" :key="i">{{ para }}</p>
              </div>

              <div class="q-list">
                <article v-for="q in section.questions" :key="q.id" class="q-item">
                  <div class="q-item__head">
                    <span class="q-no">{{ q.sortIndex }}</span>
                    <span class="q-type">{{ typeLabel(q.taskType) }}</span>
                  </div>

                  <p v-if="q.questionTitle" class="q-title">{{ q.questionTitle }}</p>

                  <template v-if="isWriting(q)">
                    <p class="q-prompt">{{ q.prompt }}</p>
                    <el-input
                      v-model="answers[q.id]"
                      type="textarea"
                      :rows="9"
                      :disabled="moduleLocked[activeModule]"
                      resize="vertical"
                      placeholder="用英文写作（Issue 建议 400 词以上，Argument 建议 350 词以上）"
                    />
                    <div class="editor-foot">
                      <span class="muted">{{ wordCount(answers[q.id]) }} 词</span>
                      <span class="muted">{{ q.scoreStandard }}</span>
                    </div>
                  </template>

                  <template v-else>
                    <div class="options">
                      <button
                        v-for="opt in q.options"
                        :key="opt.key"
                        type="button"
                        :class="['option', { 'is-selected': answers[q.id] === opt.key }]"
                        :disabled="moduleLocked[activeModule]"
                        @click="chooseOption(q, opt.key)"
                      >
                        <span class="option__key">{{ opt.key }}</span>
                        <span class="option__text">{{ opt.text }}</span>
                      </button>
                    </div>
                  </template>
                </article>
              </div>
            </div>

            <div v-if="mod.sections.length === 0" class="empty card card-pad">这个模块还没有题目。</div>
          </div>
        </template>
      </section>

      <!-- 底部进度 + 提交 -->
      <div class="exam-foot card">
        <div class="progress-text">
          <strong>{{ answeredCount }}</strong>
          <span class="muted"> / {{ totalQuestions }} 题已作答</span>
          <span v-if="unansweredCount" class="pill pill--warn">还有 {{ unansweredCount }} 题没做</span>
        </div>
        <div class="exam-foot__actions">
          <el-button round @click="leave">暂存并退出</el-button>
          <el-button type="primary" round :loading="submitting" @click="submitExam">提交并判分</el-button>
        </div>
      </div>
    </div>
  </AppShell>
</template>

<script setup>
/**
 * GRE 套题作答页（/gre/exam?recordId=&suiteId=）
 *
 * 数据：GET /api/greSuite/{suiteId}（题目已脱敏，不含答案与解析）
 * 提交：POST /api/greExam/submit（整卷一次性判分）
 *
 * 缓存：greExam:answers:<recordId>，刷新或"暂存退出"后回来接着做。
 * 三个模块（Writing / Verbal / Quant）都在这一个页面完成，答案不会实时判分。
 */
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import AppShell from '@/components/AppShell.vue'

const route = useRoute()
const router = useRouter()

const recordId = route.query.recordId
const suiteId = route.query.suiteId

const suite = ref({})
const modules = computed(() => suite.value.modules || [])
const loading = ref(false)
const submitting = ref(false)
const activeModule = ref('writing')
const answers = reactive({})

const seconds = ref(0)          // 整卷总用时（向上累计，提交时作为 costSeconds 上报）
let timer = null

/* ---------------- 计时：和真实 GRE 一样，进入某一部分即自动倒计时，不暂停 ---------------- */
const moduleRemain = reactive({})   // moduleType -> 该部分剩余秒
const moduleLimit = reactive({})    // moduleType -> 该部分总时限（后端 section.limitSecond 之和）
const moduleLocked = reactive({})   // moduleType -> 时间到即锁定

const currentUrgent = computed(() =>
  (moduleRemain[activeModule.value] || 0) > 0 && (moduleRemain[activeModule.value] || 0) <= 60
)
const moduleCountdownText = computed(() => formatClock(moduleRemain[activeModule.value] || 0))

function formatClock(total) {
  const value = Math.max(0, Math.floor(Number(total) || 0))
  const h = Math.floor(value / 3600)
  const m = Math.floor((value % 3600) / 60)
  const s = value % 60
  const mm = String(m).padStart(2, '0')
  const ss = String(s).padStart(2, '0')
  return h > 0 ? `${h}:${mm}:${ss}` : `${mm}:${ss}`
}

const moduleOrder = ['writing', 'verbal', 'quant']

/** 按各 section 的限时之和，给每个模块初始化时限 */
function initModuleTimers() {
  modules.value.forEach(mod => {
    const total = (mod.sections || []).reduce(
      (sum, section) => sum + (Number(section.limitSecond) || 0),
      0
    )
    moduleLimit[mod.moduleType] = total
    moduleRemain[mod.moduleType] = total
    moduleLocked[mod.moduleType] = false
  })
}

/** 时间到的处理：锁定当前部分，自动跳到下一个还有时间的部分；全做完就自动交卷 */
function advanceAfterTimeout() {
  const lockedType = activeModule.value
  ElMessage.warning(`${moduleLabel(lockedType)} 时间到，已锁定`)
  const idx = moduleOrder.indexOf(lockedType)
  for (let i = idx + 1; i < moduleOrder.length; i++) {
    const type = moduleOrder[i]
    if ((moduleLimit[type] || 0) > 0 && !moduleLocked[type]) {
      activeModule.value = type
      return
    }
  }
  // 后面没有可做的部分了：整卷自动提交（跳过「还有题没做」确认）
  ElMessage.info('全部部分计时结束，自动提交整卷')
  submitExam(true)
}

function tick() {
  seconds.value += 1
  const type = activeModule.value
  if (moduleLocked[type]) return
  const total = moduleLimit[type] || 0
  if (total <= 0) return          // 没配时限的模块不计时
  moduleRemain[type] = Math.max(0, moduleRemain[type] - 1)
  if (moduleRemain[type] === 0) advanceAfterTimeout()
}

/** 切模块：不重置计时，切回来继续倒计时（模拟真实考试「时间一直在走」） */
function switchModule(type) {
  if (activeModule.value === type) return
  activeModule.value = type
}

function chooseOption(q, key) {
  if (moduleLocked[activeModule.value]) return
  answers[q.id] = key
}

const elapsedText = computed(() => {
  const h = Math.floor(seconds.value / 3600)
  const m = Math.floor((seconds.value % 3600) / 60)
  const s = seconds.value % 60
  return h > 0
    ? `${h}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
    : `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
})

const allQuestions = computed(() => {
  const list = []
  modules.value.forEach(mod => {
    (mod.sections || []).forEach(section => {
      (section.questions || []).forEach(q => list.push(q))
    })
  })
  return list
})

const totalQuestions = computed(() => allQuestions.value.length)

const answeredCount = computed(() =>
  allQuestions.value.filter(q => hasAnswer(q)).length
)

const unansweredCount = computed(() => totalQuestions.value - answeredCount.value)

function moduleLabel(moduleType) {
  if (moduleType === 'writing') return 'Analytical Writing'
  if (moduleType === 'verbal') return 'Verbal Reasoning'
  if (moduleType === 'quant') return 'Quantitative Reasoning'
  return moduleType
}

function typeLabel(taskType) {
  const map = {
    text_completion: 'Text Completion',
    sentence_equivalence: 'Sentence Equivalence',
    reading: 'Reading Comprehension',
    quant_compare: 'Quantitative Comparison',
    quant_problem: 'Problem Solving',
    awa_issue: 'AWA · Issue',
    awa_argument: 'AWA · Argument'
  }
  return map[taskType] || taskType || '题目'
}

function isWriting(q) {
  return q.taskType && String(q.taskType).startsWith('awa')
}

function hasAnswer(q) {
  const value = answers[q.id]
  return isWriting(q) ? !!String(value || '').trim() : !!value
}

function wordCount(text) {
  if (!text) return 0
  return String(text).split(/\s+/).filter(t => /[A-Za-z0-9'’-]+/.test(t)).length
}

function paragraphs(text) {
  return String(text || '').split(/\n+/).filter(p => p.trim().length > 0)
}

function answeredInModule(mod) {
  let count = 0
  ;(mod.sections || []).forEach(section => {
    ;(section.questions || []).forEach(q => {
      if (hasAnswer(q)) count += 1
    })
  })
  return count
}

/* ---------------- 缓存 ---------------- */
const cacheKey = 'greExam:answers:' + recordId

function restoreCache() {
  const raw = localStorage.getItem(cacheKey)
  if (!raw) return
  try {
    const saved = JSON.parse(raw)
    Object.keys(saved).forEach(k => { answers[k] = saved[k] })
  } catch (e) {
    localStorage.removeItem(cacheKey)
  }
}

watch(answers, () => {
  localStorage.setItem(cacheKey, JSON.stringify(answers))
}, { deep: true })

/* ---------------- 加载套题 ---------------- */
async function loadSuite() {
  loading.value = true
  try {
    const res = await request.get('/api/greSuite/' + suiteId)
    if (res.code === 200) {
      suite.value = res.data || {}
      // 按 section 限时给每个模块算总时限，然后停在第一个有题的模块
      initModuleTimers()
      const first = (suite.value.modules || []).find(m => m.questionCount > 0)
      if (first) activeModule.value = first.moduleType
    } else {
      ElMessage.error(res.msg || '套题加载失败')
    }
  } catch (e) {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

/* ---------------- 提交 ---------------- */
/**
 * @param {boolean} autoSubmit 计时到点自动交卷时为 true，跳过「还有题没做」的二次确认
 */
async function submitExam(autoSubmit = false) {
  if (submitting.value) return
  if (unansweredCount.value > 0 && !autoSubmit) {
    try {
      await ElMessageBox.confirm(
        `还有 ${unansweredCount.value} 题没作答，提交后未作答的题会显示为「我的答案：空」。确定提交吗？`,
        '还有题没做完',
        { type: 'warning', confirmButtonText: '仍然提交', cancelButtonText: '继续做题' }
      )
    } catch (e) {
      return
    }
  }

  submitting.value = true
  try {
    const list = []
    allQuestions.value.forEach(q => {
      if (!hasAnswer(q)) return
      list.push({ questionId: q.id, userAnswer: String(answers[q.id]) })
    })
    const res = await request.post('/api/greExam/submit', {
      recordId: Number(recordId),
      costSeconds: seconds.value,
      answers: list
    })
    if (res.code !== 200) {
      ElMessage.error(res.msg || '提交失败')
      return
    }
    localStorage.removeItem(cacheKey)
    ElMessage.success(`已判分：总分 ${res.data.totalScore}（V ${res.data.verbalScaled} / Q ${res.data.quantScaled} / AWA ${res.data.awaScore ?? '—'}）`)
    router.push({ path: '/gre/result', query: { recordId } })
  } catch (e) {
    // 拦截器已提示
  } finally {
    submitting.value = false
  }
}

function leave() {
  router.push('/gre')
}

onMounted(() => {
  restoreCache()
  loadSuite()
  // 进入页面即开始计时：每秒推进总用时 + 当前部分倒计时
  timer = setInterval(tick, 1000)
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.exam-head {
  display: flex; align-items: flex-start; justify-content: space-between;
  gap: 18px; padding: 20px 24px; margin-bottom: 16px;
}
.exam-head__title { margin: 8px 0 4px; font-size: 24px; letter-spacing: -0.4px; }
.exam-head__sub { margin: 0; font-size: 13px; line-height: 1.7; color: var(--ink-500); max-width: 720px; }
.exam-head__right { display: flex; flex-direction: column; align-items: flex-end; gap: 10px; flex: none; }
.timer { display: flex; flex-direction: column; align-items: flex-end; }
.timer span { font-size: 11px; color: var(--ink-500); }
.timer strong { font-size: 22px; font-variant-numeric: tabular-nums; color: var(--brand-700); }
.timer strong.is-urgent { color: #c0392b; }
.timer__total { font-size: 11px; color: var(--ink-300); font-variant-numeric: tabular-nums; }

.time-up {
  display: flex; align-items: center; justify-content: space-between; gap: 16px;
  padding: 14px 20px; border-left: 4px solid #c0392b; background: #fdf1f1;
}
.time-up__text { display: flex; flex-direction: column; gap: 4px; }
.time-up__text strong { font-size: 13.5px; color: #a52e2e; }
.time-up__text span { font-size: 12.5px; color: #8a3a3a; }

.module-tabs { display: flex; gap: 10px; margin-bottom: 16px; flex-wrap: wrap; }
.module-tab {
  appearance: none; cursor: pointer; border: 1px solid var(--line);
  background: #fff; border-radius: var(--radius-md); padding: 10px 16px;
  display: flex; flex-direction: column; gap: 2px; text-align: left;
}
.module-tab strong { font-size: 13px; }
.module-tab small { font-size: 11px; color: var(--ink-500); }
.module-tab.is-active { border-color: var(--brand-500); background: var(--brand-50); }
.module-tab.is-active strong { color: var(--brand-700); }
.module-tab.is-locked { opacity: 0.75; border-style: dashed; cursor: default; }
.module-tab__lock { color: #a52e2e !important; font-weight: 700; }
.module-tab__left { font-variant-numeric: tabular-nums; color: var(--brand-700) !important; }

.module-body { min-height: 200px; }
.module-content { display: flex; flex-direction: column; gap: 16px; }

.section-block { padding: 20px 24px; }
.section-head { display: flex; align-items: baseline; gap: 12px; margin-bottom: 12px; }
.section-head h3 { margin: 0; font-size: 17px; }

.passage {
  padding: 14px 16px; border-radius: var(--radius-md);
  background: var(--surface-2); border: 1px solid var(--line); margin-bottom: 14px;
}
.passage p { margin: 0 0 10px; line-height: 1.85; font-size: 14px; color: var(--ink-700); }
.passage p:last-child { margin-bottom: 0; }

.q-list { display: flex; flex-direction: column; gap: 18px; }
.q-item { padding-top: 14px; border-top: 1px dashed var(--line); }
.q-item:first-child { padding-top: 0; border-top: 0; }
.q-item__head { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.q-no {
  width: 24px; height: 24px; border-radius: 50%; display: grid; place-items: center;
  background: var(--brand-50); color: var(--brand-700); font-size: 12px; font-weight: 800;
}
.q-type { font-size: 11px; color: var(--ink-500); text-transform: uppercase; letter-spacing: 0.4px; }
.q-title { margin: 0 0 12px; font-size: 14.5px; line-height: 1.75; white-space: pre-line; }
.q-prompt {
  white-space: pre-line; margin: 0 0 12px; padding: 12px 14px;
  border-radius: var(--radius-md); background: var(--surface-2);
  border: 1px solid var(--line); font-size: 13.5px; line-height: 1.8;
}
.options { display: flex; flex-direction: column; gap: 8px; }
.option {
  appearance: none; text-align: left; display: flex; gap: 10px; align-items: flex-start;
  padding: 10px 14px; border-radius: var(--radius-md); border: 1px solid var(--line);
  background: #fff; cursor: pointer; font-size: 14px; line-height: 1.6;
}
.option:hover:not(:disabled) { border-color: var(--brand-100); background: var(--brand-50); }
.option:disabled { cursor: not-allowed; opacity: 0.72; }
.option.is-selected { border-color: var(--brand-500); background: var(--brand-50); }
.option__key {
  flex: none; width: 22px; height: 22px; border-radius: 50%; display: grid; place-items: center;
  font-size: 12px; font-weight: 800; color: var(--ink-500); background: var(--surface-3);
}
.option.is-selected .option__key { background: var(--brand-500); color: #fff; }
.editor-foot { display: flex; justify-content: space-between; gap: 16px; margin-top: 8px; font-size: 12px; }

.exam-foot {
  margin-top: 18px; padding: 14px 22px; display: flex; align-items: center;
  justify-content: space-between; gap: 16px;
}
.progress-text { display: flex; align-items: center; gap: 8px; font-size: 13px; }
.progress-text strong { font-size: 20px; color: var(--brand-700); font-variant-numeric: tabular-nums; }
.exam-foot__actions { display: flex; gap: 10px; }

.pill--warn { background: var(--accent-100); color: var(--accent-600); font-weight: 700; }
.muted { color: var(--ink-300); }
.small { font-size: 12px; }

@media (max-width: 860px) {
  .exam-head { flex-direction: column; }
  .exam-head__right { align-items: flex-start; }
  .exam-foot { flex-direction: column; align-items: flex-start; }
}
</style>
