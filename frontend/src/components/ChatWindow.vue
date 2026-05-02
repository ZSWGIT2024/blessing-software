<template>
  <div class="chat-window" :class="{ 'chat-collapsed': isCollapsed }">
    <!-- 聊天窗口头部 -->
    <div class="chat-header">
      <div class="chat-tabs">
        <button v-for="tab in tabs" :key="tab.id" :class="{ active: activeTab === tab.id }" @click="activeTab = tab.id">
          {{ tab.name }}
        </button>
      </div>
      <button class="toggle-chat" @click="toggleCollapse" :title= "isCollapsed ? '展开聊天框' : '隐藏聊天框'">
        {{ isCollapsed ? '🔼' : '🔽' }}
      </button>
    </div>

    <div class="chat-content" v-if="!isCollapsed">
      <!-- 系统消息 -->
      <div v-if="activeTab === 'system'" class="system-messages">
        <div v-for="(message, index) in systemMessages" :key="index" class="message">
          <div class="message-time1">🌸{{ formatTime(message.time) }}🌸</div>
          <div class="message-text1">{{ message.text }}</div>
        </div>
      </div>

      <!-- 世界聊天 -->
      <div v-if="activeTab === 'world'" class="world-messages">
        <div v-for="(message, index) in filteredWorldMessages" :key="index" class="message">
          <div class="message-avatar" @click="onAvatarClick(message.user, $event)">
            <img :src="message.user?.avatar || userInfoStore.currentUser.avatar" :alt="message.user?.name || '未知用户'">
          </div>
          <div class="message-content">
            <div class="message-user">{{ message.user?.name || userInfoStore.currentUser.username }}</div>
            <div class="message-time">🌸{{ formatTime(message.time) }}🌸</div>
            <div class="message-text">{{ message.text }}</div>
          </div>
        </div>
      </div>

      <!-- 输入区域 -->
      <div v-if="activeTab !== 'system'" class="chat-input">
        <input v-model="messageInput" @keyup.enter="sendMessage" placeholder="请输入消息...">
        <button class="emoji-btn" @click.stop="toggleEmojiPicker">+</button>
        <button @click="sendMessage">发送</button>

        <EmojiPicker v-if="showEmojiPicker" @select="addEmoji" @close="closeEmojiPicker" />
      </div>

      <!-- 用户菜单 -->


      <UserMenu v-if="showMenu && selectedUser" :user="selectedUser" :position="menuPosition" @close="closeUserMenu"
        @view-profile="handleViewProfile" @add-friend="handleAddFriend" @block-user="handleBlockUser"
        @private-message="handlePrivateMessage" />

<div class="user-profile-container" v-if="showUserProfile" :user="profileUser">
<button class="close-btn" @click="showUserProfile = false">X</button>
      <UserProfile  />
</div>
      <PrivateMessage v-if="showPrivateMessage" :target-user="messageTarget" @close="showPrivateMessage = false" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'
import UserMenu from '../views/UserMenu.vue'
import EmojiPicker from '../views/EmojiPicker.vue'
import UserProfile from '../views/UserProfile.vue'
import PrivateMessage from '../views/PrivateMessage.vue'
import { useUserInfoStore } from '@/stores/userInfo'

// 组件props和emits
// const props = defineProps({})
// const emit = defineEmits([])

// 状态管理
const isCollapsed = ref(true)
const activeTab = ref('world')
const messageInput = ref('')
const showMenu = ref(false)
const selectedUser = ref(null)
const menuPosition = ref({ x: 0, y: 0 })
const showEmojiPicker = ref(false)
const showUserProfile = ref(false)
const showPrivateMessage = ref(false)
const profileUser = ref(null)
const messageTarget = ref(null)

// Store
const userInfoStore = useUserInfoStore()

// 数据
const tabs = [
  { id: 'system', name: '系统' },
  { id: 'world', name: '世界' },
  { id: 'room', name: '房间' }
]

const systemMessages = ref([
  { text: '系统公告：服务器将于今晚维护', time: new Date() }
])

const worldMessages = ref([
  {
    user: {
      id: userInfoStore.currentUser.id,
      name: userInfoStore.currentUser.username,
      avatar: userInfoStore.currentUser.avatar
    },
    text: '大家好！',
    time: new Date()
  }
])

// 计算属性
const filteredWorldMessages = computed(() => {
  return worldMessages.value.filter(msg => msg && msg.user)
})

// 方法
const formatTime = (date) => {
  return date?.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) || ''
}

const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}

const sendMessage = async () => {
  if (!messageInput.value.trim()) return

  const newMessage = {
    user: {
      id: userInfoStore.currentUser.id,
      name: userInfoStore.currentUser.username,
      avatar: userInfoStore.currentUser.avatar
    },
    text: messageInput.value,
    time: new Date()
  }

  worldMessages.value.push(newMessage)
  messageInput.value = ''

  await nextTick()
  const container = document.querySelector('.world-messages')
  if (container) container.scrollTop = container.scrollHeight
}

const showUserMenu = (user, event) => {
  if (!user || !event) return

  selectedUser.value = {
    id: user.id || userInfoStore.currentUser.id,
    name: user.name || userInfoStore.currentUser.username,
    avatar: user.avatar || userInfoStore.currentUser.avatar
  }

  menuPosition.value = {
    x: Math.min(event.clientX, window.innerWidth - 200),
    y: Math.min(event.clientY, window.innerHeight - 300)
  }

  showMenu.value = true
}

const closeUserMenu = () => {
  showMenu.value = false
  selectedUser.value = null
}

const onAvatarClick = (user, event) => {
  event?.stopPropagation()
  event?.preventDefault()
  showUserMenu(user, event)
}

const toggleEmojiPicker = (event) => {
  event?.stopPropagation()
  showEmojiPicker.value = !showEmojiPicker.value
}

const closeEmojiPicker = () => {
  showEmojiPicker.value = false
}

const addEmoji = (emoji) => {
  messageInput.value += emoji
  closeEmojiPicker()
  const input = document.querySelector('.chat-input input')
  input?.focus()
}

const handleViewProfile = (user) => {
  profileUser.value = user
  showUserProfile.value = true
}

const handleAddFriend = (user) => {
  console.log(`好友请求已发送给 ${user.name}`)
}

const handleBlockUser = (user) => {
  console.log(`已屏蔽用户 ${user.name}`)
}

const handlePrivateMessage = (user) => {
  messageTarget.value = user
  showPrivateMessage.value = true
}

</script>


<style scoped>
.chat-window {
  position: fixed;
  left: 50%;
  transform: translateX(-50%);
  bottom: 50px;
  width: 1300px;
  height: 800px;
  background: rgba(255, 240, 245, 0.9);
  border-radius: 15px 15px 0 0;
  box-shadow: 0 0 20px rgba(18, 230, 88, 0.7);
  z-index: 999;
  transition: all 0.3s ease;
}

.chat-window.chat-collapsed {
  height: 1px;
  bottom: 15px;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 15px;
  background: linear-gradient(135deg, #fa94c3 0%, #50b9b4 100%);
  color: white;
  border-radius: 15px 15px 0 0;
  cursor: move;
  user-select: none;
   -webkit-app-region: drag;
}

.chat-tabs {
  display: flex;
  gap: 10px;
}

.chat-tabs button {
  background: none;
  border: none;
  color: white;
  padding: 5px 10px;
  cursor: pointer;
  border-radius: 4px;
}

.chat-tabs button.active {
  background: rgba(255, 255, 255, 0.3);
}

.toggle-chat {
  position: absolute;
  top: -10px;
  right: 0px;
  background: none;
  border: none;
  color: rgba(255, 16, 203, 0.685);
  font-size: 30px;
  cursor: pointer;
}
.toggle-chat:hover {
  top: -15px;
  right: -10px;
  color: rgba(240, 7, 96, 0.7);
  font-size: 40px;
}

.chat-content {
  height: calc(100% - 40px);
  display: flex;
  flex-direction: column;
}

.system-messages,
.world-messages,
.room-messages {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

.message-content {
  margin-left: 50px;
}
.message-user {
   margin-left: -50px;
  font-weight: bold;
  font-size: 14px;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  font-style: italic;
  color: #ff69b4;
  text-shadow: 3px 3px 3px rgba(30, 235, 218, 0.7);
}
.message-text {
  font-size: 14px;
  font-weight: bold;
  font-family: fantasy;
  line-height: 1.4;
  color: #312c2e;
  text-shadow: 3px 3px 3px rgba(30, 235, 218, 0.7);
}
.message-time {
  font-size: 13px;
  color: #1b0101;
  margin-left: -50px;
}

.message {
  display: flex;
  margin-bottom: 15px;
}

.message-time1 {
  font-size: 13px;
  color: #1b0101;
}
.message-text1 {
  font-size: 14px;
  font-weight: bold;
  font-family: fantasy;
  line-height: 1.4;
  color: #f1177d;
  text-shadow: 3px 3px 3px rgba(30, 235, 218, 0.7);
}

.message-avatar {
  cursor: pointer;
  margin-right: 10px;
}

.message-avatar img {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  cursor: pointer;
}

.chat-input {
  display: flex;
  padding: 10px;
  width: 500px;
  border-top: 1px solid #ffcce0;
  position: relative;
}

.chat-input input {
  flex: 1;
  padding: 8px;
  border: 1px solid #f82dee;
  border-radius: 10px;
  margin-right: 10px;
}

.emoji-btn {
  background: rgb(199, 131, 159);
  border: none;
  font-size: 18px;
  cursor: pointer;
  padding: 0 10px;
}

.chat-input input:focus {
  outline: none;
  border-color: #ff69b4;
}

.user-profile-container {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 700px;
  height: auto;
  transform: translate(-50%, -50%);
  background: rgba(255, 255, 255, 0.9);
  padding: 20px;
  border-radius: 10px;
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.3);
  z-index: 1000;
}


.user-profile-container .close-btn {
  position: absolute;
  top: 10px;
  right: 10px;
  font-size: 20px;
  background: none;
  border: none;
  border-radius: 50%;
  color: #ff69b4;
  cursor: pointer;
}
.user-profile-container .close-btn:hover {
  color: #17dae0;
}
</style>