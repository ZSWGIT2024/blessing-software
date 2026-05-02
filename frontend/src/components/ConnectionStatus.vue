<!-- src/components/ConnectionStatus.vue -->
<template>
  <Transition name="slide">
    <div v-if="show" :class="['connection-status', status]">
      <span v-if="status === 'connected'">🟢 实时消息已连接</span>
      <span v-else-if="status === 'disconnected'">🔴 实时消息已断开，正在重连...</span>
      <span v-else-if="status === 'error'">⚠️ 连接错误，请刷新页面</span>
    </div>
  </Transition>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import websocketService from '@/utils/websocket'
import { useUserInfoStore } from '@/stores/userInfo'

const userStore = useUserInfoStore()
const show = ref(false)
const status = ref('disconnected')
let hideTimer = null

onMounted(() => {
  websocketService.onStatusChange((newStatus) => {
    status.value = newStatus
    show.value = true
    
    if (hideTimer) clearTimeout(hideTimer)
    
    if (newStatus === 'connected') {
      hideTimer = setTimeout(() => {
        show.value = false
      }, 3000)
    }
  })
})

onUnmounted(() => {
  if (hideTimer) clearTimeout(hideTimer)
})
</script>

<style scoped>
.connection-status {
  position: fixed;
  bottom: 20px;
  right: 20px;
  padding: 10px 20px;
  border-radius: 30px;
  font-size: 13px;
  font-weight: 500;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 9999;
}

.connection-status.connected {
  background: linear-gradient(135deg, #4caf50, #45a049);
  color: white;
}

.connection-status.disconnected {
  background: linear-gradient(135deg, #ff9800, #f57c00);
  color: white;
}

.connection-status.error {
  background: linear-gradient(135deg, #f44336, #d32f2f);
  color: white;
}

.slide-enter-active,
.slide-leave-active {
  transition: all 0.3s ease;
}

.slide-enter-from,
.slide-leave-to {
  transform: translateX(100%);
  opacity: 0;
}
</style>