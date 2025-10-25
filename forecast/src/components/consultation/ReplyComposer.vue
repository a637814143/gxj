<template>
  <div class="reply-composer" :class="{ disabled }">
    <div class="composer-header">
      <h4 class="composer-title">发送回复</h4>
      <p class="composer-subtitle">提供详细信息，帮助农业专家快速定位问题</p>
    </div>
    <el-input
      v-model="draft"
      type="textarea"
      :rows="4"
      resize="none"
      :disabled="disabled || sending"
      placeholder="输入您的回复内容，例如作物症状、环境信息等"
    />

    <div class="composer-actions">
      <el-button
        type="primary"
        :loading="sending"
        :disabled="disabled || sending || !draft.trim()"
        @click="handleSend"
      >
        发送
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  disabled: {
    type: Boolean,
    default: false
  },
  sending: {
    type: Boolean,
    default: false
  },
  resetKey: {
    type: Number,
    default: 0
  }
})

const emit = defineEmits(['send'])

const draft = ref('')

watch(
  () => props.resetKey,
  () => {
    draft.value = ''
  }
)

const handleSend = () => {
  if (props.disabled || props.sending) {
    return
  }
  const trimmed = draft.value.trim()
  if (!trimmed) {
    ElMessage.warning('请输入消息内容')
    return
  }
  emit('send', {
    content: trimmed
  })
}
</script>

<style scoped>
.reply-composer {
  padding: 20px 24px;
  border-top: 1px solid #e4e7ed;
  background: #f9fbfc;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.reply-composer.disabled {
  opacity: 0.6;
  pointer-events: none;
}

.composer-header {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.composer-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1b4332;
}

.composer-subtitle {
  margin: 0;
  font-size: 12px;
  color: #607d8b;
}

.composer-actions {
  display: flex;
  justify-content: flex-end;
}
</style>
