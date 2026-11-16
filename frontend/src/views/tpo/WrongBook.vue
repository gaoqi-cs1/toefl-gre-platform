<template>
  <div class="exam-page">
    <header class="exam-header">
      <div class="header-left">
        <span class="title">我的错题本</span>
        <span class="count">{{ wrongList.length }} 道</span>
      </div>
      <div class="header-right">
        <button class="btn ghost" @click="goTpo">返回 TPO</button>
      </div>
    </header>

    <div class="card-wrap">
      <div v-if="wrongList.length === 0" class="empty">暂无错题</div>

      <div v-for="item in wrongList" :key="item.id" class="wrong-card">
        <div class="card-top">
          <span class="type-tag" :class="item.sectionType">{{ item.sectionType }}</span>
          <span class="q-title">{{ item.questionTitle }}</span>
        </div>

        <ul v-if="isObjective(item.sectionType)" class="option-list">
          <li
            v-for="opt in buildOptions(item)"
            :key="opt.key"
            class="option-item"
            :class="{
              correct: opt.key === item.correctAnswer,
              mine: opt.key === item.userAnswer
            }"
          >
            <span class="opt-key">{{ opt.key }}</span>
            <span class="opt-text">{{ opt.text || '暂无内容' }}</span>
            <span v-if="opt.key === item.correctAnswer" class="mark correct-mark">正确答案</span>
            <span v-if="opt.key === item.userAnswer" class="mark mine-mark">我的答案</span>
          </li>
        </ul>
        <div v-else class="no-option">该题为主观题，无选项</div>

        <div class="answer-line">
          <span>我的答案：<b>{{ item.userAnswer || '空' }}</b></span>
          <span>正确答案：<b class="right">{{ item.correctAnswer || '暂无' }}</b></span>
          <span class="state" :class="item.isCorrect === 1 ? 'ok' : 'bad'">
            {{ item.isCorrect === 1 ? '答对' : '答错' }}
          </span>
        </div>

        <div class="card-action">
          <button class="btn primary" @click="goDo(item)">去做题</button>
          <button class="btn ghost danger" @click="delWrong(item.id)">移除错题</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
// 统一走封装 service（自动带 token、统一错误提示），原来是裸 axios
import service from '@/utils/request'

const router = useRouter()
const wrongList = ref([])

const readUserId = () => {
  try {
    const info = JSON.parse(localStorage.getItem('userInfo') || '{}')
    return Number(info.userId) || 1
  } catch (e) {
    return 1
  }
}
const userId = readUserId()

const goTpo = () => router.push('/tpo')

// 后端 /api/wrong/list 会把题目选项、正确答案、解析、篇章类型和"我的答案"一起组装返回
const getList = async () => {
  try {
    const res = await service.get('/api/wrong/list', { params: { userId } })
    if (res.code === 200) {
      wrongList.value = res.data || []
    } else {
      ElMessage.warning(res.msg || '加载错题失败')
    }
  } catch (err) {
    ElMessage.error('加载错题失败')
    console.error(err)
  }
}

const isObjective = (type) => type === 'reading' || type === 'listening'

const buildOptions = (item) => [
  { key: 'A', text: item.optionA },
  { key: 'B', text: item.optionB },
  { key: 'C', text: item.optionC },
  { key: 'D', text: item.optionD }
]

const goDo = (item) => {
  let routeName
  if (item.sectionType === 'reading') routeName = 'ReadingExam'
  else if (item.sectionType === 'listening') routeName = 'ListeningExam'
  else if (item.sectionType === 'speaking') routeName = 'SpeakingExam'
  else if (item.sectionType === 'writing') routeName = 'WritingExam'
  else {
    ElMessage.warning('未知篇章类型：' + item.sectionType)
    return
  }
  if (!item.sectionId) {
    ElMessage.warning('该错题没有关联篇章，无法跳转做题')
    return
  }
  router.push({
    name: routeName,
    params: { sectionId: item.sectionId },
    query: { jumpQid: item.questionId }
  })
}

const delWrong = async (wid) => {
  try {
    const res = await service.delete('/api/wrong/del', { params: { id: wid } })
    if (res.code === 200) {
      ElMessage.success('已移除错题')
      getList()
    } else {
      ElMessage.warning(res.msg || '移除失败')
    }
  } catch (err) {
    ElMessage.error('移除错题失败')
    console.error(err)
  }
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
.title { font-size: 16px; font-weight: 600; color: #1f2d3d; }
.count {
  font-size: 12px; color: #7d8ca1; background: #f2f4f8;
  padding: 2px 8px; border-radius: 6px;
}
.btn {
  border: none; cursor: pointer; border-radius: 9px; font-size: 13px;
  padding: 8px 14px; transition: all 0.18s;
}
.btn.primary { background: #2b6ef2; color: #fff; }
.btn.primary:hover { background: #1b5cd8; }
.btn.ghost { background: #f2f4f8; color: #5a6b82; }
.btn.ghost:hover { background: #e6eaf2; }
.btn.ghost.danger { color: #dd5252; }

.card-wrap { display: grid; grid-template-columns: repeat(auto-fill, minmax(520px, 1fr)); gap: 14px; }
.wrong-card {
  background: #fff; border-radius: 14px; padding: 16px;
  box-shadow: 0 2px 12px rgba(28, 50, 92, 0.06);
  display: flex; flex-direction: column; gap: 12px;
}
.card-top { display: flex; align-items: flex-start; gap: 10px; }
.type-tag {
  font-size: 12px; padding: 3px 8px; border-radius: 5px;
  color: #fff; white-space: nowrap; flex: none;
}
.type-tag.reading { background: #2b6ef2; }
.type-tag.listening { background: #14a094; }
.type-tag.speaking { background: #ec8a1f; }
.type-tag.writing { background: #6b5be0; }
.q-title { font-size: 14px; color: #35404f; line-height: 1.6; }

.option-list { list-style: none; margin: 0; padding: 0; }
.option-item {
  display: flex; align-items: flex-start; gap: 8px; padding: 8px 10px;
  margin-bottom: 6px; border: 1px solid #eef1f6; border-radius: 8px; background: #fbfcfe;
}
.option-item.correct { border-color: #22a06b; background: #f1fbf6; }
.option-item.mine { border-color: #f0a13a; background: #fff9f0; }
.option-item.correct.mine { border-color: #22a06b; background: #f1fbf6; }
.opt-key {
  width: 20px; height: 20px; border-radius: 5px; background: #eef2f9; color: #5a6b82;
  font-size: 12px; font-weight: 700; display: inline-flex; align-items: center;
  justify-content: center; flex: none;
}
.option-item.correct .opt-key { background: #22a06b; color: #fff; }
.option-item.mine:not(.correct) .opt-key { background: #f0a13a; color: #fff; }
.opt-text { font-size: 13.5px; line-height: 1.6; color: #35404f; flex: 1; }
.mark { font-size: 11px; padding: 1px 6px; border-radius: 4px; white-space: nowrap; align-self: center; }
.correct-mark { color: #22a06b; background: #e7f8f0; }
.mine-mark { color: #d98b1c; background: #fff2dd; }

.answer-line {
  display: flex; align-items: center; gap: 16px; font-size: 13px;
  color: #5a6b82; flex-wrap: wrap;
}
.answer-line b { color: #1f2d3d; }
.answer-line b.right { color: #22a06b; }
.state { font-size: 12px; padding: 1px 8px; border-radius: 5px; }
.state.ok { color: #22a06b; background: #e7f8f0; }
.state.bad { color: #dd5252; background: #fdecec; }
.no-option { font-size: 13px; color: #a3aec1; padding: 6px 0; }

.card-action { display: flex; gap: 10px; justify-content: flex-end; }
.empty { text-align: center; padding: 60px 0; color: #a3aec1; }
</style>
