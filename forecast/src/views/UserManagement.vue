<template>
  <div class="user-management">
    <!-- 搜索表单 -->
    <div class="search-form">
      <el-form :model="searchForm" inline>
        <el-form-item label="用户名">
          <el-input
            v-model="searchForm.username"
            placeholder="请输入用户名"
            clearable
          />
        </el-form-item>
        <el-form-item label="组织">
          <el-input
            v-model="searchForm.organization"
            placeholder="请输入组织"
            clearable
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="searchForm.status"
            placeholder="请选择状态"
            clearable
          >
            <el-option label="全部" value="" />
            <el-option label="活跃" value="ACTIVE" />
            <el-option label="非活跃" value="INACTIVE" />
            <el-option label="锁定" value="LOCKED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
    
    <!-- 操作按钮 -->
    <div class="button-group">
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        新增用户
      </el-button>
      <el-button type="danger" @click="handleBatchDelete" :disabled="!selectedUsers.length">
        <el-icon><Delete /></el-icon>
        批量删除
      </el-button>
    </div>
    
    <!-- 用户表格 -->
    <div class="table-container">
      <el-table
        :data="userList"
        :loading="loading"
        @selection-change="handleSelectionChange"
        stripe
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="realName" label="真实姓名" width="120" />
        <el-table-column prop="email" label="邮箱" width="200" />
        <el-table-column prop="organization" label="组织" width="150" />
        <el-table-column prop="regionCode" label="地区代码" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="roles" label="角色" width="150">
          <template #default="scope">
            <el-tag
              v-for="role in scope.row.roles"
              :key="role.id"
              size="small"
              style="margin-right: 5px;"
            >
              {{ role.displayName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <el-button size="small" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button size="small" type="warning" @click="handleResetPassword">重置密码</el-button>
            <el-button size="small" type="danger" @click="handleDelete">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.currentPage"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>
    
    <!-- 用户表单对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      @close="handleDialogClose"
    >
      <el-form
        ref="userFormRef"
        :model="userForm"
        :rules="userRules"
        label-width="100px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="userForm.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号" prop="phoneNumber">
          <el-input v-model="userForm.phoneNumber" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="组织" prop="organization">
          <el-input v-model="userForm.organization" placeholder="请输入组织" />
        </el-form-item>
        <el-form-item label="地区代码" prop="regionCode">
          <el-input v-model="userForm.regionCode" placeholder="请输入地区代码" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="userForm.status" placeholder="请选择状态">
            <el-option label="活跃" value="ACTIVE" />
            <el-option label="非活跃" value="INACTIVE" />
            <el-option label="锁定" value="LOCKED" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色" prop="roles">
          <el-select
            v-model="userForm.roleIds"
            multiple
            placeholder="请选择角色"
            style="width: 100%"
          >
            <el-option
              v-for="role in roleList"
              :key="role.id"
              :label="role.displayName"
              :value="role.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!userForm.id" label="密码" prop="password">
          <el-input
            v-model="userForm.password"
            type="password"
            placeholder="请输入密码"
            show-password
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

export default {
  name: 'UserManagement',
  setup() {
    const loading = ref(false)
    const submitting = ref(false)
    const dialogVisible = ref(false)
    const dialogTitle = ref('')
    const userFormRef = ref()
    const selectedUsers = ref([])
    
    const searchForm = reactive({
      username: '',
      organization: '',
      status: ''
    })
    
    const userForm = reactive({
      id: null,
      username: '',
      realName: '',
      email: '',
      phoneNumber: '',
      organization: '',
      regionCode: '',
      status: 'ACTIVE',
      roleIds: []
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
      organization: [
        { required: true, message: '请输入组织', trigger: 'blur' }
      ],
      status: [
        { required: true, message: '请选择状态', trigger: 'change' }
      ],
      roleIds: [
        { required: true, message: '请选择角色', trigger: 'change' }
      ],
      password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
      ]
    }
    
    const userList = ref([])
    const roleList = ref([])
    
    const pagination = reactive({
      currentPage: 1,
      pageSize: 10,
      total: 0
    })
    
    const getStatusType = (status) => {
      const statusMap = {
        'ACTIVE': 'success',
        'INACTIVE': 'info',
        'LOCKED': 'danger'
      }
      return statusMap[status] || 'info'
    }
    
    const getStatusText = (status) => {
      const statusMap = {
        'ACTIVE': '活跃',
        'INACTIVE': '非活跃',
        'LOCKED': '锁定'
      }
      return statusMap[status] || '未知'
    }
    
    const loadUserList = async () => {
      loading.value = true
      try {
        // 模拟API调用
        await new Promise(resolve => setTimeout(resolve, 1000))
        
        // 模拟数据
        userList.value = [
          {
            id: 1,
            username: 'admin',
            realName: '管理员',
            email: 'admin@example.com',
            phoneNumber: '13800138000',
            organization: '农业部门',
            regionCode: 'BJ',
            status: 'ACTIVE',
            roles: [
              { id: 1, name: 'ADMIN', displayName: '管理员' }
            ],
            createdAt: '2024-01-01 10:00:00'
          },
          {
            id: 2,
            username: 'researcher1',
            realName: '研究员1',
            email: 'researcher1@example.com',
            phoneNumber: '13800138001',
            organization: '农业科研所',
            regionCode: 'SH',
            status: 'ACTIVE',
            roles: [
              { id: 2, name: 'RESEARCHER', displayName: '研究员' }
            ],
            createdAt: '2024-01-02 10:00:00'
          }
        ]
        
        pagination.total = userList.value.length
      } catch (error) {
        ElMessage.error('加载用户列表失败')
      } finally {
        loading.value = false
      }
    }
    
    const loadRoleList = async () => {
      try {
        // 模拟角色数据
        roleList.value = [
          { id: 1, name: 'ADMIN', displayName: '管理员' },
          { id: 2, name: 'RESEARCHER', displayName: '研究员' },
          { id: 3, name: 'FARMER', displayName: '农户' }
        ]
      } catch (error) {
        ElMessage.error('加载角色列表失败')
      }
    }
    
    const handleSearch = () => {
      pagination.currentPage = 1
      loadUserList()
    }
    
    const handleReset = () => {
      Object.keys(searchForm).forEach(key => {
        searchForm[key] = ''
      })
      handleSearch()
    }
    
    const handleAdd = () => {
      dialogTitle.value = '新增用户'
      Object.keys(userForm).forEach(key => {
        if (key === 'status') {
          userForm[key] = 'ACTIVE'
        } else if (key === 'roleIds') {
          userForm[key] = []
        } else {
          userForm[key] = ''
        }
      })
      userForm.id = null
      dialogVisible.value = true
    }
    
    const handleEdit = (row) => {
      dialogTitle.value = '编辑用户'
      Object.keys(userForm).forEach(key => {
        if (key === 'roleIds') {
          userForm[key] = row.roles.map(role => role.id)
        } else {
          userForm[key] = row[key]
        }
      })
      dialogVisible.value = true
    }
    
    const handleDelete = () => {
      ElMessageBox.confirm('确定要删除该用户吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        // 模拟删除操作
        ElMessage.success('删除成功')
        loadUserList()
      })
    }
    
    const handleBatchDelete = () => {
      ElMessageBox.confirm('确定要删除选中的用户吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        // 模拟批量删除操作
        ElMessage.success('批量删除成功')
        loadUserList()
      })
    }
    
    const handleResetPassword = () => {
      ElMessageBox.confirm('确定要重置该用户的密码吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        // 模拟重置密码操作
        ElMessage.success('密码重置成功')
      })
    }
    
    const handleSubmit = () => {
      userFormRef.value.validate((valid) => {
        if (valid) {
          submitting.value = true
          // 模拟提交操作
          setTimeout(() => {
            submitting.value = false
            dialogVisible.value = false
            ElMessage.success(userForm.id ? '更新成功' : '创建成功')
            loadUserList()
          }, 1000)
        }
      })
    }
    
    const handleDialogClose = () => {
      userFormRef.value?.resetFields()
    }
    
    const handleSelectionChange = (selection) => {
      selectedUsers.value = selection
    }
    
    const handleSizeChange = (size) => {
      pagination.pageSize = size
      loadUserList()
    }
    
    const handleCurrentChange = (page) => {
      pagination.currentPage = page
      loadUserList()
    }
    
    onMounted(() => {
      loadUserList()
      loadRoleList()
    })
    
    return {
      loading,
      submitting,
      dialogVisible,
      dialogTitle,
      userFormRef,
      selectedUsers,
      searchForm,
      userForm,
      userRules,
      userList,
      roleList,
      pagination,
      getStatusType,
      getStatusText,
      handleSearch,
      handleReset,
      handleAdd,
      handleEdit,
      handleDelete,
      handleBatchDelete,
      handleResetPassword,
      handleSubmit,
      handleDialogClose,
      handleSelectionChange,
      handleSizeChange,
      handleCurrentChange
    }
  }
}
</script>

<style scoped>
.user-management {
  padding: 0;
}

.search-form {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.button-group {
  margin-bottom: 20px;
}

.table-container {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.pagination-container {
  margin-top: 20px;
  text-align: right;
}
</style>
