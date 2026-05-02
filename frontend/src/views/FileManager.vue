<!-- FileManager.vue -->
<template>
  <div class="file-manager-dialog" v-if="visible" @click.self="close">
    <div class="file-manager-content">
      <div class="file-manager-header">
        <h3>聊天文件管理</h3>
        <button @click="close" class="close-btn">×</button>
      </div>

      <div class="file-manager-body">
        <!-- 文件类型筛选 -->
        <div class="file-filter">
          <button v-for="type in fileTypes" :key="type.value"
            :class="['filter-btn', { 'active': activeType === type.value }]" @click="changeType(type.value)">
            {{ type.label }}
          </button>
        </div>

        <!-- 文件列表 -->
        <div class="file-list">
          <div v-if="loading" class="loading">加载中...</div>
          <div v-else-if="files.length === 0" class="empty">暂无文件</div>

          <div v-for="file in files" :key="file.id" class="file-item">
            <div class="file-icon" @click="previewFile(file)">
              <img v-if="file.fileType === 'image'" :src="file.thumbnailUrl || file.fileUrl" />
              <div v-else class="file-type-icon">{{ getFileTypeIcon(file.fileType) }}</div>
            </div>

            <div class="file-info">
              <div class="file-name" @click="previewFile(file)">
                {{ file.fileName }}
              </div>
              <div class="file-meta">
                <span class="file-size">{{ file.fileSize }}</span>
                <span class="file-time">{{ formatTime(file.createTime) }}</span>
                <span class="file-uploader" :class="{ 'self': file.uploaderId === currentUserId }">
                  {{ file.uploaderId === currentUserId ? '我' : (file.uploaderNickname || '对方') }}
                </span>
              </div>
            </div>

            <div class="file-actions">
              <button @click="downloadFile(file)" class="action-btn download" title="下载">
                下载
              </button>
              <button v-if="file.uploaderId === currentUserId" @click="deleteFile(file)" class="action-btn delete"
                title="删除">
                删除
              </button>
            </div>
          </div>
        </div>

        <!-- 分页 -->
        <div v-if="total > pageSize" class="file-pagination">
          <button :disabled="pageNum === 1" @click="prevPage" class="page-btn">
            上一页
          </button>
          <span class="page-info">{{ pageNum }} / {{ totalPages }}</span>
          <button :disabled="pageNum >= totalPages" @click="nextPage" class="page-btn">
            下一页
          </button>
        </div>
      </div>
    </div>

    <!-- 文件预览 -->
    <div v-if="previewFile" class="file-preview" @click.self="closePreview">
      <div class="preview-content">
        <button @click="closePreview" class="preview-close">×</button>

        <div v-if="previewFile.fileType === 'image'" class="image-preview">
          <img :src="previewFile.fileUrl" />
        </div>

        <div v-else class="file-info-preview">
          <div class="file-icon-large">{{ getFileTypeIcon(previewFile.fileType) }}</div>
          <div class="file-details">
            <h4>{{ previewFile.fileName }}</h4>
            <p>大小: {{ previewFile.fileSize }}</p>
            <p>上传者: {{ previewFile.uploaderNickname }}</p>
            <p>上传时间: {{ formatTime(previewFile.createTime) }}</p>
            <button @click="downloadFile(previewFile)" class="download-btn">下载文件</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useUserInfoStore } from '@/stores/userInfo'  // 添加这行
import * as socialApi from '@/api/socialApi'
import { ElMessage, ElMessageBox } from 'element-plus'  // 添加这行

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  friendId: {
    type: Number,
    required: true
  }
})

const emit = defineEmits(['update:visible', 'close'])

// 获取用户store
const userStore = useUserInfoStore()

// 状态
const files = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const activeType = ref('all')
const previewFileData = ref(null)  // 改名避免与函数重名

// 文件类型选项
const fileTypes = [
  { value: 'all', label: '全部' },
  { value: 'image', label: '图片' },
  { value: 'video', label: '视频' },
  { value: 'audio', label: '音频' },
  { value: 'document', label: '文档' },
  { value: 'other', label: '其他' }
]

// 计算属性
const totalPages = computed(() => Math.ceil(total.value / pageSize.value))
const currentUserId = computed(() => userStore.currentUser?.id)

// 加载文件
const loadFiles = async () => {
  if (!props.friendId) {
    console.log('friendId is required')
    return
  }

  loading.value = true
  try {
    // 后端接口只需要 fileType、pageNum、pageSize 参数
    const res = await socialApi.getChatFiles({
      fileType: activeType.value === 'all' ? null : activeType.value,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })

    if (res.code === 0) {
      // 后端返回的是 List<ChatFileDTO>
      files.value = res.data || []

      // 前端过滤：只显示和当前 friendId 相关的文件
      // 可以是上传者是自己且接收者是 friendId
      // 或者是上传者是 friendId 且接收者是自己
      // fileList = fileList.filter(file => 
      //   (file.uploaderId === currentUserId.value && file.receiverId === props.friendId) ||
      //   (file.uploaderId === props.friendId && file.receiverId === currentUserId.value)
      // )

      
      total.value = fileList.length  // 使用过滤后的长度
    } else {
      throw new Error(res.msg)
    }
  } catch (error) {
    console.error('加载文件失败:', error)
    ElMessage.error('加载文件失败: ' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

const changeType = (type) => {
  activeType.value = type
  pageNum.value = 1
  loadFiles()
}

const prevPage = () => {
  if (pageNum.value > 1) {
    pageNum.value--
    loadFiles()
  }
}

const nextPage = () => {
  if (pageNum.value < totalPages.value) {
    pageNum.value++
    loadFiles()
  }
}

const previewFile = (file) => {
  previewFileData.value = file
}

const closePreview = () => {
  previewFileData.value = null
  emit('close')
}

const downloadFile = async (file) => {
  try {
    // 创建隐藏的a标签触发下载
    const link = document.createElement('a')
    link.href = file.fileUrl
    link.download = file.fileName || 'download'
    link.style.display = 'none'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  } catch (error) {
    console.error('下载文件失败:', error)
    ElMessage.error('下载文件失败')
  }
}

const deleteFile = async (file) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除文件 "${file.fileName}" 吗？`,
      '删除文件',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const res = await socialApi.deleteChatFile(file.id)
    if (res.code === 0) {
      ElMessage.success('文件删除成功')
      loadFiles()
    } else {
      throw new Error(res.msg)
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除文件失败:', error)
      ElMessage.error('删除文件失败: ' + (error.message || '未知错误'))
    }
  }
}

const getFileTypeIcon = (fileType) => {
  const icons = {
    image: '🖼️',
    video: '🎬',
    audio: '🎵',
    document: '📄',
    text: '📝',
    pdf: '📕',
    other: '📎'
  }
  return icons[fileType] || icons.other
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleDateString() + ' ' + date.toLocaleTimeString()
}

const close = () => {
  emit('update:visible', false)
  emit('close')
}

// 监听visible变化
watch(() => props.visible, (newVal) => {
  if (newVal) {
    pageNum.value = 1
    activeType.value = 'all'
    loadFiles()
  }
})

// 监听friendId变化
watch(() => props.friendId, () => {
  if (props.visible) {
    loadFiles()
  }
})
</script>

<style scoped>
.file-manager-dialog {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.file-manager-content {
  width: 800px;
  height: 500px;
  background: white;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.file-manager-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #e0e0e0;
}

.file-manager-header h3 {
  margin: 0;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #999;
}

.file-manager-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.file-filter {
  display: flex;
  gap: 10px;
  padding: 15px;
  border-bottom: 1px solid #f0f0f0;
}

.filter-btn {
  padding: 6px 12px;
  border: 1px solid #e0e0e0;
  background: white;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  color: #666;
}

.filter-btn.active {
  background: #1890ff;
  color: white;
  border-color: #1890ff;
}

.file-list {
  flex: 1;
  overflow-y: auto;
  padding: 15px;
}

.file-item {
  display: flex;
  align-items: center;
  padding: 10px;
  border-bottom: 1px solid #f5f5f5;
  transition: background 0.2s;
}

.file-item:hover {
  background: #f9f9f9;
}

.file-icon {
  width: 60px;
  height: 60px;
  margin-right: 15px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 6px;
  background: #f5f5f5;
}

.file-icon img {
  max-width: 100%;
  max-height: 100%;
  object-fit: cover;
}

.file-type-icon {
  font-size: 24px;
}

.file-info {
  flex: 1;
  min-width: 0;
}

.file-name {
  font-weight: 500;
  margin-bottom: 5px;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-name:hover {
  color: #1890ff;
}

.file-meta {
  font-size: 12px;
  color: #999;
  display: flex;
  gap: 15px;
}

.file-actions {
  display: flex;
  gap: 10px;
}

.action-btn {
  padding: 4px 12px;
  border: 1px solid #e0e0e0;
  background: white;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
}

.action-btn.download {
  color: #1890ff;
  border-color: #1890ff;
}

.action-btn.delete {
  color: #ff4d4f;
  border-color: #ff4d4f;
}

.file-pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 20px;
  padding: 15px;
  border-top: 1px solid #f0f0f0;
}

.page-btn {
  padding: 6px 16px;
  border: 1px solid #d9d9d9;
  background: white;
  border-radius: 4px;
  cursor: pointer;
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 文件预览样式 */
.file-preview {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2001;
}

.preview-content {
  position: relative;
  background: white;
  border-radius: 12px;
  max-width: 90%;
  max-height: 90%;
  overflow: auto;
}

.preview-close {
  position: absolute;
  top: 10px;
  right: 10px;
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: white;
  background: rgba(0, 0, 0, 0.5);
  width: 40px;
  height: 40px;
  border-radius: 50%;
  z-index: 1;
}

.image-preview img {
  max-width: 100%;
  max-height: 80vh;
  display: block;
}

.file-info-preview {
  padding: 40px;
  text-align: center;
}

.file-icon-large {
  font-size: 60px;
  margin-bottom: 20px;
}

.download-btn {
  margin-top: 20px;
  padding: 10px 24px;
  background: #1890ff;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}

.download-btn:hover {
  background: #40a9ff;
}

.loading,
.empty {
  text-align: center;
  padding: 40px;
  color: #999;
}

.file-uploader.self {
  color: #1890ff;
  font-weight: 500;
}
</style>