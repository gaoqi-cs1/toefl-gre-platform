<template>
  <AppShell>
    <div class="page practice">
      <div class="practice-head card">
        <div class="practice-head__left">
          <el-button text @click="back">← 返回 GRE 首页</el-button>
          <h1 class="practice-head__title">{{ section.title || '加载中…' }}</h1>
          <p class="practice-head__sub">{{ section.subtitle }}</p>
          <div class="practice-head__meta">
            <span class="tag">{{ moduleLabel(section.moduleType) }}</span>
            <span class="tag">{{ section.difficulty }}</span>
            <span class="tag">共 {{ questions.length }} 题</span>
            <span class="tag">已提交 {{ submittedCount }} 题</span>
          </div>
        </div>
        <div class="practice-head__right">
          <div class="timer">
            <span>本单元剩余</span>
            <strong :class="{ 'is-urgent': urgent }">{{ countdownText }}</strong>
          </div>
          <p class="muted small">进入单元即自动计时，中途不暂停，和真实 GRE 一致。</p>
        </div>
      </div>

      <!-- 计时结束：选项与提交全部锁定；点「重做本单元」会重新开始计时 -->
      <div v-if="timeUp" class="time-up card">
        <div class="time-up__text">
          <strong>本单元计时已结束（限时 {{ limitText }}）</strong>
          <span>选项与提交已锁定，避免超时后继续作答。想再练一遍点右侧按钮，计时会重新开始。</span>
        </div>
        <el-button size="small" round @click="resetSection">重做本单元</el-button>
      </div>

      <section v-if="section.contentText" class="passage card card-pad">
        <h3 class="section-title">Passage</h3>
        <p v-for="(para, i) in passageParagraphs" :key="i" class="passage__para">{{ para }}</p>
      </section>

      <section v-loading="loading" class="questions">
        <article
          v-for="(q, index) in questions"
          :key="q.id"
          :class="['q-card', 'card', { 'is-submitted': results[q.id] }]"
        >
          <header class="q-card__head">
            <span class="q-index">Q{{ index + 1 }}</span>
            <span class="q-type">{{ typeLabel(q.taskType) }}</span>
            <span v-if="q.difficulty" class="q-diff">{{ q.difficulty }}</span>
          </header>

          <p v-if="q.questionTitle" class="q-title">{{ q.questionTitle }}</p>

          <template v-if="isWriting(q)">
            <p class="q-prompt">{{ q.prompt }}</p>
            <el-input
              v-model="answers[q.id]"
              type="textarea"
              :rows="10"
              :disabled="!!results[q.id] || timeUp"
              resize="vertical"
              placeholder="用英文写作，建议先花 2-3 分钟列提纲，再分段展开"
            />
            <div class="q-editor-foot">
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
                :class="['option', optionClass(q, opt)]"
                :disabled="!!results[q.id] || timeUp"
                @click="choose(q, opt.key)"
              >
                <span class="option__key">{{ opt.key }}</span>
                <span class="option__text">{{ opt.text }}</span>
              </button>
            </div>
          </template>

          <div class="q-actions">
            <el-button
              type="primary"
              round
              :disabled="!!results[q.id] || !hasAnswer(q) || timeUp"
              :loading="submitting[q.id]"
              @click="submitOne(q)"
            >
              {{ results[q.id] ? '已提交' : '提交本题' }}
            </el-button>
            <span v-if="!results[q.id] && !hasAnswer(q)" class="muted small">先选择或填写答案</span>
          </div>

          <div v-if="results[q.id]" class="result">
            <template v-if="!isWriting(q)">
              <div :class="['result__verdict', results[q.id].correct ? 'is-right' : 'is-wrong']">
                <strong>{{ results[q.id].correct ? '回答正确' : '回答错误' }}</strong>
                <span>正确答案：{{ results[q.id].correctAnswer }}</span>
              </div>
              <div class="result__block">
                <h4>解析</h4>
                <p class="pre-line">{{ results[q.id].analysis || '暂无解析' }}</p>
              </div>
            </template>

            <template v-else>
              <div class="result__verdict is-score">
                <strong>AWA {{ results[q.id].awaScore }} / 6</strong>
                <span>{{ results[q.id].feedback && results[q.id].feedback.level }}</span>
              </div>

              <div v-if="results[q.id].feedback" class="result__block">
                <h4>维度评分</h4>
                <div v-for="dim in results[q.id].feedback.dimensions" :key="dim.name" class="dimension">
                  <div class="dimension__head">
                    <span>{{ dim.name }}</span>
                    <strong>{{ dim.score }} / {{ dim.max }}</strong>
                  </div>
                  <el-progress
                    :percentage="Math.round((dim.score / dim.max) * 100)"
                    :stroke-width="6"
                    :show-text="false"
                    color="#2b4a8f"
                  />
                  <p class="dimension__comment">{{ dim.comment }}</p>
                </div>
              </div>

              <div v-if="results[q.id].feedback" class="result__two">
                <div class="result__block">
                  <h4>做得好的地方</h4>
                  <ul class="bullets">
                    <li v-for="(s, i) in results[q.id].feedback.strengths" :key="'s' + i">{{ s }}</li>
                  </ul>
                </div>
                <div class="result__block">
                  <h4>下一步怎么改</h4>
                  <ul class="bullets">
                    <li v-for="(s, i) in results[q.id].feedback.improvements" :key="'i' + i">{{ s }}</li>
                  </ul>
                </div>
              </div>

              <el-collapse v-if="results[q.id].sampleAnswer" class="result__sample">
                <el-collapse-item title="满分范例（6 分档）" name="sample">
                  <p class="pre-line">{{ results[q.id].sampleAnswer }}</p>
                </el-collapse-item>
              </el-collapse>
            </template>
          </div>
        </article>

        <div v-if="!loading && questions.length === 0" class="empty card card-pad">
          这个练习单元还没有题目。
        </div>
      </section>

      <div v-if="questions.length" class="practice-foot card">
        <div>
          <strong>{{ submittedCount }} / {{ questions.length }}</strong>
          <span class="muted"> 题已提交</span>
        </div>
        <div class="practice-foot__actions">
          <el-button round @click="back">返回目录</el-button>
          <el-button type="primary" round :disabled="submittedCount < questions.length" @click="resetSection">
            重做本单元
          </el-button>
        </div>
      </div>
    </div>
  </AppShell>
</template>

<script setup>
/**
 * GRE 分块练习页
 * 数据：GET /api/gre/practice/section/{sectionId}
 * 提交：POST /api/gre/practice/answer（客观题判对错，写作题走 AWA 评分）
 *
 * 交互要点：打开时选项没有任何选中态；每题提交后锁定并显示答案 / 解析 / AWA 维度分 / 满分范例。
 */
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import AppShell from '@/components/AppShell.vue'

const route = useRoute()
const router = useRouter()

const section = ref({})
const questions = computed(() => section.value.questions || [])
const loading = ref(false)
const answers = reactive({})
const results = reactive({})
const submitting = reactive({})

/* ---------------- 计时：进入单元自动开始，和真实 GRE 一样倒计时、不暂停 ---------------- */
const remaining = ref(0)          // 剩余秒数
const timeUp = ref(false)         // 时间是否用完（用完即锁定作答）
let timer = null

// 单元时限：后端 gre_section.limit_second（已按 每题型 × 题量 折算成真实 GRE 节奏）
const limit = computed(() => Number(section.value.limitSecond) || 0)
const urgent = computed(() => remaining.value > 0 && remaining.value <= 60)
const limitText = computed(() => formatClock(limit.value))

function formatClock(total) {
  const value = Math.max(0, Math.floor(Number(total) || 0))
  const h = Math.floor(value / 3600)
  const m = Math.floor((value % 3600) / 60)
  const s = value % 60
  const mm = String(m).padStart(2, '0')
  const ss = String(s).padStart(2, '0')
  return h > 0 ? `${h}:${mm}:${ss}` : `${mm}:${ss}`
}

const countdownText = computed(() => formatClock(remaining.value))

/** 起表：剩余置为时限，每秒减一；归零锁卷。limit 为 0 表示这套数据没配时限，则不计时。 */
function startTimer() {
  if (timer) clearInterval(timer)
  timeUp.value = false
  remaining.value = limit.value
  if (limit.value <= 0) return
  timer = setInterval(() => {
    remaining.value -= 1
    if (remaining.value <= 0) {
      remaining.value = 0
      timeUp.value = true
      clearInterval(timer)
      timer = null
      ElMessage.warning('本单元计时结束，作答已锁定')
    }
  }, 1000)
}

const submittedCount = computed(() => Object.keys(results).length)

const passageParagraphs = computed(() => {
  const text = section.value.contentText || ''
  return text.split(/\n+/).filter(p => p.trim().length > 0)
})

function moduleLabel(moduleType) {
  if (moduleType === 'writing') return 'Analytical Writing'
  if (moduleType === 'verbal') return 'Verbal Reasoning'
  if (moduleType === 'quant') return 'Quantitative Reasoning'
  return moduleType || ''
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
  return map[taskType] || taskType || '练习'
}

function isWriting(q) {
  return q.taskType && String(q.taskType).startsWith('awa')
}

function choose(q, key) {
  if (results[q.id] || timeUp.value) return
  answers[q.id] = key
}

/* 提交前只有"选中"；提交后正确项绿、错选项红、其余淡化 */
function optionClass(q, opt) {
  const result = results[q.id]
  if (!result) {
    return { 'is-selected': answers[q.id] === opt.key }
  }
  if (opt.key === result.correctAnswer) return { 'is-correct': true }
  if (answers[q.id] === opt.key) return { 'is-wrong': true }
  return { 'is-dim': true }
}

function hasAnswer(q) {
  const value = answers[q.id]
  return isWriting(q) ? !!String(value || '').trim() : !!value
}

function wordCount(text) {
  if (!text) return 0
  return String(text).split(/\s+/).filter(t => /[A-Za-z0-9'’-]+/.test(t)).length
}

async function submitOne(q) {
  if (results[q.id] || !hasAnswer(q)) return
  if (timeUp.value) {
    ElMessage.warning('本单元计时已结束，不能再提交；需要重练请点「重做本单元」')
    return
  }
  submitting[q.id] = true
  try {
    const res = await request.post('/api/gre/practice/answer', {
      questionId: q.id,
      userAnswer: answers[q.id]
    })
    if (res.code !== 200) {
      ElMessage.error(res.msg || '提交失败')
      return
    }
    results[q.id] = res.data
  } catch (e) {
    // 拦截器已提示
  } finally {
    submitting[q.id] = false
  }
}

async function loadSection() {
  loading.value = true
  try {
    const res = await request.get('/api/gre/practice/section/' + route.params.sectionId)
    if (res.code === 200) {
      section.value = res.data || {}
      // 题目一到手就开始计时：和真实 GRE 一样，进入页面即开始，不等用户点「开始」
      startTimer()
    } else {
      ElMessage.error(res.msg || '加载失败')
    }
  } catch (e) {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

function resetSection() {
  Object.keys(answers).forEach(k => delete answers[k])
  Object.keys(results).forEach(k => delete results[k])
  startTimer()   // 重做同样从头计时
  ElMessage.success('已清空本单元的作答，计时重新开始')
}

function back() {
  router.push('/gre')
}

onMounted(() => {
  loadSection()
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.practice-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  padding: 20px 24px;
  margin-bottom: 18px;
}
.practice-head__title { margin: 8px 0 4px; font-size: 24px; letter-spacing: -0.4px; }
.practice-head__sub { margin: 0 0 10px; font-size: 13px; color: var(--ink-500); }
.practice-head__meta { display: flex; gap: 6px; flex-wrap: wrap; }
.practice-head__right { text-align: right; flex: none; }
.timer { display: flex; flex-direction: column; align-items: flex-end; }
.timer span { font-size: 11px; color: var(--ink-500); }
.timer strong { font-size: 22px; font-variant-numeric: tabular-nums; color: var(--brand-700); }
.timer strong.is-urgent { color: #c0392b; }
.time-up {
  display: flex; align-items: center; justify-content: space-between; gap: 16px;
  padding: 14px 20px; margin-bottom: 16px;
  border-left: 4px solid #c0392b; background: #fdf1f1;
}
.time-up__text { display: flex; flex-direction: column; gap: 4px; }
.time-up__text strong { font-size: 13.5px; color: #a52e2e; }
.time-up__text span { font-size: 12.5px; color: #8a3a3a; }
.tag {
  font-size: 11px; padding: 3px 9px; border-radius: 999px;
  border: 1px solid var(--line); background: var(--surface-2); color: var(--ink-500);
}
.muted { color: var(--ink-300); }
.small { font-size: 12px; }
.passage { margin-bottom: 18px; }
.passage__para { margin: 0 0 12px; line-height: 1.85; font-size: 14.5px; color: var(--ink-700); }
.questions { display: flex; flex-direction: column; gap: 16px; }
.q-card { padding: 20px 24px 18px; }
.q-card.is-submitted { border-color: var(--brand-100); }
.q-card__head { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.q-index {
  font-weight: 800; color: var(--brand-700); font-size: 13px;
  background: var(--brand-50); padding: 3px 9px; border-radius: 8px;
}
.q-type { font-size: 11px; color: var(--ink-500); text-transform: uppercase; letter-spacing: 0.4px; }
.q-diff {
  margin-left: auto; font-size: 11px; color: var(--ink-500);
  border: 1px solid var(--line); border-radius: 999px; padding: 2px 8px;
}
.q-title { margin: 0 0 14px; font-size: 15px; line-height: 1.75; white-space: pre-line; }
.q-prompt {
  white-space: pre-line; margin: 0 0 14px; padding: 14px 16px;
  border-radius: var(--radius-md); background: var(--surface-2);
  border: 1px solid var(--line); font-size: 14px; line-height: 1.8;
}
.options { display: flex; flex-direction: column; gap: 8px; }
.option {
  appearance: none; text-align: left; display: flex; gap: 12px; align-items: flex-start;
  padding: 11px 14px; border-radius: var(--radius-md); border: 1px solid var(--line);
  background: #fff; cursor: pointer; font-size: 14px; line-height: 1.6;
  transition: border-color 0.14s ease, background 0.14s ease;
}
.option:hover:not(:disabled) { border-color: var(--brand-100); background: var(--brand-50); }
.option__key {
  flex: none; width: 22px; height: 22px; border-radius: 50%;
  display: grid; place-items: center; font-size: 12px; font-weight: 800;
  color: var(--ink-500); background: var(--surface-3);
}
.option__text { min-width: 0; }
.option.is-selected { border-color: var(--brand-500); background: var(--brand-50); }
.option.is-selected .option__key { background: var(--brand-500); color: #fff; }
.option.is-correct { border-color: #17936a; background: #f0fbf5; }
.option.is-correct .option__key { background: #17936a; color: #fff; }
.option.is-wrong { border-color: #c0392b; background: #fdf1f1; }
.option.is-wrong .option__key { background: #c0392b; color: #fff; }
.option.is-dim { opacity: 0.6; }
.q-actions { display: flex; align-items: center; gap: 12px; margin-top: 16px; }
.result { margin-top: 16px; padding-top: 16px; border-top: 1px dashed var(--line); }
.result__verdict {
  display: flex; align-items: center; gap: 12px; padding: 10px 14px;
  border-radius: var(--radius-md); font-size: 13px;
}
.result__verdict.is-right { background: #f0fbf5; color: #157a4f; }
.result__verdict.is-wrong { background: #fdf1f1; color: #a52e2e; }
.result__verdict.is-score { background: var(--brand-50); color: var(--brand-700); }
.result__verdict strong { font-size: 15px; }
.result__block { margin-top: 14px; }
.result__block h4 { margin: 0 0 6px; font-size: 13px; color: var(--ink-700); }
.result__block p { margin: 0; font-size: 13.5px; line-height: 1.8; color: var(--ink-700); }
.pre-line { white-space: pre-line; }
.q-editor-foot {
  display: flex; align-items: center; justify-content: space-between;
  gap: 16px; margin-top: 8px; font-size: 12px;
}
.dimension { margin-bottom: 12px; }
.dimension__head { display: flex; justify-content: space-between; font-size: 13px; margin-bottom: 4px; }
.dimension__comment { margin: 4px 0 0; font-size: 12px; color: var(--ink-500); }
.result__two { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; }
.bullets { margin: 0; padding-left: 18px; font-size: 13px; line-height: 1.8; color: var(--ink-700); }
.result__sample { margin-top: 16px; }
.result__sample :deep(.el-collapse-item__content) { font-size: 13.5px; line-height: 1.85; }
.practice-foot {
  margin-top: 20px; padding: 14px 20px; display: flex;
  align-items: center; justify-content: space-between;
}
.practice-foot__actions { display: flex; gap: 10px; }
@media (max-width: 860px) {
  .practice-head { flex-direction: column; }
  .practice-head__right { text-align: left; }
  .result__two { grid-template-columns: 1fr; }
}
</style>
