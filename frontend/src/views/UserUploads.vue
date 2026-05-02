<template>
  <div class="modal-overlay" >
    <div class="submit-container">
      <button class="close-modal" @click="emit('close')">×</button>

      <h2 class="upload-title">上传媒体文件</h2>

      <!-- 文件上传区域 - 添加拖拽事件监听 -->
      <div class="upload-area" @click="triggerFileInput" @dragover.prevent="handleDragOver"
        @dragleave.prevent="handleDragLeave" @drop.prevent="handleDrop" :class="{ 'drag-over': isDragOver }">
        <input type="file" ref="fileInputElement" @change="handleFileUpload" accept="image/*,video/*"
          style="display: none" multiple>

        <!-- 添加拖拽提示 -->
        <div v-if="isDragOver && !uploadState.selectedFiles.length" class="drag-overlay">
          <div class="drag-message">
            <span class="drag-icon-large">⬇</span>
            <p>释放文件以上传</p>
          </div>
        </div>

        <!-- 原有内容保持不变 -->
        <div v-if="!uploadState.selectedFiles.length" class="upload-prompt">
          <span class="upload-icon">+</span>
          <p class="upload-instruction">支持图片批量上传，最多一次性上传20个文件。视频请单独上传！</p>
          <p class="upload-hint">单个图片文件不超过30MB，视频文件不超过500MB！</p>
        </div>
        <div v-else class="preview-container">
          <div v-for="(file, index) in uploadState.selectedFiles" :key="index" class="preview-item"
            :class="{ 'is-video': file.mediaType === MediaType.VIDEO }">
            <div class="preview-content">
              <img v-if="file.mediaType === MediaType.IMAGE" :src="file.previewUrl" class="preview-image">
              <div v-else class="video-preview">
                <span class="video-icon">▶</span>
                <p class="video-label">视频</p>
              </div>
              <button class="remove-btn" @click.stop="removeFile(index)">×</button>
              <span class="preview-index">{{ index + 1 }}</span>
            </div>
            <div class="file-info">
              <p class="file-name">{{ file.name }}</p>
              <p class="file-size">{{ formatFileSize(file.size) }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 文件数量提示 -->
      <div v-if="uploadState.selectedFiles.length" class="file-count">
        已选择 {{ uploadState.selectedFiles.length }} 个文件
      </div>

      <!-- 主题输入 -->
      <div class="form-group">
        <label class="form-label">主题：</label>
        <input v-model="uploadState.filename" placeholder="请输入作品主题（选填）" class="form-input" maxlength="36">
        <div class="input-counter">{{ uploadState.filename.length }}/36</div>
      </div>

      <!-- 描述输入 -->
      <div class="form-group">
        <label class="form-label">描述：</label>
        <textarea v-model="uploadState.description" placeholder="请描述一下你的作品（选填）" class="form-textarea" maxlength="66"
          rows="3"></textarea>
        <div class="textarea-counter">{{ uploadState.description.length }}/66</div>
      </div>

      <!-- 分类选择 -->
      <div class="form-group">
        <label class="form-label">分类：</label>
        <select v-model="uploadState.category" class="form-select">
          <option value="">请选择分类（选填）</option>
          <option v-for="option in categoryOptions" :key="option.value" :value="option.value">
            {{ option.label }}
          </option>
        </select>
      </div>

      <!-- 隐私设置 -->
      <div class="form-group">
        <label class="privacy-label">
          <input type="checkbox" v-model="uploadState.isPublic" class="privacy-checkbox">
          <span class="privacy-text">公开分享</span>
        </label>
        <p class="privacy-hint">公开后其他用户可以查看和点赞你的作品</p>
      </div>
      <div class="form-group-ai">
        <label class="AI-label">
          <input type="checkbox" v-model="uploadState.isAI" class="AI-checkbox">
          <span class="AI-text">是否AI生成</span>
        </label>
        <p class="AI-hint">如果是AI生成的作品，请勾选此选项</p>
      </div>

      <!-- 状态提示 -->
      <div v-if="statusMessage" class="status-message" :class="statusType">
        <div class="status-content">
          {{ statusMessage }}
          <!-- 进度条 -->
          <div v-if="uploadState.uploadProgress > 0 && uploadState.uploadProgress < 100" class="progress-bar-container">
            <div class="progress-bar" :style="{ width: uploadState.uploadProgress + '%' }"></div>
          </div>
        </div>
      </div>

      <!-- 提交按钮 -->
      <button class="submit-btn" @click="submitMedia" :disabled="!canSubmit"
        :class="{ 'loading': uploadState.isSubmitting }">
        <span v-if="!uploadState.isSubmitting">提交上传</span>
        <span v-else class="loading-text">
          <span class="loading-dots">...</span>
        </span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onUnmounted, reactive, watch } from 'vue'
import { uploadMediaService } from '@/api/media'
import { uploadSubmitMediaService } from '@/api/submit'
import { compressImage } from '@/utils/compressImage'
import { useUserInfoStore } from '@/stores/userInfo'

const emit = defineEmits(['close', 'upload-success'])

// 用户信息（可以从store或props获取）
const userInfoStore = useUserInfoStore()
const userId = ref(userInfoStore.currentUser?.id || null)
// 文件输入元素的 ref
const fileInputElement = ref(null)
// 上传相关状态 - 统一使用 reactive 管理
const uploadState = reactive({
  // 文件相关
  fileInput: null,
  selectedFiles: [],

  // 表单数据
  filename: '',
  description: '',
  category: '',
  isPublic: true,
  isAI: false,

  // 上传状态
  isSubmitting: false,
  isComponentMounted: true,

  uploadProgress: 0,
  uploadSpeed: 0,
  uploadedBytes: 0,
  totalBytes: 0,
  startTime: 0,

  // 状态信息
  status: {
    type: '',
    message: '',
    progress: 0
  }
})

// 媒体类型枚举
const MediaType = {
  IMAGE: 'image',
  VIDEO: 'video'
}

// 分类选项
const categoryOptions = [
  { value: '生活', label: '生活随拍' },
  { value: '工作', label: '工作相关' },
  { value: '宠物', label: '萌宠' },
  { value: '风景', label: '自然风景' },
  { value: '人像', label: '人像摄影' },
  { value: '艺术画', label: '艺术画' },
  { value: '二次元', label: '二次元' },
  { value: '其他', label: '其他' }
]

// 添加拖拽状态
const isDragOver = ref(false)

// 拖拽事件处理函数
const handleDragOver = (e) => {
  e.preventDefault()
  isDragOver.value = true
}

const handleDragLeave = (e) => {
  // 只有当鼠标离开上传区域时才取消拖拽状态
  // 检查是否真的离开了上传区域，而不是进入了子元素
  const rect = e.currentTarget.getBoundingClientRect()
  const x = e.clientX
  const y = e.clientY
  
  // 判断鼠标是否在元素外
  if (x < rect.left || x >= rect.right || y < rect.top || y >= rect.bottom) {
    isDragOver.value = false
  }
}

const handleDrop = (e) => {
  e.preventDefault()
  isDragOver.value = false
  
  const files = e.dataTransfer.files
  if (files.length > 0) {
    // 使用现有的文件处理逻辑
    handleFileUpload(files)
  }
}

// 计算属性
const canSubmit = computed(() => {
  return uploadState.selectedFiles.length > 0 &&
    !uploadState.isSubmitting &&
    userId.value !== null
})

const statusMessage = computed(() => {
  return uploadState.status?.message || ''
})

const statusType = computed(() => {
  return uploadState.status?.type || ''
})

// 组件销毁标记
onUnmounted(() => {
  uploadState.isComponentMounted = false
  // 清理所有预览URL
  cleanupPreviewUrls()
})

// 安全更新状态（带进度）
const safeUpdateStatus = (updates) => {
  if (!uploadState.isComponentMounted || !uploadState.status) return
  Object.assign(uploadState.status, updates)
}

// 更新上传进度
const updateUploadProgress = (progressEvent) => {
  if (!progressEvent.lengthComputable || !uploadState.isComponentMounted) return

  const now = Date.now()
  const elapsedTime = (now - uploadState.startTime) / 1000 // 转换为秒

  uploadState.uploadedBytes = progressEvent.loaded
  uploadState.totalBytes = progressEvent.total
  uploadState.uploadProgress = (progressEvent.loaded / progressEvent.total) * 100

  // 计算上传速度（字节/秒）
  if (elapsedTime > 0) {
    uploadState.uploadSpeed = progressEvent.loaded / elapsedTime
  }

  // 创建详细的状态消息
  let statusMessage = '正在上传到服务器...'

  if (uploadState.uploadProgress > 0) {
    const progressText = Math.round(uploadState.uploadProgress)
    const speedText = formatSpeed(uploadState.uploadSpeed)
    const etaText = calculateETA()

    statusMessage = `上传中: ${progressText}% | 速度: ${speedText} | 剩余: ${etaText}`
  }

  safeUpdateStatus({
    message: statusMessage,
    progress: uploadState.uploadProgress
  })
}

// 格式化文件大小
const formatFileSize = (bytes) => {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
  return `${(bytes / (1024 * 1024 * 1024)).toFixed(1)} GB`
}

// 触发文件选择
const triggerFileInput = async () => {

  if (fileInputElement.value) {

    // 重置文件输入值，允许重新选择相同的文件
    fileInputElement.value.value = ''

    // 触发文件选择对话框
    fileInputElement.value.click()
  } else {
    console.error('无法找到文件输入元素')
    // 尝试创建临时的文件输入
    createTempFileInput()
  }
}

// 备用方法：创建临时文件输入
const createTempFileInput = () => {

  const tempInput = document.createElement('input')
  tempInput.type = 'file'
  tempInput.accept = 'image/*,video/*'
  tempInput.multiple = true
  tempInput.style.display = 'none'

  tempInput.onchange = (event) => {
    handleFileUpload(event)
    document.body.removeChild(tempInput)
  }

  document.body.appendChild(tempInput)
  tempInput.click()
}


// 处理文件上传
const handleFileUpload = (eventOrFiles) => {
  // 判断传入的是 event 还是 FileList
  let files
  if (eventOrFiles instanceof Event) {
    files = Array.from(eventOrFiles.target.files)
  } else if (eventOrFiles instanceof FileList) {
    files = Array.from(eventOrFiles)
  } else {
    console.error('Invalid argument type')
    return
  }
  
  if (!files.length) return

  // 安全更新状态
  safeUpdateStatus({
    type: '',
    message: ''
  })

  // 验证文件类型和大小
  const { validFiles, warningMessages } = validateFiles(files)

  // 显示警告信息（如果有）
  if (warningMessages.length > 0) {
    safeUpdateStatus({
      type: 'warning',
      message: warningMessages[0] // 显示第一个警告
    })
  }

  // 检查视频文件数量
  const videoFiles = validFiles.filter(f => f.type.startsWith('video/'))
  if (videoFiles.length > 0 && validFiles.length > 1) {
    safeUpdateStatus({
      type: 'warning',
      message: '视频文件只能单独上传，不能与其他文件一起上传'
    })
    return
  }

  // 检查总文件数量
  if (validFiles.length > 20) {
    safeUpdateStatus({
      type: 'warning',
      message: '一次最多只能上传20个文件,已自动选择前20个文件。'
    })
  }

  // 释放之前的URL对象
  cleanupPreviewUrls()

  // 处理有效文件
  uploadState.selectedFiles = validFiles
    .slice(0, 20) // 限制最多20个文件
    .map(file => {
      const mediaType = file.type.startsWith('image/') ? MediaType.IMAGE : MediaType.VIDEO
      return {
        raw: file,
        previewUrl: URL.createObjectURL(file),
        name: file.name,
        size: file.size,
        type: file.type,
        mediaType: mediaType
      }
    })
}
// 文件验证函数
const validateFiles = (files) => {
  const validFiles = []
  const warningMessages = []

  files.forEach(file => {
    const isImage = file.type.startsWith('image/')
    const isVideo = file.type.startsWith('video/')

    // 类型校验
    if (!isImage && !isVideo) {
      warningMessages.push(`文件 ${file.name} 不是图片或视频格式`)
      return
    }

    // 大小校验
    if (isImage && file.size > 30 * 1024 * 1024) {
      warningMessages.push(`图片 ${file.name} 超过30MB大小限制`)
      return
    }

    if (isVideo && file.size > 500 * 1024 * 1024) {
      warningMessages.push(`视频 ${file.name} 超过500MB大小限制`)
      return
    }

    validFiles.push(file)
  })

  return { validFiles, warningMessages }
}

// 清理预览URL
const cleanupPreviewUrls = () => {
  uploadState.selectedFiles.forEach(file => {
    if (file.previewUrl) {
      URL.revokeObjectURL(file.previewUrl)
    }
  })
}

// 移除文件
const removeFile = (index) => {
  if (uploadState.selectedFiles[index]?.previewUrl) {
    URL.revokeObjectURL(uploadState.selectedFiles[index].previewUrl)
  }
  uploadState.selectedFiles.splice(index, 1)
}

// 提交媒体文件
const submitMedia = async () => {
  if (!canSubmit.value || !uploadState.isComponentMounted) return

  uploadState.isSubmitting = true

  // 重置上传统计
  uploadState.uploadProgress = 0
  uploadState.uploadSpeed = 0
  uploadState.uploadedBytes = 0
  uploadState.totalBytes = 0
  uploadState.startTime = Date.now()

  const controller = new AbortController()
  let timeoutId

  try {
    const formData = new FormData()
    formData.append('userId', userId.value)

    // 计算总文件大小
    let totalSize = 0
    for (let i = 0; i < uploadState.selectedFiles.length; i++) {
      totalSize += uploadState.selectedFiles[i].size
    }
    uploadState.totalBytes = totalSize

    safeUpdateStatus({
      type: 'info',
      message: `准备上传 ${uploadState.selectedFiles.length} 个文件 (${formatFileSize(totalSize)})...`
    })

    // 处理每个文件
    for (let i = 0; i < uploadState.selectedFiles.length; i++) {
      if (!uploadState.isComponentMounted) return

      const fileData = uploadState.selectedFiles[i]

      safeUpdateStatus({
        message: `正在处理文件 ${i + 1}/${uploadState.selectedFiles.length} (${formatFileSize(fileData.size)})...`
      })

      let fileToUpload = fileData.raw

      //如果文件是图片且大于5MB，压缩图片
      if (fileData.mediaType === MediaType.IMAGE && fileData.size > 5 * 1024 * 1024) {
        try {
          const compressionPromise = compressImage(fileToUpload, { quality: 0.8 })
          fileToUpload = await Promise.race([
            compressionPromise,
            new Promise((_, reject) =>
              setTimeout(() => reject(new Error('压缩超时')), 10000)
            )
          ])
        } catch (compressError) {
          console.warn('图片压缩失败，使用原图:', compressError)
        }
      }

      formData.append('files', fileToUpload, fileData.name)
    }

    if (!uploadState.isComponentMounted) return

    formData.append('filename', uploadState.filename || '')
    formData.append('description', uploadState.description || '')
    if (uploadState.isAI) {
      formData.append('category', '(AI)' + (uploadState.category || ''))
    } else {
      formData.append('category', uploadState.category || '')
    }
    formData.append('isPublic', uploadState.isPublic ? 'true' : 'false')

    // 开始上传
    safeUpdateStatus({
      message: '正在上传到服务器。请耐心等待...'
    })

    timeoutId = setTimeout(() => controller.abort(), 300000)

    // 调用上传接口
    //如果是AI上传，则调用uploadMediaService，否则调用uploadSubmitMediaService
    const response = await (uploadState.isAI ? uploadMediaService : uploadSubmitMediaService)(formData, {
      signal: controller.signal,

      onUploadProgress: updateUploadProgress
    })

    clearTimeout(timeoutId)

    // 上传完成，显示100%
    safeUpdateStatus({
      type: 'success',
      message: `上传完成！`,
      progress: 100
    })

    setTimeout(() => {
      if (uploadState.isComponentMounted) {
        emit('upload-success', response.data || response.result)
        resetForm()
      }
    }, 1000)

  } catch (error) {
    clearTimeout(timeoutId)

    if (uploadState.isComponentMounted) {
      let errorMessage = '上传失败'
      if (error.name === 'AbortError') {
        errorMessage = '上传超时，请检查网络连接'
      } else if (error.message) {
        errorMessage = error.message
      }

      safeUpdateStatus({
        type: 'error',
        message: `${errorMessage} (进度: ${Math.round(uploadState.uploadProgress)}%)`
      })
    }
  } finally {
    if (uploadState.isComponentMounted) {
      uploadState.isSubmitting = false
    }
  }
}

// 计算剩余时间
const calculateETA = (remainingSeconds) => {
  if (!remainingSeconds || remainingSeconds <= 0) return '计算中...'

  if (remainingSeconds < 60) {
    return `${Math.ceil(remainingSeconds)}秒`
  } else if (remainingSeconds < 3600) {
    return `${Math.ceil(remainingSeconds / 60)}分钟`
  } else {
    return `${Math.ceil(remainingSeconds / 3600)}小时`
  }
}

// 格式化速度
const formatSpeed = (bytesPerSecond) => {
  if (!bytesPerSecond || bytesPerSecond <= 0) return '0 B/s'

  if (bytesPerSecond < 1024) {
    return `${Math.round(bytesPerSecond)} B/s`
  } else if (bytesPerSecond < 1024 * 1024) {
    return `${(bytesPerSecond / 1024).toFixed(1)} KB/s`
  } else {
    return `${(bytesPerSecond / (1024 * 1024)).toFixed(1)} MB/s`
  }
}

// 重置表单
const resetForm = () => {
  // 清理预览URL
  cleanupPreviewUrls()

  // 重置状态
  uploadState.selectedFiles = []
  uploadState.filename = ''
  uploadState.description = ''
  uploadState.category = ''
  uploadState.isPublic = true
  uploadState.isAI = false

  // 安全重置状态消息
  safeUpdateStatus({ type: '', message: '' })
}

// 监听用户信息变化
watch(
  () => userInfoStore.currentUser,
  (newUser) => {
    userId.value = newUser?.id || null
  },
  { immediate: true }
)

// 暴露方法给模板使用
defineExpose({
  triggerFileInput,
  handleFileUpload,
  removeFile,
  formatFileSize,
  submitMedia,
  resetForm
})
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.submit-container {
  border-radius: 12px;
  padding: 30px;
  width: 90%;
  max-width: 600px;
  max-height: 90vh;
  position: relative;
  overflow-y: auto;
  background-image: url(../assets/images/uploadBG.jpg);
  background-size: cover;
  opacity: 0.9;
  box-shadow: 10px 10pxpx 15px rgba(39, 238, 238, 0.7);
}

.close-modal {
  position: absolute;
  top: 15px;
  right: 15px;
  width: 30px;
  height: 30px;
  border: none;
  background: #f5f5f5;
  border-radius: 50%;
  font-size: 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}
.close-modal:hover {
  background: #17a1e0;
}

.upload-title {
  text-align: center;
  margin-bottom: 25px;
  color: rgba(30, 223, 149, 0.9);
  text-shadow: 5px 5px 10px rgba(227, 14, 235, 0.7);
  font-size: 22px;
  font-weight: bold;
}

.upload-area {
  border: 2px dashed #ddd;
  border-radius: 8px;
  padding: 40px 20px;
  text-align: center;
  cursor: pointer;
  transition: border-color 0.3s;
  margin-bottom: 20px;
}

.upload-area:hover {
  border-color: #4679ac;
}

.upload-prompt {
  color: #666;
}

.upload-icon {
  display: block;
  font-size: 48px;
  color: #409eff;
  margin-bottom: 15px;
}

.upload-instruction {
  margin: 0;
  font-size: 16px;
  color: #15dceb;
}

.upload-hint {
  color: #f56c6c;
  font-size: 15px;
  margin-top: 10px;
  font-weight: bold;
}

.preview-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 15px;
}

.preview-item {
  position: relative;
}

.preview-content {
  position: relative;
  width: 120px;
  height: 120px;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f5f5;
}

.preview-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.video-preview {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: #409eff;
  color: white;
}

.video-icon {
  font-size: 32px;
}

.video-label {
  font-size: 14px;
  margin-top: 5px;
}

.remove-btn {
  position: absolute;
  top: 5px;
  right: 5px;
  width: 24px;
  height: 24px;
  border: none;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  border-radius: 50%;
  cursor: pointer;
  font-size: 16px;
  line-height: 1;
}

.preview-index {
  position: absolute;
  top: 5px;
  left: 5px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
}

.file-info {
  margin-top: 8px;
  text-align: center;
}

.file-name {
  font-size: 12px;
  color: #14dd9a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.file-size {
  font-size: 11px;
  color: #c3e407;
}

.file-count {
  text-align: center;
  color: #e717a9;
  margin-bottom: 20px;
}

.form-group {
  margin-bottom: 15px;
}

.form-group-ai {
  margin-bottom: 10px;
}

.form-label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #f10eb9;
}

.form-input,
.form-textarea,
.form-select {
  width: 100%;
  padding: 8px 3px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  transition: border-color 0.3s;
}

.form-input:focus,
.form-textarea:focus,
.form-select:focus {
  border-color: #409eff;
  outline: none;
}

.form-textarea {
  resize: vertical;
}

.input-counter,
.textarea-counter {
  text-align: right;
  font-size: 13px;
  color: #e60840;
  margin-top: 4px;
}

.privacy-label {
  display: flex;
  align-items: left;
  cursor: pointer;
}

.privacy-checkbox {
  margin-right: 8px;
  width: 16px;
  height: 16px;
}

.privacy-text {
  font-weight: 500;
}

.privacy-hint {
  font-size: 12px;
  color: #666;
  margin-top: 5px;
  margin-left: 24px;
}

.AI-label {
  display: flex;
  align-items: right;
  cursor: pointer;
}

.AI-checkbox {
  margin-right: 8px;
  width: 16px;
  height: 16px;
}

.AI-text {
  font-weight: 500;
}

.AI-hint {
  font-size: 12px;
  color: #666;
  margin-top: 5px;
  margin-left: 24px;
}

.status-message {
  padding: 12px;
  text-align: center;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  font-size: 14px;
}

.status-message.info {
  background-color: #e8f4fd;
  color: #31708f;
  border: 1px solid #bce8f1;
}

.status-message.success {
  background-color: #dff0d8;
  color: #3c763d;
  border: 1px solid #d6e9c6;
}

.status-message.error {
  background-color: #f2dede;
  color: #a94442;
  border: 1px solid #ebccd1;
}

.status-message.warning {
  background-color: #fcf8e3;
  color: #8a6d3b;
  border: 1px solid #faebcc;
}

.submit-btn {
  width: 100%;
  padding: 14px;
  background-color: #409eff;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.3s;
}

.submit-btn:hover:not(:disabled) {
  background-color: #66b1ff;
}

.submit-btn:disabled {
  background-color: #c0c4cc;
  cursor: not-allowed;
}

.submit-btn.loading {
  background-color: #a0cfff;
  cursor: wait;
}

.loading-text {
  display: flex;
  align-items: center;
  justify-content: center;
}

.loading-dots {
  animation: loadingDots 1.5s infinite;
  font-weight: bold;
}

.status-content {
  position: relative;
}

.progress-bar-container {
  margin-top: 8px;
  height: 4px;
  background-color: rgba(255, 255, 255, 0.3);
  border-radius: 2px;
  overflow: hidden;
}

.progress-bar {
  height: 100%;
  background-color: currentColor;
  transition: width 0.3s ease;
}

/* 根据状态类型设置进度条颜色 */
.status-message.info .progress-bar {
  background-color: #31708f;
}

.status-message.success .progress-bar {
  background-color: #3c763d;
}

.status-message.error .progress-bar {
  background-color: #a94442;
}

.status-message.warning .progress-bar {
  background-color: #8a6d3b;
}

@keyframes loadingDots {

  0%,
  20% {
    content: '.';
  }

  40% {
    content: '..';
  }

  60%,
  100% {
    content: '...';
  }
}

/* 拖拽状态样式 */
.upload-area.drag-over {
  border-color: #409eff;
  background: rgba(64, 158, 255, 0.05);
  border-style: solid;
}

/* 拖拽覆盖层 */
.drag-overlay {
  position: absolute;
  top: 100px;
  left: 30px;
  right: 30px;
  bottom: 500px;
  background: rgba(64, 158, 255, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  z-index: 10;
}

.drag-message {
  text-align: center;
  color: white;
}

.drag-icon-large {
  font-size: 48px;
  display: block;
  margin-bottom: 16px;
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

.drag-message p {
  font-size: 18px;
  font-weight: bold;
  margin: 0;
}
</style>