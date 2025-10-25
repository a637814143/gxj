<template>
  <div class="setting-page">
    <section class="hero-card">
      <div class="hero-copy">
        <div class="hero-badge">云惠农作业智能分析系统</div>
        <h1 class="hero-title">平台设置中心</h1>
        <p class="hero-desc">
          统一管理系统基础配置、通知策略与计算资源开关，为业务团队提供稳定一致的使用体验。
        </p>
        <div class="hero-stats">
          <div v-for="item in highlightStats" :key="item.label" class="hero-stat">
            <div class="hero-stat-label">{{ item.label }}</div>
            <div class="hero-stat-value">{{ item.value }}</div>
            <div class="hero-stat-sub">{{ item.sub }}</div>
          </div>
        </div>
      </div>
      <div class="hero-side">
        <div class="side-card">
          <div class="side-card-title">变更提示</div>
          <div class="side-items">
            <div v-for="item in quickOverview" :key="item.label" class="side-item">
              <div class="side-item-label">{{ item.label }}</div>
              <div class="side-item-value">{{ item.value }}</div>
              <div class="side-item-trend" :class="{ up: item.trend > 0, down: item.trend < 0 }">
                {{ formatTrend(item.trend) }}
              </div>
            </div>
          </div>
          <div class="side-divider" />
          <div class="side-footer">
            <div class="side-footer-title">配置建议</div>
            <ul>
              <li v-for="notice in reminders" :key="notice">{{ notice }}</li>
            </ul>
          </div>
        </div>
        <div class="hero-decor" />
      </div>
    </section>

    <el-card class="setting-card" shadow="hover" v-loading="loading">
      <template #header>
        <div class="card-header">
          <div>
            <div class="card-title">基础参数配置</div>
            <div class="card-subtitle">调整默认区域、通知策略与计算资源开关</div>
          </div>
          <el-button type="primary" :loading="saving" :disabled="loading" @click="save">保存设置</el-button>
        </div>
      </template>
      <el-form
        ref="formRef"
        label-width="120px"
        :model="settings"
        :rules="rules"
        status-icon
        class="setting-form"
      >
        <el-form-item label="默认区域">
          <el-select v-model="settings.defaultRegion" placeholder="请选择" :disabled="loading || regionLoading">
            <el-option
              v-for="region in regions"
              :key="region.id"
              :label="region.name"
              :value="region.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="区域管理">
          <div class="region-manage">
            <div class="region-add">
              <el-input v-model="newRegionName" placeholder="输入区域名称" :disabled="loading || regionLoading" />
              <el-button type="primary" :loading="regionLoading" :disabled="loading" @click="addRegion">新增区域</el-button>
            </div>
            <el-table :data="regions" border size="small" class="region-table" :loading="regionLoading">
              <el-table-column prop="name" label="区域名称">
                <template #default="{ row }">
                  <div v-if="editingRegionId === row.id" class="region-edit-row">
                    <el-input v-model="editRegionName" class="edit-input" :disabled="regionLoading" />
                  </div>
                  <span v-else>{{ row.name }}</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="200">
                <template #default="{ row }">
                  <div class="table-actions">
                    <template v-if="editingRegionId === row.id">
                      <el-button size="small" type="primary" :loading="regionLoading" @click="confirmEdit(row)">保存</el-button>
                      <el-button size="small" :disabled="regionLoading" @click="cancelEdit">取消</el-button>
                    </template>
                    <template v-else>
                      <el-button size="small" type="primary" text :disabled="regionLoading" @click="startEdit(row)">编辑</el-button>
                      <el-button size="small" type="danger" text :disabled="regionLoading" @click="removeRegion(row)">删除</el-button>
                    </template>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-form-item>
        <el-form-item label="通知邮箱" prop="notifyEmail">
          <el-input v-model="settings.notifyEmail" placeholder="请输入" :disabled="loading" />
        </el-form-item>
        <el-form-item label="待审批变更">
          <el-input-number
            v-model="settings.pendingChangeCount"
            :min="0"
            :step="1"
            :max="999999"
            controls-position="right"
          />
        </el-form-item>
        <el-form-item label="安全策略说明">
          <el-input
            v-model="settings.securityStrategy"
            type="textarea"
            :autosize="{ minRows: 2, maxRows: 4 }"
            placeholder="请输入安全策略与合规要求"
          />
        </el-form-item>
        <el-form-item label="模型计算集群">
          <el-switch v-model="settings.clusterEnabled" />
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import apiClient from '../services/http'

const regions = ref([])
const loading = ref(false)
const regionLoading = ref(false)
const saving = ref(false)
const formRef = ref()

const settings = reactive({
  defaultRegion: null,
  notifyEmail: '',
  clusterEnabled: true,
  pendingChangeCount: 0,
  securityStrategy: '',
  updatedAt: null
})

const rules = {
  notifyEmail: [
    {
      validator: (_, value) => {
        const candidate = (value ?? '').toString().trim()
        if (!candidate) {
          return Promise.resolve()
        }
        const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
        if (!emailPattern.test(candidate)) {
          return Promise.reject(new Error('请输入有效的邮箱地址'))
        }
        return Promise.resolve()
      },
      trigger: ['blur', 'change']
    }
  ]
}

const newRegionName = ref('')
const editingRegionId = ref(null)
const editRegionName = ref('')

const defaultRegionName = computed(() => {
  const target = regions.value.find(region => region.id === settings.defaultRegion)
  return target?.name ?? '未设置'
})

const highlightStats = computed(() => [
  {
    label: '默认区域',
    value: defaultRegionName.value,
    sub: '用于快速定位数据导入与预测范围'
  },
  {
    label: '通知邮箱',
    value: settings.notifyEmail || '未配置',
    sub: '用于接收任务告警与报表推送'
  },
  {
    label: '计算资源',
    value: settings.clusterEnabled ? '集群已启用' : '单机模式',
    sub: settings.clusterEnabled ? '预测任务将自动扩容运行资源' : '建议高峰期开启集群模式'
  }
])

const quickOverview = computed(() => {
  const pending = Number.isFinite(settings.pendingChangeCount) ? settings.pendingChangeCount : 0
  return [
    { label: '待审批变更', value: `${pending} 项`, trend: pending > 0 ? 1 : 0 },
    { label: '上次更新', value: formatLastUpdated(settings.updatedAt), trend: 0 },
    { label: '安全策略', value: settings.securityStrategy || '未配置', trend: 0 }
  ]
})

const reminders = computed(() => {
  const notices = []
  if (settings.notifyEmail) {
    notices.push(`通知邮箱 ${settings.notifyEmail} 将接收任务提醒与报表推送`)
  } else {
    notices.push('尚未配置通知邮箱，建议及时补充以接收预测告警')
  }

  if (settings.clusterEnabled) {
    notices.push('模型计算集群已启用，可在高峰期保持自动扩容能力')
  } else {
    notices.push('当前集群关闭，建议在高峰任务前开启以保障性能')
  }

  notices.push(`默认区域已设置为「${defaultRegionName.value}」，可通过下方区域管理动态调整`)
  return notices
})

function formatTrend(value) {
  if (value > 0) return `较昨日 +${value}`
  if (value < 0) return `较昨日 ${value}`
  return '较昨日 持平'
}

function formatLastUpdated(value) {
  if (!value) return '暂无记录'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  const diff = Date.now() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes} 分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours} 小时前`
  const days = Math.floor(hours / 24)
  if (days < 30) return `${days} 天前`
  const months = Math.floor(days / 30)
  if (months < 12) return `${months} 个月前`
  const years = Math.floor(days / 365)
  return `${years} 年前`
}

const applyRegionList = list => {
  const normalized = Array.isArray(list)
    ? list
        .filter(item => item && item.id != null)
        .map(item => ({
          id: item.id,
          name: item.name,
          code: item.code,
          level: item.level,
          parentCode: item.parentCode,
          parentName: item.parentName,
          description: item.description
        }))
    : []

  regions.value = normalized.sort((a, b) => String(a.name || '').localeCompare(String(b.name || ''), 'zh-Hans-CN'))
  ensureDefaultRegionValidity()
}

const applySettings = payload => {
  settings.defaultRegion = payload?.defaultRegionId ?? null
  settings.notifyEmail = payload?.notifyEmail ?? ''
  settings.clusterEnabled = Boolean(payload?.clusterEnabled)
  settings.pendingChangeCount = Number(payload?.pendingChangeCount ?? 0)
  settings.securityStrategy = payload?.securityStrategy ?? ''
  const updatedAtValue = payload?.updatedAt ?? payload?.updated_at ?? null
  settings.updatedAt = updatedAtValue ?? settings.updatedAt ?? null
  ensureDefaultRegionValidity()
}

const ensureDefaultRegionValidity = () => {
  if (!regions.value.length) {
    return
  }
  if (settings.defaultRegion == null) {
    settings.defaultRegion = regions.value[0].id
    return
  }
  const exists = regions.value.some(region => region.id === settings.defaultRegion)
  if (!exists) {
    settings.defaultRegion = regions.value[0].id
  }
}

const fetchRegions = async () => {
  regionLoading.value = true
  try {
    const { data } = await apiClient.get('/api/base/regions')
    const list = Array.isArray(data?.data) ? data.data : data
    applyRegionList(list)
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '加载区域列表失败')
    applyRegionList([])
  } finally {
    regionLoading.value = false
  }
}

const fetchSettings = async () => {
  loading.value = true
  try {
    const { data } = await apiClient.get('/api/system/settings')
    applySettings(data?.data ?? data ?? {})
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '加载系统设置失败')
  } finally {
    loading.value = false
  }
}

const initialize = async () => {
  loading.value = true
  try {
    await Promise.all([fetchRegions(), fetchSettings()])
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  initialize()
})

const addRegion = async () => {
  const name = newRegionName.value.trim()
  if (!name) {
    ElMessage.warning('请输入区域名称')
    return
  }
  if (regions.value.some(region => region.name === name)) {
    ElMessage.warning('区域名称已存在')
    return
  }
  regionLoading.value = true
  try {
    await apiClient.post('/api/base/regions', { name, level: 'PREFECTURE' })
    await fetchRegions()
    if (!settings.defaultRegion && regions.value.length) {
      settings.defaultRegion = regions.value[0].id
    }
    newRegionName.value = ''
    ElMessage.success('区域已新增')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '新增区域失败')
  } finally {
    regionLoading.value = false
  }
}

const startEdit = region => {
  editingRegionId.value = region.id
  editRegionName.value = region.name
}

const cancelEdit = () => {
  editingRegionId.value = null
  editRegionName.value = ''
}

const confirmEdit = async region => {
  const name = editRegionName.value.trim()
  if (!name) {
    ElMessage.warning('请输入区域名称')
    return
  }
  if (regions.value.some(item => item.name === name && item.id !== region.id)) {
    ElMessage.warning('区域名称已存在')
    return
  }

  regionLoading.value = true
  try {
    await apiClient.put(`/api/base/regions/${region.id}`, {
      code: region.code,
      name,
      level: region.level,
      parentCode: region.parentCode,
      parentName: region.parentName,
      description: region.description
    })
    await fetchRegions()
    cancelEdit()
    ElMessage.success('区域已更新')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '更新区域失败')
  } finally {
    regionLoading.value = false
  }
}

const removeRegion = async region => {
  if (regions.value.length === 1) {
    ElMessage.warning('至少保留一个区域配置')
    return
  }
  try {
    await ElMessageBox.confirm(`确定删除区域「${region.name}」吗？`, '提示', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch (error) {
    return
  }

  regionLoading.value = true
  try {
    await apiClient.delete(`/api/base/regions/${region.id}`)
    await fetchRegions()
    if (editingRegionId.value === region.id) {
      cancelEdit()
    }
    ElMessage.success('区域已删除')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '删除区域失败')
  } finally {
    regionLoading.value = false
  }
}

const save = async () => {
  const form = formRef.value
  if (form) {
    try {
      await form.validate()
    } catch (error) {
      return
    }
  }
  saving.value = true
  try {
    const payload = {
      defaultRegionId: settings.defaultRegion ?? null,
      notifyEmail: settings.notifyEmail.trim() ? settings.notifyEmail.trim() : null,
      clusterEnabled: settings.clusterEnabled,
      pendingChangeCount: settings.pendingChangeCount,
      securityStrategy: settings.securityStrategy.trim() ? settings.securityStrategy.trim() : null
    }
    const { data } = await apiClient.put('/api/system/settings', payload)
    const applied = data?.data ?? data ?? {}
    applySettings(applied)
    if (!(applied?.updatedAt ?? applied?.updated_at)) {
      settings.updatedAt = new Date().toISOString()
    }
    ElMessage.success('设置已保存')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '保存设置失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.setting-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding-bottom: 32px;
}

.hero-card {
  position: relative;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 32px;
  padding: 32px;
  border-radius: 24px;
  background: linear-gradient(120deg, #e8f1ff 0%, #f4f8ff 35%, #ffffff 100%);
  box-shadow: 0 28px 60px rgba(51, 112, 255, 0.15);
  overflow: hidden;
}

.hero-card::before {
  content: '';
  position: absolute;
  top: -160px;
  right: -160px;
  width: 400px;
  height: 400px;
  background: radial-gradient(circle at center, rgba(60, 132, 255, 0.35), transparent 65%);
  transform: rotate(12deg);
}

.hero-copy {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 16px;
  font-size: 13px;
  border-radius: 999px;
  color: #2262ff;
  background: rgba(34, 98, 255, 0.12);
  font-weight: 600;
  letter-spacing: 1px;
}

.hero-title {
  margin: 0;
  font-size: 28px;
  color: #0d1b3d;
  font-weight: 700;
}

.hero-desc {
  margin: 0;
  font-size: 14px;
  line-height: 1.8;
  color: #455471;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 16px;
}

.hero-stat {
  padding: 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.85);
  box-shadow: inset 0 0 0 1px rgba(34, 98, 255, 0.08);
  backdrop-filter: blur(4px);
}

.region-manage {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
}

.region-add {
  display: flex;
  gap: 12px;
  align-items: center;
}

.region-add :deep(.el-input) {
  flex: 1;
}

.region-table {
  width: 100%;
}

.table-actions {
  display: flex;
  gap: 8px;
}

.region-edit-row {
  display: flex;
  align-items: center;
}

.region-edit-row .edit-input {
  width: 100%;
}

.hero-stat-label {
  font-size: 12px;
  color: #5c6d8d;
  margin-bottom: 8px;
}

.hero-stat-value {
  font-size: 20px;
  font-weight: 700;
  color: #163b8c;
  margin-bottom: 6px;
}

.hero-stat-sub {
  font-size: 12px;
  color: #6e7fa1;
}

.hero-side {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 24px;
}

.side-card {
  border-radius: 20px;
  padding: 24px;
  background: linear-gradient(160deg, rgba(34, 98, 255, 0.92) 0%, rgba(100, 149, 255, 0.8) 65%, rgba(255, 255, 255, 0.95) 100%);
  color: #fff;
  box-shadow: 0 20px 45px rgba(32, 84, 204, 0.25);
  overflow: hidden;
  position: relative;
}

.side-card::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.12), transparent 65%);
  pointer-events: none;
}

.side-card-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 20px;
}

.side-items {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.side-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.16);
  backdrop-filter: blur(2px);
}

.side-item-label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.85);
}

.side-item-value {
  font-size: 18px;
  font-weight: 700;
}

.side-item-trend {
  font-size: 12px;
  font-weight: 500;
}

.side-item-trend.up {
  color: #91ffba;
}

.side-item-trend.down {
  color: #ffd48a;
}

.side-divider {
  height: 1px;
  margin: 24px 0;
  background: rgba(255, 255, 255, 0.28);
}

.side-footer-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 12px;
}

.side-footer ul {
  margin: 0;
  padding-left: 18px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: rgba(255, 255, 255, 0.85);
  font-size: 13px;
}

.hero-decor {
  height: 140px;
  border-radius: 22px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.8), rgba(214, 228, 255, 0.6));
  box-shadow: inset 0 0 0 1px rgba(34, 98, 255, 0.1);
}

.setting-card {
  border-radius: 20px;
  overflow: hidden;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a2a4a;
}

.card-subtitle {
  font-size: 13px;
  color: #6c7d9c;
  margin-top: 4px;
}

.setting-form {
  margin-top: 12px;
}

@media (max-width: 992px) {
  .hero-card {
    padding: 24px;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .card-header > :last-child {
    width: 100%;
    display: flex;
    justify-content: flex-end;
  }
}

@media (max-width: 768px) {
  .hero-stats {
    grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  }

  .card-header > :last-child {
    justify-content: flex-start;
  }
}
</style>
