<template>
  <div class="page">
    <div class="page-wrap">
      <div class="page-header">
        <div class="title">
          <span class="title-icon">📋</span>
          <span>模考记录</span>
        </div>
        <el-button type="primary" plain @click="$router.push('/tpo/suite/list')">返回套题列表</el-button>
      </div>

      <el-card shadow="hover" class="record-card">
        <el-table :data="tableData" border stripe class="record-table">
          <el-table-column prop="suiteName" label="套题名称" />
          <el-table-column label="总分">
            <template #default="scope">
              <span class="total-score">{{ scope.row.totalScore ?? '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="分项">
            <template #default="scope">
              <div class="score-item">R:{{ scope.row.readScore ?? '-' }} | L:{{ scope.row.listenScore ?? '-' }} | S:{{ scope.row.speakScore ?? '-' }} | W:{{ scope.row.writeScore ?? '-' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="状态">
            <template #default="scope">
              <el-tag :type="scope.row.examStatus === 'doing' ? 'warning' : 'success'">
                {{ scope.row.examStatus === 'doing' ? '进行中' : '已完成' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="startTime" label="开始时间" />
          <el-table-column prop="endTime" label="结束时间" />
          <el-table-column label="操作">
            <template #default="scope">
              <el-button size="small" @click="goExam(scope.row)" v-if="scope.row.examStatus === 'doing'">继续模考</el-button>
              <el-button size="small" type="success" @click="viewResult(scope.row)" v-else>查看成绩</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import service from '@/utils/request.js'

const router = useRouter()
const tableData = ref([])

const loadList = async () => {
  try {
    const res = await service.get('/api/examRecord/list')
    if (res.code === 200) tableData.value = res.data
  } catch (e) {
    ElMessage.error('加载模考记录失败')
  }
}

const goExam = (row) => {
  router.push({ path: '/tpo/suite/exam', query: { suiteId: row.suiteId, recordId: row.id } })
}
const viewResult = (row) => {
  // 成绩详情页，后面可以单独做，这里先弹窗/跳转到成绩页
  router.push({ path: '/tpo/suite/result', query: { recordId: row.id } })
}

onMounted(loadList)
</script>

<style scoped>
.page {
  padding: 20px;
  background-color: #f7f8fa;
  min-height: 100vh;
}
.page-wrap {
  max-width: 1200px;
  margin: 0 auto;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}
.title-icon {
  font-size: 22px;
}
.record-card {
  border-radius: 12px;
}
.record-table {
  font-size: 14px;
}
.total-score {
  font-weight: bold;
  color: #409eff;
  font-size:16px;
}
.score-item {
  color:#606266;
  font-size:13px;
}
</style>
