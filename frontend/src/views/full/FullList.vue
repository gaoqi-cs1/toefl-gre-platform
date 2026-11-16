<template>
  <AppShell>
    <div class="page">
      <!-- 页头 -->
      <div class="page-head">
        <div>
          <h1 class="page-title">真题套题</h1>
          <p class="page-sub">
            四个模块一次性完成：点选选项即时保存在本地，做完 Reading / Listening / Speaking / Writing
            后统一提交，后端一次性判分（口语与写作由 AI 按新托福 1-6 档评分）。
          </p>
        </div>
        <div class="head-actions">
          <span class="pill pill--muted">题库随机组合 · 每次都不一样</span>
          <el-button type="primary" round :loading="generating" @click="generateSuite">
            一键生成套题
          </el-button>
        </div>
      </div>

      <!-- 四个做题入口平行切换 -->
      <ModeSwitch />

      <!-- 套题卡片 -->
      <section v-loading="loading" class="suite-grid">
        <article v-for="suite in suites" :key="suite.id" class="suite-card card">
          <div class="suite-card__top">
            <span class="suite-card__badge">{{ suite.difficulty || '中级' }}</span>
            <span class="suite-card__minutes">{{ suite.totalMinutes }} 分钟</span>
          </div>

          <h2 class="suite-card__title">{{ suite.title }}</h2>
          <p class="suite-card__desc">{{ suite.description }}</p>

          <div class="suite-card__stats">
            <div class="stat">
              <strong>{{ suite.readingCount || 0 }}</strong>
              <span>阅读</span>
            </div>
            <div class="stat">
              <strong>{{ suite.listeningCount || 0 }}</strong>
              <span>听力</span>
            </div>
            <div class="stat">
              <strong>{{ suite.speakingCount || 0 }}</strong>
              <span>口语</span>
            </div>
            <div class="stat">
              <strong>{{ suite.writingCount || 0 }}</strong>
              <span>写作</span>
            </div>
          </div>

          <div class="suite-card__foot">
            <span class="suite-card__count">共 {{ suite.questionCount || 0 }} 题</span>
            <el-button type="primary" round :loading="startingId === suite.id" @click="startExam(suite)">
              开始模考
            </el-button>
          </div>
        </article>

        <div v-if="!loading && suites.length === 0" class="empty card card-pad">
          还没有套题数据，请先执行项目根目录的 full-suite-db.sql。
        </div>
      </section>

      <!-- 我的套题记录 -->
      <section class="records card card-pad">
        <div class="records__head">
          <h3 class="section-title">我的套题记录</h3>
          <el-button text type="primary" @click="loadRecords">刷新</el-button>
        </div>

        <el-table v-if="records.length" :data="records" style="width: 100%" empty-text="暂无记录">
          <el-table-column prop="suiteTitle" label="套题" min-width="180" />
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <span :class="['pill', row.status === 'finished' ? 'pill--ok' : 'pill--muted']">
                {{ row.status === 'finished' ? '已完成' : '作答中' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="总分" width="120">
            <template #default="{ row }">
              <span v-if="row.status === 'finished'" class="band-badge">
                {{ row.totalBand }}<small>/6</small>
              </span>
              <span v-else class="muted">—</span>
            </template>
          </el-table-column>
          <el-table-column label="用时" width="110">
            <template #default="{ row }">
              {{ row.costSeconds ? formatDuration(row.costSeconds) : '—' }}
            </template>
          </el-table-column>
          <el-table-column label="开始时间" min-width="160">
            <template #default="{ row }">{{ formatTime(row.startTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="220" align="right">
            <template #default="{ row }">
              <el-button
                v-if="row.status === 'finished'"
                size="small"
                round
                @click="viewResult(row)"
              >查看成绩</el-button>
              <el-button
                v-else
                size="small"
                type="primary"
                round
                @click="resumeExam(row)"
              >继续作答</el-button>
              <el-button size="small" text type="danger" @click="removeRecord(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div v-else class="empty">还没有套题记录，点上面的「开始模考」就是第一次。</div>
      </section>
    </div>
  </AppShell>
</template>

<script setup>
/**
 * 真题套题目录页
 * 数据来源：GET /api/fullSuite/list（套题目录 + 四模块题量）
 *           GET /api/fullExam/records（我的套题记录）
 * 开始模考：POST /api/fullExam/start → 拿到 recordId 后跳到答题页
 */
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import AppShell from '@/components/AppShell.vue'
import ModeSwitch from '@/components/ModeSwitch.vue'

const router = useRouter()
const loading = ref(false)
const startingId = ref(null)
const generating = ref(false)
const suites = ref([])
const records = ref([])

const userId = ref(1)

function loadUser() {
  const str = localStorage.getItem('userInfo')
  if (str) {
    try {
      const info = JSON.parse(str)
      if (info.userId) userId.value = info.userId
    } catch (e) {
      // 解析失败就用兜底 1
    }
  }
}

async function loadSuites() {
  loading.value = true
  try {
    const res = await request.get('/api/fullSuite/list')
    if (res.code === 200) {
      suites.value = res.data || []
    } else {
      ElMessage.error(res.msg || '套题加载失败')
    }
  } catch (e) {
    // 拦截器已经提示过错误，这里不重复弹
  } finally {
    loading.value = false
  }
}

async function loadRecords() {
  try {
    const res = await request.get('/api/fullExam/records', { params: { userId: userId.value } })
    if (res.code === 200) {
      records.value = res.data || []
    }
  } catch (e) {
    // 忽略
  }
}

/* 一键生成套题：后端从单模块练习题库随机抽 阅读/听力/口语/写作 各 1 篇，
   复制成一套全新的套题（写入 full_* 表），生成完立刻刷新列表 */
async function generateSuite() {
  generating.value = true
  try {
    const res = await request.post('/api/fullSuite/generate', {
      reading: 1,
      listening: 1,
      speaking: 1,
      writing: 1
    })
    if (res.code !== 200) {
      ElMessage.error(res.msg || '生成套题失败')
      return
    }
    const d = res.data || {}
    ElMessage.success(
      `已生成「${d.title}」：阅读 ${d.readingCount || 0} 题 / 听力 ${d.listeningCount || 0} 题 / ` +
      `口语 ${d.speakingCount || 0} 题 / 写作 ${d.writingCount || 0} 题`
    )
    await loadSuites()
  } catch (e) {
    // 拦截器已提示
  } finally {
    generating.value = false
  }
}

/* 开始一套新模考：先建记录，再进答题页 */
async function startExam(suite) {
  startingId.value = suite.id
  try {
    const res = await request.post('/api/fullExam/start', { suiteId: suite.id, userId: userId.value })
    if (res.code !== 200) {
      ElMessage.error(res.msg || '开始失败')
      return
    }
    const record = res.data
    // 新记录：清掉同一套题可能残留的本地缓存，避免串答案
    clearLocalCache(record.id)
    router.push({ path: '/full/exam', query: { recordId: record.id, suiteId: suite.id } })
  } catch (e) {
    // 拦截器已提示
  } finally {
    startingId.value = null
  }
}

/* 继续未完成的记录：直接回答题页，本地缓存按 recordId 自动恢复 */
function resumeExam(row) {
  router.push({ path: '/full/exam', query: { recordId: row.id, suiteId: row.suiteId } })
}

function viewResult(row) {
  router.push({ path: '/full/result', query: { recordId: row.id } })
}

async function removeRecord(row) {
  try {
    await ElMessageBox.confirm('删除这条记录后无法恢复（含逐题作答与 AI 反馈），确定删除吗？', '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch (e) {
    return
  }
  try {
    const res = await request.delete('/api/fullExam/record/' + row.id)
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

/* 本地缓存按 recordId 隔离，删记录时一起清掉 */
function clearLocalCache(recordId) {
  localStorage.removeItem('fullExam:answers:' + recordId)
  localStorage.removeItem('fullExam:progress:' + recordId)
  localStorage.removeItem('fullExam:submit:' + recordId)
}

function formatDuration(seconds) {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${m} 分 ${s} 秒`
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
  margin-bottom: 26px;
}

.suite-card {
  padding: 20px 22px 18px;
  display: flex;
  flex-direction: column;
  transition: transform 0.16s ease, box-shadow 0.16s ease;
}

.suite-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-md);
}

.suite-card__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

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

.suite-card__minutes {
  font-size: 12px;
  color: var(--ink-500);
}

.suite-card__title {
  margin: 0 0 8px;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.3px;
}

.suite-card__desc {
  margin: 0 0 16px;
  font-size: 13px;
  line-height: 1.65;
  color: var(--ink-500);
  min-height: 42px;
}

.suite-card__stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  padding: 12px;
  border-radius: var(--radius-md);
  background: var(--surface-2);
  border: 1px solid var(--line);
}

.stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.stat strong {
  font-size: 18px;
  font-variant-numeric: tabular-nums;
  color: var(--brand-700);
}

.stat span { font-size: 11px; color: var(--ink-500); }

.suite-card__foot {
  margin-top: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.suite-card__count { font-size: 12px; color: var(--ink-300); }

.records__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.records__head :deep(.section-title) { margin-bottom: 0; }

.records :deep(.el-table) { margin-top: 12px; }
.records :deep(.el-table th) {
  background: var(--surface-2) !important;
  color: var(--ink-700);
  font-weight: 600;
}

.band-badge small { font-size: 11px; color: var(--ink-300); font-weight: 600; }
.muted { color: var(--ink-300); }
</style>
