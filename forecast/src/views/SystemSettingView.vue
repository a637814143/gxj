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

    <el-card class="setting-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <div>
            <div class="card-title">基础参数配置</div>
            <div class="card-subtitle">调整默认区域、通知策略与计算资源开关</div>
          </div>
          <el-button type="primary" @click="save">保存设置</el-button>
        </div>
      </template>
      <el-form label-width="120px" :model="settings" class="setting-form">
        <el-form-item label="默认区域">
          <el-select v-model="settings.defaultRegion" placeholder="请选择">
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
              <el-input v-model="newRegionName" placeholder="输入区域名称" />
              <el-button type="primary" @click="addRegion">新增区域</el-button>
            </div>
            <el-table :data="regions" border size="small" class="region-table">
              <el-table-column prop="name" label="区域名称">
                <template #default="{ row }">
                  <div v-if="editingRegionId === row.id" class="region-edit-row">
                    <el-input v-model="editRegionName" class="edit-input" />
                  </div>
                  <span v-else>{{ row.name }}</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="200">
                <template #default="{ row }">
                  <div class="table-actions">
                    <template v-if="editingRegionId === row.id">
                      <el-button size="small" type="primary" @click="confirmEdit(row)">保存</el-button>
                      <el-button size="small" @click="cancelEdit">取消</el-button>
                    </template>
                    <template v-else>
                      <el-button size="small" type="primary" text @click="startEdit(row)">编辑</el-button>
                      <el-button size="small" type="danger" text @click="removeRegion(row)">删除</el-button>
                    </template>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-form-item>
        <el-form-item label="通知邮箱">
          <el-input v-model="settings.notifyEmail" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="模型计算集群">
          <el-switch v-model="settings.clusterEnabled" />
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const regions = ref([
  { id: 'heilongjiang', name: '黑龙江' },
  { id: 'henan', name: '河南' },
  { id: 'sichuan', name: '四川' },
  { id: 'yunnan', name: '云南' }
])

const settings = reactive({
  defaultRegion: 'henan',
  notifyEmail: 'agri.ops@example.com',
  clusterEnabled: true
})

if (!regions.value.some((region) => region.id === settings.defaultRegion) && regions.value.length) {
  settings.defaultRegion = regions.value[0].id
}

const newRegionName = ref('')
const editingRegionId = ref(null)
const editRegionName = ref('')

const defaultRegionName = computed(() => {
  const match = regions.value.find((region) => region.id === settings.defaultRegion)
  return match ? match.name : '未设置'
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

const quickOverview = computed(() => [
  { label: '待审批变更', value: settings.clusterEnabled ? '0 项' : '1 项', trend: settings.clusterEnabled ? 0 : 1 },
  { label: '上次更新', value: '3 天前', trend: 0 },
  { label: '安全策略', value: '双因素', trend: 0 }
])

const reminders = [
  '建议为敏感操作开启审批流程与操作日志留存',
  '通知邮箱支持多人配置，可用逗号分隔',
  '集群资源调整后需同步运维团队确认容量'
]

function formatTrend(value) {
  if (value > 0) return `较昨日 +${value}`
  if (value < 0) return `较昨日 ${value}`
  return '较昨日 持平'
}

const createRegionId = () => {
  let base = `custom-${Date.now()}`
  while (regions.value.some((region) => region.id === base)) {
    base = `custom-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`
  }
  return base
}

const addRegion = () => {
  const name = newRegionName.value.trim()
  if (!name) {
    ElMessage.warning('请输入区域名称')
    return
  }
  if (regions.value.some((region) => region.name === name)) {
    ElMessage.warning('区域名称已存在')
    return
  }
  regions.value.push({ id: createRegionId(), name })
  if (!settings.defaultRegion) {
    settings.defaultRegion = regions.value[0].id
  }
  newRegionName.value = ''
  ElMessage.success('区域已新增')
}

const startEdit = (region) => {
  editingRegionId.value = region.id
  editRegionName.value = region.name
}

const cancelEdit = () => {
  editingRegionId.value = null
  editRegionName.value = ''
}

const confirmEdit = (region) => {
  const name = editRegionName.value.trim()
  if (!name) {
    ElMessage.warning('请输入区域名称')
    return
  }
  if (regions.value.some((item) => item.name === name && item.id !== region.id)) {
    ElMessage.warning('区域名称已存在')
    return
  }
  region.name = name
  cancelEdit()
  ElMessage.success('区域已更新')
}

const removeRegion = async (region) => {
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

  const index = regions.value.findIndex((item) => item.id === region.id)
  if (index !== -1) {
    regions.value.splice(index, 1)
  }

  if (!regions.value.some((item) => item.id === settings.defaultRegion)) {
    settings.defaultRegion = regions.value.length ? regions.value[0].id : ''
  }

  if (editingRegionId.value === region.id) {
    cancelEdit()
  }

  ElMessage.success('区域已删除')
}

const save = () => {
  ElMessage.success('设置已保存，后续将与后端接口对接')
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
