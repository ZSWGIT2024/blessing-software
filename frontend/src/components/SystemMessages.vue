<template>
  <div class="system-messages" :style="bgStyle">
    <div v-if="messages.length === 0" class="empty-state">暂无系统消息</div>
    <div v-for="msg in messages" :key="msg.messageId" class="message system-msg-item">
      <div class="msg-icon">📢</div>
      <div class="msg-body">
        <div class="msg-title">{{ msg.title }}</div>
        <div class="msg-content">{{ msg.content }}</div>
        <div class="msg-time">{{ formatTime(msg.createTime) }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getActiveSystemMessages } from '@/api/systemMessageApi'

const props = defineProps({
  backgroundImage: { type: String, default: '' },
  backgroundOpacity: { type: Number, default: 1 }
})

const messages = ref([])
const bgStyle = computed(() => {
  if (!props.backgroundImage) return {}
  const overlayAlpha = (1 - props.backgroundOpacity).toFixed(2)
  const safeUrl = props.backgroundImage.startsWith('data:')
    ? props.backgroundImage
    : encodeURI(props.backgroundImage)
  return {
    background: `linear-gradient(rgba(255,255,255,${overlayAlpha}), rgba(255,255,255,${overlayAlpha})), url("${safeUrl}") center/cover no-repeat`
  }
})

const formatTime = (time) => {
  if (!time) return ''
  const d = new Date(time)
  return d.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

const loadMessages = async () => {
  try {
    const res = await getActiveSystemMessages()
    if (res.code === 0) messages.value = res.data || []
  } catch (e) {
    console.error('获取系统消息失败:', e)
  }
}

onMounted(() => loadMessages())
</script>

<style scoped>
.system-messages {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}
.empty-state {
  text-align: center;
  color: #e949b9;
  font-size: 16px;
  font-weight: bold;
  font-family: 'Times New Roman', Times, serif;
  font-style: italic;
  padding: 40px;
}
.system-msg-item {
  display: flex;
  padding: 12px;
  border-bottom: 1px solid #ffcce0;
  align-items: flex-start;
}
.msg-icon {
  font-size: 24px;
  margin-right: 12px;
}
.msg-body {
  flex: 1;
}
.msg-title {
  font-weight: bold;
  color: #f1177d;
  margin-bottom: 4px;
}
.msg-content {
  font-size: 14px;
  color: #312c2e;
  line-height: 1.5;
}
.msg-time {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}
</style>
