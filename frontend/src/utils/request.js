import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const service = axios.create({
  baseURL: '', // 空！交给vite代理转发
  // 原来 5000ms 太短：整卷模考提交要判分 + 批量写库，很容易超时被前端当成失败，
  // 这里放宽到 20 秒
  timeout: 20000
})

// 请求拦截器
service.interceptors.request.use(config => {
  const userStr = localStorage.getItem('userInfo')
  if(userStr){
    const userInfo = JSON.parse(userStr)
    if(userInfo.token){
      config.headers.Authorization = 'Bearer ' + userInfo.token
    }
  }
  return config
})

// 响应拦截器
service.interceptors.response.use(
  res => {
    return res.data
  },
  err => {
    ElMessage.error(err.response?.data?.msg || '请求失败')
    if(err.response?.status === 401){
      localStorage.removeItem('userInfo')
      router.push('/login')
    }
    return Promise.reject(err)
  }
)

export default service
