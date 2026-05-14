<template>
  <div class="world-chat-wrapper" :style="bgStyle">
    <div class="world-messages" ref="messagesContainer" @scroll="handleScroll">
      <template v-for="(msg, idx) in displayMessages" :key="msg.messageId || idx">
        <div class="message" :class="{ 'my-msg': msg.senderId === currentUserId }"
          @contextmenu.prevent="onMsgContextMenu(msg, $event)">
          <div class="message-avatar" @click.stop="msg.senderId !== currentUserId && showUserDetail(msg.senderId)">
            <img :src="msg.senderAvatar || '/default-avatar.png'" :alt="msg.senderName" style="cursor: pointer">
          </div>
          <div class="message-content">
            <div class="message-user">{{ msg.senderName || '未知用户' }}</div>

            <!-- Quoted message preview -->
            <div v-if="msg.quotedMessage" class="quoted-msg-preview">
              <span class="quoted-sender">引用消息：{{ msg.quotedMessage.senderName || '用户' }}:</span>
              <span class="quoted-text">{{ getQuoteContent(msg.quotedMessage) }}</span>
            </div>

            <!-- Text message -->
            <div v-if="msg.contentType === 'text' && msg.status !== 'withdrawn'" class="message-text">{{ msg.content }}</div>

            <!-- Image message -->
            <div v-if="msg.contentType === 'image' && msg.status !== 'withdrawn'" class="msg-image">
              <img :src="msg.fileUrl" @click="previewImage(msg)" class="msg-image-preview" alt="图片" loading="lazy">
            </div>

            <!-- Video message -->
            <div v-if="msg.contentType === 'video' && msg.status !== 'withdrawn'" class="msg-video">
              <video :src="msg.fileUrl" controls preload="metadata" class="msg-video-player"></video>
            </div>

            <!-- File message -->
            <div v-if="msg.contentType === 'file' && msg.status !== 'withdrawn'" class="msg-file">
              <span class="file-icon">&#x1F4C4;</span>
              <div class="file-info">
                <div class="file-name">{{ msg.fileName || '文件' }}</div>
                <div class="file-size">{{ msg.fileSize || '' }}</div>
              </div>
              <button @click="downloadFile(msg)" class="download-btn">下载</button>
            </div>

            <!-- Withdrawn -->
            <div v-if="msg.status === 'withdrawn'" class="msg-withdrawn">[{{ msg.senderName }}] 已撤回消息</div>

            <!-- Message meta -->
            <div class="msg-meta">
              <span class="msg-time">{{ formatTime(msg.createTime) }}</span>
              <span v-if="msg.senderId === currentUserId" class="msg-status">
                <span v-if="msg.status === 'sending'" title="发送中..." class="sending-icon">&#1x23F3;</span>
                <span v-else-if="msg.status === 'sent'" title="已发送" class="sent-icon">&#x2713;</span>
                <span v-else-if="msg.status === 'failed'" title="发送失败" class="failed-icon">&#x26A0;</span>
              </span>
            </div>
          </div>
        </div>
      </template>
      <div v-if="displayMessages.length === 0" class="empty-world">欢迎来到世界聊天，发送第一条消息吧~</div>
      <div ref="scrollAnchor"></div>
    </div>

    <!-- Scroll to bottom -->
    <button v-if="!isAtBottom && displayMessages.length > 0" class="scroll-bottom-btn" @click="scrollToBottom">
      &#x2193; 回到最新消息
      <span v-if="unreadBelow > 0" class="unread-count-badge">{{ unreadBelow }}</span>
    </button>

    <!-- Right-click context menu -->
    <div v-if="ctxMenuVisible" class="context-menu" :style="ctxMenuStyle" @click.stop>
      <div v-if="ctxMenuTarget?.contentType === 'text' && ctxMenuTarget?.status !== 'withdrawn'" class="menu-item" @click="copyMsg">复制</div>
      <div v-if="ctxMenuTarget?.status !== 'withdrawn'" class="menu-item" @click="quoteMsg">引用</div>
      <div v-if="ctxMenuTarget?.senderId === currentUserId && canWithdraw(ctxMenuTarget) && ctxMenuTarget?.status !== 'withdrawn'" class="menu-item danger" @click="withdrawMsg">撤回</div>
      <div v-if="ctxMenuTarget?.senderId === currentUserId" class="menu-item danger" @click="deleteMsg">删除</div>
      <div v-if="ctxMenuTarget?.contentType !== 'text' && ctxMenuTarget?.status !== 'withdrawn'" class="menu-item" @click="downloadFile(ctxMenuTarget)">下载</div>
    </div>

    <!-- Emoji picker -->
    <div class="emoji-picker-wrapper" v-if="showEmoji">
      <EmojiPicker :visible="showEmoji" :targetElement="emojiRef"
        @select="insertEmoji" @select-pack="sendEmojiPack" @close="showEmoji = false"
        @update:visible="showEmoji = $event" />
    </div>

    <!-- Upload progress -->
    <div v-if="uploadingFiles.length > 0" class="upload-progress">
      <div v-for="f in uploadingFiles" :key="f.id" class="upload-item">
        <span class="warning-text">图片和视频(最大10MB)上传速度较慢，请耐心等待...</span>
        <div class="file-info-row">
          <span class="uf-name">{{ f.name }}</span>
          <span class="uf-size">{{ formatFileSize(f.size) }}</span>
        </div>
        <div class="progress-bar">
          <div class="progress" :style="{ width: f.progress + '%' }"></div>
          <span class="progress-text">{{ f.progress }}%</span>
        </div>
        <button v-if="f.error" @click="retryUpload(f)" class="retry-btn">重试</button>
        <button v-if="f.error" @click="cancelUpload(f)" class="cancel-btn">取消</button>
      </div>
    </div>

    <!-- Input area -->
    <div class="world-input">
      <button @click="showEmoji = !showEmoji" ref="emojiRef" class="tool-btn" title="表情">&#x1F60A;</button>
      <button @click="triggerImage" class="tool-btn" title="图片">🖼️</button>
      <button @click="triggerFile" class="tool-btn" title="文件">&#x1F4C4;</button>
      <input type="file" ref="imageInput" accept="image/*" style="display:none" @change="onImageChange" multiple>
      <input type="file" ref="fileInput" style="display:none" @change="onFileChange" multiple>

      <div class="input-box">
        <div v-if="quotedMessage" class="quote-preview">
          <span class="quote-label">引用消息：{{ quotedMessage.senderName || '用户' }}:</span>
          <span class="quote-text">{{ getQuoteContent(quotedMessage) }}</span>
          <button @click="quotedMessage = null" class="cancel-quote">&times;</button>
        </div>
        <input class="input-field" v-model="inputText" @keyup.enter="send" placeholder="说点什么..."
          :disabled="sending">
      </div>
      <button class="send-btn" @click="send" :disabled="sending">发送</button>
    </div>

    <!-- Image preview modal -->
    <div v-if="previewUrl" class="image-preview-modal" @click="previewUrl = null">
      <img :src="previewUrl" @click.stop>
    </div>

    <!-- User Detail Modal -->
    <UserDetail v-model:visible="showUserDetailModal" :friendId="selectedUserId" @close="selectedUserId = null" />
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserInfoStore } from '@/stores/userInfo'
import websocketService from '@/utils/websocket'
import * as worldChatApi from '@/api/worldChatApi'
import EmojiPicker from '@/views/EmojiPicker.vue'
import UserDetail from '@/views/UserDetail.vue'

const userInfoStore = useUserInfoStore()
const currentUser = computed(() => userInfoStore.currentUser)
const currentUserId = computed(() => userInfoStore.currentUser?.id)

const inputText = ref('')
const sending = ref(false)
const messagesContainer = ref(null)
const scrollAnchor = ref(null)
const isAtBottom = ref(true)
const unreadBelow = ref(0)

// Emoji
const showEmoji = ref(false)
const emojiRef = ref(null)

// File upload
const imageInput = ref(null)
const fileInput = ref(null)
const uploadingFiles = ref([])
const previewUrl = ref(null)

// Quoting
const quotedMessage = ref(null)

// Context menu
const ctxMenuVisible = ref(false)
const ctxMenuTarget = ref(null)
const ctxMenuStyle = ref({})
let ctxMenuTimer = null

// User detail
const showUserDetailModal = ref(false)
const selectedUserId = ref(null)

const showUserDetail = (userId) => {
  if (!userId) return
  selectedUserId.value = userId
  showUserDetailModal.value = true
}

// Background
const props = defineProps({
  backgroundImage: { type: String, default: '' },
  backgroundOpacity: { type: Number, default: 1 }
})
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

// Normalize incoming messages
const displayMessages = computed(() => {
  return userInfoStore.worldMessages.map(msg => {
    const m = { ...msg }
    if (m.extraData && typeof m.extraData === 'object') {
      Object.assign(m, {
        fileUrl: m.extraData.fileUrl || m.fileUrl,
        fileName: m.extraData.fileName || m.fileName,
        fileSize: m.extraData.fileSize || m.fileSize,
        quotedMessage: m.extraData.quotedMessage || m.quotedMessage
      })
    }
    return m
  })
})

// ============ Format ============
const formatTime = (t) => { if (!t) return ''; return new Date(t).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) }
const formatFileSize = (b) => { if (!b) return '0 B'; const k = 1024; const s = ['B','KB','MB','GB']; const i = Math.floor(Math.log(b) / Math.log(k)); return parseFloat((b / Math.pow(k, i)).toFixed(2)) + ' ' + s[i] }
const getQuoteContent = (m) => { if (!m) return ''; return m.contentType === 'text' ? (m.content || '').substring(0, 30) : (m.contentType === 'image' ? '[图片]' : m.contentType === 'video' ? '[视频]' : '[文件]') }
const canWithdraw = (m) => { if (!m?.createTime) return false; return Date.now() - new Date(m.createTime).getTime() <= 2 * 60 * 1000 }

// ============ Send Text ============
const send = async () => {
  if (!inputText.value.trim() || sending.value) return
  const cid = currentUserId.value
  const msgId = cid + '_world_' + Date.now()

  const data = {
    content: inputText.value.trim(),
    contentType: 'text',
    messageId: msgId,
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

  // Optimistic add
  userInfoStore.worldMessages.push({
    messageId: msgId, senderId: cid, senderName: currentUser.value?.username,
    senderAvatar: currentUser.value?.avatar, content: data.content,
    contentType: 'text', status: 'sending', createTime: new Date().toISOString(),
    quotedMessage: quotedMessage.value ? { ...quotedMessage.value } : null
  })
  inputText.value = ''
  const quote = quotedMessage.value
  quotedMessage.value = null
  sending.value = true
  await nextTick(); scrollToBottom()

  try {
    const res = await worldChatApi.sendWorldMessage(data)
    if (res.code === 0) {
      const msgs = userInfoStore.worldMessages
      const idx = msgs.findIndex(m => m.messageId === msgId)
      if (idx !== -1) msgs[idx].status = 'sent'
      if (res.data?.messageId) {
        if (idx !== -1) msgs[idx].messageId = res.data.messageId
      }
    } else {
      const idx = userInfoStore.worldMessages.findIndex(m => m.messageId === msgId)
      if (idx !== -1) userInfoStore.worldMessages[idx].status = 'failed'
    }
  } catch (e) {
    const idx = userInfoStore.worldMessages.findIndex(m => m.messageId === msgId)
    if (idx !== -1) userInfoStore.worldMessages[idx].status = 'failed'
  }
  sending.value = false
}

// ============ Emoji ============
const insertEmoji = (emoji) => { inputText.value += emoji }
const sendEmojiPack = async (packItem) => {
  const cid = currentUserId.value
  const msgId = cid + '_world_' + Date.now()
  userInfoStore.worldMessages.push({
    messageId: msgId, senderId: cid, senderName: currentUser.value?.username,
    senderAvatar: currentUser.value?.avatar, content: '[表情包]',
    contentType: 'image', status: 'sending', createTime: new Date().toISOString(),
    fileUrl: packItem.imageUrl
  })
  await nextTick(); scrollToBottom()
  try {
    const res = await worldChatApi.sendWorldMessage({
      content: '[表情包]', contentType: 'image', messageId: msgId,
      extraData: { type: 'emoji_pack', imageUrl: packItem.imageUrl, packItemId: packItem.id }
    })
    const idx = userInfoStore.worldMessages.findIndex(m => m.messageId === msgId)
    if (idx !== -1) userInfoStore.worldMessages[idx].status = res.code === 0 ? 'sent' : 'failed'
  } catch (e) {
    const idx = userInfoStore.worldMessages.findIndex(m => m.messageId === msgId)
    if (idx !== -1) userInfoStore.worldMessages[idx].status = 'failed'
  }
}

// ============ File Upload ============
const triggerImage = () => imageInput.value?.click()
const triggerFile = () => fileInput.value?.click()
const onImageChange = async (e) => { for (const f of Array.from(e.target.files)) await upload(f, 'image'); e.target.value = '' }
const onFileChange = async (e) => { for (const f of Array.from(e.target.files)) { const t = f.type?.startsWith('video/') ? 'video' : 'file'; await upload(f, t) } e.target.value = '' }

const upload = async (file, fileType) => {
  const cid = currentUserId.value
  const msgId = cid + '_world_' + Date.now()
  const upId = 'wu_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)
  const controller = new AbortController()

  uploadingFiles.value.push({ id: upId, name: file.name, size: file.size, progress: 0, fileObj: file, controller })

  const tempMsg = {
    messageId: msgId, senderId: cid, senderName: currentUser.value?.username,
    senderAvatar: currentUser.value?.avatar, contentType: fileType, content: fileType === 'image' ? '[图片]' : fileType === 'video' ? '[视频]' : '[文件]',
    status: 'sending', createTime: new Date().toISOString(),
    fileName: file.name, fileSize: formatFileSize(file.size)
  }
  userInfoStore.worldMessages.push(tempMsg)
  await nextTick(); scrollToBottom()

  try {
    const fd = new FormData()
    fd.append('file', file); fd.append('groupId', ''); fd.append('chatType', 'world'); fd.append('messageId', msgId)

    const res = await worldChatApi.uploadGroupChatFile(fd, {
      signal: controller.signal,
      onUploadProgress: (e) => {
        if (e.total) { const p = Math.round(e.loaded * 100 / e.total); const ui = uploadingFiles.value.find(f => f.id === upId); if (ui) ui.progress = p }
      }
    })

    if (res.code === 0 && res.data) {
      const fd2 = res.data
      const ct = fd2.fileType || fileType
      const msgs = userInfoStore.worldMessages; const mi = msgs.findIndex(m => m.messageId === msgId)
      if (mi !== -1) { msgs[mi].fileUrl = fd2.fileUrl; msgs[mi].contentType = ct; msgs[mi].status = 'sent'; msgs[mi].content = fd2.contentType === 'image' ? '[图片]' : fd2.contentType === 'video' ? '[视频]' : '[文件]'; }

      const ui = uploadingFiles.value.find(f => f.id === upId)
      if (ui) ui.progress = 100
      setTimeout(() => { uploadingFiles.value = uploadingFiles.value.filter(f => f.id !== upId) }, 2000)

      // Send WS broadcast for others
      websocketService.sendWorldMessage({
        content: fd2.contentType === 'image' ? '[图片]' : fd2.contentType === 'video' ? '[视频]' : '[文件]', contentType: ct, messageId: msgId,
        extraData: { type: 'file', fileUrl: fd2.fileUrl, fileName: fd2.fileName, fileSize: fd2.fileSize, fileType: ct }
      })
    } else {
      throw new Error(res.msg || '上传失败')
    }
  } catch (e) {
    const ui = uploadingFiles.value.find(f => f.id === upId)
    if (ui) ui.error = e.message || '上传失败'
    const mi = userInfoStore.worldMessages.findIndex(m => m.messageId === msgId)
    if (mi !== -1) userInfoStore.worldMessages[mi].status = 'failed'
  }
}

const retryUpload = (f) => {
  if (f.controller) { f.controller.abort(); f.controller = null }
  uploadingFiles.value = uploadingFiles.value.filter(x => x.id !== f.id)
  const fo = f.fileObj || new File([], f.name, { type: f.type || 'image/png' })
  const ft = f.type?.startsWith('image/') ? 'image' : f.type?.startsWith('video/') ? 'video' : 'file'
  if (fo.size > 0) upload(fo, ft)
}
const cancelUpload = (f) => {
  if (f.controller) f.controller.abort()
  uploadingFiles.value = uploadingFiles.value.filter(x => x.id !== f.id)
}

// ============ Context Menu ============
const onMsgContextMenu = (msg, e) => {
  ctxMenuTarget.value = msg
  ctxMenuStyle.value = { left: e.clientX + 'px', top: e.clientY + 'px' }
  ctxMenuVisible.value = true
  clearTimeout(ctxMenuTimer)
  ctxMenuTimer = setTimeout(() => { ctxMenuVisible.value = false }, 4000)
}
const copyMsg = () => { navigator.clipboard?.writeText(ctxMenuTarget.value?.content || ''); ctxMenuVisible.value = false; ElMessage.success('已复制') }
const quoteMsg = () => { quotedMessage.value = { ...ctxMenuTarget.value }; ctxMenuVisible.value = false }
const withdrawMsg = async () => {
  if (!ctxMenuTarget.value) return
  try {
    await worldChatApi.withdrawWorldMessage({ messageId: ctxMenuTarget.value.messageId })
    const idx = userInfoStore.worldMessages.findIndex(m => m.messageId === ctxMenuTarget.value.messageId)
    if (idx !== -1) { userInfoStore.worldMessages[idx].status = 'withdrawn'; userInfoStore.worldMessages[idx].content = '【消息已撤回】' }
    ElMessage.success('已撤回')
  } catch (e) { ElMessage.error('撤回失败') }
  ctxMenuVisible.value = false
}
const deleteMsg = async () => {
  if (!ctxMenuTarget.value) return
  try {
    await worldChatApi.deleteWorldMessage(ctxMenuTarget.value.messageId)
    userInfoStore.worldMessages = userInfoStore.worldMessages.filter(m => m.messageId !== ctxMenuTarget.value.messageId)
    ElMessage.success('已删除')
  } catch (e) { ElMessage.error('删除失败') }
  ctxMenuVisible.value = false
}

// ============ File actions ============
const downloadFile = (msg) => { const u = msg.fileUrl; if (!u) return; const a = document.createElement('a'); a.href = u; a.download = msg.fileName || 'download'; a.style.display = 'none'; document.body.appendChild(a); a.click(); document.body.removeChild(a) }
const previewImage = (msg) => { previewUrl.value = msg.fileUrl }

// ============ Scroll ============
const handleScroll = () => {
  const el = messagesContainer.value; if (!el) return
  const atBottom = Math.abs(el.scrollHeight - el.scrollTop - el.clientHeight) < 50
  isAtBottom.value = atBottom
  if (atBottom) unreadBelow.value = 0
}
const scrollToBottom = () => { scrollAnchor.value?.scrollIntoView({ behavior: 'smooth' }); isAtBottom.value = true; unreadBelow.value = 0 }

watch(() => displayMessages.value.length, (n, o) => { if (n > o && !isAtBottom.value) unreadBelow.value += n - o })

onMounted(() => {
  scrollToBottom()
  document.addEventListener('click', () => { ctxMenuVisible.value = false })
})
</script>

<style scoped>
.world-chat-wrapper { display: flex; flex-direction: column; height: 100%; position: relative; }
.world-messages { flex: 1; overflow-y: auto; padding: 10px; }
.scroll-bottom-btn { position: absolute; bottom: 80px; left: 50%; transform: translateX(-50%); padding: 6px 16px; background: #fff; border: 1px solid #ff69b4; border-radius: 16px; color: #ff69b4; font-size: 12px; cursor: pointer; box-shadow: 0 2px 8px rgba(0,0,0,0.1); z-index: 10; }
.unread-count-badge { background: #ff4757; color: white; border-radius: 10px; padding: 0 6px; font-size: 10px; margin-left: 4px; }

/* Messages */
.message { display: flex; margin-bottom: 15px; }
.message.my-msg { flex-direction: row-reverse; }
.message-avatar { margin: 0 10px; flex-shrink: 0; }
.message-avatar img { width: 40px; height: 40px; border-radius: 50%; }
.message-content { flex: 1; min-width: 0; }
.my-msg .message-content { text-align: right; }
.message-user { font-weight: bold; font-size: 14px; color: #ff69b4; }
.message-time { font-size: 12px; color: #999; }
.message-text {width: fit-content; max-width: 300px; background: #b9dbc0; font-size: 14px; color: #312c2e; line-height: 1.4; white-space: pre-wrap; word-break: break-word; padding: 6px 12px;  border-radius: 12px; margin-top: 4px; }
.my-msg .message-text {margin-left: auto; width: fit-content; max-width: 300px; background: linear-gradient(135deg, #fa94c3, #50b9b4); color: white; }
.msg-meta { display: flex; gap: 4px; font-size: 11px; opacity: 0.7; margin-top: 2px; }
.msg-status { cursor: pointer; }
.msg-status .sending-icon { font-size: 14px; color: #ff69b4;}
.msg-status .sent-icon { font-size: 14px; color: #03f710;}
.msg-status .failed-icon { font-size: 14px; color: #ff4757;}
.my-msg .msg-meta { justify-content: flex-end; }
.msg-withdrawn { color: rgba(200,50,50,0.8); font-style: italic; font-size: 13px; padding: 4px 0; }
.quoted-msg-preview {width: fit-content; max-width: 300px; padding: 4px 8px; background: rgba(255,255,255,0.5); border-left: 3px solid #ff69b4; border-radius: 4px; font-size: 12px; margin-bottom: 4px; }

/* Image/Video/File */
.msg-image img { max-width: 200px; max-height: 200px; border-radius: 8px; cursor: pointer; }
.msg-video-player { max-width: 220px; max-height: 180px; border-radius: 8px; }
.msg-file { width: fit-content; max-width: 300px; display: flex; align-items: center; gap: 8px; padding: 6px; background: rgba(255,255,255,0.6); border-radius: 6px; margin-top: 4px; }
.my-msg .msg-file {margin-left: auto; width: fit-content; max-width: 300px; background: rgba(255,255,255,0.3); }
.file-icon { font-size: 22px; }
.file-info { flex: 1; min-width: 0; }
.file-name { font-size: 13px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.file-size { font-size: 11px; color: #999; }
.download-btn { padding: 4px 8px; background: #ff69b4; color: white; border: none; border-radius: 4px; cursor: pointer; font-size: 11px; }

.empty-world { text-align: center; color: #e949b9; font-size: 16px; font-weight: bold; font-style: italic; padding: 60px 20px; }

/* Input area */
.world-input { display: flex; padding: 10px; border-top: 1px solid #ffcce0; align-items: center; gap: 6px; background: rgba(255,255,255,0.9); }
.tool-btn { background: none; border: none; font-size: 18px; cursor: pointer; padding: 4px 6px; border-radius: 6px; color: #ff69b4; }
.tool-btn:hover { background: #fff0f5; }
.input-box { flex: 1; min-width: 0; }
.input-field { width: 100%; padding: 8px; border: 1px solid #f82dee; border-radius: 10px; box-sizing: border-box; }
.input-field:focus { outline: none; border-color: #ff69b4; }
.send-btn { padding: 8px 14px; border: none; border-radius: 10px; background: rgb(141, 188, 233); color: #ff69b4; cursor: pointer; }
.send-btn:hover { background: #ff69b4; color: #fff; }
.send-btn:disabled { background: #ccc; cursor: not-allowed; }
.quote-preview {display: flex; align-items: center; gap: 4px; padding: 4px 8px; background: #f0f0f0; border-left: 3px solid #ff69b4; border-radius: 4px; margin-bottom: 4px; font-size: 12px; }
.quote-label { color: #ff69b4; font-weight: bold; font-size: 10px; }
.quote-text { color: #999; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.cancel-quote { background: none; border: none; font-size: 14px; color: #999; cursor: pointer; }

/* Upload progress */
.upload-progress { padding: 6px 10px; background: #fff9fa; border-top: 1px dashed #ffd6e7; font-size: 12px; max-height: 120px; overflow-y: auto; }
.upload-item { margin-bottom: 6px; padding: 6px; background: white; border-radius: 6px; border: 1px solid #ffecef; }
.warning-text { font-size: 11px; color: rgba(255,77,79,0.8); font-weight: 500; }
.file-info-row { display: flex; justify-content: space-between; font-size: 12px; color: #333; margin: 4px 0; }
.progress-bar { position: relative; height: 16px; background: #ffecef; border-radius: 8px; overflow: hidden; }
.progress { position: absolute; top: 0; left: 0; height: 100%; background: linear-gradient(90deg, #ff85a2, #ff69b4); transition: width 0.3s; }
.progress-text { position: absolute; top: 50%; left: 50%; transform: translate(-50%,-50%); font-size: 10px; color: #333; z-index: 1; font-weight: 500; }
.retry-btn, .cancel-btn { padding: 2px 8px; font-size: 10px; border: none; border-radius: 4px; cursor: pointer; margin-left: 4px; }
.retry-btn { background: #ff4757; color: white; }
.cancel-btn { background: #999; color: white; }

/* Context menu */
.context-menu { position: fixed; margin-left:-150px; margin-top:-70px; background: white; border: 1px solid #ddd; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); z-index: 3000; min-width: 100px; padding: 4px 0; }
.menu-item { padding: 8px 16px; cursor: pointer; font-size: 13px; }
.menu-item:hover { background: #f5f5f5; }
.menu-item.danger { color: #ff4757; }

/* Emoji picker */
.emoji-picker-wrapper { position: relative; bottom: 400px; z-index: 1002; }

/* Modal */
.image-preview-modal { position: fixed; inset: 0; background: rgba(0,0,0,0.9); display: flex; align-items: center; justify-content: center; z-index: 3500; cursor: pointer; }
.image-preview-modal img { max-width: 90%; max-height: 90%; object-fit: contain; cursor: default; }
</style>
