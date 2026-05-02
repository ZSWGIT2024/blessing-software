<template>
  <div class="gallery-container">
    <!-- 顶部装饰标题 -->
    <div class="gallery-header">
      <div class="header-content">
        <h1 class="gallery-title">
          <span class="title-decoration">✦✦✦</span>
          <span class="title-text">AI画廊</span>
          <span class="title-decoration">✦✦✦</span>
        </h1>
        <p class="gallery-subtitle">探索AI创造的视觉奇迹</p>
      </div>
    </div>
    
    <!-- 顶部操作栏：搜索、筛选、排序、上传 -->
    <div class="top-options">
      <!-- 左侧：搜索框 -->
      <div class="search-container">
        <div class="search-input-wrapper">
          <input 
            v-model="searchKeyword"
            type="text" 
            placeholder="搜索AI作品标题、描述或关键词..." 
            class="search-input"
            @keyup.enter="handleSearch"
          />
          <button class="search-button" @click="handleSearch">
            <span class="iconfont icon-search">搜索</span>
          </button>
        </div>
      </div>
      
      <!-- 右侧：类型筛选、排序、上传按钮 -->
      <div class="right-actions">
        <!-- AI类型筛选下拉框 -->
        <div class="filter-options">
          <select v-model="selectedType" class="type-select" @change="handleTypeChange">
            <option value="all">所有类型</option>
            <option value="image">仅图片</option>
            <option value="video">仅视频</option>
          </select>
        </div>
        
        <!-- 排序选项 -->
        <div class="sort-options">
          <button class="sort-btn" :class="{ active: sortBy === 'latest' }" @click="changeSort('latest')">
            最新
          </button>
          <button class="sort-btn" :class="{ active: sortBy === 'popular' }" @click="changeSort('popular')">
            人气
          </button>
        </div>
        
        <!-- AI创作按钮 -->
        <button class="ai-generate-button" @click="uploadAIArt">
          <span class="iconfont icon-ai">上传AI作品</span>
        </button>
      </div>
    </div>

    <!-- 返回按钮 -->
    <button class="back-button" @click="goBack">
      <i class="arrow-icon">←</i> 返回
    </button>

    <!-- 画廊主体 - 使用瀑布流布局，更加美观 -->
    <div class="gallery-hall">
      <!-- 添加分页信息和加载状态 -->
      <div v-if="!isLoading && mediaList.length > 0" class="page-info">
        显示 {{ (currentPage - 1) * pageSize + 1 }}-{{ Math.min(currentPage * pageSize, totalItems) }} 
        条，共 {{ totalItems }} 条AI作品
      </div>
      
      <!-- 瀑布流布局 -->
      <div class="masonry-grid">
        <div class="masonry-column" v-for="(column, colIndex) in masonryColumns" :key="colIndex">
          <div 
            v-for="media in column" 
            :key="media.id" 
            class="masonry-item"
            @click="openLightbox(media)"
          >
            <ArtFrame 
              :artwork="media"
            />
          </div>
        </div>
      </div>
      
      <!-- 分页组件 -->
      <div v-if="totalPages > 1" class="pagination">
        <button 
          class="page-btn" 
          :disabled="currentPage === 1" 
          @click="changePage(currentPage - 1)"
        >
          上一页
        </button>
        
        <div class="page-numbers">
          <button 
            v-for="page in visiblePages" 
            :key="page"
            class="page-number" 
            :class="{ active: page === currentPage }"
            @click="changePage(page)"
          >
            {{ page }}
          </button>
          <span v-if="hasEllipsis" class="page-ellipsis">...</span>
        </div>

        <!-- 跳转页面功能 -->
        <div class="page-jump">
          <span class="jump-text">跳转到</span>
          <input 
            v-model.number="jumpPageInput"
            type="number"
            min="1"
            :max="totalPages"
            class="jump-input"
            @keyup.enter="jumpToPage"
            @blur="jumpToPage"
          />
          <span class="jump-text">页</span>
        </div>
        
        <button 
          class="page-btn" 
          :disabled="currentPage === totalPages" 
          @click="changePage(currentPage + 1)"
        >
          下一页
        </button>
      </div>
    </div>

    <!-- 图片查看器 -->
    <Lightbox 
      v-if="showLightbox" 
      :images="filteredImageList" 
      :current-index="currentIndex" 
      @close="closeLightbox"
      @change-image="newIndex => currentIndex = newIndex" 
    />

    <!-- 视频播放器模态框 -->
    <VideoModal 
      v-if="showVideoPlayer && currentVideo" 
      :show="showVideoPlayer" 
      :video="currentVideo"
      @close="showVideoPlayer = false" 
    />

 <!-- 上传组件 -->
    <UserUploads 
      v-if="showUploadModal" 
      @close="showUploadModal = false" 
      @upload-success="handleUploadSuccess" 
    />
    
    <!-- 加载状态 -->
    <div v-if="isLoading" class="loading">
      <div class="loading-spinner"></div>
      <p>正在加载AI作品...</p>
    </div>

    <!-- 错误提示 -->
    <div v-if="errorMessage" class="error-message">
      {{ errorMessage }}
    </div>

    <!-- 空状态提示 -->
    <div v-if="!isLoading && mediaList.length === 0 && !isSearching" class="empty-state">
      <div class="empty-icon">🤖</div>
      <p>AI画廊等待创作中...</p>
    </div>
    
    <!-- 搜索结果为空提示 -->
    <div v-if="!isLoading && mediaList.length === 0 && isSearching" class="empty-state">
      <div class="empty-icon">🔍</div>
      <p>没有找到相关AI作品</p>
      <button class="empty-action-btn" @click="clearSearch">清空搜索</button>
    </div>

    <!-- AI提示信息 -->
    <div v-if="!isLoading && mediaList.length > 0" class="ai-tip">
      <span class="tip-icon">💡</span>
      <span>所有作品均由AI生成，点击作品查看详情</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useUserInfoStore } from '@/stores/userInfo'
import { useTokenStore } from '@/stores/token'
import ArtFrame from './ArtFrame.vue'
import Lightbox from './Lightbox.vue'
import VideoModal from './VideoModal.vue'
import UserUploads from './UserUploads.vue'
import { getMediaListService } from '@/api/media'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserInfoStore()
const tokenStore = useTokenStore()

// 数据状态
const mediaList = ref([])
const errorMessage = ref('')
const sortBy = ref('latest')
const showLightbox = ref(false)
const showUploadModal = ref(false)
const currentIndex = ref(0)
const isLoading = ref(false)

// 视频播放相关状态
const showVideoPlayer = ref(false)
const currentVideo = ref(null)

// 分页相关状态
const currentPage = ref(1)
const pageSize = ref(50)
const totalItems = ref(0)
const totalPages = ref(0)
const jumpPageInput = ref('')

// 类型筛选状态
const selectedType = ref('all') // 默认显示AI绘画

// 搜索相关状态
const searchKeyword = ref('')
const isSearching = ref(false)

// 瀑布流列数
const columnCount = ref(5)
const masonryColumns = ref([])

// 监听窗口大小调整列数
const updateColumnCount = () => {
  const width = window.innerWidth
  if (width < 576) columnCount.value = 2
  else if (width < 768) columnCount.value = 3
  else if (width < 1200) columnCount.value = 4
  else columnCount.value = 5
}

// 重新排列为瀑布流
const rearrangeMasonry = () => {
  const columns = Array.from({ length: columnCount.value }, () => [])
  
  mediaList.value.forEach((item, index) => {
    const columnIndex = index % columnCount.value
    columns[columnIndex].push(item)
  })
  
  masonryColumns.value = columns
}

// 检查用户是否登录
const checkLogin = () => {
  if (!tokenStore.accessToken) {
    ElMessage({
      type: 'warning',
      message: '请先登录后再使用AI创作'
    })
    router.push('/login')
    return false
  }
  return true
}

// 修改上传按钮的点击事件
const uploadAIArt = () => {
  if (checkLogin()) {
    showUploadModal.value = true
  }
}

// 上传成功处理（保持原样，新增作品放在第一页）
const handleUploadSuccess = (newMedia) => {
  const newMedias = Array.isArray(newMedia) ? newMedia : [newMedia]
  
  newMedias.forEach(media => {
    mediaList.value = [{
      id: media.id,
      title: media.filename,
      artist: userStore.currentUser.username || '匿名用户',
      image: media.filePath || media.url,
      thumbnailPath: media.thumbnailPath,
      year: new Date().getFullYear().toString(),
      description: media.description,
      category: media.category,
      wall: getRandomWall(),
      likes: 0,
      liked: media.isLiked,
      viewCount: 0,
      mediaType: media.mediaType,
      fileSize: media.fileSize,
      height: media.height,
      width: media.width,
      duration: media.duration,
      resolution: media.resolution,
      uploadTime: media.uploadTime,
      userId: media.userId,
      isPublic: media.isPublic,
      status: 'pending',
      _likeCount: 0,
      _uploadTime: media.uploadTime
    }, ...mediaList.value]
  })
  
  // 更新总数
  totalItems.value += newMedias.length
  
  ElMessage({
    type: 'success',
    message: `成功上传${newMedias.length}个作品`
  })
}

// 计算显示的页码范围
const visiblePages = computed(() => {
  const maxVisible = 7
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

const hasEllipsis = computed(() => {
  return totalPages.value > 7 && visiblePages.value[visiblePages.value.length - 1] < totalPages.value
})

// 跳转到指定页面
const jumpToPage = () => {
  if (!jumpPageInput.value || jumpPageInput.value === '') {
    return
  }
  
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

// 获取媒体列表
const fetchMediaList = async () => {
  try {
    isLoading.value = true
    errorMessage.value = ''
    
    const params = {
      category: null, // 固定为AI艺术类别
      status: 'active',
      isPublic: true,
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      orderBy: sortBy.value === 'latest' ? 'upload_time' : 'like_count',
      order: 'DESC'
    }
    
    // 如果有搜索关键词
    if (searchKeyword.value.trim()) {
      params.keyword = searchKeyword.value.trim()
      isSearching.value = true
    } else {
      isSearching.value = false
    }
    
    // 类型筛选
    if (selectedType.value !== 'all') {
      params.mediaType = selectedType.value
    }
    
    const response = await getMediaListService(params)
    
    if (response.code === 0) {
      console.log('获取所有用户AI作品数据信息response', response.data)
      
      // 更新分页信息
      totalItems.value = response.data?.total || 0
      totalPages.value = Math.ceil(totalItems.value / pageSize.value)
      
      // 转换API数据为前端需要的格式
      mediaList.value = (response.data?.items || []).map(item => ({
        id: item.id,
        title: item.filename,
        artist: userStore.currentUser.username || 'AI创作者',
        image: item.filePath || item.url,
        thumbnailPath: item.thumbnailPath,
        year: item.uploadDate?.split('-')[0] || new Date(item.uploadTime).getFullYear().toString(),
        description: item.description || 'AI生成的艺术作品',
        category: item.category || 'AI艺术',
        tags: item.tags,
        wall: Math.random() > 0.5 ? 'left' : 'right',
        likes: item.likeCount || 0,
        liked: item.isLiked,
        viewCount: item.viewCount || 0,
        mediaType: item.mediaType || 'image',
        fileSize: item.fileSize,
        resolution: `${item.height || 0}x${item.width || 0}`,
        height: item.height,
        width: item.width,
        duration: item.duration,
        uploadTime: item.uploadTime,
        userId: item.userId,
        isPublic: item.isPublic,
        status: item.status,
        _likeCount: item.likeCount || 0,
        _uploadTime: item.uploadTime
      }))
      
      // 重新排列瀑布流
      nextTick(() => {
        rearrangeMasonry()
      })
    } else {
      errorMessage.value = response.message || '获取AI作品失败'
      mediaList.value = []
      totalItems.value = 0
      totalPages.value = 0
    }
  } catch (err) {
    console.error('获取AI作品列表失败:', err)
    errorMessage.value = err.response?.data?.message || '无法加载AI作品，请稍后重试'
    mediaList.value = []
    totalItems.value = 0
    totalPages.value = 0
  } finally {
    isLoading.value = false
  }
}

// 处理类型筛选变化
const handleTypeChange = () => {
  currentPage.value = 1
  fetchMediaList()
}

// 处理搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchMediaList()
}

// 清空搜索
const clearSearch = () => {
  searchKeyword.value = ''
  currentPage.value = 1
  fetchMediaList()
}

// 分页变化处理
const changePage = (page) => {
  if (page < 1 || page > totalPages.value || page === currentPage.value) return
  
  currentPage.value = page
  fetchMediaList()
  
  window.scrollTo({ top: 0, behavior: 'smooth' })
}


// 计算图片列表（用于Lightbox）
const filteredImageList = computed(() => {
  return mediaList.value.filter(media => media.mediaType !== 'video')
})

// 打开查看器
const openLightbox = (media) => {
  if (media.status === 'pending') {
    ElMessage.warning('该作品正在生成中，请稍后查看')
    return
  }
  
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

const closeLightbox = () => {
  showLightbox.value = false
}

// 改变排序方式
const changeSort = (type) => {
  sortBy.value = type
  currentPage.value = 1
  fetchMediaList()
}

// 返回上一页
const goBack = () => {
  router.push('/gallery')
}

// 初始化
onMounted(() => {
  updateColumnCount()
  window.addEventListener('resize', updateColumnCount)
  fetchMediaList()
})

// 监听排序方式变化
watch(sortBy, fetchMediaList)

// 监听当前页码变化
watch(currentPage, () => {
  jumpPageInput.value = ''
})

// 监听mediaList变化重新排列瀑布流
watch(mediaList, () => {
  nextTick(() => {
    rearrangeMasonry()
  })
})

// 组件卸载时移除事件监听器
onBeforeUnmount(() => {
  window.removeEventListener('resize', updateColumnCount)
})
</script>

<style scoped>
/* 画廊容器 */
.gallery-container {
  position: relative;
  width: 100%;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 5px;
  z-index: 200;
}

/* 画廊标题样式 */
.gallery-header {
  text-align: center;
  margin-top: 90px;
}

.header-content {
  max-width: 800px;
  margin: 0 auto;
}

.gallery-title {
  font-size: 3.5rem;
  color: #28f5d3;
  font-family: 'Courier New', Courier, monospace;
  font-weight: bold;
  margin: 0 0 10px 0;
  text-shadow: 
    0 0 10px rgba(255, 255, 255, 0.8),
    0 0 20px rgba(102, 126, 234, 0.8),
    0 0 30px rgba(118, 75, 162, 0.8);
  letter-spacing: 2px;
  animation: titleGlow 3s infinite alternate;
}

.gallery-subtitle {
  font-size: 1.4rem;
  font-style: italic;
  font-weight: bold;
  color: rgba(245, 88, 179, 0.9);
  text-shadow: 10px -10px 30px rgba(104, 235, 16, 0.8);
  font-family: 'Noto Sans SC', sans-serif;
  margin: 0;
}
.gallery-subtitle:hover {
  font-size: 1.8rem;
  color: rgba(11, 245, 69, 0.9);
  text-shadow: 10px -10px 30px rgba(226, 250, 8, 0.8);
  transition: all 3s ease;
}

.title-decoration {
  margin: 0 15px;
  animation: sparkle 3s infinite;
}

@keyframes titleGlow {
  0% { text-shadow: 0 0 10px rgba(255, 255, 255, 0.8), 0 0 20px rgba(102, 126, 234, 0.8); }
  100% { text-shadow: 0 0 20px rgba(255, 255, 255, 1), 0 0 40px rgba(102, 126, 234, 1); }
}

@keyframes sparkle {
  0%, 100% { opacity: 0.5; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.2); }
}

/* 顶部操作栏样式 */
.top-options {
  position: fixed;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  width: 90%;
  max-width: 1200px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(135deg, #fa94c3 0%, #50b9b4 100%);
  backdrop-filter: blur(10px);
  padding: 15px 25px;
  border-radius: 20px;
  box-shadow: 
    0 10px 30px rgba(0, 0, 0, 0.2),
    0 0 20px rgba(102, 126, 234, 0.3);
  z-index: 1000;
  gap: 20px;
  margin: 0 auto 30px;
}

/* 搜索容器 */
.search-container {
  flex: 1;
  max-width: 500px;
}

.search-input-wrapper {
  display: flex;
  gap: 10px;
  align-items: center;
}

.search-input {
  flex: 1;
  padding: 12px 20px;
  border: 2px solid transparent;
  border-radius: 30px;
  color: rgba(21, 88, 233, 0.8);
  font-size: 16px;
  font-weight: 500;
  font-style: oblique;
  font-family: 'Noto Sans SC', sans-serif;
  background: rgba(255, 255, 255, 0.9);
  transition: all 0.3s ease;
  box-shadow: inset 0 2px 5px rgba(0, 0, 0, 0.1);
}

.search-input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 
    inset 0 2px 5px rgba(0, 0, 0, 0.1),
    0 0 15px rgba(102, 126, 234, 0.3);
}

.search-button {
  padding: 12px 25px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: rgba(28, 247, 8, 0.8);
  border: none;
  border-radius: 30px;
  cursor: pointer;
  font-family: 'Noto Sans SC', sans-serif;
  font-weight: 600;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
}

.search-button:hover {
  transform: translateY(-4px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.6);
}

/* 右侧操作区域 */
.right-actions {
  display: flex;
  align-items: center;
  gap: 15px;
}

/* 类型筛选下拉框 */
.type-select {
  padding: 12px 40px 12px 20px;
  border: 2px solid #667eea;
  border-radius: 30px;
  background: rgba(255, 255, 255, 0.9) url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='16' height='16' viewBox='0 0 24 24' fill='none' stroke='%23667eea' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='6 9 12 15 18 9'%3E%3C/polyline%3E%3C/svg%3E") no-repeat right 15px center;
  font-family: 'Noto Sans SC', sans-serif;
  font-size: 14px;
  color: #667eea;
  cursor: pointer;
  appearance: none;
  -webkit-appearance: none;
  -moz-appearance: none;
  transition: all 0.3s ease;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
}

.type-select:focus {
  outline: none;
  border-color: #764ba2;
  box-shadow: 0 0 15px rgba(118, 75, 162, 0.3);
}

/* 排序按钮 */
.sort-options {
  display: flex;
  gap: 10px;
}

.sort-btn {
  padding: 10px 20px;
  border: 2px solid rgba(102, 126, 234, 0.3);
  border-radius: 25px;
  background: rgba(255, 255, 255, 0.9);
  color: #667eea;
  cursor: pointer;
  transition: all 0.3s ease;
  font-family: 'Noto Sans SC', sans-serif;
  font-size: 14px;
  font-weight: 500;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
}

.sort-btn:hover {
  background: rgba(102, 126, 234, 0.1);
  border-color: #667eea;
}

.sort-btn.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-color: transparent;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
}
/* AI创作按钮 */
.ai-generate-button {
  padding: 12px 25px;
  background: linear-gradient(135deg, #ff6b6b 0%, #ffd166 100%);
  color: white;
  border: none;
  border-radius: 30px;
  cursor: pointer;
  font-family: 'Noto Sans SC', sans-serif;
  font-weight: 600;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(255, 107, 107, 0.4);
  display: flex;
  align-items: center;
  gap: 8px;
}

.ai-generate-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(255, 107, 107, 0.6);
}
/* 返回按钮 */
.back-button {
  position: fixed;
  top: 30px;
  left: 30px;
  padding: 12px 20px;
  background: rgba(255, 255, 255, 0.95);
  border: 2px solid #667eea;
  border-radius: 30px;
  cursor: pointer;
  z-index: 1000;
  display: flex;
  align-items: center;
  font-family: 'Noto Sans SC', sans-serif;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

.back-button:hover {
  background: #667eea;
  color: white;
  transform: translateX(-5px);
}

.arrow-icon {
  margin-right: 8px;
  font-size: 1.2rem;
}

/* 画廊主体 */
.gallery-hall {
  padding: 20px 0;
  min-height: calc(100vh - 300px);
}

/* 瀑布流布局 */
.masonry-grid {
  display: flex;
  gap: 20px;
  padding: 0 20px;
  max-width: 1600px;
  margin: 0 auto;
}

.masonry-column {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.masonry-item {
  break-inside: avoid;
  transition: all 0.3s ease;
}

.masonry-item:hover {
  transform: translateY(-10px);
}

/* 卡片样式优化 */
.masonry-item .art-frame {
  border: 2px solid transparent;
  border-radius: 12px;
  overflow: hidden;
  background: white;
  box-shadow: 10px 15px 15px rgba(201, 33, 216, 0.8);
  transition: all 1s ease;
}

.masonry-item .art-frame:hover {
  transform: translateY(-10px);
  box-shadow: 
    0 20px 40px rgba(78, 240, 86, 0.7),
    0 0 0 1px rgba(223, 182, 182, 0.7);
}

/* 分页信息 */
.page-info {
  text-align: center;
  margin: 0 auto;
  margin-bottom: 10px;
  color: rgba(227, 241, 20, 0.8);
  font-size: 16px;
  font-family: 'Noto Sans SC', sans-serif;
  opacity: 0.9;
}

/* 分页组件 */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 15px;
  margin: 40px 0 20px;
  padding: 20px;
  flex-wrap: wrap;
}

.page-btn {
  padding: 10px 25px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-radius: 30px;
  background: rgba(255, 255, 255, 0.9);
  color: #667eea;
  cursor: pointer;
  font-family: 'Noto Sans SC', sans-serif;
  font-size: 14px;
  transition: all 0.3s ease;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
}

.page-btn:hover:not(:disabled) {
  background: #667eea;
  color: white;
  border-color: transparent;
  transform: translateY(-2px);
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-numbers {
  display: flex;
  gap: 8px;
  align-items: center;
}

.page-number {
  min-width: 40px;
  height: 40px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.9);
  color: #667eea;
  cursor: pointer;
  font-family: 'Noto Sans SC', sans-serif;
  font-size: 14px;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.page-number:hover {
  background: rgba(102, 126, 234, 0.1);
  border-color: #667eea;
}

.page-number.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-color: transparent;
  box-shadow: 0 4px 10px rgba(102, 126, 234, 0.4);
}

.page-ellipsis {
  color: rgba(255, 255, 255, 0.7);
  padding: 0 5px;
}

/* 跳转页面 */
.page-jump {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 15px;
}

.jump-text {
  color: white;
  font-size: 14px;
  font-family: 'Noto Sans SC', sans-serif;
}

.jump-input {
  width: 60px;
  padding: 8px 12px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.9);
  color: #333;
  font-family: 'Noto Sans SC', sans-serif;
  font-size: 14px;
  text-align: center;
  transition: all 0.3s ease;
}

.jump-input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 10px rgba(102, 126, 234, 0.3);
}

/* 加载状态 */
.loading {
  text-align: center;
  padding: 60px 20px;
  color: white;
}

.loading-spinner {
  width: 50px;
  height: 50px;
  border: 3px solid rgba(255, 255, 255, 0.3);
  border-top: 3px solid #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 20px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 错误提示 */
.error-message {
  text-align: center;
  padding: 40px 20px;
  color: #ff6b6b;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 20px;
  margin: 20px auto;
  max-width: 600px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: white;
  max-width: 500px;
  margin: 0 auto;
}

.empty-icon {
  font-size: 4rem;
  margin-bottom: 20px;
  animation: float 3s infinite ease-in-out;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-20px); }
}

.empty-state p {
  font-size: 1.2rem;
  margin-bottom: 30px;
  opacity: 0.9;
}

.empty-action-btn {
  padding: 15px 30px;
  background: linear-gradient(135deg, #ff6b6b 0%, #ffd166 100%);
  color: white;
  border: none;
  border-radius: 30px;
  cursor: pointer;
  font-family: 'Noto Sans SC', sans-serif;
  font-weight: 600;
  font-size: 1rem;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(255, 107, 107, 0.4);
}

.empty-action-btn:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 20px rgba(255, 107, 107, 0.6);
}

/* AI提示信息 */
.ai-tip {
  position: fixed;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(129, 223, 230, 0.8);
  padding: 5px 5px;
  border-radius: 30px;
  font-family: 'Noto Sans SC', sans-serif;
  font-size: 14px;
  color: #667eea;
  box-shadow: 
    0 5px 20px rgba(0, 0, 0, 0.1),
    0 0 15px rgba(102, 126, 234, 0.3);
  display: flex;
  align-items: center;
  gap: 10px;
  z-index: 100;
  animation: fadeInUp 3s ease;
}

.tip-icon {
  font-size: 1.5rem;
  animation: pulse 3s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateX(-50%) translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateX(-50%) translateY(0);
  }
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .gallery-title {
    font-size: 2.8rem;
  }
  
  .top-options {
    flex-direction: column;
    align-items: stretch;
    gap: 15px;
  }
  
  .search-container {
    max-width: 100%;
  }
}

@media (max-width: 992px) {
  .gallery-title {
    font-size: 2.2rem;
  }
  
  .masonry-grid {
    gap: 15px;
  }
  
  .right-actions {
    flex-wrap: wrap;
    justify-content: center;
  }
}

@media (max-width: 768px) {
  .gallery-container {
    padding: 10px;
  }
  
  .gallery-title {
    font-size: 1.8rem;
  }
  
  .gallery-subtitle {
    font-size: 1rem;
  }
  
  .top-options {
    width: 95%;
    padding: 12px 15px;
  }
  
  .search-input-wrapper {
    flex-direction: column;
  }
  
  .search-input,
  .search-button,
  .type-select {
    width: 100%;
  }
  
  .back-button {
    top: 15px;
    left: 15px;
    padding: 10px 15px;
  }
  
  .masonry-grid {
    padding: 0 10px;
    gap: 10px;
  }
  
  .pagination {
    flex-direction: column;
    gap: 15px;
  }
  
  .page-jump {
    order: 3;
    margin: 10px 0;
  }
  
  .ai-tip {
    bottom: 20px;
    font-size: 12px;
    padding: 10px 20px;
  }
}

@media (max-width: 576px) {
  .gallery-title {
    font-size: 1.5rem;
  }
  
  .top-options {
    position: relative;
    top: 0;
    transform: none;
    margin: 20px auto;
  }
  
  .right-actions {
    flex-direction: column;
    align-items: stretch;
  }
  
  .sort-options {
    justify-content: center;
  }
  
  .masonry-grid {
    flex-direction: column;
  }
  
  .masonry-column {
    width: 100%;
  }
  
  .empty-icon {
    font-size: 3rem;
  }
  
  .ai-tip {
    display: none; /* 小屏幕隐藏AI提示 */
  }
}

/* 网格布局中的分隔线效果 */
.gallery-hall::before {
  content: '';
  position: fixed;
  left: 50%;
  top: 0;
  bottom: 0;
  width: 1px;
  background: linear-gradient(to bottom, transparent, rgba(167, 27, 231, 0.3), transparent);
  opacity: 0.2;
  z-index: 0;
  pointer-events: none;
}
</style>