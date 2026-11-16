<template>
  <div style="padding:20px">
    <h2>我的错题本</h2>
    <el-table :data="wrongList" border>
      <el-table-column label="题目ID" prop="questionId"/>
      <el-table-column label="操作">
        <template #default="scope">
          <el-button type="danger" @click="removeWrong(scope.row.questionId)">移除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>
<script setup>
import {ref,onMounted} from 'vue'
import service from '@/utils/request'
const wrongList = ref([])

const loadWrong = async ()=>{
  const res = await service.get('/api/wrong/list')
  if(res.code === 200){
    wrongList.value = res.data
  }
}

const removeWrong = async (qId)=>{
  const res = await service.delete(`/api/wrong/remove/${qId}`)
  alert(res.msg)
  loadWrong()
}

onMounted(()=>{
  loadWrong()
})
</script>
