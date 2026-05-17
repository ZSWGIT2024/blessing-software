<template>
  <div class="content-section sakura-bg">
    <h2>意见建议</h2>
    <div class="feedback-container">
      <!-- Submit Form -->
      <div class="feedback-form">
        <div class="form-group">
          <label>反馈类型</label>
          <select v-model="form.type">
            <option value="suggestion">功能建议</option>
            <option value="bug">Bug 报告</option>
            <option value="complaint">投诉</option>
            <option value="experience">体验反馈</option>
            <option value="other">其他</option>
          </select>
        </div>

        <div class="form-group">
          <label>反馈内容 <span class="count">{{ form.content.length }}/500</span></label>
          <textarea v-model="form.content" placeholder="请详细描述您的反馈..." rows="5" maxlength="500"></textarea>
        </div>

        <div class="form-group">
          <label>上传截图 (最多5张，单张≤10MB)</label>
          <div class="upload-images">
            <div v-for="(img, i) in form.images" :key="i" class="img-preview-item">
              <img :src="img.preview" />
              <button class="img-remove" @click="form.images.splice(i, 1)">×</button>
            </div>
            <div v-if="form.images.length < 5" class="upload-area" @click="triggerFileInput">
              <span class="upload-icon">+</span>
              <span>上传</span>
            </div>
          </div>
          <input type="file" ref="fileInput" accept="image/*" style="display:none" @change="handleImages" multiple />
        </div>

        <div class="form-group">
          <label>联系方式 (选填)</label>
          <input type="text" v-model="form.contact" placeholder="邮箱/QQ/微信等" maxlength="100" />
        </div>

        <button class="submit-button" :disabled="submitting" @click="submitFeedback">
          {{ submitting ? '提交中...' : '提交反馈' }}
        </button>
      </div>

      <!-- History -->
      <div class="feedback-history">
        <h3>历史反馈 ({{ history.length }})</h3>
        <div v-if="history.length === 0" class="empty-hint">暂无反馈记录</div>
        <div v-for="item in history" :key="item.id" class="history-item">
          <div class="history-header">
            <span class="history-type">{{ typeLabel(item.type) }}</span>
            <span class="history-date">{{ formatDate(item.createTime) }}</span>
            <span class="history-status" :class="item.status">{{ statusLabel(item.status) }}</span>
          </div>
          <div class="history-content">{{ item.content }}</div>
          <!-- Images -->
          <div v-if="item.images" class="history-images">
            <img v-for="(url, i) in parseImageUrls(item.images)" :key="i" :src="url" @click="previewImage(url)" class="history-thumb" />
          </div>
          <div v-if="item.adminReply" class="admin-reply">
            <strong>管理员回复：</strong>{{ item.adminReply }}
          </div>
          <!-- Actions: only for pending -->
          <div v-if="item.status === 'pending'" class="history-actions">
            <button @click="editFeedback(item)" class="action-btn edit">编辑</button>
            <button @click="deleteFeedback(item.id)" class="action-btn delete">删除</button>
          </div>
          <div v-else-if="item.status === 'resolved' || item.status === 'closed'" class="history-actions">
            <button @click="deleteFeedback(item.id)" class="action-btn delete">删除</button>
          </div>
        </div>
      </div>
    </div>

    <!-- Edit Dialog -->
    <div v-if="editing" class="dialog-overlay" @click.self="editing = null">
      <div class="dialog-box">
        <h3>编辑反馈</h3>
        <div class="form-group">
          <label>反馈类型</label>
          <select v-model="editForm.type">
            <option value="suggestion">功能建议</option>
            <option value="bug">Bug 报告</option>
            <option value="complaint">投诉</option>
            <option value="experience">体验反馈</option>
            <option value="other">其他</option>
          </select>
        </div>
        <div class="form-group">
          <label>反馈内容 <span class="count">{{ editForm.content.length }}/500</span></label>
          <textarea v-model="editForm.content" rows="5" maxlength="500"></textarea>
        </div>
        <div class="form-group">
          <label>联系方式</label>
          <input type="text" v-model="editForm.contact" maxlength="100" />
        </div>
        <div class="dialog-actions">
          <button @click="editing = null">取消</button>
          <button @click="confirmEdit" :disabled="editLoading">{{ editLoading ? '保存中...' : '保存' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/requests'

const fileInput = ref(null)
const submitting = ref(false)
const editing = ref(null)
const editLoading = ref(false)
const history = ref([])

const form = reactive({
  type: 'suggestion',
  content: '',
  images: [], // { file, preview }
  contact: ''
})

const editForm = reactive({ type: '', content: '', contact: '' })

const typeLabel = (t) => ({ suggestion: '功能建议', bug: 'Bug报告', complaint: '投诉', experience: '体验反馈', other: '其他' }[t] || t)
const statusLabel = (s) => ({ pending: '待处理', processing: '处理中', resolved: '已处理', closed: '已关闭' }[s] || s)

const formatDate = (d) => d ? new Date(d).toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' }) : ''

const triggerFileInput = () => fileInput.value?.click()

const handleImages = (e) => {
  const files = Array.from(e.target.files)
  e.target.value = ''
  if (form.images.length + files.length > 5) { ElMessage.warning('最多上传5张图片'); return }
  for (const f of files) {
    if (f.size > 10 * 1024 * 1024) { ElMessage.warning(`图片 ${f.name} 超过10MB`); continue }
    if (!f.type.startsWith('image/')) { ElMessage.warning(`${f.name} 不是图片文件`); continue }
    form.images.push({ file: f, preview: URL.createObjectURL(f) })
  }
}

const submitFeedback = async () => {
  if (!form.content.trim()) { ElMessage.warning('请填写反馈内容'); return }
  if (form.content.length > 500) { ElMessage.warning('反馈内容不能超过500字'); return }
  submitting.value = true
  try {
    // Upload images via dedicated feedback upload endpoint
    const urls = []
    if (form.images.length > 0) {
      for (const img of form.images) {
        const fd = new FormData(); fd.append('file', img.file)
        const res = await request.post('/feedback/upload-image', fd, {
          headers: { 'Content-Type': 'multipart/form-data' }
        })
        if (res.code === 0) urls.push(res.data.url)
        else throw new Error(res.msg || '图片上传失败')
      }
    }
    const imageUrls = JSON.stringify(urls)

    const res = await request.post('/feedback', {
      type: form.type, content: form.content, images: imageUrls, contact: form.contact
    })
    if (res.code === 0) {
      ElMessage.success('反馈已提交')
      form.type = 'suggestion'; form.content = ''; form.images = []; form.contact = ''
      loadHistory()
    } else ElMessage.error(res.msg || '提交失败')
  } catch (e) {
    ElMessage.error('提交失败: ' + (e.message || '请重试'))
  } finally { submitting.value = false }
}

const loadHistory = async () => {
  try {
    const res = await request.get('/feedback/my')
    if (res.code === 0) history.value = res.data || []
  } catch (_) {}
}

const editFeedback = (item) => {
  editForm.type = item.type; editForm.content = item.content; editForm.contact = item.contact || ''
  editing.value = item
}

const confirmEdit = async () => {
  if (!editing.value) return
  editLoading.value = true
  try {
    const res = await request.put(`/feedback/${editing.value.id}`, {
      type: editForm.type, content: editForm.content, contact: editForm.contact
    })
    if (res.code === 0) { ElMessage.success('已更新'); loadHistory(); editing.value = null }
    else ElMessage.error(res.msg)
  } catch (_) { ElMessage.error('编辑失败') }
  finally { editLoading.value = false }
}

const deleteFeedback = async (id) => {
  if (!confirm('确定删除此反馈？')) return
  try {
    const res = await request.delete(`/feedback/${id}`)
    if (res.code === 0) { ElMessage.success('已删除'); loadHistory() }
    else ElMessage.error(res.msg)
  } catch (_) { ElMessage.error('删除失败') }
}

const parseImageUrls = (images) => {
  if (!images) return []
  try { return JSON.parse(images) } catch { return images.split(',') }
}

const previewImage = (url) => window.open(url, '_blank')

onMounted(() => loadHistory())
</script>

<style scoped>
.feedback-container { display: flex; gap: 24px; }
.feedback-form { flex: 1; background: rgba(255,255,255,0.85); border-radius: 15px; padding: 20px; }
.form-group { margin-bottom: 16px; }
.form-group label { display: block; margin-bottom: 6px; color: #666; font-weight: bold; font-size: 14px; }
.form-group select, .form-group input, .form-group textarea {
  width: 100%; padding: 10px; border: 1px solid #ffb6c1; border-radius: 6px; background: #fff; font-size: 14px; box-sizing: border-box;
}
.form-group textarea { resize: vertical; }
.count { font-weight: normal; color: #999; font-size: 12px; float: right; }

.upload-images { display: flex; gap: 8px; flex-wrap: wrap; }
.img-preview-item { position: relative; width: 80px; height: 80px; border-radius: 6px; overflow: hidden; border: 1px solid #ddd; }
.img-preview-item img { width: 100%; height: 100%; object-fit: cover; }
.img-remove { position: absolute; top: 0; right: 0; background: #f44336; color: white; border: none; width: 18px; height: 18px; font-size: 12px; cursor: pointer; border-radius: 0 6px 0 6px; }
.upload-area { width: 80px; height: 80px; border: 2px dashed #ffb6c1; border-radius: 6px; display: flex; flex-direction: column; align-items: center; justify-content: center; cursor: pointer; color: #ff69b4; font-size: 12px; transition: background 0.3s; }
.upload-area:hover { background: #fff0f5; }
.upload-icon { font-size: 24px; }

.submit-button { width: 100%; padding: 12px; background: #ff69b4; color: white; border: none; border-radius: 6px; font-weight: bold; cursor: pointer; transition: background 0.3s; }
.submit-button:disabled { opacity: 0.5; cursor: not-allowed; }
.submit-button:hover:not(:disabled) { background: #ff1493; }

.feedback-history { flex: 1; background: rgba(255,255,255,0.85); border-radius: 15px; padding: 20px; max-height: 600px; overflow-y: auto; }
.feedback-history h3 { margin-top: 0; color: #f1177d; }
.empty-hint { color: #999; text-align: center; padding: 40px 0; }

.history-item { margin-bottom: 14px; padding-bottom: 14px; border-bottom: 1px dashed #ffb6c1; }
.history-item:last-child { border-bottom: none; }
.history-header { display: flex; justify-content: space-between; margin-bottom: 8px; font-size: 13px; align-items: center; }
.history-type { color: #ff69b4; font-weight: bold; }
.history-date { color: #999; font-size: 12px; }
.history-status { padding: 2px 10px; border-radius: 10px; font-size: 11px; }
.history-status.pending { background: #f0f0f0; color: #666; }
.history-status.processing { background: #fff3e0; color: #ff9800; }
.history-status.resolved { background: #e8f5e9; color: #2e7d32; }
.history-status.closed { background: #e0e0e0; color: #555; }
.history-content { color: #333; line-height: 1.5; font-size: 14px; word-break: break-all; }
.history-images { display: flex; gap: 6px; margin-top: 6px; }
.history-thumb { width: 60px; height: 60px; object-fit: cover; border-radius: 4px; cursor: pointer; border: 1px solid #eee; }
.admin-reply { margin-top: 8px; padding: 8px 12px; background: #e8f5e9; border-radius: 6px; font-size: 13px; color: #2e7d32; }
.history-actions { margin-top: 8px; display: flex; gap: 8px; }
.action-btn { padding: 3px 12px; border: none; border-radius: 4px; cursor: pointer; font-size: 12px; }
.action-btn.edit { background: #4caf50; color: white; }
.action-btn.delete { background: #f44336; color: white; }

.dialog-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 3000; }
.dialog-box { background: white; border-radius: 12px; padding: 24px; width: 440px; }
.dialog-box h3 { margin: 0 0 16px; color: #f1177d; }
.dialog-actions { display: flex; gap: 12px; justify-content: flex-end; margin-top: 16px; }
.dialog-actions button { padding: 8px 20px; border: none; border-radius: 6px; cursor: pointer; }
.dialog-actions button:first-child { background: #f0f0f0; color: #666; }
.dialog-actions button:last-child { background: #ff69b4; color: white; }
.dialog-actions button:last-child:disabled { opacity: 0.5; }
</style>
