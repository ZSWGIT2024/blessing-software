<template>
  <div class="chat-window" :class="{ 'chat-collapsed': isCollapsed }">
    <div class="chat-header">
      <div class="chat-tabs">
        <button v-for="tab in tabs" :key="tab.id" :class="{ active: activeTab === tab.id }" @click="activeTab = tab.id">
          {{ tab.name }}
        </button>
      </div>
      <!-- 如果用户未登录，则不显示主题设置按钮和展开/隐藏按钮 -->
      <div class="header-actions">
        <button class="theme-toggle-btn" @click="showThemePanel = !showThemePanel" title="主题设置">&#x1F3A8;</button>
        <button v-if="isAuthenticated" class="toggle-chat" @click="toggleCollapse" :title="isCollapsed ? '展开聊天框' : '隐藏聊天框'">
          {{ isCollapsed ? '&#x1F53C;' : '&#x1F53D;' }}
        </button>
      </div>
    </div>

    <div class="chat-content" v-if="!isCollapsed">
      <SystemMessages v-if="activeTab === 'system'" :background-image="bgImage" :background-opacity="bgOpacity" />
      <WorldChat v-if="activeTab === 'world'" ref="worldChatRef" :background-image="bgImage" :background-opacity="bgOpacity" />
      <GroupChat v-if="activeTab === 'group'" :background-image="bgImage" :background-opacity="bgOpacity" />

      <!-- Theme Settings Panel -->
      <div v-if="showThemePanel" class="theme-panel-overlay" @click.self="showThemePanel = false">
        <div class="theme-panel">
          <div class="theme-panel-header">
            <h3>主题设置</h3>
            <button class="close-btn" @click="showThemePanel = false">&times;</button>
          </div>
          <div class="theme-section">
            <label>背景图片</label>
            <div class="bg-presets">
              <div v-for="(bg, idx) in presetBackgrounds" :key="idx" class="bg-preset"
                :class="{ active: bgImage === bg.url }" @click="selectBg(bg.url)">
                <img :src="bg.url" :alt="bg.name">
                <span>{{ bg.name }}</span>
              </div>
              <div class="bg-preset" :class="{ active: bgImage === '' }" @click="selectBg('')">
                <div class="no-bg">无</div>
                <span>默认</span>
              </div>
            </div>
          </div>
          <div class="theme-section">
            <label>当前背景图</label>
            <div class="current-bg-display" v-if="bgImage">
              <img :src="bgImage" class="current-bg-preview">
              <span class="current-bg-label">已设置</span>
            </div>
            <div class="bg-actions">
              <button class="favorites-btn" @click="showFavorites = true">从收藏中选择</button>
              <input type="file" ref="localBgInput" accept="image/*" style="display:none" @change="onPickLocalBg">
              <button class="local-bg-btn" @click="localBgInput.click()">选择本地图片</button>
            </div>
          </div>
          <div class="theme-section">
            <label>背景透明度: {{ Math.round(bgOpacity * 100) }}%</label>
            <input type="range" v-model.number="bgOpacity" min="0" max="1" step="0.05">
          </div>
          <button class="reset-btn" @click="resetTheme">恢复默认</button>
        </div>
      </div>

      <EmojiPicker v-if="showEmojiPicker" @select="addEmoji" @close="closeEmojiPicker" />

      <UserMenu v-if="showMenu && selectedUser" :user="selectedUser" :position="menuPosition" @close="closeUserMenu"
        @view-profile="handleViewProfile" @add-friend="handleAddFriend" @block-user="handleBlockUser"
        @private-message="handlePrivateMessage" />

      <div class="user-profile-container" v-if="showUserProfile">
        <button class="close-btn" @click="showUserProfile = false">X</button>
        <UserProfile :user="profileUser" />
      </div>
      <PrivateMessage v-if="showPrivateMessage" :target-user="messageTarget" @close="showPrivateMessage = false" />

      <!-- Favorite Manager (used for background picker and standalone) -->
      <FavoriteManager v-if="showFavorites" :select-mode="true" @close="showFavorites = false"
        @select="onPickFavoriteBg" />
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import UserMenu from '../views/UserMenu.vue'
import EmojiPicker from '../views/EmojiPicker.vue'
import UserProfile from '../views/UserProfile.vue'
import PrivateMessage from '../views/PrivateMessage.vue'
import SystemMessages from '@/components/SystemMessages.vue'
import WorldChat from '@/components/WorldChat.vue'
import GroupChat from '@/components/GroupChat.vue'
import FavoriteManager from '@/components/FavoriteManager.vue'
import { useUserInfoStore } from '@/stores/userInfo'

const userInfoStore = useUserInfoStore()

const isCollapsed = ref(true)
const activeTab = ref('world')
const showMenu = ref(false)
const selectedUser = ref(null)
const menuPosition = ref({ x: 0, y: 0 })
const showEmojiPicker = ref(false)
const showUserProfile = ref(false)
const showPrivateMessage = ref(false)
const profileUser = ref(null)
const messageTarget = ref(null)
const isAuthenticated = ref(localStorage.getItem('isAuthenticated') === 'true')

// Theme state
const showThemePanel = ref(false)
const bgImage = ref(localStorage.getItem('chat_bg_image') || '')
const bgOpacity = ref(parseFloat(localStorage.getItem('chat_bg_opacity') || '0.85'))
const showFavorites = ref(false)
const localBgInput = ref(null)

const presetBackgrounds = [
  { name: '图1', url: '/images/x (1).jpg' },
  { name: '图2', url: '/images/x (2).jpg' },
  { name: '图3', url: '/images/x (3).jpg' },
  { name: '图4', url: '/images/x (4).jpg' },
  { name: '图5', url: '/images/x (5).jpg' },
  { name: '图6', url: '/images/x (6).jpg' },
  { name: '动图1', url: '/images/x (1).png' },
  { name: '动图2', url: '/images/x (2).png' },
  { name: '动图3', url: '/images/x (3).png' },
  { name: '动图4', url: '/images/x (4).png' },
  { name: '动图5', url: '/images/x (5).png' },
  { name: '动图6', url: '/images/x (6).png' },
  { name: '动图7', url: '/images/x (7).png' },
  { name: '动图8', url: '/images/x (8).png' },
  { name: '动图9', url: '/images/x (9).png' },
  { name: '动图10', url: '/images/x (10).png' },
  { name: '动图11', url: '/images/x (11).png' },
  { name: '动图12', url: '/images/x (12).png' }
]

const selectBg = (url) => {
  bgImage.value = url
  localStorage.setItem('chat_bg_image', url)
}

const onPickFavoriteBg = (imageUrl) => {
  if (imageUrl) {
    bgImage.value = imageUrl
    localStorage.setItem('chat_bg_image', imageUrl)
  }
  showFavorites.value = false
}

const onPickLocalBg = (event) => {
  const file = event.target.files?.[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = (e) => {
    const dataUrl = e.target?.result
    if (dataUrl) {
      bgImage.value = dataUrl
      localStorage.setItem('chat_bg_image', dataUrl)
    }
  }
  reader.readAsDataURL(file)
}

const resetTheme = () => {
  bgImage.value = ''
  bgOpacity.value = 0.85
  localStorage.removeItem('chat_bg_image')
  localStorage.removeItem('chat_bg_opacity')
}

watch(bgOpacity, (val) => {
  localStorage.setItem('chat_bg_opacity', val)
})

const tabs = [
  { id: 'system', name: '系统' },
  { id: 'world', name: '世界' },
  { id: 'group', name: '群聊' }
]

const toggleCollapse = () => { isCollapsed.value = !isCollapsed.value }
const closeUserMenu = () => { showMenu.value = false; selectedUser.value = null }
const closeEmojiPicker = () => { showEmojiPicker.value = false }
const addEmoji = (emoji) => { closeEmojiPicker(); navigator.clipboard?.writeText(emoji) }
const handleViewProfile = (user) => { profileUser.value = user; showUserProfile.value = true }
const handleAddFriend = (user) => {}
const handleBlockUser = (user) => {}
const handlePrivateMessage = (user) => { messageTarget.value = user; showPrivateMessage.value = true }
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
.chat-window.chat-collapsed { height: 1px; bottom: 10px; }

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 15px;
  background: linear-gradient(135deg, #fa94c3 0%, #50b9b4 100%);
  color: white;
  border-radius: 15px 15px 0 0;
  user-select: none;
}
.chat-tabs { display: flex; gap: 10px; }
.chat-tabs button {
  background: none; border: none; color: white; padding: 5px 10px;
  cursor: pointer; border-radius: 4px; font-size: 14px;
}
.chat-tabs button.active { background: rgba(255,255,255,0.3); }

.header-actions { display: flex; align-items: center; gap: 8px; }
.theme-toggle-btn {
  background: none; border: none; font-size: 18px; cursor: pointer;
  padding: 4px 8px; border-radius: 6px; transition: background 0.2s;
}
.theme-toggle-btn:hover { background: rgba(255,255,255,0.2); }
.toggle-chat { position: relative; top: -15px; right: -20px; background: none; border: none; font-size: 24px; cursor: pointer;
}
.toggle-chat:hover { transform: scale(1.3); transition: transform 0.5s; }

.chat-content {
  height: calc(100% - 44px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;
}

/* Theme panel */
.theme-panel-overlay { position: absolute; inset: 0; background: rgba(0,0,0,0.3); z-index: 2000; display: flex; justify-content: flex-end; }
.theme-panel {
  width: 360px; background: white; height: 100%; overflow-y: auto;
  padding: 20px; box-shadow: -2px 0 10px rgba(0,0,0,0.1);
}
.theme-panel-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.theme-panel-header h3 { margin: 0; color: #f1177d; }
.close-btn { background: none; border: none; font-size: 22px; cursor: pointer; color: #999; }
.theme-section { margin-bottom: 18px; }
.theme-section label { display: block; font-size: 13px; color: #555; margin-bottom: 6px; font-weight: 500; }
.theme-section input[type="range"] { width: 100%; }
.theme-section input[type="text"] { width: 100%; padding: 6px 10px; border: 1px solid #ddd; border-radius: 6px; font-size: 13px; box-sizing: border-box; }

.bg-presets { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; }
.bg-preset { cursor: pointer; text-align: center; border-radius: 8px; padding: 6px; border: 2px solid transparent; transition: border 0.2s; }
.bg-preset.active { border-color: #ff69b4; }
.bg-preset img { width: 60px; height: 40px; object-fit: cover; border-radius: 4px; margin-bottom: 2px; }
.bg-preset span { font-size: 10px; color: #666; display: block; }
.no-bg { width: 60px; height: 40px; background: #f0f0f0; border-radius: 4px; display: flex; align-items: center; justify-content: center; font-size: 12px; color: #999; }
.current-bg-display { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.current-bg-preview { width: 80px; height: 50px; object-fit: cover; border-radius: 6px; border: 2px solid #ffcce0; }
.current-bg-label { font-size: 11px; color: #4caf50; font-weight: bold; }
.bg-actions { display: flex; gap: 8px; }
.favorites-btn { padding: 8px 16px; background: linear-gradient(135deg, #fa94c3, #50b9b4); color: white; border: none; border-radius: 6px; cursor: pointer; font-size: 13px; flex: 1; }
.local-bg-btn { padding: 8px 16px; background: #667eea; color: white; border: none; border-radius: 6px; cursor: pointer; font-size: 13px; flex: 1; }
.local-bg-btn:hover { background: #5a6fd6; }
.scope-options label { font-size: 12px; font-weight: normal; }
.reset-btn { padding: 6px 16px; background: #f5f5f5; border: 1px solid #ddd; border-radius: 6px; cursor: pointer; font-size: 13px; margin-top: 8px; }

.user-profile-container {
  position: absolute;
  left: 50%; top: 50%;
  width: 700px; height: auto;
  transform: translate(-50%, -50%);
  background: rgba(255,255,255,0.9);
  padding: 20px; border-radius: 10px;
  box-shadow: 0 0 10px rgba(0,0,0,0.3);
  z-index: 1000;
}
.user-profile-container .close-btn {
  position: absolute; top: 10px; right: 10px;
  font-size: 20px; background: none; border: none;
  border-radius: 50%; color: #ff69b4; cursor: pointer;
}
.user-profile-container .close-btn:hover { color: #17dae0; }
</style>
