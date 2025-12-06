<template>
  <div class="user-management-page">
    <el-card class="intro-card" shadow="hover">
      <div class="intro-content">
        <div class="intro-text">
          <div class="intro-badge">账户与权限中心</div>
          <h1 class="intro-title">农业部门与用户统一管理</h1>
          <p class="intro-desc">
            集中维护农业部门及业务用户的基础信息，支持管理员快速分配角色、重置密码与停用账户，保障平台安全合规运行。
          </p>
          <div class="intro-highlights">
            <div class="highlight-item">
              <div class="highlight-value">{{ pagination.total }}</div>
              <div class="highlight-label">已接入账户</div>
            </div>
            <div class="highlight-item">
              <div class="highlight-value">{{ agricultureUserCount }}</div>
              <div class="highlight-label">农业部门用户</div>
            </div>
            <div class="highlight-item">
              <div class="highlight-value">{{ farmerUserCount }}</div>
              <div class="highlight-label">企业/农户用户</div>
            </div>
          </div>
        </div>
        <div class="intro-side">
          <div class="side-tip-title">操作建议</div>
          <ul class="side-tip-list">
            <li>定期核验角色配置，确保农业部门具备最新权限。</li>
            <li>为临时账户设置到期提醒，按时回收使用权限。</li>
            <li>重置密码后及时通知相关责任人完成首次登录。</li>
          </ul>
        </div>
      </div>
    </el-card>

    <el-card class="table-card" shadow="hover">
      <template #header>
        <div class="table-header">
          <div>
            <div class="table-title">用户账户列表</div>
            <div class="table-subtitle">查看与维护农业部门、企业/农户等平台用户的基础信息</div>
          </div>
          <div class="table-actions">
            <el-button @click="fetchUsers" :loading="loading">刷新</el-button>
            <el-button type="primary" @click="openCreateDialog">新增用户</el-button>
          </div>
        </div>
      </template>

      <el-table :data="users" v-loading="loading" empty-text="暂无用户数据" :header-cell-style="tableHeaderStyle">
        <el-table-column prop="username" label="用户名" min-width="140" />
        <el-table-column label="姓名" min-width="140">
          <template #default="{ row }">
            {{ row.fullName || '未填写' }}
          </template>
        </el-table-column>
        <el-table-column label="邮箱" min-width="200">
          <template #default="{ row }">
            {{ row.email || '未绑定邮箱' }}
          </template>
        </el-table-column>
        <el-table-column label="角色" min-width="220">
          <template #default="{ row }">
            <el-space wrap>
              <el-tag v-for="role in row.roles" :key="role.id" type="success" effect="dark">
                {{ formatRoleLabel(role) }}
              </el-tag>
            </el-space>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="200">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" text size="small" @click="openEditDialog(row)">编辑信息</el-button>
            <el-button type="warning" text size="small" @click="openPasswordDialog(row)">重置密码</el-button>
            <el-button
              type="danger"
              text
              size="small"
              :disabled="!canDelete(row)"
              @click="confirmDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-footer">
        <div class="table-tip">建议为农业部门及企业/农户分配匹配的角色权限，保障数据安全。</div>
        <div class="table-pagination">
          <div class="pagination-info">
            <span class="pagination-badge">每页显示 5 条</span>
            <span>分页展示（5 条/页）</span>
            <span>共 {{ pagination.total }} 条，当前第 {{ pagination.page }}/{{ totalPages }} 页</span>
          </div>
          <el-pagination
            :current-page="pagination.page"
            :page-size="pagination.size"
            :total="pagination.total"
            layout="prev, pager, next"
            background
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </el-card>

    <el-dialog
      v-model="editDialogVisible"
      :title="isCreateMode ? '新增用户' : '编辑用户信息'"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="100px" status-icon>
        <el-form-item label="用户名" prop="username">
          <el-input v-model.trim="editForm.username" :disabled="isEditMode" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item v-if="isCreateMode" label="登录密码" prop="password">
          <el-input v-model="editForm.password" type="password" show-password placeholder="请输入登录密码" />
        </el-form-item>
        <el-form-item label="姓名" prop="fullName">
          <el-input v-model="editForm.fullName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model.trim="editForm.email" placeholder="请输入邮箱地址" />
        </el-form-item>
        <el-form-item label="用户角色" prop="roleIds">
          <el-select
            v-model="editForm.roleIds"
            multiple
            filterable
            placeholder="请选择角色"
            class="full-width"
            :loading="roleLoading"
          >
            <el-option
              v-for="role in roleOptions"
              :key="role.id"
              :label="formatRoleLabel(role)"
              :value="role.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="editDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="editSubmitting" @click="submitEditForm">
            {{ isCreateMode ? '创建' : '保存' }}
          </el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog
      v-model="passwordDialogVisible"
      title="重置用户密码"
      width="480px"
      :close-on-click-modal="false"
    >
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="100px" status-icon>
        <el-form-item label="用户名">
          <el-input :model-value="passwordForm.username" disabled />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="passwordDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="passwordSubmitting" @click="submitPasswordForm">确认重置</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import apiClient from '../services/http'
import { useAuthStore } from '../stores/auth'

const PAGE_SIZE = 5

const users = ref([])
const loading = ref(false)
const pagination = reactive({ page: 1, size: PAGE_SIZE, total: 0 })
const totalPages = computed(() => {
  const total = Math.max(pagination.total, 0)
  const size = pagination.size || PAGE_SIZE
  return Math.max(1, Math.ceil(total / size || 1))
})

const roleOptions = ref([])
const roleLoading = ref(false)

const editDialogVisible = ref(false)
const editDialogMode = ref('create')
const editFormRef = ref()
const editSubmitting = ref(false)
const editForm = reactive({
  id: null,
  username: '',
  password: '',
  fullName: '',
  email: '',
  roleIds: []
})

const passwordDialogVisible = ref(false)
const passwordFormRef = ref()
const passwordSubmitting = ref(false)
const passwordForm = reactive({
  id: null,
  username: '',
  newPassword: ''
})

const authStore = useAuthStore()
const currentUserId = computed(() => authStore.user?.id ?? null)

const isCreateMode = computed(() => editDialogMode.value === 'create')
const isEditMode = computed(() => editDialogMode.value === 'edit')

const tableHeaderStyle = () => ({
  backgroundColor: '#f5f7fa',
  color: '#455a64',
  fontWeight: 500
})

const formatDateTime = value => {
  if (!value) {
    return '-'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return '-'
  }
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

const formatRoleLabel = role => {
  if (!role) {
    return ''
  }
  if (role.name && role.code) {
    return `${role.name}（${role.code}）`
  }
  return role.name || role.code || '未命名角色'
}

const sanitizeRoleIds = roleIds => {
  if (!Array.isArray(roleIds)) {
    return []
  }
  return roleIds
    .map(id => Number(id))
    .filter(id => Number.isInteger(id) && id > 0)
}

const normalizeOptional = value => {
  if (value == null) {
    return null
  }
  const trimmed = String(value).trim()
  return trimmed.length ? trimmed : null
}

const fetchRoles = async () => {
  roleLoading.value = true
  try {
    const { data } = await apiClient.get('/api/auth/users/roles')
    roleOptions.value = Array.isArray(data?.data) ? data.data : []
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '获取角色信息失败')
  } finally {
    roleLoading.value = false
  }
}

const fetchUsers = async () => {
  loading.value = true
  try {
    const { data } = await apiClient.get('/api/auth/users', {
      params: {
        page: Math.max(pagination.page - 1, 0),
        size: PAGE_SIZE
      }
    })
    const response = data?.data
    users.value = Array.isArray(response?.records) ? response.records : []
    pagination.total = Number(response?.total ?? 0)
    const currentPage = Number(response?.page ?? 0)
    const totalPageCount = Math.max(1, Math.ceil(Math.max(pagination.total, 0) / PAGE_SIZE))
    pagination.page = Math.min(Math.max(currentPage + 1, 1), totalPageCount)
    pagination.size = PAGE_SIZE
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '获取用户列表失败')
  } finally {
    loading.value = false
  }
}

const agricultureUserCount = computed(() =>
  users.value.filter(user => Array.isArray(user?.roles) && user.roles.some(role => role.code === 'AGRICULTURE_DEPT')).length
)

const farmerUserCount = computed(() =>
  users.value.filter(user => Array.isArray(user?.roles) && user.roles.some(role => role.code === 'FARMER')).length
)

const resetEditForm = () => {
  editForm.id = null
  editForm.username = ''
  editForm.password = ''
  editForm.fullName = ''
  editForm.email = ''
  editForm.roleIds = []
}

const openCreateDialog = () => {
  editDialogMode.value = 'create'
  resetEditForm()
  if (!roleOptions.value.length) {
    fetchRoles()
  }
  editDialogVisible.value = true
}

const openEditDialog = user => {
  if (!user) {
    return
  }
  editDialogMode.value = 'edit'
  editForm.id = user.id ?? null
  editForm.username = user.username ?? ''
  editForm.password = ''
  editForm.fullName = user.fullName ?? ''
  editForm.email = user.email ?? ''
  editForm.roleIds = Array.isArray(user.roles)
    ? user.roles
        .map(role => Number(role?.id))
        .filter(id => Number.isInteger(id) && id > 0)
    : []
  if (!roleOptions.value.length) {
    fetchRoles()
  }
  editDialogVisible.value = true
}

const openPasswordDialog = user => {
  if (!user) {
    return
  }
  passwordForm.id = user.id ?? null
  passwordForm.username = user.username ?? ''
  passwordForm.newPassword = ''
  passwordDialogVisible.value = true
}

const canDelete = user => {
  if (!user || user.id == null) {
    return false
  }
  if (currentUserId.value != null && Number(user.id) === Number(currentUserId.value)) {
    return false
  }
  return true
}

const handlePageChange = page => {
  pagination.page = page
  fetchUsers()
}

const validatePassword = (rule, value, callback) => {
  if (!isCreateMode.value) {
    callback()
    return
  }
  const password = value == null ? '' : String(value)
  if (!password.trim()) {
    callback(new Error('请输入密码'))
    return
  }
  if (password.length < 6 || password.length > 128) {
    callback(new Error('密码长度需要在6-128个字符之间'))
    return
  }
  callback()
}

const editRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 64, message: '用户名长度需要在2-64个字符之间', trigger: 'blur' }
  ],
  password: [{ validator: validatePassword, trigger: 'blur' }],
  fullName: [{ max: 128, message: '姓名长度不能超过128个字符', trigger: 'blur' }],
  email: [{ type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }],
  roleIds: [{ type: 'array', required: true, message: '请至少选择一个角色', trigger: 'change' }]
}

const passwordRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 128, message: '密码长度需要在6-128个字符之间', trigger: 'blur' }
  ]
}

const submitEditForm = async () => {
  if (!editFormRef.value) {
    return
  }
  try {
    await editFormRef.value.validate()
  } catch (error) {
    return
  }

  editSubmitting.value = true
  try {
    if (isCreateMode.value) {
      const payload = {
        username: editForm.username.trim(),
        password: editForm.password,
        fullName: normalizeOptional(editForm.fullName),
        email: normalizeOptional(editForm.email),
        roleIds: sanitizeRoleIds(editForm.roleIds)
      }
      await apiClient.post('/api/auth/users', payload)
      ElMessage.success('已创建新用户')
    } else {
      const payload = {
        fullName: normalizeOptional(editForm.fullName),
        email: normalizeOptional(editForm.email),
        roleIds: sanitizeRoleIds(editForm.roleIds)
      }
      await apiClient.put(`/api/auth/users/${editForm.id}`, payload)
      ElMessage.success('用户信息已更新')
    }
    editDialogVisible.value = false
    await fetchUsers()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || (isCreateMode.value ? '创建用户失败' : '更新用户失败'))
  } finally {
    editSubmitting.value = false
  }
}

const submitPasswordForm = async () => {
  if (!passwordFormRef.value) {
    return
  }
  try {
    await passwordFormRef.value.validate()
  } catch (error) {
    return
  }

  passwordSubmitting.value = true
  try {
    await apiClient.put(`/api/auth/users/${passwordForm.id}/password`, {
      newPassword: passwordForm.newPassword
    })
    ElMessage.success('密码已重置')
    passwordDialogVisible.value = false
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '重置密码失败')
  } finally {
    passwordSubmitting.value = false
  }
}

const confirmDelete = user => {
  if (!canDelete(user)) {
    return
  }
  ElMessageBox.confirm(
    `确认删除用户「${user.username}」吗？删除后该账号将无法登录系统。`,
    '删除用户',
    {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
    .then(async () => {
      try {
        await apiClient.delete(`/api/auth/users/${user.id}`)
        ElMessage.success('用户已删除')
        if (users.value.length === 1 && pagination.page > 1) {
          pagination.page -= 1
        }
        await fetchUsers()
      } catch (error) {
        ElMessage.error(error?.response?.data?.message || '删除用户失败')
      }
    })
    .catch(() => {})
}

onMounted(async () => {
  await Promise.all([fetchRoles(), fetchUsers()])
})
</script>

<style scoped>
.user-management-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.intro-card {
  border: none;
}

.intro-content {
  display: flex;
  gap: 32px;
  align-items: stretch;
}

.intro-text {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.intro-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #1b5e20;
  background: rgba(27, 94, 32, 0.12);
  border-radius: 999px;
  padding: 6px 14px;
  width: fit-content;
}

.intro-title {
  font-size: 24px;
  font-weight: 600;
  color: #1f2d3d;
  margin: 0;
}

.intro-desc {
  margin: 0;
  color: #546e7a;
  line-height: 1.6;
}

.intro-highlights {
  display: flex;
  gap: 24px;
}

.highlight-item {
  background: #f5f7fa;
  border-radius: 12px;
  padding: 16px 20px;
  min-width: 160px;
}

.highlight-value {
  font-size: 28px;
  font-weight: 600;
  color: #0d47a1;
}

.highlight-label {
  margin-top: 6px;
  font-size: 13px;
  color: #607d8b;
}

.intro-side {
  width: 280px;
  background: linear-gradient(180deg, rgba(0, 121, 107, 0.14), rgba(0, 150, 136, 0.05));
  border-radius: 16px;
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  color: #004d40;
}

.side-tip-title {
  font-weight: 600;
  font-size: 16px;
}

.side-tip-list {
  padding-left: 18px;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.table-card {
  border: none;
}

.table-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.table-title {
  font-size: 18px;
  font-weight: 600;
  color: #263238;
}

.table-subtitle {
  margin-top: 4px;
  color: #607d8b;
  font-size: 13px;
}

.table-actions {
  display: flex;
  gap: 12px;
}

.table-footer {
  margin-top: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}

.table-tip {
  color: #546e7a;
  font-size: 13px;
}

.table-pagination {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.pagination-info {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #607d8b;
  font-size: 12px;
}

.pagination-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  border-radius: 999px;
  background: #e3f2fd;
  color: #1565c0;
  font-weight: 600;
  font-size: 12px;
}

.full-width {
  width: 100%;
}

@media (max-width: 1024px) {
  .intro-content {
    flex-direction: column;
  }

  .intro-side {
    width: 100%;
    flex-direction: row;
    justify-content: space-between;
    align-items: flex-start;
  }

  .side-tip-list {
    padding-left: 16px;
  }
}
</style>
