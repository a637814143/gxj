<template>
  <div class="page">
    <el-card shadow="hover">
      <template #header>系统基础设置</template>
      <el-form :model="form" label-width="120px" :inline="false" class="page__form">
        <el-form-item label="系统名称">
          <el-input v-model="form.systemName" />
        </el-form-item>
        <el-form-item label="默认地区">
          <el-select v-model="form.defaultRegion" placeholder="选择默认地区">
            <el-option label="昆明市" value="kunming" />
            <el-option label="大理州" value="dali" />
            <el-option label="红河州" value="honghe" />
          </el-select>
        </el-form-item>
        <el-form-item label="预测提前期(年)">
          <el-input-number v-model="form.horizon" :min="1" :max="5" />
        </el-form-item>
        <el-form-item label="通知邮箱">
          <el-input v-model="form.notifyEmail" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary">保存配置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive } from 'vue';

const form = reactive({
  systemName: '云南农业大数据平台',
  defaultRegion: 'dali',
  horizon: 3,
  notifyEmail: 'agri-data@ynu.edu.cn'
});
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page__form {
  max-width: 520px;
}
</style>
