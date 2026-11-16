<template>
  <AppShell>
    <div v-loading="loading" class="page result-page">
      <!-- ============ 成绩总览 ============ -->
      <section class="hero card">
        <div class="hero__left">
          <span class="pill pill--muted">{{ data.suiteTitle || record.suiteTitle }}</span>
          <div class="hero__band">
            <span class="hero__band-num">{{ record.totalBand != null ? record.totalBand : '—' }}</span>
            <span class="hero__band-max">/ 6</span>
          </div>
          <p class="hero__comment">{{ data.totalComment || '提交后由后端统一判分' }}</p>
          <div class="hero__tags">
            <span class="pill">CEFR {{ data.cefr || '—' }}</span>
            <span class="pill pill--accent">兼容分 {{ record.legacyScore || 0 }} / 120</span>
            <span class="pill pill--muted">用时 {{ durationText }}</span>
            <span class="pill" :class="record.status === 'finished' ? 'pill--ok' : 'pill--muted'">
              {{ record.status === 'finished' ? '已完成' : '作答中' }}
            </span>
          </div>
        </div>

        <div class="hero__right">
          <div class="band-grid">
            <div v-for="item in bands" :key="item.key" class="band-cell">
              <div class="band-cell__label">{{ item.label }}</div>
              <div class="band-cell__value">{{ item.band != null ? item.band : '—' }}<small>/6</small></div>
              <div class="band-cell__extra">{{ item.extra }}</div>
              <div class="band-cell__bar">
                <span :style="{ width: bandPercent(item.band) }"></span>
              </div>
            </div>
          </div>

          <div class="hero__actions">
            <el-button round @click="backList">返回套题目录</el-button>
            <el-button type="primary" round :loading="restarting" @click="retake">再考一次</el-button>
          </div>
        </div>
      </section>

      <!-- ============ 模块切换 ============ -->
      <div class="module-tabs card">
        <button
          v-for="item in moduleTabs"
          :key="item.key"
          type="button"
          :class="['tab', { 'is-active': item.key === activeModule }]"
          @click="activeModule = item.key"
        >
          <strong>{{ item.label }}</strong>
          <small>{{ item.band != null ? item.band + '/6' : '—' }} · 作答 {{ item.answered }}/{{ item.count }} 题</small>
        </button>
      </div>

      <!-- ============ 逐题回顾 ============ -->
      <section class="review">
        <!-- 材料（阅读正文 / 听力音频与原文） -->
        <article v-for="section in activeSections" :key="section.sectionId" class="material card">
          <div class="material__head">
            <h3>{{ section.title }}</h3>
            <span class="pill pill--muted">{{ moduleLabel(section.moduleType) }}材料</span>
          </div>
          <audio v-if="section.audioUrl" class="audio" :src="section.audioUrl" controls preload="none"></audio>
          <p
            v-for="(para, pi) in splitParagraphs(section.contentText)"
            :key="pi"
            class="material__para"
          >{{ para }}</p>
        </article>

        <div v-if="activeAnswers.length === 0" class="empty card card-pad">
          这个模块没有作答记录{{ activeModule === 'reading' || activeModule === 'listening' ? '，或者题目未作答。' : '。' }}
        </div>

        <article
          v-for="ans in activeAnswers"
          :key="ans.questionId"
          :class="['ans card', { 'is-wrong': ans.isCorrect === 0, 'is-right': ans.isCorrect === 1 }]"
        >
          <div class="ans__head">
            <span class="ans__no">Q{{ ans.sortIndex }}</span>
            <span class="pill pill--muted">{{ moduleLabel(ans.moduleType) }}</span>
            <span v-if="ans.isCorrect === 1" class="pill pill--ok">正确</span>
            <span v-else-if="ans.isCorrect === 0" class="pill pill--danger">错误</span>
            <span v-else-if="ans.band != null" class="pill pill--accent">AI {{ ans.band }}/6</span>
            <span v-if="!ans.userAnswer" class="pill pill--muted">未作答</span>
          </div>

          <p class="ans__title">{{ ans.questionTitle }}</p>

          <!-- 客观题回顾 -->
          <template v-if="ans.moduleType === 'reading' || ans.moduleType === 'listening'">
            <div class="ans__compare">
              <div :class="['compare-item', ans.isCorrect === 1 ? 'is-right' : 'is-wrong']">
                <span class="compare-item__label">我的答案</span>
                <span class="compare-item__value">{{ ans.userAnswer ? optionText(ans, ans.userAnswer) : '空（未作答）' }}</span>
              </div>
              <div class="compare-item is-right">
                <span class="compare-item__label">正确答案</span>
                <span class="compare-item__value">{{ optionText(ans, ans.correctAnswer) }}</span>
              </div>
            </div>
            <!-- 没作答也给解析：整卷提交后所有题目与答案一律可见 -->
            <div v-if="ans.analysis" class="ans__analysis">
              <span class="ans__analysis-label">解析</span>
              <p>{{ ans.analysis }}</p>
            </div>
          </template>

          <!-- 主观题回顾 -->
          <template v-else>
            <p v-if="ans.prompt" class="ans__prompt">{{ ans.prompt }}</p>
            <div :class="['ans__answer', { 'is-empty': !ans.userAnswer }]">
              <span class="ans__answer-label">我的作答</span>
              <p>{{ ans.userAnswer || '空（未作答）' }}</p>
            </div>

            <!-- 口语录音回放：在考试页录好的音会存进 audio_url，这里直接播放 -->
            <div v-if="ans.moduleType === 'speaking' && ans.audioUrl" class="ans__audio">
              <span class="ans__answer-label">我的录音</span>
              <audio :src="ans.audioUrl" controls preload="none"></audio>
            </div>

            <!-- AI 反馈（结构：band + dimensions + strengths + improvements） -->
            <div v-if="feedbackOf(ans)" class="ai">
              <div class="ai__head">
                <span class="ai__title">AI 评分反馈</span>
                <span class="pill pill--accent">{{ feedbackOf(ans).band }}/6</span>
                <span class="ai__engine">{{ feedbackOf(ans).engine || 'local rubric' }}</span>
              </div>

              <div class="ai__dims">
                <div v-for="dim in feedbackOf(ans).dimensions || []" :key="dim.name" class="dim">
                  <div class="dim__top">
                    <span class="dim__name">{{ dim.name }}</span>
                    <span class="dim__score">{{ dim.score }} / {{ dim.max }}</span>
                  </div>
                  <div class="dim__bar">
                    <span :style="{ width: dimPercent(dim) }"></span>
                  </div>
                  <p class="dim__comment">{{ dim.comment }}</p>
                </div>
              </div>

              <div class="ai__lists">
                <div class="ai__list">
                  <h5>做得好的地方</h5>
                  <ul>
                    <li v-for="(s, i) in feedbackOf(ans).strengths || []" :key="i">{{ s }}</li>
                    <li v-if="(feedbackOf(ans).strengths || []).length === 0" class="dim">暂无</li>
                  </ul>
                </div>
                <div class="ai__list">
                  <h5>下一步怎么练</h5>
                  <ul>
                    <li v-for="(s, i) in feedbackOf(ans).improvements || []" :key="i">{{ s }}</li>
                    <li v-if="(feedbackOf(ans).improvements || []).length === 0" class="dim">暂无</li>
                  </ul>
                </div>
              </div>

              <p v-if="feedbackOf(ans).stats" class="ai__stats">
                词数 {{ feedbackOf(ans).stats.words }}（目标 {{ feedbackOf(ans).stats.targetWords }}） ·
                句子 {{ feedbackOf(ans).stats.sentences }} ·
                平均句长 {{ feedbackOf(ans).stats.avgSentenceLen }} 词 ·
                词汇多样性 {{ feedbackOf(ans).stats.typeTokenRatio }} ·
                过渡词 {{ feedbackOf(ans).stats.connectives }} 处
              </p>
            </div>

            <!-- 满分范例：口语/写作专用，无论是否作答都展示，方便照着练 -->
            <div class="ans__sample">
              <div class="ans__sample-head">
                <span class="ans__sample-label">满分范例 · Model Answer</span>
                <span v-if="ans.sampleAnswer" class="ans__sample-meta">{{ sampleMeta(ans) }}</span>
                <el-button
                  v-if="ans.sampleAnswer"
                  size="small"
                  text
                  @click="copySample(ans.sampleAnswer)"
                >复制</el-button>
              </div>
              <p v-if="ans.sampleAnswer" class="ans__sample-text">{{ ans.sampleAnswer }}</p>
              <p v-else class="ans__sample-empty">这道题还没有配置范例答案，可以参照上方 AI 反馈里的改进建议自己先写一版。</p>
            </div>
          </template>
        </article>
      </section>
    </div>
  </AppShell>
</template>

<script setup>
/**
 * 真题套题成绩页
 * 数据来源：GET /api/fullExam/record/{recordId}
 *   record  —— 总分 / 四模块分档 / 正确数 / 用时
 *   modules —— 逐题明细（客观题含正确答案与解析，主观题含 AI 反馈）
 *   sections —— 材料原文（阅读正文、听力原文与音频）
 */
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import AppShell from '@/components/AppShell.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const restarting = ref(false)
const record = ref({})
const data = ref({})
const activeModule = ref('reading')

const MODULE_LABELS = { reading: '阅读', listening: '听力', speaking: '口语', writing: '写作' }
const MODULE_ORDER = ['reading', 'listening', 'speaking', 'writing']

function moduleLabel(type) {
  return MODULE_LABELS[type] || type
}

const durationText = computed(() => {
  const seconds = record.value.costSeconds || 0
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${m} 分 ${s} 秒`
})

/* 四模块分档卡片 */
const bands = computed(() => [
  {
    key: 'reading',
    label: '阅读',
    band: record.value.readingBand,
    extra: `${record.value.readingCorrect || 0} / ${record.value.readingTotal || 0} 题`
  },
  {
    key: 'listening',
    label: '听力',
    band: record.value.listeningBand,
    extra: `${record.value.listeningCorrect || 0} / ${record.value.listeningTotal || 0} 题`
  },
  { key: 'speaking', label: '口语', band: record.value.speakingBand, extra: 'AI 评分' },
  { key: 'writing', label: '写作', band: record.value.writingBand, extra: 'AI 评分' }
])

function bandPercent(band) {
  const value = Number(band || 0)
  return Math.min(100, Math.max(0, (value / 6) * 100)) + '%'
}

function dimPercent(dim) {
  const value = Number(dim.score || 0)
  const max = Number(dim.max || 1)
  return Math.min(100, Math.max(0, (value / max) * 100)) + '%'
}

const allAnswers = computed(() => {
  const list = []
  for (const moduleView of data.value.modules || []) {
    for (const ans of moduleView.answers || []) {
      list.push(ans)
    }
  }
  return list
})

/* 模块切换的 tab 数据：分档 + 题量 + 实际作答数 */
const moduleTabs = computed(() => {
  return MODULE_ORDER.map(key => {
    const answers = allAnswers.value.filter(a => a.moduleType === key)
    const answered = answers.filter(a => a.userAnswer && String(a.userAnswer).trim()).length
    const bandMap = {
      reading: record.value.readingBand,
      listening: record.value.listeningBand,
      speaking: record.value.speakingBand,
      writing: record.value.writingBand
    }
    return { key, label: moduleLabel(key), band: bandMap[key], count: answers.length, answered }
  })
})

const activeAnswers = computed(() => allAnswers.value.filter(a => a.moduleType === activeModule.value))

const activeSections = computed(() =>
  (data.value.sections || []).filter(s => s.moduleType === activeModule.value)
)

function splitParagraphs(text) {
  return String(text || '')
    .split(/\n+/)
    .map(t => t.trim())
    .filter(Boolean)
}

/* AI 反馈可能是对象，也可能是没解析成功的字符串 */
function feedbackOf(ans) {
  if (!ans || !ans.aiFeedback) return null
  if (typeof ans.aiFeedback === 'string') {
    try {
      return JSON.parse(ans.aiFeedback)
    } catch (e) {
      return null
    }
  }
  return ans.aiFeedback
}

/* 客观题选项展示成 "B. 选项原文" */
function optionText(ans, key) {
  if (!key) return '未作答'
  const text = ans['option' + String(key).toUpperCase()]
  return text ? `${key}. ${text}` : key
}

/* 范例答案的字数 / 句数，让考生知道"满分大概写多长" */
function sampleMeta(ans) {
  const text = String(ans.sampleAnswer || '').trim()
  if (!text) return ''
  const words = (text.match(/[A-Za-z][A-Za-z'-]*/g) || []).length
  const sentences = (text.match(/[.!?]+/g) || []).length
  return `${words} 词 · ${sentences} 句`
}

/* 复制范例答案（口语范例可直接拿来做跟读/背诵材料） */
async function copySample(text) {
  const value = String(text || '')
  if (!value) return
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(value)
    } else {
      const area = document.createElement('textarea')
      area.value = value
      area.style.position = 'fixed'
      area.style.opacity = '0'
      document.body.appendChild(area)
      area.select()
      document.execCommand('copy')
      document.body.removeChild(area)
    }
    ElMessage.success('范例答案已复制')
  } catch (e) {
    ElMessage.warning('复制失败，可以手动选中文本')
  }
}

async function loadDetail() {
  const recordId = route.query.recordId
  if (!recordId) {
    ElMessage.error('缺少 recordId')
    loading.value = false
    return
  }
  loading.value = true
  try {
    const res = await request.get('/api/fullExam/record/' + recordId)
    if (res.code !== 200) {
      ElMessage.error(res.msg || '成绩加载失败')
      return
    }
    record.value = res.data.record || {}
    data.value = res.data
    // 默认选中第一个有作答的模块
    const first = MODULE_ORDER.find(key => (res.data.modules || [])
      .some(m => m.moduleType === key && (m.answers || []).length > 0))
    activeModule.value = first || 'reading'
  } catch (e) {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

function backList() {
  router.push('/full')
}

/* 再考一次：用同一套题新建一条记录 */
async function retake() {
  if (!record.value.suiteId) return
  restarting.value = true
  try {
    const res = await request.post('/api/fullExam/start', { suiteId: record.value.suiteId })
    if (res.code !== 200) {
      ElMessage.error(res.msg || '开始失败')
      return
    }
    router.push({ path: '/full/exam', query: { recordId: res.data.id, suiteId: record.value.suiteId } })
  } catch (e) {
    // 拦截器已提示
  } finally {
    restarting.value = false
  }
}

onMounted(loadDetail)
</script>

<style scoped>
.result-page { padding-top: 22px; }

/* ---------- 总览 ---------- */
.hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.15fr);
  gap: 26px;
  padding: 28px 30px;
  background: linear-gradient(135deg, #ffffff 0%, var(--brand-50) 65%, #fdf7ec 100%);
  border: 1px solid var(--line);
}

.hero__left { display: flex; flex-direction: column; gap: 10px; }

.hero__band { display: flex; align-items: baseline; gap: 8px; }

.hero__band-num {
  font-size: 62px;
  font-weight: 800;
  line-height: 1;
  letter-spacing: -2px;
  color: var(--brand-800);
  font-variant-numeric: tabular-nums;
}

.hero__band-max { font-size: 18px; font-weight: 700; color: var(--ink-300); }

.hero__comment { margin: 0; font-size: 14px; color: var(--ink-700); }

.hero__tags { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 6px; }

.hero__right { display: flex; flex-direction: column; justify-content: space-between; gap: 18px; }

.band-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.band-cell {
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  padding: 12px 12px 10px;
  box-shadow: var(--shadow-sm);
}

.band-cell__label { font-size: 12px; color: var(--ink-500); font-weight: 600; }
.band-cell__value {
  font-size: 24px;
  font-weight: 800;
  color: var(--brand-700);
  font-variant-numeric: tabular-nums;
  margin: 2px 0 2px;
}
.band-cell__value small { font-size: 11px; color: var(--ink-300); font-weight: 600; margin-left: 2px; }
.band-cell__extra { font-size: 11px; color: var(--ink-300); margin-bottom: 8px; }

.band-cell__bar { height: 4px; border-radius: 999px; background: var(--surface-3); overflow: hidden; }
.band-cell__bar span {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--brand-500), var(--accent-500));
}

.hero__actions { display: flex; justify-content: flex-end; gap: 10px; }

/* ---------- 模块切换 ---------- */
.module-tabs {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 6px;
  padding: 6px;
  margin: 16px 0 18px;
}

.tab {
  appearance: none;
  border: 0;
  background: transparent;
  border-radius: 12px;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  cursor: pointer;
  text-align: left;
  transition: background 0.15s ease;
}

.tab:hover { background: var(--brand-50); }
.tab strong { font-size: 14px; color: var(--ink-900); }
.tab small { font-size: 11px; color: var(--ink-500); }
.tab.is-active { background: var(--brand-900); }
.tab.is-active strong { color: #fff; }
.tab.is-active small { color: rgba(255, 255, 255, 0.72); }

/* ---------- 回顾 ---------- */
.review { display: flex; flex-direction: column; gap: 14px; }

.material { padding: 20px 24px; }
.material__head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.material__head h3 { margin: 0; font-size: 16px; font-weight: 700; }
.material__para { margin: 0 0 12px; font-size: 14px; line-height: 1.85; color: var(--ink-700); text-align: justify; }
.audio { width: 100%; height: 44px; margin-bottom: 14px; }

.ans { padding: 18px 20px; border-left: 3px solid transparent; }
.ans.is-right { border-left-color: var(--success); }
.ans.is-wrong { border-left-color: var(--danger); }

.ans__head { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-bottom: 10px; }

.ans__no {
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
}

.ans__title { margin: 0 0 12px; font-size: 14px; font-weight: 600; line-height: 1.65; }

.pill--danger { background: #fdeceb; color: var(--danger); border-color: #f7d5d3; }

.ans__compare { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }

.compare-item {
  border: 1px solid var(--line);
  border-radius: 12px;
  padding: 10px 12px;
  background: var(--surface-2);
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.compare-item.is-right { border-color: #c7ecda; background: #f3fcf7; }
.compare-item.is-wrong { border-color: #f7d5d3; background: #fef6f6; }
.compare-item__label { font-size: 11px; color: var(--ink-500); font-weight: 600; }
.compare-item__value { font-size: 13px; color: var(--ink-900); line-height: 1.6; }

.ans__analysis {
  margin-top: 12px;
  padding: 12px 14px;
  border-radius: 12px;
  background: var(--brand-50);
  border: 1px solid var(--brand-100);
}

.ans__analysis-label { font-size: 11px; font-weight: 700; color: var(--brand-600); }
.ans__analysis p { margin: 6px 0 0; font-size: 13px; line-height: 1.8; color: var(--ink-700); }

.ans__prompt { margin: 0 0 12px; font-size: 13px; line-height: 1.8; color: var(--ink-500); white-space: pre-wrap; }

.ans__answer {
  padding: 12px 14px;
  border-radius: 12px;
  background: var(--surface-2);
  border: 1px solid var(--line);
}

.ans__answer-label { font-size: 11px; font-weight: 700; color: var(--ink-500); }
.ans__answer p { margin: 6px 0 0; font-size: 13px; line-height: 1.85; white-space: pre-wrap; color: var(--ink-700); }

/* 未作答时把"空"写得淡一点，避免和正文混淆 */
.ans__answer.is-empty p { color: var(--ink-300); font-style: italic; }

/* 口语录音回放 */
.ans__audio {
  margin-top: 12px;
  padding: 12px 14px;
  border-radius: 12px;
  background: #fffaf4;
  border: 1px solid #ffe0cc;
}

.ans__audio audio {
  display: block;
  width: 100%;
  height: 42px;
  margin-top: 8px;
}

/* 满分范例（口语/写作） */
.ans__sample {
  margin-top: 14px;
  padding: 16px;
  border-radius: var(--radius-md);
  border: 1px solid #cfe4d8;
  background: #f6fcf8;
}

.ans__sample-head { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; margin-bottom: 8px; }
.ans__sample-label { font-size: 13px; font-weight: 700; color: #157a51; }
.ans__sample-meta { font-size: 11px; color: var(--ink-500); }
.ans__sample-head :deep(.el-button) { margin-left: auto; color: #157a51; }
.ans__sample-text {
  margin: 0;
  font-size: 13px;
  line-height: 1.9;
  color: var(--ink-900);
  white-space: pre-wrap;
}
.ans__sample-empty { margin: 0; font-size: 12px; line-height: 1.8; color: var(--ink-300); }

/* ---------- AI 反馈 ---------- */
.ai {
  margin-top: 14px;
  padding: 16px;
  border-radius: var(--radius-md);
  border: 1px dashed #e3d1ad;
  background: #fffdf8;
}

.ai__head { display: flex; align-items: center; gap: 10px; margin-bottom: 14px; flex-wrap: wrap; }
.ai__title { font-size: 13px; font-weight: 700; color: var(--ink-900); }
.ai__engine { font-size: 11px; color: var(--ink-300); margin-left: auto; }

.ai__dims { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }

.dim__top { display: flex; align-items: baseline; justify-content: space-between; margin-bottom: 4px; }
.dim__name { font-size: 12px; font-weight: 600; color: var(--ink-700); }
.dim__score { font-size: 12px; color: var(--brand-600); font-weight: 700; font-variant-numeric: tabular-nums; }
.dim__bar { height: 4px; border-radius: 999px; background: var(--surface-3); overflow: hidden; }
.dim__bar span { display: block; height: 100%; background: linear-gradient(90deg, var(--brand-500), var(--accent-500)); }
.dim__comment { margin: 6px 0 0; font-size: 11px; line-height: 1.7; color: var(--ink-500); }

.ai__lists { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; margin-top: 16px; }
.ai__list h5 { margin: 0 0 6px; font-size: 12px; font-weight: 700; color: var(--ink-900); }
.ai__list ul { margin: 0; padding-left: 16px; }
.ai__list li { font-size: 12px; line-height: 1.85; color: var(--ink-700); }

.ai__stats { margin: 14px 0 0; padding-top: 12px; border-top: 1px dashed #ece0c8; font-size: 11px; color: var(--ink-500); line-height: 1.8; }

/* ---------- 窄屏 ---------- */
@media (max-width: 900px) {
  .hero { grid-template-columns: minmax(0, 1fr); }
  .band-grid { grid-template-columns: repeat(2, 1fr); }
  .module-tabs { grid-template-columns: repeat(2, 1fr); }
  .ans__compare, .ai__dims, .ai__lists { grid-template-columns: minmax(0, 1fr); }
  .hero__actions { justify-content: flex-start; }
}
</style>
