<template>
  <div class="statistics-panel">
    <!-- 数据加载状态 -->
    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>加载统计数据中...</p>
    </div>
    
    <!-- 统计卡片网格 -->
    <div class="stats-grid">
      <!-- 总用户数 -->
      <div class="stat-card">
        <div class="stat-header">
          <h4>总用户数</h4>
          <i class="stat-icon user-icon">👥</i>
        </div>
        <div class="stat-value">{{ formatNumber(stats.totalUsers) }}</div>
        <div class="stat-change" :class="getTrendClass(stats.userGrowthRate)">
          <span class="trend-arrow">
            {{ stats.userGrowthRate >= 0 ? '↑' : '↓' }}
          </span>
          {{ Math.abs(stats.userGrowthRate) }}% 本月
        </div>
        <div class="stat-detail">
          <span>今日新增: {{ stats.todayNewUsers || 0 }}</span>
          <span>本月新增: {{ stats.monthNewUsers || 0 }}</span>
        </div>
      </div>
      
      <!-- 活跃用户 -->
      <div class="stat-card">
        <div class="stat-header">
          <h4>活跃用户</h4>
          <i class="stat-icon active-icon">🔥</i>
        </div>
        <div class="stat-value">{{ formatNumber(stats.activeUsers) }}</div>
        <div class="stat-change" :class="getTrendClass(stats.activeUserRate)">
          <span class="trend-arrow">
            {{ stats.activeUserRate >= 0 ? '↑' : '↓' }}
          </span>
          {{ Math.abs(stats.activeUserRate) }}% 占比
        </div>
        <div class="stat-detail">
          <span>7日活跃: {{ stats.weeklyActiveUsers || 0 }}</span>
          <span>30日活跃: {{ stats.monthlyActiveUsers || 0 }}</span>
        </div>
      </div>
      
      <!-- VIP用户 -->
      <div class="stat-card">
        <div class="stat-header">
          <h4>VIP用户</h4>
          <i class="stat-icon vip-icon">⭐</i>
        </div>
        <div class="stat-value">{{ formatNumber(stats.vipUsers) }}</div>
        <div class="stat-change" :class="getTrendClass(stats.vipGrowthRate)">
          <span class="trend-arrow">
            {{ stats.vipGrowthRate >= 0 ? '↑' : '↓' }}
          </span>
          {{ Math.abs(stats.vipGrowthRate) }}% 本月
        </div>
        <div class="stat-detail">
          <div class="vip-distribution">
            <span v-for="vip in vipDistribution" :key="vip.type" 
                  :class="`vip-count vip-${vip.type}`">
              {{ vip.typeName }}: {{ vip.count }}
            </span>
          </div>
        </div>
      </div>
      
      <!-- 封禁用户 -->
      <div class="stat-card">
        <div class="stat-header">
          <h4>封禁用户</h4>
          <i class="stat-icon banned-icon">🚫</i>
        </div>
        <div class="stat-value">{{ formatNumber(stats.bannedUsers) }}</div>
        <div class="stat-change" :class="getTrendClass(stats.bannedChange)">
          <span class="trend-arrow">
            {{ stats.bannedChange >= 0 ? '↑' : '↓' }}
          </span>
          {{ Math.abs(stats.bannedChange) }}% 变化
        </div>
        <div class="stat-detail">
          <span>今日封禁: {{ stats.todayBanned || 0 }}</span>
          <span>本月封禁: {{ stats.monthBanned || 0 }}</span>
        </div>
      </div>
    </div>
    
   <div class="charts-container">
    <!-- 用户增长趋势图 -->
    <div class="chart-card">
      <div class="chart-header">
        <h4>用户增长趋势</h4>
        <div class="time-filter">
          <button :class="{ active: timeRange === 'week' }" @click="changeTimeRange('week')">
            近7天
          </button>
          <button :class="{ active: timeRange === 'month' }" @click="changeTimeRange('month')">
            近30天
          </button>
          <button :class="{ active: timeRange === 'quarter' }" @click="changeTimeRange('quarter')">
            近90天
          </button>
        </div>
      </div>
      <div class="chart-wrapper">
        <div class="simple-chart" v-if="growthData.length > 0">
          <div class="chart-bars">
            <div v-for="item in growthData" :key="item.date" class="bar-container">
              <div class="bar-label">{{ formatDateLabel(item.date) }}</div>
              <div class="bar-wrapper">
                <div class="bar" :style="{ height: getBarHeight(item.newUsers) + 'px' }"
                     :title="'新增: ' + item.newUsers">
                  <span class="bar-value">{{ item.newUsers }}</span>
                </div>
              </div>
            </div>
          </div>
          <div class="chart-legend">
            <div class="legend-item">
              <span class="legend-color new-user"></span>
              <span>新增用户</span>
            </div>
          </div>
        </div>
        <div v-else class="chart-placeholder">暂无数据</div>
      </div>
    </div>
   
    
    <!-- 活跃用户趋势图 -->
    <div class="chart-card">
      <div class="chart-header">
        <h4>活跃用户趋势</h4>
      </div>
      <div class="chart-wrapper">
        <div class="simple-chart" v-if="activeTrendData.length > 0">
          <div class="chart-bars">
            <div v-for="item in activeTrendData" :key="item.date" class="bar-container">
              <div class="bar-label">{{ formatDateLabel(item.date) }}</div>
              <div class="bar-wrapper">
                <div class="bar active-bar" :style="{ height: getActiveBarHeight(item.activeUsers) + 'px' }"
                     :title="'活跃: ' + item.activeUsers">
                  <span class="bar-value">{{ item.activeUsers }}</span>
                </div>
              </div>
            </div>
          </div>
          <div class="chart-legend">
            <div class="legend-item">
              <span class="legend-color active-user"></span>
              <span>活跃用户</span>
            </div>
          </div>
        </div>
        <div v-else class="chart-placeholder">暂无数据</div>
      </div>
    </div>
  </div>
      
      <!-- 用户分布饼图 -->
      <div class="chart-card">
        <div class="chart-header">
          <h4>用户状态分布</h4>
        </div>
        <div class="chart-wrapper">
          <div v-if="statusDistribution.length > 0" class="distribution-pie">
            <div class="pie-chart" v-if="statusDistribution.length > 0">
              <svg width="200" height="200" viewBox="0 0 200 200">
                <circle cx="100" cy="100" r="80" fill="transparent" stroke="#f0f0f0" stroke-width="40"/>
                <circle cx="100" cy="100" r="80" fill="transparent" 
                        :stroke="statusDistribution[0]?.color || '#1890ff'" 
                        stroke-width="40"
                        :stroke-dasharray="getDashArray(statusDistribution[0]?.percentage || 0)"
                        stroke-dashoffset="25"/>
                <circle cx="100" cy="100" r="80" fill="transparent" 
                        :stroke="statusDistribution[1]?.color || '#52c41a'" 
                        stroke-width="40"
                        :stroke-dasharray="getDashArray(statusDistribution[1]?.percentage || 0)"
                        :stroke-dashoffset="25 - (statusDistribution[0]?.percentage || 0) * 2.5"/>
                <circle cx="100" cy="100" r="80" fill="transparent" 
                        :stroke="statusDistribution[2]?.color || '#f5222d'" 
                        stroke-width="40"
                        :stroke-dasharray="getDashArray(statusDistribution[2]?.percentage || 0)"
                        :stroke-dashoffset="25 - ((statusDistribution[0]?.percentage || 0) + 
                                                  (statusDistribution[1]?.percentage || 0)) * 2.5"/>
              </svg>
              <div class="pie-center">
                <div class="pie-total">{{ stats.totalUsers }}</div>
                <div class="pie-label">总用户</div>
              </div>
            </div>
            <div class="pie-legend">
              <div v-for="item in statusDistribution" :key="item.status" class="legend-item">
                <span class="legend-color" :style="{ background: item.color }"></span>
                <span class="legend-label">{{ item.label }}</span>
                <span class="legend-value">{{ item.count }} ({{ item.percentage }}%)</span>
              </div>
            </div>
          </div>
          <div v-else class="chart-placeholder">暂无数据</div>
        </div>
      </div>
    </div>
    
    <!-- 数据表格区域 -->
    <div class="data-table">
      <h4>详细统计数据</h4>
      <table class="stats-table">
        <thead>
          <tr>
            <th>指标</th>
            <th>数值</th>
            <th>变化率</th>
            <th>趋势</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td>平均日活跃</td>
            <td>{{ stats.dailyActiveAvg || 0 }}</td>
            <td>{{ formatPercentage(stats.dailyActiveChange || 0) }}</td>
            <td>
              <span :class="getTrendClass(stats.dailyActiveChange || 0)">
                {{ stats.dailyActiveChange >= 0 ? '↑' : '↓' }}
              </span>
            </td>
          </tr>
          <tr>
            <td>用户留存率</td>
            <td>{{ formatPercentage(stats.userRetention || 0) }}</td>
            <td>{{ formatPercentage(stats.retentionChange || 0) }}</td>
            <td>
              <span :class="getTrendClass(stats.retentionChange || 0)">
                {{ stats.retentionChange >= 0 ? '↑' : '↓' }}
              </span>
            </td>
          </tr>
          <tr>
            <td>VIP转化率</td>
            <td>{{ formatPercentage(stats.vipConversion || 0) }}</td>
            <td>{{ formatPercentage(stats.conversionChange || 0) }}</td>
            <td>
              <span :class="getTrendClass(stats.conversionChange || 0)">
                {{ stats.conversionChange >= 0 ? '↑' : '↓' }}
              </span>
            </td>
          </tr>
          <tr>
            <td>平均等级</td>
            <td>{{ stats.avgLevel?.toFixed(1) || 0 }}</td>
            <td>{{ formatPercentage(stats.levelChange || 0) }}</td>
            <td>
              <span :class="getTrendClass(stats.levelChange || 0)">
                {{ stats.levelChange >= 0 ? '↑' : '↓' }}
              </span>
            </td>
          </tr>
          <tr>
            <td>平均积分</td>
            <td>{{ formatNumber(stats.avgCoins || 0) }}</td>
            <td>{{ formatPercentage(stats.coinsChange || 0) }}</td>
            <td>
              <span :class="getTrendClass(stats.coinsChange || 0)">
                {{ stats.coinsChange >= 0 ? '↑' : '↓' }}
              </span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    
    <!-- 刷新按钮和时间戳 -->
    <div class="footer-info">
      <button @click="refreshData" class="refresh-btn" :disabled="loading">
        {{ loading ? '刷新中...' : '刷新数据' }}
      </button>
      <span class="update-time">
        最后更新: {{ lastUpdateTime || '--' }}
      </span>
    </div>

</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getUserStatsService, getRealTimeStatisticsService, getUserGrowthTrendService, getActiveUserTrendService } from '@/api/adminUser'

// 响应式数据
const loading = ref(false)
const stats = ref({})
const timeRange = ref('month')
const lastUpdateTime = ref('')
const growthData = ref([])
const activeTrendData = ref([])  // 新增活跃趋势数据

// VIP分布计算属性
const vipDistribution = computed(() => {
  const vipTypeCounts = stats.value.vipTypeCounts || {}
  return [
    { type: 1, typeName: '月度VIP', count: vipTypeCounts[1] || 0 },
    { type: 2, typeName: '季度VIP', count: vipTypeCounts[2] || 0 },
    { type: 3, typeName: '年度VIP', count: vipTypeCounts[3] || 0 },
    { type: 4, typeName: '终身VIP', count: vipTypeCounts[4] || 0 }
  ].filter(item => item.count > 0)
})

// 用户状态分布计算属性
const statusDistribution = computed(() => {
  const result = []
  
  // 正常用户
  if (stats.value.activeUsers > 0) {
    result.push({
      status: 'active',
      label: '正常用户',
      count: stats.value.activeUsers,
      percentage: ((stats.value.activeUsers / stats.value.totalUsers) * 100).toFixed(1),
      color: '#52c41a'
    })
  }
  
  // 封禁用户
  if (stats.value.bannedUsers > 0) {
    result.push({
      status: 'banned',
      label: '封禁用户',
      count: stats.value.bannedUsers,
      percentage: ((stats.value.bannedUsers / stats.value.totalUsers) * 100).toFixed(1),
      color: '#f5222d'
    })
  }
 
  return result
})

// 页面加载时获取统计数据
onMounted(() => {
  loadStatistics()
})

/**
 * 加载统计数据
 */
const loadStatistics = async () => {
  try {
    loading.value = true
    
    // 1. 获取实时统计信息
    const statsResponse = await getRealTimeStatisticsService()
    if (statsResponse.code === 0) {
      stats.value = statsResponse.data || {}
      lastUpdateTime.value = new Date().toLocaleString('zh-CN')
    } else {
      console.error('获取统计数据失败:', statsResponse.message)
      setDefaultStats()
    }
    
    // 2. 获取增长趋势数据
    const trendResponse = await getUserGrowthTrendService(30)
    if (trendResponse.code === 0) {
      const trendData = trendResponse.data.trendData || []
      growthData.value = trendData.map(item => ({
        date: item.date,
        newUsers: item.count || 0
      }))
    } else {
      // 如果新的接口失败，尝试使用旧的接口
      await loadGrowthDataFromOldApi()
    }
    
    // 3. 获取活跃用户趋势数据
    const activeResponse = await getActiveUserTrendService(30)
    if (activeResponse.code === 0) {
      const trendData = activeResponse.data.trendData || []
      activeTrendData.value = trendData.map(item => ({
        date: item.date,
        activeUsers: item.count || 0
      }))
    }
    
  } catch (error) {
    console.error('获取统计数据异常:', error)
    setDefaultStats()
  } finally {
    loading.value = false
  }
}

/**
 * 从旧接口获取增长数据（兼容性处理）
 */
const loadGrowthDataFromOldApi = async () => {
  try {
    const response = await getUserStatsService()
    if (response.code === 0) {
      // 这里可以根据旧接口的数据格式进行处理
      stats.value = response.data || {}
      console.log('使用旧接口获取统计数据')
    }
  } catch (error) {
    console.error('从旧接口获取数据失败:', error)
  }
}

/**
 * 修改时间范围
 */
const changeTimeRange = (range) => {
  timeRange.value = range
  loadTrendData(range)
}

/**
 * 加载趋势数据
 */
const loadTrendData = async (range) => {
  const days = range === 'week' ? 7 : range === 'month' ? 30 : 90
  
  try {
    // 加载增长趋势
    const growthResponse = await getUserGrowthTrendService(days)
    if (growthResponse.code === 0) {
      growthData.value = growthResponse.data.trendData || []
    }
    
    // 加载活跃趋势
    const activeResponse = await getActiveUserTrendService(days)
    if (activeResponse.code === 0) {
      activeTrendData.value = activeResponse.data.trendData || []
    }
  } catch (error) {
    console.error('加载趋势数据失败:', error)
  }
}

/**
 * 刷新数据
 */
const refreshData = () => {
  loadStatistics()
}

/**
 * 格式化数字（添加千分位）
 * @param {number} num - 要格式化的数字
 * @returns {string} 格式化后的字符串
 */
const formatNumber = (num) => {
  if (num === undefined || num === null) return '0'
  return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

/**
 * 格式化百分比
 * @param {number} num - 小数表示的百分比
 * @returns {string} 格式化后的百分比
 */
const formatPercentage = (num) => {
  if (num === undefined || num === null) return '0%'
  return (num * 100).toFixed(1) + '%'
}

/**
 * 获取趋势样式类名
 * @param {number} value - 变化值
 * @returns {string} CSS类名
 */
const getTrendClass = (value) => {
  if (value > 0) return 'up'
  if (value < 0) return 'down'
  return 'neutral'
}

/**
 * 格式化日期标签
 * @param {string} dateString - 日期字符串
 * @returns {string} 格式化后的标签
 */
const formatDateLabel = (dateString) => {
  const date = new Date(dateString)
  return timeRange.value === 'week' 
    ? date.getDate().toString() + '日'
    : (date.getMonth() + 1) + '/' + date.getDate()
}

/**
 * 计算条形图高度
 * @param {number} value - 数值
 * @returns {number} 条形图高度
 */
const getBarHeight = (value) => {
  const maxValue = Math.max(...growthData.value.map(item => item.newUsers))
  return maxValue > 0 ? (value / maxValue) * 150 : 0
}

/**
 * 计算饼图dasharray值
 * @param {number} percentage - 百分比
 * @returns {string} dasharray字符串
 */
const getDashArray = (percentage) => {
  const circumference = 2 * Math.PI * 80
  const length = (percentage / 100) * circumference
  return `${length} ${circumference}`
}
</script>

<style scoped>
.statistics-panel {
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;
}

/* 加载状态 */
.loading-state {
  text-align: center;
  padding: 40px;
  background: white;
  border-radius: 8px;
  margin-bottom: 20px;
}

.loading-state .spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid #1890ff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 10px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 统计卡片网格 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

.stat-card {
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s, box-shadow 0.3s;
}

.stat-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.stat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.stat-header h4 {
  margin: 0;
  font-size: 14px;
  color: #666;
  font-weight: 500;
}

.stat-icon {
  font-size: 24px;
}

.user-icon { color: #1890ff; }
.active-icon { color: #fa8c16; }
.vip-icon { color: #722ed1; }
.banned-icon { color: #f5222d; }

.stat-value {
  font-size: 32px;
  font-weight: bold;
  margin: 10px 0;
  color: #333;
}

.stat-change {
  font-size: 14px;
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 5px;
}

.stat-change.up {
  color: #52c41a;
}

.stat-change.down {
  color: #f5222d;
}

.stat-change.neutral {
  color: #666;
}

.trend-arrow {
  font-weight: bold;
}

.stat-detail {
  font-size: 12px;
  color: #999;
  display: flex;
  justify-content: space-between;
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px solid #f0f0f0;
}

.vip-distribution {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 5px;
}

.vip-count {
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 3px;
  background: #f5f5f5;
  color: #666;
}

.vip-count.vip-1 { background: #e6f7ff; color: #1890ff; }
.vip-count.vip-2 { background: #f6ffed; color: #52c41a; }
.vip-count.vip-3 { background: #fff7e6; color: #fa8c16; }
.vip-count.vip-4 { background: #f9f0ff; color: #722ed1; }

/* 图表区域 */
.charts-container {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

.chart-card {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.chart-header {
  padding: 20px 20px 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-header h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
}

.time-filter {
  display: flex;
  gap: 5px;
}

.time-filter button {
  padding: 4px 8px;
  border: 1px solid #ddd;
  background: white;
  border-radius: 3px;
  font-size: 12px;
  cursor: pointer;
}

.time-filter button.active {
  background: #1890ff;
  color: white;
  border-color: #1890ff;
}

.chart-wrapper {
  padding: 20px;
}

.simple-chart {
  height: 300px;
}

.chart-bars {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  height: 200px;
  padding: 20px 0;
  border-bottom: 1px solid #f0f0f0;
}

.bar-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
}

.bar-label {
  font-size: 12px;
  color: #666;
  margin-bottom: 10px;
}

.bar-wrapper {
  flex: 1;
  display: flex;
  align-items: flex-end;
  width: 100%;
  padding: 0 5px;
}

.bar {
  width: 100%;
  background: linear-gradient(to top, #1890ff, #69c0ff);
  border-radius: 3px 3px 0 0;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  position: relative;
  transition: height 0.3s;
}

.bar:hover {
  opacity: 0.8;
}

.bar-value {
  position: absolute;
  top: -20px;
  font-size: 12px;
  color: #666;
}

.chart-legend {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #666;
}

.legend-color {
  width: 12px;
  height: 12px;
  border-radius: 2px;
}

.legend-color.new-user {
  background: linear-gradient(to top, #1890ff, #69c0ff);
}

.chart-placeholder {
  height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  font-size: 14px;
}

/* 饼图样式 */
.distribution-pie {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 40px;
}

.pie-chart {
  position: relative;
  width: 200px;
  height: 200px;
}

.pie-center {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
}

.pie-total {
  font-size: 24px;
  font-weight: bold;
  color: #333;
}

.pie-label {
  font-size: 12px;
  color: #999;
  margin-top: 5px;
}

.pie-legend {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.pie-legend .legend-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.legend-color {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  flex-shrink: 0;
}

.legend-label {
  font-size: 14px;
  color: #333;
  width: 80px;
}

.legend-value {
  font-size: 14px;
  color: #666;
}

/* 数据表格 */
.data-table {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 20px;
  margin-bottom: 20px;
}

.data-table h4 {
  margin: 0 0 20px 0;
  font-size: 16px;
  font-weight: 500;
}

.stats-table {
  width: 100%;
  border-collapse: collapse;
}

.stats-table th,
.stats-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #f0f0f0;
}

.stats-table th {
  font-weight: 500;
  color: #666;
  background: #fafafa;
}

.stats-table tbody tr:hover {
  background: #fafafa;
}

.stats-table .up {
  color: #52c41a;
  font-weight: bold;
}

.stats-table .down {
  color: #f5222d;
  font-weight: bold;
}

.stats-table .neutral {
  color: #666;
}

/* 页脚信息 */
.footer-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 0;
}

.refresh-btn {
  padding: 8px 16px;
  background: #1890ff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.refresh-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.update-time {
  color: #999;
  font-size: 14px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .stats-grid,
  .charts-container {
    grid-template-columns: 1fr;
  }
  
  .charts-container {
    grid-template-columns: 1fr;
  }
  
  .distribution-pie {
    flex-direction: column;
    gap: 20px;
  }
  
  .footer-info {
    flex-direction: column;
    gap: 10px;
    align-items: flex-start;
  }
  
  .stats-table {
    display: block;
    overflow-x: auto;
  }
}
</style>