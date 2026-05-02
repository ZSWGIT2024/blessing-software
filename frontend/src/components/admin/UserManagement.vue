<template>
  <div class="user-management">
    <!-- 搜索和筛选区域 -->
    <div class="filter-bar">
      <div class="search-section">
        <input 
          type="text" 
          placeholder="搜索用户名、用户昵称或手机号和邮箱..." 
          v-model="searchParams.keyword"
          @keyup.enter="loadUsers"
        >
        <button @click="loadUsers" :disabled="loading">搜索</button>
        <button @click="resetFilters" class="secondary">重置</button>
      </div>
      
      <!-- 高级筛选 -->
      <div class="advanced-filters" v-if="showAdvancedFilters">
        <div class="filter-row">
          <div class="filter-group">
            <label>状态：</label>
            <select v-model="searchParams.status">
              <option value="">全部</option>
              <option value="active">正常</option>
              <option value="banned">封禁</option>
            </select>
          </div>
          
          <div class="filter-group">
            <label>VIP类型：</label>
            <select v-model="searchParams.vipType">
              <option value="">全部</option>
              <option value="0">非VIP</option>
              <option value="1">月度VIP</option>
              <option value="2">季度VIP</option>
              <option value="3">年度VIP</option>
              <option value="4">终身VIP</option>
            </select>
          </div>
          
          <div class="filter-group">
            <label>等级范围：</label>
            <input type="number" v-model="searchParams.minLevel" placeholder="最小" min="1">
            <span> - </span>
            <input type="number" v-model="searchParams.maxLevel" placeholder="最大">
          </div>
        </div>
      </div>
      
      <button @click="showAdvancedFilters = !showAdvancedFilters" class="toggle-filters">
        {{ showAdvancedFilters ? '收起筛选' : '高级筛选' }}
      </button>
    </div>
    
    <!-- 用户表格 -->
    <div class="table-container">
      <table class="user-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>用户名</th>
            <th>手机号</th>
            <th>VIP类型</th>
            <th>等级</th>
            <th>积分</th>
            <th>状态</th>
            <th>注册时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in userList" :key="user.id">
            <td>{{ user.id }}</td>
            <td>{{ user.username }}</td>
            <td>{{ user.phone }}</td>
            <td>
              <span :class="`vip-badge vip-${user.vipType}`">
                {{ getVipTypeName(user.vipType) }}
              </span>
              <span v-if="user.vipExpireTime" class="expire-time">
                {{ formatDate(user.vipExpireTime) }}
              </span>
            </td>
            <td>
              <span class="level-badge">Lv.{{ user.level }}</span>
              <span class="exp-info">{{ user.exp }}/{{ user.nextLevelExp }}</span>
            </td>
            <td>{{ user.coinBalance }}</td>
            <td>
              <span :class="`status-badge status-${user.status}`">
                {{ getStatusName(user.status) }}
              </span>
            </td>
            <td>{{ formatDate(user.createTime) }}</td>
            <td>
              <div class="action-buttons">
                <button @click="viewDetails(user.id)" class="btn-detail">详情</button>
                <button 
                  @click="toggleBan(user)" 
                  :class="['btn-status', user.status === 'banned' ? 'btn-unban' : 'btn-ban']"
                >
                  {{ user.status === 'banned' ? '解封' : '封禁' }}
                </button>
                <div class="dropdown">
                  <button class="btn-more">更多</button>
                  <div class="dropdown-content">
                    <a @click="editVip(user)">修改VIP</a>
                    <a @click="adjustCoins(user)">调整积分</a>
                    <a @click="editLevel(user)">修改等级</a>
                  </div>
                </div>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      
      <!-- 加载状态 -->
      <div v-if="loading" class="loading-overlay">
        <div class="spinner"></div>
        <p>加载中...</p>
      </div>
      
      <!-- 空状态 -->
      <div v-if="!loading && userList.length === 0" class="empty-state">
        <p>暂无用户数据</p>
      </div>
    </div>
    
    <!-- 分页组件 -->
    <div class="pagination" v-if="pagination.total > 0">
      <button 
        :disabled="pagination.page === 1" 
        @click="changePage(pagination.page - 1)"
      >
        上一页
      </button>
      
      <span class="page-info">
        第 {{ pagination.page }} 页 / 共 {{ pagination.totalPages }} 页
        ({{ pagination.total }} 条记录)
      </span>
      
      <button 
        :disabled="pagination.page >= pagination.totalPages" 
        @click="changePage(pagination.page + 1)"
      >
        下一页
      </button>
      
      <div class="page-size">
        <label>每页显示：</label>
        <select v-model="pageSize" @change="changePageSize">
          <option value="10">10</option>
          <option value="20">20</option>
          <option value="50">50</option>
          <option value="100">100</option>
        </select>
      </div>
    </div>
    
    <!-- 用户详情模态框 -->
    <div v-if="showDetailModal" class="modal-overlay">
      <div class="modal-content">
        <div class="modal-header">
          <h3>用户详情</h3>
          <button @click="closeDetailModal" class="close-btn">×</button>
        </div>
        <div class="modal-body" v-if="currentUser">
          <div class="user-info-grid">
            <div class="info-item">
              <label>用户昵称：</label>
              <span>{{ currentUser.nickname }}</span>
            </div>
            <div class="info-item">
              <label>上传作品数：</label>
              <span>{{ currentUser.uploadCount }}</span>
            </div>
            <div class="info-item">
              <label>获得点赞数：</label>
              <span>{{ currentUser.likedCount }}</span>
            </div>
            <div class="info-item">
              <label>邮箱：</label>
              <span>{{ currentUser.email || '未绑定' }}</span>
            </div>
            <div class="info-item">
              <label>粉丝数：</label>
              <span :class="`vip-status vip-${currentUser.vipType}`">
                {{ currentUser.followerCount }}
              </span>
            </div>
            <div class="info-item">
              <label>最后登录时间：</label>
              <span>{{ currentUser.lastLoginTime ? formatDate(currentUser.lastLoginTime) : '从未登录' }}</span>
            </div>
            <div class="info-item">
              <label>登录总天数：</label>
              <span>{{ currentUser.totalLoginDays }}</span>
            </div>
            <div class="info-item">
              <label>注册地：</label>
              <span :class="`status-badge status-${currentUser.status}`">
                {{ currentUser.registerLocation || '未绑定' }}
              </span>
            </div>
            <div class="info-item">
              <label>最后登录地：</label>
              <span>{{ currentUser.lastLoginLocation || '未登录' }}</span>
            </div>
            <div class="info-item">
              <label>注册源：</label>
              <span>{{ currentUser.registerSource || '未绑定' }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- VIP设置模态框 -->
    <div v-if="showVipModal" class="modal-overlay">
      <div class="modal-content">
        <div class="modal-header">
          <h3>设置VIP - {{ vipUser?.username }}</h3>
          <button @click="closeVipModal" class="close-btn">×</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>VIP类型：</label>
            <select v-model="vipForm.vipType" @change="onVipTypeChange">
              <option value="0">非VIP</option>
              <option value="1">月度VIP</option>
              <option value="2">季度VIP</option>
              <option value="3">年度VIP</option>
              <option value="4">终身VIP</option>
            </select>
          </div>
          
          <div class="form-group" v-if="vipForm.vipType > 0 && vipForm.vipType < 4">
            <label>有效期（天）：</label>
            <input 
              type="number" 
              v-model="vipForm.days" 
              min="1" 
              max="3650"
              placeholder="留空使用默认时长"
            >
            <small>提示：月度VIP默认30天，季度VIP90天，年度VIP365天</small>
          </div>
          
          <div class="modal-footer">
            <button @click="saveVip" :disabled="saving">确认设置</button>
            <button @click="closeVipModal" class="secondary">取消</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { 
  getUserListService, 
  getUserDetailService, 
  updateUserStatusService,
  updateUserVipService,
  updateUserCoinsService,
  updateUserLevelService
} from '@/api/adminUser'

// 响应式数据
const userList = ref([]) // 用户列表数据
const loading = ref(false) // 加载状态
const showAdvancedFilters = ref(false) // 是否显示高级筛选
const showDetailModal = ref(false) // 详情模态框显示状态
const showVipModal = ref(false) // VIP设置模态框显示状态
const currentUser = ref(null) // 当前查看的用户详情
const vipUser = ref(null) // 正在设置VIP的用户
const saving = ref(false) // 保存状态

// 分页和筛选参数
const searchParams = reactive({
  keyword: '',
  status: '',
  vipType: '',
  minLevel: '',
  maxLevel: ''
})

const pagination = reactive({
  page: 1,
  size: 20,
  total: 0,
  totalPages: 0
})

const pageSize = ref(20)

// 初始化VIP表单
const vipForm = reactive({
  vipType: 0,
  days: null
})

// 页面加载时获取用户列表
onMounted(() => {
  loadUsers()
})

/**
 * 加载用户列表数据
 */
const loadUsers = async () => {
  try {
    loading.value = true
    const params = {
      ...searchParams,
      page: pagination.page,
      size: pageSize.value
    }
    
    // 移除空值参数
    Object.keys(params).forEach(key => {
      if (params[key] === '' || params[key] === null) {
        delete params[key]
      }
    })
    
    const response = await getUserListService(params)
    if (response.code === 0) {
      userList.value = response.data.users || []
      pagination.total = response.data.total || 0
      pagination.totalPages = Math.ceil(pagination.total / pageSize.value)
    } else {
      console.error('加载用户列表失败:', response.message)
    }
  } catch (error) {
    console.error('加载用户列表异常:', error)
  } finally {
    loading.value = false
  }
}

/**
 * 重置筛选条件
 */
const resetFilters = () => {
  searchParams.keyword = ''
  searchParams.status = ''
  searchParams.vipType = ''
  searchParams.minLevel = ''
  searchParams.maxLevel = ''
  pagination.page = 1
  loadUsers()
}

/**
 * 切换分页
 * @param {number} page - 目标页码
 */
const changePage = (page) => {
  if (page < 1 || page > pagination.totalPages) return
  pagination.page = page
  loadUsers()
}

/**
 * 修改每页显示数量
 */
const changePageSize = () => {
  pagination.page = 1
  pagination.size = pageSize.value
  loadUsers()
}

/**
 * 查看用户详情
 * @param {number} userId - 用户ID
 */
const viewDetails = async (userId) => {
  try {
    const response = await getUserDetailService(userId)
    if (response.code === 0) {
      currentUser.value = response.data
      showDetailModal.value = true
    }
  } catch (error) {
    console.error('获取用户详情失败:', error)
  }
}

/**
 * 封禁/解封用户
 * @param {Object} user - 用户对象
 */
const toggleBan = async (user) => {
  if (!confirm(`确定要${user.status === 'banned' ? '解封' : '封禁'}用户 ${user.username} 吗？`)) {
    return
  }
  
  try {
    const newStatus = user.status === 'banned' ? 'active' : 'banned'
    const response = await updateUserStatusService(user.id, newStatus)
    
    if (response.code === 0) {
      // 更新本地数据
      user.status = newStatus
      alert('操作成功')
    } else {
      alert(`操作失败: ${response.message}`)
    }
  } catch (error) {
    console.error('更新用户状态失败:', error)
    alert('操作失败，请重试')
  }
}

/**
 * 打开VIP设置模态框
 * @param {Object} user - 用户对象
 */
const editVip = (user) => {
  vipUser.value = user
  vipForm.vipType = user.vipType
  vipForm.days = null
  showVipModal.value = true
}

/**
 * VIP类型变更处理
 */
const onVipTypeChange = () => {
  if (vipForm.vipType == 0 || vipForm.vipType == 4) {
    vipForm.days = null
  }
}

/**
 * 保存VIP设置
 */
const saveVip = async () => {
  try {
    saving.value = true
    const response = await updateUserVipService(
      vipUser.value.id, 
      vipForm.vipType, 
      vipForm.days
    )
    
    if (response.code === 0) {
      // 更新本地数据
      const user = userList.value.find(u => u.id === vipUser.value.id)
      if (user) {
        user.vipType = vipForm.vipType
      }
      closeVipModal()
      alert(response.message || 'VIP设置成功')
    } else {
      alert(`设置失败: ${response.message}`)
    }
  } catch (error) {
    console.error('设置VIP失败:', error)
    alert('设置失败，请重试')
  } finally {
    saving.value = false
  }
}

/**
 * 调整用户积分
 * @param {Object} user - 用户对象
 */
const adjustCoins = async (user) => {
  const amount = prompt(`请输入要调整的积分数量（当前: ${user.coinBalance}）`, '0')
  if (amount === null) return
  
  const numAmount = parseInt(amount)
  if (isNaN(numAmount)) {
    alert('请输入有效的数字')
    return
  }
  
  const reason = prompt('请输入调整原因：', '管理员调整')
  if (reason === null) return
  
  try {
    const response = await updateUserCoinsService(user.id, numAmount, reason)
    if (response.code === 0) {
      user.coinBalance += numAmount
      alert(response.message || '积分调整成功')
    } else {
      alert(`调整失败: ${response.message}`)
    }
  } catch (error) {
    console.error('调整积分失败:', error)
    alert('操作失败，请重试')
  }
}

/**
 * 修改用户等级
 * @param {Object} user - 用户对象
 */
const editLevel = async (user) => {
  const level = prompt(`请输入新的等级（当前: Lv.${user.level}）`, user.level)
  if (level === null) return
  
  const numLevel = parseInt(level)
  if (isNaN(numLevel) || numLevel < 1) {
    alert('请输入有效的等级')
    return
  }
  
  const exp = prompt(`请输入经验值（当前: ${user.exp}）`, user.exp)
  if (exp === null) return
  
  const numExp = parseInt(exp)
  if (isNaN(numExp) || numExp < 0) {
    alert('请输入有效的经验值')
    return
  }
  
  try {
    const response = await updateUserLevelService(user.id, numLevel, numExp)
    if (response.code === 0) {
      user.level = numLevel
      user.exp = numExp
      alert(response.message || '等级修改成功')
    } else {
      alert(`修改失败: ${response.message}`)
    }
  } catch (error) {
    console.error('修改等级失败:', error)
    alert('操作失败，请重试')
  }
}

/**
 * 关闭详情模态框
 */
const closeDetailModal = () => {
  showDetailModal.value = false
  currentUser.value = null
}

/**
 * 关闭VIP模态框
 */
const closeVipModal = () => {
  showVipModal.value = false
  vipUser.value = null
  vipForm.vipType = 0
  vipForm.days = null
}

/**
 * 获取VIP类型名称
 * @param {number} vipType - VIP类型代码
 * @returns {string} VIP类型名称
 */
const getVipTypeName = (vipType) => {
  const names = {
    0: '非VIP',
    1: '月度VIP',
    2: '季度VIP',
    3: '年度VIP',
    4: '终身VIP'
  }
  return names[vipType] || '未知'
}

/**
 * 获取状态名称
 * @param {string} status - 状态代码
 * @returns {string} 状态名称
 */
const getStatusName = (status) => {
  const names = {
    active: '正常',
    banned: '封禁'
  }
  return names[status] || '未知'
}

/**
 * 格式化日期时间
 * @param {string|Date} date - 日期
 * @returns {string} 格式化后的日期
 */
const formatDate = (date) => {
  if (!date) return '--'
  const d = new Date(date)
  return d.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<style scoped>
.user-management {
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;
}

/* 筛选栏样式 */
.filter-bar {
  background: white;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.search-section {
  display: flex;
  gap: 10px;
  margin-bottom: 15px;
}

.search-section input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.search-section button {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.search-section button:first-of-type {
  background: #1890ff;
  color: white;
}

.search-section button.secondary {
  background: #f5f5f5;
  color: #666;
}

/* 高级筛选 */
.advanced-filters {
  padding: 15px;
  background: #fafafa;
  border-radius: 4px;
  margin-bottom: 10px;
}

.filter-row {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-group label {
  font-size: 14px;
  color: #666;
  white-space: nowrap;
}

.filter-group select,
.filter-group input {
  padding: 6px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.filter-group input {
  width: 80px;
}

.toggle-filters {
  background: none;
  border: none;
  color: #1890ff;
  cursor: pointer;
  font-size: 14px;
  padding: 5px 0;
}

/* 操作栏 */
.action-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.action-bar button {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}



/* 表格样式 */
.table-container {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  position: relative;
  min-height: 300px;
}

.user-table {
  width: 100%;
  border-collapse: collapse;
}

.user-table th,
.user-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #eee;
  font-size: 14px;
}

.user-table th {
  background: #fafafa;
  font-weight: 600;
  color: #666;
}

.user-table tbody tr:hover {
  background: #f5f7fa;
}

/* 徽章样式 */
.vip-badge {
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 12px;
  margin-right: 5px;
}

.vip-0 { background: #f5f5f5; color: #666; }
.vip-1 { background: #e6f7ff; color: #1890ff; }
.vip-2 { background: #f6ffed; color: #52c41a; }
.vip-3 { background: #fff7e6; color: #fa8c16; }
.vip-4 { background: #f9f0ff; color: #722ed1; }

.expire-time {
  font-size: 12px;
  color: #999;
  margin-left: 5px;
}

.level-badge {
  display: inline-block;
  background: #1890ff;
  color: white;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 12px;
  margin-right: 5px;
}

.exp-info {
  font-size: 12px;
  color: #999;
}

.status-badge {
  padding: 3px 8px;
  border-radius: 12px;
  font-size: 12px;
}

.status-active { background: #f6ffed; color: #52c41a; }
.status-banned { background: #fff1f0; color: #f5222d; }
.status-inactive { background: #f5f5f5; color: #666; }

/* 操作按钮 */
.action-buttons {
  display: flex;
  gap: 5px;
}

.action-buttons button {
  padding: 4px 8px;
  border: 1px solid #ddd;
  border-radius: 3px;
  background: white;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-detail {
  color: #1890ff;
  border-color: #1890ff;
}

.btn-detail:hover {
  background: #e6f7ff;
}

.btn-status:hover {
  opacity: 0.8;
}

.btn-ban {
  color: #f5222d;
  border-color: #f5222d;
}

.btn-unban {
  color: #52c41a;
  border-color: #52c41a;
}

.dropdown {
  position: relative;
  display: inline-block;
}

.btn-more {
  color: #666;
}

.dropdown-content {
  display: none;
  position: absolute;
  right: 0;
  background: white;
  min-width: 120px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  border-radius: 4px;
  z-index: 1;
}

.dropdown-content a {
  display: block;
  padding: 8px 12px;
  color: #333;
  text-decoration: none;
  font-size: 12px;
  cursor: pointer;
}

.dropdown-content a:hover {
  background: #f5f5f5;
}

.dropdown:hover .dropdown-content {
  display: block;
}

/* 加载状态 */
.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.8);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 10;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid #1890ff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading-overlay p {
  margin-top: 10px;
  color: #666;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #999;
}

/* 分页样式 */
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
  margin-top: 20px;
  padding: 20px;
  background: white;
  border-radius: 8px;
}

.pagination button {
  padding: 8px 16px;
  border: 1px solid #ddd;
  background: white;
  border-radius: 4px;
  cursor: pointer;
}

.pagination button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info {
  color: #666;
  font-size: 14px;
}

.page-size {
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-size select {
  padding: 6px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

/* 模态框样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 8px;
  width: 90%;
  max-width: 800px;
  max-height: 90vh;
  overflow-y: auto;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #eee;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #999;
}

.modal-body {
  padding: 20px;
}

/* 用户信息网格 */
.user-info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
}

.info-item {
  display: flex;
  align-items: center;
  padding: 10px 0;
}

.info-item label {
  width: 100px;
  color: #666;
  font-weight: 500;
}

.info-item span {
  flex: 1;
  color: #333;
}

/* 表单组 */
.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
}

.form-group select,
.form-group input {
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.form-group small {
  display: block;
  margin-top: 5px;
  color: #999;
  font-size: 12px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.modal-footer button {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.modal-footer button:first-child {
  background: #1890ff;
  color: white;
}

.modal-footer button.secondary {
  background: #f5f5f5;
  color: #666;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .stats-grid,
  .charts-container {
    grid-template-columns: 1fr;
  }
  
  .filter-row {
    flex-direction: column;
    gap: 10px;
  }
  
  .user-info-grid {
    grid-template-columns: 1fr;
  }
  
  .pagination {
    flex-direction: column;
    gap: 10px;
  }
  
  .user-table {
    display: block;
    overflow-x: auto;
  }
}
</style>