<template>
  <div class="upload-page-overlay" @click.self="emit('close')">
    <div class="upload-page">
      <div class="upload-header">
        <h2>上传媒体作品</h2>
        <button class="close-btn" @click="emit('close')">✕</button>
      </div>

      <!-- Status Bar -->
      <div class="status-bar">
        <div class="daily-info">
          📊 今日已上传 <b>{{ dailyUsed }}</b> 张
          <span v-if="dailyLimit > 0">，剩余可上传 <b>{{ dailyRemaining }}</b> 张</span>
          <span v-if="dailyLimit < 0">（无限上传）</span>
          <span v-if="vipLabel" class="vip-tag">{{ vipLabel }}</span>
        </div>
      </div>

      <!-- Drop Zone -->
      <div class="drop-zone" :class="{ 'drag-over': isDragOver }"
        @dragover.prevent="isDragOver = true" @dragleave.prevent="isDragOver = false"
        @drop.prevent="handleDrop" @click="triggerFileInput">
        <input ref="fileInput" type="file" accept="image/*,video/*" multiple
               style="display:none" @change="handleFileSelect" />
        <div class="drop-content">
          <span class="drop-icon">📁</span>
          <p>拖拽文件到此处，或点击选择</p>
          <p class="drop-hint">图片批量上传（单张≤30MB，最多10张），视频单独上传（≤500MB）</p>
        </div>
      </div>

      <!-- File Cards -->
      <div v-if="files.length > 0" class="files-section">
        <div v-for="f in files" :key="f.id" class="file-card"
          :class="{ 'card-success': f.status === 'success', 'card-uploading': f.status === 'uploading', 'card-failed': f.status === 'failed' }">
          <div class="card-preview">
            <img v-if="f.previewUrl" :src="f.previewUrl" class="card-thumb" />
            <div v-else class="card-thumb-placeholder">⏯️</div>
            <div v-if="f.status === 'uploading'" class="card-progress">
              <div class="progress-bar"><div class="progress-fill" :style="{ width: f.progress + '%' }"></div></div>
              <div class="progress-detail">
                <span>{{ f.progress }}%</span>
                <span v-if="f.eta > 0" class="eta"> 约{{ formatEta(f.eta) }}</span>
              </div>
            </div>
            <div v-if="f.status === 'success'" class="card-badge success">✅ 已就绪</div>
            <div v-if="f.status === 'failed'" class="card-badge failed">❌ {{ f.error }}</div>
            <div v-if="f.status === 'cancelled'" class="card-badge cancelled">✕ 已取消</div>
          </div>
          <div class="card-edit">
            <div class="edit-field">
              <label>标题</label>
              <input v-model="f.editName" :placeholder="f.name" maxlength="100" />
            </div>
            <div class="edit-field">
              <label>描述</label>
              <textarea v-model="f.editDesc" placeholder="添加描述..." rows="2" maxlength="500"></textarea>
            </div>
            <div class="edit-row">
              <div class="edit-field">
                <label>分类</label>
                <select v-model="f.editCategory">
                  <option v-for="c in categoryOptions" :key="c.value" :value="c.label">{{ c.label }}</option>
                </select>
              </div>
              <div class="edit-field">
                <label>可见性</label>
                <select v-model="f.editPublic">
                  <option :value="true">公开</option>
                  <option :value="false">私有</option>
                </select>
              </div>
            </div>
            <div class="card-actions">
              <template v-if="f.status === 'uploading'">
                <button class="btn-cancel" @click="cancelFile(f.id)">取消上传</button>
              </template>
              <template v-if="f.status === 'failed' || f.status === 'cancelled'">
                <button class="btn-retry" @click="retryFile(f.id)">重试</button>
              </template>
              <button class="btn-delete" @click="removeFile(f.id)">{{ f.status === 'uploading' ? '取消' : '删除' }}</button>
            </div>
          </div>
        </div>
      </div>

      <!-- Bottom Bar -->
      <div v-if="files.length > 0" class="bottom-bar">
        <div class="bottom-options">
          <label class="checkbox-label" @click="globalIsAI = !globalIsAI">
            <span class="checkbox-box" :class="{ checked: globalIsAI }">{{ globalIsAI ? '☑' : '☐' }}</span> 是否AI作品
          </label>
          <label class="checkbox-label" @click="globalIsPublic = !globalIsPublic">
            <span class="checkbox-box" :class="{ checked: globalIsPublic }">{{ globalIsPublic ? '☑' : '☐' }}</span> 公开
          </label>
        </div>
        <div class="bottom-right">
          <span class="file-count">{{ files.length }} 个文件，{{ successCount }} 个已就绪</span>
          <button class="btn-submit" :disabled="successCount === 0 || submitting || uploadingCount > 0" @click="submitAll">
            {{ submitting ? '提交中...' : uploadingCount > 0 ? '等待上传完成...' : '提交发布 (' + successCount + ')' }}
          </button>
        </div>
      </div>

      <div v-if="submitResult" class="submit-result" :class="{ 'result-ok': submitResult.ok, 'result-err': !submitResult.ok }">
        {{ submitResult.msg }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserInfoStore } from '@/stores/userInfo'
import { batchMediaService } from '@/api/media'
import { batchSubmitService } from '@/api/submit'
import { compressImage } from '@/utils/compressImage'

const emit = defineEmits(['close', 'upload-success'])
const userInfoStore = useUserInfoStore()
const userId = ref(userInfoStore.currentUser?.id || null)

const fileInput = ref(null)
const isDragOver = ref(false)
const submitting = ref(false)
const submitResult = ref(null)

// Category options
const categoryOptions = [
  { value: 'life', label: '生活随拍' }, { value: 'work', label: '工作相关' },
  { value: 'pet', label: '萌宠' }, { value: 'scenery', label: '自然风景' },
  { value: 'portrait', label: '人像摄影' }, { value: 'art', label: '艺术画' },
  { value: 'anime', label: '二次元' }, { value: 'other', label: '其他' }
]

// Global options
const globalIsAI = ref(false)
const globalIsPublic = ref(true)

// Daily limit
const dailyUsed = ref(0)
const dailyLimit = computed(() => {
  const vt = userInfoStore.currentUser?.vipType || 0
  if (vt === 4) return -1 // lifetime unlimited
  return [20, 50, 100, 200][vt] || 20
})
const dailyRemaining = computed(() => dailyLimit.value < 0 ? -1 : Math.max(0, dailyLimit.value - dailyUsed.value))
const vipLabel = computed(() => {
  const vt = userInfoStore.currentUser?.vipType || 0
  return ['', '月度VIP', '季度VIP', '年度VIP', '终身VIP'][vt] || ''
})

const files = reactive([])
const successCount = computed(() => files.filter(f => f.status === 'success').length)
const uploadingCount = computed(() => files.filter(f => f.status === 'uploading').length)

let idCounter = 0

const fetchDailyInfo = async () => {
  try {
    const tk = localStorage.getItem('accessToken') || ''
    const r = await fetch('/api/oss/upload/daily-info', { headers: { 'Authorization': 'Bearer ' + tk } })
    const d = await r.json()
    if (d.code === 0) dailyUsed.value = d.data.todayUsed || 0
  } catch (_) {}
}

onMounted(() => { fetchDailyInfo() })

const triggerFileInput = () => fileInput.value?.click()
const handleDrop = (e) => { isDragOver.value = false; addFiles(Array.from(e.dataTransfer.files)) }
const handleFileSelect = (e) => { addFiles(Array.from(e.target.files)); e.target.value = '' }

const addFiles = (fileList) => {
  //判断上传的是不是图片或视频，如果不是，提示用户
  if (fileList.length === 0) return
  if (dailyRemaining.value === 0) { ElMessage.error('今日上传次数已达上限'); return }
  if (files.length + fileList.length > 10) { ElMessage.error('一次最多上传10张图片'); return }
  if (fileList.some(f => !f.type.startsWith('image/') && !f.type.startsWith('video/'))) { ElMessage.error('只能上传图片或视频'); return }
  const images = fileList.filter(f => f.type.startsWith('image/'))
  const videos = fileList.filter(f => f.type.startsWith('video/'))
  for (const f of images) { if (f.size > 30 * 1024 * 1024) { ElMessage.error(`图片 ${f.name} 超过30MB`); return } }
  for (const f of videos) { if (f.size > 500 * 1024 * 1024) { ElMessage.error(`视频 ${f.name} 超过500MB`); return } }
  if (videos.length > 1) { ElMessage.error('视频只能单独上传'); return }
  const all = [...images, ...videos]
  if (all.length + files.length > 10) { ElMessage.error('一次最多上传10张图片'); return }
  for (const file of all) startUpload(file)
}

const startUpload = async (file) => {
  if (dailyRemaining.value === 0) { ElMessage.error('今日上传次数已达上限'); return }
  const id = 'uf_' + (++idCounter) + '_' + Date.now()
  const startTs = Date.now()
  const totalSize = file.size
  let lastBytes = 0, lastTs = startTs

  const entry = reactive({
    id, name: file.name, size: file.size, progress: 0, eta: 0,
    status: 'uploading', fileUrl: null, error: null,
    previewUrl: null, fileObj: file, controller: null,
    editName: '', editDesc: '', editCategory: '生活随拍', editPublic: true
  })
  if (file.type.startsWith('image/')) entry.previewUrl = URL.createObjectURL(file)
  files.push(entry)

  try {
    const controller = new AbortController()
    entry.controller = controller

    const onProgress = (pct) => {
      const now = Date.now()
      if (pct > 0 && pct < 100) {
        const bytesDone = totalSize * pct / 100
        const speed = (bytesDone - lastBytes) / Math.max((now - lastTs) / 1000, 0.1)
        lastBytes = bytesDone; lastTs = now
        entry.eta = speed > 0 ? (totalSize - bytesDone) / speed : 0
      }
      entry.progress = Math.min(pct, 99)
    }

    // Maybe compress large images
    let uploadFile = file
    if (file.type.startsWith('image/') && file.size > 5 * 1024 * 1024) {
      try { uploadFile = await compressImage(file, { quality: 0.8 }) } catch (_) {}
    }

    const result = await uploadToOss(uploadFile, controller, onProgress)
    entry.status = 'success'; entry.progress = 100; entry.eta = 0
    entry.fileUrl = result.fileUrl; entry.editName = file.name.replace(/\.[^/.]+$/, '')
  } catch (e) {
    entry.status = e.name === 'CanceledError' || e.message === 'cancelled' ? 'cancelled' : 'failed'
    if (entry.status === 'failed') entry.error = e.message || '上传失败'
  }
}

const uploadToOss = async (file, controller, onProgress) => {
  const tk = localStorage.getItem('accessToken') || ''
  // Get PUT presigned URL
  const sigRes = await fetch('/api/oss/upload/signature', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + tk },
    body: JSON.stringify({ fileName: file.name, fileSize: file.size, folder: 'media', contentType: file.type || 'application/octet-stream' })
  })
  const sigData = await sigRes.json()
  if (sigData.code !== 0) throw new Error(sigData.msg || '获取签名失败')

  const host = sigData.data.host
  const { objectName, signedUrl } = sigData.data

  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest()
    xhr.open('PUT', signedUrl)
    xhr.setRequestHeader('Content-Type', file.type || 'application/octet-stream')
    xhr.upload.onprogress = (e) => { if (e.lengthComputable) onProgress(Math.round(e.loaded * 100 / e.total)) }
    xhr.onload = () => {
      if (xhr.status === 200 || xhr.status === 204) resolve({ fileUrl: host + '/' + objectName, fileSize: file.size, objectName, fileName: file.name })
      else reject(new Error('OSS error ' + xhr.status))
    }
    xhr.onerror = () => {
      // CORS fallback: try backend proxy
      uploadViaBackend(file, controller, onProgress).then(resolve).catch(reject)
    }
    xhr.onabort = () => reject(new DOMException('cancelled', 'CanceledError'))
    controller.signal.addEventListener('abort', () => xhr.abort())
    xhr.send(file)
  })
}

const uploadViaBackend = (file, controller, onProgress) => new Promise((resolve, reject) => {
  const fd = new FormData(); fd.append('file', file); fd.append('folder', 'media')
  const xhr = new XMLHttpRequest(); xhr.open('POST', '/api/oss/upload/direct')
  const tk = localStorage.getItem('accessToken') || ''
  if (tk) xhr.setRequestHeader('Authorization', 'Bearer ' + tk)
  xhr.upload.onprogress = (e) => { if (e.lengthComputable) onProgress(Math.round(e.loaded * 100 / e.total)) }
  xhr.onload = () => { try { const r = JSON.parse(xhr.responseText); r.code === 0 ? resolve(r.data) : reject(new Error(r.msg)) } catch (_) { reject(new Error('Parse error')) } }
  xhr.onerror = () => reject(new Error('Network error'))
  xhr.onabort = () => reject(new DOMException('cancelled', 'CanceledError'))
  controller.signal.addEventListener('abort', () => xhr.abort())
  xhr.send(fd)
})

const cancelFile = (fid) => { const e = files.find(f => f.id === fid); if (e?.controller) e.controller.abort() }
const retryFile = (fid) => { const i = files.findIndex(f => f.id === fid); if (i === -1 || !files[i].fileObj) return; const f = files[i]; files.splice(i, 1); startUpload(f.fileObj) }
const removeFile = (fid) => { const i = files.findIndex(f => f.id === fid); if (i === -1) return; const f = files[i]; if (f.controller) f.controller.abort(); if (f.previewUrl) URL.revokeObjectURL(f.previewUrl); files.splice(i, 1) }

const guessMimeType = (file) => {
  if (!file) return 'application/octet-stream'
  if (file.type && file.type !== '') return file.type
  const ext = (file.name || '').split('.').pop()?.toLowerCase()
  const map = { jpg:'image/jpeg', jpeg:'image/jpeg', png:'image/png', gif:'image/gif', webp:'image/webp', bmp:'image/bmp', svg:'image/svg+xml', mp4:'video/mp4', avi:'video/x-msvideo', mov:'video/quicktime', mkv:'video/x-matroska', webm:'video/webm', flv:'video/x-flv', wmv:'video/x-ms-wmv', mp3:'audio/mpeg', wav:'audio/wav', pdf:'application/pdf' }
  return map[ext] || 'application/octet-stream'
}

const formatEta = (s) => { if (s <= 0) return ''; if (s < 60) return Math.ceil(s) + '秒'; return Math.ceil(s / 60) + '分钟' }

const submitAll = async () => {
  const ready = files.filter(f => f.status === 'success')
  if (ready.length === 0) return
  submitting.value = true; submitResult.value = null

  try {
    const items = ready.map(f => {
      let cat = f.editCategory || '生活随拍'
      if (globalIsAI.value && !cat.includes('(AI)')) cat = '(AI)' + cat
      const fObj = f.fileObj
      return {
        fileUrl: f.fileUrl,
        fileName: f.editName || f.name,           // 用户编辑的标题
        originalName: fObj?.name || f.name,         // 原始文件名（如 123.jpg）
        mimeType: guessMimeType(fObj) || 'application/octet-stream',
        description: f.editDesc || '',
        category: cat,
        isPublic: globalIsPublic.value,
        mediaType: fObj?.type?.startsWith('video/') ? 'video' : 'image',
        fileSize: f.fileSize || f.size,
        wall: Math.random() > 0.5 ? 'left' : 'right'
      }
    })

    // Route to correct endpoint based on isAI
    const apiFn = globalIsAI.value ? batchMediaService : batchSubmitService
    const res = await apiFn(items)

    if (res.code === 0) {
      const count = res.data?.length || items.length
      submitResult.value = { ok: true, msg: `成功发布 ${count} 个作品！` }
      ready.forEach(f => { if (f.previewUrl) URL.revokeObjectURL(f.previewUrl) })
      files.splice(0, files.length, ...files.filter(f => f.status !== 'success'))
      emit('upload-success', res.data)
      dailyUsed.value += count
      setTimeout(() => { submitResult.value = null }, 3000)
    } else throw new Error(res.msg || '提交失败')
  } catch (e) {
    submitResult.value = { ok: false, msg: '提交失败: ' + (e.message || '请重试') }
  } finally { submitting.value = false }
}

onUnmounted(() => {
  files.forEach(f => { if (f.controller) f.controller.abort(); if (f.previewUrl) URL.revokeObjectURL(f.previewUrl) })
})
</script>

<style scoped>
.upload-page-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.6); display: flex; align-items: center; justify-content: center; z-index: 3000; }
.upload-page { width: 92vw; max-width: 1200px; height: 90vh; background: #fff; border-radius: 16px; display: flex; flex-direction: column; overflow: hidden; box-shadow: 0 20px 60px rgba(0,0,0,0.3); }
.upload-header { display: flex; justify-content: space-between; align-items: center; padding: 14px 24px; border-bottom: 1px solid #eee; background: linear-gradient(135deg, #fa94c3, #50b9b4); }
.upload-header h2 { margin: 0; color: white; font-size: 18px; }
.close-btn { background: none; border: none; font-size: 24px; color: white; cursor: pointer; }

.status-bar { padding: 8px 24px; background: #fff9fa; border-bottom: 1px solid #ffecef; font-size: 13px; color: #666; }
.vip-tag { background: #ff69b4; color: white; padding: 1px 8px; border-radius: 10px; font-size: 11px; margin-left: 6px; }

.drop-zone { margin: 12px 20px; padding: 30px 20px; border: 3px dashed #ddd; border-radius: 12px; text-align: center; cursor: pointer; transition: all 0.3s; background: #fafafa; flex-shrink: 0; }
.drop-zone:hover, .drop-zone.drag-over { border-color: #ff69b4; background: #fff5f8; }
.drop-icon { font-size: 36px; }
.drop-content p { color: #888; margin: 6px 0 0; font-size: 14px; }
.drop-hint { font-size: 12px !important; color: #bbb !important; }

.files-section { flex: 1; overflow-y: auto; padding: 0 20px 12px; display: flex; flex-wrap: wrap; gap: 12px; align-content: flex-start; }
.file-card { display: flex; gap: 12px; background: #fff; border: 1px solid #eee; border-radius: 10px; padding: 10px; width: calc(50% - 6px); min-width: 350px; transition: all 0.3s; }
.file-card.card-success { border-color: #c8e6c9; }
.file-card.card-uploading { border-color: #ffe0b2; }
.file-card.card-failed { border-color: #ffcdd2; background: #fff5f5; }

.card-preview { width: 140px; flex-shrink: 0; position: relative; }
.card-thumb { width: 140px; height: 100px; object-fit: cover; border-radius: 8px; }
.card-thumb-placeholder { width: 140px; height: 100px; display: flex; align-items: center; justify-content: center; background: #f0f0f0; border-radius: 8px; font-size: 32px; }
.card-progress { margin-top: 4px; }
.progress-bar { height: 5px; background: #e0e0e0; border-radius: 3px; overflow: hidden; }
.progress-fill { height: 100%; background: linear-gradient(90deg, #ff9800, #4caf50); transition: width 0.3s; }
.progress-detail { display: flex; justify-content: space-between; margin-top: 2px; font-size: 10px; color: #666; }
.eta { color: #ff9800; }
.card-badge { position: absolute; top: 4px; left: 4px; padding: 2px 6px; border-radius: 4px; font-size: 10px; }
.card-badge.success { background: #c8e6c9; color: #2e7d32; }
.card-badge.failed { background: #ffcdd2; color: #c62828; }
.card-badge.cancelled { background: #e0e0e0; color: #757575; }

.card-edit { flex: 1; display: flex; flex-direction: column; gap: 4px; min-width: 0; }
.edit-field { display: flex; flex-direction: column; gap: 1px; }
.edit-field label { font-size: 10px; color: #888; font-weight: 500; }
.edit-field input, .edit-field textarea, .edit-field select {
  padding: 4px 6px; border: 1px solid #e0e0e0; border-radius: 5px; font-size: 11px; outline: none; transition: border-color 0.2s; width: 100%; box-sizing: border-box;
}
.edit-field input:focus, .edit-field textarea:focus, .edit-field select:focus { border-color: #ff69b4; }
.edit-row { display: flex; gap: 6px; }
.edit-row .edit-field { flex: 1; }
.card-actions { display: flex; gap: 6px; margin-top: auto; padding-top: 4px; }
.btn-cancel { padding: 3px 10px; background: #ff9800; color: white; border: none; border-radius: 5px; cursor: pointer; font-size: 10px; }
.btn-retry { padding: 3px 10px; background: #4caf50; color: white; border: none; border-radius: 5px; cursor: pointer; font-size: 10px; }
.btn-delete { padding: 3px 10px; background: #f44336; color: white; border: none; border-radius: 5px; cursor: pointer; font-size: 10px; }

.bottom-bar { display: flex; justify-content: space-between; align-items: center; padding: 10px 24px; border-top: 1px solid #eee; background: #fafafa; flex-shrink: 0; }
.bottom-options { display: flex; gap: 16px; }
.checkbox-label { display: flex; align-items: center; gap: 4px; cursor: pointer; user-select: none; font-size: 13px; color: #555; }
.checkbox-box { font-size: 15px; color: #888; }
.checkbox-box.checked { color: #ff69b4; }
.bottom-right { display: flex; align-items: center; gap: 14px; }
.file-count { color: #666; font-size: 12px; }
.btn-submit { padding: 8px 28px; border: none; border-radius: 8px; background: linear-gradient(135deg, #fa94c3, #50b9b4); color: white; font-size: 14px; font-weight: 600; cursor: pointer; transition: all 0.3s; }
.btn-submit:disabled { opacity: 0.4; cursor: not-allowed; }
.btn-submit:not(:disabled):hover { transform: translateY(-2px); box-shadow: 0 4px 15px rgba(250,148,195,0.4); }
.submit-result { padding: 6px 24px; text-align: center; font-size: 13px; }
.result-ok { background: #e8f5e9; color: #2e7d32; }
.result-err { background: #ffebee; color: #c62828; }

@media (max-width: 768px) {
  .upload-page { width: 96vw; height: 96vh; }
  .file-card { width: 100%; flex-direction: column; }
  .card-preview { width: 100%; }
  .card-thumb { width: 100%; height: 140px; }
  .bottom-bar { flex-direction: column; gap: 8px; }
}
</style>
