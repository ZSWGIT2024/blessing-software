<template>

  <div class="private-message-container" :class="{ 'mobile-mode': isMobile }">
    <button class="modal-close-btn" @click="closeChat">×</button>

    <!-- 联系人列表 -->
    <div class="contact-list" :class="{ 'hidden': isMobile && !showContactList }">
      <div class="list-header">
        <h2>🌸 私信消息</h2>
        <div class="unread-indicator">
          <span class="unread-dot" v-if="totalUnread > 0"></span>
          {{ totalUnread }} 未读
        </div>
      </div>

      <div class="contact-categories">
        <button v-for="category in categories" :key="category.id"
          :class="['category-btn', { 'active': activeCategory === category.id }]" @click="switchCategory(category.id)">
          {{ category.name }}
        </button>
      </div>

      <div class="contact-scroll">
        <!-- 好友聊天 -->
        <div v-if="activeCategory === 'friends'" class="contact-group">
          <div class="group-title">👥 好友聊天</div>
          <div v-for="contact in friendContacts" :key="contact.userId" class="contact-item" :class="{
            'active': activeContact?.userId === contact.userId,
            'unread': contact.unreadCount > 0
          }" @click="selectContact(contact)">
            <div class="avatar-container">
              <img :src="contact.avatar" class="contact-avatar" />
              <img :src="contact.avatarFrame" class="contact-avatar-frame" />
            </div>
            <div class="contact-info">
              <div class="contact-name">
                {{ contact.username || contact.nickname }}
                <span v-if="contact.remark" class="contact-tag">{{ contact.remark }}</span>
              </div>
              <div class="contact-status">
                <span v-if="contact.isOnline == 'online'" class="online-dot" title="在线"></span>
                <span v-if="contact.isOnline == 'offline'" class="offline-dot" title="离线"></span>
              </div>
              <div class="last-message">{{ contact.lastMessage }}</div>
            </div>
            <div class="contact-meta">
              <div class="last-time">{{ formatRelativeTime(contact.lastTime) }}</div>
              <div v-if="contact.unreadCount > 0" class="unread-count">
                {{ contact.unreadCount }}
              </div>
            </div>
          </div>
        </div>

        <!-- 陌生人聊天 -->
        <div v-if="activeCategory === 'strangers'" class="contact-group">
          <div class="group-title">👤 陌生人消息</div>
          <div v-for="contact in strangerContacts" :key="contact.userId" class="contact-item" :class="{
            'active': activeContact?.userId === contact.userId,
            'unread': contact.unreadCount > 0
          }" @click="selectContact(contact)">
            <div class="avatar-container">
              <img :src="contact.avatar" class="contact-avatar" />
              <img :src="contact.avatarFrame" class="contact-avatar-frame" />
            </div>
            <div class="contact-info">
              <div class="contact-name">{{ contact.username || contact.nickname }}</div>
              <div class="contact-status">
                <span v-if="contact.isOnline == 'online'" class="online-dot" title="在线"></span>
                <span v-if="contact.isOnline == 'offline'" class="offline-dot" title="离线"></span>
              </div>
              <div class="last-message">{{ contact.lastMessage }}</div>
            </div>
            <div class="contact-meta">
              <div class="last-time">{{ formatRelativeTime(contact.lastTime) }}</div>
              <div v-if="contact.unreadCount > 0" class="unread-count">
                {{ contact.unreadCount }}
              </div>
            </div>
          </div>
        </div>

        <!-- 系统消息 -->
        <div v-if="activeCategory === 'system'" class="contact-group">
          <div class="group-title">🔔 系统通知</div>
          <div v-for="notification in systemNotifications" :key="notification.id" class="system-item"
            :class="{ 'unread': !notification.isRead }" @click="viewSystemNotification(notification)">
            <div class="system-icon">🔔</div>
            <div class="system-info">
              <div class="system-title">
                <span v-if="!notification.isRead" class="new-badge">[新]</span>
                {{ notification.title }}
              </div>
              <div class="system-content">{{ notification.content }}</div>
              <div class="system-time">{{ formatRelativeTime(notification.createTime) }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 聊天区域 -->
    <div class="chat-area" :class="{ 'full-width': isMobile && !showContactList }">
      <div v-if="activeContact" class="chat-container">
        <!-- 聊天头部 -->
        <div class="chat-header">
          <button v-if="isMobile" @click="showContactList = true" class="back-btn">
            ←
          </button>
          <img :src="activeContact.avatar" class="chat-avatar" />
          <img :src="activeContact.avatarFrame" class="chat-avatar-frame" />
          <div class="chat-info">
            <div class="chat-name">
              {{ activeContact.remark || activeContact.nickname }}
              <span v-if="activeContact.isOnline == 'online'" class="status online">在线</span>
              <span v-else class="status offline">离线</span>
            </div>
            <div v-if="activeContact.isFriend" class="chat-relation">好友</div>
            <div v-else class="chat-relation">陌生人</div>
          </div>

          <!-- 新增：输入状态提示 -->
          <div class="typing-indicator" v-if="isContactTyping">
            <span class="dot"></span>
            <span class="dot"></span>
            <span class="dot"></span>
            {{ activeContact.nickname }} 正在输入...
          </div>

          <div class="chat-actions">
            <button v-if="!activeContact.isFriend" @click="sendFriendRequest(activeContact.userId)"
              class="action-btn add-friend" title="加为好友">
              添加好友
            </button>
            <button v-if="messages.length > 0" @click="clearChatHistory()" class="action-btn clear" title="清空记录">
              清空聊天记录
            </button>
          </div>
        </div>

        <!-- 消息区域 -->
        <div class="messages-container-wrapper">
          <div class="messages-container" ref="messagesContainer" @scroll="handleScroll">
            <!-- 顶部加载更多指示器 - 只在有更多消息且不在加载中时显示 -->
            <div v-if="hasMoreMessages" class="load-more-indicator">
              <button v-if="!loadingMoreMessages" @click="loadMoreMessages" class="load-more-btn"
                :disabled="loadingMoreMessages">
                加载更早的消息
              </button>
              <div v-else class="loading-spinner">
                <span class="spinner"></span> 加载中...
              </div>
            </div>
            <!-- 如果没有更多消息，显示提示 -->
            <div v-else-if="messages.length > 0 && !loadingMessages" class="no-more-messages">
              没有更早的消息了
            </div>

            <!-- 消息列表 -->
            <div v-for="(message, index) in messages" :key="getMessageKey(message, index)" class="message-wrapper"
              @contextmenu.prevent="showMessageMenu($event, message)">

              <!-- 时间分隔线 -->
              <div v-if="shouldShowDateDivider(message, index)" class="date-divider">
                <span>{{ formatMessageDate(message.createTime) }}</span>
              </div>

              <!-- 消息项样式 - 根据发送者判断 -->
              <div class="message-item" :class="{
                'sent': message.senderId === currentUserId,
                'received': message.senderId !== currentUserId
              }">

                <!-- 对方消息（接收）- 显示在左侧 -->
                <div v-if="message.senderId !== currentUserId" class="avatar receiver-avatar">
                  <!-- 修改：添加头像容器 -->
                  <div class="avatar-container">
                    <img :src="activeContact.avatar" :alt="activeContact.nickname" @error="handleAvatarError" />
                    <!-- 修改：对方动态头像框使用绝对定位 -->
                    <img :src="activeContact.avatarFrame" class="othersAvatar-frame" />
                  </div>
                </div>

                <!-- 自己消息（发送）- 显示在右侧 -->
                <div v-if="message.senderId === currentUserId" class="avatar sender-avatar">
                  <!-- 修改：添加头像容器 -->
                  <div class="avatar-container">
                    <img :src="userStore.currentUser?.avatar || defaultAvatar" alt="我" @error="handleAvatarError" />
                    <!-- 修改：自己动态头像框使用绝对定位 -->
                    <img :src="message.senderAvatarFrame || avatarFrame" class="currentAvatar-frame" />
                  </div>
                </div>

                <div class="message-content-wrapper">
                  <!-- 对方用户名显示 - 只显示在对方消息上 -->
                  <div v-if="message.senderId !== currentUserId">
                    <!-- 对方消息，显示发送方的用户名（因为对方是发送者） -->
                    <span class="username receiver-username" title="用户名">{{ activeContact.username }}</span>
                    <!-- 备注：只有当当前用户是接收方时，才显示备注 -->
                    <span v-if="message.remark && message.receiverId === currentUserId" class="username-remark"
                      title="备注">
                      [{{ activeContact.remark }}]
                    </span>
                  </div>

                  <!-- 自己用户名不显示，因为是自己发的消息 -->

                  <div class="message-bubble">
                    <!-- 新增：引用消息预览 -->
                    <div v-if="message.quotedMessage" class="quoted-message-preview">
                      <div class="quoted-header">
                        <span class="quoted-sender">[{{ getQuoteSenderName(message.quotedMessage) }}]</span>
                        <span class="quoted-time">{{ formatMessageTime(message.quotedMessage.createTime) }}</span>
                      </div>
                      <div class="quoted-content">
                        {{ getQuoteContent(message.quotedMessage) }}
                      </div>
                    </div>
                    <!-- 文本消息 -->
                    <div v-if="message.contentType === 'text'" class="message-text">
                      {{ message.content }}
                    </div>

                    <!-- 图片消息 -->
                    <div v-if="message.contentType === 'image' && message.status !== 'withdrawn'" class="message-image">
                      <div class="image-info">
                        <div class="image-name" title="文件名">{{ message.fileName || '图片' }}</div>
                        <img :src="message.content || message.thumbnailUrl" @click="previewImage(message)"
                          class="message-image-preview" :alt="message.fileName || '图片'" loading="lazy" />
                        <div class="image-size" title="文件大小">{{ message.fileSize }}</div>
                      </div>
                    </div>

                    <!-- 视频消息 -->
                    <div v-if="message.contentType === 'video' && message.status !== 'withdrawn'" class="message-video">
                      <div class="video-info">
                        <div class="video-name" title="文件名">{{ message.fileName || '视频' }}</div>
                        <video :src="message.content || message.thumbnailUrl" :poster="message.content" controls
                          preload="metadata" class="message-video-player" @click="playVideo(message)">
                          您的浏览器不支持视频播放
                        </video>
                        <div class="video-size" title="文件大小">{{ message.fileSize }}</div>
                      </div>

                    </div>

                    <!-- 文件消息 -->
                    <div v-if="message.contentType === 'file' && message.status !== 'withdrawn'" class="message-file">
                      <div class="file-icon">📄</div>
                      <div class="file-info">
                        <div class="file-name" title="文件名">{{ message.fileName || '文件' }}</div>
                        <div class="file-size" title="文件大小">{{ message.fileSize }}</div>
                      </div>
                      <button @click="downloadFile(message)" class="download-btn">下载</button>
                    </div>

                    <!-- 撤回消息显示 -->
                    <div v-if="message.status === 'withdrawn'" class="message-withdrawn">
                      [{{ message.senderUsername }}] {{ '已成功撤回该消息！' }}
                    </div>

                    <!-- 系统消息 -->
                    <div v-if="message.type === 'system'" class="message-system">
                      {{ message.content }}
                    </div>

                    <div class="message-meta">
                      <span class="message-time" title="发送时间">{{ formatMessageTime(message.createTime) }}</span>

                      <!-- 消息状态指示器 - 只显示在自己发送的消息上 -->
                      <span v-if="message.senderId === currentUserId" class="message-status">
                        <span v-if="message.status === 'sending'" class="status-icon sending" title="发送中">⏳</span>
                        <span v-else-if="message.status === 'sent'" class="status-icon sent" title="已发送">✓</span>
                        <span v-else-if="message.status === 'withdrawn'" class="status-icon withdrawn"
                          title="已撤回">🔙</span>
                        <span v-else-if="message.status === 'read'" class="status-icon read" title="已读">已读</span>
                        <span v-else-if="message.status === 'failed'" class="status-icon failed" title="发送失败">⚠️</span>
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 空消息提示 -->
            <div v-if="messages.length === 0 && !loadingMessages" class="empty-messages">
              还没有消息，开始聊天吧！
            </div>

            <!-- 底部占位元素，用于滚动定位 -->
            <div ref="scrollBottomRef" class="scroll-bottom-anchor"></div>
          </div>
        </div>

        <!-- 修复：回到底部按钮 - 只在不在底部且有消息时显示 -->
        <transition name="fade">
          <button v-if="!isAtBottom && messages.length > 0" class="scroll-to-bottom-btn"
            @click="scrollToBottom($event)">
            ⬇️ 回到最新消息
            <span v-if="unreadMessagesCount > 0" class="unread-badge">{{ unreadMessagesCount }}</span>
          </button>
        </transition>

        <!-- 消息输入区域 -->
        <div class="input-area">
          <div class="input-tools">
            <button @click="toggleEmojiPicker" ref="emojiButtonRef" class="tool-btn emoji" title="表情">😊</button>
            <button @click="triggerImageUpload" class="tool-btn image" title="图片">🖼️</button>
            <button @click="triggerFileUpload" class="tool-btn file" title="文件">📄</button>

            <input type="file" ref="imageInput" accept="image/*" style="display: none" @change="handleImageUpload"
              multiple />
            <input type="file" ref="fileInput" style="display: none" @change="handleFileUpload" multiple />
          </div>

          <div class="emoji-picker-wrapper" v-if="showEmojiPicker">
            <EmojiPicker :key="'emoji-picker-' + showEmojiPicker" :visible="showEmojiPicker"
              :targetElement="emojiButtonRef" @select="insertEmoji" @select-pack="sendEmojiPackMessage"
              @close="handleEmojiPickerClose" @update:visible="handleEmojiPickerVisibleChange" />
          </div>

          <!-- 文件上传进度 -->
          <div v-if="uploadingFiles.length > 0" class="upload-progress">
            <div v-for="file in uploadingFiles" :key="file.id" class="upload-item">
              <span class="warning-text">图片和视频(最大10MB)上传速度较慢，请耐心等待...</span>
              <div class="file-info">
                <span class="file-name">{{ file.name }}</span>
                <span class="file-size">{{ formatFileSize(file.size) }}</span>
              </div>
              <div class="progress-bar">
                <div class="progress" :style="{ width: file.progress + '%' }"></div>
                <span class="progress-text">{{ file.progress }}%</span>
              </div>
              <button v-if="file.error" @click="retryUpload(file)" class="retry-btn">重试</button>
              <button v-if="file.error" @click="cancelUpload(file)" class="cancel-btn">取消</button>
            </div>
          </div>

          <div class="input-box">
            <!-- 新增：引用消息预览区域 - 放在输入框上方 -->
            <div v-if="quotedMessage" class="quote-preview">
              <div class="quote-content">
                <span class="quote-label">引用消息</span>
                <span class="quote-sender">{{ getQuoteSenderName(quotedMessage) }}</span>
                <span class="quote-text">{{ getQuoteContent(quotedMessage) }}</span>
                <button @click="cancelQuote" class="cancel-quote">×</button>
              </div>
            </div>
            <textarea v-model="newMessage" @keydown.enter.exact.prevent="sendTextMessage"
              @keydown.enter.shift.exact.prevent="newMessage += '\n'" placeholder="输入消息... (Enter发送，Shift+Enter换行)"
              rows="3" ref="messageInput"></textarea>

            <div class="input-actions">
              <button @click="sendTextMessage" :disabled="!newMessage.trim()" class="send-btn">
                发送
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 无聊天时的占位 -->
      <div v-else class="no-chat-selected">
        <div class="placeholder-content">
          <img src="@/assets/chat-placeholder.webp" alt="选择聊天" />
          <h3>选择聊天对象</h3>
          <p>从左侧列表中选择好友或陌生人开始聊天</p>
        </div>
      </div>
    </div>

    <!-- 右键菜单 -->
    <!-- 右键菜单 - 找到原有菜单部分，替换为以下代码 -->
    <div v-if="activeMessageMenu" class="message-context-menu" :style="{
      top: messageMenuPosition.y + 'px',
      left: messageMenuPosition.x + 'px'
    }" @click.stop>
      <!-- 撤回消息 - 仅自己发送的且未超时的消息可撤回 -->
      <button v-if="activeMessageMenu.senderId === currentUserId && canWithdraw(activeMessageMenu)"
        @click="withdrawMessage(activeMessageMenu)" class="menu-item withdraw">
        撤回消息
      </button>

      <!-- 复制 - 仅文本消息可复制 -->
      <button v-if="activeMessageMenu.contentType === 'text'" @click="copyMessage(activeMessageMenu)"
        class="menu-item copy">
        复制
      </button>

      <!-- 重新发送 - 仅发送失败的消息 -->
      <button v-if="activeMessageMenu.status === 'failed'" @click="resendMessage(activeMessageMenu)"
        class="menu-item resend">
        重新发送
      </button>

      <!-- 下载文件 - 非文本消息可下载 -->
      <button v-if="activeMessageMenu.contentType !== 'text' && activeMessageMenu.status !== 'withdrawn'"
        @click="downloadFile(activeMessageMenu)" class="menu-item download">
        下载文件
      </button>

      <!-- 新增：引用消息 - 所有消息都可引用 -->
      <button @click="quoteMessage(activeMessageMenu)" class="menu-item quote">
        引用消息
      </button>

      <!-- 新增：删除消息（仅对自己删除，对方仍可见） -->
      <button @click="deleteMessage(activeMessageMenu)" class="menu-item delete">
        删除消息
      </button>

    </div>

    <!-- 图片预览模态框 -->
    <div v-if="previewImageUrl" class="image-preview-modal" @click="previewImageUrl = null">
      <img :src="previewImageUrl" @click.stop />
    </div>
  </div>

</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useUserInfoStore } from '@/stores/userInfo'
import websocketService from '@/utils/websocket'
import { getCurrentFrame } from '@/api/avatarFrame'
import * as socialApi from '@/api/socialApi'
import EmojiPicker from './EmojiPicker.vue'
import { ElMessage, ElMessageBox } from 'element-plus'

// 默认头像
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const props = defineProps({
  targetUser: {
    type: Object,
    default: null
  },
  visible: {
    type: Boolean,
    default: false
  },
  userData: {
    type: Object,
    default: () => ({})
  },
  friendId: {
    type: Number,
    default: null  // 改为默认 null
  }
})

const emits = defineEmits(['contentChanged', 'openLightbox', 'playVideo', 'update:visible', 'close'])
const userStore = useUserInfoStore()
// 从本地存储获取头像框
const avatarFrame = ref(null)
const emojiButtonRef = ref(null)
const isComponentMounted = ref(false)
// 响应式数据
const showContactList = ref(false)
const activeCategory = ref('friends')
const activeContact = ref(null)
const messages = ref([])
const newMessage = ref('')
const showEmojiPicker = ref(false)
const previewImageUrl = ref(null)
const loadingMessages = ref(false)
const loadingMoreMessages = ref(false)
const hasMoreMessages = ref(false)
const imageInput = ref(null)
const fileInput = ref(null)
const messagesContainer = ref(null)
const scrollBottomRef = ref(null)
const messageInput = ref(null)
const activeVideo = ref(null)

// 滚动控制标志
const isAtBottom = ref(true)
const userScrolled = ref(false)
const unreadMessagesCount = ref(0)

// 文件上传相关状态
const uploadingFiles = ref([])

// 消息菜单相关状态
const activeMessageMenu = ref(null)
const messageMenuPosition = ref({ x: 0, y: 0 })
const statusPollingTimers = ref({})

// 计算属性
const isMobile = computed(() => window.innerWidth < 768)
const currentUserId = computed(() => userStore.currentUser?.id)

// 可见性控制
const visible = ref(props.visible)
// 引用消息相关状态
const quotedMessage = ref(null)  // 当前引用的消息

// 从接口获取用户头像框
const fetchAvatarFrame = async () => {
  try {
    const response = await getCurrentFrame()
    avatarFrame.value = response.data.url
  } catch (error) {
    console.error('Failed to fetch avatar frame:', error)
  }
}

// 修改 findAndSelectContact 方法，添加更多日志
const findAndSelectContact = (targetUser) => {
  if (!targetUser) {
    console.log('targetUser is null')
    return
  }

  // UserDetail 传来的数据 id 字段存 userId，聊天列表用 userId
  const userId = targetUser.userId || targetUser.id || props.friendId
  if (!userId) {
    console.warn('targetUser has no userId or id', targetUser)
    return
  }

  // 先在好友中查找
  let contact = friendContacts.value.find(c => c.userId === userId)

  // 如果没找到，在陌生人中查找
  if (!contact) {
    contact = strangerContacts.value.find(c => c.userId === userId)
  }

  // 如果找到了，自动选择
  if (contact) {
    selectContact(contact)
  } else {
    // 如果没找到，创建一个临时联系人
    const tempContact = {
      userId: userId,
      id: userId,
      username: targetUser.username || targetUser.nickname,
      nickname: targetUser.nickname || targetUser.username,
      avatar: targetUser.avatar || defaultAvatar,
      avatarFrame: targetUser.avatarFrame,
      isOnline: targetUser.isOnline || 'offline',
      isFriend: targetUser.isFriend || false,
      lastMessage: '暂无消息',
      lastTime: new Date().toISOString(),
      unreadCount: 0
    }
    // 根据是否为好友添加到对应列表
    if (tempContact.isFriend) {
      friendContacts.value.push(tempContact)
    } else {
      strangerContacts.value.push(tempContact)
    }

    selectContact(tempContact)
  }
}

const closeChat = () => {
  // visible.value = false
  emits('update:visible', false)
  emits('close')
  activeContact.value = null
  messages.value = []
}

const categories = [
  { id: 'friends', name: '好友' },
  { id: 'strangers', name: '陌生人' },
  { id: 'system', name: '系统' }
]

const friendContacts = ref([])
const strangerContacts = ref([])
const systemNotifications = ref([])
// 计算未读消息总数
const totalUnread = computed(() => {
  return friendContacts.value.reduce((sum, c) => sum + c.unreadCount, 0) +
    strangerContacts.value.reduce((sum, c) => sum + c.unreadCount, 0) +
    systemNotifications.value.filter(n => !n.isRead).length
})

// 新增：计算对方是否正在输入
const isContactTyping = computed(() => {
  if (!activeContact.value) return false
  return userStore.isUserTyping(activeContact.value.userId)
})

// 新增：输入防抖
let typingTimeout = null

// 修改原有的 watch newMessage
watch(newMessage, (newVal, oldVal) => {
  if (!activeContact.value || !newVal.trim() || newVal === oldVal) return

  // 清除之前的定时器
  if (typingTimeout) clearTimeout(typingTimeout)

  // 发送正在输入状态
  userStore.sendTypingStatus(activeContact.value.userId, true)

  // 3秒后停止输入状态
  typingTimeout = setTimeout(() => {
    userStore.sendTypingStatus(activeContact.value.userId, false)
  }, 3000)
})


// 监听消息变化
watch(messages, async (newMessages, oldMessages) => {
  if (newMessages.length > oldMessages?.length) {
    const lastMessage = newMessages[newMessages.length - 1]
    const isNewMessage = lastMessage && lastMessage.senderId === currentUserId.value

    if (isNewMessage || (isAtBottom.value && !userScrolled.value)) {
      await nextTick()
      scrollToBottom('smooth')
    } else if (!isAtBottom.value) {
      unreadMessagesCount.value++
    }
  }
}, { deep: true })

// 监听容器滚动事件
const handleScroll = () => {
  if (!messagesContainer.value) return

  const { scrollTop, scrollHeight, clientHeight } = messagesContainer.value
  const bottomThreshold = 50
  const atBottom = Math.abs(scrollHeight - scrollTop - clientHeight) < bottomThreshold

  if (atBottom) {
    isAtBottom.value = true
    userScrolled.value = false
    unreadMessagesCount.value = 0
  } else {
    isAtBottom.value = false
    userScrolled.value = true
  }
}

// 滚动到底部
const scrollToBottom = (event) => {
  if (!messagesContainer.value) return

  // 如果有事件对象，阻止默认行为
  if (event) {
    event.preventDefault?.()
    event.stopPropagation?.()
  }

  // 使用 scrollBottomRef 锚点滚动
  if (scrollBottomRef.value) {
    scrollBottomRef.value.scrollIntoView({
      behavior: 'smooth',
      block: 'end'
    })
  } else {
    // 备选方案
    messagesContainer.value.scrollTo({
      top: messagesContainer.value.scrollHeight,
      behavior: 'smooth'
    })
  }

  // 滚动到底部后重置状态
  isAtBottom.value = true
  userScrolled.value = false
  unreadMessagesCount.value = 0
}

const getMessageKey = (message, index) => {
  return message.messageId || `msg_${message.createTime}_${index}`
}

const handleAvatarError = (e) => {
  e.target.src = defaultAvatar
}

const switchCategory = (categoryId) => {
  activeCategory.value = categoryId
  if (isMobile.value) {
    showContactList.value = true
  }
}

// 修改 selectContact 方法，添加已读回执
const selectContact = async (contact) => {
  try {
     // 在加载聊天历史前先关闭表情选择器
    showEmojiPicker.value = false

    activeContact.value = contact
    showContactList.value = false
    hasMoreMessages.value = true

    await loadChatHistory(contact.userId)
    markChatAsRead(contact.userId)

    // 发送已读回执
    if (messages.value && messages.value.length > 0) {
      messages.value.forEach(msg => {
        if (msg.senderId === contact.userId && !msg.isRead) {
          websocketService.sendReadReceipt(contact.userId, msg.messageId)
        }
      })
    }

    isAtBottom.value = true
    userScrolled.value = false
    unreadMessagesCount.value = 0

    await nextTick()
    scrollToBottom('auto')
  } catch (error) {
    console.error('选择联系人失败:', error)
    ElMessage.error('加载聊天记录失败')
  }
}

const markChatAsRead = async (userId) => {
  try {
    await socialApi.markMessagesAsRead(userId)

    const contact = [...friendContacts.value, ...strangerContacts.value]
      .find(c => c.userId === userId)
    if (contact) {
      contact.unreadCount = 0
    }
  } catch (error) {
    console.error('标记已读失败:', error)
  }
}

const getFirstMessageId = () => {
  for (const msg of messages.value) {
    if (msg.messageId) {
      return msg.messageId
    }
  }
  return null
}

const loadMoreMessages = async () => {
  if (loadingMoreMessages.value || !activeContact.value || !hasMoreMessages.value) return

  const container = messagesContainer.value
  if (!container) return

  const oldScrollHeight = container.scrollHeight
  const firstMessageId = getFirstMessageId()

  loadingMoreMessages.value = true
  try {
    const response = await socialApi.getChatHistory(activeContact.value.userId, {
      lastMessageId: firstMessageId,
      pageSize: 20
    })

    if (response.code === 0 && Array.isArray(response.data)) {
      if (response.data.length > 0) {
        const newMessages = response.data.map(msg => formatMessageData(msg))
        messages.value = [...newMessages, ...messages.value]
        hasMoreMessages.value = response.data.length === 20
      } else {
        hasMoreMessages.value = false
      }

      await nextTick()

      const newScrollHeight = container.scrollHeight
      container.scrollTop = newScrollHeight - oldScrollHeight
    }
  } catch (error) {
    console.error('加载更多消息失败:', error)
    ElMessage.error('加载更多消息失败')
  } finally {
    loadingMoreMessages.value = false
  }
}


// 获取引用消息的发送者名称
const getQuoteSenderName = (message) => {
  if (message.senderId === currentUserId.value) {
    return '我'
  }
  return activeContact.value?.remark || activeContact.value?.nickname || activeContact.value?.username || '对方'
}

// 获取引用消息的预览内容
const getQuoteContent = (message) => {
  switch (message.contentType) {
    case 'text':
      return message.content?.length > 30 ? message.content.substring(0, 30) + '...' : message.content
    case 'image':
      return '[图片]'
    case 'video':
      return '[视频]'
    case 'file':
      return `[文件] ${message.fileName || ''}`
    default:
      return message.content || '[未知消息]'
  }
}

// 引用消息功能
const quoteMessage = (message) => {
  quotedMessage.value = { ...message }
  closeMessageMenu()
  // 聚焦到输入框
  messageInput.value?.focus()
}

// 取消引用
const cancelQuote = () => {
  quotedMessage.value = null
}

// 修改原有的 sendTextMessage 函数，添加引用消息处理
const sendTextMessage = async () => {
  if (!newMessage.value.trim() || !activeContact.value || !currentUserId.value) return

  // 如果组件已经卸载，不执行
  if (!isComponentMounted.value) return
  const tempId = currentUserId.value + '_' + activeContact.value.userId + '_' + Date.now()
  const messageContent = newMessage.value

  // 构建消息对象，如果有引用消息则添加引用信息
  const messageData = {
    receiverId: activeContact.value.userId,
    content: messageContent,
    contentType: 'text',
    chatType: activeContact.value.isFriend ? 'friend' : 'stranger',
    messageId: tempId
  }

  // 如果有引用消息，添加到 extraData 中
  if (quotedMessage.value) {
    messageData.extraData = {
      quotedMessage: {
        messageId: quotedMessage.value.messageId,
        senderId: quotedMessage.value.senderId,
        content: getQuoteContent(quotedMessage.value),
        contentType: quotedMessage.value.contentType,
        createTime: quotedMessage.value.createTime
      }
    }
  }

  const tempMessage = {
    messageId: tempId,
    senderId: currentUserId.value,
    senderName: userStore.currentUser?.nickname,
    senderUsername: userStore.currentUser?.username,
    senderAvatar: userStore.currentUser?.avatar,
    content: messageContent,
    contentType: 'text',
    createTime: new Date().toISOString(),
    isRead: false,
    status: 'sending',
    quotedMessage: quotedMessage.value ? { ...quotedMessage.value } : null  // 临时消息也保存引用信息
  }

  messages.value.push(tempMessage)

  isAtBottom.value = true
  userScrolled.value = false
  await nextTick()
  scrollToBottom()

  // 使用 WebSocket 发送
  const success = await userStore.sendMessage(messageData)

  if (success) {
    // 更新消息状态
    const index = messages.value.findIndex(m => m.messageId === tempId)
    if (index !== -1) {
      messages.value[index].status = 'sent'
    }

    newMessage.value = ''
    quotedMessage.value = null
    updateContactLastMessage(messageContent)

    // 停止输入状态
    if (typingTimeout) clearTimeout(typingTimeout)
    userStore.sendTypingStatus(activeContact.value.userId, false)
  } else {
    // 发送失败，保持 sending 状态，稍后会通过重试机制处理
    console.log('消息已加入离线队列')
  }
}

// 删除消息（仅对自己删除）
const deleteMessage = async (message) => {
  try {
    // 确认删除
    await ElMessageBox.confirm(
      '确定要删除这条消息吗？此操作只对您自己有效，对方仍能看到该消息。',
      '删除消息',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'info'
      }
    )

    // 从本地消息列表中移除
    try {
      const index = messages.value.findIndex(m => m.messageId === message.messageId)
      if (index !== -1) {
        messages.value.splice(index, 1)
      }
      const res = await socialApi.deleteMessage(message.messageId)
      if (res.code === 0) {
        ElMessage.success('消息已删除')
      } else {
        ElMessage.error(res.msg || '删除失败')
      }
    } catch (error) {
      console.error('删除消息失败:', error)
      ElMessage.error('删除失败')
    }
  } catch (error) {
  } finally {
    closeMessageMenu()
  }
}

const removeDuplicateMessages = (messageArray) => {
  const seen = new Map()
  return messageArray.filter(msg => {
    const key = msg.messageId
    if (!key) return true

    if (seen.has(key)) {
      return false
    }
    seen.set(key, true)
    return true
  })
}

const formatMessageData = (msg) => {
  const baseMessage = {
    messageId: msg.messageId,
    senderId: msg.senderId,
    receiverId: msg.receiverId,
    contentType: msg.contentType?.toLowerCase() || 'text',
    type: msg.contentType?.toLowerCase() || 'text',
    chatType: msg.chatType,
    createTime: msg.createTime,
    isRead: msg.isRead,
    status: msg.status || 'sent',
  }

  if (msg.fileInfo) {
    baseMessage.senderNickname = msg.fileInfo.uploaderNickname
    baseMessage.senderUsername = msg.fileInfo.uploaderUsername
    baseMessage.senderAvatar = msg.fileInfo.uploaderAvatar
    baseMessage.senderAvatarFrame = msg.fileInfo.uploaderAvatarFrame
    baseMessage.remark = msg.fileInfo.uploaderRemark
    baseMessage.fileName = msg.fileInfo.fileName
    baseMessage.fileUrl = msg.fileInfo.fileUrl
    baseMessage.thumbnailUrl = msg.fileInfo.thumbnailUrl
    baseMessage.fileSize = msg.fileInfo.fileSize
    baseMessage.fileType = msg.fileInfo.fileType
  }

  if (msg.receiverInfo) {
    baseMessage.receiverNickname = msg.receiverInfo.nickname
    baseMessage.receiverUsername = msg.receiverInfo.username
    baseMessage.receiverAvatar = msg.receiverInfo.avatar
    baseMessage.receiverAvatarFrame = msg.receiverInfo.avatarFrame
  }

  if (baseMessage.contentType === 'text') {
    return {
      ...baseMessage,
      content: msg.content,
    }
  } else {
    return {
      ...baseMessage,
      content: msg.fileInfo?.fileUrl || msg.content,
    }
  }
}

const updateContactLastMessage = (content) => {
  if (!activeContact.value) return

  const contacts = activeContact.value.isFriend ? friendContacts : strangerContacts
  const contact = contacts.value.find(c => c.userId === activeContact.value.userId)

  if (contact) {
    contact.lastMessage = content.length > 20 ? content.substring(0, 20) + '...' : content
    contact.lastTime = new Date().toISOString()
  }
}

const sendFriendRequest = async (userId) => {
  if (!activeContact.value) return

  try {
    const { value: msg } = await ElMessageBox.prompt('请输入申请留言（可选）', '发送好友申请', {
      confirmButtonText: '发送',
      cancelButtonText: '取消',
      inputPlaceholder: '输入留言...'
    })

    await socialApi.sendFriendApply({
      receiverId: userId,
      applyMsg: msg || ''
    })
    ElMessage.success('好友申请已发送')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.msg || '发送失败')
    }
  }
}

const clearChatHistory = async () => {
  if (!activeContact.value || !confirm('确定要清空聊天记录吗？')) return

  try {
    const res = await socialApi.deleteChatHistory(activeContact.value.userId)
    if (res.code === 0) {
      ElMessage.success('清空聊天记录成功')
    }
    updateContactLastMessage('暂时还没有消息，开始聊天吧')
    messages.value = []
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '清空失败')
  }
}

// 切换表情选择器
const toggleEmojiPicker = () => {
  console.log('切换表情选择器，当前状态:', showEmojiPicker.value)
  
  // 直接切换，不在这里做额外操作
  showEmojiPicker.value = !showEmojiPicker.value
  
  // 如果关闭，确保不留下定时器
  if (!showEmojiPicker.value) {
    // 不做额外操作，让子组件自己清理
  }
}

// 新增：处理 EmojiPicker 的 close 事件
const handleEmojiPickerClose = () => {
  console.log('EmojiPicker 请求关闭')
  showEmojiPicker.value = false
}

// 新增：处理 EmojiPicker 的 visible 更新
const handleEmojiPickerVisibleChange = (val) => {
  console.log('EmojiPicker visible 变更为:', val)
  showEmojiPicker.value = val
}

// 插入Emoji（原有的方法）
const insertEmoji = (emoji) => {
  newMessage.value += emoji
  messageInput.value?.focus()
}

// 新增：发送表情包图片
const sendEmojiPackMessage = async (packItem) => {
  if (!activeContact.value || !currentUserId.value) return

  const tempId = currentUserId.value + '_' + activeContact.value.userId + '_' + Date.now()

  // 构建消息数据
  const messageData = {
    receiverId: activeContact.value.userId,
    content: '[表情包]', // 显示文本
    contentType: 'image', // 类型为图片
    chatType: activeContact.value.isFriend ? 'friend' : 'stranger',
    messageId: tempId,
    extraData: {
      type: 'emoji_pack',
      packItemId: packItem.id,
      imageUrl: packItem.imageUrl,
      description: packItem.description || '表情包'
    }
  }

  // 添加临时消息
  const tempMessage = {
    messageId: tempId,
    senderId: currentUserId.value,
    senderName: userStore.currentUser?.nickname,
    senderUsername: userStore.currentUser?.username,
    senderAvatar: userStore.currentUser?.avatar,
    content: packItem.imageUrl,
    contentType: 'image',
    createTime: new Date().toISOString(),
    isRead: false,
    status: 'sending',
    fileName: packItem.description || '表情包',
    fileUrl: packItem.imageUrl,
    thumbnailUrl: packItem.imageUrl,
    isEmojiPack: true,
    emojiPackId: packItem.id
  }

  messages.value.push(tempMessage)
  await nextTick()
  scrollToBottom()

  // 使用 WebSocket 发送
  const success = await userStore.sendMessage(messageData)

  if (success) {
    const index = messages.value.findIndex(m => m.messageId === tempId)
    if (index !== -1) {
      messages.value[index].status = 'sent'
    }

    // 更新最近聊天记录
    updateContactLastMessage('[表情包]')

  } else {
    const index = messages.value.findIndex(m => m.messageId === tempId)
    if (index !== -1) {
      messages.value[index].status = 'failed'
    }
  }
}

const triggerImageUpload = () => {
  imageInput.value.click()
}

const triggerFileUpload = () => {
  fileInput.value.click()
}

const loadChatHistory = async (userId) => {
  // 如果组件已经卸载，不执行
  if (!isComponentMounted.value) return
  loadingMessages.value = true
  try {
    const response = await socialApi.getChatHistory(userId, {
      lastMessageId: null,
      pageSize: 20
    })

    // 再次检查组件是否仍然挂载
    if (!isComponentMounted.value) return
    if (response.code === 0 && Array.isArray(response.data)) {
      const formattedMessages = response.data.map(msg => formatMessageData(msg))
      messages.value = removeDuplicateMessages(formattedMessages)
      hasMoreMessages.value = response.data.length === 20
    } else {
      // messages.value = []
      hasMoreMessages.value = false

      if (activeContact.value) {
        messages.value.push({
          messageId: 'welcome_' + Date.now(),
          content: '暂无聊天记录，开始发送第一条消息吧！',
          contentType: 'system',
          type: 'system',
          createTime: new Date().toISOString(),
          status: 'sent'
        })
      }
    }
  } catch (error) {
    console.error('加载聊天历史失败:', error)
    messages.value = []
    hasMoreMessages.value = false
  } finally {
    loadingMessages.value = false
    await nextTick()
    scrollToBottom('auto')
  }
}

const handleImageUpload = async (event) => {
  const files = Array.from(event.target.files)
  if (files.length === 0) return

  for (const file of files) {
    await uploadSingleFile(file, 'image')
  }
  event.target.value = ''
}

const handleFileUpload = async (event) => {
  const files = Array.from(event.target.files)
  if (files.length === 0) return

  for (const file of files) {
    await uploadSingleFile(file, 'file')
  }
  event.target.value = ''
}

const uploadSingleFile = async (file, fileType) => {
  if (!activeContact.value || !currentUserId.value) {
    ElMessage.error('请先选择聊天对象')
    return
  }

  const uploadId = 'upload_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)
  const fileMessageId = currentUserId.value + '_' + activeContact.value.userId + '_' + Date.now()

  const controller = new AbortController()
  uploadingFiles.value.push({
    id: uploadId,
    name: file.name,
    size: file.size,
    progress: 0,
    status: 'uploading',
    error: null,
    type: fileType,
    fileObj: file,
    controller
  })

  try {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('receiverId', activeContact.value.userId)
    formData.append('messageId', fileMessageId)

    const config = {
      signal: controller.signal,
      onUploadProgress: (progressEvent) => {
        if (progressEvent.total) {
          const percentCompleted = Math.round(
            (progressEvent.loaded * 100) / progressEvent.total
          )
          const fileItem = uploadingFiles.value.find(f => f.id === uploadId)
          if (fileItem) {
            fileItem.progress = percentCompleted
          }
        }
      }
    }

    // 调用上传文件接口
    const res = await socialApi.uploadChatFile(formData, config)

    if (res.code === 0 && res.data) {
      // 文件上传成功后，发送文件消息（使用 sendMessage 接口）
      await sendFileMessage(res.data, file)

      const fileItem = uploadingFiles.value.find(f => f.id === uploadId)
      if (fileItem) {
        fileItem.progress = 100
        fileItem.status = 'success'
      }

      setTimeout(() => {
        uploadingFiles.value = uploadingFiles.value.filter(f => f.id !== uploadId)
      }, 2000)
    } else {
      throw new Error(res.msg || '上传失败')
    }
  } catch (error) {
    console.error('文件上传失败:', error)
    const fileItem = uploadingFiles.value.find(f => f.id === uploadId)
    if (fileItem) {
      fileItem.status = 'error'
      fileItem.error = error.response?.data?.msg || error.message || '上传失败'
    }
  }
}


// 修改 sendFileMessage 函数，添加 WebSocket 和引用消息支持
const sendFileMessage = async (fileData, originalFile) => {
  // 确定消息类型和内容
  const contentType = fileData.fileType ||
    (originalFile.type?.startsWith('image/') ? 'image' :
      originalFile.type?.startsWith('video/') ? 'video' : 'file')
  const messageContent = contentType === 'image' ? '[图片]' :
    contentType === 'video' ? '[视频]' :
      `[文件] ${fileData.fileName || originalFile.name}`

  // 构建消息数据
  const messageData = {
    receiverId: activeContact.value.userId,
    content: messageContent,
    contentType: contentType,
    chatType: activeContact.value.isFriend ? 'friend' : 'stranger',
    messageId: fileData.messageId
  }

  // 构建 extraData
  const extraData = {
    type: 'file',
    fileId: fileData.id,
    fileName: fileData.fileName || originalFile.name,
    filePath: fileData.fileUrl || fileData.filePath,
    fileType: fileData.fileType || originalFile.type,
    fileSize: fileData.fileSize || originalFile.size,
    thumbnailPath: fileData.fileUrl || fileData.filePath
  }

  // 如果有引用消息，添加到 extraData
  if (quotedMessage.value) {
    extraData.quotedMessage = {
      messageId: quotedMessage.value.messageId,
      senderId: quotedMessage.value.senderId,
      content: getQuoteContent(quotedMessage.value),
      contentType: quotedMessage.value.contentType,
      createTime: quotedMessage.value.createTime
    }
  }

  messageData.extraData = extraData

  // 添加临时消息
  const tempMessage = {
    messageId: fileData.messageId,
    senderId: currentUserId.value,
    content: fileData.fileUrl || fileData.thumbnailUrl,
    contentType: contentType,
    createTime: new Date().toISOString(),
    isRead: false,
    status: 'sending',
    fileName: fileData.fileName || originalFile.name,
    fileSize: fileData.fileSize || originalFile.size,
    fileUrl: fileData.fileUrl || fileData.filePath,
    thumbnailUrl: fileData.fileUrl || fileData.filePath,
    senderAvatar: userStore.currentUser?.avatar,
    senderAvatarFrame: userStore.currentUser?.avatarFrame,
    quotedMessage: quotedMessage.value ? { ...quotedMessage.value } : null
  }

  messages.value.push(tempMessage)
  await nextTick()
  scrollToBottom()

  // 使用 WebSocket 发送
  const success = await userStore.sendMessage(messageData)

  if (success) {
    const index = messages.value.findIndex(m => m.messageId === fileData.messageId)
    if (index !== -1) {
      messages.value[index].status = 'sent'
      if (fileData.messageId) {
        messages.value[index].messageId = fileData.messageId
      }
    }
    updateContactLastMessage(messageContent)
    quotedMessage.value = null // 发送成功后清除引用
  } else {
    const index = messages.value.findIndex(m => m.messageId === fileData.messageId)
    if (index !== -1) {
      messages.value[index].status = 'failed'
    }
    console.log('文件消息已加入离线队列')
  }
}

const formatFileSize = (bytes) => {
  if (!bytes || bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const retryUpload = (file) => {
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过10MB')
    return
  }
  if (file.controller) { file.controller.abort(); file.controller = null }
  ElMessage.warning('正在尝试重新上传...')
  uploadingFiles.value = uploadingFiles.value.filter(f => f.id !== file.id)
  const fo = file.fileObj || new File([], file.name, { type: file.type || 'application/octet-stream' })
  if (fo.size > 0) uploadSingleFile(fo, file.type)
}

const cancelUpload = (file) => {
  if (file.controller) file.controller.abort()
  const index = uploadingFiles.value.findIndex(f => f.id === file.id)
  if (index !== -1) {
    uploadingFiles.value.splice(index, 1)
    ElMessage.success('取消上传成功')
  }
}

const stopStatusPolling = () => {
  Object.values(statusPollingTimers.value).forEach(timer => {
    clearInterval(timer)
  })
  statusPollingTimers.value = {}
}

const showMessageMenu = (event, message) => {
  activeMessageMenu.value = { ...message }
  messageMenuPosition.value = {
    x: event.clientX,
    y: event.clientY
  }

  setTimeout(() => {
    document.addEventListener('click', closeMessageMenu)
  }, 0)
}

const closeMessageMenu = () => {
  activeMessageMenu.value = null
  document.removeEventListener('click', closeMessageMenu)
}

const canWithdraw = (message) => {
  if (!message.createTime) return false
  if (message.status === 'withdrawn') return false

  const messageTime = new Date(message.createTime).getTime()
  const currentTime = Date.now()
  const timeDiff = currentTime - messageTime

  return timeDiff <= 2 * 60 * 1000
}

const withdrawMessage = async (message) => {
  if (!canWithdraw(message)) {
    ElMessage.error('消息发送时间过长，无法撤回')
    closeMessageMenu()
    return
  }

  try {
    const res = await socialApi.withdrawMessage({
      messageId: message.messageId,
      reason: ''
    })

    if (res.code === 0) {
      const index = messages.value.findIndex(m => m.messageId === message.messageId)
      if (index !== -1) {
        messages.value[index].status = 'withdrawn'
        messages.value[index].content = '【消息已撤回】'
      }

      ElMessage.success('消息撤回成功')
    }

  } catch (error) {
    console.error('撤回消息失败:', error)
    ElMessage.error(error.response?.data?.msg || '撤回消息失败')
  } finally {
    closeMessageMenu()
  }
}

const copyMessage = async (message) => {
  try {
    await navigator.clipboard.writeText(message.content)
    ElMessage.success('复制成功')
  } catch (error) {
    console.error('复制失败:', error)
    ElMessage.error('复制失败')
  } finally {
    closeMessageMenu()
  }
}

const resendMessage = async (message) => {
  try {
    const res = await socialApi.sendMessage({
      receiverId: activeContact.value.userId,
      content: message.content,
      contentType: message.contentType || 'text',
      chatType: activeContact.value.isFriend ? 'friend' : 'stranger',
      messageId: message.messageId
    })

    if (res.code === 0) {
      const index = messages.value.findIndex(m => m.messageId === message.messageId)
      if (index !== -1) {
        messages.value.splice(index, 1)
      }

      const newMsg = {
        ...message,
        messageId: res.data?.messageId,
        status: 'sent',
        createTime: new Date().toISOString()
      }
      messages.value.push(newMsg)

      ElMessage.success('消息重新发送成功')
    }

  } catch (error) {
    console.error('重新发送失败:', error)
    ElMessage.error(error.response?.data?.msg || '重新发送失败')
  } finally {
    closeMessageMenu()
  }
}

const downloadFile = (message) => {
  try {
    const fileUrl = message.fileUrl || message.content
    if (fileUrl) {
      const link = document.createElement('a')
      link.href = fileUrl
      link.download = message.fileName || 'download'
      link.style.display = 'none'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
    } else {
      ElMessage.error('文件链接无效')
    }
  } catch (error) {
    console.error('下载文件失败:', error)
    ElMessage.error('下载文件失败')
  } finally {
    closeMessageMenu()
  }
}

const previewImage = (message) => {
  previewImageUrl.value = message.content || message.fileUrl
}

const playVideo = (message) => {
  activeVideo.value = {
    url: message.content || message.fileUrl,
    thumbnail: message.thumbnailUrl
  }
}

const loadInitialRealData = async () => {
  try {
    const friendsRes = await socialApi.getFriendList()
    if (friendsRes.code === 0) {
      friendContacts.value = friendsRes.data.map(friend => ({
        userId: friend.friendId,
        username: friend.friendUsername,
        nickname: friend.friendNickname,
        avatar: friend.friendAvatar || defaultAvatar,
        avatarFrame: friend.friendAvatarFrame,
        groupName: friend.groupName,
        remark: friend.remark,
        isOnline: friend.isOnline || 'offline',
        isFriend: true,
        isBlocked: friend.isBlocked || false,
        lastMessage: friend.lastMessage || '暂无消息',
        lastTime: friend.lastInteractionTime,
        unreadCount: friend.unreadCount || 0
      }))
    }

    const recentChatsRes = await socialApi.getRecentChats()
    if (recentChatsRes.code === 0) {
      const friendIds = new Set(friendContacts.value.map(f => f.userId))
      const strangers = recentChatsRes.data.filter(
        stranger => !friendIds.has(stranger.relatedUserId)
      )

      strangerContacts.value = strangers.map(stranger => ({
        userId: stranger.relatedUserId,
        username: stranger.username,
        nickname: stranger.nickname || `用户${stranger.id}`,
        avatar: stranger.avatar || defaultAvatar,
        avatarFrame: stranger.avatarFrame,
        isOnline: stranger.isOnline || 'offline',
        lastMessage: stranger.lastMessage || '暂无消息',
        lastTime: stranger.lastMessageTime || new Date().toISOString(),
        unreadCount: stranger.unreadCount || 0
      }))
    }

    const notificationsRes = await socialApi.getNotifications()
    if (notificationsRes.code === 0) {
      systemNotifications.value = notificationsRes.data.map(notif => ({
        id: notif.id,
        type: notif.type,
        title: notif.title,
        content: notif.content,
        createTime: notif.createTime,
        isRead: notif.isRead,
        readTime: notif.readTime,
        relatedUser: notif.relatedUser,
        unread: notif.unreadCount || 0
      }))
    }

  } catch (error) {
    console.error('加载初始化数据失败:', error)
  }
}

const viewSystemNotification = (notification) => {
  socialApi.markAsRead(notification.id).then(res => {
    if (res.code === 0) {
      notification.isRead = true
    }
  })
}

const shouldShowDateDivider = (message, index) => {
  if (index === 0) return true

  const currentDate = new Date(message.createTime).toDateString()
  const prevDate = new Date(messages.value[index - 1].createTime).toDateString()

  return currentDate !== prevDate
}

const formatRelativeTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diffMs = now - date
  const diffMins = Math.floor(diffMs / 60000)
  const diffHours = Math.floor(diffMs / 3600000)
  const diffDays = Math.floor(diffMs / 86400000)

  if (diffMins < 1) return '刚刚'
  if (diffMins < 60) return `${diffMins}分钟前`
  if (diffHours < 24) return `${diffHours}小时前`
  if (diffDays < 7) return `${diffDays}天前`

  return date.toLocaleDateString()
}

const formatMessageDate = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const today = new Date()
  const yesterday = new Date(today)
  yesterday.setDate(yesterday.getDate() - 1)

  if (date.toDateString() === today.toDateString()) return '今天'
  if (date.toDateString() === yesterday.toDateString()) return '昨天'
  return date.toLocaleDateString()
}

const formatMessageTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleTimeString([], {
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 修改 handleClickOutside，避免与子组件冲突
const handleClickOutside = (event) => {
  // 如果 EmojiPicker 没有显示，直接返回
  if (!showEmojiPicker.value) return
  
  // 如果点击的是表情按钮，不处理（让 EmojiPicker 自己决定）
  if (emojiButtonRef.value?.contains(event.target)) {
    return
  }
  
  // 其他情况，延迟一点让子组件有机会处理
  setTimeout(() => {
    // 再次检查状态，避免重复关闭
    if (showEmojiPicker.value) {
      // 检查点击的元素是否在 EmojiPicker 内
      const picker = document.querySelector('.emoji-picker')
      if (picker && !picker.contains(event.target)) {
        showEmojiPicker.value = false
      }
    }
  }, 10)
}

// 新增：监听 WebSocket 连接状态
watch(() => userStore.wsConnected, (connected) => {
  if (connected) {
    ElMessage.success('已连接实时消息服务')
  } else {
    ElMessage.warning('实时消息服务已断开，正在尝试重连...')
  }
})

// 监听 props.visible 变化
watch(() => props.visible, (newVal) => {
  visible.value = newVal
  if (newVal && props.targetUser) {
    // 当窗口打开且有目标用户时，自动选择该用户
    nextTick(() => {
      findAndSelectContact(props.targetUser)
    })
  }
})

// 监听 targetUser 变化
watch(() => props.targetUser, (newVal) => {
  console.log('targetUser changed:', newVal)
  if (visible.value && newVal) {
    loadChatHistory()
    nextTick(() => {
      findAndSelectContact(newVal)
    })
  }
}, { deep: true, immediate: true })

// 在组件挂载时监听新消息
onMounted(() => {
  isComponentMounted.value = true
  fetchAvatarFrame()
  loadInitialRealData()
  window.addEventListener('resize', handleResize)
  // 添加点击外部监听
  document.addEventListener('click', handleClickOutside)
  if (window.innerWidth <= 768) {
    showContactList.value = false
  }

  // 监听 WebSocket 新消息
  websocketService.on('newMessage', (message) => {
    // 如果当前聊天对象是消息发送者，添加到消息列表
    if (activeContact.value && activeContact.value.userId === message.senderId) {
      messages.value.push(message)
      scrollToBottom()

      // 标记已读
      userStore.markMessageAsRead(message.senderId, message.messageId)
      websocketService.sendReadReceipt(message.senderId, message.messageId)
    }
  })
})

// 组件卸载时清理
onUnmounted(() => {
  isComponentMounted.value = false
  stopStatusPolling()
  document.removeEventListener('click', closeMessageMenu)
  document.removeEventListener('click', handleClickOutside) // 添加清理
  window.removeEventListener('resize', handleResize)
  // 清理输入状态
  if (typingTimeout) clearTimeout(typingTimeout)
  if (activeContact.value) {
    userStore.sendTypingStatus(activeContact.value.userId, false)
  }
})

const handleResize = () => {
  if (window.innerWidth <= 768) {
    showContactList.value = false
  } else {
    showContactList.value = true
  }
}
</script>

<style scoped>
.private-message-container {
  width: 100%;
  max-width: 1400px;
  height: 75vh;
  background: white;
  border-radius: 12px;
  overflow: hidden;
  position: relative;
  display: flex;

}

.modal-close-btn {
  position: absolute;
  top: 10px;
  right: 10px;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: rgba(160, 196, 241, 0.5);
  color: rgb(212, 129, 129);
  border: none;
  font-size: 20px;
  line-height: 1;
  cursor: pointer;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-close-btn:hover {
  scale: 1.1;
}

.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.3s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

/* 联系人列表样式 */
.contact-list {
  width: 300px;
  border-right: 1px solid #eaeaea;
  display: flex;
  flex-direction: column;
  background: #fafafa;
  height: 100%;
  overflow: hidden;
}

.list-header {
  padding: 20px;
  border-bottom: 1px solid #eaeaea;
  background: #fff;
  flex-shrink: 0;
}

.list-header h2 {
  margin: 0 0 10px 0;
  color: #ff69b4;
  font-size: 18px;
}

.unread-indicator {
  font-size: 13px;
  color: #666;
  display: flex;
  align-items: center;
  gap: 5px;
}

.unread-dot {
  width: 8px;
  height: 8px;
  background: #ff4757;
  border-radius: 50%;
}

.contact-categories {
  display: flex;
  padding: 10px;
  gap: 5px;
  background: #fff;
  border-bottom: 1px solid #eaeaea;
  flex-shrink: 0;
}

.category-btn {
  flex: 1;
  padding: 8px 0;
  border: none;
  background: #f5f5f5;
  border-radius: 20px;
  cursor: pointer;
  font-size: 13px;
  color: #666;
  transition: all 0.2s;
}

.category-btn.active {
  background: linear-gradient(135deg, #ff85a2 0%, #ff69b4 100%);
  color: white;
}

.contact-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

.contact-group {
  margin-bottom: 20px;
}

.group-title {
  font-size: 14px;
  font-weight: 600;
  color: #666;
  padding: 8px 12px;
  background: #f0f0f0;
  border-radius: 20px;
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.contact-item {
  display: flex;
  align-items: flex-start;
  padding: 12px;
  border-radius: 8px;
  margin-bottom: 4px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}

.contact-item:hover {
  background: #f0e6ff;
}

.contact-item.active {
  background: #e3f2fd;
}

.contact-item.unread {
  background: #fff3e0;
}

.avatar-container {
  position: relative;
  width: 40px;
  height: 40px;
  margin-right: 12px;
  flex-shrink: 0;
}

.contact-avatar {
  width: 75%;
  height: 75%;
  left: 1px;
  top: 8px;
  border-radius: 50%;
  object-fit: cover;
  position: relative;
  z-index: 1;
}

.contact-avatar-frame {
  position: absolute;
  top: -1px;
  left: -8px;
  width: 50px;
  height: 50px;
  pointer-events: none;
  z-index: 2;
  opacity: 70%;
}

.contact-info {
  flex: 1;
  min-width: 0;
}

.contact-name {
  font-weight: 500;
  font-size: 14px;
  color: #333;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 5px;
}

.contact-tag {
  font-size: 11px;
  color: #ff69b4;
  padding: 2px 6px;
  border-radius: 12px;
  background: #fff0f5;
}

.contact-status {
  display: flex;
  gap: 4px;
  margin-bottom: 4px;
}

.online-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  background: #4caf50;
  border-radius: 50%;
}

.offline-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  background: #999;
  border-radius: 50%;
}

.last-message {
  font-size: 12px;
  color: #666;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.contact-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  margin-left: 8px;
  flex-shrink: 0;
}

.last-time {
  font-size: 11px;
  color: #999;
  white-space: nowrap;
}

.unread-count {
  background: #ff4757;
  color: white;
  font-size: 11px;
  min-width: 18px;
  height: 18px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 5px;
}

.new-badge {
  color: #ff4757;
  font-weight: bold;
  margin-right: 4px;
}

/* 系统通知样式 */
.system-item {
  display: flex;
  align-items: flex-start;
  padding: 12px;
  border-radius: 8px;
  margin-bottom: 4px;
  cursor: pointer;
  transition: all 0.2s;
}

.system-item:hover {
  background: #f0f0f0;
}

.system-item.unread {
  background: #fff3e0;
}

.system-icon {
  font-size: 20px;
  margin-right: 12px;
  flex-shrink: 0;
}

.system-info {
  flex: 1;
  min-width: 0;
}

.system-title {
  font-weight: 500;
  font-size: 14px;
  color: #333;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
}

.system-content {
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.system-time {
  font-size: 11px;
  color: #999;
}

/* 聊天区域样式 */
.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  background: #fff9fa;
}

.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.chat-header {
  padding: 15px 20px;
  border-bottom: 1px solid #ffecef;
  display: flex;
  align-items: center;
  background: #fff;
  flex-shrink: 0;
}

.back-btn {
  background: none;
  border: none;
  font-size: 20px;
  margin-right: 10px;
  cursor: pointer;
  color: #ff69b4;
  display: none;
}

.chat-avatar {
  width: 38px;
  height: 35px;
  border-radius: 50%;
  object-fit: cover;
  margin-right: 12px;
}

.chat-avatar-frame {
  position: relative;
  top: 0;
  left: -65px;
  width: 65px;
  height: 65px;
  border-radius: 50%;
  border: 2px solid #fff;
  z-index: 1;
  pointer-events: none;
  opacity: 70%;
}

.chat-info {
  flex: 1;
  margin-left: -40px;
}

.chat-name {
  font-weight: 500;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.status {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
}

.status.online {
  background: #e8f5e9;
  color: #4caf50;
}

.status.offline {
  background: #f5f5f5;
  color: #999;
}

.chat-relation {
  font-size: 11px;
  color: #999;
}

.chat-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  margin-right: 20px;
  padding: 6px 8px;
  border-radius: 20px;
  border: 1px solid #ffecef;
  background: white;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.2s;
}

.action-btn:hover {
  background: #fff0f5;
}

.action-btn.add-friend {
  background: #ff69b4;
  color: white;
  border-color: #ff69b4;
}

.action-btn.add-friend:hover {
  background: #ff85c0;
}

.action-btn.clear {
  color: #f5222d;
}


/* 消息区域样式 */
.messages-container-wrapper {
  flex: 1;
  min-height: 0;
  height: 0;
  position: relative;
  overflow: hidden;
  background: #fff9fa;
}

.messages-container {
  height: 100%;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 20px;
  display: flex;
  flex-direction: column;
  scroll-behavior: smooth;
}

/* 自定义滚动条样式 */
.messages-container::-webkit-scrollbar {
  width: 6px;
}

.messages-container::-webkit-scrollbar-track {
  background: #ffecef;
  border-radius: 3px;
}

.messages-container::-webkit-scrollbar-thumb {
  background: #ff99b9;
  border-radius: 3px;
}

.messages-container::-webkit-scrollbar-thumb:hover {
  background: #ff69b4;
}

.message-wrapper {
  margin-bottom: 16px;
  width: 100%;
}

.message-item {
  display: flex;
  gap: 10px;
  width: 100%;
  margin-bottom: 16px;
}

.message-item.sent {
  flex-direction: row-reverse;
}

.message-item.received {
  flex-direction: row;
}



.avatar .avatar-container {
  position: relative;
  width: 50px;
  height: 50px;
  border-radius: 50%;
  flex-shrink: 0;
}

.avatar img {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  object-fit: cover;
}

.sender-avatar {
  margin-left: 10px;
}

.receiver-avatar {
  margin-right: 10px;
}

.avatar-container .othersAvatar-frame {
  position: absolute;
  top: -15px;
  left: -15px;
  width: 80px;
  height: 80px;
  border-radius: 50%;
  z-index: 1;
  opacity: 80%;
}

.avatar-container .currentAvatar-frame {
  position: absolute;
  top: -15px;
  left: -15px;
  width: 80px;
  height: 80px;
  border-radius: 50%;
  z-index: 1;
  opacity: 80%;
}

.message-content-wrapper {
  max-width: 70%;
  display: flex;
  flex-direction: column;
}

.message-item.sent .message-content-wrapper {
  align-items: flex-end;
}

.message-item.received .message-content-wrapper {
  align-items: flex-start;
}

.username {
  font-size: 14px;
  font-weight: bold;
  color: #666;
  margin-bottom: 4px;
  padding: 0 5px;
}

.receiver-username {
  color: #2a8be6;
  text-align: left;
}

.username-remark {
  font-size: 11px;
  color: #ee7322;
  margin-left: 4px;
}

.message-bubble {
  padding: 10px 15px;
  border-radius: 18px;
  position: relative;
  word-break: break-word;
  max-width: 100%;
}

.message-item.sent .message-bubble {
  background: linear-gradient(135deg, #e0adba 0%, #89d8c9 100%);
  color: white;
  border-bottom-right-radius: 5px;
  box-shadow: 0 2px 5px rgba(255, 105, 180, 0.3);
}

.message-item.received .message-bubble {
  background: linear-gradient(135deg, #8fc1ca 0%, #66d1a8 100%);
  color: #333;
  border-bottom-left-radius: 5px;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.3);
  border: 1px solid #ffecef;
}

.message-text {
  line-height: 1.4;
  white-space: pre-wrap;
  font-size: 14px;
  font-style: italic;
  font-weight: bold;
  font-family: 'Courier New', Courier, monospace;
  color: rgba(207, 93, 183, 0.9);
}

.message-withdrawn {
  color: rgba(245, 30, 59, 0.8);
  font-style: italic;
  padding: 5px 0;
}

.message-image img {
  max-width: 120px;
  max-height: 120px;
  border-radius: 8px;
  cursor: pointer;
  transition: transform 0.2s;
}

.message-image img:hover {
  transform: scale(1.02);
}

.message-video video {
  max-width: 150px;
  max-height: 120px;
  border-radius: 8px;
  cursor: pointer;
}

.message-file {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  min-width: 120px;
}

.message-item.sent .message-file {
  background: rgba(255, 255, 255, 0.2);
}

.message-item.received .message-file {
  background: #f8f9fa;
}

.file-icon {
  font-size: 28px;
}

.file-info {
  flex: 1;
  min-width: 0;
}

.file-name {
  font-weight: 500;
  margin-bottom: 4px;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-size {
  font-size: 11px;
  color: #999;
}

.message-item.sent .file-size {
  color: rgba(255, 255, 255, 0.8);
}

.download-btn {
  padding: 4px 12px;
  background: #ff69b4;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  white-space: nowrap;
}

.download-btn:hover {
  background: #ff85c0;
}

.message-system {
  font-size: 12px;
  color: #999;
  text-align: center;
  padding: 8px 15px;
  background: #f5f5f5;
  border-radius: 8px;
  width: 100%;
}

.message-meta {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 5px;
  margin-top: 5px;
  font-size: 11px;
}

.message-item.sent .message-meta {
  color: rgba(238, 106, 139, 0.8);
}

.message-item.received .message-meta {
  color: #999999;
}

.message-status {
  display: flex;
  align-items: center;
}

.status-icon {
  font-size: 12px;
}

.status-icon.sending {
  animation: spin 1s linear infinite;
}

.date-divider {
  text-align: center;
  margin: 20px 0;
  position: relative;
}

.date-divider::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  height: 1px;
  background: #ffecef;
  z-index: 1;
}

.date-divider span {
  background: #fff9fa;
  padding: 0 15px;
  position: relative;
  z-index: 2;
  color: #999;
  font-size: 12px;
}

.empty-messages {
  text-align: center;
  padding: 60px 20px;
  color: #999;
  font-size: 14px;
}

/* 修复：回到底部按钮 */
.scroll-to-bottom-btn {
  position: relative;
  left: 40%;
  padding: 5px 10px;
  width: 100px;
  background: #b5d0e6;
  color: rgb(238, 136, 224);
  border: none;
  border-radius: 30px;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(94, 235, 245, 0.7);
  transition: all 0.3s;
  z-index: 100;
  font-size: 10px;
  align-items: center;
  gap: 8px;
}

.scroll-to-bottom-btn:hover {
  transform: translateY(-2px);
  scale: 1.1;
}

.unread-badge {
  background: #ff4757;
  color: white;
  font-size: 11px;
  min-width: 18px;
  height: 18px;
  border-radius: 9px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 5px;
}

/* 底部锚点 */
.scroll-bottom-anchor {
  height: 1px;
  width: 100%;
}

.emoji-picker-wrapper {
  position: relative;
  bottom: 400px;
  z-index: 1002;
}

/* 输入区域样式 */
.input-area {
  padding: 15px 20px;
  border-top: 1px solid #ffecef;
  background: #fff;
  flex-shrink: 0;
  position: relative;
  z-index: 100;
}

.input-tools {
  display: flex;
  gap: 10px;
  margin-bottom: 10px;
}

.tool-btn {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  padding: 5px;
  border-radius: 50%;
  transition: all 0.2s;
  color: #ff69b4;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.tool-btn:hover {
  background: #fff0f5;
  transform: scale(1.1);
}

.input-box {
  position: relative;
}

.input-box textarea {
  width: 100%;
  padding: 12px 80px 12px 15px;
  border: 1px solid #ffd6e7;
  border-radius: 20px;
  font-size: 14px;
  line-height: 1.5;
  resize: none;
  outline: none;
  transition: all 0.3s;
  background: #fff9fa;
  color: #333;
  font-family: inherit;
}

.input-box textarea:focus {
  border-color: #ff69b4;
  box-shadow: 0 0 0 2px rgba(255, 105, 180, 0.1);
}

.input-actions {
  position: absolute;
  right: 10px;
  bottom: 8px;
}

.send-btn {
  padding: 6px 20px;
  background: linear-gradient(135deg, #ff85a2 0%, #ff69b4 100%);
  color: white;
  border: none;
  border-radius: 15px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.send-btn:hover:not(:disabled) {
  opacity: 0.9;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(255, 105, 180, 0.3);
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 上传进度样式 */
.upload-progress {
  background: #fff9fa;
  border-radius: 8px;
  padding: 10px;
  margin-bottom: 10px;
  max-height: 150px;
  overflow-y: auto;
  border: 1px dashed #ffd6e7;
}

.upload-item {
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 8px;
  background: white;
  border-radius: 6px;
  margin-bottom: 8px;
  border: 1px solid #ffecef;
}

.warning-text {
  font-size: 12px;
  color: rgba(255, 77, 79, 0.8);
  font-weight: 500;
  font-style: italic;
  text-align: center;
  margin-bottom: 10px;
}

.upload-item .file-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.upload-item .file-name {
  font-size: 12px;
  font-weight: 500;
  color: #333;
}

.upload-item .file-size {
  font-size: 11px;
  color: #999;
}

.progress-bar {
  position: relative;
  height: 20px;
  background: #ffecef;
  border-radius: 10px;
  overflow: hidden;
}

.progress {
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  background: linear-gradient(90deg, #ff85a2, #ff69b4);
  transition: width 0.3s;
}

.progress-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 11px;
  color: #333;
  z-index: 1;
  font-weight: 500;
}

.retry-btn,
.cancel-btn {
  align-self: flex-end;
  padding: 2px 8px;
  font-size: 11px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  margin-left: 5px;
}

.retry-btn {
  background: #ff4d4f;
  color: white;
}

.retry-btn:hover {
  background: #ff7875;
}

.cancel-btn {
  background: #999;
  color: white;
}

.cancel-btn:hover {
  background: #b3b3b3;
}

/* 右键菜单样式 */
.message-context-menu {
  position: fixed;
  background: white;
  border: 1px solid #ffecef;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(255, 105, 180, 0.2);
  z-index: 2100;
  min-width: 140px;
  overflow: hidden;
}

.menu-item {
  display: block;
  width: 100%;
  padding: 10px 16px;
  text-align: left;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 13px;
  color: #333;
  transition: all 0.2s;
}

.menu-item:hover {
  background: #fff0f5;
}

.menu-item.withdraw {
  color: #ff4d4f;
}

.menu-item.withdraw:hover {
  background: #fff2f0;
}

.menu-item.delete {
  color: #ff4d4f;
}

.menu-item.delete:hover {
  background: #fff2f0;
}

/* 图片预览模态框 */
.image-preview-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2200;
  cursor: pointer;
}

.image-preview-modal img {
  max-width: 90%;
  max-height: 90%;
  object-fit: contain;
  cursor: default;
}

/* 加载更多指示器 */
.load-more-indicator {
  text-align: center;
  margin-top: 20px
}

.load-more-btn {
  padding: 4px 10px;
  background: #fff;
  border: 1px solid #ff69b4;
  color: #ff69b4;
  border-radius: 20px;
  cursor: pointer;
  font-size: 10px;
  transition: all 0.2s;
}

.load-more-btn:hover:not(:disabled) {
  background: #fff0f5;
  transform: translateY(-1px);
}

.load-more-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 没有更多消息提示 */
.no-more-messages {
  text-align: center;
  padding: 10px;
  color: #999;
  font-size: 12px;
  margin-top: 10px;
}

.loading-spinner {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #ff69b4;
  font-size: 13px;
}

.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid #ffecef;
  border-top-color: #ff69b4;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* 无聊天占位 */
.no-chat-selected {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  background: #fff9fa;
}

.placeholder-content {
  text-align: center;
  color: #999;
  padding: 20px;
}

.placeholder-content img {
  width: 150px;
  opacity: 0.6;
  margin-bottom: 20px;
}

.placeholder-content h3 {
  margin: 0 0 10px 0;
  color: #333;
  font-size: 18px;
}

.placeholder-content p {
  margin: 0;
  font-size: 14px;
}

/* 引用消息预览样式（输入框上方） */
.quote-preview {
  padding: 8px 12px;
  margin-bottom: 8px;
  background: #f0f0f0;
  border-left: 4px solid #ff69b4;
  border-radius: 4px;
  display: flex;
  align-items: center;
  position: relative;
}

.quote-content {
  flex: 1;
  font-size: 12px;
  color: #666;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.quote-label {
  color: #ff69b4;
  font-weight: bold;
  font-size: 11px;
  background: #fff0f5;
  padding: 2px 6px;
  border-radius: 10px;
}

.quote-sender {
  color: #333;
  font-weight: 500;
}

.quote-text {
  color: #999;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cancel-quote {
  background: none;
  border: none;
  font-size: 18px;
  color: #999;
  cursor: pointer;
  padding: 0 4px;
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
}

.cancel-quote:hover {
  color: #ff4757;
}

/* 消息气泡中的引用预览 */
.quoted-message-preview {
  padding: 6px 10px;
  margin-bottom: 8px;
  background: rgba(255, 255, 255, 0.3);
  border-left: 3px solid #ff69b4;
  border-radius: 4px;
  font-size: 12px;
}

.message-item.sent .quoted-message-preview {
  background: rgba(255, 255, 255, 0.2);
  border-left-color: #fff;
}

.quoted-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
}

.quoted-sender {
  font-weight: 500;
  color: #333;
}

.message-item.sent .quoted-sender {
  color: rgba(63, 159, 223, 0.8);
}

.quoted-time {
  font-size: 10px;
  color: #999;
}

.message-item.sent .quoted-time {
  color: rgba(216, 63, 63, 0.8);
}

.quoted-content {
  color: #666;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.message-item.sent .quoted-content {
  color: rgba(255, 255, 255, 0.9);
}

/* 右键菜单新增样式 */
.menu-divider {
  height: 1px;
  background: #ffecef;
  margin: 4px 0;
}

.menu-item.quote {
  color: #2d8cf0;
}

.menu-item.quote:hover {
  background: #e6f7ff;
}

.menu-item.delete {
  color: #faad14;
}

.menu-item.delete:hover {
  background: #fff7e6;
}

/* 输入状态提示样式 */
.typing-indicator {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #999;
  margin-left: 12px;
  padding: 4px 8px;
  background: #f5f5f5;
  border-radius: 12px;
}

.typing-indicator .dot {
  width: 4px;
  height: 4px;
  background: #999;
  border-radius: 50%;
  animation: typingBounce 1.4s infinite;
}

.typing-indicator .dot:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-indicator .dot:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typingBounce {

  0%,
  60%,
  100% {
    transform: translateY(0);
  }

  30% {
    transform: translateY(-4px);
  }
}

/* 连接状态提示 */
.connection-status {
  position: fixed;
  bottom: 20px;
  right: 20px;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 12px;
  z-index: 9999;
  animation: slideIn 0.3s;
}

.connection-status.connected {
  background: #e8f5e9;
  color: #4caf50;
}

.connection-status.disconnected {
  background: #ffebee;
  color: #f44336;
}

@keyframes slideIn {
  from {
    transform: translateX(100%);
    opacity: 0;
  }

  to {
    transform: translateX(0);
    opacity: 1;
  }
}

/* 移动端适配 */
.mobile-mode .contact-list {
  position: absolute;
  top: 0;
  left: 0;
  bottom: 0;
  width: 100%;
  z-index: 10;
  transition: transform 0.3s;
}

.mobile-mode .contact-list.hidden {
  transform: translateX(-100%);
}

.mobile-mode .chat-area.full-width {
  width: 100%;
}

.mobile-mode .back-btn {
  display: block;
}

/* 动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 加载更多动画 */
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>