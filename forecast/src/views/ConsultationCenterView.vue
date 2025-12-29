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
        :allow-close="canCloseActiveConversation"
        :closing="consultationStore.closing"
        @close="handleCloseConversation"
        @recall="handleRecallMessage"
      />
      <ReplyComposer
        :disabled="composerDisabled"
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
        <el-form-item label="咨询部门" prop="departmentCode">
          <el-select
            v-model="createForm.departmentCode"
            placeholder="请选择需要咨询的部门"
            :loading="loadingDepartments"
            loading-text="加载部门列表中..."
            filterable
            no-data-text="暂无可用部门"
          >
            <el-option
              v-for="option in departmentOptions"
              :key="option.code"
              :label="option.name"
              :value="option.code"
            />
          </el-select>
          <transition name="fade">
            <div v-if="selectedDepartment" class="department-hint">
              <div class="department-hint__header">
                <span class="department-hint__name">{{ selectedDepartment.name }}</span>
                <el-tag type="success" size="small" effect="dark">部门专属</el-tag>
              </div>
              <p class="department-hint__description" v-if="selectedDepartment.description">
                {{ selectedDepartment.description }}
              </p>
              <p class="department-hint__contact" v-if="selectedDepartment.contactName">
                值班专家：{{ selectedDepartment.contactName }}
                <span v-if="selectedDepartment.contactUsername">（账号 {{ selectedDepartment.contactUsername }}）</span>
              </p>
              <p class="department-hint__note">{{ departmentLockMessage }}</p>
            </div>
          </transition>
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

const LAST_DEPARTMENT_KEY = 'consultation:last_department'

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
const canCloseConversation = computed(() => authStore.hasAnyRole(['FARMER', 'AGRICULTURE_DEPT', 'ADMIN']))
const sidebarSubtitle = computed(() =>
  canCreateConsultation.value ? '与农业部门保持实时沟通' : '查看农户咨询并提供建议'
)
const sidebarEmptyDescription = computed(() =>
  canCreateConsultation.value ? '暂无相关会话，立即发起咨询' : '暂无新的咨询会话'
)
const conversationClosed = computed(
  () => (activeConversation.value?.status || '').toLowerCase() === 'closed'
)
const canCloseActiveConversation = computed(
  () => canCloseConversation.value && !!activeConversation.value && !conversationClosed.value
)
const composerDisabled = computed(() => !activeConversation.value || conversationClosed.value)
const departmentOptions = computed(() => consultationStore.departments || [])
const loadingDepartments = computed(() => consultationStore.loadingDepartments)

const createVisible = ref(false)
const resetKey = ref(0)

const createFormRef = ref(null)
const createForm = reactive({
  cropType: '',
  departmentCode: '',
  subject: '',
  description: '',
  priority: 'normal'
})

const selectedDepartment = computed(() =>
  departmentOptions.value.find(item => item.code === createForm.departmentCode) || null
)

const departmentLockMessage = computed(() => {
  if (!selectedDepartment.value) {
    return '选择咨询部门后，只有该部门的农业专家可以查看会话内容。'
  }
  return `当前对话仅向${selectedDepartment.value.name}开放，其它部门无法查看。`
})

const rules = {
  cropType: [{ required: true, message: '请选择作物类型', trigger: 'change' }],
  departmentCode: [{ required: true, message: '请选择咨询部门', trigger: 'change' }],
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

const handleRecallMessage = async message => {
  if (!message?.id) {
    return
  }
  try {
    await consultationStore.recallMessage({
      consultationId: activeConversationId.value,
      messageId: message.id
    })
    ElMessage.success('消息已撤回')
  } catch (error) {
    const text = error?.response?.data?.message || error.message || '撤回失败，请稍后重试'
    ElMessage.error(text)
  }
}

const handleCloseConversation = async () => {
  if (!activeConversation.value) {
    return
  }
  try {
    await consultationStore.closeConversation(activeConversation.value.id)
    resetKey.value = Date.now()
    ElMessage.success('对话已结束')
  } catch (error) {
    const message = error?.response?.data?.message || error.message || '结束对话失败'
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
        departmentCode: createForm.departmentCode,
        subject: createForm.subject,
        description: createForm.description,
        priority: createForm.priority
      })
      ElMessage.success('咨询已创建，稍后将有专家回复')
      resetKey.value = Date.now()
      createVisible.value = false
      createForm.cropType = ''
      createForm.departmentCode = ''
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
  consultationStore
    .loadDepartments()
    .then(options => {
      if (typeof window !== 'undefined') {
        const cached = window.localStorage?.getItem(LAST_DEPARTMENT_KEY)
        if (cached && Array.isArray(options) && options.some(option => option.code === cached)) {
          createForm.departmentCode = cached
        }
      }
    })
    .catch(error => {
      console.warn('加载咨询部门失败', error)
    })
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

watch(
  () => createForm.departmentCode,
  value => {
    if (value && typeof window !== 'undefined') {
      try {
        window.localStorage?.setItem(LAST_DEPARTMENT_KEY, value)
      } catch (error) {
        console.warn('缓存咨询部门失败', error)
      }
    }
  }
)
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
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: rgba(27, 67, 50, 0.28) transparent;
  box-shadow: 0 24px 60px rgba(21, 82, 56, 0.12);
}

.consultation-center::-webkit-scrollbar {
  width: 8px;
}

.consultation-center::-webkit-scrollbar-track {
  background: transparent;
}

.consultation-center::-webkit-scrollbar-thumb {
  background: rgba(27, 67, 50, 0.28);
  border-radius: 8px;
}

.consultation-center:hover::-webkit-scrollbar-thumb {
  background: rgba(27, 67, 50, 0.4);
}

.conversation-panel {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  background: linear-gradient(180deg, #fefefe 0%, #f5f9ff 100%);
}

.department-hint {
  margin-top: 10px;
  padding: 12px 14px;
  border-radius: 12px;
  background: rgba(15, 76, 58, 0.06);
  border: 1px solid rgba(15, 76, 58, 0.18);
  color: #1d3b2f;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.department-hint__header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.department-hint__name {
  font-size: 14px;
}

.department-hint__description {
  font-size: 13px;
  color: #2f4f40;
  line-height: 1.6;
}

.department-hint__contact {
  font-size: 13px;
  color: #245847;
}

.department-hint__note {
  font-size: 12px;
  color: #5a6f66;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.24s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.knowledge-panel {
  min-width: 0;
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

.department-hint {
  margin-top: 6px;
  font-size: 12px;
  color: #607d8b;
  line-height: 1.5;
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
