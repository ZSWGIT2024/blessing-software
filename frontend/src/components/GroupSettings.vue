<template>
  <div v-if="visible" class="settings-overlay" @click.self="$emit('close')">
    <div class="settings-panel">
      <div class="panel-header">
        <h3>群设置</h3>
        <button class="close-btn" @click="$emit('close')">✕</button>
      </div>

      <div class="settings-scroll">
        <div class="form-group">
          <label>群名称</label>
          <div class="name-input-wrapper">
            <input
              v-model="form.name"
              @input="onGroupNameInput"
              :disabled="!canEdit"
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
          <textarea v-model="form.description" :disabled="!canEdit" placeholder="请输入群简介, 最多500个字符。" rows="5" maxlength="500"></textarea>
        </div>
        <div class="form-group">
          <label>最大成员数</label>
          <input type="number" v-model="form.maxMembers" :disabled="!canEdit" min="1" max="200">
        </div>
        <div class="form-group">
          <label>加入方式</label>
          <select v-model="form.joinPermission" :disabled="!canEdit">
            <option value="anyone">任何人可加入</option>
            <option value="approval">需要审批</option>
            <option value="invite">仅邀请加入</option>
          </select>
        </div>
        <div class="form-group">
          <label>邀请码</label>
          <div class="invite-row">
            <code>{{ inviteCode || '点击生成' }}</code>
            <button class="generate-btn" @click="generateCode" v-if="canManage">生成</button>
            <button class="copy-btn" v-if="inviteCode" @click="copyCode">复制</button>
          </div>
        </div>
        <div class="form-group">
          <label>全员禁言</label>
          <input type="checkbox" v-model="form.isMutedAll" :disabled="!canManage">
        </div>

        <button v-if="canEdit" class="save-btn" @click="save" :disabled="saving">
          {{ saving ? '保存中...' : '保存设置' }}
        </button>

        <!-- My Member Info -->
        <div class="member-info-section" v-if="myMemberInfo">
          <h4 class="section-title">我的群内信息</h4>
          <div class="info-row"><span class="info-label">角色</span><span class="info-value role-badge" :class="'role-' + myMemberInfo.role">{{ roleLabel(myMemberInfo.role) }}</span></div>
          <div class="info-row"><span class="info-label">禁言状态</span><span class="info-value">{{ myMemberInfo.isMuted ? '已禁言' : '正常' }}</span></div>
          <div class="info-row" v-if="myMemberInfo.isMuted && myMemberInfo.mutedUntil"><span class="info-label">禁言到期</span><span class="info-value">{{ formatTime(myMemberInfo.mutedUntil) }}</span></div>
          <div class="info-row"><span class="info-label">加入时间</span><span class="info-value">{{ formatTime(myMemberInfo.joinTime) }}</span></div>
          <div class="form-group">
            <label>群昵称</label>
            <input v-model="myNickname" @blur="saveNickname" placeholder="设置群内昵称, 最多20个字符" maxlength="20">
          </div>
        </div>

        <!-- Join Requests (admin/owner only) -->
        <div class="join-requests-section" v-if="canManage">
          <h4 class="section-title">入群申请： <span class="request-badge">{{ joinRequests.length }}件</span></h4>
          <div v-for="req in joinRequests" :key="req.userId" class="request-item">
            <img :src="req.avatar || '/default-avatar.png'" class="req-avatar">
            <span class="req-name">{{ req.username }}</span>
            <div class="req-actions">
              <button class="approve-btn" @click="handleApprove(req.userId)">通过</button>
              <button class="reject-btn" @click="handleReject(req.userId)">拒绝</button>
            </div>
          </div>
        </div>

        <div class="danger-zone" v-if="isOwner">
          <h4>危险操作</h4>
          <button class="danger-btn" @click="handleDissolve">解散群聊</button>
        </div>

        <div v-if="success" class="success-msg">保存成功</div>
        <div v-if="error" class="error-msg">{{ error }}!</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRoomStore } from '@/stores/room'
import { useUserInfoStore } from '@/stores/userInfo'
import { compressImageFunc } from '@/utils/compressImage'
import { useTokenStore } from '@/stores/token'
import * as groupChatApi from '@/api/groupChatApi'
import { checkGroupName } from '@/api/groupChatApi'
import { ElMessage, ElMessageBox } from 'element-plus'

const props = defineProps({ visible: Boolean, groupId: String, myRole: String })
const emit = defineEmits(['close', 'dissolved'])

const roomStore = useRoomStore()
const userInfoStore = useUserInfoStore()
const currentUserId = computed(() => userInfoStore.currentUser?.id)
const saving = ref(false)
const avatarUploading = ref(false)
const error = ref('')
const success = ref(false)
const inviteCode = ref('')
const loaded = ref(false)

// Group name real-time validation
const nameStatus = ref('') // '', 'checking', 'valid', 'invalid'
const nameMessage = ref('')
let nameCheckTimer = null
let originalName = '' // track original name for comparison

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
  // Unchanged from original
  if (name === originalName) {
    nameStatus.value = 'valid'
    nameMessage.value = '与当前群名称一致'
    if (nameCheckTimer) clearTimeout(nameCheckTimer)
    return
  }

  if (nameCheckTimer) clearTimeout(nameCheckTimer)
  nameCheckTimer = setTimeout(async () => {
    nameStatus.value = 'checking'
    try {
      const res = await checkGroupName(name, props.groupId)
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

const isOwner = computed(() => props.myRole === 'owner')
const canEdit = computed(() => props.myRole === 'owner' || props.myRole === 'admin')
const canManage = computed(() => props.myRole === 'owner' || props.myRole === 'admin')

const myMemberInfo = computed(() => {
  if (!props.groupId || !currentUserId.value) return null
  const members = roomStore.groupMembers[props.groupId] || []
  return members.find(m => m.userId === currentUserId.value) || null
})
const myNickname = ref('')

const joinRequests = ref([])
const roleLabel = (r) => ({ owner: '群主', admin: '管理员', member: '成员' })[r] || r

const formatTime = (t) => { if (!t) return ''; return new Date(t).toLocaleString('zh-CN') }

const loadJoinRequests = async () => {
  if (!canManage.value || !props.groupId) return
  try {
    const res = await groupChatApi.getJoinRequests(props.groupId)
    if (res.code === 0) joinRequests.value = res.data || []
  } catch (_) {}
}

const saveNickname = async () => {
  if (!props.groupId || !myNickname.value) return
  try {
    await groupChatApi.setMyNickname(props.groupId, myNickname.value)
    ElMessage.success('群昵称已保存')
  } catch (e) { ElMessage.error('保存失败') }
}

const handleApprove = async (applicantId) => {
  try {
    const res = await groupChatApi.approveJoinRequest(props.groupId, applicantId)
    if (res.code === 0) { ElMessage.success('已通过'); loadJoinRequests() }
    else ElMessage.error(res.msg)
  } catch (_) {}
}
const handleReject = async (applicantId) => {
  try {
    const res = await groupChatApi.rejectJoinRequest(props.groupId, applicantId)
    if (res.code === 0) { ElMessage.success('已拒绝'); loadJoinRequests() }
    else ElMessage.error(res.msg)
  } catch (_) {}
}

const form = reactive({ name: '', avatar: '', description: '', maxMembers: 0, joinPermission: 'anyone', isMutedAll: false })

const handleAvatarUpload = async (event) => {
  const file = event.target.files?.[0]
  if (!file) return
  if (file.size > 10 * 1024 * 1024) { error.value = '头像大小不能超过10MB'; return }
  avatarUploading.value = true; error.value = ''
  try {
   // 压缩图片
    const compressedFile = await compressImageFunc(file);
    //获取token状态
    const tokenStore = useTokenStore()
    const formData = new FormData();
    formData.append('file', compressedFile);
    // 上传图片到服务器
    const response = await fetch('/api/upload', { // 修改为与后端一致的路径
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${tokenStore.accessToken}`
        // 不要手动设置Content-Type，浏览器会自动添加正确的multipart边界
      },
      body: formData // 使用FormData作为body
    });

    // 处理服务器响应
    if (response.ok) {
      const data = await response.json();
      // 构建完整URL
      const fullAvatarUrl = data.data
      form.avatar = fullAvatarUrl
      ElMessage.success('头像上传成功,保存设置后生效！')
    } else {
      error.value = data.msg || '上传失败'
      ElMessage.error(data.msg || '上传失败')
    }
  } catch (e) {
    error.value = e.message || '头像上传失败'
    ElMessage.error(e.message || '头像上传失败')
  } finally {
    avatarUploading.value = false
    event.target.value = ''
  }
}
const loadGroupInfo = async () => {
  if (!props.groupId) return
  const info = await roomStore.fetchGroupInfo(props.groupId)
  if (info) {
    Object.assign(form, {
      name: info.name || '',
      avatar: info.avatar || '',
      description: info.description || '',
      maxMembers: info.maxMembers || 0,
      joinPermission: info.joinPermission || 'anyone',
      isMutedAll: info.isMutedAll || false
    })
    inviteCode.value = info.inviteCode || ''
    originalName = info.name || ''
  }
  const mi = myMemberInfo.value
  if (mi) myNickname.value = mi.nicknameInGroup || ''
  loaded.value = true
}

watch(() => props.visible, (val) => {
  if (val) { loaded.value = false; loadGroupInfo(); loadJoinRequests() }
}, { immediate: true })

const save = async () => {
  saving.value = true; error.value = ''; success.value = false
  if (nameStatus.value === 'invalid') {
    error.value = nameMessage.value || '群名称不可用'
    saving.value = false
    return
  }
  try {
    const res = await groupChatApi.updateGroup(props.groupId, { ...form })
    if (res.code === 0) {
       success.value = true
       //刷新群聊信息
       await roomStore.fetchGroupInfo(props.groupId)
    } 
    else error.value = res.msg
  } catch (e) {
    error.value = '保存失败'
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const generateCode = async () => {
  const res = await groupChatApi.generateInviteCode(props.groupId)
  if (res.code === 0) inviteCode.value = res.data
}

const copyCode = () => {
  if (inviteCode.value) navigator.clipboard?.writeText(inviteCode.value)
}

const handleDissolve = async () => {
  if (!await ElMessageBox.confirm('确定要解散该群聊吗？此操作不可撤销！', '解散群聊', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })) return
  const res = await roomStore.dissolveGroup(props.groupId)
  if (res.code === 0) { ElMessage.success('群聊已解散'); emit('dissolved') }
}
</script>

<style scoped>
.settings-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.3); display: flex; justify-content: flex-end; z-index: 2000; }
.settings-panel { width: 380px; background: white; height: 88vh; overflow: hidden; display: flex; flex-direction: column; }
.panel-header { display: flex; justify-content: space-between; align-items: center; padding: 16px; border-bottom: 1px solid #eee; }
.panel-header h3 { margin: 0; color: #f1177d; }
.close-btn { background: none; border: none; font-size: 20px; cursor: pointer; color: #999; }
.settings-scroll { flex: 1; overflow-y: auto; padding: 16px; max-height: 700px;}
.form-group { margin-bottom: 14px; }
.form-group label { display: block; font-size: 13px; margin-bottom: 4px; color: #555; }
.form-group input, .form-group textarea, .form-group select {
  width: 100%; padding: 8px; border: 1px solid #ddd; border-radius: 6px; font-size: 14px; box-sizing: border-box;
}
.avatar-upload { display: flex; align-items: center; gap: 10px; }
.avatar-preview { width: 60px; height: 60px; border-radius: 50%; object-fit: cover; border: 2px solid #ffcce0; }
.upload-btn { padding: 6px 14px; border: 1px solid #ff69b4; border-radius: 6px; background: #fff0f5; color: #ff69b4; cursor: pointer; font-size: 12px; }
.upload-btn:hover { background: #ff69b4; color: #fff; }
.upload-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.invite-row { display: flex; gap: 8px; align-items: center; }
.invite-row code { flex: 1; padding: 6px; background: #f5f5f5; border-radius: 4px; font-size: 12px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.invite-row button { background-color: rgb(93, 194, 106); color: #ff69b4; padding: 5px 12px;border-radius: 15px;border: none;cursor: pointer;font-size: 14px;transition: all 0.3s; }
.save-btn { width: 100%; padding: 10px; background: linear-gradient(135deg, #fa94c3, #50b9b4); color: white; border: none; border-radius: 6px; cursor: pointer; margin-top: 10px; }
.danger-zone { margin-top: 24px; padding-top: 16px; border-top: 1px solid #ffcce0; }
.danger-zone h4 { color: #ff4444; margin: 0 0 10px; }
.danger-btn { padding: 8px 16px; background: #ff4444; color: white; border: none; border-radius: 6px; cursor: pointer; }
.member-info-section { margin-top: 20px; padding-top: 16px; border-top: 1px solid #eee; }
.section-title { color: #f1177d; font-size: 14px; margin: 0 0 10px; }
.info-row { display: flex; justify-content: space-between; padding: 6px 0; border-bottom: 1px solid #f5f5f5; font-size: 13px; }
.info-label { color: #888; }
.info-value { color: #333; }
.role-badge { padding: 1px 8px; border-radius: 10px; font-size: 11px; }
.role-owner { background: #ffe0b2; color: #e65100; }
.role-admin { background: #e3f2fd; color: #1565c0; }
.role-member { background: #f5f5f5; color: #666; }

.join-requests-section { margin-top: 20px; padding-top: 16px; border-top: 1px solid #eee; }
.request-badge { background: #ff4757; color: white; font-size: 11px; padding: 1px 6px; border-radius: 8px; }
.request-item { display: flex; align-items: center; padding: 8px; background: #fff5f5; border-radius: 6px; margin-bottom: 6px; gap: 8px; }
.req-avatar { width: 32px; height: 32px; border-radius: 50%; }
.req-name { flex: 1; font-size: 13px; }
.req-actions { display: flex; gap: 6px; }
.approve-btn { padding: 4px 10px; background: #4caf50; color: white; border: none; border-radius: 4px; cursor: pointer; font-size: 11px; }
.reject-btn { padding: 4px 10px; background: #ff4757; color: white; border: none; border-radius: 4px; cursor: pointer; font-size: 11px; }
.success-msg { color: #4caf50; font-size: 16px; margin-top: 20px; margin-left: 150px; }
.error-msg { color: #eb197b; font-size: 18px; margin-top: 20px; margin-left: 150px; }

.name-input-wrapper { display: flex; flex-direction: column; gap: 4px; }
.name-input-wrapper input { transition: border-color 0.3s; }
.name-input-wrapper input.input-valid { border-color: #4caf50; }
.name-input-wrapper input.input-invalid { border-color: #f44336; }
.status-hint { font-size: 12px; }
.status-hint.valid { color: #4caf50; }
.status-hint.invalid { color: #f44336; }
.status-hint.checking { color: #ff9800; }
</style>
