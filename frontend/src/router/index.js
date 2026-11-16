import { createRouter, createWebHistory } from 'vue-router'
import Login from '@/views/Login.vue'
import Home from '@/views/Home.vue'
import UserList from '@/views/UserList.vue'
// TPO页面
import TpoList from '@/views/tpo/TpoList.vue'
import ReadingExam from '@/views/tpo/ReadingExam.vue'
import ListeningExam from '@/views/tpo/ListeningExam.vue'
import SpeakingExam from '@/views/tpo/SpeakingExam.vue'
import WritingExam from '@/views/tpo/WritingExam.vue'
// 错题本
import WrongBook from '@/views/user/WrongBook.vue'

const routes = [
  {
    path: '/',
    redirect: '/tpo'  // 根路径直接跳TPO首页（四大板块）
  },
  {
    path: '/login',
    name: 'Login',
    component: Login
  },
  {
    path: '/home',
    name: 'Home',
    component: Home,
    meta: { requireAuth: true }
  },
  {
    path: '/user',
    name: 'UserList',
    component: UserList,
    meta: { requireAuth: true }
  },
  // ========== TPO托福练习路由 ==========
  {
    path: '/tpo',
    name: 'TpoList',
    component: TpoList,
    meta: { requireAuth: true }
  },
  {
  // 板块内页：某题型下的篇章/单元列表（主页只放四个板块入口）
  path: '/tpo/module/:type',
  name: 'TpoModuleList',
  component: () => import('@/views/tpo/TpoModuleList.vue'),
  meta: { requireAuth: true }
},
{
  path: '/tpo/ReadingExam/:sectionId',
  name: 'ReadingExam',
  component: () => import('@/views/tpo/ReadingExam.vue'),
  meta: { requireAuth: true }
},
{
  path: '/tpo/listening/:sectionId',
  name: 'ListeningExam',
  component: () => import('@/views/tpo/ListeningExam.vue'),
  meta: { requireAuth: true }
},
{
  path: '/tpo/speaking/:sectionId',
  name: 'SpeakingExam',
  component: () => import('@/views/tpo/SpeakingExam.vue'),
  meta: { requireAuth: true }
},
{
  path: '/tpo/writing/:sectionId',
  name: 'WritingExam',
  component: () => import('@/views/tpo/WritingExam.vue'),
  meta: { requireAuth: true }
},

 
  // 错题本
  {
  path: '/wrongbook',
  name: 'WrongBook',
  component: () => import('@/views/tpo/WrongBook.vue')
},

// ========== 说明：旧的套题模块（/tpo/suite/*、/suite/*）已整体下线 ==========
// 原来这里挂着 5 条旧套题路由（/tpo/suite/list、/tpo/suite/exam、/tpo/suite/records、
// /tpo/suite/result、/suite/result），它们对应 views/tpo/SuiteList.vue 等旧页面，
// 和「真题套题 /full」形成两套并行的套题列表，容易让人分不清。
// 现在全站只保留 /full 这一套（数据在 full_* 表，支持一键随机生成套题）。
// 旧页面文件（SuiteList.vue / SuiteExam.vue / ExamRecord.vue / result.vue / SuiteResult.vue）
// 全部原样保留在磁盘上，只是不再挂路由，需要回看历史代码时直接读文件即可。

// ========== 真题套题（独立数据，新模块） ==========
// /full        套题目录（列套题 + 我的套题记录）
// /full/exam   单页答题（四模块顺序作答，答案本地缓存，最后一次性提交）
// /full/result 成绩页（四项 1-6 档 + AI 评分反馈 + 逐题回顾）
{
  path: '/full',
  name: 'FullList',
  component: () => import('@/views/full/FullList.vue'),
  meta: { requireAuth: true }
},
{
  path: '/full/exam',
  name: 'FullExam',
  component: () => import('@/views/full/FullExam.vue'),
  meta: { requireAuth: true }
},
{
  path: '/full/result',
  name: 'FullResult',
  component: () => import('@/views/full/FullResult.vue'),
  meta: { requireAuth: true }
},

// 说明：/tpo/fullExam、/tpo/fullRecord、/tpo/fullResult 三条路由原来指向
// views/tpo/fullExam.vue / fullRecord.vue / fullResult.vue，但这三个文件只存在 .bak 版本，
// 动态导入必然失败、npm run build 直接报错。它们对应的后端接口 /api/fullSuiteExam/*
// 在后端也不存在，功能已被 /tpo/suite/* + /api/examRecord/* 取代，故删除这三条路由。
// 原始页面文件 fullExam.vue.bak / fullRecord.vue.bak / fullResult.vue.bak 均保留未删除。

// ========== GRE 模块（与 TOEFL 完全独立的一套数据） ==========
// /gre                GRE 入口：分块练习目录 + 全真套题目录 + 我的模考记录
// /gre/practice/:id   分块练习页（单节做题，逐题提交判分）
// /gre/exam           套题作答页（Writing → Verbal → Quant 整卷提交）
// /gre/result         套题成绩页（总分 + 逐题回顾 + AWA 范文）
// /gre/wrong           GRE 错题本（独立表 gre_wrong，不并入托福 /wrongbook）
// 说明：GRE 的分块练习（gre_section/gre_question/gre_answer）与套题
// （gre_suite/gre_suite_section/gre_suite_question/gre_exam_*）各自独立建表，
// 页面与接口都不复用上面 TOEFL 的 /tpo、/full 路由。
{
  path: '/gre',
  name: 'GreHome',
  component: () => import('@/views/gre/GreHome.vue'),
  meta: { requireAuth: true }
},
{
  path: '/gre/practice/:sectionId',
  name: 'GrePractice',
  component: () => import('@/views/gre/GrePractice.vue'),
  meta: { requireAuth: true }
},
{
  path: '/gre/exam',
  name: 'GreExam',
  component: () => import('@/views/gre/GreExam.vue'),
  meta: { requireAuth: true }
},
{
  path: '/gre/result',
  name: 'GreResult',
  component: () => import('@/views/gre/GreResult.vue'),
  meta: { requireAuth: true }
},
{
  // GRE 全真套题：与 TOEFL /full 平行的一级入口（套题数据独立于分块练习）
  path: '/gre/suite',
  name: 'GreSuiteHome',
  component: () => import('@/views/gre/GreSuiteHome.vue'),
  meta: { requireAuth: true }
},
{
  // GRE 错题本：独立表 gre_wrong，和托福 /wrongbook 完全分开
  path: '/gre/wrong',
  name: 'GreWrong',
  component: () => import('@/views/gre/GreWrong.vue'),
  meta: { requireAuth: true }
},

]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫 沿用你原来的逻辑，不用改
router.beforeEach((to, from, next) => {
  const userStr = localStorage.getItem('userInfo')
  const token = userStr ? JSON.parse(userStr).token : null
  if (to.meta.requireAuth && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
