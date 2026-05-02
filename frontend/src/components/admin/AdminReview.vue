<template>
  <div class="admin-review-container">
    <!-- 顶部操作栏：筛选、搜索、批量操作 -->
    <div class="top-toolbar">
      <!-- 左侧：类型筛选和搜索 -->
      <div class="left-tools">
        <!-- 类型筛选 -->
        <div class="filter-group">
          <select v-model="selectedType" class="type-select" @change="handleFilterChange">
            <option value="all">所有类型</option>
            <option value="image">仅图片</option>
            <option value="video">仅视频</option>
          </select>
        </div>

        <!-- 状态筛选 -->
        <div class="filter-group">
          <select v-model="filterStatus" class="status-select" @change="handleFilterChange">
            <option value="pending">待审核</option>
            <option value="all">所有状态</option>
            <option value="approved">已通过</option>
            <option value="rejected">已拒绝</option>
          </select>
        </div>

        <!-- 搜索框 -->
        <div class="search-group">
          <input v-model="searchKeyword" type="text" placeholder="搜索标题、描述、标签..." class="search-input"
            @keyup.enter="handleSearch" />
          <button class="search-btn" @click="handleSearch">
            <i class="iconfont icon-search">搜索</i>
          </button>
        </div>
      </div>

      <!-- 右侧：批量操作按钮 -->
      <div class="right-tools" v-if="selectedItems.length > 0">
        <button class="batch-btn approve-btn" @click="handleBatchApprove" :disabled="batchLoading">
          <i class="iconfont icon-check">通过审核</i>
        </button>
        <button class="batch-btn reject-btn" @click="handleBatchReject" :disabled="batchLoading">
          <i class="iconfont icon-close">拒绝通过</i>
        </button>
        <span class="selected-count">已选择 {{ selectedItems.length }} 项</span>
        <button class="clear-btn" @click="clearSelection">
          <i class="iconfont icon-clear">清空</i>
        </button>
      </div>
    </div>

    <!-- 错误提示 -->
    <div v-if="error" class="error-message">
      {{ error }}
    </div>

    <!-- 全选操作栏 -->
    <div class="select-all-bar" v-if="filteredMediaList.length > 0">
      <label class="select-all-checkbox">
        <input type="checkbox" :checked="isAllSelected" @change="toggleSelectAll" />
        <span>全选本页</span>
      </label>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-indicator">
      加载中...
    </div>

    <!-- 媒体列表 -->
    <div v-else class="media-list">
      <div v-for="media in filteredMediaList" :key="media.id" class="media-item">
        <!-- 复选框 -->
        <div class="media-checkbox">
          <input type="checkbox" :value="media.id" v-model="selectedItems" />
        </div>

        <!-- 媒体预览 -->
        <div class="media-preview" @click="openMediaViewer(media)">
          <div class="preview-container">
            <!-- 图片预览 -->
            <img v-if="media.mediaType === 'image'" :src="media.thumbnailPath || media.image" :alt="media.title"
              class="preview-image" @error="handleImageError(media)" />

            <!-- 视频预览 -->
            <div v-else-if="media.mediaType === 'video'" class="video-preview">
              <img :src="media.thumbnailPath || '/video-thumbnail.jpg'" :alt="media.title" class="preview-image"
                @error="handleImageError(media)" />
              <div class="video-overlay">
                <i class="iconfont icon-play">播放</i>
              </div>
              <div class="video-duration" v-if="media.duration">
                {{ formatDuration(media.duration) }}
              </div>
            </div>

            <!-- 媒体类型标签 -->
            <div class="media-type-badge" :class="media.mediaType">
              {{ media.mediaType === 'image' ? '图片' : '视频' }}
            </div>
          </div>
        </div>

        <!-- 媒体详情 -->
        <div class="media-details">
          <h3 class="media-title">主题：{{ media.title }}</h3>
          <button class="media-btn" @click="getUserInfo(media.userId)">获取用户名</button>

          <div class="media-meta">
            <div class="meta-item">
              <i class="iconfont icon-user">用户ID: </i>
              <span>{{ media.userId || '匿名用户' }}</span>
            </div>
            <div class="meta-item">
              <i class="iconfont icon-username">用户名: </i>
              <span>{{ username || '匿名用户' }}</span>
            </div>

            <div class="meta-item">
              <i class="iconfont icon-time">时间</i>
              <span>{{ formatDate(media.uploadTime) }}</span>
            </div>

            <div class="meta-item">
              <i class="iconfont icon-category">分类</i>
              <span>{{ media.category || '未分类' }}</span>
            </div>

            <div v-if="media.resolution" class="meta-item">
              <i class="iconfont icon-size">尺寸</i>
              <span>{{ media.resolution }}</span>
            </div>

            <div v-if="media.fileSize" class="meta-item">
              <i class="iconfont icon-file">大小</i>
              <span>{{ media.fileSize }}</span>
            </div>
          </div>

          <div class="media-description">
            <p class="desc-label">描述：</p>
            <p class="desc-content">{{ media.description || '无描述' }}</p>
          </div>

          <!-- 状态和操作 -->
          <div class="media-actions">
            <div class="status-badge" :class="getStatusClass(media.status)">
              {{ getStatusText(media.status) }}
            </div>

            <!-- 单个作品操作按钮 -->
            <div class="action-buttons" v-if="media.status === 'pending'">
              <button class="action-btn approve-btn" @click="handleSingleApprove(media)">
                <i class="iconfont icon-check">通过</i>
              </button>
              <button class="action-btn reject-btn" @click="handleSingleReject(media)">
                <i class="iconfont icon-close">拒绝</i>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 分页组件 -->
    <div v-if="totalPages > 1" class="pagination">
      <button class="page-btn" :disabled="currentPage === 1" @click="changePage(currentPage - 1)">
        上一页
      </button>

      <div class="page-numbers">
        <button v-for="page in visiblePages" :key="page" class="page-number" :class="{ active: page === currentPage }"
          @click="changePage(page)">
          {{ page }}
        </button>
        <span v-if="hasEllipsis" class="page-ellipsis">...</span>
      </div>

      <div class="page-jump">
        <span class="jump-text">跳转到</span>
        <input v-model.number="jumpPageInput" type="number" min="1" :max="totalPages" class="jump-input"
          @keyup.enter="jumpToPage" @blur="jumpToPage" />
        <span class="jump-text">页</span>
      </div>

      <button class="page-btn" :disabled="currentPage === totalPages" @click="changePage(currentPage + 1)">
        下一页
      </button>

      <div class="page-info">
        共 {{ totalItems }} 条，每页 {{ pageSize }} 条
      </div>
    </div>

    <!-- 空状态提示 -->
    <div v-if="!loading && filteredMediaList.length === 0" class="no-media">
      <p>{{ getEmptyMessage() }}</p>
    </div>

    <!-- 图片查看器 -->
    <Lightbox v-if="showLightbox" :images="filteredImageList" :current-index="currentIndex" @close="closeLightbox"
      @change-image="newIndex => currentIndex = newIndex" />

    <!-- 视频播放器模态框 -->
    <VideoModal v-if="showVideoPlayer && currentVideo" :show="showVideoPlayer" :video="currentVideo"
      @close="showVideoPlayer = false" />

    <!-- 拒绝原因选择对话框 -->
    <div v-if="showRejectDialog" class="dialog-overlay" @click.self="closeRejectDialog">
      <div class="reject-dialog">
        <h3>选择拒绝原因</h3>
        <div class="reject-reasons">
          <div class="reason-options">
            <div v-for="reason in rejectReasons" :key="reason.value" class="reason-option"
              :class="{ selected: selectedReason === reason.value }" @click="selectReason(reason.value)">
              <span class="reason-text">{{ reason.label }}</span>
              <span v-if="reason.description" class="reason-desc">{{ reason.description }}</span>
            </div>

            <!-- 自定义原因 -->
            <div class="custom-reason">
              <label for="custom-reason-input">自定义原因：</label>
              <input id="custom-reason-input" v-model="customReason" type="text" placeholder="请输入拒绝原因..."
                class="custom-reason-input" @focus="selectReason('custom')" />
            </div>
          </div>
        </div>

        <div class="dialog-actions">
          <button class="dialog-btn confirm-btn" :disabled="selectedReason === 'custom' && !customReason.trim()"
            @click="confirmReject">
            确认拒绝
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { getMediaListService, batchUpdateMediaStatusService } from '@/api/media'
import { getSubmitListService, batchUpdateSubmitStatusService } from '@/api/submit'
import { getUserInfoByIdService } from '@/api/user'
import Lightbox from '@/views/Lightbox.vue'
import VideoModal from '@/views/VideoModal.vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const username = ref('')
// 数据状态
const mediaList = ref([])
const loading = ref(false)
const error = ref(null)
const batchLoading = ref(false)

// 筛选状态
const filterStatus = ref('pending')
const selectedType = ref('all')
const searchKeyword = ref('')

// 分页状态
const currentPage = ref(1)
const pageSize = ref(20)
const totalItems = ref(0)
const totalPages = ref(0)
const jumpPageInput = ref('')

// 选择状态
const selectedItems = ref([])

// 查看器状态
const showLightbox = ref(false)
const showVideoPlayer = ref(false)
const currentIndex = ref(0)
const currentVideo = ref(null)

// 拒绝原因对话框状态
const showRejectDialog = ref(false)
const selectedReason = ref('')
const customReason = ref('')
const pendingRejectMedia = ref(null) // 保存待处理的媒体（单个或批量）

// 拒绝原因选项
const rejectReasons = [
  { value: 'content_inappropriate', label: '内容不合适', description: '包含不适当内容' },
  { value: 'copyright_issue', label: '版权问题', description: '可能侵犯他人版权' },
  { value: 'quality_poor', label: '质量不佳', description: '图片/视频质量过低' },
  { value: 'format_invalid', label: '格式无效', description: '文件格式不支持' },
  { value: 'violates_policy', label: '违反政策', description: '违反平台相关政策' },
  { value: 'duplicate_content', label: '重复内容', description: '与已有内容重复' },
  { value: 'custom', label: '其他原因' }
]

// 获取用户信息
const getUserInfo = async (id) => {
  try {
    const response = await getUserInfoByIdService(id)
    if (response.code === 0) {
      username.value = response.data.username || '未知用户'
    } else {
      username.value = '获取用户名失败'
    }
  } catch (error) {
    username.value = '获取用户名失败'
  }
}

// 计算属性：过滤后的媒体列表（当前页）
const filteredMediaList = computed(() => {
  return mediaList.value.filter(media => {
    // 状态筛选
    if (filterStatus.value !== 'all' && media.status !== filterStatus.value) {
      return false
    }

    // 类型筛选
    if (selectedType.value !== 'all' && media.mediaType !== selectedType.value) {
      return false
    }

    // 搜索筛选
    if (searchKeyword.value.trim()) {
      const keyword = searchKeyword.value.toLowerCase()
      return (
        (media.title && media.title.toLowerCase().includes(keyword)) ||
        (media.description && media.description.toLowerCase().includes(keyword)) ||
        (media.tags && media.tags.toLowerCase().includes(keyword))
      )
    }

    return true
  })
})

// 计算属性：过滤的图片列表（用于Lightbox）
const filteredImageList = computed(() => {
  return filteredMediaList.value.filter(media => media.mediaType === 'image')
})

// 计算属性：是否全选
const isAllSelected = computed(() => {
  return filteredMediaList.value.length > 0 &&
    filteredMediaList.value.every(media => selectedItems.value.includes(media.id))
})

// 计算属性：可见页码
const visiblePages = computed(() => {
  const maxVisible = 5
  const half = Math.floor(maxVisible / 2)
  let start = Math.max(1, currentPage.value - half)
  let end = Math.min(totalPages.value, start + maxVisible - 1)

  if (end - start + 1 < maxVisible) {
    start = Math.max(1, end - maxVisible + 1)
  }

  const pages = []
  for (let i = start; i <= end; i++) {
    pages.push(i)
  }
  return pages
})

// 计算属性：是否显示省略号
const hasEllipsis = computed(() => {
  return totalPages.value > 5 && visiblePages.value[visiblePages.value.length - 1] < totalPages.value
})

// 从API加载媒体数据
const loadMediaList = async () => {
  try {
    loading.value = true
    error.value = null

    const params = {
      mediaType: selectedType.value === 'all' ? null : selectedType.value,
      status: filterStatus.value === 'all' ? null : filterStatus.value,
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      orderBy: 'upload_time',
      order: 'DESC'
    }

    // 如果有搜索关键词
    if (searchKeyword.value.trim()) {
      params.keyword = searchKeyword.value.trim()
    }

    const response1 = await getMediaListService(params)
    const response2 = await getSubmitListService(params)

    if (response1.code === 0 || response2.code === 0) {
      // 转换数据格式
      mediaList.value = (response1.data?.items || []).concat(response2.data?.items || []).map(item => ({
        id: item.id,
        title: item.filename,
        image: item.filePath || item.url,
        thumbnailPath: item.thumbnailPath,
        description: item.description,
        category: item.category,
        tags: item.tags,
        status: item.status,
        mediaType: item.mediaType,
        fileSize: item.fileSize,
        resolution: item.width + '×' + item.height,
        height: item.height,
        width: item.width,
        duration: item.duration,
        uploadTime: item.uploadTime,
        userId: item.userId,
        viewCount: item.viewCount || 0,
        likeCount: item.likeCount || 0
      }))

      // 更新分页信息
      totalItems.value = (response1.data?.total || 0) + (response2.data?.total || 0)
      totalPages.value = Math.ceil(totalItems.value / pageSize.value)

      // 清空选择
      selectedItems.value = []
    } else {
      error.value = (response1.message || response2.message || '获取数据失败')
      mediaList.value = []
    }
  } catch (err) {
    console.error('加载媒体列表失败:', err)
    error.value = (err.response1?.data?.message || err.response2?.data?.message || '无法加载数据，请稍后重试')
    mediaList.value = []
  } finally {
    loading.value = false
  }
}

// 批量更新状态（统一处理类型转换）
// batchUpdateStatus 方法 - 修复版
const batchUpdateStatus = async (ids, status, tags) => {
  try {
    batchLoading.value = true
    
    console.log('批量更新状态请求:', { ids, status, tags })
    
    // 转换为数字数组
    const numericIds = Array.isArray(ids) ? ids.map(id => Number(id)) : [Number(ids)]
    
    // 使用API服务
    const response1 = await batchUpdateMediaStatusService(numericIds, status, tags)
    const response2 = await batchUpdateSubmitStatusService(numericIds, status, tags)
    
    console.log('批量更新响应:', response1, response2)
    
    // 检查响应
    if (response1.code === 0 || response2.code === 0) {
      const actionText = status === 'active' ? '通过' : '拒绝'
      const reasonText = tags ? `，原因：${tags}` : ''
      ElMessage.success(`成功${actionText} ${numericIds.length} 个作品${reasonText}`)
      
      // 重新加载数据
      await loadMediaList()
      
      // 清空选择
      selectedItems.value = []
      
      return true
    } else {
      // 业务错误
      console.error('批量更新业务错误:', response)
      const errorMsg = (response1.message || response2.message || '操作失败')
      ElMessage.error(errorMsg)
      return false
    }
  } catch (err) {
    console.error('批量更新捕获的完整错误对象:', err)
    
    // 根据错误类型处理
    if (err.code !== undefined && err.code !== 0) {
      // 业务错误（从拦截器的result.data.code来）
      const errorMsg = err.message || '操作失败'
      ElMessage.error(errorMsg)
      
      // 可以检查具体的错误代码
      if (err.code === 401) {
        // 未授权
        console.log('未授权错误，可能需要重新登录')
      }
    } else if (err.isHttpError) {
      // HTTP错误
      console.error('HTTP错误状态码:', err.status)
      
      if (err.response) {
        // 尝试从响应中获取错误信息
        const data1 = err.response1?.data
        const data2 = err.response2?.data
        const errorMsg = (data1?.msg || 
                        data1?.message || 
                        data1?.error || 
                        data2?.msg || 
                        data2?.message || 
                        data2?.error || 
                        `请求失败 (${err.status})`)
        ElMessage.error(errorMsg)
      } else {
        ElMessage.error(`HTTP错误: ${err.status}`)
      }
    } else if (err.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请检查网络连接')
    } else if (err.message === 'Network Error') {
      ElMessage.error('网络连接失败，请检查网络')
    } else {
      // 其他错误
      const errorMsg = err.message || '操作失败'
      ElMessage.error(errorMsg)
    }
    
    return false
  } finally {
    batchLoading.value = false
  }
}

// 这样调用方法可以简化为：
const handleBatchApprove = async () => {
  if (selectedItems.value.length === 0) {
    ElMessage.warning('请先选择要操作的作品')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要通过 ${selectedItems.value.length} 个作品的审核吗？`,
      '确认操作',
      {
        type: 'warning',
        confirmButtonText: '确定',
        cancelButtonText: '取消'
      }
    )

    // 直接传selectedItems.value，batchUpdateStatus会内部转换
    await batchUpdateStatus(selectedItems.value, 'active')

  } catch (err) {
    if (err !== 'cancel') {
      console.error('批量通过操作失败:', err)
    }
  }
}

// 批量拒绝
const handleBatchReject = () => {
  if (selectedItems.value.length === 0) {
    ElMessage.warning('请先选择要操作的作品')
    return
  }

  pendingRejectMedia.value = {
    type: 'batch',
    ids: selectedItems.value // 保存原始值，确认时再转换
  }

  openRejectDialog()
}

// 处理单个通过
const handleSingleApprove = async (media) => {
  try {
    await ElMessageBox.confirm(
      `确定要通过作品 "${media.title}" 的审核吗？`,
      '确认操作',
      { type: 'warning' }
    )

    // 单个id也要转成数字数组
    await batchUpdateStatus([Number(media.id)], 'active')
  } catch (err) {
    if (err !== 'cancel') {
      console.error('单个通过操作失败:', err)
    }
  }
}

// 处理单个拒绝
const handleSingleReject = (media) => {
  // 保存待处理的媒体（id转为数字）
  pendingRejectMedia.value = {
    type: 'single',
    media: media,
    ids: [Number(media.id)]
  }

  // 打开拒绝原因选择对话框
  openRejectDialog()
}

// 确认拒绝时修改
const confirmReject = async () => {
  if (!pendingRejectMedia.value) return
  
  try {
    // 获取拒绝原因文本
    let rejectReason = ''
    if (selectedReason.value === 'custom') {
      if (!customReason.value.trim()) {
        ElMessage.warning('请输入自定义拒绝原因')
        return
      }
      rejectReason = customReason.value.trim()
    } else if (selectedReason.value) {
      const reason = rejectReasons.find(r => r.value === selectedReason.value)
      rejectReason = reason ? reason.label : ''
    }
    
    // 确认提示
    const confirmMessage = pendingRejectMedia.value.type === 'single'
      ? `确定要拒绝作品 "${pendingRejectMedia.value.media.title}" 吗？` + 
        (rejectReason ? `\n原因：${rejectReason}` : '')
      : `确定要拒绝 ${pendingRejectMedia.value.ids.length} 个作品吗？` + 
        (rejectReason ? `\n原因：${rejectReason}` : '')
    
    await ElMessageBox.confirm(
      confirmMessage,
      '确认拒绝',
      { type: 'warning' }
    )
    
    // batchUpdateStatus内部会处理类型转换
    await batchUpdateStatus(pendingRejectMedia.value.ids, 'hidden', rejectReason)
    
    // 关闭对话框
    closeRejectDialog()
  } catch (err) {
    if (err !== 'cancel') {
      console.error('拒绝操作失败:', err)
    }
  }
}


// 打开拒绝原因对话框
const openRejectDialog = () => {
  showRejectDialog.value = true
  selectedReason.value = ''
  customReason.value = ''
}

// 关闭拒绝原因对话框
const closeRejectDialog = () => {
  showRejectDialog.value = false
  pendingRejectMedia.value = null
}

// 选择拒绝原因
const selectReason = (reason) => {
  selectedReason.value = reason
}

// 打开媒体查看器
const openMediaViewer = (media) => {
  if (media.mediaType === 'video') {
    currentVideo.value = {
      ...media,
      filePath: media.filePath || media.image,
      thumbnailPath: media.thumbnailPath || media.image
    }
    showVideoPlayer.value = true
  } else {
    const index = filteredImageList.value.findIndex(item => item.id === media.id)
    if (index !== -1) {
      currentIndex.value = index
      showLightbox.value = true
    }
  }
}

// 关闭Lightbox
const closeLightbox = () => {
  showLightbox.value = false
}

// 处理筛选变化
const handleFilterChange = () => {
  currentPage.value = 1
  loadMediaList()
}

// 处理搜索
const handleSearch = () => {
  currentPage.value = 1
  loadMediaList()
}

// 切换分页
const changePage = (page) => {
  if (page < 1 || page > totalPages.value || page === currentPage.value) return

  currentPage.value = page
  loadMediaList()

  // 滚动到顶部
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// 跳转到指定页
const jumpToPage = () => {
  if (!jumpPageInput.value || jumpPageInput.value === '') return

  let targetPage = parseInt(jumpPageInput.value)

  if (isNaN(targetPage)) {
    ElMessage.warning('请输入有效的页码')
    jumpPageInput.value = ''
    return
  }

  if (targetPage < 1) {
    targetPage = 1
  } else if (targetPage > totalPages.value) {
    targetPage = totalPages.value
  }

  if (targetPage === currentPage.value) {
    jumpPageInput.value = ''
    return
  }

  changePage(targetPage)
  jumpPageInput.value = ''
}

// 切换全选
const toggleSelectAll = () => {
  if (isAllSelected.value) {
    // 取消全选
    filteredMediaList.value.forEach(media => {
      const index = selectedItems.value.indexOf(media.id)
      if (index > -1) {
        selectedItems.value.splice(index, 1)
      }
    })
  } else {
    // 全选
    filteredMediaList.value.forEach(media => {
      if (!selectedItems.value.includes(media.id)) {
        selectedItems.value.push(media.id)
      }
    })
  }
}

// 清空选择
const clearSelection = () => {
  selectedItems.value = []
}

// 处理图片加载错误
const handleImageError = (media) => {
  console.error(`加载图片失败: ${media.title}`)
  media.image = '/placeholder-image.jpg'
  media.thumbnailPath = '/placeholder-image.jpg'
}

// 格式化日期
const formatDate = (timestamp) => {
  if (!timestamp) return '未知时间'
  return new Date(timestamp).toLocaleString('zh-CN')
}

// 格式化视频时长
const formatDuration = (seconds) => {
  if (!seconds) return ''
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins}:${secs.toString().padStart(2, '0')}`
}

// 获取状态文本
const getStatusText = (status) => {
  const statusMap = {
    pending: '待审核',
    approved: '已通过',
    rejected: '已拒绝',
    active: '已激活',
    hidden: '已隐藏'
  }
  return statusMap[status] || status
}

// 获取状态类名
const getStatusClass = (status) => {
  const classMap = {
    pending: 'status-pending',
    approved: 'status-approved',
    rejected: 'status-rejected',
    active: 'status-active',
    hidden: 'status-hidden'
  }
  return classMap[status] || ''
}

// 获取空状态消息
const getEmptyMessage = () => {
  if (searchKeyword.value.trim()) {
    return '没有找到符合条件的作品'
  }
  if (filterStatus.value === 'pending') {
    return '暂无待审核的作品'
  }
  if (selectedType.value !== 'all') {
    return `暂无${selectedType.value === 'image' ? '图片' : '视频'}作品`
  }
  return '暂无作品数据'
}

// 监听当前页码变化
watch(currentPage, () => {
  jumpPageInput.value = ''
})

// 初始化加载
onMounted(() => {
  loadMediaList()
})
</script>

<style scoped>
/* 基础容器样式 */
.admin-review-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;
}

h2 {
  text-align: center;
  color: #333;
  margin-bottom: 30px;
  padding-bottom: 15px;
  border-bottom: 2px solid #409eff;
}

/* 顶部工具栏样式 */
.top-toolbar {
  background: white;
  padding: 15px 20px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  margin-bottom: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 15px;
}

.left-tools {
  display: flex;
  align-items: center;
  gap: 15px;
  flex-wrap: wrap;
}

.filter-group {
  position: relative;
}

.type-select,
.status-select {
  padding: 8px 30px 8px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: white;
  color: #606266;
  font-size: 14px;
  cursor: pointer;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%2360666c' d='M6 8L2 4h8z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 10px center;
  min-width: 100px;
}

.type-select:focus,
.status-select:focus {
  outline: none;
  border-color: #409eff;
}

.search-group {
  display: flex;
  gap: 8px;
}

.search-input {
  padding: 8px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
  min-width: 200px;
}

.search-input:focus {
  outline: none;
  border-color: #409eff;
}

.search-btn {
  padding: 8px 16px;
  background: #409eff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.search-btn:hover {
  background: #66b1ff;
}

/* 右侧工具样式 */
.right-tools {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.batch-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 5px;
}

.batch-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.approve-btn {
  background: #67c23a;
  color: white;
}

.approve-btn:hover:not(:disabled) {
  background: #85ce61;
}

.reject-btn {
  background: #f56c6c;
  color: white;
}

.reject-btn:hover:not(:disabled) {
  background: #f78989;
}

.selected-count {
  color: #409eff;
  font-size: 14px;
  font-weight: 500;
}

.clear-btn {
  padding: 6px 12px;
  background: #909399;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
}

.clear-btn:hover {
  background: #a6a9ad;
}

/* 全选栏样式 */
.select-all-bar {
  background: #e6f7ff;
  padding: 10px 20px;
  margin-bottom: 15px;
  border-radius: 4px;
  border-left: 4px solid #409eff;
}

.select-all-checkbox {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 14px;
  color: #333;
}

.select-all-checkbox input[type="checkbox"] {
  width: 16px;
  height: 16px;
}

/* 媒体列表样式 */
.media-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(450px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

.media-item {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  transition: transform 0.2s, box-shadow 0.2s;
  display: flex;
}

.media-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px 0 rgba(0, 0, 0, 0.15);
}

/* 复选框样式 */
.media-checkbox {
  padding: 15px;
  display: flex;
  align-items: flex-start;
}

.media-checkbox input[type="checkbox"] {
  width: 18px;
  height: 18px;
  cursor: pointer;
}

/* 媒体预览样式 */
.media-preview {
  padding: 15px;
  width: 180px;
  cursor: pointer;
}

.preview-container {
  position: relative;
  width: 150px;
  height: 150px;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #ebeef5;
}

.preview-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.video-preview {
  position: relative;
  width: 100%;
  height: 100%;
}

.video-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
}

.video-overlay i {
  color: white;
  font-size: 24px;
}

.video-duration {
  position: absolute;
  bottom: 5px;
  right: 5px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 12px;
}

.media-type-badge {
  position: absolute;
  top: 5px;
  left: 5px;
  padding: 2px 8px;
  border-radius: 3px;
  font-size: 12px;
  color: white;
}

.media-type-badge.image {
  background: #409eff;
}

.media-type-badge.video {
  background: #e6a23c;
}

/* 媒体详情样式 */
.media-details {
  flex: 1;
  padding: 15px 15px 15px 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.media-title {
  margin: 0;
  font-size: 16px;
  color: #303133;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.media-btn {
  padding: 6px 12px;
  background: #409eff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
}

.media-meta {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
  font-size: 12px;
  color: #606266;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 5px;
}

.meta-item i {
  color: #909399;
}

.media-description {
  margin-top: 5px;
}

.desc-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 2px;
}

.desc-content {
  font-size: 13px;
  color: #606266;
  line-height: 1.4;
  margin: 0;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.media-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.tag {
  background: #f0f9ff;
  color: #409eff;
  padding: 2px 8px;
  border-radius: 3px;
  font-size: 12px;
  border: 1px solid #d9ecff;
}

/* 媒体操作样式 */
.media-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: auto;
}

.status-badge {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.status-pending {
  background: #fdf6ec;
  color: #e6a23c;
  border: 1px solid #faecd8;
}

.status-approved {
  background: #f0f9eb;
  color: #67c23a;
  border: 1px solid #e1f3d8;
}

.status-rejected {
  background: #fef0f0;
  color: #f56c6c;
  border: 1px solid #fde2e2;
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.action-btn {
  padding: 6px 12px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 分页样式 */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
  margin: 30px 0;
  padding: 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  flex-wrap: wrap;
}

.page-btn {
  padding: 8px 16px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: white;
  color: #606266;
  cursor: pointer;
  font-size: 14px;
  min-width: 80px;
}

.page-btn:hover:not(:disabled) {
  color: #409eff;
  border-color: #c6e2ff;
}

.page-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.page-numbers {
  display: flex;
  gap: 5px;
}

.page-number {
  min-width: 36px;
  height: 36px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: white;
  color: #606266;
  cursor: pointer;
  font-size: 14px;
}

.page-number:hover {
  color: #409eff;
  border-color: #c6e2ff;
}

.page-number.active {
  background: #409eff;
  color: white;
  border-color: #409eff;
}

.page-ellipsis {
  color: #c0c4cc;
  padding: 0 10px;
}

.page-jump {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 15px;
}

.jump-text {
  color: #606266;
  font-size: 14px;
}

.jump-input {
  width: 60px;
  padding: 6px 10px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
  text-align: center;
}

.jump-input:focus {
  outline: none;
  border-color: #409eff;
}

.jump-input::-webkit-inner-spin-button,
.jump-input::-webkit-outer-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

.page-info {
  color: #909399;
  font-size: 14px;
  margin-left: 15px;
}

/* 状态提示样式 */
.error-message {
  background: #fef0f0;
  color: #f56c6c;
  padding: 12px;
  border-radius: 4px;
  margin-bottom: 20px;
  border-left: 4px solid #f56c6c;
}

.loading-indicator {
  text-align: center;
  padding: 40px;
  color: #909399;
  font-size: 16px;
}

.no-media {
  text-align: center;
  padding: 60px 20px;
  color: #909399;
  font-size: 16px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .media-list {
    grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  }
}

@media (max-width: 992px) {
  .media-list {
    grid-template-columns: 1fr;
  }

  .top-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .left-tools,
  .right-tools {
    justify-content: center;
  }
}

@media (max-width: 768px) {
  .media-item {
    flex-direction: column;
  }

  .media-preview {
    width: 100%;
    display: flex;
    justify-content: center;
  }

  .preview-container {
    width: 200px;
    height: 200px;
  }

  .media-details {
    padding: 15px;
  }

  .pagination {
    flex-direction: column;
    gap: 15px;
  }

  .page-jump {
    order: 3;
  }
}

@media (max-width: 576px) {
  .admin-review-container {
    padding: 10px;
  }

  .media-list {
    gap: 15px;
  }

  .left-tools {
    flex-direction: column;
    align-items: stretch;
  }

  .search-input {
    min-width: 0;
  }

  .right-tools {
    justify-content: center;
    flex-wrap: wrap;
  }

  .media-meta {
    grid-template-columns: 1fr;
  }
}

/* 新增：拒绝原因对话框样式 */
.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.reject-dialog {
  background: white;
  border-radius: 8px;
  width: 500px;
  max-width: 90%;
  max-height: 80vh;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}

.reject-dialog h3 {
  margin: 0;
  padding: 20px;
  background: #f56c6c;
  color: white;
  text-align: center;
  font-size: 18px;
}

.reject-reasons {
  padding: 20px;
  max-height: 400px;
  overflow-y: auto;
}

.reason-options {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.reason-option {
  padding: 12px 15px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.reason-option:hover {
  border-color: #f56c6c;
  background: #fef0f0;
}

.reason-option.selected {
  border-color: #f56c6c;
  background: #fef0f0;
  box-shadow: 0 0 0 1px #f56c6c;
}

.reason-text {
  display: block;
  font-weight: 500;
  color: #303133;
  margin-bottom: 3px;
}

.reason-desc {
  display: block;
  font-size: 12px;
  color: #909399;
}

.custom-reason {
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px solid #ebeef5;
}

.custom-reason label {
  display: block;
  margin-bottom: 8px;
  color: #606266;
  font-weight: 500;
}

.custom-reason-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
}

.custom-reason-input:focus {
  outline: none;
  border-color: #f56c6c;
  box-shadow: 0 0 0 2px rgba(245, 108, 108, 0.1);
}

.dialog-actions {
  padding: 20px;
  border-top: 1px solid #ebeef5;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.dialog-btn {
  padding: 8px 20px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  min-width: 80px;
}

.cancel-btn {
  background: #f4f4f5;
  color: #909399;
}

.cancel-btn:hover {
  background: #e9e9eb;
  color: #606266;
}

.confirm-btn {
  background: #f56c6c;
  color: white;
}

.confirm-btn:hover:not(:disabled) {
  background: #f78989;
}

.confirm-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 响应式调整 */
@media (max-width: 576px) {
  .reject-dialog {
    width: 95%;
  }

  .dialog-actions {
    flex-direction: column;
  }

  .dialog-btn {
    width: 100%;
  }
}
</style>