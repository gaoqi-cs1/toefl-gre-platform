<template>
  <div>
    <el-button type="primary" @click="openAddDialog">新增用户</el-button>
    <el-table :data="tableData" border style="width: 100%; margin-top:10px">
      <el-table-column prop="id" label="ID" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="createTime" label="创建时间" />
      <el-table-column label="操作">
        <template #default="scope">
          <el-button type="warning" link>编辑</el-button>
          <el-button type="danger" link>删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getUserList } from '@/api/user'
const tableData = ref([])

const loadList = async () => {
  const res = await getUserList()
  tableData.value = res.data
}
onMounted(() => loadList())
</script>
