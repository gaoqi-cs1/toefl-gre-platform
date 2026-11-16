<template>
  <div class="mode-switch" role="tablist" aria-label="做题入口切换">
    <span class="mode-switch__label">做题入口</span>
    <button
      v-for="item in tabs"
      :key="item.path"
      type="button"
      role="tab"
      :aria-selected="isActive(item.path)"
      :class="['mode-switch__tab', { 'is-active': isActive(item.path) }]"
      @click="go(item.path)"
    >
      <span class="mode-switch__exam">{{ item.exam }}</span>
      <span class="mode-switch__kind">{{ item.kind }}</span>
    </button>
  </div>
</template>

<script setup>
/**
 * ModeSwitch —— 四个做题入口的平行切换条
 *
 * TOEFL 分块练习 (/tpo) · TOEFL 全真套题 (/full)
 * GRE   分块练习 (/gre) · GRE   全真套题 (/gre/suite)
 *
 * 四者是一级并列关系：托福和 GRE 各自都有"分块"与"套题"，
 * 数据也各自独立（tpo_ / full_ 前端路由与 gre_ 前端路由各自独立，后端表也各自独立）。
 */
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const tabs = [
  { exam: 'TOEFL', kind: '分块练习', path: '/tpo' },
  { exam: 'TOEFL', kind: '全真套题', path: '/full' },
  { exam: 'GRE', kind: '分块练习', path: '/gre' },
  { exam: 'GRE', kind: '全真套题', path: '/gre/suite' }
]

/** 子路由也算命中：/gre/practice/1 算 GRE 分块，/full/exam 算 TOEFL 套题 */
function isActive(path) {
  if (route.path === path) return true
  if (path === '/gre' && route.path.startsWith('/gre/') && !route.path.startsWith('/gre/suite')) {
    return !route.path.startsWith('/gre/wrong') && !route.path.startsWith('/gre/exam') && !route.path.startsWith('/gre/result')
  }
  if (path === '/gre/suite' && (route.path === '/gre/suite' || route.path === '/gre/exam' || route.path === '/gre/result')) {
    return true
  }
  if (path === '/full' && route.path.startsWith('/full')) return true
  if (path === '/tpo' && route.path.startsWith('/tpo')) return true
  return false
}

function go(path) {
  if (route.path !== path) router.push(path)
}
</script>

<style scoped>
.mode-switch {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  padding: 8px 10px;
  margin-bottom: 20px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(8px);
  width: fit-content;
  max-width: 100%;
}

.mode-switch__label {
  font-size: 12px;
  font-weight: 700;
  color: var(--ink-300);
  letter-spacing: 0.6px;
  padding: 0 8px 0 6px;
}

.mode-switch__tab {
  appearance: none;
  border: 1px solid var(--line);
  background: #fff;
  border-radius: 999px;
  padding: 7px 14px;
  cursor: pointer;
  display: inline-flex;
  align-items: baseline;
  gap: 6px;
  transition: border-color 0.15s ease, background 0.15s ease, color 0.15s ease;
}

.mode-switch__tab:hover { border-color: var(--brand-100); background: var(--brand-50); }

.mode-switch__tab.is-active {
  background: var(--brand-900);
  border-color: var(--brand-900);
  color: #fff;
}

.mode-switch__exam {
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.4px;
  color: var(--brand-600);
}

.mode-switch__tab.is-active .mode-switch__exam { color: #f0b64a; }

.mode-switch__kind { font-size: 13px; font-weight: 600; }
</style>
