<template>
  <div class="consultation-center">
    <ConversationSidebar
      :conversations="conversations"
      :active-id="activeConversationId"
      :loading="loadingConversations"
      :allow-create="canCreateConsultation"
      :subtitle="sidebarSubtitle"
      :empty-description="sidebarEmptyDescription"
      @select="handleSelectConversation"
      @create="handleCreateRequest"
    />

    <section class="conversation-panel">
      <MessageThread
        :conversation="activeConversation"
        :messages="activeMessages"
        :loading="loadingMessages"
        :current-user-id="currentUserId"
      />
      <ReplyComposer
        :disabled="!activeConversation"
        :sending="consultationStore.sending"
        :reset-key="resetKey"
        @send="handleSendReply"
      />
    </section>

    <aside class="knowledge-panel">
      <div class="knowledge-header">
        <h4>作物诊断助手</h4>
        <p>查看常见病虫害与处理建议</p>
      </div>
      <el-collapse>
        <el-collapse-item title="水稻叶瘟" name="blast">
          <p>症状：叶片出现褐色梭形斑点。</p>
          <p>处理建议：选择抗病品种，使用三环唑、稻瘟灵等药剂喷施。</p>
        </el-collapse-item>
        <el-collapse-item title="小麦赤霉病" name="scab">
          <p>症状：穗部、茎部呈褐色霉层。</p>
          <p>处理建议：抽穗期喷施氰霜唑、戊唑醇等药剂，注意轮作。</p>
        </el-collapse-item>
        <el-collapse-item title="玉米螟" name="corn-borer">
          <p>症状：心叶被蛀孔，植株生长受阻。</p>
          <p>处理建议：采用灯光诱杀、释放赤眼蜂，结合防治药剂。</p>
        </el-collapse-item>
      </el-collapse>
    </aside>

    <el-dialog
      v-if="canCreateConsultation"
      v-model="createVisible"
      title="发起新的咨询"
      width="520px"
      destroy-on-close
    >
      <el-form ref="createFormRef" :model="createForm" :rules="rules" label-width="96px">
        <el-form-item label="作物类型" prop="cropType">
          <el-select v-model="createForm.cropType" placeholder="请选择作物类型">
            <el-option label="水稻" value="水稻" />
            <el-option label="小麦" value="小麦" />
            <el-option label="玉米" value="玉米" />
            <el-option label="蔬菜" value="蔬菜" />
            <el-option label="果树" value="果树" />
          </el-select>
        </el-form-item>
        <el-form-item label="咨询主题" prop="subject">
          <el-input v-model="createForm.subject" placeholder="简要描述咨询主题" />
        </el-form-item>
        <el-form-item label="问题详情" prop="description">
          <el-input
            v-model="createForm.description"
            type="textarea"
            :rows="4"
            placeholder="请详细描述作物问题、环境与历史处理情况"
          />
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-radio-group v-model="createForm.priority">
            <el-radio-button label="normal">普通</el-radio-button>
            <el-radio-button label="high">较高</el-radio-button>
            <el-radio-button label="urgent">紧急</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="closeCreateDialog">取消</el-button>
          <el-button type="primary" :loading="consultationStore.creating" @click="submitCreate">
            提交
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import ConversationSidebar from '../components/consultation/ConversationSidebar.vue'
import MessageThread from '../components/consultation/MessageThread.vue'
import ReplyComposer from '../components/consultation/ReplyComposer.vue'
import { useConsultationStore } from '../stores/consultation'
import { useAuthStore } from '../stores/auth'

const consultationStore = useConsultationStore()
const authStore = useAuthStore()

const conversations = computed(() => consultationStore.conversations)
const activeConversation = computed(() => consultationStore.activeConversation)
const activeMessages = computed(() => consultationStore.activeMessages)
const activeConversationId = computed(() => consultationStore.activeConversationId)
const loadingConversations = computed(() => consultationStore.loadingConversations)
const loadingMessages = computed(() => consultationStore.loadingMessages)
const currentUserId = computed(() => authStore.user?.id || authStore.user?.userId || authStore.user?.username)
const canCreateConsultation = computed(() => authStore.hasAnyRole(['FARMER']))
const sidebarSubtitle = computed(() =>
  canCreateConsultation.value ? '与农业部门保持实时沟通' : '查看农户咨询并提供建议'
)
const sidebarEmptyDescription = computed(() =>
  canCreateConsultation.value ? '暂无相关会话，立即发起咨询' : '暂无新的咨询会话'
)

const createVisible = ref(false)
const resetKey = ref(0)

const createFormRef = ref(null)
const createForm = reactive({
  cropType: '',
  subject: '',
  description: '',
  priority: 'normal'
})

const rules = {
  cropType: [{ required: true, message: '请选择作物类型', trigger: 'change' }],
  subject: [{ required: true, message: '请输入咨询主题', trigger: 'blur' }],
  description: [{ required: true, message: '请填写问题详情', trigger: 'blur' }]
}

const handleSelectConversation = id => {
  consultationStore.setActiveConversation(id)
}

const handleSendReply = async payload => {
  try {
    await consultationStore.sendReply(payload)
    resetKey.value = Date.now()
    ElMessage.success('消息已发送')
    consultationStore.markAsRead()
  } catch (error) {
    const message = error?.response?.data?.message || error.message || '发送失败，请稍后重试'
    ElMessage.error(message)
  }
}

const handleCreateRequest = () => {
  if (!canCreateConsultation.value) {
    return
  }
  createVisible.value = true
}

const closeCreateDialog = () => {
  createVisible.value = false
}

const submitCreate = () => {
  if (!createFormRef.value) {
    return
  }
  createFormRef.value.validate(async valid => {
    if (!valid) {
      return
    }
    try {
      if (!canCreateConsultation.value) {
        throw new Error('当前账号无权发起咨询')
      }
      await consultationStore.createConsultation({
        cropType: createForm.cropType,
        subject: createForm.subject,
        description: createForm.description,
        priority: createForm.priority
      })
      ElMessage.success('咨询已创建，稍后将有专家回复')
      resetKey.value = Date.now()
      createVisible.value = false
      createForm.cropType = ''
      createForm.subject = ''
      createForm.description = ''
      createForm.priority = 'normal'
    } catch (error) {
      const message = error?.response?.data?.message || error.message || '创建咨询失败'
      ElMessage.error(message)
    }
  })
}

watch(
  activeConversationId,
  async newId => {
    if (!newId) {
      return
    }
    try {
      await consultationStore.loadMessages(newId)
      consultationStore.markAsRead(newId)
    } catch (error) {
      console.warn('加载消息失败', error)
    }
  },
  { immediate: false }
)

onMounted(async () => {
  try {
    await consultationStore.loadConversations()
    if (consultationStore.activeConversationId) {
      await consultationStore.loadMessages(consultationStore.activeConversationId)
      consultationStore.markAsRead()
    }
  } catch (error) {
    const message = error?.response?.data?.message || error.message || '加载咨询数据失败'
    ElMessage.error(message)
  }
  consultationStore.startPolling()
})

onUnmounted(() => {
  consultationStore.stopPolling()
})
</script>

<style scoped>
.consultation-center {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr) 280px;
  grid-template-rows: minmax(0, 1fr);
  height: calc(100vh - 80px);
  background: #ffffff;
  border-radius: 18px;
  overflow: hidden;
  box-shadow: 0 24px 60px rgba(21, 82, 56, 0.12);
}

.conversation-panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: linear-gradient(180deg, #fefefe 0%, #f5f9ff 100%);
}

.knowledge-panel {
  padding: 24px;
  border-left: 1px solid #e4e7ed;
  background: linear-gradient(180deg, #f3fbf7 0%, #ffffff 100%);
  overflow-y: auto;
}

.knowledge-header {
  margin-bottom: 16px;
}

.knowledge-header h4 {
  margin: 0;
  font-size: 16px;
  color: #1b4332;
}

.knowledge-header p {
  margin: 4px 0 0;
  font-size: 12px;
  color: #607d8b;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

@media (max-width: 1280px) {
  .consultation-center {
    grid-template-columns: 280px minmax(0, 1fr);
    grid-template-rows: minmax(0, 1fr) 240px;
    height: auto;
  }

  .knowledge-panel {
    grid-column: 1 / -1;
    border-left: none;
    border-top: 1px solid #e4e7ed;
  }
}

@media (max-width: 920px) {
  .consultation-center {
    grid-template-columns: minmax(0, 1fr);
    grid-template-rows: auto auto auto;
    height: auto;
  }

  .knowledge-panel {
    order: 3;
  }

  .conversation-panel {
    order: 2;
  }
}
</style>
