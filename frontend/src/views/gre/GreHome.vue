<template>
  <AppShell>
    <div class="page">
      <!-- GRE 分块练习主视觉（与 TOEFL /tpo 平行的入口之一） -->
      <section class="gre-hero">
        <div class="gre-hero__glow" aria-hidden="true"></div>
        <div class="gre-hero__left">
          <span class="gre-hero__eyebrow">GRADUATE RECORD EXAMINATIONS</span>
          <h1 class="gre-hero__title">GRE 分块练习</h1>
          <p class="gre-hero__sub">
            按题型逐个击破：填空、六选一、阅读、数量比较、问题求解、写作。
            每题提交后才显示对错、正确答案与中英解析；写作提交后给 AWA 维度分与满分范例。
            做错的题自动进 GRE 错题本，答对一次就移出。
          </p>
          <div class="gre-hero__actions">
            <el-button type="primary" size="large" round @click="scrollToPractice">开始练习</el-button>
            <el-button size="large" round @click="goSuite">GRE 全真套题</el-button>
            <button type="button" class="gre-hero__wrong" @click="goWrongBook">
              <span>GRE 错题本</span>
              <em>{{ wrongTotal }}</em>
            </button>
          </div>
        </div>
        <div class="gre-hero__stats">
          <div class="hero-stat"><strong>{{ catalog.totalQuestions || 0 }}</strong><span>练习题</span></div>
          <div class="hero-stat"><strong>{{ catalog.sectionCount || 0 }}</strong><span>练习单元</span></div>
          <div class="hero-stat"><strong>{{ suiteCount }}</strong><span>可用套题</span></div>
          <div class="hero-stat"><strong>{{ wrongTotal }}</strong><span>待复习错题</span></div>
        </div>
      </section>

      <!-- 四个做题入口平行切换：TOEFL 分块 / TOEFL 套题 / GRE 分块 / GRE 套题 -->
      <ModeSwitch />

      <!-- 练习单元 -->
      <section id="practice-block" class="block">
        <div class="block__head">
          <h2 class="section-title">练习单元</h2>
          <span class="muted">共 {{ catalog.totalQuestions || 0 }} 题 · {{ catalog.sectionCount || 0 }} 个单元</span>
        </div>

        <div v-loading="loadingCatalog" class="module-grid">
          <article v-for="mod in catalog.modules" :key="mod.moduleType" class="module-card card">
            <header class="module-card__head">
              <div>
                <h3>{{ mod.label }}</h3>
                <p class="muted">{{ moduleHint(mod.moduleType) }}</p>
              </div>
              <span class="module-card__count">{{ mod.questionCount }} 题</span>
            </header>

            <ul class="section-list">
              <li v-for="section in mod.sections" :key="section.sectionId">
                <button type="button" class="section-item" @click="openPractice(section)">
                  <span class="section-item__main">
                    <strong>{{ section.title }}</strong>
                    <small>{{ section.subtitle }}</small>
                  </span>
                  <span class="section-item__meta">
                    <span class="tag">{{ section.difficulty }}</span>
                    <span class="tag tag--ghost">{{ section.questionCount }} 题</span>
                    <span v-if="section.hasPassage" class="tag tag--accent">含文章</span>
                  </span>
                </button>
              </li>
            </ul>
          </article>

          <div v-if="!loadingCatalog && (!catalog.modules || catalog.modules.length === 0)" class="empty card card-pad">
            还没有分块题库，请先执行项目根目录的 db-gre-module.sql。
          </div>
        </div>
      </section>
    </div>
  </AppShell>
</template>

<script setup>
/**
 * GRE 分块练习页（与 TOEFL /tpo 平行的一级入口）
 *
 * 接口（只读 GRE 的练习题数据，不碰套题表，也不调任何 TOEFL 接口）：
 *   GET /api/gre/practice/catalog    三个模块 + 每个模块的练习单元
 *   GET /api/greWrong/list           只取 stats，用来显示待复习错题角标
 *   GET /api/greSuite/list           只用于显示"可用套题"数量
 */
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import AppShell from '@/components/AppShell.vue'
import ModeSwitch from '@/components/ModeSwitch.vue'

const router = useRouter()

const catalog = ref({ modules: [], totalQuestions: 0, sectionCount: 0 })
const loadingCatalog = ref(false)
const wrongTotal = ref(0)
const suiteCount = ref(0)
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

async function loadCatalog() {
  loadingCatalog.value = true
  try {
    const res = await request.get('/api/gre/practice/catalog')
    if (res.code === 200) catalog.value = res.data || { modules: [] }
  } catch (e) {
    // 拦截器已提示
  } finally {
    loadingCatalog.value = false
  }
}

/** 待复习错题数（三模块相加） */
async function loadWrongTotal() {
  try {
    const res = await request.get('/api/greWrong/list', { params: { userId: userId.value } })
    if (res.code === 200 && res.data && res.data.stats) {
      const s = res.data.stats
      wrongTotal.value = (s.verbal || 0) + (s.quant || 0) + (s.writing || 0)
    }
  } catch (e) {
    // 统计失败不影响练习页
  }
}

/** 可用套题数（仅用于 hero 数据块，与练习数据无关） */
async function loadSuiteCount() {
  try {
    const res = await request.get('/api/greSuite/list')
    if (res.code === 200) suiteCount.value = (res.data || []).length
  } catch (e) {
    // 同上
  }
}

function scrollToPractice() {
  const el = document.getElementById('practice-block')
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function goSuite() {
  router.push('/gre/suite')
}

function goWrongBook() {
  router.push('/gre/wrong')
}

function openPractice(section) {
  router.push({ path: '/gre/practice/' + section.sectionId })
}

function moduleHint(moduleType) {
  if (moduleType === 'writing') return '30 分钟一篇，AI 按 0-6 档评分'
  if (moduleType === 'verbal') return '填空 / 六选一 / 阅读，正确率换算 130-170'
  if (moduleType === 'quant') return '数量比较 / 问题求解，正确率换算 130-170'
  return ''
}

onMounted(() => {
  loadUser()
  loadCatalog()
  loadWrongTotal()
  loadSuiteCount()
})
</script>

<style scoped>
.block { margin-bottom: 30px; }

.block__head {
  display: flex;
  align-items: baseline;
  gap: 14px;
  margin-bottom: 14px;
}

.block__head .section-title { margin-bottom: 0; }

.module-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(330px, 1fr));
  gap: 18px;
}

.module-card { padding: 20px 22px; }

.module-card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 14px;
}

.module-card__head h3 { margin: 0 0 4px; font-size: 18px; letter-spacing: -0.2px; }
.module-card__head p { margin: 0; font-size: 12px; }

.module-card__count {
  font-size: 12px;
  font-weight: 700;
  color: var(--brand-600);
  background: var(--brand-50);
  padding: 4px 10px;
  border-radius: 999px;
  white-space: nowrap;
}

.section-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 8px; }

.section-item {
  width: 100%;
  appearance: none;
  text-align: left;
  border: 1px solid var(--line);
  background: var(--surface-2);
  border-radius: var(--radius-md);
  padding: 12px 14px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 8px;
  transition: border-color 0.15s ease, background 0.15s ease, transform 0.15s ease;
}

.section-item:hover {
  background: #fff;
  border-color: var(--brand-100);
  transform: translateY(-1px);
}

.section-item__main { display: flex; flex-direction: column; gap: 2px; }
.section-item__main strong { font-size: 14px; }
.section-item__main small { font-size: 12px; color: var(--ink-500); }

.section-item__meta { display: flex; gap: 6px; flex-wrap: wrap; }

.tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 999px;
  background: #fff;
  border: 1px solid var(--line);
  color: var(--ink-500);
}

.tag--ghost { color: var(--ink-500); }
.tag--accent { background: var(--accent-100); border-color: transparent; color: var(--accent-600); font-weight: 700; }

/* ===== GRE 主视觉板块 ===== */
.gre-hero {
  position: relative;
  overflow: hidden;
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(0, 1fr);
  gap: 28px;
  align-items: center;
  padding: 34px 36px;
  margin-bottom: 22px;
  border-radius: var(--radius-xl);
  background: linear-gradient(135deg, #10214a 0%, #1b3566 46%, #27468c 100%);
  box-shadow: 0 22px 48px rgba(16, 33, 74, 0.28);
  color: #fff;
}

.gre-hero__glow {
  position: absolute;
  width: 460px;
  height: 460px;
  right: -120px;
  top: -180px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.22), rgba(255, 255, 255, 0) 66%);
  pointer-events: none;
}

.gre-hero__left { position: relative; z-index: 1; }

.gre-hero__eyebrow {
  display: inline-block;
  font-size: 11px;
  letter-spacing: 2.4px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.62);
  margin-bottom: 10px;
}

.gre-hero__title {
  margin: 0 0 12px;
  font-size: 34px;
  line-height: 1.15;
  font-weight: 800;
  letter-spacing: -0.6px;
}

.gre-hero__sub {
  margin: 0 0 20px;
  max-width: 620px;
  font-size: 14px;
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.78);
}

.gre-hero__actions { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }

.gre-hero__wrong {
  appearance: none;
  border: 1px solid rgba(255, 255, 255, 0.28);
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
  border-radius: 999px;
  padding: 11px 18px;
  font-size: 14px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  transition: background 0.16s ease, border-color 0.16s ease;
}

.gre-hero__wrong:hover { background: rgba(255, 255, 255, 0.18); border-color: rgba(255, 255, 255, 0.5); }

.gre-hero__wrong em {
  font-style: normal;
  min-width: 22px;
  height: 22px;
  padding: 0 6px;
  border-radius: 999px;
  background: #f0b64a;
  color: #2b1a00;
  font-size: 12px;
  font-weight: 800;
  display: inline-grid;
  place-items: center;
  font-variant-numeric: tabular-nums;
}

.gre-hero__stats {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.hero-stat {
  padding: 16px 18px;
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(6px);
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.hero-stat strong { font-size: 26px; font-weight: 800; line-height: 1.1; font-variant-numeric: tabular-nums; }
.hero-stat span { font-size: 12px; color: rgba(255, 255, 255, 0.66); }

@media (max-width: 960px) {
  .gre-hero { grid-template-columns: 1fr; padding: 26px 22px; }
  .gre-hero__title { font-size: 27px; }
}
</style>
