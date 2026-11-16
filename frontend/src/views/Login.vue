<template>
  <div class="login-container">
    <el-card style="width:420px">
      <h2 style="text-align:center">托福刷题平台</h2>
      <el-form :model="form">
        <el-form-item label="用户名">
          <el-input v-model="form.username"></el-input>
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="doLogin">登录</el-button>
          <el-button @click="doRegister">注册</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { login } from '@/api/login'
import service from '@/utils/request'

const router = useRouter()
const userStore = useUserStore()

// 表单双向绑定数据
const form = ref({
  username: '',
  password: ''
})

// 登录点击事件
const doLogin = async () => {
  try {
    const res = await login(form.value)
    if (res.code === 200) {
      userStore.setUserInfo({
        token: res.data.token,
        userId: res.data.userId,
        username: res.data.username
      })
      ElMessage.success('登录成功')
      router.push('/tpo') // 跳转到TPO首页
    } else {
      ElMessage.error(res.msg)
    }
  } catch (err) {
    console.error('登录异常：', err)
    ElMessage.error('登录请求失败')
  }
}

// 注册点击事件
const doRegister = async () => {
  if (!form.value.username || !form.value.password) {
    ElMessage.warning("用户名和密码不能为空")
    return
  }
  try {
    const res = await service.post('/api/user/register', form.value)
    ElMessage.success(res.msg || "注册成功")
  } catch (err) {
    console.log("注册失败：", err)
    ElMessage.error("注册失败，请检查后端")
  }
}
</script>

<style scoped>
.login-container {
  height: 100vh; /* 占满屏幕高度 */
  display: flex;
  align-items: center; /* 垂直居中 */
  justify-content: center; /* 水平居中 */
}
</style>
