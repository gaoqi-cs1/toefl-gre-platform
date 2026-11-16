<template>
  <AppShell>
    <div class="page result">
      <!-- 成绩头 -->
      <div class="score-hero card">
        <div class="score-hero__left">
          <el-button text @click="back">← 返回 GRE 首页</el-button>
          <h1 class="score-hero__title">{{ data.suiteTitle || record.suiteTitle || 'GRE 模考成绩' }}</h1>
          <p class="muted">提交时间：{{ formatTime(record.endTime || record.startTime) }} · 用时 {{ formatDuration(record.costSeconds) }}</p>
        </div>

        <div class="score-hero__right">
          <div class="big-score">
            <span class="big-score__value">{{ record.totalScore ?? '—' }}</span>
            <span class="big-score__unit">/ 340</span>
            <span class="big-score__label">Total</span>
          </div>
          <div class="score-grid">
            <div class="score-item">
              <span>Verbal</span>
              <strong>{{ record.verbalScaled ?? '—' }}</strong>
              <small>{{ record.verbalCorrect ?? 0 }}/{{ record.verbalTotal ?? 0 }} 正确</small>
            </div>
            <div class="score-item">
              <span>Quant</span>
              <strong>{{ record.quantScaled ?? '—' }}</strong>
              <small>{{ record.quantCorrect ?? 0 }}/{{ record.quantTotal ?? 0 }} 正确</small>
            </div>
            <div class="score-item">
              <span>AWA</span>
              <strong>{{ record.awaScore ?? '—' }}</strong>
              <small>{{ record.awaScore == null ? '未作答' : '6 分制' }}</small>
            </div>
          </div>
        </div>
      </div>

      <div v-if="data.verbalComment || data.quantComment || data.awaComment" class="comments">
        <div class="comment card card-pad">
          <h4>Verbal 水平判断</h4>
          <p>{{ data.verbalComment || '暂无' }}</p>
        </div>
        <div class="comment card card-pad">
          <h4>Quant 水平判断</h4>
          <p>{{ data.quantComment || '暂无' }}</p>
        </div>
        <div class="comment card card-pad">
          <h4>AW 评分说明</h4>
          <p>{{ data.awaComment || '暂无' }}</p>
        </div>
      </div>

      <!-- 模块切换 -->
      <div class="module-tabs">
        <button
          v-for="mod in data.modules"
          :key="mod.moduleType"
          type="button"
          :class="['module-tab', { 'is-active': activeModule === mod.moduleType }]"
          @click="activeModule = mod.moduleType"
        >
          <strong>{{ mod.label }}</strong>
          <small>{{ statOf(mod) }}</small>
        </button>
      </div>

      <!-- 逐题回顾 -->
      <section v-loading="loading" class="review">
        <template v-for="mod in data.modules" :key="mod.moduleType">
          <div v-if="activeModule === mod.moduleType" class="review-list">
            <article v-for="item in mod.answers" :key="item.questionId" class="review-item card">
              <header class="review-item__head">
                <span class="q-no">{{ item.sortIndex }}</span>
                <h3>{{ item.sectionTitle }}</h3>
                <span v-if="item.isCorrect === 1" class="pill pill--ok">正确</span>
                <span v-else-if="item.isCorrect === 0" class="pill pill--bad">错误</span>
                <span v-else class="pill pill--muted">{{ item.awaScore != null ? '已评分' : '未作答' }}</span>
              </header>

              <p v-if="item.questionTitle" class="q-title">{{ item.questionTitle }}</p>
              <p v-if="item.prompt" class="q-prompt">{{ item.prompt }}</p>

              <!-- 客观题：我的答案 + 正确答案 -->
              <div v-if="!isWriting(item) && hasOptions(item)" class="options">
                <div
                  v-for="opt in optionsOf(item)"
                  :key="opt.key"
                  :class="['option', optionClass(item, opt)]"
                >
                  <span class="option__key">{{ opt.key }}</span>
                  <span class="option__text">{{ opt.text }}</span>
                  <span v-if="opt.key === item.correctAnswer" class="option__tag">正确答案</span>
                  <span v-else-if="opt.key === item.userAnswer" class="option__tag option__tag--mine">我的选择</span>
                </div>
              </div>

              <div class="answer-line">
                <span class="chip chip--mine">我的答案：{{ item.userAnswer ? item.userAnswer : '空' }}</span>
                <span v-if="!isWriting(item)" class="chip chip--ok">正确答案：{{ item.correctAnswer }}</span>
              </div>

              <!-- 写作：分数 + 维度 + 范文 -->
              <template v-if="isWriting(item)">
                <p class="my-essay">{{ item.userAnswer ? item.userAnswer : '（本题未作答，我的答案为空）' }}</p>

                <div v-if="item.awaScore != null" class="awa-box">
                  <div class="awa-score">
                    <strong>{{ item.awaScore }}</strong>
                    <span>/ 6</span>
                  </div>
                  <div v-if="feedbackOf(item)" class="awa-body">
                    <p class="awa-level">{{ feedbackOf(item).level }}</p>
                    <div v-for="dim in feedbackOf(item).dimensions || []" :key="dim.name" class="dimension">
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
                    <div class="awa-two">
                      <div>
                        <h5>优点</h5>
                        <ul class="bullets">
                          <li v-for="(s, i) in feedbackOf(item).strengths || []" :key="'s' + i">{{ s }}</li>
                        </ul>
                      </div>
                      <div>
                        <h5>改进</h5>
                        <ul class="bullets">
                          <li v-for="(s, i) in feedbackOf(item).improvements || []" :key="'i' + i">{{ s }}</li>
                        </ul>
                      </div>
                    </div>
                  </div>
                </div>

                <el-collapse v-if="item.sampleAnswer" class="sample">
                  <el-collapse-item title="满分范例（6 分档）" name="sample">
                    <p class="pre-line">{{ item.sampleAnswer }}</p>
                  </el-collapse-item>
                </el-collapse>
              </template>

              <!-- 客观题解析 -->
              <div v-if="!isWriting(item) && item.analysis" class="analysis">
                <h4>解析</h4>
                <p class="pre-line">{{ item.analysis }}</p>
              </div>
            </article>

            <div v-if="(mod.answers || []).length === 0" class="empty card card-pad">该模块没有题目。</div>
          </div>
        </template>
      </section>

      <div class="result-foot card">
        <div class="muted small">量表分按正确率近似换算，用于自我定位，非官方预测分。</div>
        <div class="result-foot__actions">
          <el-button round @click="back">返回首页</el-button>
          <el-button type="primary" round @click="goPractice">去分块练习补弱项</el-button>
        </div>
      </div>
    </div>
  </AppShell>
</template>

<script setup>
/**
 * GRE 成绩页（/gre/result?recordId=）
 * 数据：GET /api/greExam/record/{recordId}
 * 整卷每一题都展示：我的答案（未作答显示"空"）、正确答案、解析；写作额外给 AWA 分数、维度反馈与满分范例。
 */
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import AppShell from '@/components/AppShell.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const detail = ref({})
const activeModule = ref('writing')

const data = computed(() => detail.value || {})
const record = computed(() => data.value.record || {})

function isWriting(item) {
  return item.taskType && String(item.taskType).startsWith('awa')
}

function hasOptions(item) {
  return !!(item.optionA || item.optionB || item.optionC || item.optionD || item.optionE || item.optionF)
}

function optionsOf(item) {
  const keys = ['A', 'B', 'C', 'D', 'E', 'F']
  const values = [item.optionA, item.optionB, item.optionC, item.optionD, item.optionE, item.optionF]
  const list = []
  keys.forEach((key, i) => {
    if (values[i] && String(values[i]).trim()) {
      list.push({ key, text: values[i] })
    }
  })
  return list
}

function optionClass(item, opt) {
  if (opt.key === item.correctAnswer) return { 'is-correct': true }
  if (opt.key === item.userAnswer) return { 'is-wrong': true }
  return { 'is-dim': true }
}

function feedbackOf(item) {
  return item.aiFeedback || null
}

function statOf(mod) {
  const answers = mod.answers || []
  if (mod.moduleType === 'writing') {
    const scored = answers.filter(a => a.awaScore != null)
    return `写作 ${scored.length}/${answers.length} 篇已评分`
  }
  const objective = answers.filter(a => a.isCorrect != null)
  const correct = answers.filter(a => a.isCorrect === 1)
  return `正确 ${correct.length}/${answers.length} 题`
}

function formatDuration(seconds) {
  if (!seconds) return '—'
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  return h > 0 ? `${h} 小时 ${m} 分` : `${m} 分 ${s} 秒`
}

function formatTime(value) {
  if (!value) return '—'
  return String(value).replace('T', ' ').slice(0, 16)
}

async function load() {
  loading.value = true
  try {
    const res = await request.get('/api/greExam/record/' + route.query.recordId)
    if (res.code === 200) {
      detail.value = res.data || {}
      const first = (data.value.modules || []).find(m => (m.answers || []).length > 0)
      if (first) activeModule.value = first.moduleType
    } else {
      ElMessage.error(res.msg || '成绩加载失败')
    }
  } catch (e) {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

function back() {
  router.push('/gre')
}

function goPractice() {
  router.push('/gre')
}

onMounted(load)
</script>

<style scoped>
.score-hero {
  display: flex; align-items: center; justify-content: space-between; gap: 24px;
  padding: 22px 26px; margin-bottom: 16px;
}
.score-hero__title { margin: 8px 0 6px; font-size: 24px; letter-spacing: -0.4px; }
.score-hero__right { display: flex; align-items: center; gap: 26px; }
.big-score { display: flex; flex-direction: column; align-items: flex-end; }
.big-score__value { font-size: 44px; font-weight: 800; line-height: 1; color: var(--brand-700); font-variant-numeric: tabular-nums; }
.big-score__unit { font-size: 12px; color: var(--ink-300); }
.big-score__label { font-size: 11px; letter-spacing: 0.18em; color: var(--ink-500); text-transform: uppercase; }
.score-grid { display: flex; gap: 10px; }
.score-item {
  min-width: 88px; padding: 10px 12px; border-radius: var(--radius-md);
  background: var(--surface-2); border: 1px solid var(--line);
  display: flex; flex-direction: column; gap: 2px;
}
.score-item span { font-size: 11px; color: var(--ink-500); }
.score-item strong { font-size: 22px; font-variant-numeric: tabular-nums; }
.score-item small { font-size: 11px; color: var(--ink-300); }

.comments { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; margin-bottom: 18px; }
.comment h4 { margin: 0 0 6px; font-size: 13px; }
.comment p { margin: 0; font-size: 13px; line-height: 1.75; color: var(--ink-700); }

.module-tabs { display: flex; gap: 10px; margin-bottom: 16px; flex-wrap: wrap; }
.module-tab {
  appearance: none; cursor: pointer; border: 1px solid var(--line); background: #fff;
  border-radius: var(--radius-md); padding: 10px 16px; display: flex; flex-direction: column;
  gap: 2px; text-align: left;
}
.module-tab strong { font-size: 13px; }
.module-tab small { font-size: 11px; color: var(--ink-500); }
.module-tab.is-active { border-color: var(--brand-500); background: var(--brand-50); }
.module-tab.is-active strong { color: var(--brand-700); }

.review-list { display: flex; flex-direction: column; gap: 14px; }
.review-item { padding: 18px 22px; }
.review-item__head { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.review-item__head h3 { margin: 0; font-size: 14px; color: var(--ink-500); font-weight: 600; }
.q-no {
  width: 24px; height: 24px; border-radius: 50%; display: grid; place-items: center;
  background: var(--brand-50); color: var(--brand-700); font-size: 12px; font-weight: 800;
}
.q-title { margin: 0 0 10px; font-size: 14.5px; line-height: 1.75; white-space: pre-line; }
.q-prompt { margin: 0 0 12px; font-size: 13.5px; line-height: 1.8; color: var(--ink-700); white-space: pre-line; }

.options { display: flex; flex-direction: column; gap: 6px; }
.option {
  display: flex; gap: 10px; align-items: flex-start; padding: 8px 12px;
  border-radius: var(--radius-md); border: 1px solid var(--line); background: #fff;
  font-size: 13.5px; line-height: 1.6;
}
.option.is-correct { border-color: #17936a; background: #f0fbf5; }
.option.is-wrong { border-color: #c0392b; background: #fdf1f1; }
.option.is-dim { opacity: 0.55; }
.option__key {
  flex: none; width: 20px; height: 20px; border-radius: 50%; display: grid; place-items: center;
  font-size: 11px; font-weight: 800; color: var(--ink-500); background: var(--surface-3);
}
.option__tag { margin-left: auto; font-size: 11px; color: var(--ink-500); white-space: nowrap; }
.option__tag--mine { color: var(--brand-600); font-weight: 700; }

.answer-line { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 12px; }
.chip {
  font-size: 12px; padding: 4px 10px; border-radius: 999px; border: 1px solid var(--line);
  background: var(--surface-2); color: var(--ink-700);
}
.chip--ok { background: #f0fbf5; border-color: transparent; color: #157a4f; font-weight: 700; }
.chip--mine { background: var(--brand-50); border-color: transparent; color: var(--brand-700); font-weight: 700; }

.my-essay {
  margin: 12px 0 0; padding: 12px 14px; border-radius: var(--radius-md);
  background: var(--surface-2); border: 1px solid var(--line);
  font-size: 13.5px; line-height: 1.85; white-space: pre-line; color: var(--ink-700);
}

.awa-box {
  margin-top: 14px; display: flex; gap: 18px; padding: 14px 16px;
  border-radius: var(--radius-md); background: #fff; border: 1px solid var(--line);
}
.awa-score { display: flex; flex-direction: column; align-items: center; justify-content: center; min-width: 78px; }
.awa-score strong { font-size: 30px; color: var(--brand-700); font-variant-numeric: tabular-nums; }
.awa-score span { font-size: 11px; color: var(--ink-300); }
.awa-body { flex: 1; min-width: 0; }
.awa-level { margin: 0 0 10px; font-size: 13px; font-weight: 700; color: var(--ink-700); }
.dimension { margin-bottom: 10px; }
.dimension__head { display: flex; justify-content: space-between; font-size: 12.5px; margin-bottom: 4px; }
.dimension__comment { margin: 4px 0 0; font-size: 12px; color: var(--ink-500); }
.awa-two { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-top: 10px; }
.awa-two h5 { margin: 0 0 4px; font-size: 12px; color: var(--ink-700); }
.bullets { margin: 0; padding-left: 18px; font-size: 12.5px; line-height: 1.75; color: var(--ink-700); }

.sample { margin-top: 14px; }
.sample :deep(.el-collapse-item__content) { font-size: 13.5px; line-height: 1.85; }
.pre-line { white-space: pre-line; }

.analysis { margin-top: 14px; padding-top: 12px; border-top: 1px dashed var(--line); }
.analysis h4 { margin: 0 0 6px; font-size: 13px; }
.analysis p { margin: 0; font-size: 13.5px; line-height: 1.8; color: var(--ink-700); }

.result-foot {
  margin-top: 18px; padding: 14px 20px; display: flex; align-items: center;
  justify-content: space-between; gap: 16px;
}
.result-foot__actions { display: flex; gap: 10px; }
.pill--bad { background: #fdf1f1; color: #a52e2e; }
.muted { color: var(--ink-300); }
.small { font-size: 12px; }

@media (max-width: 900px) {
  .score-hero { flex-direction: column; align-items: flex-start; }
  .comments { grid-template-columns: 1fr; }
  .awa-box { flex-direction: column; }
  .awa-two { grid-template-columns: 1fr; }
  .result-foot { flex-direction: column; align-items: flex-start; }
}
</style>
