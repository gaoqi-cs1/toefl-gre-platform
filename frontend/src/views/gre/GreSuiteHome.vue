<template>
  <AppShell>
    <div class="page">
      <!-- 页头：与 TOEFL 真题套题、GRE 分块练习三处保持同一套结构 -->
      <div class="page-head">
        <div>
          <h1 class="page-title">GRE 全真套题</h1>
          <p class="page-sub">
            Writing → Verbal → Quant 顺序作答，答案先存在浏览器本地，四个板块全部做完后再一次性提交，
            由后端统一判分（AWA 按 0-6 档给维度反馈与满分范例）。套题数据独立于分块练习，两边互不调用。
          </p>
        </div>
        <div class="head-actions">
          <span class="pill pill--muted">{{ suites.length }} 套可用 · 每次组卷都不一样</span>
          <el-button type="primary" round :loading="generating" @click="generateSuite">
            一键生成套题
          </el-button>
        </div>
      </div>

      <!-- 四个入口平行切换: TOEFL 分块 / TOEFL 套题 / GRE 分块 / GRE 套题 -->
      <ModeSwitch />

      <!-- 套题卡片 -->
      <section v-loading="loading" class="suite-grid">
        <article v-for="suite in suites" :key="suite.id" class="suite-card card">
          <div class="suite-card__top">
            <span class="suite-card__badge">{{ suite.difficulty || '中级' }}</span>
            <span class="suite-card__minutes">{{ suite.totalMinutes }} 分钟</span>
          </div>

          <h3 class="suite-card__title">{{ suite.title }}</h3>
          <p class="suite-card__desc">{{ suite.description }}</p>

          <div class="suite-card__stats">
            <div class="stat"><strong>{{ suite.writingCount || 0 }}</strong><span>写作</span></div>
            <div class="stat"><strong>{{ suite.verbalCount || 0 }}</strong><span>Verbal</span></div>
            <div class="stat"><strong>{{ suite.quantCount || 0 }}</strong><span>Quant</span></div>
            <div class="stat"><strong>{{ suite.questionCount || 0 }}</strong><span>总题量</span></div>
          </div>

          <div class="suite-card__foot">
            <el-button type="primary" round :loading="startingId === suite.id" @click="startExam(suite)">
              开始模考
            </el-button>
            <el-button
              v-if="suite.source === 'generated'"
              text
              type="danger"
              size="small"
              @click="removeSuite(suite)"
            >删除</el-button>
          </div>
        </article>

        <div v-if="!loading && suites.length === 0" class="empty card card-pad">
          还没有 GRE 套题，点右上角「一键生成套题」从分块题库随机组一套。
        </div>
      </section>

      <!-- 我的记录 -->
      <section class="records card card-pad">
        <div class="records__head">
          <h3 class="section-title">我的 GRE 模考记录</h3>
          <el-button text type="primary" @click="loadRecords">刷新</el-button>
        </div>

        <el-table v-if="records.length" :data="records" style="width: 100%" empty-text="暂无记录">
          <el-table-column prop="suiteTitle" label="套题" min-width="200" />
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <span :class="['pill', row.status === 'finished' ? 'pill--ok' : 'pill--muted']">
                {{ row.status === 'finished' ? '已完成' : '作答中' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="总分" width="110">
            <template #default="{ row }">
              <span v-if="row.status === 'finished'" class="score-badge">{{ row.totalScore }}</span>
              <span v-else class="muted">—</span>
            </template>
          </el-table-column>
          <el-table-column label="V / Q" width="120">
            <template #default="{ row }">
              <span v-if="row.status === 'finished'">{{ row.verbalScaled }} / {{ row.quantScaled }}</span>
              <span v-else class="muted">—</span>
            </template>
          </el-table-column>
          <el-table-column label="AWA" width="90">
            <template #default="{ row }">
              <span v-if="row.awaScore != null">{{ row.awaScore }}<small>/6</small></span>
              <span v-else class="muted">—</span>
            </template>
          </el-table-column>
          <el-table-column label="用时" width="110">
            <template #default="{ row }">
              {{ row.costSeconds ? formatDuration(row.costSeconds) : '—' }}
            </template>
          </el-table-column>
          <el-table-column label="开始时间" min-width="150">
            <template #default="{ row }">{{ formatTime(row.startTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="210" align="right">
            <template #default="{ row }">
              <el-button v-if="row.status === 'finished'" size="small" round @click="viewResult(row)">
                查看成绩
              </el-button>
              <el-button v-else size="small" type="primary" round @click="resumeExam(row)">继续作答</el-button>
              <el-button size="small" text type="danger" @click="removeRecord(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div v-else class="empty">还没有 GRE 模考记录，先选一套开始。</div>
      </section>
    </div>
  </AppShell>
</template>

<script setup>
/**
 * GRE 全真套题页（与 TOEFL /full 平行的一级入口）
 *
 * 接口（全部是 GRE 自己的套题接口，不读 gre_section / gre_question，也不调 TOEFL 的 /api/fullSuite）：
 *   GET    /api/greSuite/list        套题目录
 *   POST   /api/greSuite/generate    一键随机组卷（按来源练习单元分组复制，RC 正文跟着题走）
 *   DELETE /api/greSuite/{id}        删除随机生成的套题（预置套题不可删）
 *   POST   /api/greExam/start        开始一次模考
 *   GET    /api/greExam/records      我的模考记录
 *   DELETE /api/greExam/record/{id}  删除记录
 */
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import AppShell from '@/components/AppShell.vue'
import ModeSwitch from '@/components/ModeSwitch.vue'

const router = useRouter()

const suites = ref([])
const records = ref([])
const loading = ref(false)
const generating = ref(false)
const startingId = ref(null)
const userId = ref(1)

function loadUser() {
  const str = localStorage.getItem('userInfo')
  if (!str) return
  try {
    const info = JSON.parse(str)
    if (info.userId) userId.value = info.userId
  } catch (e) {
    // 解析失败用兜底 1
  }
}

async function loadSuites() {
  loading.value = true
  try {
    const res = await request.get('/api/greSuite/list')
    if (res.code === 200) suites.value = res.data || []
  } catch (e) {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

async function loadRecords() {
  try {
    const res = await request.get('/api/greExam/records', { params: { userId: userId.value } })
    if (res.code === 200) records.value = res.data || []
  } catch (e) {
    // 忽略
  }
}

async function generateSuite() {
  generating.value = true
  try {
    const res = await request.post('/api/greSuite/generate', { writing: 2, verbal: 10, quant: 10 })
    if (res.code !== 200) {
      ElMessage.error(res.msg || '生成失败')
      return
    }
    const d = res.data || {}
    ElMessage.success(`已生成「${d.title}」：写作 ${d.writingCount} 篇 / Verbal ${d.verbalCount} 题 / Quant ${d.quantCount} 题`)
    await loadSuites()
  } catch (e) {
    // 拦截器已提示
  } finally {
    generating.value = false
  }
}

async function removeSuite(suite) {
  try {
    await ElMessageBox.confirm('删除这套随机生成的套题？预置套题不受影响。', '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch (e) {
    return
  }
  try {
    const res = await request.delete('/api/greSuite/' + suite.id)
    if (res.code === 200) {
      ElMessage.success('已删除')
      loadSuites()
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  } catch (e) {
    // 拦截器已提示
  }
}

async function startExam(suite) {
  startingId.value = suite.id
  try {
    const res = await request.post('/api/greExam/start', { suiteId: suite.id, userId: userId.value })
    if (res.code !== 200) {
      ElMessage.error(res.msg || '开始失败')
      return
    }
    const record = res.data
    clearLocalCache(record.id)
    router.push({ path: '/gre/exam', query: { recordId: record.id, suiteId: suite.id } })
  } catch (e) {
    // 拦截器已提示
  } finally {
    startingId.value = null
  }
}

function resumeExam(row) {
  router.push({ path: '/gre/exam', query: { recordId: row.id, suiteId: row.suiteId } })
}

function viewResult(row) {
  router.push({ path: '/gre/result', query: { recordId: row.id } })
}

async function removeRecord(row) {
  try {
    await ElMessageBox.confirm('删除这条记录后无法恢复（含逐题作答与 AWA 反馈），确定删除吗？', '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch (e) {
    return
  }
  try {
    const res = await request.delete('/api/greExam/record/' + row.id)
    if (res.code === 200) {
      clearLocalCache(row.id)
      ElMessage.success('已删除')
      loadRecords()
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  } catch (e) {
    // 拦截器已提示
  }
}

/* GRE 的本地缓存用 greExam:* 前缀，和 TOEFL 的 fullExam:* 完全分开 */
function clearLocalCache(recordId) {
  localStorage.removeItem('greExam:answers:' + recordId)
  localStorage.removeItem('greExam:startedAt:' + recordId)
}

function formatDuration(seconds) {
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  return h > 0 ? `${h} 小时 ${m} 分` : `${m} 分 ${s} 秒`
}

function formatTime(value) {
  if (!value) return '—'
  return String(value).replace('T', ' ').slice(0, 16)
}

onMounted(() => {
  loadUser()
  loadSuites()
  loadRecords()
})
</script>

<style scoped>
.head-actions { display: flex; align-items: center; gap: 10px; }

.suite-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 18px;
  margin-bottom: 24px;
}

.suite-card {
  padding: 20px 22px 16px;
  display: flex;
  flex-direction: column;
  transition: transform 0.16s ease, box-shadow 0.16s ease;
}

.suite-card:hover { transform: translateY(-3px); box-shadow: var(--shadow-md); }

.suite-card__top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }

.suite-card__badge {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  background: var(--accent-100);
  color: var(--accent-600);
  font-size: 12px;
  font-weight: 700;
}

.suite-card__minutes { font-size: 12px; color: var(--ink-500); }

.suite-card__title { margin: 0 0 8px; font-size: 20px; font-weight: 700; letter-spacing: -0.3px; }

.suite-card__desc { margin: 0 0 14px; font-size: 13px; line-height: 1.65; color: var(--ink-500); min-height: 42px; }

.suite-card__stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  padding: 12px;
  border-radius: var(--radius-md);
  background: var(--surface-2);
  border: 1px solid var(--line);
}

.stat { display: flex; flex-direction: column; align-items: center; gap: 2px; }
.stat strong { font-size: 18px; font-variant-numeric: tabular-nums; color: var(--brand-700); }
.stat span { font-size: 11px; color: var(--ink-500); }

.suite-card__foot {
  margin-top: 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.records__head { display: flex; align-items: center; justify-content: space-between; }
.records__head :deep(.section-title) { margin-bottom: 0; }
.records :deep(.el-table) { margin-top: 12px; }

.score-badge { font-size: 18px; font-weight: 800; color: var(--brand-700); font-variant-numeric: tabular-nums; }
.score-badge small { font-size: 11px; color: var(--ink-300); }
.muted { color: var(--ink-300); }
</style>
