import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
// 全站主题：必须在 element-plus 的样式之后引入，才能覆盖它的 CSS 变量
import './styles/theme.css'

const app = createApp(App)
const pinia = createPinia()
app.use(pinia)
app.use(router)
app.use(ElementPlus)

// 刷新自动加载用户
import { useUserStore } from '@/stores/user'
const userStore = useUserStore()
userStore.loadUser()

app.mount('#app')
