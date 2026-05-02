<template>
  <div class="admin-vip-config">
    <!-- 配置管理头部 -->
    <div class="admin-header">
      <h2>VIP配置管理</h2>
      <div class="header-actions">
        <button class="add-config-btn" @click="showAddConfigModal = true">
          <span>+</span> 添加VIP配置
        </button>
        <button class="refresh-btn" @click="loadVipConfigs">
          🔄 刷新
        </button>
      </div>
    </div>

    <!-- VIP配置列表 -->
    <div class="configs-list">
      <div class="configs-table-container">
        <table class="configs-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>VIP类型</th>
              <th>套餐名称</th>
              <th>价格</th>
              <th>有效期</th>
              <th>特权数量</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="config in vipConfigs" :key="config.id">
              <td>{{ config.id }}</td>
              <td>
                <span class="vip-type-badge" :class="`vip-type-${config.vipType}`">
                  {{ getVipTypeName(config.vipType) }}
                </span>
              </td>
              <td>
                <div class="config-name">{{ config.vipName }}</div>
                <div v-if="config.discountTag" class="discount-tag-small">
                  {{ config.discountTag }}
                </div>
              </td>
              <td>
                <div class="price-info">
                  <div class="original-price">¥{{ config.originalPrice }}</div>
                  <div class="current-price">¥{{ config.currentPrice }}</div>
                </div>
              </td>
              <td>{{ config.durationDays || 0 }}天</td>
              <td>
                <span class="features-count">
                  {{ countFeatures(config) }} 项特权
                </span>
              </td>
              <td>
                <span :class="['status-badge', config.isActive ? 'active' : 'inactive']">
                  {{ config.isActive ? '启用' : '禁用' }}
                </span>
              </td>
              <td>
                <div class="action-buttons">
                  <button class="edit-btn" @click="editConfig(config)">编辑</button>
                  <button class="status-btn" @click="toggleConfigStatus(config)"
                    :class="{ 'disable-btn': config.isActive }">
                    {{ config.isActive ? '禁用' : '启用' }}
                  </button>
                  <button class="delete-btn" @click="confirmDelete(config)">删除</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 空状态 -->
      <div v-if="!loading && vipConfigs.length === 0" class="empty-state">
        <p>暂无VIP配置</p>
        <button class="add-first-btn" @click="showAddConfigModal = true">
          添加第一个VIP配置
        </button>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <p>加载中...</p>
      </div>
    </div>

    <!-- 订单管理区域 -->
    <div class="orders-management">
      <div class="section-header">
        <h3>VIP订单管理</h3>
        <div class="search-controls">
          <input type="text" placeholder="搜索订单号,用户名,VIP类型..." v-model="searchOrderNo" @keyup.enter="searchOrders">
          <button class="search-btn" @click="searchOrders">搜索</button>
          <button class="export-btn" @click="exportOrders">导出</button>
        </div>
      </div>

      <!-- 订单列表 -->
      <div class="orders-table-container">
        <table class="orders-table">
          <thead>
            <tr>
              <th><input type="checkbox" v-model="selectAllOrders" @change="toggleSelectAllOrders"></th>
              <th>订单号</th>
              <th>用户名</th>
              <th>VIP类型</th>
              <th>金额</th>
              <th>状态</th>
              <th>支付时间</th>
              <th>有效期至</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in vipOrders" :key="order.id">
              <td><input type="checkbox" v-model="selectedOrders" :value="order.id"></td>
              <td>{{ order.orderNo }}</td>
              <td>{{ order.username || '--' }}</td>
              <td>{{ order.vipName }}</td>
              <td>¥{{ order.amount }}</td>
              <td>
                <span :class="`order-status order-status-${order.status}`">
                  {{ getOrderStatusText(order.status) }}
                </span>
              </td>
              <td>{{ formatDateTime(order.payTime) }}</td>
              <td>{{ formatDate(order.expireTime) }}</td>
              <td>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 分页 -->
      <div v-if="pagination.total > 0" class="pagination">
        <div class="pagination-info">
          共 {{ pagination.total }} 条记录，第 {{ pagination.page }} 页
        </div>
        <div class="pagination-controls">
          <button :disabled="pagination.page === 1" @click="changePage(pagination.page - 1)">
            上一页
          </button>
          <span class="page-current">{{ pagination.page }}</span>
          <button :disabled="pagination.page >= pagination.totalPages" @click="changePage(pagination.page + 1)">
            下一页
          </button>
        </div>
      </div>
    </div>

    <!-- 添加/编辑VIP配置模态框 -->
    <div v-if="showAddConfigModal || showEditConfigModal" class="modal-overlay">
      <div class="modal-content config-modal">
        <div class="modal-header">
          <h3>{{ showEditConfigModal ? '编辑VIP配置' : '添加VIP配置' }}</h3>
        </div>

        <div class="modal-body">
          <form @submit.prevent="saveConfig">
            <div class="form-grid">
              <div class="form-group">
                <label>VIP类型 <span class="required">*</span></label>
                <select v-model="currentConfig.vipType" required>
                  <option value="1">月度VIP</option>
                  <option value="2">季度VIP</option>
                  <option value="3">年度VIP</option>
                  <option value="4">终身VIP</option>
                  <option value="5">半年VIP</option>
                  <option value="6">两年VIP</option>
                  <option value="7">三年VIP</option>
                </select>
              </div>

              <div class="form-group">
                <label>套餐名称 <span class="required">*</span></label>
                <input type="text" v-model="currentConfig.vipName" placeholder="例如：樱花月度会员" required>
              </div>

              <div class="form-group">
                <label>原价 <span class="required">*</span></label>
                <input type="number" v-model="currentConfig.originalPrice" placeholder="0.00" step="0.01" min="0"
                  required>
              </div>

              <div class="form-group">
                <label>现价 <span class="required">*</span></label>
                <input type="number" v-model="currentConfig.currentPrice" placeholder="0.00" step="0.01" min="0"
                  required>
              </div>

              <div class="form-group">
                <label>有效期天数</label>
                <input type="number" v-model="currentConfig.durationDays" placeholder="终身VIP请填写0" min="0">
                <small>终身VIP请填写0，其他类型填写对应天数</small>
              </div>

              <div class="form-group">
                <label>每日最大上传数</label>
                <input type="number" v-model="currentConfig.maxDailyUpload" placeholder="20" min="0">
              </div>

              <div class="form-group">
                <label>折扣标签</label>
                <input type="text" v-model="currentConfig.discountTag" placeholder="例如：限时8折">
              </div>

              <div class="form-group full-width">
                <label>专属徽章</label>
                <input type="text" v-model="currentConfig.exclusiveBadge" placeholder="例如：樱花粉头像框">
              </div>

              <div class="form-group checkbox-group">
                <label>
                  <input type="checkbox" v-model="currentConfig.adFree">
                  去广告特权
                </label>
                <label>
                  <input type="checkbox" v-model="currentConfig.priorityReview">
                  优先审核
                </label>
                <label>
                  <input type="checkbox" v-model="currentConfig.isActive">
                  是否启用
                </label>
              </div>

              <div class="form-group full-width">
                <label>特权描述</label>
                <textarea v-model="currentConfig.description" rows="3" placeholder="详细描述VIP特权..."></textarea>
              </div>

              <div class="form-group full-width">
                <label>突出特点 (JSON对象)</label>
                <textarea v-model="highlightFeaturesText" rows="4" 
                placeholder='例如：{"feature1": "专属头像框", "feature2": "测试json"}'></textarea>
                <small>请使用有效的JSON对象格式</small>
              </div>

              <div class="form-group full-width">
                <label>图标URL</label>
                <input type="text" v-model="currentConfig.iconUrl" placeholder="图标URL地址">
              </div>
            </div>

            <div class="form-actions">
              <button type="submit" class="save-btn" :disabled="saving">
                {{ saving ? '保存中...' : '保存配置' }}
              </button>
              <button type="button" class="cancel-btn" @click="closeConfigModal">
                取消
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getAllVipConfigsService,
  addVipConfigService,
  updateVipConfigService,
  deleteVipConfigService,
  toggleVipConfigStatusService,
  getVipOrdersAdminService,
  exportVipOrdersService
} from '@/api/adminVipApi'

// 配置管理数据
const loading = ref(false)
const vipConfigs = ref([])
const vipOrders = ref([])
const selectedOrders = ref([])
const searchOrderNo = ref('')
const showAddConfigModal = ref(false)
const showEditConfigModal = ref(false)
const saving = ref(false)

// 分页数据
const pagination = ref({
  page: 1,
  pageSize: 20,
  total: 0,
  totalPages: 0
})

// 表单数据
const currentConfig = ref({
  vipType: '1',
  vipName: '',
  originalPrice: '',
  currentPrice: '',
  durationDays: 30,
  maxDailyUpload: 20,
  adFree: true,
  exclusiveBadge: '',
  priorityReview: false,
  iconUrl: '',
  description: '',
  discountTag: '',
  isActive: true
})

const highlightFeaturesText = ref('')

// 页面加载时获取数据
onMounted(() => {
  loadVipConfigs()
  loadVipOrders()
})

/**
 * 加载VIP配置列表
 */
const loadVipConfigs = async () => {
  try {
    loading.value = true
    const response = await getAllVipConfigsService()
    if (response.code === 0) {
      vipConfigs.value = response.data
    }
  } catch (error) {
    console.error('加载VIP配置失败:', error)
  } finally {
    loading.value = false
  }
}

/**
 * 加载VIP订单列表
 */
const loadVipOrders = async () => {
  try {
    const response = await getVipOrdersAdminService({
      username: searchOrderNo.value,
      page: pagination.value.page,
      pageSize: pagination.value.pageSize
    })

    if (response.code === 0) {
      vipOrders.value = response.data.list || []
      pagination.value.total = response.data.total || 0
      pagination.value.totalPages = Math.ceil(pagination.value.total / pagination.value.pageSize)
    }
  } catch (error) {
    console.error('加载VIP订单失败:', error)
  }
}

/**
 * 搜索订单
 */
const searchOrders = () => {
  pagination.value.page = 1
  loadVipOrders()
}

/**
 * 导出订单
 */
const exportOrders = async () => {
  try {
    const params = {
      orderNo: searchOrderNo.value,
      ...pagination.value
    }

    const response = await exportVipOrdersService(params)

    if (response) {
      // 创建下载链接
      const blob = new Blob([response], { type: 'application/vnd.ms-excel' })
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `VIP订单_${new Date().toISOString().split('T')[0]}.xlsx`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
    }
  } catch (error) {
    console.error('导出订单失败:', error)
    ElMessage.error('导出失败')
  }
}

/**
 * 切换页面
 */
const changePage = (page) => {
  if (page < 1 || page > pagination.value.totalPages) return
  pagination.value.page = page
  loadVipOrders()
}

/**
 * 切换配置状态
 */
const toggleConfigStatus = async (config) => {
  if (!confirm(`确定要${config.isActive ? '禁用' : '启用'}这个VIP配置吗？`)) return

  try {
    const response = await toggleVipConfigStatusService(config.id, !config.isActive)
    if (response.code === 0) {
      config.isActive = !config.isActive
      ElMessage.success(`VIP配置 "${config.vipName}" 已${config.isActive ? '启用' : '禁用'}`)
    }
  } catch (error) {
    console.error('切换配置状态失败:', error)
    ElMessage.error('操作失败')
  }
}

/**
 * 确认删除配置
 */
const confirmDelete = (config) => {
  if (!confirm(`确定要删除VIP配置 "${config.vipName}" 吗？此操作不可恢复。`)) return

  deleteConfig(config.id)
}

/**
 * 删除配置
 */
const deleteConfig = async (id) => {
  try {
    const response = await deleteVipConfigService(id)
    if (response.code === 0) {
      ElMessage.success(`VIP配置已删除`)
      loadVipConfigs()
    }
  } catch (error) {
    console.error('删除配置失败:', error)
    ElMessage.error('删除失败')
  }
}

// 编辑配置
const editConfig = (config) => {
  currentConfig.value = { ...config }
  // 将对象转为JSON字符串显示
  highlightFeaturesText.value = JSON.stringify(config.highlightFeatures || {}, null, 2)
  showEditConfigModal.value = true
}
/**
 * 保存配置
 */
const saveConfig = async () => {
  try {
    saving.value = true

    // 处理highlightFeatures
    let highlightFeaturesObj = {}
    try {
      if (highlightFeaturesText.value.trim()) {
        highlightFeaturesObj = JSON.parse(highlightFeaturesText.value)
      }
    } catch (e) {
      ElMessage.error('突出特点必须是有效的JSON格式')
      return
    }

    const configData = {
      ...currentConfig.value,
      highlightFeatures: highlightFeaturesObj
    }

    let response
    if (showEditConfigModal.value && currentConfig.value.id) {
      response = await updateVipConfigService(configData)
    } else {
      response = await addVipConfigService(configData)
    }

    if (response.code === 0) {
      ElMessage.success('保存成功')
      closeConfigModal()
      loadVipConfigs()
    }
  } catch (error) {
    console.error('保存配置失败:', error)
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

/**
 * 关闭配置模态框
 */
const closeConfigModal = () => {
  showAddConfigModal.value = false
  showEditConfigModal.value = false
  currentConfig.value = {
    vipType: '1',
    vipName: '',
    originalPrice: '',
    currentPrice: '',
    durationDays: 30,
    maxDailyUpload: 20,
    adFree: true,
    exclusiveBadge: '',
    priorityReview: false,
    iconUrl: '',
    description: '',
    discountTag: '',
    isActive: true
  }
  highlightFeaturesText.value = '{}'
}

/**
 * 全选订单
 */
const selectAllOrders = computed({
  get: () => vipOrders.value.length > 0 &&
    selectedOrders.value.length === vipOrders.value.length,
  set: (value) => {
    if (value) {
      selectedOrders.value = vipOrders.value.map(order => order.id)
    } else {
      selectedOrders.value = []
    }
  }
})

const toggleSelectAllOrders = () => {
  selectAllOrders.value = !selectAllOrders.value
}

// 辅助方法
const getVipTypeName = (vipType) => {
  const names = {
    1: '月度',
    2: '季度',
    3: '年度',
    4: '终身'
  }
  return names[vipType] || '未知'
}

const countFeatures = (config) => {
  try {
    const features = JSON.parse(config.highlightFeatures || '[]')
    let count = features.length
    if (config.adFree) count++
    if (config.priorityReview) count++
    if (config.exclusiveBadge) count++
    return count
  } catch (e) {
    return 0
  }
}

const getOrderStatusText = (status) => {
  const statusTexts = {
    0: '待支付',
    1: '已支付',
    2: '支付失败',
    3: '已取消',
    4: '已退款'
  }
  return statusTexts[status] || '未知状态'
}

const formatDate = (dateString) => {
  if (!dateString) return '--'
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

const formatDateTime = (dateTime) => {
  if (!dateTime) return '--'
  const date = new Date(dateTime)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<style scoped>
.admin-vip-config {
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;
}

/* 头部样式 */
.admin-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  background: white;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.header-actions {
  display: flex;
  gap: 10px;
}

.add-config-btn {
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

.add-config-btn span {
  font-size: 20px;
}

.refresh-btn {
  background: #f5f5f5;
  border: 1px solid #ddd;
  padding: 10px 15px;
  border-radius: 6px;
  cursor: pointer;
}

/* 配置列表样式 */
.configs-list {
  background: white;
  border-radius: 12px;
  padding: 10px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.configs-table-container {
  overflow-x: auto;
}

.configs-table {
  width: 100%;
  border-collapse: collapse;
}

.configs-table th,
.configs-table td {
  padding: 12px;
  border-bottom: 1px solid #eee;
  text-align: left;
}

.configs-table th {
  background: #fafafa;
  font-weight: 600;
  color: #666;
}

/* VIP类型标签 */
.vip-type-badge {
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.vip-type-1 {
  background: #e6f7ff;
  color: #1890ff;
}

.vip-type-2 {
  background: #f6ffed;
  color: #52c41a;
}

.vip-type-3 {
  background: #fff7e6;
  color: #fa8c16;
}

.vip-type-4 {
  background: #f9f0ff;
  color: #722ed1;
}

/* 配置名称 */
.config-name {
  font-weight: 500;
  margin-bottom: 4px;
}

.discount-tag-small {
  display: inline-block;
  background: #fff1f0;
  color: #f5222d;
  padding: 2px 6px;
  border-radius: 10px;
  font-size: 11px;
}

/* 价格信息 */
.price-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.original-price {
  text-decoration: line-through;
  color: #999;
  font-size: 12px;
}

.current-price {
  color: #ff6b6b;
  font-weight: 500;
}

/* 特权数量 */
.features-count {
  background: #f0f0f0;
  padding: 4px 8px;
  border-radius: 10px;
  font-size: 12px;
}

/* 状态标签 */
.status-badge {
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-badge.active {
  background: #f6ffed;
  color: #52c41a;
}

.status-badge.inactive {
  background: #f5f5f5;
  color: #666;
}

/* 操作按钮 */
.action-buttons {
  display: flex;
  gap: 6px;
}

.edit-btn,
.status-btn,
.delete-btn {
  padding: 6px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
}

.edit-btn {
  background: #1890ff;
  color: white;
  border-color: #1890ff;
}

.status-btn {
  background: #52c41a;
  color: white;
  border-color: #52c41a;
}

.status-btn.disable-btn {
  background: #f5222d;
  border-color: #f5222d;
}

.delete-btn {
  background: #fff1f0;
  color: #f5222d;
  border-color: #ffa39e;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 40px;
  color: #999;
}

.add-first-btn {
  background: #1890ff;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 6px;
  cursor: pointer;
  margin-top: 15px;
}

/* 订单管理区域 */
.orders-management {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.search-controls {
  display: flex;
  gap: 10px;
  align-items: center;
}

.search-controls input {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  width: 200px;
}

.search-btn,
.export-btn {
  padding: 8px 16px;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
}

.search-btn {
  background: #1890ff;
  color: white;
  border-color: #1890ff;
}

.search-btn:hover {
  opacity: 0.9;
  background-color: rgb(196, 29, 218);
}

.export-btn:hover {
  opacity: 0.9;
  background-color: rgb(228, 26, 93);
}

.export-btn {
  background: #52c41a;
  color: white;
  border-color: #52c41a;
}

/* 订单表格 */
.orders-table-container {
  overflow-x: auto;
  margin-bottom: 20px;
}

.orders-table {
  width: 100%;
  border-collapse: collapse;
}

.orders-table th,
.orders-table td {
  padding: 12px;
  border-bottom: 1px solid #eee;
  text-align: left;
}

.order-status {
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.order-status-0 {
  background: #fff7e6;
  color: #fa8c16;
}

.order-status-1 {
  background: #f6ffed;
  color: #52c41a;
}

.order-status-2 {
  background: #fff1f0;
  color: #f5222d;
}

.order-status-3 {
  background: #f5f5f5;
  color: #666;
}

.order-status-4 {
  background: #e6f7ff;
  color: #1890ff;
}

/* 订单操作按钮 */
.order-actions {
  display: flex;
  gap: 6px;
}

.detail-btn,
.refund-btn {
  padding: 6px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
}

.detail-btn {
  background: #1890ff;
  color: white;
  border-color: #1890ff;
}

.refund-btn {
  background: #fff1f0;
  color: #f5222d;
  border-color: #ffa39e;
}

/* 分页样式 */
.pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 10px;
}

.pagination-controls button {
  padding: 8px 16px;
  border: 1px solid #ddd;
  background: white;
  border-radius: 4px;
  cursor: pointer;
}

.pagination-controls button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-current {
  padding: 8px 12px;
  background: #f5f5f5;
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
  border-radius: 12px;
  max-width: 800px;
  max-height: 90vh;
  overflow-y: auto;
}

/* 配置表单样式 */
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  padding: 20px;
}

.form-group {
  margin-bottom: 16px;
}

.form-group.full-width {
  grid-column: 1 / -1;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  font-weight: 400;
}

.form-group .required {
  color: #f5222d;
}

.form-group input,
.form-group select,
.form-group textarea {
  width: 80%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.form-group textarea {
  resize: vertical;
  min-height: 60px;
}

.form-group small {
  display: block;
  margin-top: 4px;
  color: #999;
  font-size: 12px;
}

.checkbox-group {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.checkbox-group label {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  font-weight: normal;
}

.checkbox-group input[type="checkbox"] {
  width: auto;
}

/* 表单操作按钮 */
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.save-btn,
.cancel-btn,
.confirm-refund-btn {
  padding: 10px 20px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 500;
}

.save-btn,
.confirm-refund-btn {
  background: #1890ff;
  color: white;
}

.save-btn:hover:hover,
.confirm-refund-btn:hover:hover {
  opacity: 0.9;
  background-color: rgb(196, 29, 218);
}

.save-btn:disabled,
.confirm-refund-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.cancel-btn {
  background: #f5f5f5;
  color: #666;
}

.cancel-btn:hover:hover {
  opacity: 0.9;
  background-color: rgb(199, 134, 156);
}

/* 加载状态 */
.loading-state {
  text-align: center;
  padding: 40px;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid #1890ff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 10px;
}

.spinner.small {
  width: 30px;
  height: 30px;
  border-width: 2px;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }

  100% {
    transform: rotate(360deg);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }

  .search-controls {
    flex-direction: column;
    align-items: stretch;
  }

  .search-controls input {
    width: 100%;
  }

  .action-buttons {
    flex-direction: column;
  }
}
</style>