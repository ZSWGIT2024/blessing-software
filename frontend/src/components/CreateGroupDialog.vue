<template>
  <div v-if="visible" class="dialog-overlay" @click.self="$emit('close')">
    <div class="dialog-box">
      <h3>创建群聊</h3>
      <div class="form-group">
        <label>群名称 <span class="required">*</span></label>
        <div class="name-input-wrapper">
          <input
            v-model="form.name"
            @input="onGroupNameInput"
            placeholder="请输入群名称"
            maxlength="50"
            :class="{ 'input-valid': nameStatus === 'valid', 'input-invalid': nameStatus === 'invalid' }"
          />
          <span v-if="nameStatus === 'checking'" class="status-hint checking">⏳ 检查中...</span>
          <span v-if="nameStatus === 'valid'" class="status-hint valid">✅ {{ nameMessage }}</span>
          <span v-if="nameStatus === 'invalid'" class="status-hint invalid">❌ {{ nameMessage }}</span>
        </div>
      </div>
      <div class="form-group">
        <label>群头像</label>
        <div class="avatar-upload">
          <img v-if="form.avatar" :src="form.avatar" class="avatar-preview">
          <input type="file" ref="avatarInput" accept="image/*" style="display:none" @change="handleAvatarUpload">
          <button type="button" class="upload-btn" @click="$refs.avatarInput.click()" :disabled="avatarUploading">
            {{ avatarUploading ? '上传中...' : (form.avatar ? '更换头像' : '上传头像') }}
          </button>
        </div>
      </div>
      <div class="form-group">
        <label>群简介</label>
        <textarea v-model="form.description" placeholder="介绍一下这个群（可选）" maxlength="200" rows="3"></textarea>
      </div>
      <div class="form-group">
        <label>加入方式</label>
        <select v-model="form.joinPermission">
          <option value="anyone">任何人可加入</option>
          <option value="approval">需要审批</option>
          <option value="invite">仅邀请加入</option>
        </select>
      </div>
      <div class="form-group">
        <label>最大成员数</label>
        <input v-model.number="form.maxMembers" type="range" min="50" max="200" step="10">
        <span class="range-value">{{ form.maxMembers }}</span>
      </div>
      <div class="dialog-actions">
        <button @click="$emit('close')" class="btn-cancel">取消</button>
        <button @click="handleCreate" class="btn-create" :disabled="loading">
          {{ loading ? '创建中...' : '创建' }}
        </button>
      </div>
      <div v-if="error" class="error-msg">{{ error }}</div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRoomStore } from '@/stores/room'
import { useUserInfoStore } from '@/stores/userInfo'
import * as worldChatApi from '@/api/worldChatApi'
import { checkGroupName } from '@/api/groupChatApi'

defineProps({ visible: { type: Boolean, default: false } })
const emit = defineEmits(['close', 'created'])

const roomStore = useRoomStore()
const userInfoStore = useUserInfoStore()
const loading = ref(false)
const error = ref('')
const avatarUploading = ref(false)

// Group name real-time validation
const nameStatus = ref('') // '', 'checking', 'valid', 'invalid'
const nameMessage = ref('')
let nameCheckTimer = null

const onGroupNameInput = () => {
  const name = form.name.trim()
  if (!name) {
    nameStatus.value = ''
    nameMessage.value = ''
    if (nameCheckTimer) clearTimeout(nameCheckTimer)
    return
  }
  if (name.length < 2) {
    nameStatus.value = 'invalid'
    nameMessage.value = '群名称不能少于2个字符'
    return
  }

  if (nameCheckTimer) clearTimeout(nameCheckTimer)
  nameCheckTimer = setTimeout(async () => {
    nameStatus.value = 'checking'
    try {
      const res = await checkGroupName(name)
      if (res.code === 0 && res.data) {
        if (res.data.available) {
          nameStatus.value = 'valid'
          nameMessage.value = res.data.message || '群名称可用'
        } else {
          nameStatus.value = 'invalid'
          nameMessage.value = res.data.message || '群名称不可用'
        }
      }
    } catch {
      nameStatus.value = ''
    }
  }, 400)
}

const currentUserId = computed(() => userInfoStore.currentUser?.id)

const form = reactive({
  name: '',
  avatar: '',
  description: '',
  joinPermission: 'anyone',
  maxMembers: 200
})

const handleAvatarUpload = async (event) => {
  const file = event.target.files?.[0]
  if (!file) return
  if (file.size > 10 * 1024 * 1024) { error.value = '头像大小不能超过10MB'; return }
  avatarUploading.value = true; error.value = ''
  try {
    const fd = new FormData()
    fd.append('file', file)
    fd.append('groupId', '')
    fd.append('chatType', 'group')
    fd.append('messageId', 'avatar_' + currentUserId.value + '_' + Date.now())
    const res = await worldChatApi.uploadGroupChatFile(fd)
    if (res.code === 0 && res.data) {
      form.avatar = res.data.fileUrl
    } else {
      error.value = res.msg || '上传失败'
    }
  } catch (e) {
    error.value = '头像上传失败'
  } finally {
    avatarUploading.value = false
    event.target.value = ''
  }
}

const handleCreate = async () => {
  error.value = ''
  if (!form.name.trim()) {
    error.value = '请输入群名称'
    return
  }
  if (nameStatus.value === 'invalid') {
    error.value = nameMessage.value || '群名称不可用'
    return
  }
  loading.value = true
  try {
    const result = await roomStore.createGroup({
      name: form.name.trim(),
      avatar: form.avatar,
      description: form.description,
      joinPermission: form.joinPermission,
      maxMembers: form.maxMembers
    })
    emit('created', result)
    emit('close')
    Object.assign(form, { name: '', avatar: '', description: '', joinPermission: 'anyone', maxMembers: 200 })
    nameStatus.value = ''
    if (nameCheckTimer) clearTimeout(nameCheckTimer)
  } catch (e) {
    error.value = e.message || '创建失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.dialog-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}
.dialog-box {
  background: white;
  border-radius: 12px;
  padding: 24px;
  width: 420px;
  max-height: 90vh;
  overflow-y: auto;
}
.dialog-box h3 { margin: 0 0 16px; color: #f1177d; }
.form-group { margin-bottom: 14px; }
.form-group label { display: block; font-size: 13px; margin-bottom: 4px; color: #555; }
.form-group input, .form-group textarea, .form-group select {
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  box-sizing: border-box;
}
.form-group input:focus, .form-group textarea:focus { border-color: #ff69b4; outline: none; }
.required { color: #ff4444; }
.range-value { margin-left: 8px; font-size: 13px; color: #555; }
.dialog-actions { display: flex; gap: 10px; justify-content: flex-end; margin-top: 16px; }
.btn-cancel { padding: 8px 16px; border: 1px solid #ddd; border-radius: 6px; background: #f5f5f5; cursor: pointer; }
.btn-create { padding: 8px 16px; border: none; border-radius: 6px; background: linear-gradient(135deg, #fa94c3, #50b9b4); color: white; cursor: pointer; }
.btn-create:disabled { opacity: 0.5; cursor: not-allowed; }
.avatar-upload { display: flex; align-items: center; gap: 10px; }
.avatar-preview { width: 60px; height: 60px; border-radius: 50%; object-fit: cover; border: 2px solid #ffcce0; }
.upload-btn { padding: 6px 14px; border: 1px solid #ff69b4; border-radius: 6px; background: #fff0f5; color: #ff69b4; cursor: pointer; font-size: 12px; }
.upload-btn:hover { background: #ff69b4; color: #fff; }
.upload-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.error-msg { color: #ff4444; font-size: 13px; margin-top: 10px; }

.name-input-wrapper { display: flex; flex-direction: column; gap: 4px; }
.name-input-wrapper input { transition: border-color 0.3s; }
.name-input-wrapper input.input-valid { border-color: #4caf50; }
.name-input-wrapper input.input-invalid { border-color: #f44336; }
.status-hint { font-size: 12px; }
.status-hint.valid { color: #4caf50; }
.status-hint.invalid { color: #f44336; }
.status-hint.checking { color: #ff9800; }
</style>
