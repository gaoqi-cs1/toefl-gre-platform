<template>
  <AppShell>
    <div class="page">
      <!-- 页头 -->
      <div class="page-head">
        <div>
          <h1 class="page-title">GRE 错题本</h1>
          <p class="page-sub">
            分块练习答错、套题提交判错的题都会自动收进来（分块题 scope=practice、套题题 scope=suite，
            两套题库的题目 id 各自独立，不会互相覆盖）。答对一次就自动移出，也可以手动标记「已掌握」。
          </p>
        </div>
        <div class="head-actions">
          <el-button round @click="load">刷新</el-button>
          <el-button type="primary" round link @click="goGreHome">返回 GRE 首页</el-button>
        </div>
      </div>

      <!-- 模块统计 + 筛选 -->
      <section class="stats card card-pad">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          type="button"
          :class="['stat-tab', { 'is-active': activeModule === tab.key }]"
          @click="switchModule(tab.key)"
        >
          <span class="stat-tab__label">{{ tab.label }}</span>
          <span class="stat-tab__count">{{ tab.key === 'all' ? items.length : (stats[tab.key] || 0) }}</span>
        </button>
        <span class="stats__tip">共 {{ items.length }} 道待复习</span>
      </section>

      <!-- 错题列表 -->
      <section v-loading="loading" class="wrong-list">
        <article v-for="item in items" :key="item.id" class="wrong-card card">
          <header class="wrong-card__head">
            <div class="wrong-card__meta">
              <span class="pill pill--muted">{{ moduleLabel(item.moduleType) }}</span>
              <span class="pill pill--muted">{{ item.lastSource === 'exam' ? '来自套题' : '来自分块练习' }}</span>
              <span class="pill pill--bad">错 {{ item.wrongCount }} 次</span>
              <span class="wrong-card__time">{{ formatTime(item.updateTime) }}</span>
            </div>
            <el-button text type="danger" size="small" @click="removeItem(item)">移出错题本</el-button>
          </header>

          <h3 class="wrong-card__title">{{ cleanText(item.questionTitle) }}</h3>

          <!-- 选项：正确答案标绿，我的错误答案标红 -->
          <ul class="wrong-card__options">
            <li
              v-for="opt in optionsOf(item)"
              :key="opt.key"
              :class="['option', {
                'option--correct': opt.key === item.correctAnswer,
                'option--wrong': opt.key === (item.userAnswer || '').toUpperCase() && opt.key !== item.correctAnswer
              }]"
            >
              <span class="option__key">{{ opt.key }}</span>
              <span class="option__text">{{ cleanText(opt.text) }}</span>
              <span v-if="opt.key === item.correctAnswer" class="option__tag">正确答案</span>
              <span
                v-else-if="opt.key === (item.userAnswer || '').toUpperCase()"
                class="option__tag option__tag--wrong"
              >我的答案</span>
            </li>
          </ul>

          <div class="wrong-card__answer">
            <span>我的答案：<strong class="is-bad">{{ item.userAnswer || '空（未作答）' }}</strong></span>
            <span>正确答案：<strong class="is-good">{{ item.correctAnswer || '—' }}</strong></span>
          </div>

          <!-- 解析（中英双语） -->
          <div v-if="item.analysis" class="wrong-card__analysis">
            <div class="wrong-card__analysis-title">解析 · Analysis</div>
            <div class="analysis-body">{{ cleanText(item.analysis) }}</div>
          </div>

          <footer class="wrong-card__foot">
            <el-button size="small" round @click="markMastered(item)">标记为已掌握</el-button>
            <el-button size="small" text type="primary" @click="reviewQuestion(item)">去重做这道题</el-button>
          </footer>
        </article>

        <div v-if="!loading && items.length === 0" class="empty card card-pad">
          错题本还是空的。去「分块练习」做几题，或者做一套套题，答错的题会自动出现在这里。
        </div>
      </section>
    </div>
  </AppShell>
</template>

<script setup>
/**
 * GRE 错题本页面
 *
 * 数据全部来自 GRE 自己的接口（不碰托福 /api/wrongbook）：
 *   GET    /api/greWrong/list?userId=&moduleType=&includeMastered=
 *   POST   /api/greWrong/master   { id, mastered }
 *   DELETE /api/greWrong/{id}
 *
 * 列表里的 option_a..f、correct_answer、analysis 都是答错那一刻从题库复制过来的快照，
 * 所以题库以后改了题，回看这里的记录仍然是当时那道题。
 */
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import AppShell from '@/components/AppShell.vue'

const router = useRouter()

const items = ref([])
const stats = ref({ verbal: 0, quant: 0, writing: 0 })
const loading = ref(false)
const activeModule = ref('all')
const userId = ref(1)

const tabs = [
  { key: 'all', label: '全部' },
  { key: 'verbal', label: 'Verbal' },
  { key: 'quant', label: 'Quant' },
  { key: 'writing', label: 'Writing' }
]

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

async function load() {
  loading.value = true
  try {
    const params = { userId: userId.value, includeMastered: false }
    if (activeModule.value !== 'all') params.moduleType = activeModule.value
    const res = await request.get('/api/greWrong/list', { params })
    if (res.code === 200) {
      items.value = res.data.items || []
      stats.value = res.data.stats || { verbal: 0, quant: 0, writing: 0 }
    } else {
      ElMessage.error(res.msg || '加载失败')
    }
  } catch (e) {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

function switchModule(key) {
  activeModule.value = key
  load()
}

async function markMastered(item) {
  try {
    const res = await request.post('/api/greWrong/master', { id: item.id, mastered: true })
    if (res.code === 200) {
      ElMessage.success('已标记为掌握，不再出现在列表里')
      // 本地先移除，再拉一次统计
      items.value = items.value.filter((row) => row.id !== item.id)
      load()
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  } catch (e) {
    // 拦截器已提示
  }
}

async function removeItem(item) {
  try {
    await ElMessageBox.confirm('把这道题从错题本移除？以后再做错会重新收进来。', '移除确认', {
      type: 'warning',
      confirmButtonText: '移除',
      cancelButtonText: '取消'
    })
  } catch (e) {
    return
  }
  try {
    const res = await request.delete('/api/greWrong/' + item.id)
    if (res.code === 200) {
      ElMessage.success('已移出错题本')
      load()
    } else {
      ElMessage.error(res.msg || '移除失败')
    }
  } catch (e) {
    // 拦截器已提示
  }
}

/** 分块练习的题可以带着 sectionId 跳回去重做；套题题没有 sectionId，只能回 GRE 首页 */
function reviewQuestion(item) {
  if (item.questionScope === 'suite' || !item.sectionId) {
    ElMessage.info('这道题来自套题，重做请回 GRE 首页选套题')
    return
  }
  router.push({ path: '/gre/practice/' + item.sectionId, query: { focus: item.questionId } })
}

function goGreHome() {
  router.push('/gre')
}

/** 把 optionA..F 拼成前端循环用的小数组 */
const optionsOf = computed(() => (item) => {
  const keys = ['A', 'B', 'C', 'D', 'E', 'F']
  const values = [item.optionA, item.optionB, item.optionC, item.optionD, item.optionE, item.optionF]
  return keys
    .map((key, index) => ({ key, text: values[index] }))
    .filter((opt) => opt.text && String(opt.text).trim() !== '')
})

/** 数据库里存的是真实换行，这里把残留的字面量 \n 也顺手清掉 */
function cleanText(text) {
  if (!text) return ''
  return String(text).replace(/\\n/g, '\n')
}

function moduleLabel(moduleType) {
  if (moduleType === 'writing') return 'Analytical Writing'
  if (moduleType === 'verbal') return 'Verbal Reasoning'
  if (moduleType === 'quant') return 'Quantitative Reasoning'
  return moduleType || '—'
}

function formatTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 16)
}

onMounted(() => {
  loadUser()
  load()
})
</script>

<style scoped>
.head-actions { display: flex; align-items: center; gap: 10px; }

.stats {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}

.stat-tab {
  appearance: none;
  border: 1px solid var(--line);
  background: #fff;
  border-radius: 999px;
  padding: 8px 16px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  transition: border-color 0.15s ease, background 0.15s ease;
}

.stat-tab:hover { border-color: var(--brand-100); }

.stat-tab.is-active {
  background: var(--brand-900);
  border-color: var(--brand-900);
  color: #fff;
}

.stat-tab__label { font-size: 13px; font-weight: 600; }

.stat-tab__count {
  font-size: 12px;
  font-weight: 800;
  padding: 1px 8px;
  border-radius: 999px;
  background: var(--brand-50);
  color: var(--brand-700);
  font-variant-numeric: tabular-nums;
}

.stat-tab.is-active .stat-tab__count { background: rgba(255, 255, 255, 0.2); color: #fff; }

.stats__tip { margin-left: auto; font-size: 12px; color: var(--ink-300); }

.wrong-list { display: flex; flex-direction: column; gap: 16px; }

.wrong-card { padding: 20px 22px; }

.wrong-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.wrong-card__meta { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }

.wrong-card__time { font-size: 12px; color: var(--ink-300); }

.wrong-card__title { margin: 0 0 12px; font-size: 16px; line-height: 1.6; font-weight: 600; }

.wrong-card__options {
  list-style: none;
  margin: 0 0 12px;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.option {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  border: 1px solid var(--line);
  background: var(--surface-2);
  border-radius: var(--radius-md);
  padding: 10px 12px;
  font-size: 13px;
  line-height: 1.55;
}

.option--correct { border-color: #17936a; background: #f0fbf5; }
.option--wrong { border-color: #c0392b; background: #fdf3f2; }

.option__key {
  flex: none;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-weight: 700;
  font-size: 12px;
  background: #fff;
  border: 1px solid var(--line);
}

.option__text { flex: 1; }

.option__tag {
  flex: none;
  font-size: 11px;
  font-weight: 700;
  color: #17936a;
  background: #e7f7ef;
  border-radius: 999px;
  padding: 2px 8px;
}

.option__tag--wrong { color: #c0392b; background: #fdecea; }

.wrong-card__answer {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
  font-size: 13px;
  color: var(--ink-500);
  margin-bottom: 10px;
}

.is-bad { color: #c0392b; }
.is-good { color: #17936a; }

.wrong-card__analysis {
  border-left: 3px solid var(--brand-100);
  background: var(--surface-3);
  border-radius: var(--radius-sm);
  padding: 12px 14px;
  margin-bottom: 12px;
}

.wrong-card__analysis-title {
  font-size: 12px;
  font-weight: 700;
  color: var(--brand-700);
  margin-bottom: 6px;
  letter-spacing: 0.2px;
}

.analysis-body {
  font-size: 13px;
  line-height: 1.75;
  color: var(--ink-700);
  white-space: pre-wrap;
}

.wrong-card__foot { display: flex; gap: 10px; }
</style>
