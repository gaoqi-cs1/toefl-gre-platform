<template>
  <div class="home">
    <el-container style="height:100vh">
      <!-- 侧边导航栏 -->
      <el-aside width="200px" style="background-color: #304156;">
        <el-menu
          router
          background-color="#304156"
          text-color="#fff"
          active-text-color="#ffd04b"
        >
          <!-- 菜单路径必须和 router/index.js 里的真实路由一致：
               原来写 /tpo/list、/user/wrong，这两个路由都不存在，点了会白屏 -->
          <el-menu-item index="/tpo">
            <span>TPO题库</span>
          </el-menu-item>
          <el-menu-item index="/wrongbook">
            <span>错题本</span>
          </el-menu-item>
          <!-- 真题套题：全站唯一的套题入口（旧的「真题模考 / 模考记录」两条菜单已移除，
               两套列表只保留这一套；旧页面文件仍在磁盘上，未删除） -->
          <el-menu-item index="/full">
            <span>真题套题</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <!-- 右侧内容区域，路由出口 -->
      <el-main style="padding:0;">
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>
<script setup>
/**
 * 首页布局页面
 * 侧边菜单栏 + 右侧路由容器
 * 进入home之后，所有子页面在右侧渲染
 */
</script>
<style scoped>
.home{
  width:100%;
  height:100vh;
  margin:0;
  padding:0;
}
</style>
