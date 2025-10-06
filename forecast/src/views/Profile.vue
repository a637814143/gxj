<template>
  <div class="profile">
    <div class="page-header">
      <h2>个人资料</h2>
      <p>管理个人信息和账户设置</p>
    </div>
    
    <el-row :gutter="20">
      <!-- 个人信息 -->
      <el-col :xs="24" :lg="16">
        <div class="profile-card">
          <div class="card-header">
            <h3>个人信息</h3>
            <el-button type="primary" @click="editMode = !editMode">
              {{ editMode ? '取消编辑' : '编辑资料' }}
            </el-button>
          </div>
          
          <el-form :model="userForm" :rules="userRules" ref="userFormRef" label-width="100px">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="userForm.username" :disabled="!editMode" />
            </el-form-item>
            
            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model="userForm.realName" :disabled="!editMode" />
            </el-form-item>
            
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="userForm.email" :disabled="!editMode" />
            </el-form-item>
            
            <el-form-item label="手机号" prop="phoneNumber">
              <el-input v-model="userForm.phoneNumber" :disabled="!editMode" />
            </el-form-item>
            
            <el-form-item label="组织" prop="organization">
              <el-input v-model="userForm.organization" :disabled="!editMode" />
            </el-form-item>
            
            <el-form-item label="地区代码" prop="regionCode">
              <el-input v-model="userForm.regionCode" :disabled="!editMode" />
            </el-form-item>
            
            <el-form-item v-if="editMode">
              <el-button type="primary" @click="saveProfile" :loading="saving">保存修改</el-button>
              <el-button @click="resetForm">重置</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-col>
      
      <!-- 头像和角色信息 -->
      <el-col :xs="24" :lg="8">
        <div class="avatar-card">
          <div class="avatar-section">
            <div class="avatar-container">
              <el-avatar :size="120" :src="userForm.avatar">
                <el-icon><User /></el-icon>
              </el-avatar>
            </div>
            <div class="avatar-actions">
              <el-button size="small" @click="changeAvatar">更换头像</el-button>
            </div>
          </div>
          
          <div class="role-info">
            <h4>角色信息</h4>
            <div class="role-list">
              <el-tag
                v-for="role in userForm.roles"
                :key="role.id"
                :type="getRoleType(role.name)"
                size="large"
                style="margin: 4px;"
              >
                {{ role.displayName }}
              </el-tag>
            </div>
          </div>
          
          <div class="user-stats">
            <h4>账户统计</h4>
            <div class="stat-item">
              <span class="stat-label">注册时间：</span>
              <span class="stat-value">{{ userForm.createdAt }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">最后登录：</span>
              <span class="stat-value">{{ userForm.lastLoginAt }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">登录次数：</span>
              <span class="stat-value">{{ userForm.loginCount }} 次</span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>
    
    <!-- 密码修改 -->
    <div class="password-card">
      <div class="card-header">
        <h3>修改密码</h3>
      </div>
      
      <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="100px">
        <el-row :gutter="20">
          <el-col :xs="24" :sm="12">
            <el-form-item label="当前密码" prop="currentPassword">
              <el-input
                v-model="passwordForm.currentPassword"
                type="password"
                show-password
                placeholder="请输入当前密码"
              />
            </el-form-item>
          </el-col>
          
          <el-col :xs="24" :sm="12">
            <el-form-item label="新密码" prop="newPassword">
              <el-input
                v-model="passwordForm.newPassword"
                type="password"
                show-password
                placeholder="请输入新密码"
              />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            show-password
            placeholder="请再次输入新密码"
            style="width: 300px;"
          />
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="changePassword" :loading="changingPassword">修改密码</el-button>
          <el-button @click="resetPasswordForm">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
    
    <!-- 操作日志 -->
    <div class="activity-card">
      <div class="card-header">
        <h3>最近活动</h3>
        <el-button size="small" @click="refreshActivity">刷新</el-button>
      </div>
      
      <el-timeline>
        <el-timeline-item
          v-for="activity in activities"
          :key="activity.id"
          :timestamp="activity.timestamp"
          :type="getActivityType(activity.type)"
        >
          <div class="activity-content">
            <h4>{{ activity.title }}</h4>
            <p>{{ activity.description }}</p>
            <span class="activity-ip">IP: {{ activity.ip }}</span>
          </div>
        </el-timeline-item>
      </el-timeline>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

export default {
  name: 'UserProfile',
  setup() {
    const editMode = ref(false)
    const saving = ref(false)
    const changingPassword = ref(false)
    const userFormRef = ref()
    const passwordFormRef = ref()
    
    const userForm = reactive({
      id: 1,
      username: 'admin',
      realName: '管理员',
      email: 'admin@example.com',
      phoneNumber: '13800138000',
      organization: '农业部门',
      regionCode: 'BJ',
      avatar: '',
      roles: [
        { id: 1, name: 'ADMIN', displayName: '管理员' }
      ],
      createdAt: '2024-01-01 10:00:00',
      lastLoginAt: '2024-01-15 09:30:00',
      loginCount: 156
    })
    
    const passwordForm = reactive({
      currentPassword: '',
      newPassword: '',
      confirmPassword: ''
    })
    
    const userRules = {
      username: [
        { required: true, message: '请输入用户名', trigger: 'blur' }
      ],
      realName: [
        { required: true, message: '请输入真实姓名', trigger: 'blur' }
      ],
      email: [
        { required: true, message: '请输入邮箱', trigger: 'blur' },
        { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
      ],
      phoneNumber: [
        { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
      ],
      organization: [
        { required: true, message: '请输入组织', trigger: 'blur' }
      ]
    }
    
    const passwordRules = {
      currentPassword: [
        { required: true, message: '请输入当前密码', trigger: 'blur' }
      ],
      newPassword: [
        { required: true, message: '请输入新密码', trigger: 'blur' },
        { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
      ],
      confirmPassword: [
        { required: true, message: '请确认新密码', trigger: 'blur' },
        {
          validator: (rule, value, callback) => {
            if (value !== passwordForm.newPassword) {
              callback(new Error('两次输入的密码不一致'))
            } else {
              callback()
            }
          },
          trigger: 'blur'
        }
      ]
    }
    
    const activities = ref([
      {
        id: 1,
        type: 'login',
        title: '用户登录',
        description: '成功登录系统',
        timestamp: '2024-01-15 09:30:00',
        ip: '192.168.1.100'
      },
      {
        id: 2,
        type: 'data',
        title: '数据操作',
        description: '导入了作物产量数据',
        timestamp: '2024-01-15 08:45:00',
        ip: '192.168.1.100'
      },
      {
        id: 3,
        type: 'prediction',
        title: '预测分析',
        description: '运行了产量预测模型',
        timestamp: '2024-01-14 16:20:00',
        ip: '192.168.1.100'
      }
    ])
    
    const getRoleType = (roleName) => {
      const typeMap = {
        'ADMIN': 'danger',
        'RESEARCHER': 'success',
        'FARMER': 'warning'
      }
      return typeMap[roleName] || 'info'
    }
    
    const getActivityType = (type) => {
      const typeMap = {
        'login': 'primary',
        'data': 'success',
        'prediction': 'warning',
        'system': 'danger'
      }
      return typeMap[type] || 'info'
    }
    
    const saveProfile = () => {
      userFormRef.value.validate((valid) => {
        if (valid) {
          saving.value = true
          // 模拟保存
          setTimeout(() => {
            saving.value = false
            editMode.value = false
            ElMessage.success('个人信息保存成功')
          }, 1000)
        }
      })
    }
    
    const resetForm = () => {
      userFormRef.value?.resetFields()
    }
    
    const changePassword = () => {
      passwordFormRef.value.validate((valid) => {
        if (valid) {
          changingPassword.value = true
          // 模拟密码修改
          setTimeout(() => {
            changingPassword.value = false
            resetPasswordForm()
            ElMessage.success('密码修改成功')
          }, 1000)
        }
      })
    }
    
    const resetPasswordForm = () => {
      passwordFormRef.value?.resetFields()
    }
    
    const changeAvatar = () => {
      ElMessage.success('头像更换功能开发中...')
    }
    
    const refreshActivity = () => {
      ElMessage.success('活动记录已刷新')
    }
    
    onMounted(() => {
      // 加载用户信息
    })
    
    return {
      editMode,
      saving,
      changingPassword,
      userFormRef,
      passwordFormRef,
      userForm,
      passwordForm,
      userRules,
      passwordRules,
      activities,
      getRoleType,
      getActivityType,
      saveProfile,
      resetForm,
      changePassword,
      resetPasswordForm,
      changeAvatar,
      refreshActivity
    }
  }
}
</script>

<style scoped>
.profile {
  padding: 0;
}

.page-header {
  margin-bottom: 30px;
}

.page-header h2 {
  color: #333;
  margin-bottom: 8px;
}

.page-header p {
  color: #666;
  font-size: 14px;
}

.profile-card,
.avatar-card,
.password-card,
.activity-card {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e9ecef;
}

.card-header h3 {
  color: #333;
  margin: 0;
}

.avatar-section {
  text-align: center;
  margin-bottom: 24px;
}

.avatar-container {
  margin-bottom: 16px;
}

.role-info {
  margin-bottom: 24px;
}

.role-info h4 {
  color: #333;
  margin-bottom: 12px;
  font-size: 14px;
}

.role-list {
  text-align: center;
}

.user-stats {
  margin-bottom: 24px;
}

.user-stats h4 {
  color: #333;
  margin-bottom: 12px;
  font-size: 14px;
}

.stat-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 14px;
}

.stat-label {
  color: #666;
}

.stat-value {
  color: #333;
  font-weight: 500;
}

.activity-content h4 {
  color: #333;
  margin-bottom: 4px;
  font-size: 14px;
}

.activity-content p {
  color: #666;
  margin-bottom: 4px;
  font-size: 13px;
}

.activity-ip {
  color: #999;
  font-size: 12px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .avatar-section {
    margin-bottom: 20px;
  }
  
  .stat-item {
    flex-direction: column;
    gap: 4px;
  }
}
</style>
