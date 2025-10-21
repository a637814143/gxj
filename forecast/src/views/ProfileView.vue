<template>
  <div class="profile-page">
    <PageHeader
      badge="账户中心"
      title="个人资料与偏好设置"
      description="管理账号安全、完善业务资料并配置个性化偏好，确保工作流程与数据管理更加顺畅。"
    >
      <template #meta>
        <div class="header-metrics">
          <div class="metric-item">
            <div class="metric-value">{{ lastLoginDisplay }}</div>
            <div class="metric-label">最近登录</div>
          </div>
          <div class="metric-item">
            <div class="metric-value">{{ devices.length }}</div>
            <div class="metric-label">活跃设备</div>
          </div>
          <div class="metric-item">
            <div class="metric-value">{{ completionRate }}%</div>
            <div class="metric-label">资料完成度</div>
          </div>
        </div>
      </template>
      <template #extra>
        <el-space wrap :size="12">
          <el-button @click="resetAll">恢复默认设置</el-button>
          <el-button type="primary" @click="saveAll" :loading="savingAll">保存全部修改</el-button>
        </el-space>
      </template>
    </PageHeader>

    <el-row :gutter="24" class="profile-grid">
      <el-col :xs="24" :lg="16">
        <el-card class="section-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <div>
                <div class="card-title">安全设置</div>
                <div class="card-subtitle">更新密码、查看设备登录情况并保障账号安全。</div>
              </div>
            </div>
          </template>

          <div class="card-section">
            <div class="section-header">
              <div>
                <div class="section-title">密码修改</div>
                <div class="section-description">建议定期更换密码，并确保至少包含 8 位字符（含数字与符号）。</div>
                <div class="section-meta">上次修改：{{ lastPasswordChangeDisplay }}</div>
              </div>
              <el-button type="primary" :loading="savingPassword" @click="submitPasswordForm">保存密码</el-button>
            </div>
            <el-form
              ref="passwordFormRef"
              :model="passwordForm"
              :rules="passwordRules"
              label-width="120px"
              class="section-form"
              status-icon
            >
              <el-form-item label="当前密码" prop="currentPassword">
                <el-input
                  v-model="passwordForm.currentPassword"
                  type="password"
                  show-password
                  autocomplete="current-password"
                  placeholder="请输入当前密码"
                />
              </el-form-item>
              <el-form-item label="新密码" prop="newPassword">
                <el-input
                  v-model="passwordForm.newPassword"
                  type="password"
                  show-password
                  autocomplete="new-password"
                  placeholder="请输入新密码"
                />
              </el-form-item>
              <el-form-item label="确认新密码" prop="confirmPassword">
                <el-input
                  v-model="passwordForm.confirmPassword"
                  type="password"
                  show-password
                  autocomplete="new-password"
                  placeholder="请再次输入新密码"
                />
              </el-form-item>
            </el-form>
          </div>

          <el-divider />

          <div class="card-section">
            <div class="section-header">
              <div>
                <div class="section-title">设备管理</div>
                <div class="section-description">查看最近登录的设备，及时注销异常登录。</div>
              </div>
              <el-button text type="primary" @click="markDevicesTrusted">全部标记为信任</el-button>
            </div>
            <el-table :data="devices" class="device-table" empty-text="暂无设备记录">
              <el-table-column prop="name" label="设备" min-width="200">
                <template #default="{ row }">
                  <div class="device-name">{{ row.name }}</div>
                  <div class="device-meta">{{ row.browser }} · {{ row.system }}</div>
                </template>
              </el-table-column>
              <el-table-column prop="location" label="登录地点" min-width="180" />
              <el-table-column prop="lastActive" label="最近活跃" min-width="180" />
              <el-table-column label="状态" width="120">
                <template #default="{ row }">
                  <el-tag :type="row.trusted ? 'success' : 'warning'" effect="plain">
                    {{ row.trusted ? '已信任' : '待确认' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="140" fixed="right">
                <template #default="{ row }">
                  <el-space size="12">
                    <el-button text type="primary" size="small" @click="toggleTrust(row)">
                      {{ row.trusted ? '取消信任' : '信任设备' }}
                    </el-button>
                    <el-button text type="danger" size="small" @click="revokeDevice(row)">
                      注销
                    </el-button>
                  </el-space>
                </template>
              </el-table-column>
            </el-table>
            <el-alert
              class="security-tip"
              type="info"
              show-icon
              title="如果发现异常设备，请立即注销并修改密码。"
            />
          </div>
        </el-card>

        <el-card class="section-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <div>
                <div class="card-title">业务信息</div>
                <div class="card-subtitle">完善工作职责、业务覆盖范围等信息，帮助系统按需推荐数据与任务。</div>
              </div>
              <el-button type="primary" :loading="savingBusiness" @click="saveBusinessInfo">保存业务资料</el-button>
            </div>
          </template>

          <el-form
            ref="businessFormRef"
            :model="businessForm"
            label-position="top"
            class="section-form business-form"
          >
            <div
              v-for="group in businessFieldGroups"
              :key="group.id"
              class="card-section"
            >
              <div class="section-header compact">
                <div>
                  <div class="section-title">{{ group.title }}</div>
                  <div class="section-description">{{ group.description }}</div>
                </div>
              </div>
              <el-row :gutter="16" class="business-field-grid">
                <el-col
                  v-for="field in group.fields"
                  :key="field.key"
                  :xs="24"
                  :sm="field.fullWidth ? 24 : 12"
                >
                  <el-form-item :label="field.label" :prop="field.key" :rules="field.rules">
                    <component
                      :is="resolveFieldComponent(field)"
                      v-model="businessForm[field.key]"
                      v-bind="field.props"
                    >
                      <template v-if="field.type === 'select'" #default>
                        <el-option
                          v-for="option in field.options"
                          :key="option.value"
                          :label="option.label"
                          :value="option.value"
                        />
                      </template>
                    </component>
                    <div v-if="field.hint" class="field-hint">{{ field.hint }}</div>
                  </el-form-item>
                </el-col>
              </el-row>
            </div>
          </el-form>

          <el-divider />

          <div class="card-section">
            <div class="section-header">
              <div>
                <div class="section-title">自定义字段</div>
                <div class="section-description">按需扩展业务信息，支持为合作项目或地区创建个性化字段。</div>
              </div>
              <el-button type="primary" text @click="addCustomField">
                <el-icon class="inline-icon"><Plus /></el-icon>
                添加字段
              </el-button>
            </div>
            <div
              v-for="field in customFields"
              :key="field.id"
              class="custom-field"
            >
              <el-input
                v-model.trim="field.label"
                placeholder="字段名称（如：驻点地区）"
              />
              <el-input
                v-model.trim="field.value"
                placeholder="字段内容"
              />
              <el-button
                circle
                text
                type="danger"
                :disabled="customFields.length === 1"
                @click="removeCustomField(field.id)"
              >
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="8">
        <el-card class="section-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <div>
                <div class="card-title">个性化设置</div>
                <div class="card-subtitle">根据偏好配置主题、通知与工作方式，打造舒适的使用体验。</div>
              </div>
            </div>
          </template>

          <div class="card-section">
            <div class="section-header compact">
              <div class="section-title">主题与外观</div>
            </div>
            <el-form :model="personalizationForm" label-position="top" class="section-form">
              <el-form-item label="主题模式">
                <el-radio-group v-model="personalizationForm.theme">
                  <el-radio-button label="system">跟随系统</el-radio-button>
                  <el-radio-button label="light">浅色</el-radio-button>
                  <el-radio-button label="dark">深色</el-radio-button>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="强调色">
                <el-color-picker v-model="personalizationForm.accentColor" show-alpha />
              </el-form-item>
              <el-form-item label="界面密度">
                <el-radio-group v-model="personalizationForm.density">
                  <el-radio-button
                    v-for="option in densityOptions"
                    :key="option.value"
                    :label="option.value"
                  >
                    {{ option.label }}
                  </el-radio-button>
                </el-radio-group>
              </el-form-item>
            </el-form>
          </div>

          <el-divider />

          <div class="card-section">
            <div class="section-header compact">
              <div class="section-title">通知偏好</div>
            </div>
            <div class="notification-item" v-for="channel in notificationChannels" :key="channel.key">
              <div class="notification-info">
                <div class="notification-title">{{ channel.title }}</div>
                <div class="notification-description">{{ channel.description }}</div>
              </div>
              <el-switch v-model="personalizationForm.notifications[channel.key]" />
            </div>
            <el-divider />
            <el-form :model="personalizationForm" label-position="top" class="section-form">
              <el-form-item label="通知摘要频率">
                <el-select v-model="personalizationForm.digestFrequency" placeholder="请选择推送频率">
                  <el-option label="实时推送" value="realtime" />
                  <el-option label="按日汇总" value="daily" />
                  <el-option label="按周汇总" value="weekly" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-checkbox v-model="personalizationForm.quietMode">
                  夜间（22:00-8:00）启用勿扰模式，仅保留紧急通知
                </el-checkbox>
              </el-form-item>
            </el-form>
            <el-button type="primary" :loading="savingPersonalization" @click="savePersonalization">
              保存个性化设置
            </el-button>
          </div>
        </el-card>

        <el-card class="section-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <div>
                <div class="card-title">数据管理</div>
                <div class="card-subtitle">导出个人相关数据或清理冗余记录，满足合规与安全需求。</div>
              </div>
            </div>
          </template>

          <div class="card-section">
            <div class="section-header compact">
              <div class="section-title">数据导出</div>
              <div class="section-description">选择需要导出的数据类型与时间范围。</div>
            </div>
            <el-form :model="exportForm" label-position="top" class="section-form">
              <el-form-item label="导出范围">
                <el-radio-group v-model="exportForm.scope">
                  <el-radio label="profile">个人资料</el-radio>
                  <el-radio label="activity">操作日志</el-radio>
                  <el-radio label="all">全部数据</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="导出格式">
                <el-select v-model="exportForm.format" placeholder="请选择格式">
                  <el-option label="Excel (.xlsx)" value="xlsx" />
                  <el-option label="CSV" value="csv" />
                  <el-option label="JSON" value="json" />
                </el-select>
              </el-form-item>
              <el-form-item label="时间范围">
                <el-date-picker
                  v-model="exportForm.range"
                  type="daterange"
                  unlink-panels
                  range-separator="至"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                  format="YYYY-MM-DD"
                />
              </el-form-item>
              <el-form-item>
                <el-checkbox v-model="exportForm.includeAudit">包含安全审计信息</el-checkbox>
              </el-form-item>
              <el-button type="primary" :loading="exportingData" @click="handleExportData">
                导出数据
              </el-button>
            </el-form>
            <el-alert
              v-if="lastExportTime"
              class="export-tip"
              type="success"
              :title="`上次导出：${lastExportTime}`"
              show-icon
            />
          </div>

          <el-divider />

          <div class="card-section">
            <div class="section-header compact">
              <div class="section-title">数据清理</div>
              <div class="section-description">按需清理缓存、历史记录等冗余信息，释放空间并保护隐私。</div>
            </div>
            <el-checkbox-group v-model="cleanupSelection" class="cleanup-list">
              <el-checkbox
                v-for="item in cleanupOptions"
                :key="item.value"
                :label="item.value"
              >
                <div class="cleanup-item">
                  <div class="cleanup-title">{{ item.title }}</div>
                  <div class="cleanup-description">{{ item.description }}</div>
                </div>
              </el-checkbox>
            </el-checkbox-group>
            <div class="cleanup-actions">
              <el-button @click="cleanupSelection = []">清除选择</el-button>
              <el-button
                type="danger"
                :disabled="!cleanupSelection.length"
                :loading="cleaningData"
                @click="handleCleanup"
              >
                立即清理
              </el-button>
            </div>
            <el-timeline class="cleanup-timeline">
              <el-timeline-item
                v-for="record in cleanupHistory"
                :key="record.id"
                :timestamp="record.time"
                :type="record.type"
              >
                {{ record.description }}
              </el-timeline-item>
            </el-timeline>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Plus } from '@element-plus/icons-vue'
import PageHeader from '../components/PageHeader.vue'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()

const savingAll = ref(false)
const savingPassword = ref(false)
const savingBusiness = ref(false)
const savingPersonalization = ref(false)
const exportingData = ref(false)
const cleaningData = ref(false)
const lastExportTime = ref('')

const passwordFormRef = ref()
const businessFormRef = ref()

const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirmPassword = (_rule, value, callback) => {
  if (!value) {
    callback(new Error('请再次输入新密码'))
    return
  }
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
    return
  }
  callback()
}

const passwordRules = {
  currentPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, message: '密码长度不能少于 8 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const resetPasswordForm = () => {
  passwordForm.currentPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
}

let customFieldIdSeed = 1
const nextCustomFieldId = () => customFieldIdSeed++

let cleanupHistoryIdSeed = 1
const nextCleanupHistoryId = () => cleanupHistoryIdSeed++

const createDefaultBusinessForm = () => ({
  organization: '',
  position: '',
  phone: '',
  wechat: '',
  mainCrops: [],
  managedArea: '',
  focusRegions: '',
  partners: '',
  reportFrequency: '',
  preferredContact: ''
})

const createDefaultCustomFields = () => [
  { id: nextCustomFieldId(), label: '对接领导', value: '张主任' }
]

const createDefaultDevices = () => [
  {
    id: 1,
    name: '台式机',
    browser: 'Chrome 121',
    system: 'Windows 11',
    location: '浙江 杭州',
    lastActive: '2024-03-20 09:12',
    trusted: true
  },
  {
    id: 2,
    name: '政务工作机',
    browser: 'Edge 119',
    system: 'Windows 10',
    location: '浙江 宁波',
    lastActive: '2024-03-18 18:35',
    trusted: false
  },
  {
    id: 3,
    name: '移动端',
    browser: 'Safari',
    system: 'iOS 17',
    location: '浙江 温州',
    lastActive: '2024-03-17 07:58',
    trusted: true
  }
]

const createDefaultPersonalization = () => ({
  theme: 'system',
  accentColor: '#2563eb',
  density: 'comfortable',
  digestFrequency: 'daily',
  quietMode: true,
  notifications: {
    email: true,
    sms: false,
    inApp: true,
    security: true
  }
})

const createDefaultCleanupSelection = () => ['sessions']

const createDefaultCleanupHistory = () => [
  {
    id: nextCleanupHistoryId(),
    time: '2024-03-15 10:30',
    description: '清理历史会话 4 条',
    type: 'primary'
  },
  {
    id: nextCleanupHistoryId(),
    time: '2024-03-08 09:12',
    description: '删除导出文件 2 个',
    type: 'success'
  }
]

const createDefaultExportForm = () => ({
  scope: 'profile',
  format: 'xlsx',
  range: '',
  includeAudit: true
})

const businessFieldGroups = [
  {
    id: 'basic',
    title: '基本信息',
    description: '描述当前的部门归属与岗位角色。',
    fields: [
      {
        key: 'organization',
        label: '所属部门/单位',
        type: 'select',
        props: { placeholder: '请选择所属部门/单位' },
        options: [
          { label: '农业农村局耕保科', value: 'agri-protection' },
          { label: '农业农村局种植业管理科', value: 'planting' },
          { label: '农业农村局执法大队', value: 'law-enforcement' }
        ],
        rules: [{ required: true, message: '请选择所属部门/单位', trigger: 'change' }]
      },
      {
        key: 'position',
        label: '岗位职能',
        type: 'select',
        props: { placeholder: '请选择岗位职能' },
        options: [
          { label: '业务负责人', value: 'owner' },
          { label: '数据专员', value: 'data-analyst' },
          { label: '技术联络人', value: 'it-contact' }
        ],
        rules: [{ required: true, message: '请选择岗位职能', trigger: 'change' }]
      },
      {
        key: 'phone',
        label: '联系电话',
        type: 'input',
        props: { placeholder: '请输入办公电话', maxlength: 20 },
        rules: [{ required: true, message: '请输入联系电话', trigger: 'blur' }]
      },
      {
        key: 'wechat',
        label: '工作微信',
        type: 'input',
        props: { placeholder: '请输入微信号' }
      }
    ]
  },
  {
    id: 'coverage',
    title: '业务覆盖范围',
    description: '填写负责的区域、作物类型等信息，便于系统个性化推荐。',
    fields: [
      {
        key: 'mainCrops',
        label: '主要作物',
        type: 'select',
        props: { multiple: true, placeholder: '请选择主要关注的作物' },
        options: [
          { label: '水稻', value: 'rice' },
          { label: '小麦', value: 'wheat' },
          { label: '油菜', value: 'rapeseed' },
          { label: '茶叶', value: 'tea' }
        ],
        rules: [{ required: true, message: '请选择至少一种主要作物', trigger: 'change' }]
      },
      {
        key: 'managedArea',
        label: '覆盖面积（万亩）',
        type: 'input',
        props: { placeholder: '请输入负责区域面积', type: 'number', min: 0 },
        rules: [{ required: true, message: '请输入覆盖面积', trigger: 'blur' }],
        hint: '请填写数字，可保留 1 位小数'
      },
      {
        key: 'focusRegions',
        label: '重点地区',
        type: 'textarea',
        fullWidth: true,
        props: {
          type: 'textarea',
          autosize: { minRows: 3, maxRows: 4 },
          placeholder: '例如：杭州市萧山区、绍兴市越城区'
        }
      }
    ]
  },
  {
    id: 'collaboration',
    title: '协同信息',
    description: '记录常用的合作对象、对接人以及工作节奏。',
    fields: [
      {
        key: 'partners',
        label: '合作单位',
        type: 'textarea',
        fullWidth: true,
        props: {
          type: 'textarea',
          autosize: { minRows: 2, maxRows: 4 },
          placeholder: '请输入常协作的企业或部门'
        }
      },
      {
        key: 'reportFrequency',
        label: '汇报频率',
        type: 'select',
        props: { placeholder: '请选择汇报频率' },
        options: [
          { label: '每周一次', value: 'weekly' },
          { label: '每月两次', value: 'biweekly' },
          { label: '按需汇报', value: 'on-demand' }
        ],
        rules: [{ required: true, message: '请选择汇报频率', trigger: 'change' }]
      },
      {
        key: 'preferredContact',
        label: '首选联系方式',
        type: 'select',
        props: { placeholder: '请选择联系方式' },
        options: [
          { label: '电话', value: 'phone' },
          { label: '微信', value: 'wechat' },
          { label: '邮件', value: 'email' }
        ],
        rules: [{ required: true, message: '请选择首选联系方式', trigger: 'change' }]
      }
    ]
  }
]

const businessForm = reactive(createDefaultBusinessForm())
const customFields = ref(createDefaultCustomFields())
const devices = ref(createDefaultDevices())
const personalizationForm = reactive(createDefaultPersonalization())
const cleanupSelection = ref(createDefaultCleanupSelection())
const cleanupHistory = ref(createDefaultCleanupHistory())
const cleanupOptions = [
  {
    value: 'sessions',
    title: '历史会话',
    description: '清理已失效的会话与登录令牌'
  },
  {
    value: 'notifications',
    title: '通知消息',
    description: '删除超过 90 天的站内通知记录'
  },
  {
    value: 'exports',
    title: '导出文件',
    description: '移除已下载的历史导出文件'
  },
  {
    value: 'cache',
    title: '临时缓存',
    description: '释放本地缓存与离线数据'
  }
]
const exportForm = reactive(createDefaultExportForm())
const lastPasswordChange = ref('')

const densityOptions = [
  { label: '宽松', value: 'comfortable' },
  { label: '适中', value: 'standard' },
  { label: '紧凑', value: 'compact' }
]

const notificationChannels = [
  {
    key: 'email',
    title: '邮件通知',
    description: '用于发送周报、导出结果及安全提醒'
  },
  {
    key: 'sms',
    title: '短信提醒',
    description: '异常登录、紧急预警短信即时推送'
  },
  {
    key: 'inApp',
    title: '站内通知',
    description: '平台任务、审批与动态实时提醒'
  },
  {
    key: 'security',
    title: '安全通知',
    description: '密码修改、权限调整等安全事件'
  }
]

const toLocaleStringSafe = value => {
  if (!value) {
    return ''
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleString()
}

const lastLoginDisplay = computed(() => {
  const candidate =
    authStore.user?.lastLoginAt ||
    authStore.user?.lastLoginTime ||
    authStore.user?.lastLogin ||
    authStore.user?.profile?.security?.lastLoginAt
  return candidate ? toLocaleStringSafe(candidate) : '暂无记录'
})

const lastPasswordChangeDisplay = computed(() =>
  lastPasswordChange.value ? toLocaleStringSafe(lastPasswordChange.value) : '尚未修改'
)

const completionRate = computed(() => {
  const entries = []
  businessFieldGroups.forEach(group => {
    group.fields.forEach(field => {
      entries.push(businessForm[field.key])
    })
  })
  customFields.value.forEach(field => {
    entries.push(field.label && field.value ? field.value : '')
  })
  ;['theme', 'accentColor', 'density', 'digestFrequency'].forEach(key => {
    entries.push(personalizationForm[key])
  })
  if (!entries.length) {
    return 0
  }
  const filled = entries.filter(value => {
    if (Array.isArray(value)) {
      return value.length > 0
    }
    if (typeof value === 'number') {
      return !Number.isNaN(value)
    }
    if (typeof value === 'string') {
      return value.trim().length > 0
    }
    if (value && typeof value === 'object') {
      return Object.keys(value).length > 0
    }
    return Boolean(value)
  }).length
  return Math.min(100, Math.round((filled / entries.length) * 100))
})

const resolveFieldComponent = field => {
  if (field.component) {
    return field.component
  }
  if (field.type === 'select') {
    return 'el-select'
  }
  if (field.type === 'textarea') {
    return 'el-input'
  }
  if (field.type === 'date' || field.type === 'daterange') {
    return 'el-date-picker'
  }
  return 'el-input'
}

const hydrateCustomFields = stored => {
  if (!Array.isArray(stored) || !stored.length) {
    customFieldIdSeed = 1
    customFields.value = createDefaultCustomFields()
    return
  }
  const normalized = stored.map((field, index) => ({
    id: field.id ?? index + 1,
    label: field.label || '',
    value: field.value || ''
  }))
  customFieldIdSeed = normalized.reduce((max, field) => Math.max(max, field.id), 0) + 1
  customFields.value = normalized
}

const hydrateCleanupHistory = stored => {
  if (!Array.isArray(stored) || !stored.length) {
    cleanupHistoryIdSeed = 1
    cleanupHistory.value = createDefaultCleanupHistory()
    return
  }
  const normalized = stored.map((record, index) => ({
    id: record.id ?? index + 1,
    time: record.time || '',
    description: record.description || '',
    type: record.type || 'primary'
  }))
  cleanupHistoryIdSeed = normalized.reduce((max, record) => Math.max(max, record.id), 0) + 1
  cleanupHistory.value = normalized
}

const resetStateToDefault = () => {
  devices.value = createDefaultDevices()
  Object.assign(businessForm, createDefaultBusinessForm())
  customFieldIdSeed = 1
  customFields.value = createDefaultCustomFields()
  Object.assign(personalizationForm, createDefaultPersonalization())
  cleanupSelection.value = createDefaultCleanupSelection()
  cleanupHistoryIdSeed = 1
  cleanupHistory.value = createDefaultCleanupHistory()
  Object.assign(exportForm, createDefaultExportForm())
  lastExportTime.value = ''
  lastPasswordChange.value = ''
}

const loadProfileFromStore = () => {
  if (!authStore.user?.profile) {
    resetStateToDefault()
    return
  }
  const profile = authStore.user.profile
  const security = profile.security || {}
  const business = profile.business || {}
  const personalization = profile.personalization || {}
  const dataOperations = profile.dataOperations || {}

  const storedDevices = Array.isArray(security.devices) ? security.devices : []
  devices.value = storedDevices.length
    ? storedDevices.map((device, index) => ({
        id: device.id ?? index + 1,
        name: device.name || '未命名设备',
        browser: device.browser || '',
        system: device.system || '',
        location: device.location || '',
        lastActive: device.lastActive ? toLocaleStringSafe(device.lastActive) : '',
        trusted: device.trusted !== undefined ? Boolean(device.trusted) : true
      }))
    : createDefaultDevices()

  Object.assign(businessForm, { ...createDefaultBusinessForm(), ...(business.form || {}) })
  hydrateCustomFields(business.customFields)
  Object.assign(personalizationForm, { ...createDefaultPersonalization(), ...personalization })

  cleanupSelection.value =
    Array.isArray(dataOperations.lastSelection) && dataOperations.lastSelection.length
      ? [...dataOperations.lastSelection]
      : createDefaultCleanupSelection()

  hydrateCleanupHistory(dataOperations.history)

  lastPasswordChange.value = security.lastPasswordChange || ''
  lastExportTime.value = dataOperations.lastExportTime || ''

  const storedExportForm = dataOperations.exportForm || {}
  const storedRange = Array.isArray(storedExportForm.range)
    ? storedExportForm.range.map(value => {
        const date = new Date(value)
        return Number.isNaN(date.getTime()) ? value : date
      })
    : ''
  Object.assign(exportForm, { ...createDefaultExportForm(), ...storedExportForm, range: storedRange })
}

const persistProfile = () => {
  if (!authStore.user) {
    return
  }
  const exportRangePayload = Array.isArray(exportForm.range)
    ? exportForm.range.map(value => (value instanceof Date ? value.toISOString() : value))
    : ''
  const profilePayload = {
    security: {
      devices: devices.value.map(device => ({ ...device })),
      lastPasswordChange: lastPasswordChange.value
    },
    business: {
      form: { ...businessForm },
      customFields: customFields.value.map(field => ({
        id: field.id,
        label: field.label,
        value: field.value
      }))
    },
    personalization: JSON.parse(JSON.stringify(personalizationForm)),
    dataOperations: {
      lastSelection: [...cleanupSelection.value],
      history: cleanupHistory.value.map(record => ({ ...record })),
      lastExportTime: lastExportTime.value,
      exportForm: {
        scope: exportForm.scope,
        format: exportForm.format,
        includeAudit: exportForm.includeAudit,
        range: exportRangePayload
      }
    }
  }
  authStore.user = {
    ...authStore.user,
    profile: profilePayload
  }
  if (typeof authStore.persistState === 'function') {
    authStore.persistState()
  }
}

watch(
  () => authStore.user?.profile,
  () => {
    loadProfileFromStore()
  },
  { immediate: true }
)

const toggleTrust = device => {
  device.trusted = !device.trusted
  persistProfile()
  ElMessage.success(`已${device.trusted ? '信任' : '取消信任'} ${device.name}`)
}

const markDevicesTrusted = () => {
  if (!devices.value.length) {
    ElMessage.info('暂无设备需要标记')
    return
  }
  let updated = 0
  devices.value.forEach(device => {
    if (!device.trusted) {
      device.trusted = true
      updated += 1
    }
  })
  persistProfile()
  ElMessage.success(updated ? '已将当前设备全部标记为信任' : '所有设备均已信任')
}

const revokeDevice = async device => {
  try {
    await ElMessageBox.confirm(`确认要注销 ${device.name} 的登录状态吗？`, '安全确认', {
      confirmButtonText: '确认注销',
      cancelButtonText: '取消',
      type: 'warning'
    })
    devices.value = devices.value.filter(item => item.id !== device.id)
    persistProfile()
    ElMessage.success('设备已注销')
  } catch (error) {
    // 用户取消操作，无需处理
  }
}

const addCustomField = () => {
  customFields.value.push({ id: nextCustomFieldId(), label: '', value: '' })
}

const removeCustomField = id => {
  if (customFields.value.length === 1) {
    return
  }
  customFields.value = customFields.value.filter(field => field.id !== id)
}

const saveBusinessInfo = async ({ silent = false } = {}) => {
  if (businessFormRef.value) {
    try {
      await businessFormRef.value.validate()
    } catch (error) {
      return false
    }
  }
  savingBusiness.value = true
  return new Promise(resolve => {
    setTimeout(() => {
      savingBusiness.value = false
      persistProfile()
      if (!silent) {
        ElMessage.success('业务资料已更新')
      }
      resolve(true)
    }, 600)
  })
}

const savePersonalization = async ({ silent = false } = {}) => {
  savingPersonalization.value = true
  return new Promise(resolve => {
    setTimeout(() => {
      savingPersonalization.value = false
      persistProfile()
      if (!silent) {
        ElMessage.success('个性化设置已保存')
      }
      resolve(true)
    }, 600)
  })
}

const submitPasswordForm = async ({ silent = false } = {}) => {
  if (!passwordFormRef.value) {
    return false
  }
  try {
    await passwordFormRef.value.validate()
  } catch (error) {
    return false
  }
  savingPassword.value = true
  return new Promise(resolve => {
    setTimeout(() => {
      savingPassword.value = false
      lastPasswordChange.value = new Date().toISOString()
      resetPasswordForm()
      persistProfile()
      if (!silent) {
        ElMessage.success('密码修改成功')
      }
      resolve(true)
    }, 800)
  })
}

const handleExportData = () => {
  exportingData.value = true
  setTimeout(() => {
    exportingData.value = false
    lastExportTime.value = new Date().toLocaleString()
    persistProfile()
    ElMessage.success('数据导出任务已创建，稍后将在通知中心推送下载链接')
  }, 800)
}

const handleCleanup = async () => {
  if (!cleanupSelection.value.length) {
    ElMessage.warning('请选择需要清理的数据类型')
    return
  }
  try {
    await ElMessageBox.confirm('确认立即清理所选数据吗？该操作不可撤销。', '数据清理确认', {
      confirmButtonText: '确认清理',
      cancelButtonText: '取消',
      type: 'warning'
    })
    cleaningData.value = true
    setTimeout(() => {
      cleaningData.value = false
      const description = `已清理 ${cleanupSelection.value.length} 类数据`
      cleanupHistory.value.unshift({
        id: nextCleanupHistoryId(),
        time: new Date().toLocaleString(),
        description,
        type: 'warning'
      })
      cleanupSelection.value = []
      persistProfile()
      ElMessage.success('所选数据已清理')
    }, 700)
  } catch (error) {
    // 用户取消操作，无需处理
  }
}

const resetAll = () => {
  resetPasswordForm()
  resetStateToDefault()
  persistProfile()
  ElMessage.success('已恢复默认设置')
}

const saveAll = async () => {
  if (savingAll.value) {
    return
  }
  savingAll.value = true
  try {
    const businessSaved = await saveBusinessInfo({ silent: true })
    if (!businessSaved) {
      savingAll.value = false
      return
    }
    const passwordFilled =
      passwordForm.currentPassword || passwordForm.newPassword || passwordForm.confirmPassword
    if (passwordFilled) {
      const passwordSaved = await submitPasswordForm({ silent: true })
      if (!passwordSaved) {
        savingAll.value = false
        return
      }
    }
    await savePersonalization({ silent: true })
    persistProfile()
    ElMessage.success('个人资料已全部保存')
  } finally {
    savingAll.value = false
  }
}
</script>


<style scoped>
.profile-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding: 24px 0 48px;
}

.header-metrics {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
}

.metric-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 120px;
}

.metric-value {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary, #111827);
}

.metric-label {
  font-size: 14px;
  color: var(--text-secondary, #667085);
}

.profile-grid {
  margin: 0;
}

.section-card {
  margin-bottom: 24px;
  border-radius: 16px;
  overflow: hidden;
}

.card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.card-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary, #111827);
}

.card-subtitle {
  margin-top: 4px;
  font-size: 14px;
  color: var(--text-secondary, #667085);
}

.card-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 0 0 8px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.section-header.compact {
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary, #111827);
}

.section-description {
  margin-top: 4px;
  font-size: 14px;
  color: var(--text-secondary, #667085);
}

.section-meta {
  margin-top: 6px;
  font-size: 13px;
  color: var(--text-tertiary, #98a2b3);
}

.section-form {
  width: 100%;
}

.business-form {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.business-field-grid {
  margin-top: 8px;
}

.field-hint {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-tertiary, #98a2b3);
}

.device-table {
  border-radius: 12px;
  overflow: hidden;
}

.device-name {
  font-weight: 600;
  color: var(--text-primary, #111827);
}

.device-meta {
  margin-top: 2px;
  font-size: 13px;
  color: var(--text-secondary, #667085);
}

.security-tip {
  border-radius: 12px;
}

.custom-field {
  display: grid;
  grid-template-columns: 1fr 1fr auto;
  gap: 12px;
  align-items: center;
}

.notification-item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 16px;
  border-radius: 12px;
  background: var(--surface-muted, #f2f4f7);
}

.notification-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.notification-title {
  font-weight: 600;
  color: var(--text-primary, #111827);
}

.notification-description {
  font-size: 13px;
  color: var(--text-secondary, #667085);
}

.export-tip {
  border-radius: 12px;
}

.cleanup-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.cleanup-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.cleanup-title {
  font-weight: 600;
  color: var(--text-primary, #111827);
}

.cleanup-description {
  font-size: 13px;
  color: var(--text-secondary, #667085);
}

.cleanup-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.cleanup-timeline {
  margin-top: 8px;
  padding-left: 4px;
}

.inline-icon {
  margin-right: 4px;
}

@media (max-width: 992px) {
  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .custom-field {
    grid-template-columns: 1fr;
  }

  .notification-item {
    flex-direction: column;
    align-items: flex-start;
  }

  .cleanup-actions {
    justify-content: flex-start;
  }
}
</style>
