<template>
  <div class="page">
    <el-card shadow="hover">
      <template #header>
        <div class="head">
          <div class="title-wrap">
            <span class="title-icon">📖</span>
            <span class="page-title">托福真题套题模考</span>
          </div>
          <div class="btn-group">
            <el-button @click="$router.push('/tpo/suite/records')">模考记录</el-button>
            <el-button type="primary" @click="initSuite">一键生成套题</el-button>
            <el-button @click="$router.push('/tpo')">返回 TPO 首页</el-button>
          </div>
        </div>
      </template>

      <div class="card-wrap">
        <div v-if="suiteList.length === 0" class="empty">
          <div class="empty-icon">📚</div>
          <div class="empty-text">暂无套题</div>
          <div class="empty-tip">点击右上角「一键生成套题」快速创建TPO完整模考卷</div>
        </div>

        <div v-for="item in suiteList" :key="item.suiteId" class="suite-card">
          <div class="suite-top">
            <div class="tpo-name">{{ item.title }}</div>
            <el-button type="primary" @click="startExam(item)">开始整卷模考</el-button>
          </div>
          <div class="divider"></div>
          <div class="suite-desc">
            <div class="item-row reading">
              <span class="label">📖 阅读</span>
              <span class="value">{{ item.readingCount || 0 }} 篇</span>
            </div>
            <div class="item-row listening">
              <span class="label">🎧 听力</span>
              <span class="value">{{ item.listeningCount || 0 }} 篇</span>
            </div>
            <div class="item-row speaking">
              <span class="label">🎤 口语</span>
              <span class="value">{{ item.speakingCount || 0 }} 题</span>
            </div>
            <div class="item-row writing">
              <span class="label">✍️ 写作</span>
              <span class="value">{{ item.writingCount || 0 }} 题</span>
            </div>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import service from '@/utils/request.js'

const router = useRouter()
const suiteList = ref([])

const loadSuiteList = async () => {
  try {
    const res = await service.get('/api/suite/list')
    if (res.code === 200) suiteList.value = res.data
  } catch (e) {
    ElMessage.error('加载套题列表失败')
  }
}

const initSuite = async () => {
  try {
    const res = await service.post('/api/suite/init')
    if (res.code === 200) {
      ElMessage.success('套题生成成功！')
      loadSuiteList()
    } else {
      ElMessage.warning(res.msg || '生成失败')
    }
  } catch (e) {
    ElMessage.error('生成套题接口异常')
  }
}

/**
 * 开始整卷模考。
 * 原来这段有两个问题：
 *  1) 直接跳 /tpo/suite/exam 但不带记录号，考试页无从提交；
 *  2) 还留着一个 startFullExam()，调的是后端根本不存在的 /api/fullSuiteExam/start。
 * 现在统一成：先调 /api/examRecord/start 建一条 doing 记录，再带记录号进考试页。
 */
const startExam = async (item) => {
  try {
    const res = await service.post('/api/examRecord/start', { suiteId: item.suiteId })
    if (res.code !== 200) {
      ElMessage.warning(res.msg || '开始模考失败')
      return
    }
    router.push({
      path: '/tpo/suite/exam',
      query: { suiteId: item.suiteId, recordId: res.data.id }
    })
  } catch (e) {
    ElMessage.error('开始模考接口异常')
    console.error(e)
  }
}

onMounted(loadSuiteList)
</script>

<style scoped>
.page {
  padding: 28px;
  background: #f1f5f9;
  min-height: 100vh;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC",
    "Hiragino Sans GB", "Microsoft YaHei", sans-serif;
}

.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}
.title-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}
.title-icon {
  font-size: 24px;
}
.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: 0.5px;
}
.btn-group {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.card-wrap {
  margin-top: 28px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 24px;
}

.empty {
  text-align: center;
  padding: 80px 20px;
  color: #94a3b8;
}
.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.75;
}
.empty-text {
  font-size: 19px;
  font-weight: 600;
  color: #64748b;
  margin-bottom: 10px;
}
.empty-tip {
  font-size: 14px;
  color: #94a3b8;
}

.suite-card {
  position: relative;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  padding: 26px;
  background: #ffffff;
  transition: all 0.3s ease;
  overflow: hidden;
}
/* 和suiteExam一致左侧渐变竖条 */
.suite-card::before {
  content: "";
  position: absolute;
  left: 0;
  top: 0;
  width: 5px;
  height: 100%;
  background: linear-gradient(180deg, #6366f1, #8b5cf6);
}
.suite-card:hover {
  transform: translateY(-7px);
  box-shadow: 0 24px 44px rgba(15, 23, 42, 0.11);
  border-color: #c7d2fe;
}

.suite-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.tpo-name {
  font-weight: 700;
  font-size: 18px;
  color: #0f172a;
}
.divider {
  height: 1px;
  background: linear-gradient(90deg, transparent, #e2e8f0, transparent);
  margin: 18px 0;
}

.suite-desc {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px 10px;
}
.item-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 10px;
  border-radius: 12px;
  font-size: 14px;
}
.item-row.reading {
  background: #f0f4ff;
}
.item-row.listening {
  background: #ecfeff;
}
.item-row.speaking {
  background: #fffbeb;
}
.item-row.writing {
  background: #f0fdf4;
}
.label {
  color: #475569;
}
.value {
  font-weight: 600;
  color: #0f172a;
}
</style>
