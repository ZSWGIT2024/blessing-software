<template>
  <div class="dashboard-page">
    <!-- 指标卡片行 -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-value">{{ userStats.totalUsers || 0 }}</div>
        <div class="stat-label">总用户数</div>
        <div class="stat-sub">今日新增 {{ userStats.todayNewUsers || 0 }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ loginStats.todayLogins || todayData.todayLogins || 0 }}</div>
        <div class="stat-label">今日登录</div>
        <div class="stat-sub">活跃用户 {{ loginStats.activeUsers || 0 }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ userStats.onlineUsers || 0 }}</div>
        <div class="stat-label">在线用户</div>
        <div class="stat-sub">VIP用户 {{ userStats.vipUsers || 0 }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ loginStats.failRate || '0%' }}</div>
        <div class="stat-label">登录失败率</div>
        <div class="stat-sub">失败 {{ loginStats.todayFails || 0 }} 次</div>
      </div>
    </div>

    <!-- 数据面板行 -->
    <div class="panels-row">
      <!-- 用户统计面板 -->
      <div class="panel">
        <div class="panel-header">
          <h4>用户统计</h4>
          <button @click="refreshAll" :disabled="loading" class="refresh-btn">刷新</button>
        </div>
        <div class="panel-body" v-if="userStats">
          <div class="data-row" v-for="(v, k) in userStats" :key="'u-'+k">
            <span class="data-label">{{ formatKey(k) }}</span>
            <span class="data-value">{{ v }}</span>
          </div>
        </div>
        <div v-else class="panel-empty">暂无数据</div>
      </div>

      <!-- 登录统计面板 -->
      <div class="panel">
        <div class="panel-header">
          <h4>登录统计</h4>
        </div>
        <div class="panel-body" v-if="loginStats">
          <div class="data-row" v-for="(v, k) in loginStats" :key="'l-'+k">
            <span class="data-label">{{ formatKey(k) }}</span>
            <span class="data-value">{{ typeof v === 'object' ? '' : v }}</span>
          </div>
        </div>
        <div v-else class="panel-empty">暂无数据</div>
      </div>
    </div>

    <div class="panels-row">
      <!-- 操作日志概览 -->
      <div class="panel">
        <div class="panel-header">
          <h4>操作日志概览</h4>
        </div>
        <div class="panel-body" v-if="logStats">
          <div class="tab-header">
            <button :class="{ active: logTab === 'module' }" @click="logTab = 'module'">模块统计</button>
            <button :class="{ active: logTab === 'fail' }" @click="logTab = 'fail'">失败统计</button>
          </div>
          <div v-if="logTab === 'module'">
            <div class="data-row" v-for="(v, k) in logStats.moduleStats || {}" :key="'m-'+k">
              <span class="data-label">{{ k || '未分类' }}</span>
              <span class="badge">{{ v }} 次</span>
            </div>
          </div>
          <div v-if="logTab === 'fail'">
            <div class="data-row" v-for="(v, k) in logStats.failStats || {}" :key="'f-'+k">
              <span class="data-label">{{ k }}</span>
              <span class="data-value error">{{ v }}</span>
            </div>
          </div>
        </div>
        <div v-else class="panel-empty">暂无数据</div>
      </div>

      <!-- 系统信息 -->
      <div class="panel">
        <div class="panel-header">
          <h4>系统信息</h4>
        </div>
        <div class="panel-body" v-if="systemInfo">
          <div v-if="systemInfo.jvm">
            <div class="data-row" v-for="(v, k) in systemInfo.jvm" :key="'jvm-'+k">
              <span class="data-label">JVM {{ formatKey(k) }}</span>
              <span class="data-value">{{ v }}</span>
            </div>
          </div>
          <div v-if="systemInfo.system">
            <div class="data-row" v-for="(v, k) in systemInfo.system" :key="'sys-'+k">
              <span class="data-label">{{ formatKey(k) }}</span>
              <span class="data-value">{{ v }}</span>
            </div>
          </div>
        </div>
        <div v-else class="panel-empty">暂无数据</div>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-overlay">
      <div class="spinner"></div>
      <p>加载中...</p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { adminGetDashboardService } from '@/api/adminLog'
import { adminGetSystemInfoService } from '@/api/adminUser'

const loading = ref(false)
const logTab = ref('module')

const userStats = ref({})
const loginStats = ref({})
const logStats = ref({})
const todayData = ref({})
const systemInfo = ref(null)

onMounted(() => refreshAll())

async function refreshAll() {
  loading.value = true
  try {
    const [dashRes, sysRes] = await Promise.allSettled([
      adminGetDashboardService(),
      adminGetSystemInfoService()
    ])
    if (dashRes.status === 'fulfilled' && dashRes.value?.code === 0) {
      const d = dashRes.value.data
      userStats.value = d.userStats || {}
      loginStats.value = d.loginStats || {}
      logStats.value = d.logStats || {}
      todayData.value = d.todayData || {}
    }
    if (sysRes.status === 'fulfilled' && sysRes.value?.code === 0) {
      systemInfo.value = sysRes.value.data
    }
  } finally {
    loading.value = false
  }
}

function formatKey(key) {
  const map = {
    totalUsers: '总用户数', todayNewUsers: '今日新增', activeUsers: '活跃用户',
    bannedUsers: '封禁用户', adminUsers: '管理员数', vipUsers: 'VIP用户',
    onlineUsers: '在线用户', todayLogins: '今日登录', todayFails: '今日失败',
    failRate: '失败率', avgLoginPerDay: '日均登录',
    totalMemory: '总内存', freeMemory: '空闲内存', maxMemory: '最大内存',
    availableProcessors: '可用处理器',
    osName: '操作系统', osVersion: '系统版本', javaVersion: 'Java版本', serverTime: '服务器时间'
  }
  return map[key] || key
}
</script>

<style scoped>
.dashboard-page { padding: 0; }

/* 指标卡片 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}
.stat-card {
  background: white;
  border-radius: 8px;
  padding: 20px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}
.stat-value { font-size: 28px; font-weight: 700; color: #1890ff; }
.stat-label { font-size: 14px; color: #666; margin-top: 4px; }
.stat-sub { font-size: 12px; color: #999; margin-top: 4px; }

/* 面板 */
.panels-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}
.panel {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  overflow: hidden;
}
.panel-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 14px 20px; border-bottom: 1px solid #f0f0f0;
}
.panel-header h4 { margin: 0; font-size: 15px; color: #333; }
.refresh-btn {
  padding: 4px 12px; border: 1px solid #ddd; background: white;
  border-radius: 4px; cursor: pointer; font-size: 12px;
}
.panel-body { padding: 12px 20px; }
.panel-empty { padding: 40px; text-align: center; color: #999; }

/* 数据行 */
.data-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 8px 0; border-bottom: 1px solid #f5f5f5;
}
.data-label { color: #666; font-size: 13px; }
.data-value { color: #333; font-weight: 500; }
.data-value.error { color: #f5222d; }
.badge { background: #e6f7ff; color: #1890ff; padding: 2px 8px; border-radius: 10px; font-size: 12px; }

/* Tab */
.tab-header { display: flex; gap: 12px; margin-bottom: 10px; }
.tab-header button {
  padding: 4px 12px; border: 1px solid #ddd; background: white;
  border-radius: 4px; cursor: pointer; font-size: 12px;
}
.tab-header button.active { background: #1890ff; color: white; border-color: #1890ff; }

/* 加载 */
.loading-overlay {
  display: flex; flex-direction: column; align-items: center; padding: 40px;
}
.spinner {
  width: 36px; height: 36px; border: 3px solid #f3f3f3;
  border-top: 3px solid #1890ff; border-radius: 50%;
  animation: spin 1s linear infinite;
}
@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style>
