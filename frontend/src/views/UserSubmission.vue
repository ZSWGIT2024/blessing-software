<template>
  <div class="gallery-container">
  <!-- 顶部装饰标题 -->
    <div class="gallery-header">
      <div class="header-content">
        <h1 class="gallery-title">
        <span class="title-decoration">✧✧✧</span>
          <span class="title-text">网友投稿作品</span>
          <span class="title-decoration">✧✧✧</span>
        </h1>
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
            placeholder="搜索作品标题、描述或分类..." 
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
        <!-- 类型筛选下拉框 -->
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
      </div>
    </div>

    <!-- 返回按钮 -->
    <button class="back-button" @click="goBack">
      <i class="arrow-icon">←</i> 返回
    </button>

    <!-- 修改上传按钮，添加点击事件 -->
    <button class="upload-button" @click="uploadButtonClick">
      <span class="iconfont icon-shangchuan">上传作品</span>
    </button>

   <!-- 画廊主体 - 改为网格布局 -->
    <div class="gallery-hall">
      <!-- 添加分页信息和加载状态 -->
      <div v-if="!isLoading && mediaList.length > 0" class="page-info">
        显示 {{ (currentPage - 1) * pageSize + 1 }}-{{ Math.min(currentPage * pageSize, totalItems) }} 
        条，共 {{ totalItems }} 条作品
      </div>
      
      <div class="gallery-grid">
        <ArtFrame 
          v-for="media in sortedMediaList" 
          :key="media.id" 
          :artwork="media"
          @click="openLightbox(media)" 
        />
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

        <!-- 添加跳转页面功能 -->
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
      加载中...
    </div>

    <!-- 错误提示 -->
    <div v-if="errorMessage" class="error-message">
      {{ errorMessage }}
    </div>

    <!-- 空状态提示 -->
    <div v-if="!isLoading && mediaList.length === 0 && !isSearching" class="empty-state">
      <p>暂时还没有作品哦，快来上传第一个作品吧！</p>
    </div>
    
    <!-- 搜索结果为空提示 -->
    <div v-if="!isLoading && mediaList.length === 0 && isSearching" class="empty-state">
      <p>没有找到相关作品，请尝试其他搜索关键词</p>
    </div>

  </div>
</template>



<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserInfoStore } from '@/stores/userInfo'
import { useTokenStore } from '@/stores/token' // 导入token存储
import ArtFrame from './ArtFrame.vue'
import Lightbox from './Lightbox.vue'
import UserUploads from './UserUploads.vue'
import VideoModal from './VideoModal.vue' // 引入视频播放器模态框组件
import { getSubmitListService } from '@/api/submit' // 导入API
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserInfoStore()
const tokenStore = useTokenStore() // 使用token存储

// 替换原有的artworks，使用从API获取的数据
const mediaList = ref([])
const errorMessage = ref('')
const sortBy = ref('latest')
const showUploadModal = ref(false)
const showLightbox = ref(false)
const currentIndex = ref(0)
const isLoading = ref(false)

// 添加视频播放相关状态
const showVideoPlayer = ref(false)
const currentVideo = ref(null)

// 新增：分页相关状态
const currentPage = ref(1)
const pageSize = ref(50) // 每页显示数量
const totalItems = ref(0) // 总条目数
const totalPages = ref(0) // 总页数
// 新增：跳转页面输入框的状态
const jumpPageInput = ref('')

// 新增：类型筛选状态
const selectedType = ref('all') // 'all', 'image', 'video'

// 新增：搜索相关状态
const searchKeyword = ref('')
const isSearching = ref(false)

// 检查用户是否登录
const checkLogin = () => {
  if (!tokenStore.accessToken) {
    ElMessage({
      type: 'warning',
      message: '请先登录后再上传作品'
    })
    router.push('/login')
    return false
  }
  return true
}

// 修改上传按钮的点击事件
const uploadButtonClick = () => {
  if (checkLogin()) {
    showUploadModal.value = true
  }
}

// 计算显示的页码范围（最多显示7个页码）
const visiblePages = computed(() => {
  const maxVisible = 7
  const half = Math.floor(maxVisible / 2)
  let start = Math.max(1, currentPage.value - half)
  let end = Math.min(totalPages.value, start + maxVisible - 1)
  
  // 调整起始位置
  if (end - start + 1 < maxVisible) {
    start = Math.max(1, end - maxVisible + 1)
  }
  
  const pages = []
  for (let i = start; i <= end; i++) {
    pages.push(i)
  }
  return pages
})

// 是否显示省略号
const hasEllipsis = computed(() => {
  return totalPages.value > 7 && visiblePages.value[visiblePages.value.length - 1] < totalPages.value
})

// 新增：跳转到指定页面方法
const jumpToPage = () => {
  if (!jumpPageInput.value || jumpPageInput.value === '') {
    return
  }
  
  let targetPage = parseInt(jumpPageInput.value)
  
  // 验证输入范围
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
  
  // 如果已经在目标页面，不做操作
  if (targetPage === currentPage.value) {
    jumpPageInput.value = ''
    return
  }
  
  // 跳转到目标页面
  changePage(targetPage)
  jumpPageInput.value = ''
}

// 修改fetchMediaList方法，添加分页和筛选参数
const fetchMediaList = async () => {
  try {
    isLoading.value = true
    errorMessage.value = ''
    
    // 构建查询参数
    const params = {
      mediaType: selectedType.value === 'all' ? null : selectedType.value,
      category: null,
      status: 'active', // 固定查询active状态的数据
      isPublic: true, // 固定查询公开的数据
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      orderBy: sortBy.value === 'latest' ? 'upload_time' : 'like_count',
      order: 'DESC'
    }
    
    // 如果有搜索关键词，添加到参数中
    if (searchKeyword.value.trim()) {
      params.keyword = searchKeyword.value.trim()
      isSearching.value = true
    } else {
      isSearching.value = false
    }
    
    const response = await getSubmitListService(params)
    
    if (response.code === 0) {
      
      // 更新分页信息
      totalItems.value = response.data?.total || 0
      totalPages.value = Math.ceil(totalItems.value / pageSize.value)
      if (response.data?.items?.length === 0) {
        ElMessage({
          type: 'info',
          message: '暂无符合条件的作品'
        })
      }
      
      // 转换API数据为前端需要的格式
      mediaList.value = (response.data?.items || []).map(item => ({
        id: item.id,
        title: item.filename,
        artist: userStore.currentUser.username || '匿名用户',
        image: item.filePath || item.url,
        thumbnailPath: item.thumbnailPath,
        year: item.uploadDate?.split('-')[0] || new Date(item.uploadTime).getFullYear().toString(),
        description: item.description,
        category: item.category,
        tags: item.tags,
        wall: item.wall,
        likes: item.likeCount || 0,
        liked: item.isLiked,
        viewCount: item.viewCount || 0,
        mediaType: item.mediaType,
        fileSize: item.fileSize,
        resolution: item.height + 'x' + item.width,
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
    } else {
      errorMessage.value = response.message || '获取数据失败'
      mediaList.value = []
      totalItems.value = 0
      totalPages.value = 0
    }
  } catch (err) {
    console.error('获取媒体列表失败:', err)
    errorMessage.value = err.response?.data?.message || '无法加载作品，请稍后重试'
    mediaList.value = []
    totalItems.value = 0
    totalPages.value = 0
  } finally {
    isLoading.value = false
  }
}

// 新增：处理类型筛选变化
const handleTypeChange = () => {
  currentPage.value = 1 // 重置到第一页
  fetchMediaList()
}

// 新增：处理搜索
const handleSearch = () => {
  currentPage.value = 1 // 重置到第一页
  fetchMediaList()
}

// 新增：分页变化处理
const changePage = (page) => {
  // 验证页面范围
  if (page < 1 || page > totalPages.value || page === currentPage.value) return
  
  currentPage.value = page
  fetchMediaList()
  
  // 滚动到顶部
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// 随机分配左右墙（保持原样）
const getRandomWall = () => {
  return Math.random() > 0.5 ? 'left' : 'right'
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
      liked: false,
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

// 修改 Lightbox 只接收图片数据（保持原样）
const filteredImageList = computed(() => {
  return mediaList.value.filter(media => media.mediaType !== 'video')
})

// 排序后的作品列表（保持原样）
const sortedMediaList = computed(() => {
  return [...mediaList.value].sort((a, b) => {
    if (sortBy.value === 'latest') {
      return new Date(b._uploadTime) - new Date(a._uploadTime)
    } else {
      return b._likeCount - a._likeCount
    }
  })
})

// 修改打开查看器的方法（保持原样）
const openLightbox = (media) => {
  if (media.status === 'pending') {
    ElMessage.warning('该作品正在审核中，暂时无法查看')
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

// 修改：改变排序方式时重置到第一页
const changeSort = (type) => {
  sortBy.value = type
  currentPage.value = 1
  fetchMediaList()
}

// 初始化加载
onMounted(() => {
  fetchMediaList()
})

// 监听排序方式变化（保持原样）
watch(sortBy, fetchMediaList)

// 监听当前页码变化，重置跳转输入框
watch(currentPage, () => {
  jumpPageInput.value = ''
})
// 返回上一页（保持原样）
const goBack = () => {
  router.push('/gallery')
}
</script>



<style scoped>
/* 保持原有样式不变，添加新样式 */

/* 新增：顶部操作栏样式 */
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
  background: rgba(255, 255, 255, 0.95);
  opacity: 0.8;
  padding: 15px 20px;
  border-radius: 12px;
  box-shadow: 0 10px 20px rgba(20, 214, 107, 0.8);
  z-index: 100;
  gap: 20px;
}

/* 新增：搜索容器样式 */
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
  padding: 10px 15px;
  border: 2px solid #35fcd1;
  border-radius: 25px;
  font-size: 16px;
  font-weight: bold;
  font-style: italic;
  font-family: 'Noto Sans SC', sans-serif;
  text-shadow: 1px 1px 1px rgb(60, 121, 235);
  background: #f8fdff;
  transition: all 0.3s ease;
}

.search-input:focus {
  outline: none;
  border-color: #fa94c3;
  box-shadow: 0 0 0 3px rgba(250, 148, 195, 0.1);
}

.search-button {
  padding: 10px 20px;
  background: linear-gradient(135deg, #fa94c3 0%, #50b9b4 100%);
  color: white;
  border: none;
  border-radius: 25px;
  cursor: pointer;
  font-family: 'Noto Sans SC', sans-serif;
  font-weight: 500;
  transition: all 0.3s ease;
}

.search-button:hover {
  transform: translateY(-3px);
  box-shadow: 0 4px 12px rgba(230, 24, 219, 0.6);
}

/* 新增：右侧操作区域 */
.right-actions {
  display: flex;
  align-items: center;
  gap: 15px;
}

/* 新增：类型筛选下拉框样式 */
.filter-options {
  position: relative;
}

.type-select {
  padding: 10px 35px 10px 15px;
  border: 2px solid #e07bf5;
  border-radius: 25px;
  background: #f8fdff url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%23fa94c3' d='M6 8L2 4h8z'/%3E%3C/svg%3E") no-repeat right 15px center;
  font-family: 'Noto Sans SC', sans-serif;
  font-size: 14px;
  color: #fa24e8;
  cursor: pointer;
  appearance: none;
  -webkit-appearance: none;
  -moz-appearance: none;
  transition: all 0.3s ease;
}

.type-select:focus {
  outline: none;
  border-color: #fa94c3;
  box-shadow: 0 0 0 3px rgba(250, 148, 195, 0.1);
}

.type-select option {
  padding: 10px;
  background: rgb(245, 246, 247);
}

/* 调整原有排序按钮位置 */
.sort-options {
  display: flex;
  gap: 10px;
}

.sort-btn {
  padding: 8px 16px;
  border: 2px solid #e6f7ff;
  border-radius: 20px;
  background-color: #f8fdff;
  color: #666;
  cursor: pointer;
  transition: all 0.3s ease;
  font-family: 'Noto Sans SC', sans-serif;
  font-size: 14px;
}

.sort-btn:hover {
  background-color: #e6f7ff;
  border-color: #fa94c3;
  color: #fa94c3;
}

.sort-btn.active {
  background: linear-gradient(135deg, #fa94c3 0%, #50b9b4 100%);
  color: white;
  border-color: transparent;
}

/* 新增：分页信息样式 */
.page-info {
  text-align: center;
  margin: 20px 0;
  color: #666;
  font-size: 14px;
  font-family: 'Noto Sans SC', sans-serif;
}


/* 新增：跳转页面样式 */
.page-jump {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 15px;
}

.jump-text {
  color: #666;
  font-size: 14px;
  font-family: 'Noto Sans SC', sans-serif;
}

.jump-input {
  width: 20px;
  padding: 6px 10px;
  border: 2px solid #e6f7ff;
  border-radius: 8px;
  background: #f8fdff;
  color: #333;
  font-family: 'Noto Sans SC', sans-serif;
  font-size: 14px;
  text-align: center;
  transition: all 0.3s ease;
}

.jump-input:focus {
  outline: none;
  border-color: #fa94c3;
  box-shadow: 0 0 0 3px rgba(250, 148, 195, 0.1);
}

.jump-input::-webkit-inner-spin-button,
.jump-input::-webkit-outer-spin-button {
  -webkit-appearance: none;
  margin: 0;
}



/* 调整分页容器样式以容纳跳转功能 */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
  margin: 40px 0 20px;
  padding: 20px;
  flex-wrap: wrap;
}

.page-btn {
  padding: 8px 20px;
  border: 2px solid #e6f7ff;
  border-radius: 25px;
  background: #f8fdff;
  color: #666;
  cursor: pointer;
  font-family: 'Noto Sans SC', sans-serif;
  font-size: 14px;
  transition: all 0.3s ease;
}

.page-btn:hover:not(:disabled) {
  background: #e6f7ff;
  border-color: #fa94c3;
  color: #fa94c3;
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-numbers {
  display: flex;
  gap: 5px;
  align-items: center;
}

.page-number {
  min-width: 36px;
  height: 36px;
  border: 2px solid #e6f7ff;
  border-radius: 50%;
  background: #f8fdff;
  color: #666;
  cursor: pointer;
  font-family: 'Noto Sans SC', sans-serif;
  font-size: 14px;
  transition: all 0.3s ease;
}

.page-number:hover {
  background: #e6f7ff;
  border-color: #fa94c3;
  color: #fa94c3;
}

.page-number.active {
  background: linear-gradient(135deg, #fa94c3 0%, #50b9b4 100%);
  color: white;
  border-color: transparent;
}

.page-ellipsis {
  color: #999;
  padding: 0 5px;
}

/* 调整原有样式，为顶部操作栏腾出空间 */
.gallery-hall {
  padding: 100px 60px 40px;
  min-height: calc(100vh - 200px);
}


/* 调整原有返回按钮位置 */
.back-button {
  position: fixed;
  top: 15px;
  left: 50px;
  z-index: 100;
}

/* 保持其他原有样式不变 */
.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #666;
  font-size: 16px;
  margin-top: 40px;
}

.gallery-container {
  position: relative;
  width: 100%;
  min-height: 100vh;
  background-color: #f3c4d4;
  padding: 20px;
  z-index: 200;
  opacity: 90%;
}

.back-button {
  padding: 10px 15px;
  background: rgba(236, 140, 199, 0.8);
  border: 1px solid #98e3f0;
  border-radius: 30px;
  cursor: pointer;
  display: flex;
  align-items: center;
  font-family: 'Noto Sans SC', sans-serif;
  transition: all 0.3s ease;
}

.back-button:hover {
  background: white;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.arrow-icon {
  margin-right: 5px;
  font-size: 1.2rem;
}

/* 画廊主体 */
.gallery-hall {
  padding: 10px 60px;
  min-height: calc(100vh - 200px);
}

/* 网格布局 */
.gallery-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 30px;
  max-width: 1800px;
  margin: 0 auto;
}

/* 卡片样式优化 */
.gallery-grid .art-frame {
  border-radius: 12px;
  overflow: hidden;
  background: white;
  box-shadow: 0 5px 15px rgba(50, 231, 216, 0.8);
  transition: all 0.3s ease;
}

.gallery-grid .art-frame:hover {
  transform: translateY(-10px);
  box-shadow: 
    0 20px 40px rgba(78, 240, 86, 0.7),
    0 0 0 1px rgba(223, 182, 182, 0.7);
}

/* 响应式调整 */
@media (max-width: 1400px) {
  .gallery-grid {
    grid-template-columns: repeat(4, 1fr);
    gap: 25px;
    padding: 25px;
  }
}

@media (max-width: 1200px) {
  .top-options {
    flex-direction: column;
    align-items: stretch;
    gap: 15px;
  }
  
  .search-container {
    max-width: 100%;
  }
  
  .right-actions {
    justify-content: space-between;
  }
}

@media (max-width: 992px) {
  .gallery-grid {
    grid-template-columns: repeat(3, 1fr);
  }
  
  .top-options {
    width: 95%;
    padding: 12px 15px;
  }
}

@media (max-width: 768px) {
  .gallery-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 20px;
    padding: 20px;
  }
  
  .gallery-hall {
    padding: 120px 20px 40px;
  }
  
  .right-actions {
    flex-wrap: wrap;
    justify-content: center;
  }
  .upload-button,
  .back-button {
    position: relative;
    top: auto;
    left: auto;
    right: auto;
    margin: 10px;
    display: inline-block;
  }
  .upload-button {
    margin-top: 70px;
  }
  .pagination {
    flex-direction: column;
    gap: 15px;
  }
  
  .page-jump {
    order: 3;
    margin: 10px 0;
  }
  
  .page-numbers {
    order: 2;
  }
  
  .page-btn {
    order: 1;
  }
  
  /* 小屏幕时重新排列按钮顺序 */
  .pagination .page-btn:first-child {
    order: 1;
  }
  
  .pagination .page-btn:last-child {
    order: 4;
  }
}
.upload-button {
  position: fixed;
  top: 50px;
  right: 50px;
  padding: 8px 16px;
  border: none;
  border-radius: 20px;
  background-color: #f016d3;
  color: white;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  z-index: 100;
}

.upload-button:hover {
  background-color: #57b5cc;
}
@media (max-width: 576px) {
  .gallery-grid {
    grid-template-columns: 1fr;
    gap: 20px;
    padding: 15px;
  }
  
  .top-options {
    width: calc(100% - 40px);
    margin: 0 20px;
    padding: 10px;
  }
  
  .search-input-wrapper {
    flex-direction: column;
  }
  
  .search-input,
  .search-button {
    width: 100%;
  }
  
  .type-select {
    width: 100%;
  }
  
  .pagination {
    flex-wrap: wrap;
  }

  .page-jump {
    flex-wrap: wrap;
    justify-content: center;
  }
  
  .jump-input {
    width: 50px;
  }
}

.loading,
.error-message {
  text-align: center;
  padding: 20px;
  color: #666;
}

.error-message {
  color: #dc3545;
}

.empty-state p {
  color: #db6dad;
  text-shadow: 0 0 10px #15b6df;
  font-size: 42px;
  font-family: 'Noto Sans SC', sans-serif;
  font-weight: bold;
  font-style: italic;
  text-align: center;
  line-height: 1.5;
}

/* 画廊标题样式 */
.gallery-header {
  text-align: center;
  padding: 10px 10px 10px;
  background: linear-gradient(135deg, #fa94c3 0%, #50b9b4 100%);
  margin-top: 80px;
  border-bottom: 3px solid #dfb6b6;
}

.header-content {
  max-width: 800px;
  margin: 0 auto;
}

.gallery-title {
  font-size: 3rem;
  color: #dd61c8;
  font-family: 'Courier New', Courier, monospace;
  font-weight: bold;
  margin: 0 0 15px 0;
  text-shadow: 
    2px 2px 0 #98e3f0,
    4px 4px 0 rgba(0, 0, 0, 0.1);
  letter-spacing: 2px;
}

.title-decoration {
  margin-left: 15px;
  animation: sparkle 3s infinite;
}

@keyframes sparkle {
  0%, 100% { opacity: 0.5; transform: scale(3); }
  50% { opacity: 1; transform: scale(1.5); }
}

/* 网格布局中的分隔线效果 */
.gallery-grid::before {
  content: '';
  position: absolute;
  left: 50%;
  top: 0;
  bottom: 0;
  width: 1px;
  background: linear-gradient(to bottom, transparent, #dfb6b6, transparent);
  opacity: 0.3;
  z-index: 0;
}
</style>