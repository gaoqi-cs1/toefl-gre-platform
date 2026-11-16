<template>
  <div class="tpo-page">
    <div class="header-box">
      <div class="header-left">
        <button class="back-btn" type="button" @click="$router.push('/tpo')">← 返回板块</button>
        <div>
          <h1>{{ meta.title }}</h1>
          <p class="header-sub">{{ meta.desc }}</p>
        </div>
      </div>
      <div class="header-right">
        <span class="count-badge">{{ sections.length }} 个单元 · {{ totalQuestions }} 题</span>
      </div>
    </div>

    <div class="mode-switch-wrap"><ModeSwitch /></div>

    <el-alert
      v-if="loadError"
      :title="loadError"
      type="warning"
      show-icon
      :closable="false"
      style="margin-bottom:18px;"
    />

    <div v-loading="loading" class="sec-list">
      <button
        v-for="(item, idx) in sections"
        :key="item.id"
        type="button"
        class="sec-row"
        @click="enter(item)"
      >
        <span class="sec-row__idx">{{ String(idx + 1).padStart(2, '0') }}</span>
        <span class="sec-row__main">
          <strong>{{ item.title }}</strong>
          <small>
            {{ item.questionCount }} 题
            <template v-if="item.hasTranslation"> · 已有中文译文</template>
            <template v-else> · 暂无译文</template>
          </small>
        </span>
        <span class="sec-row__enter">进入练习 →</span>
      </button>

      <div v-if="!loading && sections.length === 0" class="empty-box">
        该板块还没有可用单元。请在数据库录入篇章与题目，或用
        <code>tools\tpo-import</code> 的导入模板批量导入。
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * 板块内页：某一种题型下的篇章/单元列表（/tpo/module/:type）
 *
 * 主页只放四个板块入口，具体"有哪些文章、每篇几题"都收在这一层，
 * 这样主页不会被一串截断的英文正文占满，符合"主页不显示题目"的要求。
 */
import { computed, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import service from '@/utils/request.js'
import ModeSwitch from '@/components/ModeSwitch.vue'

const route = useRoute()
const router = useRouter()

const META = {
  reading: { title: '阅读 Reading', desc: '每篇 Passage 一单元，做完提交后才显示答案与解析', name: 'ReadingExam', base: '/tpo/ReadingExam/' },
  listening: { title: '听力 Listening', desc: '每段音频一单元，提交后显示原文，可再切换中文译文', name: 'ListeningExam', base: '/tpo/listening/' },
  speaking: { title: '口语 Speaking', desc: '录音作答，回放自查', name: 'SpeakingExam', base: '/tpo/speaking/' },
  writing: { title: '写作 Writing', desc: '综合写作与独立写作，AI 按档给分', name: 'WritingExam', base: '/tpo/writing/' }
}

const type = computed(() => route.params.type || 'reading')
const meta = computed(() => META[type.value] || META.reading)

const sections = ref([])
const loading = ref(false)
const loadError = ref('')

const totalQuestions = computed(() => sections.value.reduce((s, it) => s + (it.questionCount || 0), 0))

async function loadSections() {
  loading.value = true
  loadError.value = ''
  try {
    const res = await service.get('/api/section/list', { params: { type: type.value } })
    if (res.code !== 200) {
      loadError.value = res.msg || '加载失败'
      sections.value = []
      return
    }
    sections.value = (res.data || []).filter(s => (s.questionCount || 0) > 0)
  } catch (e) {
    loadError.value = '加载失败，请确认后端 8080 已启动'
  } finally {
    loading.value = false
  }
}

function enter(item) {
  router.push({ path: meta.value.base + item.id })
}

onMounted(loadSections)
</script>

<style scoped>
.tpo-page { max-width: 1120px; margin: 0 auto; padding: 26px 24px 60px; }

.header-box {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 18px;
}

.header-left { display: flex; align-items: flex-start; gap: 16px; }

.back-btn {
  appearance: none;
  border: 1px solid #dde5f3;
  background: #fff;
  color: #2a4a9c;
  font-size: 13px;
  font-weight: 600;
  padding: 9px 14px;
  border-radius: 10px;
  cursor: pointer;
  white-space: nowrap;
}
.back-btn:hover { background: #f4f7ff; }

h1 { margin: 0 0 6px; font-size: 24px; font-weight: 800; letter-spacing: -0.4px; }
.header-sub { margin: 0; font-size: 13px; color: #7d8ca1; }

.count-badge {
  font-size: 13px;
  color: #46536b;
  background: #f4f7ff;
  border: 1px solid #e2e9f7;
  padding: 6px 12px;
  border-radius: 999px;
  font-variant-numeric: tabular-nums;
}

.mode-switch-wrap { margin-bottom: 18px; }

.sec-list { display: flex; flex-direction: column; gap: 10px; }

.sec-row {
  width: 100%;
  appearance: none;
  text-align: left;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px 20px;
  border: 1px solid #e6ebf5;
  background: #fff;
  border-radius: 14px;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(23, 43, 99, 0.05);
  transition: transform 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease;
}

.sec-row:hover {
  transform: translateY(-2px);
  border-color: #cfd9ee;
  box-shadow: 0 12px 26px rgba(23, 43, 99, 0.1);
}

.sec-row__idx {
  font-size: 13px;
  font-weight: 800;
  color: #9db0cf;
  font-variant-numeric: tabular-nums;
  min-width: 24px;
}

.sec-row__main { flex: 1; display: flex; flex-direction: column; gap: 4px; min-width: 0; }
.sec-row__main strong { font-size: 16px; color: #16233f; letter-spacing: -0.2px; }
.sec-row__main small { font-size: 12px; color: #7d8ca1; }

.sec-row__enter { font-size: 13px; font-weight: 700; color: #2a4a9c; white-space: nowrap; }

.empty-box {
  padding: 28px 20px;
  border: 1px dashed #dbe4f2;
  border-radius: 14px;
  background: #fafbff;
  font-size: 13px;
  color: #7d8ca1;
  text-align: center;
}
</style>
