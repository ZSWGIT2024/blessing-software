<template>
  <div class="system-msg-admin">
    <!-- Compose Form -->
    <div class="compose-panel">
      <h4>{{ editingMsg ? '编辑系统消息' : '发送系统消息' }}</h4>
      <div class="form-group">
        <label>消息标题 <span class="required">*</span></label>
        <input v-model="form.title" placeholder="请输入消息标题" maxlength="100">
      </div>
      <div class="form-group">
        <label>消息内容 <span class="required">*</span></label>
        <textarea v-model="form.content" placeholder="请输入消息内容" rows="4" maxlength="500"></textarea>
      </div>
      <div class="form-group">
        <label>目标类型</label>
        <select v-model="form.targetType">
          <option value="all">全部用户</option>
          <option value="user">指定用户</option>
          <option value="group">指定群聊</option>
        </select>
      </div>
      <div class="form-group" v-if="form.targetType === 'user'">
        <label>目标用户ID</label>
        <input v-model="form.targetId" placeholder="请输入用户ID">
      </div>
      <div class="form-group" v-if="form.targetType === 'group'">
        <label>目标群ID</label>
        <input v-model="form.targetId" placeholder="请输入群ID">
      </div>
      <div class="form-group">
        <label>生效时间（可选）</label>
        <input v-model="form.effectiveTime" type="datetime-local">
      </div>
      <div class="form-group">
        <label>过期时间（可选）</label>
        <input v-model="form.expireTime" type="datetime-local">
      </div>
      <div class="form-actions">
        <button v-if="editingMsg" @click="cancelEdit" class="btn-cancel">取消编辑</button>
        <button @click="submitForm" class="btn-submit" :disabled="submitting">
          {{ submitting ? '提交中...' : (editingMsg ? '更新' : '发送') }}
        </button>
      </div>
      <div v-if="formError" class="error-msg">{{ formError }}</div>
    </div>

    <!-- Message List -->
    <div class="list-panel">
      <h4>消息列表</h4>
      <table class="msg-table" v-if="messages.length > 0">
        <thead>
          <tr>
            <th>标题</th>
            <th>目标</th>
            <th>状态</th>
            <th>生效时间</th>
            <th>过期时间</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="msg in messages" :key="msg.messageId">
            <td class="title-cell" :title="msg.title">{{ msg.title }}</td>
            <td>{{ targetLabel(msg) }}</td>
            <td>
              <span :class="['status-tag', msg.isActive ? 'active' : 'inactive']">
                {{ msg.isActive ? '生效中' : '已失效' }}
              </span>
            </td>
            <td>{{ formatTime(msg.effectiveTime) }}</td>
            <td>{{ formatTime(msg.expireTime) }}</td>
            <td>{{ formatTime(msg.createTime) }}</td>
            <td class="action-cell">
              <button @click="startEdit(msg)" class="action-btn edit">编辑</button>
              <button v-if="msg.isActive" @click="toggleActive(msg)" class="action-btn deactivate">停用</button>
              <button v-else @click="toggleActive(msg)" class="action-btn activate">启用</button>
              <button @click="handleDelete(msg)" class="action-btn delete">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-else class="empty-list">暂无系统消息</div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import * as sysMsgApi from '@/api/systemMessageApi'

const messages = ref([])
const editingMsg = ref(null)
const submitting = ref(false)
const formError = ref('')

const form = reactive({
  title: '',
  content: '',
  targetType: 'all',
  targetId: '',
  effectiveTime: '',
  expireTime: ''
})

const resetForm = () => {
  Object.assign(form, {
    title: '', content: '', targetType: 'all', targetId: '', effectiveTime: '', expireTime: ''
  })
}

const targetLabel = (msg) => {
  const map = { all: '全部', user: '用户', group: '群聊' }
  const label = map[msg.targetType] || msg.targetType
  return msg.targetId ? `${label}(${msg.targetId})` : label
}

const formatTime = (t) => {
  if (!t) return '-'
  return new Date(t).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

const loadMessages = async () => {
  try {
    const res = await sysMsgApi.getAdminSystemMessages()
    if (res.code === 0) messages.value = res.data || []
  } catch (e) {
    console.error('获取系统消息列表失败:', e)
  }
}

const submitForm = async () => {
  formError.value = ''
  if (!form.title.trim() || !form.content.trim()) {
    formError.value = '标题和内容不能为空'
    return
  }
  submitting.value = true
  try {
    const data = {
      title: form.title.trim(),
      content: form.content.trim(),
      targetType: form.targetType,
      targetId: form.targetId || null,
      effectiveTime: form.effectiveTime || null,
      expireTime: form.expireTime || null
    }
    let res
    if (editingMsg.value) {
      res = await sysMsgApi.updateSystemMessage(editingMsg.value.messageId, data)
    } else {
      res = await sysMsgApi.createSystemMessage(data)
    }
    if (res.code === 0) {
      resetForm()
      editingMsg.value = null
      await loadMessages()
    } else {
      formError.value = res.msg || '操作失败'
    }
  } catch (e) {
    formError.value = '操作失败'
  } finally {
    submitting.value = false
  }
}

const startEdit = (msg) => {
  editingMsg.value = msg
  Object.assign(form, {
    title: msg.title || '',
    content: msg.content || '',
    targetType: msg.targetType || 'all',
    targetId: msg.targetId || '',
    effectiveTime: msg.effectiveTime ? msg.effectiveTime.substring(0, 16) : '',
    expireTime: msg.expireTime ? msg.expireTime.substring(0, 16) : ''
  })
}

const cancelEdit = () => {
  editingMsg.value = null
  resetForm()
}

const toggleActive = async (msg) => {
  try {
    let res
    if (msg.isActive) {
      res = await sysMsgApi.deactivateSystemMessage(msg.messageId)
    } else {
      res = await sysMsgApi.updateSystemMessage(msg.messageId, { isActive: true })
    }
    if (res.code === 0) await loadMessages()
  } catch (e) {
    console.error('切换状态失败:', e)
  }
}

const handleDelete = async (msg) => {
  if (!confirm('确定要删除这条系统消息吗？')) return
  try {
    const res = await sysMsgApi.deleteSystemMessage(msg.messageId)
    if (res.code === 0) await loadMessages()
  } catch (e) {
    console.error('删除失败:', e)
  }
}

onMounted(() => loadMessages())
</script>

<style scoped>
.system-msg-admin { display: flex; gap: 20px; }
.compose-panel { width: 380px; flex-shrink: 0; }
.compose-panel h4, .list-panel h4 { margin: 0 0 14px; color: #f1177d; }
.form-group { margin-bottom: 12px; }
.form-group label { display: block; font-size: 13px; margin-bottom: 4px; color: #555; }
.form-group input, .form-group textarea, .form-group select {
  width: 100%; padding: 8px; border: 1px solid #ddd; border-radius: 6px; font-size: 13px; box-sizing: border-box;
}
.required { color: #ff4444; }
.form-actions { display: flex; gap: 8px; margin-top: 12px; }
.btn-cancel { padding: 6px 14px; border: 1px solid #ddd; border-radius: 6px; background: #f5f5f5; cursor: pointer; font-size: 13px; }
.btn-submit { padding: 6px 14px; border: none; border-radius: 6px; background: linear-gradient(135deg, #fa94c3, #50b9b4); color: white; cursor: pointer; font-size: 13px; }
.btn-submit:disabled { opacity: 0.5; cursor: not-allowed; }
.error-msg { color: #ff4444; font-size: 12px; margin-top: 8px; }

.list-panel { flex: 1; min-width: 0; }
.msg-table { width: 100%; border-collapse: collapse; }
.msg-table th, .msg-table td { padding: 8px 10px; text-align: left; border-bottom: 1px solid #eee; font-size: 13px; }
.msg-table th { background: #fafafa; color: #666; font-weight: 500; }
.title-cell { max-width: 140px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.status-tag { font-size: 11px; padding: 2px 8px; border-radius: 10px; }
.status-tag.active { background: #e8f5e9; color: #4caf50; }
.status-tag.inactive { background: #f5f5f5; color: #999; }
.action-cell { white-space: nowrap; }
.action-btn { padding: 3px 8px; border: none; border-radius: 4px; cursor: pointer; font-size: 11px; margin-right: 4px; }
.action-btn.edit { background: #e3f2fd; color: #2196f3; }
.action-btn.deactivate { background: #fff3e0; color: #ff9800; }
.action-btn.activate { background: #e8f5e9; color: #4caf50; }
.action-btn.delete { background: #ffebee; color: #ff4444; }
.empty-list { text-align: center; color: #999; padding: 40px; font-size: 13px; }
</style>
