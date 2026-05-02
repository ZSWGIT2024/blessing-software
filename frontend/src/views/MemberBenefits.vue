<template>
  <div class="content-section sakura-bg">
   <h2>🌸 会员权益</h2>
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-overlay">
      <div class="spinner"></div>
      <p>加载中...</p>
    </div>

    <!-- 选项卡导航 -->
    <div class="vip-tabs-container">
      <div class="vip-tabs">
        <button 
          v-for="tab in tabs" 
          :key="tab.id"
          class="tab-btn"
          :class="{ active: activeTab === tab.id }"
          @click="switchTab(tab.id)"
        >
          {{ tab.name }}
        </button>
        <!-- 滑块指示器 -->
        <div class="tab-slider" :style="sliderStyle"></div>
      </div>
    </div>

    <!-- 我的会员选项卡 -->
    <div v-show="activeTab === 'myVip'" class="tab-content">
      <!-- 当前会员状态 -->
      <div v-if="currentVip" class="current-membership">
        <div class="section-header">
          <h3>当前会员状态</h3>
          <div class="action-buttons">
            <button v-if="currentVip.vipType !== 4" class="renew-btn" @click="handleRenewVip">续费会员</button>
          </div>
        </div>
        
        <div class="membership-status">
          <div class="status-icon" :style="{ color: getVipColor(currentVip.vipType) }">
            ⭐
          </div>
          <div class="status-info">
            <div class="status-tier">{{ currentVip.vipName }}</div>
            <div class="status-details">
              <div class="detail-item">
                <span class="label">有效期至：</span>
                <span class="value">{{ formatDate(currentVip.expireTime) }}</span>
              </div>
              <div class="detail-item">
                <span class="label">剩余天数：</span>
                <span class="value">{{ getRemainingDays(currentVip.expireTime) }}天</span>
              </div>
              <div class="detail-item">
                <span class="label">套餐价格：</span>
                <span class="value">
                  <span v-if="currentVip.discountRate < 1" class="original-price-small">
                    ¥{{ currentVip.originalPrice }}
                  </span>
                  ¥{{ currentVip.currentPrice }}
                </span>
              </div>
            </div>
            <div class="vip-privileges">
              <span v-for="privilege in currentVipPrivileges" :key="privilege" class="privilege-tag">
                {{ privilege }}
              </span>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 非VIP用户提示 -->
      <div v-else class="non-vip-prompt">
        <div class="prompt-content">
          <h3>还不是会员？</h3>
          <p>立即开通会员，尊享专属特权！</p>
          <ul class="prompt-benefits">
            <li>✨ 去除所有广告干扰</li>
            <li>✨ 每日更多上传次数</li>
            <li>✨ 优先审核作品</li>
            <li>✨ 专属身份标识</li>
          </ul>
          <div class="prompt-actions">
            <button class="primary-btn" @click="activeTab = 'joinVip'">立即加入会员</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 加入会员选项卡 -->
    <div v-show="activeTab === 'joinVip'" class="tab-content">
      <div class="vip-plans-section">
        <div class="membership-cards">
          <div class="membership-card" v-for="plan in vipPlans" :key="plan.id"
               :class="{ 'recommended': plan.recommended, 'popular': plan.popularTag }">
            
            <!-- 推荐标签 -->
            <div v-if="plan.recommended" class="recommend-badge">推荐</div>
            <div v-if="plan.popularTag" class="popular-badge">{{ plan.popularTag }}</div>
            
            <!-- 卡片头部 -->
            <div class="card-header" :style="getHeaderStyle(plan.vipType)">
              <h3>{{ plan.vipName }}</h3>
              <div class="price-section">
                <div class="original-price" v-if="plan.discountRate < 1">
                  ¥{{ plan.originalPrice }}
                </div>
                <div class="current-price">
                  ¥{{ plan.currentPrice }}
                  <span class="price-unit">/{{ getPriceUnit(plan.vipType) }}</span>
                </div>
                <div v-if="plan.discountTag" class="discount-tag">
                  {{ plan.discountTag }}
                </div>
              </div>
            </div>
            
            <!-- 特权列表 -->
            <div class="card-benefits">
              <div class="benefit-item" v-for="feature in highlightFeatures(plan)" :key="feature">
                <span class="benefit-icon">✓</span>
                <span>{{ feature }}</span>
              </div>
              
              <!-- 专属徽章 -->
              <div v-if="plan.exclusiveBadge" class="exclusive-badge">
                <span class="badge-icon">🎖️</span>
                <span>{{ plan.exclusiveBadge }}</span>
              </div>
            </div>
            
            <!-- 操作按钮 -->
            <div class="card-actions">
              <button class="card-button" 
                      :style="getButtonStyle(plan.vipType)"
                      @click="handlePurchase(plan)"
                      :disabled="isCurrentVip(plan)">
                {{ getButtonText(plan) }}
              </button>
              <div class="save-note" v-if="plan.discountRate < 1">
                立省¥{{ (plan.originalPrice - plan.currentPrice).toFixed(2) }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 订单历史选项卡 -->
    <div v-show="activeTab === 'orderHistory'" class="tab-content">
      <!-- 用户订单历史 -->
      <div v-if="orderHistory.length > 0" class="order-history-section">
        <div class="section-header">
          <h3>我的订单记录</h3>
          <button @click="toggleOrderHistory" class="toggle-btn">
            {{ showOrderHistory ? '收起' : '展开' }}
          </button>
        </div>
        
        <div v-if="showOrderHistory" class="order-history-list">
          <table class="order-table">
            <thead>
              <tr>
                <th>订单号</th>
                <th>套餐</th>
                <th>金额</th>
                <th>状态</th>
                <th>下单时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="order in orderHistory" :key="order.orderNo">
                <td>{{ order.orderNo }}</td>
                <td>{{ getVipTypeName(order.vipType) }}</td>
                <td>¥{{ order.amount }}</td>
                <td :class="`order-status-${order.status}`">
                  {{ getOrderStatusText(order.status) }}
                </td>
                <td>{{ formatDateTime(order.createTime) }}</td>
                <td>
                  <button 
                    v-if="order.status === 0" 
                    @click="cancelOrder(order.orderNo)"
                    class="cancel-order-btn"
                  >
                    取消订单
                  </button>
                  <span v-else-if="order.status === 1" class="success-text">
                    ✔ 已支付
                  </span>
                  <button 
                    v-else-if="order.status === 2 || order.status === 3"
                    @click="queryOrderStatus(order.orderNo)"
                    class="query-btn"
                  >
                    查询状态
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- VIP变更记录 -->
      <div v-if="vipRecords.length > 0" class="vip-records-section">
        <div class="section-header">
          <h3>VIP变更记录</h3>
          <button @click="toggleVipRecords" class="toggle-btn">
            {{ showVipRecords ? '收起' : '展开' }}
          </button>
        </div>
        
        <div v-if="showVipRecords" class="vip-records-list">
          <div v-for="record in vipRecords" :key="record.id" class="record-item">
            <div class="record-icon">
              <span v-if="record.actionType === 1">🎯</span>
              <span v-if="record.actionType === 2">🔄</span>
              <span v-if="record.actionType === 3">⬆️</span>
              <span v-if="record.actionType === 4">⬇️</span>
              <span v-if="record.actionType === 5">🔙</span>
              <span v-if="record.actionType === 6">🔚</span>
            </div>
            <div class="record-content">
              <div class="record-action">
                {{ getActionTypeName(record.actionType) }}
                <span class="record-vip-type">{{ getVipTypeName(record.vipType) }}</span>
              </div>
              <div class="record-time">{{ formatDateTime(record.createTime) }}</div>
              <div v-if="record.remark" class="record-remark">{{ record.remark }}</div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="orderHistory.length === 0 && vipRecords.length === 0" class="empty-history">
        <div class="empty-icon">📋</div>
        <h4>暂无历史记录</h4>
        <p>您还没有任何订单或VIP变更记录</p>
        <button class="primary-btn" @click="activeTab = 'joinVip'">去开通会员</button>
      </div>
    </div>
    
    <!-- 支付模态框 -->
    <div v-if="showPaymentModal" class="modal-overlay">
      <div class="modal-content payment-modal">
        <div class="modal-header">
          <h3>开通会员</h3>
          <button @click="closePaymentModal" class="close-btn">×</button>
        </div>
        
        <div class="modal-body">
          <!-- 订单信息 -->
          <div class="order-info">
            <div class="order-item">
              <span class="label">会员套餐：</span>
              <span class="value">{{ selectedPlan.vipName }}</span>
            </div>
            <div class="order-item">
              <span class="label">套餐时长：</span>
              <span class="value">{{ getDurationText(selectedPlan.vipType) }}</span>
            </div>
            <div class="order-item">
              <span class="label">支付金额：</span>
              <span class="price-value">¥{{ selectedPlan.currentPrice }}</span>
            </div>
            <div v-if="selectedPlan.discountRate < 1" class="order-item">
              <span class="label">节省金额：</span>
              <span class="saving-value">
                立省¥{{ (selectedPlan.originalPrice - selectedPlan.currentPrice).toFixed(2) }}
              </span>
            </div>
          </div>
          
          <!-- 支付方式选择 -->
          <div class="payment-methods">
            <h4>选择支付方式</h4>
            <div class="method-list">
              <div class="method-item" 
                   v-for="method in paymentMethods" 
                   :key="method.type"
                   :class="{ 'selected': selectedMethod === method.type }"
                   @click="selectedMethod = method.type">
                <div class="method-icon">
                  <img :src="method.icon" :alt="method.name">
                </div>
                <div class="method-info">
                  <div class="method-name">{{ method.name }}</div>
                  <div class="method-desc">{{ method.desc }}</div>
                </div>
                <div class="method-check">
                  <div class="check-circle" :class="{ 'checked': selectedMethod === method.type }"></div>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 支付协议 -->
          <div class="payment-agreement">
            <label class="agreement-checkbox">
              <input type="checkbox" v-model="agreedToTerms">
              <span class="checkmark"></span>
              我已阅读并同意
              <a href="#" class="agreement-link">《会员服务协议》</a>
              和
              <a href="#" class="agreement-link">《支付服务协议》</a>
            </label>
          </div>
          
          <!-- 支付按钮 -->
          <div class="payment-actions">
            <button class="pay-button" 
                    :disabled="!agreedToTerms || paying" 
                    @click="handlePayment">
              {{ paying ? '支付中...' : `确认支付 ¥${selectedPlan.currentPrice}` }}
            </button>
            <button class="cancel-button" @click="closePaymentModal">取消</button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 支付成功模态框 -->
    <div v-if="showSuccessModal" class="modal-overlay">
      <div class="modal-content success-modal">
        <div class="success-icon">🎉</div>
        <h3>支付成功！</h3>
        <p class="success-message">您已成功开通{{ selectedPlan.vipName }}</p>
        <div class="success-details">
          <p><strong>订单号：</strong>{{ orderInfo.orderNo }}</p>
          <p><strong>有效期至：</strong>{{ formatDate(orderInfo.expireTime) }}</p>
          <p><strong>支付金额：</strong>¥{{ selectedPlan.currentPrice }}</p>
        </div>
        <div class="success-actions">
          <button class="confirm-btn" @click="closeSuccessModal">确定</button>
          <button class="view-details-btn" @click="viewOrderDetails">查看订单详情</button>
        </div>
      </div>
    </div>
    
    <!-- 权益详情模态框 -->
    <div v-if="showBenefitsModal" class="modal-overlay">
      <div class="modal-content benefits-modal">
        <div class="modal-body">
          <div v-if="benefitsData" class="benefits-content">
            <h4>{{ benefitsData.vipName }} 专属权益</h4>
            <div class="benefits-list">
              <div v-for="(benefit, index) in benefitsData.benefits" :key="index" class="benefit-detail">
                <span class="benefit-icon">✨</span>
                <span class="benefit-text">{{ benefit }}</span>
              </div>
            </div>
            <div class="price-info" v-if="benefitsData.priceInfo">
              <h5>价格信息</h5>
              <div class="price-item">
                <span>原价：</span>
                <span class="original-price">¥{{ benefitsData.priceInfo.originalPrice }}</span>
              </div>
              <div class="price-item">
                <span>现价：</span>
                <span class="current-price">¥{{ benefitsData.priceInfo.currentPrice }}</span>
              </div>
              <div v-if="benefitsData.priceInfo.discountTag" class="discount-info">
                {{ benefitsData.priceInfo.discountTag }}
              </div>
            </div>
          </div>
          <div v-else class="loading-benefits">
            <div class="spinner small"></div>
            <p>加载权益详情中...</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  getVipPlansService, 
  getCurrentVipService, 
  createVipOrderService,
  getOrderStatusService,
  checkPaymentStatus,
  renewVipService,
  cancelOrderService,
  getOrderHistoryService,
  getVipRecordsService
} from '@/api/vipApi'


// 响应式数据
const loading = ref(false)
const vipPlans = ref([])
const currentVip = ref(null)
const orderHistory = ref([])
const vipRecords = ref([])
const showPaymentModal = ref(false)
const showSuccessModal = ref(false)
const showBenefitsModal = ref(false)
const showOrderHistory = ref(true) // 默认展开
const showVipRecords = ref(true) // 默认展开
const selectedPlan = ref(null)
const selectedMethod = ref('alipay')
const agreedToTerms = ref(false)
const paying = ref(false)
const orderInfo = ref({})

// 新增：选项卡相关
const activeTab = ref('myVip') // 默认显示我的会员
const tabs = ref([
  { id: 'myVip', name: '我的会员' },
  { id: 'joinVip', name: '加入会员' },
  { id: 'orderHistory', name: '订单历史' }
])

// 计算滑块样式
const sliderStyle = computed(() => {
  const tabWidth = 100 / tabs.value.length
  const tabIndex = tabs.value.findIndex(tab => tab.id === activeTab.value)
  return {
    width: `${tabWidth}%`,
    transform: `translateX(${tabIndex * 100}%)`
  }
})

// 支付方式配置
const paymentMethods = ref([
  {
    type: 'alipay',
    name: '支付宝',
    desc: '推荐支付宝用户使用',
    icon: '/images/payment/alipay.svg'
  },
  {
    type: 'wechat',
    name: '微信支付',
    desc: '推荐微信用户使用',
    icon: '/images/payment/wechat.svg'
  },
  {
    type: 'balance',
    name: '余额支付',
    desc: '使用账户余额支付',
    icon: '/images/payment/balance.svg'
  }
])

// 页面加载时获取数据
onMounted(() => {
  loadVipData()
})

/**
 * 加载VIP数据
 */
const loadVipData = async () => {
  try {
    loading.value = true
    
    // 并行获取所有数据
    const [plansResponse, currentResponse, historyResponse, recordsResponse] = await Promise.all([
      getVipPlansService(),
      getCurrentVipService(),
      getOrderHistoryService(5),
      getVipRecordsService(5)
    ])
    
    if (plansResponse.code === 0) {
      vipPlans.value = processVipPlans(plansResponse.data)
    }
    
    if (currentResponse.code === 0) {
      currentVip.value = currentResponse.data
    }
    
    if (historyResponse.code === 0) {
      orderHistory.value = historyResponse.data
    }
    
    if (recordsResponse.code === 0) {
      vipRecords.value = recordsResponse.data
    }
    
    // 如果当前没有会员，默认显示加入会员选项卡
    if (!currentVip.value) {
      activeTab.value = 'joinVip'
    }
    
  } catch (error) {
    console.error('加载VIP数据失败:', error)
  } finally {
    loading.value = false
  }
}

/**
 * 切换选项卡
 */
const switchTab = (tabId) => {
  activeTab.value = tabId
}

/**
 * 处理续费VIP
 * @description 调用续费接口，逻辑与开通会员类似
 */
const handleRenewVip = async () => {
  if (!currentVip.value) return
  
  try {
    // 找到当前VIP对应的套餐
    const currentPlan = vipPlans.value.find(plan => plan.vipType === currentVip.value.vipType)
    if (currentPlan) {
      selectedPlan.value = currentPlan
      showPaymentModal.value = true
    }
  } catch (error) {
    console.error('续费失败:', error)
    ElMessage.error('续费失败，请重试')
  }
}

/**
 * 查询订单状态
 * @description 根据订单号查询订单最新状态
 */
const queryOrderStatus = async (orderNo) => {
  try {
    const response = await getOrderStatusService(orderNo)
    if (response.code === 0) {
      // 更新订单状态
      const orderIndex = orderHistory.value.findIndex(order => order.orderNo === orderNo)
      if (orderIndex !== -1) {
        orderHistory.value[orderIndex].status = response.data.status
        
        // 如果是支付成功状态，刷新当前VIP信息
        if (response.data.status === 1) {
          await loadCurrentVip()
          ElMessage.success('订单状态已更新：支付成功')
        } else {
          ElMessage.info(`订单状态已更新：${getOrderStatusText(response.data.status)}`)
        }
      }
    }
  } catch (error) {
    console.error('查询订单状态失败:', error)
    ElMessage.error('查询订单状态失败，请重试')
  }
}

// 修改handlePayment方法，添加续费逻辑
const handlePayment = async () => {
  if (!agreedToTerms.value) {
    ElMessage.warning('请先阅读并同意相关协议')
    return
  }
  
  try {
    paying.value = true
    
    // 判断是续费还是新开通
    const isRenewal = currentVip.value && 
                     currentVip.value.vipType === selectedPlan.value.vipType
    
    let response
    
    if (isRenewal) {
      // 调用续费接口
      response = await renewVipService(
        selectedPlan.value.vipType,
        selectedMethod.value
      )
    } else {
      // 调用新开通接口
      response = await createVipOrderService(
        selectedPlan.value.vipType,
        selectedMethod.value
      )
    }
    
    if (response.code === 0) {
      orderInfo.value = response.data
      
      if (selectedMethod.value === 'balance') {
        ElMessage.success('余额支付成功，请输入支付密码')
        // 余额支付直接跳转密码验证
        showPaymentModal.value = false
        showSuccessModal.value = true
        // 刷新当前VIP信息
        await loadCurrentVip()
        // 刷新订单历史
        await loadOrderHistory()
      
      } else {
        // 第三方支付，调用支付接口
        await initiateThirdPartyPayment(response.data.paymentParams)
        
        // 开始轮询支付状态
        const result = await checkPaymentStatus(orderInfo.value.orderNo)
        
        if (result.success) {
          showPaymentModal.value = false
          showSuccessModal.value = true
          // 刷新当前VIP信息
          await loadCurrentVip()
          // 刷新订单历史
          await loadOrderHistory()
        } else {
          ElMessage.error(result.message || '支付失败')
        }
      }
    }
  } catch (error) {
    console.error('支付失败:', error)
    ElMessage.error('支付失败，请重试')
  } finally {
    paying.value = false
  }
}

/**
 * 加载订单历史
 */
const loadOrderHistory = async () => {
  try {
    const response = await getOrderHistoryService(5)
    if (response.code === 0) {
      orderHistory.value = response.data
    }
  } catch (error) {
    console.error('加载订单历史失败:', error)
  }
}

/**
 * 处理VIP套餐数据
 */
const processVipPlans = (plans) => {
  return plans.map(plan => {
    // 计算折扣率
    const discountRate = plan.currentPrice / plan.originalPrice
    
    // 标记推荐套餐（年费套餐通常最划算）
    const recommended = plan.vipType === 3 // 年度VIP
    
    // 热门标签
    const popularTag = plan.vipType === 4 ? '最受欢迎' : 
                      plan.vipType === 3 ? '超值推荐' : null
    
    return {
      ...plan,
      discountRate,
      recommended,
      popularTag
    }
  }).sort((a, b) => a.vipType - b.vipType) // 按VIP类型排序
}

const viewOrderDetails = () => {
  // 切换到订单历史选项卡
  activeTab.value = 'orderHistory'
  showSuccessModal.value = false
}
// 新增辅助方法
const getVipTypeName = (vipType) => {
  const names = {
    1: '月度会员',
    2: '季度会员',
    3: '年度会员',
    4: '终身会员'
  }
  return names[vipType] || '未知会员'
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

const getActionTypeName = (actionType) => {
  const actionNames = {
    1: '开通',
    2: '续费',
    3: '升级',
    4: '降级',
    5: '到期',
    6: '取消'
  }
  return actionNames[actionType] || '未知操作'
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

/**
 * 切换订单历史显示
 */
const toggleOrderHistory = () => {
  showOrderHistory.value = !showOrderHistory.value
}

/**
 * 切换VIP记录显示
 */
const toggleVipRecords = () => {
  showVipRecords.value = !showVipRecords.value
}

/**
 * 取消订单
 */
const cancelOrder = async (orderNo) => {
  ElMessageBox.confirm('确定要取消该订单吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const response = await cancelOrderService(orderNo)
      if (response.code === 0) {
        ElMessage.success('订单已取消')
        // 刷新订单历史
        await loadOrderHistory()
      }
    } catch (error) {
      console.error('取消订单失败:', error)
      ElMessage.error('取消订单失败，请重试')
    }
  }).catch(() => {
    // 取消操作，不执行任何操作
    ElMessage.info('操作已取消')
  })
}

/**
 * 获取价格单位
 */
const getPriceUnit = (vipType) => {
  const units = {
    1: '月',
    2: '季',
    3: '年',
    4: '永久'
  }
  return units[vipType] || '月'
}

/**
 * 获取套餐时长文本
 */
const getDurationText = (vipType) => {
  const durations = {
    1: '月度会员',
    2: '季度会员',
    3: '年度会员',
    4: '终身会员'
  }
  return durations[vipType] || '1个月'
}

/**
 * 获取卡片头部样式
 */
const getHeaderStyle = (vipType) => {
  const gradients = {
    1: 'linear-gradient(135deg, #a1c4fd 0%, #c2e9fb 100%)',
    2: 'linear-gradient(135deg, #ff9a9e 0%, #fad0c4 100%)',
    3: 'linear-gradient(135deg, #ffc3a0 0%, #ffafbd 100%)',
    4: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
  }
  return { background: gradients[vipType] || gradients[1] }
}

/**
 * 获取按钮样式
 */
const getButtonStyle = (vipType) => {
  const colors = {
    1: '#a1c4fd',
    2: '#ff9a9e',
    3: '#ffc3a0',
    4: '#667eea'
  }
  return { background: colors[vipType] || colors[1] }
}

/**
 * 获取VIP颜色
 */
const getVipColor = (vipType) => {
  const colors = {
    1: '#a1c4fd',
    2: '#ff9a9e',
    3: '#ffc3a0',
    4: '#667eea'
  }
  return colors[vipType] || '#a1c4fd'
}

/**
 * 获取高亮特性
 */
const highlightFeatures = (plan) => {
  try {
    if (plan.highlightFeatures) {
      // 如果是字符串，解析成对象
      const features = typeof plan.highlightFeatures === 'string'
        ? JSON.parse(plan.highlightFeatures)
        : plan.highlightFeatures;
      
      // 如果是对象，提取值转为数组
      if (features && typeof features === 'object') {
        return Object.values(features); // 转为数组，如 ['去广告', '无限上传']
      }
    }
  } catch (e) {
    console.error('解析highlightFeatures失败:', e);
  }

  // 默认返回数组
  const defaultFeatures = {
    1: ['去广告', '每日50个作品', '优先审核', '专属头像框'],
    2: ['去广告', '每日100个作品', '优先审核', '专属头像框'],
    3: ['去广告', '每日200个作品', '优先审核', '专属头像框'],
    4: ['所有VIP权益', '永久有效']
  };

  return defaultFeatures[plan.vipType] || defaultFeatures[1];
};


/**
 * 获取当前VIP特权
 */
const currentVipPrivileges = computed(() => {
  if (!currentVip.value) return []
  return highlightFeatures(currentVip.value)
})

/**
 * 检查是否是当前VIP
 */
const isCurrentVip = (plan) => {
  if (!currentVip.value) return false
  return currentVip.value.vipType === plan.vipType
}

/**
 * 获取按钮文本
 */
const getButtonText = (plan) => {
  // 如果是当前VIP，则显示“当前套餐”
  if (isCurrentVip(plan)) {
    return '当前套餐'
  }
  // 如果有当前VIP，则显示“立即升级”
  if (currentVip.value) {
    return '立即升级'
  }
  
  return '立即开通'
}

/**
 * 处理购买
 */
const handlePurchase = (plan) => {
  //获取点击的VIP套餐等级
  const vipType = plan.vipType
   // 获取当前VIP套餐
  const currentPlan = vipPlans.value.find(plan => plan.vipType === currentVip.value.vipType)
  if (currentPlan) {
  //如果为永久会员，则提示无需升级
  if (currentVip.value.vipType === 4) {
    ElMessage.warning('终身会员无需升级')
    return
  }
  // 如果当前VIP套餐存在，并且VIP类型相同，则显示续费模态框
  if (vipType >= currentVip.value.vipType) {
    selectedPlan.value = plan
    showRenewalModal.value = true
    selectedMethod.value = 'alipay'
    agreedToTerms.value = false
  }
  if (vipType < currentVip.value.vipType) {
    // 如果点击的VIP套餐等级小于当前VIP等级，则提示用户是否要降级
    ElMessageBox.confirm('您的VIP到期将降级为' + getDurationText(vipType) + '，您确定要继续操作吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      // 用户确认降级
      selectedPlan.value = plan
      showPaymentModal.value = true
      selectedMethod.value = 'alipay'
      agreedToTerms.value = false
    }).catch(() => {
      // 用户取消降级
      ElMessage.info('操作已取消')
    })
  }
}else {
    // 用户不是VIP，直接显示支付模态框
    selectedPlan.value = plan
    showPaymentModal.value = true
    selectedMethod.value = 'alipay'
    agreedToTerms.value = false
  }
}
/**
 * 发起第三方支付
 */
const initiateThirdPartyPayment = async (paymentParams) => {
  // 这里根据具体的支付方式调用支付接口
  // 示例：支付宝支付
  if (selectedMethod.value === 'alipay') {
    // 创建支付宝支付表单
    const form = document.createElement('form')
    form.method = 'POST'
    form.action = '/api/payment/alipay'
    
    Object.keys(paymentParams).forEach(key => {
      const input = document.createElement('input')
      input.type = 'hidden'
      input.name = key
      input.value = paymentParams[key]
      form.appendChild(input)
    })
    
    document.body.appendChild(form)
    form.submit()
    document.body.removeChild(form)
  }
  // 微信支付
  else if (selectedMethod.value === 'wechat') {
    // 调用微信JS-SDK
    // wx.chooseWXPay({
    //   timestamp: paymentParams.timeStamp,
    //   nonceStr: paymentParams.nonceStr,
    //   package: paymentParams.package,
    //   signType: paymentParams.signType,
    //   paySign: paymentParams.paySign,
    //   success: () => {
    //     // 支付成功
    //   }
    // })
  }
}

/**
 * 加载当前VIP信息
 */
const loadCurrentVip = async () => {
  try {
    const response = await getCurrentVipService()
    if (response.code === 0) {
      currentVip.value = response.data
    }
  } catch (error) {
    console.error('加载当前VIP信息失败:', error)
  }
}

/**
 * 关闭支付模态框
 */
const closePaymentModal = () => {
  showPaymentModal.value = false
  selectedPlan.value = null
  selectedMethod.value = 'alipay'
  agreedToTerms.value = false
}

/**
 * 关闭成功模态框
 */
const closeSuccessModal = () => {
  showSuccessModal.value = false
  orderInfo.value = {}
}
/**
 * 格式化日期
 */
const formatDate = (dateString) => {
  if (!dateString) return '--'
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

/**
 * 计算剩余天数
 */
const getRemainingDays = (expireTime) => {
  if (!expireTime) return 0
  const now = new Date()
  const expire = new Date(expireTime)
  const diffTime = expire.getTime() - now.getTime()
  return Math.max(0, Math.ceil(diffTime / (1000 * 60 * 60 * 24)))
}
</script>

<style scoped>
/* 基本样式保留原有设计，新增以下样式 */

.membership-cards {
  margin-left: 50px;
  width: 90%;
  display: flex;
  justify-content: space-between;
  gap: 20px;
}

.membership-card {
  width: 100%;
  background: white;
  border-radius: 15px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(53, 224, 47, 0.8);
  transition: transform 0.5s, box-shadow 2s;
  position: relative;
}

.membership-card:hover {
  transform: translateY(-5px);
  box-shadow: 10px 15px 30px rgba(226, 14, 226, 0.7);
}

.membership-card.recommended {
  border: 2px solid #ff6b6b;
}

.recommend-badge {
  position: absolute;
  top: 10px;
  right: -30px;
  background: #ff6b6b;
  color: white;
  padding: 5px 30px;
  transform: rotate(45deg);
  font-size: 12px;
  font-weight: bold;
}

.popular-badge {
  position: absolute;
  top: 10px;
  left: 10px;
  background: #4ecdc4;
  color: white;
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
}

.card-header {
  padding: 25px;
  color: white;
  text-align: center;
  position: relative;
}

.price-section {
  margin-top: 15px;
}

.original-price {
  font-size: 16px;
  text-decoration: line-through;
  opacity: 0.8;
}

.current-price {
  font-size: 32px;
  font-weight: bold;
  margin: 5px 0;
}

.price-unit {
  font-size: 16px;
  font-weight: normal;
}

.discount-tag {
  display: inline-block;
  background: rgba(255, 255, 255, 0.2);
  padding: 4px 8px;
  border-radius: 10px;
  font-size: 12px;
  margin-top: 5px;
}

.card-benefits {
  padding: 25px;
}

.benefit-item {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  font-size: 14px;
}

.benefit-icon {
  color: #ff6b6b;
  margin-right: 10px;
  font-weight: bold;
}

.exclusive-badge {
  background: #ffeaa7;
  padding: 8px 12px;
  border-radius: 8px;
  margin: 15px 0;
  display: flex;
  align-items: center;
  font-size: 14px;
}

.badge-icon {
  margin-right: 8px;
}

.feature-details {
  background: #f8f9fa;
  padding: 15px;
  border-radius: 10px;
  margin-top: 15px;
}

.feature-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 13px;
}

.feature-label {
  color: #666;
}

.feature-value {
  color: #333;
  font-weight: 500;
}

.card-actions {
  padding: 20px;
  padding-top: 0;
  text-align: center;
}

/* 按钮优化 */
.card-button {
  width: 100%;
  padding: 16px;
  border: none;
  color: white;
  font-weight: 400;
  cursor: pointer;
  border-radius: 30px;
  font-size: 1rem;
  transition: var(--transition);
  margin-top: auto; /* 确保按钮在底部 */
}

.card-button:hover:not(:disabled) {
  opacity: 0.95;
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
}

.card-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.save-note {
  color: #ff6b6b;
  font-size: 14px;
  margin-top: 10px;
  font-weight: bold;
}

.current-membership {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 15px;
  padding: 25px;
  margin-top: 40px;
  color: white;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.renew-btn {
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid white;
  color: white;
  padding: 8px 16px;
  border-radius: 20px;
  cursor: pointer;
  transition: background 0.3s;
}

.renew-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}

.membership-status {
  display: flex;
  align-items: center;
}

.status-icon {
  font-size: 60px;
  margin-right: 25px;
}

.status-info {
  flex: 1;
}

.status-tier {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 10px;
}

.status-details {
  display: flex;
  gap: 20px;
  margin-bottom: 15px;
}

.detail-item {
  background: rgba(255, 255, 255, 0.1);
  padding: 8px 12px;
  border-radius: 8px;
}

.vip-privileges {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.privilege-tag {
  background: rgba(255, 255, 255, 0.2);
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
}

.non-vip-prompt {
  background: white;
  border-radius: 15px;
  padding: 30px;
  margin-top: 40px;
  text-align: center;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.prompt-content h3 {
  color: #ff6b6b;
  margin-bottom: 15px;
}

.prompt-benefits {
  list-style: none;
  padding: 0;
  margin: 20px 0;
}

.prompt-benefits li {
  margin: 10px 0;
  color: #666;
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
  border-radius: 15px;
  max-width: 500px;
  width: 90%;
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
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #999;
}

.order-info {
  padding: 20px;
  background: #f8f9fa;
  border-radius: 10px;
  margin: 20px;
}

.order-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
  font-size: 14px;
}

.order-item .label {
  color: #666;
}

.order-item .value {
  font-weight: 500;
}

.price-value {
  color: #ff6b6b;
  font-size: 20px;
  font-weight: bold;
}

.payment-methods {
  padding: 0 20px;
}

.method-list {
  margin: 15px 0;
}

.method-item {
  display: flex;
  align-items: center;
  padding: 15px;
  border: 2px solid #eee;
  border-radius: 10px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: border-color 0.3s;
}

.method-item:hover,
.method-item.selected {
  border-color: #667eea;
}

.method-icon img {
  width: 30px;
  height: 30px;
  margin-right: 15px;
}

.method-info {
  flex: 1;
}

.method-name {
  font-weight: 500;
  margin-bottom: 2px;
}

.method-desc {
  font-size: 12px;
  color: #999;
}

.method-check {
  width: 20px;
  height: 20px;
}

.check-circle {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 2px solid #ddd;
  transition: all 0.3s;
}

.check-circle.checked {
  border-color: #667eea;
  background: #667eea;
  position: relative;
}

.check-circle.checked::after {
  content: '✓';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: white;
  font-size: 12px;
}

.payment-agreement {
  padding: 20px;
  font-size: 14px;
  color: #666;
}

.agreement-checkbox {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.agreement-checkbox input {
  display: none;
}

.checkmark {
  width: 18px;
  height: 18px;
  border: 2px solid #ddd;
  border-radius: 3px;
  margin-right: 10px;
  position: relative;
  transition: all 0.3s;
}

.agreement-checkbox input:checked + .checkmark {
  background: #667eea;
  border-color: #667eea;
}

.agreement-checkbox input:checked + .checkmark::after {
  content: '✓';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: white;
  font-size: 12px;
}

.agreement-link {
  color: #667eea;
  text-decoration: none;
}

.agreement-link:hover {
  text-decoration: underline;
}

.payment-actions {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.pay-button {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  padding: 15px;
  border-radius: 8px;
  font-size: 16px;
  font-weight: bold;
  cursor: pointer;
  transition: opacity 0.3s;
}

.pay-button:hover:not(:disabled) {
  opacity: 0.9;
}

.pay-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.cancel-button {
  background: #f5f5f5;
  color: #666;
  border: 1px solid #ddd;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.3s;
}

.cancel-button:hover {
  background: #e9ecef;
}

.success-modal {
  text-align: center;
  padding: 30px;
}

.success-icon {
  font-size: 60px;
  margin-bottom: 20px;
}

.success-message {
  font-size: 18px;
  color: #333;
  margin-bottom: 20px;
}

.success-details {
  background: #f8f9fa;
  padding: 15px;
  border-radius: 10px;
  margin: 20px 0;
  font-size: 14px;
  text-align: left;
}

.success-details p {
  margin: 5px 0;
}

.success-actions {
  display: flex;
  gap: 10px;
  justify-content: center;
}

.confirm-btn {
  background: #667eea;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 8px;
  cursor: pointer;
}

.view-details-btn {
  background: #f5f5f5;
  color: #666;
  border: 1px solid #ddd;
  padding: 10px 20px;
  border-radius: 8px;
  cursor: pointer;
}

/* 订单历史区域 */
.order-history-section,
.vip-records-section {
  background: white;
  border-radius: 20px;
  width: 95%;
  margin: 0 auto;
  margin-bottom: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.toggle-btn {
  background: #85b2cf;
  border: 1px solid #ddd;
  padding: 5px 15px;
  border-radius: 20px;
  cursor: pointer;
  font-size: 12px;
}

.toggle-btn:hover {
  background: #667eea;
}

/* 订单表格 */
.order-table {
  width: 100%;
  border-collapse: collapse;
}

.order-table th,
.order-table td {
  padding: 12px;
  border-bottom: 1px solid #eee;
  text-align: left;
}

.order-table th {
  background: #fafafa;
  font-weight: 600;
  color: #666;
}

.order-status-0 { color: #fa8c16; }
.order-status-1 { color: #52c41a; }
.order-status-2 { color: #f5222d; }
.order-status-3 { color: #666; }
.order-status-4 { color: #1890ff; }

.cancel-order-btn {
  background: #fff1f0;
  color: #f5222d;
  border: 1px solid #ffa39e;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
}

.success-text {
  color: #52c41a;
  font-size: 12px;
}

/* VIP记录列表 */
.vip-records-list {
  margin-top: 15px;
}

.record-item {
  display: flex;
  align-items: flex-start;
  padding: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.record-item:last-child {
  border-bottom: none;
}

.record-icon {
  font-size: 20px;
  margin-right: 15px;
}

.record-content {
  flex: 1;
}

.record-action {
  font-weight: 500;
  margin-bottom: 4px;
}

.record-vip-type {
  margin-left: 8px;
  background: #f0f0f0;
  padding: 2px 6px;
  border-radius: 10px;
  font-size: 12px;
}

.record-time {
  font-size: 12px;
  color: #999;
  margin-bottom: 4px;
}

.record-remark {
  font-size: 12px;
  color: #666;
  background: #fafafa;
  padding: 4px 8px;
  border-radius: 4px;
  margin-top: 4px;
}

/* 当前会员状态的新样式 */
.action-buttons {
  display: flex;
  gap: 10px;
}

.details-btn {
  background: #f5f5f5;
  color: #666;
  border: 1px solid #ddd;
  padding: 8px 16px;
  border-radius: 20px;
  cursor: pointer;
  transition: background 0.3s;
}

.details-btn:hover {
  background: #e8e8e8;
}

.original-price-small {
  text-decoration: line-through;
  color: #999;
  margin-right: 5px;
  font-size: 14px;
}

/* 非VIP用户提示的新样式 */
.prompt-actions {
  margin-top: 20px;
}

.primary-btn {
  background: linear-gradient(135deg, #ff6b6b 0%, #ff9a9e 100%);
  color: white;
  border: none;
  padding: 12px 24px;
  border-radius: 25px;
  font-size: 16px;
  font-weight: bold;
  cursor: pointer;
  transition: opacity 0.3s;
}

.primary-btn:hover {
  opacity: 0.9;
}

/* 权益详情模态框 */
.benefits-modal {
  max-width: 600px;
}

.benefits-content h4 {
  color: #ff6b6b;
  margin-bottom: 20px;
}

.benefits-list {
  margin-bottom: 25px;
}

.benefit-detail {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
  padding: 10px;
  background: #f8f9fa;
  border-radius: 8px;
}

.benefit-icon {
  margin-right: 12px;
  font-size: 20px;
}

.benefit-text {
  font-size: 15px;
  color: #333;
}

.price-info {
  background: #f5f7fa;
  padding: 15px;
  border-radius: 8px;
  margin-top: 20px;
}

.price-info h5 {
  margin-bottom: 10px;
  color: #666;
}

.price-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 14px;
}

.current-price {
  color: #ff6b6b;
  font-size: 18px;
  font-weight: bold;
}

.original-price {
  text-decoration: line-through;
  color: #999;
}

.discount-info {
  background: #fff7e6;
  color: #fa8c16;
  padding: 6px 12px;
  border-radius: 4px;
  font-size: 13px;
  margin-top: 10px;
  display: inline-block;
}

.loading-benefits {
  text-align: center;
  padding: 40px 0;
}

.spinner.small {
  width: 30px;
  height: 30px;
  border-width: 2px;
}


/* 响应式设计 */
@media (max-width: 768px) {
  .membership-cards {
    flex-direction: column;
    margin-left: 0;
    width: 100%;
  }
  
  .membership-card {
    width: 100%;
    margin-bottom: 20px;
  }
  
  .status-details {
    flex-direction: column;
    gap: 10px;
  }
}

.vip-tabs-container {
  margin-bottom: 10px;
  padding: 0 5px;
}

.vip-tabs {
  display: flex;
  position: relative;
  background: white;
  border-radius: 50px;
  padding: 4px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  max-width: 600px;
  margin: 0 auto;
}

.tab-btn {
  flex: 1;
  padding: 12px 24px;
  border: none;
  background: transparent;
  font-size: 16px;
  font-weight: 600;
  color: #666;
  cursor: pointer;
  transition: all 0.3s ease;
  border-radius: 50px;
  position: relative;
  z-index: 1;
}

.tab-btn:hover {
  color: #667eea;
}

.tab-btn.active {
  color: white;
}

.tab-slider {
  position: absolute;
  top: 4px;
  left: 4px;
  height: calc(100% - 8px);
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50px;
  transition: transform 0.4s cubic-bezier(0.68, -0.55, 0.265, 1.55);
  z-index: 0;
}

/* 选项卡内容样式 */
.tab-content {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 空状态样式 */
.empty-history {
  text-align: center;
  padding: 60px 20px;
  background: white;
  border-radius: 15px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  margin-top: 20px;
}

.empty-icon {
  font-size: 60px;
  margin-bottom: 20px;
}

.empty-history h4 {
  color: #333;
  margin-bottom: 10px;
}

.empty-history p {
  color: #666;
  margin-bottom: 25px;
}

/* 查询按钮样式 */
.query-btn {
  background: #f5f5f5;
  color: #666;
  border: 1px solid #ddd;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  transition: background 0.3s;
}

.query-btn:hover {
  background: #e8e8e8;
}

.current-membership {
  margin-top: 20px;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .vip-tabs {
    flex-direction: column;
    border-radius: 15px;
    padding: 0;
  }
  
  .tab-btn {
    border-radius: 0;
    border-bottom: 1px solid #eee;
  }
  
  .tab-btn:last-child {
    border-bottom: none;
  }
  
  .tab-slider {
    display: none;
  }
  
  .tab-btn.active {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  }
}
</style>