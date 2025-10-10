<template>
  <div class="page">
    <el-card>
      <template #header>
        <span>平台设置</span>
      </template>
      <el-form label-width="120px" :model="settings">
        <el-form-item label="默认区域">
          <el-select v-model="settings.defaultRegion" placeholder="请选择">
            <el-option label="黑龙江" value="heilongjiang" />
            <el-option label="河南" value="henan" />
            <el-option label="四川" value="sichuan" />
          </el-select>
        </el-form-item>
        <el-form-item label="通知邮箱">
          <el-input v-model="settings.notifyEmail" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="模型计算集群">
          <el-switch v-model="settings.clusterEnabled" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="save">保存设置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { ElMessage } from 'element-plus'

const settings = reactive({
  defaultRegion: 'henan',
  notifyEmail: 'agri.ops@example.com',
  clusterEnabled: true
})

const save = () => {
  ElMessage.success('设置已保存，后续将与后端接口对接')
}
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
</style>
