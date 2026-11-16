<template>
  <div class="exam-page">
    <header class="exam-header">
      <div class="header-left">
        <span class="title">我的做题记录</span>
        <span class="count">{{ recordList.length }} 次</span>
      </div>
      <div class="header-right">
        <button class="btn danger" :disabled="recordList.length === 0" @click="clearAll">清空做题记录</button>
        <button class="btn ghost" @click="$router.push('/suite')">返回套题</button>
      </div>
    </header>

    <div v-if="recordList.length === 0" class="empty">暂无做题记录</div>

    <div v-else class="record-wrap">
      <div v-for="item in recordList" :key="item.recordId" class="record-card">
        <div class="card-top">
          <div class="suite-name">{{ item.suiteTitle }}</div>
          <div class="score">
            <span class="score-num">{{ item.correctCount }}</span>
            <span class="score-total">/{{ item.totalCount }}</span>
          </div>
        </div>

        <div class="detail-row">
          <div class="detail-item">
            <span class="d-name">阅读</span>
            <span class="d-val">{{ item.readingCorrect }} / {{ item.readingTotal }}</span>
          </div>
          <div class="detail-item">
            <span class="d-name">听力</span>
            <span class="d-val">{{ item.listeningCorrect }} / {{ item.listeningTotal }}</span>
          </div>
          <div class="detail-item">
            <span class="d-name">口语</span>
            <span class="d-val">{{ item.speakingCount }} 题</span>
          </div>
          <div class="detail-item">
            <span class="d-name">写作</span>
            <span class="d-val">{{ item.writingCount }} 篇</span>
          </div>
        </div>

        <div class="meta-row">
          <span class="meta">{{ formatDate(item.createTime) }}</span>
          <span class="dot">·</span>
          <span class="meta">用时 {{ formatCost(item.costSeconds) }}</span>
        </div>

        <div class="card-action">
          <button class="btn primary" @click="viewDetail(item)">查看详情</button>
          <button class="btn ghost danger" @click="delOne(item)">删除本条</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const recordList = ref([])
const userId = 1

const getList = async () => {
  try {
    const res = await axios.get('/api/suite/record/list', { params: { userId } })
    recordList.value = res.data.data || []
  } catch (err) {
    ElMessage.error('加载记录失败')
    console.error(err)
  }
}

const formatDate = (t) => {
  if (!t) return '--'
  return String(t).replace('T', ' ').substring(0, 16)
}

const formatCost = (s) => {
  const v = s || 0
  const h = Math.floor(v / 3600)
  const m = Math.floor((v % 3600) / 60)
  const sec = v % 60
  if (h > 0) return `${h}小时${m}分`
  return `${m}分${sec}秒`
}

const viewDetail = (item) => {
  router.push({ path: '/suite/result', query: { recordId: item.recordId } })
}

const delOne = async (item) => {
  try {
    await ElMessageBox.confirm('删除这条做题记录？删除后不可恢复', '提示', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await axios.delete('/api/suite/record/del', { params: { recordId: item.recordId } })
    ElMessage.success('删除成功')
    getList()
  } catch (e) {}
}

const clearAll = async () => {
  try {
    await ElMessageBox.confirm(
      '将清空全部做题记录（含整卷记录、每题作答明细、单篇练习答案），此操作不可恢复，确认继续？',
      '清空做题记录',
      { confirmButtonText: '确认清空', cancelButtonText: '取消', type: 'error' }
    )
    const res = await axios.delete('/api/suite/record/clear', { params: { userId } })
    if (res.data.code === 200) {
      const d = res.data.data
      ElMessage.success(`已清空：整卷 ${d.recordDeleted} 条，答题明细 ${d.answerDeleted} 条，单题作答 ${d.singleAnswerDeleted} 条`)
      getList()
    } else {
      ElMessage.warning(res.data.msg)
    }
  } catch (e) {}
}

onMounted(getList)
</script>

<style scoped>
.exam-page { min-height: 100vh; background: #f4f6fb; padding: 18px; box-sizing: border-box; }
.exam-header {
  display: flex; justify-content: space-between; align-items: center;
  background: #fff; border-radius: 14px; padding: 12px 18px;
  box-shadow: 0 2px 12px rgba(28, 50, 92, 0.06); margin-bottom: 16px;
}
.header-left { display: flex; align-items: center; gap: 10px; }
.header-right { display: flex; gap: 10px; }
.title { font-size: 16px; font-weight: 600; color: #1f2d3d; }
.count { font-size: 12px; color: #7d8ca1; background: #f2f4f8; padding: 2px 8px; border-radius: 6px; }
.btn { border: none; cursor: pointer; border-radius: 9px; font-size: 13px; padding: 8px 14px; transition: all 0.18s; }
.btn:disabled { opacity: 0.45; cursor: not-allowed; }
.btn.primary { background: #2b6ef2; color: #fff; }
.btn.primary:hover { background: #1b5cd8; }
.btn.ghost { background: #f2f4f8; color: #5a6b82; }
.btn.ghost:hover { background: #e6eaf2; }
.btn.ghost.danger { color: #dd5252; }
.btn.danger { background: #fdecec; color: #dd5252; }
.btn.danger:hover { background: #fbdede; }

.record-wrap { display: grid; grid-template-columns: repeat(auto-fill, minmax(400px, 1fr)); gap: 14px; }
.record-card {
  background: #fff; border-radius: 14px; padding: 16px;
  box-shadow: 0 2px 12px rgba(28, 50, 92, 0.06);
  display: flex; flex-direction: column; gap: 12px;
}
.card-top { display: flex; align-items: center; justify-content: space-between; }
.suite-name { font-size: 17px; font-weight: 700; color: #1f2d3d; }
.score { display: flex; align-items: baseline; }
.score-num { font-size: 26px; font-weight: 700; color: #2b6ef2; line-height: 1; }
.score-total { font-size: 14px; color: #8a99ad; }

.detail-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; }
.detail-item {
  background: #f8fafd; border-radius: 9px; padding: 8px 10px;
  display: flex; flex-direction: column; gap: 3px;
}
.d-name { font-size: 11.5px; color: #8a99ad; }
.d-val { font-size: 13.5px; font-weight: 600; color: #1f2d3d; }

.meta-row { font-size: 12.5px; color: #8a99ad; }
.dot { margin: 0 6px; }
.card-action { display: flex; gap: 10px; justify-content: flex-end; }
.empty { text-align: center; padding: 60px 0; color: #a3aec1; }
</style>
