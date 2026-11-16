<template>
  <div class="tpo-page">
    <div class="header-box">
      <h1>托福 TPO 刷题平台</h1>
      <div class="user-action">
        <!-- 真题套题：独立数据的新板块，四模块一次性提交 + 口语写作 AI 评分 -->
        <el-button type="primary" @click="$router.push('/full')">真题套题</el-button>
        <el-button @click="$router.push('/wrongbook')">错题本</el-button>
        <el-button type="danger" @click="logout">退出登录</el-button>
      </div>
    </div>

    <!-- 四个做题入口平行切换：TOEFL 分块 / TOEFL 套题 / GRE 分块 / GRE 套题 -->
    <div class="mode-switch-wrap"><ModeSwitch /></div>

    <!-- 真题套题入口横幅 -->
    <div class="suite-banner" @click="$router.push('/full')">
      <div class="suite-banner__left">
        <div class="suite-banner__title">真题套题 · 全真四模块模考</div>
        <div class="suite-banner__desc">
          阅读 / 听力 / 口语 / 写作一次做完，答案先存本地，最后统一提交判分；
          口语与写作按新托福 1-6 分档给 AI 评分反馈。
        </div>
      </div>
      <div class="suite-banner__right">
        <span class="suite-banner__cta">进入套题目录 →</span>
      </div>
    </div>

    <el-alert
      v-if="loadError"
      :title="loadError"
      type="warning"
      show-icon
      :closable="false"
      style="max-width:1000px;margin:0 auto 20px;"
    />

    <!-- 主页只放四个板块入口，不列出具体篇章；点进去才看该板块的篇章/题目列表 -->
    <div class="card-container">
      <button
        v-for="card in cards"
        :key="card.type"
        type="button"
        class="module-entry"
        @click="enterModule(card)"
      >
        <div class="module-entry__head">
          <span class="module-entry__icon">{{ card.icon }}</span>
          <span class="module-entry__badge">{{ card.sections.length }} 篇</span>
        </div>
        <div class="module-entry__title">{{ card.title }}</div>
        <div class="module-entry__desc">{{ card.desc }}</div>
        <div class="module-entry__foot">
          <span class="module-entry__count">
            {{ card.totalQuestions }} 题<template v-if="card.sections.length"> · {{ card.sections.length }} 个单元</template>
          </span>
          <span class="module-entry__cta">进入板块 →</span>
        </div>
      </button>
    </div>

    <!-- ============ GRE 入口：与 TOEFL 平行的另一条题库线，数据完全独立 ============ -->
    <div class="exam-divider"><span>其他考试</span></div>

    <div class="card-container">
      <!-- GRE 分块练习：Verbal / Quant / Writing 各自单独的练习单元 -->
      <button type="button" class="module-entry gre-entry" @click="$router.push('/gre')">
        <div class="module-entry__head">
          <span class="module-entry__icon">🎓</span>
          <span class="module-entry__badge gre">GRE</span>
        </div>
        <div class="module-entry__title">GRE 分块练习</div>
        <div class="module-entry__desc">填空 / 六选一 / 阅读 / 数量比较 / 写作，按模块单独刷</div>
        <div class="module-entry__foot">
          <span class="module-entry__count">{{ greStats.questions }} 题 · {{ greStats.units }} 个单元</span>
          <span class="module-entry__cta">进入练习 →</span>
        </div>
      </button>

      <!-- GRE 真题套题：独立表 gre_suite，与 TOEFL 套题互不调用 -->
      <button type="button" class="module-entry gre-entry" @click="$router.push('/gre/suite')">
        <div class="module-entry__head">
          <span class="module-entry__icon">🧩</span>
          <span class="module-entry__badge gre">{{ greStats.suites }} 套</span>
        </div>
        <div class="module-entry__title">GRE 真题套题</div>
        <div class="module-entry__desc">Verbal + Quant 一次成卷，作文单独按档给分</div>
        <div class="module-entry__foot">
          <span class="module-entry__count">全真计时 · 提交后统一判分</span>
          <span class="module-entry__cta">进入套题 →</span>
        </div>
      </button>
    </div>

    <div class="gre-links">
      <button type="button" class="gre-link" @click="$router.push('/gre/wrong')">GRE 错题本 →</button>
    </div>
  </div>
</template>

<script setup>
/**
 * TPO 首页（TOEFL 分块练习入口）
 *
 * 只在该页展示「四个板块」的入口卡片：
 *   阅读 Reading / 听力 Listening / 口语 Speaking / 写作 Writing
 * 具体有哪些篇章、每篇几道题，放在 /tpo/module/:type 这一层展示，
 * 这样主页不会被一串截断的英文正文占满。
 */
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import service from '@/utils/request.js'
import ModeSwitch from '@/components/ModeSwitch.vue'

const router = useRouter()

const cards = ref([
  { type: 'reading', icon: '📖', title: '阅读 Reading', desc: 'TPO 阅读文章 + 选择题练习', sections: [], totalQuestions: 0 },
  { type: 'listening', icon: '🎧', title: '听力 Listening', desc: '音频播放，完成听力题目', sections: [], totalQuestions: 0 },
  { type: 'speaking', icon: '🎤', title: '口语 Speaking', desc: '录音答题，回放自查', sections: [], totalQuestions: 0 },
  { type: 'writing', icon: '✍️', title: '写作 Writing', desc: '综合写作 & 独立写作作答', sections: [], totalQuestions: 0 }
])

const loadError = ref('')

// GRE 入口的统计数字（与 TOEFL 的 /api/section/list 完全无关，走 GRE 自己的接口）
const greStats = ref({ questions: 0, units: 0, suites: 0 })

async function loadGreStats() {
  try {
    const cat = await service.get('/api/gre/practice/catalog')
    if (cat.code === 200) {
      const modules = (cat.data && cat.data.modules) || []
      greStats.value.questions = cat.data.totalQuestions || 0
      greStats.value.units = modules.reduce((s, m) => s + ((m.sections || []).length), 0)
    }
  } catch (e) {
    // GRE 接口异常不影响 TOEFL 首页展示
  }
  try {
    const suites = await service.get('/api/greSuite/list')
    if (suites.code === 200) greStats.value.suites = (suites.data || []).length
  } catch (e) {
    // 同上
  }
}

const loadSections = async () => {
  try {
    const res = await service.get('/api/section/list')
    if (res.code !== 200) {
      loadError.value = res.msg || '篇章列表加载失败'
      return
    }
    const sections = res.data || []
    cards.value.forEach(card => {
      const list = sections
        .filter(s => s.type === card.type)
        .filter(s => (s.questionCount || 0) > 0)
        .map(s => ({ id: s.id, title: s.title, questionCount: s.questionCount || 0, hasTranslation: !!s.hasTranslation }))
      card.sections = list
      card.totalQuestions = list.reduce((sum, s) => sum + s.questionCount, 0)
    })
  } catch (e) {
    loadError.value = '篇章列表加载失败，请确认后端 8080 已启动'
  }
}

/** 进板块列表页：阅读 / 听力 / 口语 / 写作各一页，共用 TpoModuleList 组件 */
function enterModule(card) {
  router.push({ path: '/tpo/module/' + card.type })
}

function logout() {
  localStorage.removeItem('userInfo')
  router.push('/login')
}

onMounted(() => {
  loadSections()
  loadGreStats()
})
</script>

<style scoped>
.tpo-page {
  max-width: 1120px;
  margin: 0 auto;
  padding: 26px 24px 60px;
}

.header-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 18px;
}

.header-box h1 {
  margin: 0;
  font-size: 26px;
  font-weight: 800;
  letter-spacing: -0.4px;
}

.user-action { display: flex; gap: 10px; }

.mode-switch-wrap { margin-bottom: 18px; }

.suite-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 20px 24px;
  margin-bottom: 22px;
  border-radius: 16px;
  background: linear-gradient(120deg, #1b2f6b 0%, #2a4a9c 60%, #3b63c9 100%);
  color: #fff;
  cursor: pointer;
  transition: transform 0.16s ease, box-shadow 0.16s ease;
}

.suite-banner:hover { transform: translateY(-2px); box-shadow: 0 14px 30px rgba(27, 47, 107, 0.28); }

.suite-banner__title { font-size: 18px; font-weight: 700; margin-bottom: 6px; }
.suite-banner__desc { font-size: 13px; line-height: 1.7; color: rgba(255, 255, 255, 0.78); max-width: 720px; }
.suite-banner__cta { font-size: 14px; font-weight: 700; white-space: nowrap; }

.card-container {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 20px;
}

.module-entry {
  appearance: none;
  text-align: left;
  border: 1px solid #e6ebf5;
  background: #fff;
  border-radius: 18px;
  padding: 24px 26px;
  cursor: pointer;
  box-shadow: 0 6px 18px rgba(23, 43, 99, 0.06);
  transition: transform 0.16s ease, box-shadow 0.16s ease, border-color 0.16s ease;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.module-entry:hover {
  transform: translateY(-3px);
  border-color: #cfd9ee;
  box-shadow: 0 16px 34px rgba(23, 43, 99, 0.12);
}

.module-entry__head { display: flex; align-items: center; justify-content: space-between; }

.module-entry__icon { font-size: 30px; line-height: 1; }

.module-entry__badge {
  font-size: 12px;
  font-weight: 700;
  color: #8a6a1f;
  background: #fdf4dd;
  padding: 4px 10px;
  border-radius: 999px;
}

.module-entry__title { font-size: 22px; font-weight: 800; color: #16233f; letter-spacing: -0.3px; }

.module-entry__desc { font-size: 13px; color: #7d8ca1; }

.module-entry__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #e8edf6;
}

.module-entry__count { font-size: 13px; color: #46536b; font-variant-numeric: tabular-nums; }
.module-entry__cta { font-size: 13px; font-weight: 700; color: #2a4a9c; }

/* ===== 其他考试分隔与 GRE 入口 ===== */
.exam-divider {
  display: flex;
  align-items: center;
  gap: 14px;
  margin: 34px 0 18px;
  color: #6b7a91;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 1px;
}
.exam-divider::after {
  content: "";
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, #e3e9f4, transparent);
}

/* GRE 卡片用深墨绿做主色，和 TOEFL 的深蓝区分开，一眼能分辨两条题库线 */
.gre-entry { border-color: #dfe9e2; }
.gre-entry:hover { border-color: #bcd2c3; box-shadow: 0 16px 34px rgba(23, 74, 51, 0.12); }
.module-entry__badge.gre { color: #1b5c3f; background: #e5f4ea; }
.gre-entry .module-entry__cta { color: #1b5c3f; }
.gre-entry .module-entry__foot { border-top-color: #e8f1eb; }

.gre-links { margin-top: 14px; display: flex; gap: 10px; }
.gre-link {
  appearance: none;
  border: 1px solid #dfe9e2;
  background: #f7fbf8;
  color: #1b5c3f;
  font-size: 13px;
  font-weight: 600;
  padding: 9px 16px;
  border-radius: 10px;
  cursor: pointer;
}
.gre-link:hover { background: #eef7f1; }
</style>
