<template>
  <div class="shell">
    <!-- 顶部导航 -->
    <header class="shell-head">
      <div class="shell-head__inner">
        <!-- 品牌区 -->
        <div class="brand" @click="go('/tpo')">
          <span class="brand__mark">T</span>
          <span class="brand__text">
            <strong>TOEFL &amp; GRE 备考中心</strong>
            <small>2026 新托福全真模考 · GRE 分块与套题</small>
          </span>
        </div>

        <!-- 导航 -->
        <nav class="nav">
          <button
            v-for="item in navItems"
            :key="item.path"
            :class="['nav__item', { 'is-active': isActive(item.path) }]"
            type="button"
            @click="go(item.path)"
          >
            {{ item.label }}
          </button>
        </nav>

        <!-- 用户区 -->
        <div class="user">
          <span class="user__name">{{ username || '未登录' }}</span>
          <button v-if="username" class="user__logout" type="button" @click="logout">退出</button>
          <button v-else class="user__logout" type="button" @click="go('/login')">登录</button>
        </div>
      </div>
    </header>

    <!-- 页面主体 -->
    <main class="shell-body">
      <slot />
    </main>

    <footer class="shell-foot">
      <span>TOEFL &amp; GRE 备考中心 · 套题数据与单模块练习各自独立</span>
      <span class="shell-foot__dot">·</span>
      <span>托福按 1-6 分档、GRE 按 130-170 与 0-6 换算</span>
    </footer>
  </div>
</template>

<script setup>
/**
 * AppShell —— 全站统一外壳（顶部导航 + 页面主体 + 页脚）
 * 新页面（真题套题）统一套这个外壳；考试进行中的页面也用它，
 * 但内容区自带专注模式（页面内自行决定留白与宽度）。
 */
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const router = useRouter()
const route = useRoute()

const username = ref('')

const navItems = [
  { label: 'TOEFL 分块', path: '/tpo' },
  { label: 'TOEFL 套题', path: '/full' },
  { label: 'GRE 分块', path: '/gre' },
  { label: 'GRE 套题', path: '/gre/suite' },
  { label: '托福错题本', path: '/wrongbook' },
  { label: 'GRE 错题本', path: '/gre/wrong' }
]

/* 当前路径命中哪个导航（子页面也算父入口：/full/* 算真题套题，/gre/* 算 GRE 备考） */
function isActive(path) {
  if (route.path === path) return true
  if (path === '/tpo' && route.path.startsWith('/tpo')) return true
  if (path === '/wrongbook' && route.path.startsWith('/wrongbook')) return true
  if (path === '/full' && route.path.startsWith('/full')) return true
  // GRE 分块：/gre 与 /gre/practice/*
  if (path === '/gre'
    && route.path.startsWith('/gre')
    && !route.path.startsWith('/gre/wrong')
    && !route.path.startsWith('/gre/suite')
    && !route.path.startsWith('/gre/exam')
    && !route.path.startsWith('/gre/result')) return true
  // GRE 套题：/gre/suite 与作答中、成绩页
  if (path === '/gre/suite'
    && (route.path.startsWith('/gre/suite') || route.path.startsWith('/gre/exam') || route.path.startsWith('/gre/result'))) return true
  if (path === '/gre/wrong' && route.path.startsWith('/gre/wrong')) return true
  return false
}

function go(path) {
  router.push(path)
}

function logout() {
  localStorage.removeItem('userInfo')
  username.value = ''
  router.push('/login')
}

onMounted(() => {
  const str = localStorage.getItem('userInfo')
  if (str) {
    try {
      const info = JSON.parse(str)
      username.value = info.username || ''
    } catch (e) {
      localStorage.removeItem('userInfo')
    }
  }
})
</script>

<style scoped>
.shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--surface-2);
}

.shell-head {
  position: sticky;
  top: 0;
  z-index: 20;
  background: rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(14px);
  border-bottom: 1px solid var(--line);
}

.shell-head__inner {
  max-width: 1240px;
  margin: 0 auto;
  padding: 0 24px;
  height: 64px;
  display: flex;
  align-items: center;
  gap: 24px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  user-select: none;
  flex: none;
}

.brand__mark {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  font-weight: 800;
  font-size: 16px;
  color: #fff;
  background: linear-gradient(135deg, var(--brand-700), var(--brand-500));
  box-shadow: var(--shadow-sm);
}

.brand__text {
  display: flex;
  flex-direction: column;
  line-height: 1.15;
}

.brand__text strong {
  font-size: 15px;
  letter-spacing: -0.2px;
}

.brand__text small {
  font-size: 11px;
  color: var(--ink-500);
  margin-top: 2px;
}

.nav {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-left: 8px;
  flex: 1;
  min-width: 0;
}

.nav__item {
  appearance: none;
  border: 0;
  background: transparent;
  padding: 8px 14px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 600;
  color: var(--ink-700);
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease;
  white-space: nowrap;
}

.nav__item:hover {
  background: var(--brand-50);
  color: var(--brand-600);
}

.nav__item.is-active {
  background: var(--brand-900);
  color: #fff;
}

.user {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: none;
}

.user__name {
  font-size: 13px;
  color: var(--ink-700);
  font-weight: 600;
}

.user__logout {
  appearance: none;
  border: 1px solid var(--line);
  background: #fff;
  border-radius: 999px;
  padding: 6px 12px;
  font-size: 12px;
  color: var(--ink-500);
  cursor: pointer;
}

.user__logout:hover {
  color: var(--brand-600);
  border-color: var(--brand-100);
}

.shell-body {
  flex: 1;
  min-height: 0;
}

.shell-foot {
  border-top: 1px solid var(--line);
  padding: 18px 24px 26px;
  text-align: center;
  font-size: 12px;
  color: var(--ink-300);
  background: #fff;
}

.shell-foot__dot {
  margin: 0 8px;
}

@media (max-width: 860px) {
  .shell-head__inner {
    height: auto;
    padding: 12px 16px;
    flex-wrap: wrap;
    gap: 12px;
  }
  .nav {
    order: 3;
    width: 100%;
    margin-left: 0;
    overflow-x: auto;
  }
}
</style>
