import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    userId: null,
    username: '',
    token: ''
  }),
  actions: {
    // 登录成功调用，存入信息
    setUserInfo(userInfo) {
      this.userId = userInfo.userId
      this.username = userInfo.username
      this.token = userInfo.token
      // 持久化到localStorage，刷新页面不丢失
      localStorage.setItem('userInfo', JSON.stringify(userInfo))
    },
    // 退出登录，清空
    logout() {
      this.userId = null
      this.username = ''
      this.token = ''
      localStorage.removeItem('userInfo')
    },
    // 页面刷新时，从localStorage恢复用户信息
    loadUser() {
      const str = localStorage.getItem('userInfo')
      if (str) {
        try {
          const info = JSON.parse(str)
          this.userId = info.userId
          this.username = info.username
          this.token = info.token
        } catch (e) {
          localStorage.removeItem('userInfo')
        }
      }
    }
  }
  
})
