<template>
  <div class="group-chat" :class="{ 'has-bg': props.backgroundImage }" :style="bgStyle">
    <!-- Left: Group List Sidebar -->
    <div class="group-sidebar">
      <div class="sidebar-header">
        <h3>群聊列表</h3>
        <button class="create-btn" @click="showCreateDialog = true" title="创建群聊">+</button>
      </div>
      <div class="group-search">
        <input v-model="groupSearch" placeholder="搜索群聊...">
      </div>
      <div class="group-list">

        <div v-for="group in filteredGroups" :key="group.groupId" class="group-item"
          :class="{ active: roomStore.currentGroupId === group.groupId }" @click="selectGroup(group.groupId)">
          <img :src="group.avatar || '/default-avatar.png'" class="group-avatar">
          <div class="group-info">
            <div class="group-name">
              {{ group.name }}
              <span v-if="group.joinPermission && !roomStore.isCurrentMember" class="join-tag" :class="'tag-' + group.joinPermission">
                {{ joinPermLabel(group.joinPermission) }}
              </span>
            </div>
            <div class="group-last-msg">{{ group.lastMessage || '暂无消息' }}</div>
          </div>
          <div class="group-meta">
            <div class="group-time">{{ formatRelativeTime(group.lastMessageTime) }}</div>
            <span v-if="roomStore.unreadCounts[group.groupId]" class="unread-badge">
              {{ roomStore.unreadCounts[group.groupId] }}
            </span>
          </div>
        </div>
        <div v-if="myGroups.length === 0" class="empty-groups">暂无群聊，创建一个吧</div>
      </div>
    </div>

    <!-- Center: Chat Area -->
    <div class="chat-main" v-if="currentGroup">
      <div class="chat-header">
        <div class="chat-header-info">
          <img :src="currentGroup.avatar || '/default-avatar.png'" class="header-avatar">
          <div>
            <div class="header-name">{{ currentGroup.name }}</div>
            <div class="header-count">{{ currentMembers.length }} 名成员</div>
          </div>
        </div>
        <div class="header-actions-row">
          <button v-if="!roomStore.isCurrentMember" class="join-btn" @click="handleJoinGroup">加入群聊</button>
          <button class="settings-btn" @click="showSettings = true" title="群设置">&#x2699;</button>
        </div>
      </div>

      <div v-if="!roomStore.isCurrentMember" class="not-member-banner">
        你尚未加入该群聊，无法发送消息
      </div>

      <div v-if="roomStore.isCurrentMember" class="messages-area" ref="messagesContainer" @scroll="handleScroll">
        <div v-if="hasMore" class="load-more">
          <button v-if="!loadingMore" @click="loadMoreHistory" class="load-more-btn">加载更早的消息</button>
          <span v-else class="loading-text">加载中...</span>
        </div>
        <div v-else-if="currentMessages.length > 0" class="no-more">没有更早的消息了</div>

        <template v-for="(msg, idx) in currentMessages" :key="msg.messageId || idx">
          <div v-if="shouldShowDateDivider(msg, idx)" class="date-divider">
            <span>{{ formatDate(msg.createTime) }}</span>
          </div>

          <div v-if="msg.contentType === 'system'" class="system-event">
            <span>{{ msg.content }}</span>
          </div>

          <div v-else class="message-row" :class="{ 'my-msg': msg.senderId === currentUserId }"
            @contextmenu.prevent="onMsgContextMenu(msg, $event)">
            <div class="msg-avatar" v-if="msg.senderId !== currentUserId">
              <img :src="msg.senderAvatar || '/default-avatar.png'">
            </div>
            <div class="msg-body">
              <div v-if="msg.senderId !== currentUserId" class="msg-sender-name">
                {{ msg.senderName || '未知用户' }}
              </div>
              <div class="msg-bubble">
                <!-- Quoted message preview -->
                <div v-if="msg.quotedMessage" class="quoted-msg-preview">
                  <div class="quoted-header">
                    <span class="quoted-sender">{{ msg.quotedMessage.senderId === currentUserId ? '我' :
                      (msg.quotedMessage.senderName || '未知') }}</span>
                  </div>
                  <div class="quoted-content">{{ getQuoteContent(msg.quotedMessage) }}</div>
                </div>

                <!-- Text message -->
                <div v-if="msg.contentType === 'text' && msg.status !== 'withdrawn' && msg.status !== 'failed'" class="msg-content">{{ msg.content
                  }}</div>

                <!-- Image message -->
                <div v-if="msg.contentType === 'image' && msg.status !== 'withdrawn' && msg.status !== 'failed'" class="msg-image">
                  <img :src="msg.fileUrl || msg.content" @click="previewImage(msg)" class="msg-image-preview" alt="图片"
                    loading="lazy">
                  <div class="image-name" v-if="msg.fileName">{{ msg.fileName }}</div>
                </div>

                <!-- Video message -->
                <div v-if="msg.contentType === 'video' && msg.status !== 'withdrawn' && msg.status !== 'failed'" class="msg-video">
                  <video :src="msg.fileUrl || msg.content" controls preload="metadata" class="msg-video-player">
                    您的浏览器不支持视频播放
                  </video>
                  <div class="video-name" v-if="msg.fileName">{{ msg.fileName }}</div>
                </div>

                <!-- File message -->
                <div v-if="msg.contentType === 'file' && msg.status !== 'withdrawn' && msg.status !== 'failed'" class="msg-file">
                  <div class="file-icon">&#x1F4C4;</div>
                  <div class="file-info">
                    <div class="file-name">{{ msg.fileName || '文件' }}</div>
                    <div class="file-size">{{ msg.fileSize || '' }}</div>
                  </div>
                  <button @click="downloadFile(msg)" class="download-btn">下载</button>
                </div>

                <!-- Withdrawn message -->
                <div v-if="msg.status === 'withdrawn' || msg.content === '消息已撤回'" class="msg-withdrawn">
                  [{{ msg.senderName || '用户' }}] 已撤回该消息
                </div>

                <div class="msg-meta">
                  <span class="msg-time" title="发送时间">{{ formatTime(msg.createTime) }}</span>
                  <span class="msg-status">
                    <span v-if="msg.status === 'sending'" class="status-icon sending" title="发送中">&#x23F3;</span>
                    <span v-else-if="msg.status === 'sent'" class="status-icon sent" title="已发送">&#x2713;</span>
                    <span v-else-if="msg.status === 'withdrawn'" class="status-icon withdrawn"
                      title="已撤回">&#x1F519;</span>
                    <span v-else-if="msg.status === 'failed'" class="status-icon failed" title="发送失败">&#x26A0;</span>
                  </span>
                </div>
              </div>
               <div class="my-avatar" v-if="msg.senderId === currentUserId">
              <img :src="msg.senderAvatar || currentUserAvatar">
            </div>
            </div>
          </div>
        </template>

        <div v-if="currentMessages.length === 0 && !loadingMore" class="empty-chat">暂无消息，开始聊天吧</div>
        <div ref="scrollAnchor"></div>
      </div>

      <button v-if="!isAtBottom && currentMessages.length > 0" class="scroll-bottom-btn" @click="scrollToBottom">
        &#x2193; 回到底部
        <span v-if="unreadBelow > 0" class="unread-count-badge">{{ unreadBelow }}</span>
      </button>

      <!-- Input Area -->
      <div class="input-area">
        <div class="input-tools">
          <button @click="toggleEmojiPicker" ref="emojiBtnRef" class="tool-btn" title="表情">&#x1F60A;</button>
          <button @click="triggerImageUpload" class="tool-btn" title="图片">🖼️</button>
          <button @click="triggerFileUpload" class="tool-btn" title="文件">&#x1F4C4;</button>
          <input type="file" ref="imageInput" accept="image/*" style="display: none" @change="handleImageUpload"
            multiple>
          <input type="file" ref="fileInput" style="display: none" @change="handleFileUpload" multiple>
        </div>

        <div class="emoji-picker-wrapper" v-if="showEmojiPicker">
          <EmojiPicker :key="'emoji-picker-' + showEmojiPicker" :visible="showEmojiPicker" :targetElement="emojiBtnRef"
            @select="insertEmoji" @select-pack="sendEmojiPackMessage" @close="showEmojiPicker = false"
            @update:visible="showEmojiPicker = $event" />
        </div>

        <!-- Upload progress -->
        <div v-if="uploadingFiles.length > 0" class="upload-progress">
          <div v-for="file in uploadingFiles" :key="file.id" class="upload-item">
            <span class="warning-text">图片和视频(最大10MB)上传速度较慢，请耐心等待...</span>
            <div class="file-info-row">
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
          <!-- Quote preview -->
          <div v-if="quotedMessage" class="quote-preview">
            <div class="quote-content">
              <span class="quote-label">引用消息:</span>
              <span class="quote-sender">{{ quotedMessage.senderId === currentUserId ? '我' : (quotedMessage.senderName
                ||
                '用户') }}</span>
              <span class="quote-text">{{ getQuoteContent(quotedMessage) }}</span>
              <button @click="cancelQuote" class="cancel-quote">&times;</button>
            </div>
          </div>
          <textarea v-model="inputText" @keydown.enter.exact.prevent="sendMessage"
            @keydown.enter.shift.exact.prevent="inputText += '\n'" placeholder="输入消息... (Enter发送，Shift+Enter换行)"
            rows="2" ref="msgInput"></textarea>
          <div class="input-actions">
            <button @click="sendMessage" :disabled="!inputText.trim()" class="send-btn">发送</button>
          </div>
        </div>
      </div>
    </div>

    <!-- No group selected -->
    <div v-else class="no-group">
      <p>选择一个群聊开始聊天</p>
    </div>

    <!-- Right: Member List Sidebar -->
    <div class="member-sidebar" v-if="currentGroup">
      <GroupMemberList :members="currentMembers" :my-role="myRoleInGroup"
        @view-profile="onViewProfile"
        @mute-or-unmute="onMuteOrUnmuteMember"
        @kick="onKickMember"
        @change-member-role="onChangeMemberRole"
        @transfer-owner="onTransferOwner"
        @demote-self="onDemoteSelf" />
    </div>

    <!-- Group Settings Panel -->
    <GroupSettings v-if="showSettings" :visible="showSettings" :group-id="currentGroup?.groupId"
      :my-role="myRoleInGroup" @close="showSettings = false" @dissolved="onDissolved" />

    <!-- Create Group Dialog -->
    <CreateGroupDialog v-if="showCreateDialog" :visible="showCreateDialog" @close="showCreateDialog = false"
      @created="onGroupCreated" />

    <!-- Message Context Menu -->
    <div v-if="msgMenuVisible" class="context-menu" :style="msgMenuStyle" @click.stop>
      <div v-if="msgMenuTarget?.contentType === 'text'" class="menu-item" @click="copyMsgContent">复制</div>
      <div class="menu-item" @click="quoteMsg">引用</div>
      <div v-if="msgMenuTarget?.senderId === currentUserId && canWithdrawMsg && msgMenuTarget.status !== 'withdrawn'"
        class="menu-item danger" @click="withdrawMsg">撤回</div>
    </div>

    <!-- Mute Duration Dialog -->
    <div v-if="showMuteInput" class="dialog-overlay" @click.self="showMuteInput = false">
      <div class="dialog-box">
        <h4>禁言成员</h4>
        <div class="form-group">
          <label>禁言时长（分钟）</label>
          <input v-model.number="muteDuration" type="number" min="1" max="43200" placeholder="分钟数">
        </div>
        <div class="dialog-actions">
          <button @click="showMuteInput = false" class="btn-cancel">取消</button>
          <button @click="confirmMute" class="btn-confirm">确认</button>
        </div>
      </div>
    </div>

    <!-- Join Group Dialog -->
    <div v-if="showJoinDialog" class="dialog-overlay" @click.self="showJoinDialog = false">
      <div class="dialog-box">
        <h4>加入群聊</h4>
        <div class="join-group-info">
          <img :src="currentGroup?.avatar || '/default-avatar.png'" class="join-avatar">
          <div class="join-name">{{ currentGroup?.name }}</div>
          <div class="join-meta">{{ currentMembers.length }} 名成员 · {{ joinPermLabel(currentGroup?.joinPermission) }}</div>
        </div>
        <div v-if="currentGroup?.joinPermission === 'invite'" class="form-group">
          <label>邀请码</label>
          <input v-model="inviteCodeInput" placeholder="请输入邀请码">
        </div>
        <div v-if="error" class="error-msg">{{ error }}</div>
        <div class="dialog-actions">
          <button @click="showJoinDialog = false" class="btn-cancel">取消</button>
          <button @click="confirmJoin" class="btn-confirm" :disabled="joining">
            {{ joining ? '加入中...' : '确认加入' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Image Preview Modal -->
    <div v-if="previewImageUrl" class="image-preview-modal" @click="previewImageUrl = null">
      <img :src="previewImageUrl" @click.stop>
    </div>

    <!-- User Detail Modal -->
    <UserDetail v-model:visible="showUserDetailModal" :friendId="selectedUserId" @close="selectedUserId = null" />
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoomStore } from '@/stores/room'
import { useUserInfoStore } from '@/stores/userInfo'
import * as groupChatApi from '@/api/groupChatApi'
import * as worldChatApi from '@/api/worldChatApi'
import GroupMemberList from '@/components/GroupMemberList.vue'
import GroupSettings from '@/components/GroupSettings.vue'
import CreateGroupDialog from '@/components/CreateGroupDialog.vue'
import EmojiPicker from '@/views/EmojiPicker.vue'
import UserDetail from '@/views/UserDetail.vue'

const props = defineProps({
  backgroundImage: { type: String, default: '' },
  backgroundOpacity: { type: Number, default: 1 }
})

const roomStore = useRoomStore()
const userInfoStore = useUserInfoStore()

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

const currentUserId = computed(() => userInfoStore.currentUser?.id)
const currentUserAvatar = computed(() => userInfoStore.currentUser?.avatar || '/default-avatar.png')
const myGroups = computed(() => roomStore.myGroups)
const currentGroup = computed(() => roomStore.currentGroup)
const currentMessages = computed(() => roomStore.currentMessages)
const currentMembers = computed(() => roomStore.currentMembers)

const groupSearch = ref('')
const inputText = ref('')
const showSettings = ref(false)
const showCreateDialog = ref(false)
const messagesContainer = ref(null)
const msgInput = ref(null)
const scrollAnchor = ref(null)
const loadingMore = ref(false)
const hasMore = ref(true)
const isAtBottom = ref(true)
const unreadBelow = ref(0)

// Emoji & file upload
const showEmojiPicker = ref(false)
const emojiBtnRef = ref(null)
const imageInput = ref(null)
const fileInput = ref(null)
const uploadingFiles = ref([])
const previewImageUrl = ref(null)

// User detail modal
const showUserDetailModal = ref(false)
const selectedUserId = ref(null)

// Message context menu
const msgMenuVisible = ref(false)
const msgMenuTarget = ref(null)
const msgMenuStyle = ref({})
let msgMenuTimer = null

// Quoting
const quotedMessage = ref(null)

// Join group
const showJoinDialog = ref(false)
const inviteCodeInput = ref('')
const joining = ref(false)
const error = ref('')

const joinPermLabel = (perm) => {
  const map = { anyone: '自由加入', approval: '需审核', invite: '仅邀请' }
  return map[perm] || perm
}

const handleJoinGroup = () => {
  inviteCodeInput.value = ''
  error.value = ''
  showJoinDialog.value = true
}

const confirmJoin = async () => {
  if (!currentGroup.value) return
  joining.value = true; error.value = ''
  try {
    const data = {}
    if (currentGroup.value.joinPermission === 'invite') {
      if (!inviteCodeInput.value.trim()) { error.value = '请输入邀请码'; joining.value = false; return }
      data.inviteCode = inviteCodeInput.value.trim()
    }
    await roomStore.joinGroup(currentGroup.value.groupId, data)
    ElMessage.success(currentGroup.value.joinPermission === 'approval' ? '申请已提交，等待管理员审核' : '已加入群聊')
    showJoinDialog.value = false
    // Refresh to get messages
    await roomStore.setCurrentGroup(currentGroup.value.groupId)
  } catch (e) {
    error.value = e.message || '加入失败'
  } finally {
    joining.value = false
  }
}

// Mute
const showMuteInput = ref(false)
const muteTarget = ref(null)
const muteDuration = ref(60)

// Remote search with debounce
const searchResults = ref([])
const isSearching = ref(false)
let searchTimer = null

const filteredGroups = computed(() => {
  if (!groupSearch.value.trim()) return myGroups.value
  return searchResults.value
})

const doSearch = async (keyword) => {
  isSearching.value = true
  try {
    const results = await roomStore.searchGroups(keyword)
    searchResults.value = results || []
  } catch (e) {
    searchResults.value = []
  } finally {
    isSearching.value = false
  }
}

watch(groupSearch, (val) => {
  clearTimeout(searchTimer)
  if (!val || !val.trim()) {
    searchResults.value = []
    return
  }
  searchTimer = setTimeout(() => doSearch(val.trim()), 300)
})

const myMemberInfo = computed(() => {
  if (!currentGroup.value) return null
  return currentMembers.value.find(m => m.userId === currentUserId.value) || null
})
const myRoleInGroup = computed(() => myMemberInfo.value?.role || 'member')
const isMuted = computed(() => myMemberInfo.value?.isMuted || false)

const canWithdrawMsg = computed(() => {
  if (!msgMenuTarget.value?.createTime) return false
  return Date.now() - new Date(msgMenuTarget.value.createTime).getTime() <= 2 * 60 * 1000
})

// ==================== Format Helpers ====================
const formatTime = (t) => {
  if (!t) return ''
  return new Date(t).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}
const formatDate = (t) => {
  if (!t) return ''
  const d = new Date(t)
  const today = new Date()
  const yesterday = new Date(today); yesterday.setDate(yesterday.getDate() - 1)
  if (d.toDateString() === today.toDateString()) return '今天'
  if (d.toDateString() === yesterday.toDateString()) return '昨天'
  return d.toLocaleDateString()
}
const formatRelativeTime = (t) => {
  if (!t) return ''
  const diff = Date.now() - new Date(t).getTime()
  const mins = Math.floor(diff / 60000)
  if (mins < 1) return '刚刚'
  if (mins < 60) return `${mins}分钟前`
  const hours = Math.floor(mins / 60)
  if (hours < 24) return `${hours}小时前`
  return new Date(t).toLocaleDateString()
}
const formatFileSize = (bytes) => {
  if (!bytes || bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const shouldShowDateDivider = (msg, idx) => {
  if (idx === 0) return true
  const prev = currentMessages.value[idx - 1]
  return new Date(msg.createTime).toDateString() !== new Date(prev.createTime).toDateString()
}

const getQuoteContent = (msg) => {
  if (!msg) return ''
  switch (msg.contentType) {
    case 'text': return (msg.content || '').substring(0, 50) + ((msg.content?.length > 50) ? '...' : '')
    case 'image': return '[图片]' + (msg.fileName || '[图片]')
    case 'video': return '[视频]' + (msg.fileName || '[视频]')
    case 'file': return '[文件] ' + (msg.fileName || '[文件]')
    default: return msg.content || ''
  }
}

// ==================== Navigation ====================
const selectGroup = async (groupId) => {
  if (roomStore.currentGroupId === groupId) return
  msgMenuVisible.value = false
  await roomStore.setCurrentGroup(groupId)
  hasMore.value = true
  unreadBelow.value = 0
  isAtBottom.value = true
  await nextTick()
  scrollToBottom()
}

// ==================== Messaging ====================
const sendMessage = async () => {

  // Mute check
  if (isMuted.value) {
    ElMessage.error('你已被禁言，无法发送消息')
    return
  }
  if (currentGroup.value?.isMutedAll && myRoleInGroup.value === 'member') {
    ElMessage.error('当前已开启全员禁言，仅群主和管理员可发言')
    return
  }

  // Check if currentMember is in the group
  if (!currentMembers.value.find(m => m.userId === currentUserId.value)) {
    ElMessage.error('你已不在该群聊中,无法发送消息')
    return
  }

  const text = inputText.value.trim()
  if (!text || !currentGroup.value) return

  const groupId = currentGroup.value.groupId
  const messageId = currentUserId.value + '_' + groupId + '_' + Date.now()

  const data = {
    content: text,
    contentType: 'text',
    messageId: messageId,
    extraData: {}
  }

  if (quotedMessage.value) {
    data.extraData.quotedMessage = {
      messageId: quotedMessage.value.messageId,
      senderId: quotedMessage.value.senderId,
      senderName: quotedMessage.value.senderName,
      content: quotedMessage.value.content,
      contentType: quotedMessage.value.contentType,
      createTime: quotedMessage.value.createTime
    }
  }

  // Optimistically add to local array
  const tempMsg = {
    messageId: messageId,
    groupId: groupId,
    senderId: currentUserId.value,
    senderName: userInfoStore.currentUser?.username || '我',
    senderAvatar: currentUserAvatar.value,
    content: text,
    contentType: 'text',
    status: 'sending',
    createTime: new Date().toISOString(),
    quotedMessage: quotedMessage.value ? { ...quotedMessage.value } : null
  }
  roomStore.groupMessages[groupId] = roomStore.groupMessages[groupId] || []
  roomStore.groupMessages[groupId].push(tempMsg)

  inputText.value = ''
  const quote = quotedMessage.value
  quotedMessage.value = null
  await nextTick()
  scrollToBottom()

  try {
    const res = await groupChatApi.sendGroupMessage(groupId, data)
    if (res.code !== 0) throw new Error(res.msg || '发送失败')
    // Update status to sent
    const msgs = roomStore.groupMessages[groupId]
    const idx = msgs.findIndex(m => m.messageId === messageId)
    if (idx !== -1) msgs[idx].status = 'sent'
  } catch (e) {
    // Mark as failed
    const msgs = roomStore.groupMessages[groupId]
    const idx = msgs.findIndex(m => m.messageId === messageId)
    if (idx !== -1) msgs[idx].status = 'failed'
    ElMessage.error('发送失败，请重试')
  }
  msgInput.value?.focus()
}

// ==================== Emoji ====================
const toggleEmojiPicker = () => { showEmojiPicker.value = !showEmojiPicker.value }
const insertEmoji = (emoji) => {
  inputText.value += emoji
  msgInput.value?.focus()
}
const sendEmojiPackMessage = async (packItem) => {
  if (!currentGroup.value) return
  const groupId = currentGroup.value.groupId
  const messageId = currentUserId.value + '_' + groupId + '_' + Date.now()

  const tempMsg = {
    messageId: messageId, groupId: groupId,
    senderId: currentUserId.value,
    senderName: userInfoStore.currentUser?.username || '我',
    senderAvatar: currentUserAvatar.value,
    content: '[表情包]',
    contentType: 'image',
    status: 'sending',
    createTime: new Date().toISOString(),
    fileUrl: packItem.imageUrl
  }
  roomStore.groupMessages[groupId] = roomStore.groupMessages[groupId] || []
  roomStore.groupMessages[groupId].push(tempMsg)
  await nextTick()
  scrollToBottom()

  try {
    const res = await groupChatApi.sendGroupMessage(groupId, {
      content: '[表情包]',
      contentType: 'image',
      messageId: messageId,
      extraData: {
        type: 'emoji_pack',
        packItemId: packItem.id,
        imageUrl: packItem.imageUrl,
        description: packItem.description || '表情包'
      }
    })
    if (res.code !== 0) throw new Error(res.msg)
    const msgs = roomStore.groupMessages[groupId]
    const idx = msgs.findIndex(m => m.messageId === messageId)
    if (idx !== -1) msgs[idx].status = 'sent'
  } catch (e) {
    const msgs = roomStore.groupMessages[groupId]
    const idx = msgs.findIndex(m => m.messageId === messageId)
    if (idx !== -1) msgs[idx].status = 'failed'
    ElMessage.error('表情包发送失败')
  }
}

// ==================== File Upload ====================
const triggerImageUpload = () => { imageInput.value?.click() }
const triggerFileUpload = () => { fileInput.value?.click() }

const handleImageUpload = async (event) => {
  const files = Array.from(event.target.files)
  for (const file of files) await uploadSingleFile(file, 'image')
  event.target.value = ''
}

const handleFileUpload = async (event) => {
  const files = Array.from(event.target.files)
  for (const file of files) {
    const type = file.type?.startsWith('video/') ? 'video' : 'file'
    await uploadSingleFile(file, type)
  }
  event.target.value = ''
}

const uploadSingleFile = async (file, fileType) => {
  if (!currentGroup.value || !currentUserId.value) return

  const uploadId = 'upload_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)
  const groupId = currentGroup.value.groupId
  const messageId = currentUserId.value + '_' + groupId + '_' + Date.now()

  const controller = new AbortController()
  uploadingFiles.value.push({
    id: uploadId,
    name: file.name,
    size: file.size,
    progress: 0,
    status: 'uploading',
    error: null,
    fileObj: file,
    controller
  })

  try {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('groupId', groupId)
    formData.append('chatType', 'group')
    formData.append('messageId', messageId)

    const config = {
      signal: controller.signal,
      onUploadProgress: (progressEvent) => {
        if (progressEvent.total) {
          const pct = Math.round((progressEvent.loaded * 100) / progressEvent.total)
          const item = uploadingFiles.value.find(f => f.id === uploadId)
          if (item) item.progress = pct
        }
      }
    }

    const res = await worldChatApi.uploadGroupChatFile(formData, config)

    if (res.code === 0 && res.data) {
      const fileData = res.data

      // Optimistically add temp message
      const tempContentType = fileData.fileType ||
        (file.type?.startsWith('image/') ? 'image' :
          file.type?.startsWith('video/') ? 'video' : 'file')
      const tempMsg = {
        messageId: messageId, groupId: groupId,
        senderId: currentUserId.value,
        senderName: userInfoStore.currentUser?.username || '我',
        senderAvatar: currentUserAvatar.value,
        content: fileData.fileUrl || fileData.filePath,
        contentType: tempContentType,
        status: 'sending',
        createTime: new Date().toISOString(),
        fileUrl: fileData.fileUrl || fileData.filePath,
        fileName: fileData.fileName || file.name,
        fileSize: fileData.fileSize || formatFileSize(file.size)
      }
      roomStore.groupMessages[groupId] = roomStore.groupMessages[groupId] || []
      roomStore.groupMessages[groupId].push(tempMsg)
      await nextTick()
      scrollToBottom()

      // Send group message with file info in extraData
      const contentType = fileData.fileType ||
        (file.type?.startsWith('image/') ? 'image' :
          file.type?.startsWith('video/') ? 'video' : 'file')

      const msgData = {
        content: contentType === 'image' ? '[图片]' : contentType === 'video' ? '[视频]' : '[文件] ' + (fileData.fileName || file.name),
        contentType: contentType,
        messageId: messageId,
        extraData: {
          type: 'file',
          fileId: fileData.id,
          fileName: fileData.fileName || file.name,
          fileUrl: fileData.fileUrl || fileData.filePath,
          fileType: fileData.fileType || file.type,
          fileSize: fileData.fileSize || file.size,
          thumbnailUrl: fileData.fileUrl || fileData.filePath
        }
      }

      if (quotedMessage.value) {
        msgData.extraData.quotedMessage = {
          messageId: quotedMessage.value.messageId,
          senderId: quotedMessage.value.senderId,
          senderName: quotedMessage.value.senderName,
          content: quotedMessage.value.content,
          contentType: quotedMessage.value.contentType,
          createTime: quotedMessage.value.createTime
        }
      }

      const sendRes = await groupChatApi.sendGroupMessage(groupId, msgData)
      if (sendRes.code === 0) {
        const msgs = roomStore.groupMessages[groupId]
        const mi = msgs.findIndex(m => m.messageId === messageId)
        if (mi !== -1) msgs[mi].status = 'sent'
      } else {
        const msgs = roomStore.groupMessages[groupId]
        const mi = msgs.findIndex(m => m.messageId === messageId)
        if (mi !== -1) msgs[mi].status = 'failed'
      }
      quotedMessage.value = null

      const item = uploadingFiles.value.find(f => f.id === uploadId)
      if (item) { item.progress = 100; item.status = 'success' }
      setTimeout(() => { uploadingFiles.value = uploadingFiles.value.filter(f => f.id !== uploadId) }, 2000)
    } else {
      throw new Error(res.msg || '上传失败')
    }
  } catch (e) {
    const item = uploadingFiles.value.find(f => f.id === uploadId)
    if (item) { item.status = 'error'; item.error = e.message || '上传失败' }
  }
}

const retryUpload = (file) => {
  if (file.controller) { file.controller.abort(); file.controller = null }
  ElMessage.warning('正在尝试重新上传...')
  uploadingFiles.value = uploadingFiles.value.filter(f => f.id !== file.id)
  const fo = file.fileObj || new File([], file.name, { type: file.type || 'image/png' })
  if (fo.size > 0) uploadSingleFile(fo, file.type?.startsWith('image/') ? 'image' : 'file')
}

const cancelUpload = (file) => {
  if (file.controller) file.controller.abort()
  uploadingFiles.value = uploadingFiles.value.filter(f => f.id !== file.id)
}

// ==================== Message Actions ====================
const onMsgContextMenu = (msg, event) => {
  msgMenuTarget.value = msg
  msgMenuStyle.value = { left: event.clientX + 'px', top: event.clientY + 'px' }
  msgMenuVisible.value = true
  clearTimeout(msgMenuTimer)
  msgMenuTimer = setTimeout(() => { msgMenuVisible.value = false }, 4000)
}

const copyMsgContent = () => {
  if (msgMenuTarget.value?.content) navigator.clipboard?.writeText(msgMenuTarget.value.content)
  msgMenuVisible.value = false
}

const quoteMsg = () => {
  quotedMessage.value = { ...msgMenuTarget.value }
  msgMenuVisible.value = false
  msgInput.value?.focus()
}

const cancelQuote = () => { quotedMessage.value = null }

const withdrawMsg = async () => {
  if (!msgMenuTarget.value || !currentGroup.value) return
  const targetId = msgMenuTarget.value.messageId
  const groupId = currentGroup.value.groupId
  try {
    await groupChatApi.withdrawGroupMessage(groupId, { messageId: targetId })
    // Update local state immediately
    const msgs = roomStore.groupMessages[groupId]
    if (msgs) {
      const idx = msgs.findIndex(m => m.messageId === targetId)
      if (idx !== -1) {
        msgs[idx] = { ...msgs[idx], status: 'withdrawn', content: '【消息已撤回】' }
      }
    }
    ElMessage.success('消息已撤回')
  } catch (e) {
    ElMessage.error('撤回失败，请重试')
  }
  msgMenuVisible.value = false
}

const downloadFile = (msg) => {
  const url = msg.fileUrl || msg.content
  if (!url) { ElMessage.error('文件链接无效'); return }
  const link = document.createElement('a')
  link.href = url
  link.download = msg.fileName || 'download'
  link.style.display = 'none'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

const previewImage = (msg) => {
  previewImageUrl.value = msg.fileUrl || msg.content
}

// ==================== History & Scroll ====================
const loadMoreHistory = async () => {
  if (loadingMore.value || !hasMore.value || !currentGroup.value) return
  loadingMore.value = true
  const container = messagesContainer.value
  const oldHeight = container?.scrollHeight || 0
  const firstId = currentMessages.value[0]?.messageId
  const more = await roomStore.fetchGroupHistory(currentGroup.value.groupId, firstId)
  hasMore.value = more
  await nextTick()
  if (container) container.scrollTop = container.scrollHeight - oldHeight
  loadingMore.value = false
}

const handleScroll = () => {
  const el = messagesContainer.value
  if (!el) return
  const atBottom = Math.abs(el.scrollHeight - el.scrollTop - el.clientHeight) < 50
  isAtBottom.value = atBottom
  if (atBottom) {
    unreadBelow.value = 0
    const cg = currentGroup.value
    if (cg && currentMessages.value.length > 0) {
      const lastMsg = currentMessages.value[currentMessages.value.length - 1]
      roomStore.markGroupAsRead(cg.groupId, lastMsg?.messageId)
    }
  }
  if (el.scrollTop < 50 && hasMore.value && !loadingMore.value) {
    loadMoreHistory()
  }
}

const scrollToBottom = () => {
  scrollAnchor.value?.scrollIntoView({ behavior: 'smooth' })
  isAtBottom.value = true
  unreadBelow.value = 0
}

// ==================== Member Actions ====================
const onViewProfile = (member) => {
  if (!member?.userId) return
  selectedUserId.value = member.userId
  showUserDetailModal.value = true
}

const onMuteOrUnmuteMember = async (member) => {
  if (!currentGroup.value || !member) return
  try {
    if (member.isMuted) {
      await roomStore.unmuteMember(currentGroup.value.groupId, member.userId)
      ElMessage.success('已解除禁言')
    } else {
      muteTarget.value = member
      showMuteInput.value = true
    }
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

const onKickMember = async (member) => {
  if (!currentGroup.value) return
  try {
    await ElMessageBox.confirm(
      `确定要将 ${member.nicknameInGroup || member.username} 移出群聊吗？`,
      '移出群聊',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await roomStore.kickMember(currentGroup.value.groupId, member.userId)
    ElMessage.success('已移出群聊')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败')
  }
}

const onChangeMemberRole = async (member) => {
  if (!currentGroup.value || !member) return
  const newRole = member.role === 'admin' ? 'member' : 'admin'
  const actionText = newRole === 'admin' ? '设为管理员' : '取消管理员'
  try {
    await ElMessageBox.confirm(
      `确定要${actionText} ${member.nicknameInGroup || member.username} 吗？`,
      actionText,
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await roomStore.changeMemberRole(currentGroup.value.groupId, member.userId, newRole)
    ElMessage.success(`${actionText}成功`)
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败')
  }
}

const onTransferOwner = async (member) => {
  if (!currentGroup.value || !member) return
  try {
    await ElMessageBox.confirm(
      `确定要将群主身份转让给 ${member.nicknameInGroup || member.username} 吗？转让后你将失去群主权限。`,
      '转让群主',
      { confirmButtonText: '确定转让', cancelButtonText: '取消', type: 'warning' }
    )
    await roomStore.transferOwnership(currentGroup.value.groupId, member.userId)
    ElMessage.success('群主身份已转让')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败')
  }
}

const onDemoteSelf = async () => {
  if (!currentGroup.value) return
  try {
    await ElMessageBox.confirm(
      '确定要将自己降级为普通成员吗？',
      '降级为普通成员',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await roomStore.demoteSelf(currentGroup.value.groupId)
    ElMessage.success('已将你降级为普通成员')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败')
  }
}

const confirmMute = async () => {
  if (!currentGroup.value || !muteTarget.value) return
  await roomStore.muteMember(currentGroup.value.groupId, muteTarget.value.userId, muteDuration.value)
  showMuteInput.value = false
}

// ==================== Dialogs ====================
const onDissolved = () => { showSettings.value = false }
const onGroupCreated = () => { showCreateDialog.value = false }

// Watchers
watch(() => currentMessages.value.length, (newLen, oldLen) => {
  if (newLen > oldLen && !isAtBottom.value) {
    unreadBelow.value += newLen - oldLen
  }
})

onMounted(async () => {
  scrollToBottom()
  await roomStore.fetchMyGroups()
  document.addEventListener('click', () => { msgMenuVisible.value = false })
})
</script>

<style scoped>
.group-chat {
  display: flex;
  height: 100%;
  overflow: hidden;
}

.group-chat.has-bg {
  background-size: cover;
  background-position: center;
}

/* Left Sidebar */
.group-sidebar {
  width: 260px;
  border-right: 1px solid #ffcce0;
  display: flex;
  flex-direction: column;
  background: #fff5f9;
  flex-shrink: 0;
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border-bottom: 1px solid #ffcce0;
}

.sidebar-header h3 {
  margin: 0;
  color: #f1177d;
  font-size: 15px;
}

.create-btn {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: none;
  background: linear-gradient(135deg, #fa94c3, #50b9b4);
  color: white;
  font-size: 18px;
  cursor: pointer;
  line-height: 1;
}

.group-search {
  padding: 8px;
}

.group-search input {
  width: 100%;
  padding: 6px 10px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 13px;
  box-sizing: border-box;
}

.group-list {
  flex: 1;
  overflow-y: auto;
}

.group-item {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  cursor: pointer;
  transition: background 0.15s;
}

.group-item:hover {
  background: rgba(250, 148, 195, 0.1);
}

.group-item.active {
  background: rgba(80, 185, 180, 0.15);
}

.group-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  margin-right: 10px;
  flex-shrink: 0;
}

.group-info {
  flex: 1;
  min-width: 0;
}

.group-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.group-last-msg {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.group-meta {
  text-align: right;
  flex-shrink: 0;
}

.group-time {
  font-size: 11px;
  color: #bbb;
}

.unread-badge {
  display: inline-block;
  background: #ff4757;
  color: white;
  font-size: 10px;
  min-width: 16px;
  height: 16px;
  border-radius: 8px;
  text-align: center;
  line-height: 16px;
  padding: 0 4px;
  margin-top: 4px;
}

.empty-groups {
  text-align: center;
  color: #999;
  padding: 30px;
  font-size: 13px;
}

/* Chat Main */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  position: relative;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  border-bottom: 1px solid #ffcce0;
  background: #fff;
  flex-shrink: 0;
}

.chat-header-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
}

.header-name {
  font-size: 15px;
  font-weight: 600;
  color: #333;
}

.header-count { font-size: 12px; color: #999; }
.header-actions-row { display: flex; align-items: center; gap: 8px; }
.join-btn { padding: 5px 12px; background: linear-gradient(135deg, #50b9b4, #4caf50); color: white; border: none; border-radius: 14px; cursor: pointer; font-size: 12px; }
.not-member-banner { text-align: center; padding: 8px; background: #fff3cd; color: #856404; font-size: 13px; }
.join-tag { font-size: 9px; padding: 1px 5px; border-radius: 6px; margin-left: 4px; vertical-align: middle; }
.tag-public { background: #e8f5e9; color: #4caf50; }
.tag-approval { background: #fff3e0; color: #ff9800; }
.tag-invite_only { background: #fce4ec; color: #e91e63; }
.join-group-info { text-align: center; margin-bottom: 12px; }
.join-avatar { width: 60px; height: 60px; border-radius: 50%; margin-bottom: 6px; }
.join-name { font-size: 16px; font-weight: 600; color: #333; }
.join-meta { font-size: 12px; color: #999; margin-top: 2px; }

.settings-btn { background: none; border: none; font-size: 18px; cursor: pointer; color: #999; }

/* Messages */
.messages-area {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;
}

.load-more,
.no-more {
  text-align: center;
  padding: 8px;
}

.load-more-btn {
  padding: 4px 12px;
  border: 1px solid #ff69b4;
  color: #ff69b4;
  background: none;
  border-radius: 12px;
  font-size: 12px;
  cursor: pointer;
}

.loading-text {
  font-size: 12px;
  color: #999;
}

.no-more {
  font-size: 11px;
  color: #ccc;
}

.date-divider {
  text-align: center;
  margin: 16px 0;
  position: relative;
}

.date-divider::before {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  top: 50%;
  height: 1px;
  background: #ffecef;
}

.date-divider span {
  background: #fff9fa;
  padding: 0 12px;
  font-size: 12px;
  color: #999;
  position: relative;
  z-index: 1;
}

.system-event {
  text-align: center;
  margin: 10px 0;
}

.system-event span {
  background: #f0f0f0;
  padding: 4px 12px;
  border-radius: 10px;
  font-size: 12px;
  color: #888;
}

.message-row {
  display: flex;
  margin-bottom: 14px;
  gap: 8px;
  align-items: flex-start;
}

.message-row.my-msg {
  flex-direction: row-reverse;
}

.msg-avatar {
  flex-shrink: 0;
  align-items: flex-start;
}

.msg-avatar img {
  width: 36px;
  height: 36px;
  border-radius: 50%;
}

.my-avatar {
  flex-shrink: 0;

}

.my-avatar img {
  width: 36px;
  height: 36px;
  border-radius: 50%;
}


.msg-body {
  max-width: 65%;
}

.my-msg .msg-body {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.msg-sender-name {
  font-size: 12px;
  color: #ff69b4;
  margin-bottom: 2px;
  padding: 0 4px;
}

.msg-bubble {
  margin-right: 40px;
  margin-top: 20px;
  padding: 8px 24px;
  border-radius: 16px;
  position: relative;
}

.message-row:not(.my-msg) .msg-bubble {
  background: #b9dbc0;
  border-top-left-radius: 4px;
}

.my-msg .msg-bubble {
  background: linear-gradient(135deg, #fa94c3, #50b9b4);
  color: white;
  border-bottom-right-radius: 4px;
}

.msg-content {
  font-size: 14px;
  line-height: 1.4;
  white-space: pre-wrap;
  word-break: break-word;
}

.msg-meta {
  display: flex;
  gap: 6px;
  margin-top: 4px;
  font-size: 11px;
  opacity: 0.7;
}

.msg-time {
  white-space: nowrap;
  color: rgb(233, 80, 164);
}

.status-icon {
  font-size: 15px;
}

.status-icon.sending {
  color: #ccc;
}

.status-icon.sent {
  color: #029633;
}

.status-icon.failed {
  color: #fc2340;
}

.msg-withdrawn {
  color: rgba(200, 50, 50, 0.8);
  font-weight: bold;
  font-style: italic;
  font-size: 13px;
}

/* Quoted message preview */
.quoted-msg-preview {
  padding: 6px 10px;
  margin-bottom: 8px;
  background: rgba(255, 255, 255, 0.3);
  border-left: 3px solid #ff69b4;
  border-radius: 4px;
  font-size: 12px;
}

.my-msg .quoted-msg-preview {
  background: rgba(255, 255, 255, 0.2);
  border-left-color: #fff;
}

.quoted-sender {
  font-weight: 500;
  color: #333;
}

.my-msg .quoted-sender {
  color: rgba(75, 142, 243, 0.8);
}

.quoted-content {
  color: #666;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.my-msg .quoted-content {
  font-weight: bold;
  color: rgba(243, 93, 93, 0.9);
}

/* Image & Video messages */
.msg-image img,
.msg-image-preview {
  max-width: 200px;
  max-height: 200px;
  border-radius: 8px;
  cursor: pointer;
}

.msg-video-player {
  max-width: 220px;
  max-height: 180px;
  border-radius: 8px;
  cursor: pointer;
}

.image-name,
.video-name {
  font-size: 10px;
  opacity: 0.7;
  margin-top: 2px;
}

/* File messages */
.msg-file {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 8px;
  min-width: 160px;
}

.my-msg .msg-file {
  background: rgba(255, 255, 255, 0.2);
}

.file-icon {
  font-size: 24px;
}

.file-info {
  flex: 1;
  min-width: 0;
}

.file-name {
  font-size: 13px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-size {
  font-size: 11px;
  color: #999;
}

.download-btn {
  padding: 4px 10px;
  background: #ff69b4;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 11px;
  white-space: nowrap;
}

.empty-chat {
  text-align: center;
  color: #999;
  padding: 40px;
}

.scroll-bottom-btn {
  position: absolute;
  bottom: 100px;
  left: 50%;
  transform: translateX(-50%);
  padding: 6px 16px;
  background: #fff;
  border: 1px solid #ff69b4;
  border-radius: 16px;
  color: #ff69b4;
  font-size: 12px;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  z-index: 10;
}

.unread-count-badge {
  background: #ff4757;
  color: white;
  border-radius: 10px;
  padding: 0 6px;
  font-size: 10px;
  margin-left: 4px;
}

/* Input Area */
.input-area {
  padding: 10px 16px;
  border-top: 1px solid #ffcce0;
  background: #fff;
  flex-shrink: 0;
}

.input-tools {
  display: flex;
  gap: 8px;
  margin-bottom: 6px;
}

.tool-btn {
  background: none;
  border: none;
  font-size: 18px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: background 0.2s;
  color: #ff69b4;
}

.tool-btn:hover {
  background: #fff0f5;
}

.emoji-picker-wrapper {
  position: relative;
  bottom: 400px;
  z-index: 1002;
}

.input-box {
  position: relative;
}

.input-box textarea {
  width: 100%;
  padding: 8px 80px 8px 12px;
  border: 1px solid #ffd6e7;
  border-radius: 12px;
  font-size: 13px;
  resize: none;
  outline: none;
  font-family: inherit;
  box-sizing: border-box;
}

.input-box textarea:focus {
  border-color: #ff69b4;
}

.input-actions {
  position: absolute;
  right: 8px;
  bottom: 6px;
}

.send-btn {
  padding: 6px 18px;
  background: linear-gradient(135deg, #fa94c3, #50b9b4);
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 13px;
  cursor: pointer;
  white-space: nowrap;
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Quote preview in input */
.quote-preview {
  padding: 6px 10px;
  margin-bottom: 6px;
  background: #f0f0f0;
  border-left: 3px solid #ff69b4;
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
  gap: 6px;
  flex-wrap: wrap;
}

.quote-label {
  color: #ff69b4;
  font-weight: bold;
  font-size: 11px;
  background: #fff0f5;
  padding: 1px 6px;
  border-radius: 8px;
}

.quote-sender {
  color: #333;
  font-weight: 500;
  font-size: 11px;
}

.quote-text {
  color: #999;
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 11px;
}

.cancel-quote {
  background: none;
  border: none;
  font-size: 16px;
  color: #999;
  cursor: pointer;
  position: absolute;
  right: 6px;
  top: 50%;
  transform: translateY(-50%);
}

.cancel-quote:hover {
  color: #ff4757;
}

/* Upload progress */
.upload-progress {
  background: #fff9fa;
  border-radius: 8px;
  padding: 8px;
  margin-bottom: 8px;
  max-height: 120px;
  overflow-y: auto;
  border: 1px dashed #ffd6e7;
}

.upload-item {
  padding: 6px;
  background: white;
  border-radius: 6px;
  margin-bottom: 6px;
  border: 1px solid #ffecef;
}

.warning-text {
  font-size: 11px;
  color: rgba(255, 77, 79, 0.8);
  font-weight: 500;
}

.file-info-row {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #333;
  margin: 4px 0;
}

.progress-bar {
  position: relative;
  height: 16px;
  background: #ffecef;
  border-radius: 8px;
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
  font-size: 10px;
  color: #333;
  z-index: 1;
  font-weight: 500;
}

.retry-btn,
.cancel-btn {
  padding: 2px 8px;
  font-size: 10px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  margin-left: 4px;
}

.retry-btn {
  background: #ff4757;
  color: white;
}

.cancel-btn {
  background: #999;
  color: white;
}

/* No group placeholder */
.no-group {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  font-size: 14px;
}

/* Right Sidebar */
.member-sidebar {
  width: 220px;
  border-left: 1px solid #ffcce0;
  background: #fff5f9;
  flex-shrink: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* Context Menu */
.context-menu {
  position: fixed;
  margin-left:-150px;
  margin-top:-70px;
  background: white;
  border: 1px solid #ddd;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(230, 146, 146, 0.1);
  z-index: 3000;
  min-width: 100px;
  padding: 4px 0;
}

.menu-item {
  padding: 8px 16px;
  cursor: pointer;
  font-size: 13px;
}

.menu-item:hover {
  background: #f5f5f5;
}

.menu-item.danger {
  color: #ff4757;
}

/* Dialog */
.dialog-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2500;
}

.dialog-box {
  background: white;
  border-radius: 12px;
  padding: 20px;
  width: 360px;
}

.dialog-box h4 {
  margin: 0 0 14px;
  color: #f1177d;
}

.form-group {
  margin-bottom: 12px;
}

.form-group label {
  display: block;
  font-size: 13px;
  margin-bottom: 4px;
  color: #555;
}

.form-group input {
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  box-sizing: border-box;
}

.dialog-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  margin-top: 16px;
}

.btn-cancel {
  padding: 6px 14px;
  border: 1px solid #ddd;
  border-radius: 6px;
  background: #f5f5f5;
  cursor: pointer;
  font-size: 13px;
}

.btn-confirm {
  padding: 6px 14px;
  border: none;
  border-radius: 6px;
  background: linear-gradient(135deg, #fa94c3, #50b9b4);
  color: white;
  cursor: pointer;
  font-size: 13px;
}

/* Image Preview Modal */
.image-preview-modal {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 3500;
  cursor: pointer;
}

.image-preview-modal img {
  max-width: 90%;
  max-height: 90%;
  object-fit: contain;
  cursor: default;
}
</style>
