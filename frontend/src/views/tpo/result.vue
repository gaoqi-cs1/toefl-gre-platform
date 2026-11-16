<template>
  <div class="page" style="padding:20px;background:#f7f8fa;min-height:100vh">
    <div style="max-width:700px;margin:0 auto;">
      <div class="page-header" style="display:flex;justify-content:space-between;align-items:center;margin-bottom:20px;">
        <span style="font-size:22px;font-weight:600">托福模考成绩</span>
        <el-button @click="$router.push('/tpo/suite/records')">返回记录</el-button>
      </div>
      <el-card shadow="hover" class="score-card">
        <div class="score-title">{{info.suiteName}}</div>
        <div class="total-box">
          <div class="total-label">总分</div>
          <div class="total-num">{{info.totalScore}}</div>
        </div>
        <div class="score-grid">
          <div class="score-item">
            <div class="tag r">阅读 R</div>
            <div class="num">{{info.readScore}}</div>
          </div>
          <div class="score-item">
            <div class="tag l">听力 L</div>
            <div class="num">{{info.listenScore}}</div>
          </div>
          <div class="score-item">
            <div class="tag s">口语 S</div>
            <div class="num">{{info.speakScore}}</div>
            <div class="tip">待AI评分</div>
          </div>
          <div class="score-item">
            <div class="tag w">写作 W</div>
            <div class="num">{{info.writeScore}}</div>
            <div class="tip">待AI评分</div>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>
<script setup>
import {ref,onMounted} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import service from '@/utils/request.js'
import {ElMessage} from 'element-plus'
const route = useRoute()
const router = useRouter()
const info = ref({})

const load = async ()=>{
  try {
    const res = await service.get(`/api/examRecord/${route.query.recordId}`)
    if(res.code===200) info.value = res.data
  } catch (e) {
    ElMessage.error("成绩记录不存在")
    router.push('/tpo/suite/records')
  }
}
onMounted(load)
</script>

<style scoped>
.score-card{border-radius:14px;padding:20px;text-align:center;}
.score-title{font-size:18px;margin-bottom:16px;color:#333;}
.total-box{background:#409eff;color:white;border-radius:12px;padding:16px;margin-bottom:20px;}
.total-label{font-size:14px;opacity:0.9}
.total-num{font-size:42px;font-weight:bold;}
.score-grid{display:grid;grid-template-columns: 1fr 1fr 1fr 1fr;gap:12px;}
.score-item{border:1px solid #e5e7eb;border-radius:10px;padding:14px;}
.tag{display:inline-block;color:#fff;padding:2px 8px;border-radius:6px;font-size:13px;margin-bottom:8px;}
.r{background:#67c23a;}
.l{background:#e6a23c;}
.s{background:#f56c6c;}
.w{background:#909399;}
.num{font-size:28px;font-weight:bold;color:#303133;}
.tip{font-size:12px;color:#999;margin-top:4px;}
</style>
