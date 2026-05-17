<template>
  <div class="admin-panel" ref="draggableContainer" :style="panelStyle" :class="{ fullscreen: isFullScreen }">
    <!-- 顶部导航栏 -->
    <nav class="admin-navbar" @mousedown="startDrag">
      <div class="admin-brand">
        <h2>管理员控制台</h2>
        <div class="admin-status">
          <span class="welcome">欢迎 : {{ user.currentUser.username }}</span>
          <span class="role-badge">恵天下</span>
        </div>
      </div>

      <div class="admin-actions">
        <button @click="toggleWindowSize" class="window-control-btn" :title="isFullScreen ? '恢复窗口' : '全屏'">
          <i :class="'iconfont ' + (isFullScreen ? 'icon-shouqi' : 'icon-quanping')"></i>
          <span v-if="!isMobile">{{ isFullScreen ? '' : '' }}</span>
        </button>

        <button @click="closeWindow" class="close-btn">
          <span v-if="!isMobile">关闭</span>
        </button>
      </div>
    </nav>

    <!-- 主内容区 -->
    <div class="admin-container">
      <!-- 侧边栏菜单 -->
      <div class="admin-sidebar">
        <ul class="menu">
          <li v-for="item in menuItems" :key="item.path" :class="{ active: activeMenu === item.path }"
            @click="router.push(`/admin/${item.path}`)">
            <i :class="'iconfont ' + item.icon"></i>
            <span>{{ item.title }}</span>
          </li>
        </ul>
      </div>

      <!-- 内容展示区 -->
      <div class="admin-content">
        <div v-if="activeMenu === 'review'" class="content-section">
          <h3><i class="iconfont icon-review"></i> 作品审核</h3>
          <AdminReview />
        </div>

        <div v-if="activeMenu === 'users'" class="content-section">
          <h3><i class="iconfont icon-user"></i> 用户管理</h3>
          <UserManagement />
        </div>

        <div v-if="activeMenu === 'statistics'" class="content-section">
          <h3><i class="iconfont icon-chart"></i> 数据统计</h3>
          <StatisticsPanel />
        </div>

        <div v-if="activeMenu === 'settings'" class="content-section">
          <h3><i class="iconfont icon-setting"></i> 会员管理</h3>
          <SystemSettings />
        </div>

        <div v-if="activeMenu === 'emoji'" class="content-section">
          <h3><i class="iconfont icon-emoji"></i> 表情包管理</h3>
          <EmojiManagement />
        </div>

        <div v-if="activeMenu === 'dashboard'" class="content-section">
          <h3><i class="iconfont icon-dashboard"></i> 系统仪表盘</h3>
          <Dashboard />
        </div>

        <div v-if="activeMenu === 'logs/operation'" class="content-section">
          <h3><i class="iconfont icon-log"></i> 操作日志</h3>
          <OperationLogs />
        </div>

        <div v-if="activeMenu === 'logs/login'" class="content-section">
          <h3><i class="iconfont icon-record"></i> 登录记录</h3>
          <LoginRecords />
        </div>

        <div v-if="activeMenu === 'system-messages'" class="content-section">
          <h3><i class="iconfont icon-message"></i> 系统消息管理</h3>
          <SystemMessageManagement />
        </div>

        <div v-if="activeMenu === 'feedback'" class="content-section">
          <h3><i class="iconfont icon-feedback"></i> 用户反馈</h3>
          <FeedbackManagement />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useUserInfoStore } from '@/stores/userInfo.js'
import { useRoute, useRouter } from 'vue-router'
import AdminReview from '@/components/admin/AdminReview.vue'
import UserManagement from '@/components/admin/UserManagement.vue'
import StatisticsPanel from '@/components/admin/StatisticsPanel.vue'
import SystemSettings from '@/components/admin/SystemSettings.vue'
import EmojiManagement from '@/components/admin/EmojiManagement.vue'
import Dashboard from '@/components/admin/Dashboard.vue'
import OperationLogs from '@/components/admin/OperationLogs.vue'
import LoginRecords from '@/components/admin/LoginRecords.vue'
import SystemMessageManagement from '@/components/SystemMessageManagement.vue'
import FeedbackManagement from '@/components/admin/FeedbackManagement.vue'

const user = useUserInfoStore()
const route = useRoute()
const router = useRouter()

// 根据路由设置默认激活菜单（支持嵌套路径如 logs/operation）
const activeMenu = computed({
  get: () => {
    const path = route.path
    return path.startsWith('/admin/') ? path.substring(7) : 'review'
  },
  set: (value) => router.push(`/admin/${value}`)
})

// 窗口状态
const isFullScreen = ref(false)
const isTransitioning = ref(false)
const panelStyle = ref({
  top: '10%',
  left: '10%',
  width: '70%',
  height: '80%',
  borderRadius: '20px',
  transition: 'all 0.3s ease'
})

// 响应式判断
const isMobile = ref(false)
onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', checkMobile)
})

const checkMobile = () => {
  isMobile.value = window.innerWidth < 768
}

const toggleWindowSize = () => {
  if (isTransitioning.value) return

  isTransitioning.value = true

  if (isFullScreen.value) {
    // 恢复到半屏
    panelStyle.value = {
      top: '10%',
      left: '10%',
      width: '70%',
      height: '80%',
      borderRadius: '20px',
      transition: 'all 0.3s ease'
    }
  } else {
    // 切换到全屏
    panelStyle.value = {
      top: '0',
      left: '0',
      width: '100vw',
      height: '100vh',
      borderRadius: '0',
      transition: 'all 0.3s ease'
    }
  }

  setTimeout(() => {
    isFullScreen.value = !isFullScreen.value
    isTransitioning.value = false
  }, 300)
}

// 拖拽相关状态
const draggableContainer = ref(null)
const isDragging = ref(false)
const startPos = ref({ x: 0, y: 0 })
const currentPos = ref({ x: 0, y: 0 })

const startDrag = (e) => {
  if (e.target.closest('.admin-navbar') && !isFullScreen.value) {
    isDragging.value = true
    startPos.value = {
      x: e.clientX,
      y: e.clientY
    }
    currentPos.value = {
      x: draggableContainer.value.offsetLeft,
      y: draggableContainer.value.offsetTop
    }

    document.addEventListener('mousemove', handleDrag)
    document.addEventListener('mouseup', stopDrag)
    e.preventDefault()
  }
}

const handleDrag = (e) => {
  if (isDragging.value) {
    const dx = e.clientX - startPos.value.x
    const dy = e.clientY - startPos.value.y

    draggableContainer.value.style.left = `${currentPos.value.x + dx}px`
    draggableContainer.value.style.top = `${currentPos.value.y + dy}px`
  }
}

const stopDrag = () => {
  isDragging.value = false
  document.removeEventListener('mousemove', handleDrag)
  document.removeEventListener('mouseup', stopDrag)
}

onBeforeUnmount(() => {
  stopDrag()
})

const menuItems = [
  { path: 'review', title: '作品审核', icon: 'icon-review' },
  { path: 'users', title: '用户管理', icon: 'icon-user' },
  { path: 'emoji', title: '表情包管理', icon: 'icon-emoji' },
  { path: 'statistics', title: '数据统计', icon: 'icon-chart' },
  { path: 'settings', title: '会员管理', icon: 'icon-setting' },
  { path: 'dashboard', title: '系统仪表盘', icon: 'icon-dashboard' },
  { path: 'logs/operation', title: '操作日志', icon: 'icon-log' },
  { path: 'logs/login', title: '登录记录', icon: 'icon-record' },
  { path: 'system-messages', title: '系统消息管理', icon: 'icon-message' },
  { path: 'feedback', title: '用户反馈', icon: 'icon-feedback' }
]

const closeWindow = () => {
  router.push('/')
}
</script>

<style scoped>
.admin-panel {
  position: fixed;
  top: 10%;
  left: 10%;
  width: 70%;
  height: 80%;
  display: flex;
  flex-direction: column;
  background-color: #99cfd6;
  border-radius: 20px;
  overflow: hidden;
  z-index: 1000;
  /* 添加不可选中文本防止拖拽时选中文字 */
  user-select: none;
  /* 固定尺寸 */
  resize: none;
}

.admin-navbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 2rem;
  height: 60px;
  background: linear-gradient(135deg, #fa94c3 0%, #50b9b4 100%);
  color: white;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  /* 添加拖拽光标提示 */
  cursor: move;
  /* 防止navbar内部元素干扰拖拽 */
  -webkit-app-region: drag;
}

.admin-brand {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.admin-brand h2 {
  margin: 0;
  font-size: 1.5rem;
}

.admin-status {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.welcome {
  font-size: 0.9rem;
  opacity: 0.8;
}

.role-badge {
  background-color: #d63384;
  padding: 0.2rem 0.5rem;
  border-radius: 1rem;
  font-size: 0.8rem;
  font-weight: bold;
}

.close-btn {
  background: none;
  font-weight: bold;
  font-size: 1rem;
  border: none;
  color: rgb(230, 20, 149);
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border-radius: 4px;
  transition: background 0.3s;
  -webkit-app-region: no-drag;
}

.close-btn:hover {
  background: rgba(20, 228, 176, 0.3);
}

.admin-container {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.admin-sidebar {
  width: 220px;
  background-color: rgba(243, 168, 218, 0.7);
  border-right: 1px solid #eaeaea;
  padding: 1rem 0;
}

.menu {
  list-style: none;
  padding: 0;
  margin: 0;
}

.menu li {
  padding: 0.8rem 1.5rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 0.8rem;
  transition: all 0.3s;
}

.menu li:hover {
  background-color: #f0f2f5;
}

.menu li.active {
  background-color: #e6f7ff;
  color: #1890ff;
  border-right: 3px solid #1890ff;
}

.menu li .iconfont {
  font-size: 1.1rem;
}

.admin-content {
  flex: 1;
  padding: 2rem;
  overflow-y: auto;
}

.content-section {
  background: white;
  border-radius: 8px;
  padding: 1.5rem;
  box-shadow: 5px 5px 30px rgba(16, 224, 96, 0.5);
}

.content-section h3 {
  margin-top: 0;
  margin-bottom: 1.5rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: #2c3e50;
}

/* 全屏样式 */
.admin-panel.fullscreen {
  border-radius: 0 !important;
  box-shadow: none !important;
}

/* 窗口控制按钮组 */
.admin-actions {
  display: flex;
  gap: 0.5rem;
}

.window-control-btn {
  display: flex;
  align-items: center;
  background: none;
  border: none;
  padding: 0;
  cursor: pointer;
  outline: none;
  background-color: rgba(0, 0, 0, 0.1);
  opacity: 0.2;
  transition: opacity 0.3s;
}

.window-control-btn:hover {
  opacity: 1;
}

.window-control-btn .iconfont {
  font-size: 1.2rem;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .admin-actions span {
    display: none;
  }

  .admin-panel:not(.fullscreen) {
    top: 5% !important;
    left: 5% !important;
    width: 90% !important;
    height: 90% !important;
  }
}
</style>