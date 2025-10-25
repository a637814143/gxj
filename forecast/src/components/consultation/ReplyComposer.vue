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

    <div class="composer-tools">
      <div class="attachment-area">
        <input
          ref="fileInput"
          type="file"
          multiple
          class="file-input"
          accept="image/*,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
          @change="handleFileChange"
          :disabled="disabled || sending"
        />
        <el-button
          type="default"
          text
          :disabled="disabled || sending"
          @click="triggerFile"
        >
          <el-icon><Upload /></el-icon>
          添加附件
        </el-button>
      </div>
      <div class="composer-actions">
        <el-button
          type="primary"
          :loading="sending"
          :disabled="disabled || sending || (!draft.trim() && !attachments.length)"
          @click="handleSend"
        >
          发送
        </el-button>
      </div>
    </div>

    <transition-group tag="ul" name="attachment" class="attachment-list">
      <li v-for="file in attachments" :key="file.uid" class="attachment-item">
        <el-icon class="attachment-icon"><Paperclip /></el-icon>
        <div class="attachment-info">
          <div class="name">{{ file.name }}</div>
          <div class="size">{{ formatSize(file.size) }}</div>
        </div>
        <el-button
          text
          type="danger"
          :disabled="disabled || sending"
          @click="removeAttachment(file.uid)"
        >
          移除
        </el-button>
      </li>
    </transition-group>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Paperclip, Upload } from '@element-plus/icons-vue'

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
const attachments = ref([])
const fileInput = ref(null)

watch(
  () => props.resetKey,
  () => {
    draft.value = ''
    attachments.value = []
    if (fileInput.value) {
      fileInput.value.value = ''
    }
  }
)

const triggerFile = () => {
  fileInput.value?.click()
}

const handleFileChange = event => {
  const files = Array.from(event.target.files || [])
  const mapped = files.map(file => ({
    uid: `${file.name}-${file.size}-${file.lastModified}-${Math.random().toString(16).slice(2)}`,
    raw: file,
    name: file.name,
    size: file.size
  }))
  attachments.value = [...attachments.value, ...mapped]
  event.target.value = ''
}

const removeAttachment = uid => {
  attachments.value = attachments.value.filter(item => item.uid !== uid)
}

const handleSend = () => {
  if (props.disabled || props.sending) {
    return
  }
  const trimmed = draft.value.trim()
  if (!trimmed && !attachments.value.length) {
    ElMessage.warning('请输入消息内容或上传附件')
    return
  }
  emit('send', {
    content: trimmed,
    attachments: attachments.value.map(item => item.raw)
  })
}

const formatSize = size => {
  if (!size) {
    return '0 KB'
  }
  const units = ['B', 'KB', 'MB', 'GB']
  let index = 0
  let value = size
  while (value >= 1024 && index < units.length - 1) {
    value /= 1024
    index += 1
  }
  return `${value.toFixed(index === 0 ? 0 : 1)} ${units[index]}`
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

.composer-tools {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.attachment-area {
  display: flex;
  align-items: center;
  gap: 12px;
}

.file-input {
  display: none;
}

.attachment-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.attachment-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  border-radius: 12px;
  background: rgba(27, 127, 95, 0.08);
}

.attachment-icon {
  color: #1b4332;
}

.attachment-info {
  flex: 1;
}

.attachment-info .name {
  font-weight: 600;
  font-size: 13px;
}

.attachment-info .size {
  font-size: 12px;
  color: #607d8b;
}

.attachment-enter-active,
.attachment-leave-active {
  transition: all 0.2s ease;
}

.attachment-enter-from,
.attachment-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
