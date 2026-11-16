import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src')
    }
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        // 不要重写路径！我们后端就是 /api 开头
      },
      // 口语录音的静态文件：后端把 /upload/audio/** 映射到本地 upload/audio/ 目录，
      // 开发环境也要一起代理过去，否则成绩页播放录音会 404
      '/upload': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
