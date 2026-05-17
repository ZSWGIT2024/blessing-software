<template>
  <div class="content-root">
    <!-- 内容类型切换标签 - 方案四：动态渐变风格 -->
    <div class="content-type-tabs-gradient">
      <div class="gradient-tabs">
        <div class="tabs-slider" :style="sliderStyle"></div>

        <div v-for="tab in tabs" :key="tab.value" class="gradient-tab" :class="{ 'active': contentType === tab.value }"
          @click="switchContentType(tab.value)" :data-index="tab.value">
          <div class="tab-glow"></div>
          <div class="tab-inner">
            <i :class="tab.icon"></i>
            <span class="tab-name">{{ tab.label }}</span>
            <div class="tab-dot"></div>
          </div>
        </div>
      </div>
    </div>

    <!-- 状态筛选按钮 -->
    <div class="status-filter" v-if="contentType !== 'posts'">
      <div class="filter-tabs">
        <button :class="{ 'active': currentStatus === 'all' }" @click="changeStatus('all')">
          全部
        </button>
        <button :class="{ 'active': currentStatus === 'active' }" @click="changeStatus('active')">
          已发布
        </button>
        <button :class="{ 'active': currentStatus === 'pending' }" @click="changeStatus('pending')">
          待审核
        </button>
        <button :class="{ 'active': currentStatus === 'hidden' }" @click="changeStatus('hidden')">
          未通过
        </button>
      </div>

      <!-- 统计信息 -->
      <div class="stats-info" v-if="contentType !== 'posts'">
        <span class="stat-item">
          <i class="iconfont icon-zongshu"></i>
          总数: {{ stats.total }}
        </span>
        <span class="stat-item active" v-if="currentStatus === 'all'">
          <i class="iconfont icon-yifabu"></i>
          已发布: {{ stats.active }}
        </span>
        <span class="stat-item pending" v-if="currentStatus === 'all'">
          <i class="iconfont icon-daishenhe"></i>
          待审核: {{ stats.pending }}
        </span>
        <span class="stat-item hidden" v-if="currentStatus === 'all'">
          <i class="iconfont icon-wenti"></i>
          未通过: {{ stats.hidden }}
        </span>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="content-section sakura-bg">
      <div class="content-header">
        <h2>
          <template v-if="contentType === 'photos'">
            <i class="iconfont icon-tupian"></i> 图片展示
          </template>
          <template v-else-if="contentType === 'videos'">
            <i class="iconfont icon-shipin"></i> 视频展示
          </template>
          <template v-else>
            <i class="iconfont icon-tiezi"></i> 动态展示
          </template>
          <span class="content-count" v-if="contentType !== 'posts'">
            (共 {{ currentContent.length }} 个)
          </span>
        </h2>

        <!-- 排序选项 -->
        <div class="sort-options" v-if="contentType !== 'posts'">
          <select v-model="orderBy" @change="fetchMediaData">
            <option value="upload_time">按上传时间</option>
            <option value="view_count">按浏览量</option>
            <option value="like_count">按点赞数</option>
          </select>
          <select v-model="order" @change="fetchMediaData">
            <option value="DESC">降序</option>
            <option value="ASC">升序</option>
          </select>
        </div>
      </div>

      <!-- 加载和错误状态 -->
      <div v-if="isLoading" class="loading-state">
        <div class="spinner"></div>
        <span>内容加载中...</span>
      </div>

      <div v-else-if="error" class="error-state">
        <div class="error-icon">⚠️</div>
        <div class="error-message">
          <p>加载失败: {{ error.message }}</p>
          <button @click="fetchMediaData">重试</button>
        </div>
      </div>

      <!-- 内容展示区域 -->
      <div class="scroll-container">

        <!-- 图片内容 -->
        <template v-if="contentType === 'photos'">
          <div v-if="currentContent.length > 0" class="content-grid">
            <div v-for="item in currentContent" :key="`photo-${item.id}`" class="content-item"
              :class="{ 'pending': item.status === 'pending', 'clickable': item.status === 'active' }"
              @click="openLightbox(item)">
              <img :src="item.filePath" class="content-image" :alt="item.filename" loading="lazy"
                :class="{ 'blur-image': item.status === 'pending' || item.status === 'hidden' }" />
              <!-- 待审核遮罩层 -->
              <div v-if="item.status === 'pending'" class="pending-overlay">
                <span class="pending-text"></span>
              </div>
              <!-- 未通过遮罩层 -->
              <div v-if="item.status === 'hidden'" class="hidden-overlay">
                <span class="hidden-text"></span>
              </div>

              <!-- 状态标签 -->
              <div class="status-badge" :class="item.status">
                {{
                  item.status === 'active' ? '已发布' :
                    item.status === 'pending' ? '待审核' :
                      '未通过'
                }}
              </div>

              <div class="content-meta">
                <div class="meta-info">
                  <span class="meta-title">作品标题: {{ item.filename }}</span>
                  <span class="meta-size">文件大小: {{ item.fileSize }}</span>
                </div>
                <button class="meta-item like-btn" @click.stop="toggleLike(item)" :class="{ 'liked': item.liked }">
                  <i class="iconfont icon-dianzan"></i>点赞数: {{ item.likeCount || 0 }}
                </button>
                <span v-if="item.userId === userStore.currentUser.id" class="public-hidden">
                  {{ item.isPublic ? '公开' : '私密' }}
                </span>
              </div>

              <!-- 浏览数 -->
              <div class="view-count">
                <i class="iconfont icon-liulan"></i>浏览数: {{ item.viewCount || 0 }}
              </div>
            </div>
          </div>
          <div v-else class="empty-state">
            <div class="empty-icon">📷</div>
            <p v-if="currentStatus === 'all'">暂无图片内容</p>
            <p v-else-if="currentStatus === 'active'">暂无已发布的图片</p>
            <p v-else>暂无待审核的图片</p>
            <!-- <button @click="fetchMediaData">刷新</button> -->
          </div>
        </template>

        <!-- 视频内容 -->
        <template v-else-if="contentType === 'videos'">
          <div v-if="currentContent.length > 0" class="content-grid">
            <!-- 修改视频项目部分的代码 -->
            <div v-for="item in currentContent" :key="`video-${item.id}`" class="content-item video-item"
              :class="{ 'pending': item.status === 'pending', 'clickable': item.status === 'active' }"
              @click="playVideo(item)">
              <div class="video-thumbnail-wrapper">
                <video class="content-video" :poster="item.thumbnailPath" preload="metadata"
                  :class="{ 'blur-image': item.status === 'pending' || item.status === 'hidden' }">
                  <source :src="item.filePath" type="video/mp4">
                </video>

                <!-- 视频时长 -->
                <div class="video-duration" v-if="item.duration">
                  {{ formatDuration(item.duration) }}
                </div>

                <!-- 播放按钮 -->
                <div class="play-overlay" v-if="item.status === 'active'">
                  <div class="play-button">
                    <svg class="play-svg" viewBox="0 0 24 24" fill="black">
                      <path d="M8 5v14l11-7z" />
                    </svg>
                  </div>
                </div>
              </div>

              <!-- 状态标签 -->
              <div class="status-badge" :class="item.status">
                {{
                  item.status === 'active' ? '已发布' :
                    item.status === 'pending' ? '待审核' :
                      '未通过'
                }}
              </div>

              <!-- 使用SVG替代字体图标 -->
              <div class="content-meta">
                <div class="meta-info">
                  <span class="meta-title">作品标题: {{ item.filename }}</span>
                  <span class="meta-size">文件大小: {{ item.fileSize }}</span>
                </div>
                <button class="meta-item like-btn" @click.stop="toggleLike(item)" :class="{ 'liked': item.liked }">
                  <i class="iconfont icon-dianzan"></i>点赞数: {{ item.likeCount || 0 }}
                </button>
                <span v-if="item.userId === userStore.currentUser.id" class="public-hidden">
                  {{ item.isPublic ? '公开' : '私密' }}
                </span>
              </div>

              <!-- 浏览数 -->
              <div class="view-count">
                <i class="iconfont icon-liulan"></i>浏览数: {{ item.viewCount || 0 }}
              </div>
            </div>
          </div>
          <div v-else class="empty-state">
            <div class="empty-icon">🎬</div>
            <p v-if="currentStatus === 'all'">暂无视频内容</p>
            <p v-else-if="currentStatus === 'active'">暂无已发布的视频</p>
            <p v-else>暂无待审核的视频</p>
          </div>
        </template>

        <!-- 动态内容 -->
        <template v-else>
          <div v-if="filteredPosts.length > 0" class="post-list">
            <div v-for="post in filteredPosts" :key="`post-${post.id}`" class="post-item">
              <div class="post-header">
                <img :src="post.author?.avatar || defaultAvatar" class="post-avatar" :alt="post.author?.name || '用户'">
                <div class="post-user">
                  <span class="post-author">{{ post.author?.name || '匿名用户' }}</span>
                  <span class="post-time">{{ formatTime(post.time) }}</span>
                </div>
              </div>

              <div class="post-content">
                {{ post.text }}
              </div>

              <div class="post-actions">
                <button class="action-btn like-btn" @click.stop="likePost(post)" :class="{ 'liked': post.isLiked }">
                  <i class="icon-like">❤️</i> {{ post.likes || 0 }}
                </button>
                <button class="action-btn comment-btn" @click.stop="commentPost(post)">
                  <i class="iconfont icon-pinglun1"></i> {{ post.comments?.length || 0 }}
                </button>

                <button @click.stop="sharePost(post)">
                  <i class="iconfont icon-fenxiang"></i></button>
              </div>

              <div v-if="post.comments?.length > 0" class="post-comments">
                <div v-for="comment in post.comments" :key="`comment-${comment.id}`" class="comment-item">
                  <img :src="comment.user?.avatar || defaultAvatar" class="comment-avatar">
                  <div class="comment-content">
                    <span class="comment-user">{{ comment.user?.name || '用户' }}</span>
                    <p class="comment-text">{{ comment.text }}</p>
                    <span class="comment-time">{{ formatTime(comment.time) }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div v-else class="empty-state">
            <div class="empty-icon">📝</div>
            <p>暂无动态内容</p>
            <button @click="fetchData">刷新</button>
          </div>
        </template>
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
          <span v-if="currentPage < totalPages" class="page-total">总共{{ totalPages }}页</span>
        </div>

        <!-- 添加跳转页面功能 -->
        <div class="page-jump">
          <span class="jump-text">跳转到</span>
          <input v-model.number="jumpPageInput" type="number" min="1" :max="totalPages" class="jump-input"
            @keyup.enter="jumpToPage" @blur="jumpToPage" />
          <span class="jump-text">页</span>
        </div>

        <button class="page-btn" :disabled="currentPage === totalPages" @click="changePage(currentPage + 1)">
          下一页
        </button>
      </div>

    </div>

    <!-- 模态框 -->
    <LightBox2 v-if="showLightbox" ref="lightbox" :mediaItems="mediaItems" :initialIndex="selectedIndex"
      @close="showLightbox = false" />
    <!-- 模态框部分修改 -->
    <!-- 在模板中替换原来的 VideoPlayer -->
    <VideoModal v-if="showVideoPlayer" :show="showVideoPlayer" :video="selectedVideo"
      @close="showVideoPlayer = false" />
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import { useUserInfoStore } from '@/stores/userInfo.js'
import VideoModal from '../views/VideoModal.vue'
import LightBox2 from '../views/LightBox2.vue'
import { ElMessage } from 'element-plus'
import {
  getAllStatusMediaByUserService,
  toggleLikeService,
  getUserActiveMediaService,
  getUserPendingMediaService,
  getUserHiddenMediaService
} from '@/api/media'
import {
  getAllStatusSubmitByUserService,
  toggleLikeSubmitService,
  getUserActiveSubmitService,
  getUserPendingSubmitService,
  getUserHiddenSubmitService
} from '@/api/submit'

// 默认头像
const defaultAvatar = 'https://i.pravatar.cc/50?img=0'
const lightbox = ref(null)
const userStore = useUserInfoStore()

// Props 定义 - 接收 userData 对象
const props = defineProps({
  userData: {
    type: Object,
    required: true,
    default: () => ({})
  }
})


// 计算属性：从 userData 中获取 userId
const userId = computed(() => {
  return props.userData?.id || userStore.currentUser.id || ''
})

// 内容类型管理
const contentType = ref('photos')
const currentStatus = ref('all') // all, active, pending
const orderBy = ref('upload_time')
const order = ref('DESC')

// 定义 tabs 变量
const tabs = ref([
  {
    value: 'photos',
    label: '图片',
    icon: 'iconfont icon-tupian'
  },
  {
    value: 'videos',
    label: '视频',
    icon: 'iconfont icon-shipin'
  },
  {
    value: 'posts',
    label: '动态',
    icon: 'iconfont icon-tiezi'
  }
])

// 计算滑动条位置
const sliderStyle = computed(() => {
  const activeIndex = tabs.value.findIndex(tab => tab.value === contentType.value)
  const width = 100 / tabs.value.length
  const left = activeIndex * width

  return {
    width: `${width}%`,
    left: `${left}%`,
  }
})

// 模态框控制
const showLightbox = ref(false)
const showVideoPlayer = ref(false)
const selectedItem = ref(null)
const selectedVideo = ref(null)

// 加载状态
const isLoading = ref(false)
const error = ref(null)

// 数据存储
const allPhotos = ref([])
const allVideos = ref([])

// 新增：分页相关状态
const currentPage = ref(1)
const pageSize = ref(20) // 每页显示数量
const totalItems = ref(0) // 总条目数
const totalPages = ref(0) // 总页数
// 新增：跳转页面输入框的状态
const jumpPageInput = ref('')

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


// 统计信息
const stats = ref({
  total: 0,
  active: 0,
  pending: 0,
  photos: 0,
  videos: 0,
  posts: 0
})

// 计算当前显示的内容
const currentContent = computed(() => {
  if (contentType.value === 'photos') {
    return allPhotos.value
  } else if (contentType.value === 'videos') {
    return allVideos.value
  }
  return []
})

// 切换内容类型
const switchContentType = async (type) => {
  console.log('切换内容类型:', type, '当前类型:', contentType.value)

  if (contentType.value === type) {
    console.log('已经是当前类型，无需切换')
    return
  }

  contentType.value = type

  // 重置状态和排序
  if (type !== 'posts') {
    currentStatus.value = 'all'
    orderBy.value = 'upload_time'
    order.value = 'DESC'
    currentPage.value = 1

    // 确保有用户ID
    if (userId.value) {
      console.log('开始获取媒体数据，用户ID:', userId.value)
      await fetchMediaData()
    } else {
      console.warn('用户ID为空，无法获取媒体数据')
    }
  } else {
    // 切换到动态标签
    console.log('切换到动态标签')

    // 清除图片视频数据
    allPhotos.value = []
    allVideos.value = []

    // 获取动态数据
    await fetchPostsData()
  }
}

// 切换状态筛选
const changeStatus = async (status) => {
  if (currentStatus.value === status) return

  currentStatus.value = status
  currentPage.value = 1

  if (contentType.value === 'photos' || contentType.value === 'videos') {
    await fetchMediaData()
  }
}

// 新增：分页变化处理
const changePage = (page) => {
  // 验证页面范围
  if (page < 1 || page > totalPages.value || page === currentPage.value) return

  currentPage.value = page
  fetchMediaData()

  // 滚动到顶部
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// 获取媒体数据
// 在 fetchMediaData 方法中添加更详细的日志
const fetchMediaData = async () => {
  if (!userId.value || (contentType.value !== 'photos' && contentType.value !== 'videos')) return

  try {
    isLoading.value = true
    error.value = null

    const params = {
      userId: parseInt(userId.value), // 确保是数字类型
      mediaType: contentType.value === 'photos' ? 'image' : 'video',
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      orderBy: orderBy.value,
      order: order.value
    }

    console.log('请求参数:', params)

    let response1
    let response2

    if (currentStatus.value === 'all') {
      console.log('调用 getAllStatusMediaByUserService和getAllStatusSubmitByUserService')
      response1 = await getAllStatusMediaByUserService(params)
      response2 = await getAllStatusSubmitByUserService(params)
    } else if (currentStatus.value === 'active') {
      params.status = currentStatus.value
      console.log('调用 getUserActiveMediaService')
      response1 = await getUserActiveMediaService(params.userId, params)
      response2 = await getUserActiveSubmitService(params.userId, params)
    } else if (currentStatus.value === 'pending') {
      params.status = currentStatus.value
      console.log('调用 getUserPendingMediaService')
      response1 = await getUserPendingMediaService(params.userId, params)
      response2 = await getUserPendingSubmitService(params.userId, params)
    } else if (currentStatus.value === 'hidden') {
      params.status = currentStatus.value
      console.log('调用 getUserHiddenMediaService')
      response1 = await getUserHiddenMediaService(params.userId, params)
      response2 = await getUserHiddenSubmitService(params.userId, params)
    }

    console.log('API响应:', response1, response2)

    if (response1 || response2) {
      console.log('响应数据:', response1.data, response2.data)
      const mediaList = response1.data.items.concat(response2.data.items)

      console.log(`获取到 ${mediaList.length} 个媒体文件`)

      if (contentType.value === 'photos') {
        allPhotos.value = mediaList
        console.log('图片数据已更新')
      } else if (contentType.value === 'videos') {
        allVideos.value = mediaList
        console.log('视频数据已更新')
      }

      // 更新分页信息
      totalItems.value = response1.data.total + response2.data.total
      totalPages.value = Math.ceil(totalItems.value / pageSize.value / 2)
      // 更新统计信息
      updateStats(mediaList)
      console.log('统计信息已更新:', stats.value)
    } else {
      console.warn('响应数据为空或格式不正确')
      allPhotos.value = []
      allVideos.value = []
    }
  } catch (err) {
    error.value = err
    console.error('获取媒体数据失败详情:')
    console.error('错误信息:', err.message)
    console.error('错误堆栈:', err.stack)

    // 如果是401错误，可能是用户未登录
    if (err.response && err.response.status === 401) {
      ElMessage.error('请先登录后再查看内容')
    } else if (err.response && err.response.status === 404) {
      console.log('用户暂无媒体内容')
      ElMessage.info('用户暂无媒体内容')
      allPhotos.value = []
      allVideos.value = []
    }
  } finally {
    isLoading.value = false
    console.log('=== 数据获取完成 ===')
  }
}
// 更新统计信息
const updateStats = (mediaList) => {
  stats.value.total = totalItems.value
  stats.value.active = mediaList.filter(item => item.status === 'active').length
  stats.value.pending = mediaList.filter(item => item.status === 'pending').length
  stats.value.hidden = mediaList.filter(item => item.status === 'hidden').length

  // 更新类型统计
  if (contentType.value === 'photos') {
    stats.value.photos = mediaList.length
  } else if (contentType.value === 'videos') {
    stats.value.videos = mediaList.length
  }
}

// Lightbox相关
const mediaItems = computed(() => {
  return currentContent.value.map(item => ({
    url: item.filePath,
    alt: item.filename,
    caption: item.description || item.filename,
    ...item
  }))
})

const selectedIndex = computed(() => {
  if (!selectedItem.value) return 0
  return currentContent.value.findIndex(item => item.id === selectedItem.value.id)
})

const openLightbox = (item) => {
  console.log('播放图片:', item)

  if (item.status === 'pending' && item.userId !== userStore.currentUser.id) {
    ElMessage.warning('该作品正在审核中，暂时无法查看')
    return
  }
  if (item.status === 'hidden' && item.userId !== userStore.currentUser.id) {
    ElMessage.warning('该作品未通过审核，暂时无法查看')
    return
  }
  selectedItem.value = item
  showLightbox.value = true

  nextTick(() => {
    if (lightbox.value) {
      lightbox.value.open(selectedIndex.value)
    }
  })
}

// 点赞/取消点赞
const toggleLike = async (item) => {
  try {
    let response
    //如果category字段中包含'AI'，则调用toggleLikeService，否则调用toggleLikeSubmitService
    if (item.category && item.category.includes('AI')) {
      response = await toggleLikeService(item.id, currentUserId)
    } else {
      response = await toggleLikeSubmitService(item.id, currentUserId)
    }

    if (response && response.code === 0) {
      // 更新本地状态
      item.liked = !item.liked
      if (item.liked) {
        item.likeCount = (item.likeCount || 0) + 1
        ElMessage.success('点赞成功')
      } else {
        item.likeCount = Math.max(0, (item.likeCount || 1) - 1)
        ElMessage.success('取消点赞成功')
      }
    } else {
      ElMessage.error('操作失败，请稍后再试')
    }

  } catch (err) {
    console.error('点赞操作失败:', err)
  }
}

// 格式化工具体方法
const formatDuration = (seconds) => {
  if (!seconds && seconds !== 0) return '00:00'

  // 确保是数字类型
  let secs
  if (typeof seconds === 'string') {
    secs = parseFloat(seconds)
    if (isNaN(secs)) {
      return '00:00'
    }
  } else if (typeof seconds === 'number') {
    secs = seconds
  } else {
    return '00:00'
  }

  const hours = Math.floor(secs / 3600)
  const minutes = Math.floor((secs % 3600) / 60)
  const secsPart = Math.floor(secs % 60)

  if (hours > 0) {
    return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${secsPart.toString().padStart(2, '0')}`
  } else {
    return `${minutes.toString().padStart(2, '0')}:${secsPart.toString().padStart(2, '0')}`
  }
}

const playVideo = (item) => {
  console.log('播放视频:', item) // 添加日志查看数据

  if (item.status === 'pending' && item.userId !== userStore.currentUser.id) {
    ElMessage.warning('该作品正在审核中，暂时无法查看')
    return
  }
  if (item.status === 'hidden' && item.userId !== userStore.currentUser.id) {
    ElMessage.warning('该作品未通过审核，暂时无法查看')
    return
  }
  if (item.status === 'active' || item.userId === userStore.currentUser.id) {
    // 确保路径正确
    if (!item.filePath) {
      ElMessage.error('视频路径不存在')
      return
    }

    selectedVideo.value = {
      ...item,
      // 确保路径是完整 URL
      filePath: item.filePath.startsWith('http') ? item.filePath : `/api/${item.filePath}`,
      thumbnailPath: item.thumbnailPath || item.poster || ''
    }

    showVideoPlayer.value = true
    console.log('打开视频播放器，视频路径:', selectedVideo.value.filePath)
  } else {
    ElMessage.warning(item.status === 'pending' ? '视频正在审核中，暂无法播放' : '视频未通过审核')
  }
}

// 监听用户ID变化
watch(userId, (newUserId) => {
  if (newUserId) {
    fetchMediaData()
  }
}, { immediate: true })

// 监听排序变化
watch([orderBy, order], () => {
  if (userId.value && (contentType.value === 'photos' || contentType.value === 'videos')) {
    fetchMediaData()
  }
})

// 在组件中添加模拟动态数据
const posts = ref([
  {
    id: 1001,
    text: '今天终于完成了我的摄影作品集，包含了春夏秋冬四季的樱花照片！从含苞待放到落英缤纷，每一张都记录着时间的流转和生命的美好。希望能通过这些照片，让大家感受到春天的气息🌸',
    likes: 245,
    comments: [
      {
        id: 100101,
        user: {
          name: '樱花爱好者',
          avatar: 'https://i.pravatar.cc/50?img=3'
        },
        text: '太美了！特别喜欢那张樱花飘落的照片，抓拍得真好！',
        time: '2023-04-15 14:30'
      },
      {
        id: 100102,
        user: {
          name: '摄影达人',
          avatar: 'https://i.pravatar.cc/50?img=5'
        },
        text: '构图很有感觉，后期调色也很舒服，学习了！',
        time: '2023-04-15 15:45'
      }
    ],
    time: '2023-04-15 10:20',
    author: {
      name: '樱花少女',
      avatar: 'https://i.pravatar.cc/50?img=1'
    },
    userId: 'AC123456',
    isLiked: true
  },
  {
    id: 1002,
    text: '刚上传了一段樱花盛开的延时摄影视频，用了一个下午的时间拍摄，记录了樱花从微开到全盛的过程。虽然有点累，但看到成片效果还是很值得的！🎬',
    likes: 189,
    comments: [
      {
        id: 100201,
        user: {
          name: '视频创作者',
          avatar: 'https://i.pravatar.cc/50?img=7'
        },
        text: '延时摄影做得太棒了！能分享下拍摄参数吗？',
        time: '2023-04-14 16:20'
      }
    ],
    time: '2023-04-14 13:45',
    author: {
      name: '樱花少女',
      avatar: 'https://i.pravatar.cc/50?img=1'
    },
    userId: 'AC123456',
    isLiked: false
  },
  {
    id: 1003,
    text: '分享一些摄影小技巧：拍摄樱花时，可以利用低角度仰拍，让樱花和天空形成对比，效果会很惊艳！另外，清晨和傍晚的光线最适合拍摄樱花，光线柔和色彩饱满。',
    likes: 312,
    comments: [
      {
        id: 100301,
        user: {
          name: '摄影小白',
          avatar: 'https://i.pravatar.cc/50?img=9'
        },
        text: '感谢分享！明天就去试试低角度拍摄！',
        time: '2023-04-13 11:10'
      },
      {
        id: 100302,
        user: {
          name: '风景摄影师',
          avatar: 'https://i.pravatar.cc/50?img=11'
        },
        text: '补充一点，雨后拍摄樱花也很有感觉，花瓣上的水珠很漂亮！',
        time: '2023-04-13 12:30'
      },
      {
        id: 100303,
        user: {
          name: '樱花控',
          avatar: 'https://i.pravatar.cc/50?img=13'
        },
        text: '学到了，今年的樱花季一定要好好拍！',
        time: '2023-04-13 14:15'
      }
    ],
    time: '2023-04-13 09:30',
    author: {
      name: '樱花少女',
      avatar: 'https://i.pravatar.cc/50?img=1'
    },
    userId: 'AC123456',
    isLiked: true
  },
  {
    id: 1004,
    text: '最近在研究视频剪辑，发现用慢动作拍摄樱花飘落特别有感觉，配合轻柔的背景音乐，整个视频都充满了诗意。推荐大家试试看！',
    likes: 167,
    comments: [],
    time: '2023-04-12 17:20',
    author: {
      name: '樱花少女',
      avatar: 'https://i.pravatar.cc/50?img=1'
    },
    userId: 'AC123456',
    isLiked: false
  },
  {
    id: 1005,
    text: '今年的樱花比往年开得晚一些，但正因为等待的时间更长，看到盛开的樱花时更加感动。生命中的美好值得等待，就像这迟来的樱花，依然绚烂。',
    likes: 278,
    comments: [
      {
        id: 100501,
        user: {
          name: '文艺青年',
          avatar: 'https://i.pravatar.cc/50?img=15'
        },
        text: '说得真好！美的东西值得等待。',
        time: '2023-04-11 20:45'
      }
    ],
    time: '2023-04-11 18:15',
    author: {
      name: '樱花少女',
      avatar: 'https://i.pravatar.cc/50?img=1'
    },
    userId: 'AC123456',
    isLiked: true
  },
  {
    id: 1006,
    text: '刚刚整理了一下设备清单：相机：Sony A7III，镜头：24-70mm f2.8，无人机：DJI Mini 3 Pro。这些都是我拍摄樱花常用的设备，各有各的用途。大家有什么好用的设备推荐吗？',
    likes: 156,
    comments: [
      {
        id: 100601,
        user: {
          name: '器材党',
          avatar: 'https://i.pravatar.cc/50?img=17'
        },
        text: '推荐85mm定焦，拍人像樱花特别棒！',
        time: '2023-04-10 15:30'
      },
      {
        id: 100602,
        user: {
          name: '摄影老鸟',
          avatar: 'https://i.pravatar.cc/50?img=19'
        },
        text: '可以考虑加个微距镜头，拍樱花细节很惊艳。',
        time: '2023-04-10 16:45'
      }
    ],
    time: '2023-04-10 14:00',
    author: {
      name: '樱花少女',
      avatar: 'https://i.pravatar.cc/50?img=1'
    },
    userId: 'AC123456',
    isLiked: false
  },
  {
    id: 1007,
    text: '发现了一个新的樱花拍摄地点，人少景美，而且还有一片很美的樱花隧道。今天拍了一整天，收获满满！晚点整理好照片就分享给大家。',
    likes: 89,
    comments: [
      {
        id: 100701,
        user: {
          name: '探店达人',
          avatar: 'https://i.pravatar.cc/50?img=21'
        },
        text: '求地址！我也想去看看！',
        time: '2023-04-09 21:10'
      }
    ],
    time: '2023-04-09 19:30',
    author: {
      name: '樱花少女',
      avatar: 'https://i.pravatar.cc/50?img=1'
    },
    userId: 'AC123456',
    isLiked: true
  },
  {
    id: 1008,
    text: '尝试用AI工具给樱花照片做了些艺术效果处理，没想到效果还不错！现代科技和传统美景的结合，创造出了不一样的美感。',
    likes: 203,
    comments: [
      {
        id: 100801,
        user: {
          name: '科技爱好者',
          avatar: 'https://i.pravatar.cc/50?img=23'
        },
        text: '用的是什么AI工具啊？求推荐！',
        time: '2023-04-08 10:45'
      },
      {
        id: 100802,
        user: {
          name: '传统摄影师',
          avatar: 'https://i.pravatar.cc/50?img=25'
        },
        text: '虽然科技很好，但我觉得自然的美才是最美的。',
        time: '2023-04-08 11:30'
      }
    ],
    time: '2023-04-08 09:15',
    author: {
      name: '樱花少女',
      avatar: 'https://i.pravatar.cc/50?img=1'
    },
    userId: 'AC123456',
    isLiked: false
  }
])

// 修改 filteredPosts 计算属性
const filteredPosts = computed(() => {
  return props.userId
    ? posts.value.filter(post => post.userId === props.userId)
    : posts.value
})

// 修改 fetchPostsData 方法
const fetchPostsData = async () => {
  console.log('开始获取动态数据')
  isLoading.value = true

  try {
    // 如果已经有模拟数据，就直接使用
    if (posts.value.length > 0) {
      console.log('使用现有动态数据:', posts.value.length, '条')
      return
    }

    // 这里可以添加获取动态数据的API调用
    // 暂时使用新的模拟数据
    const mockPosts = [
      {
        id: Date.now(),
        text: '欢迎来到我的个人空间！这里记录着我的创作和生活点滴。',
        likes: 42,
        comments: [
          {
            id: Date.now() + 1,
            user: {
              name: '访客',
              avatar: 'https://i.pravatar.cc/50?img=2'
            },
            text: '空间布置得很有品味！',
            time: new Date().toISOString()
          }
        ],
        time: new Date().toISOString(),
        author: {
          name: userStore.currentUser?.nickname || userStore.currentUser?.username || '我',
          avatar: userStore.currentUser?.avatar || defaultAvatar
        },
        userId: userId.value,
        isLiked: false
      }
    ]

    posts.value = mockPosts
    console.log('动态数据获取成功:', posts.value.length, '条')
  } catch (error) {
    console.error('获取动态数据失败:', error)
    error.value = error
  } finally {
    isLoading.value = false
  }
}

// 修改 fetchData 方法
const fetchData = () => {
  console.log('刷新数据，当前内容类型:', contentType.value)

  if (contentType.value === 'photos' || contentType.value === 'videos') {
    fetchMediaData()
  } else if (contentType.value === 'posts') {
    fetchPostsData()
  }
}

// 添加动态的点赞功能
const likePost = (post) => {
  post.isLiked = !post.isLiked
  if (post.isLiked) {
    post.likes = (post.likes || 0) + 1
  } else {
    post.likes = Math.max(0, (post.likes || 1) - 1)
  }

  // 这里可以添加API调用
  console.log('点赞动态:', post.id, '当前点赞数:', post.likes)
}

// 添加评论功能
const commentPost = (post) => {
  const commentText = prompt('请输入评论内容：')
  if (commentText && commentText.trim()) {
    const newComment = {
      id: Date.now(),
      user: {
        name: userStore.currentUser?.nickname || userStore.currentUser?.username || '我',
        avatar: userStore.currentUser?.avatar || defaultAvatar
      },
      text: commentText.trim(),
      time: new Date().toISOString()
    }

    post.comments = post.comments || []
    post.comments.push(newComment)

    console.log('添加评论:', newComment)
  }
}

// 添加分享功能
const sharePost = (post) => {
  const shareUrl = `${window.location.origin}/post/${post.id}`
  navigator.clipboard.writeText(shareUrl)
    .then(() => {
      alert('动态链接已复制到剪贴板！')
    })
    .catch(err => {
      console.error('复制失败:', err)
      alert('复制失败，请手动复制链接')
    })

  console.log('分享动态:', post.id)
}

// 修改时间格式化函数
const formatTime = (time) => {
  if (!time) return '未知时间'

  const date = new Date(time)

  // 如果是今天
  const today = new Date()
  if (date.toDateString() === today.toDateString()) {
    return `今天 ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
  }

  // 如果是昨天
  const yesterday = new Date(today)
  yesterday.setDate(yesterday.getDate() - 1)
  if (date.toDateString() === yesterday.toDateString()) {
    return `昨天 ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
  }

  // 一周内
  const weekAgo = new Date(today)
  weekAgo.setDate(weekAgo.getDate() - 7)
  if (date > weekAgo) {
    const days = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
    return `${days[date.getDay()]} ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
  }

  // 更早的时间
  return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')} ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
}
</script>

<style scoped>
/* ========== 1. 动态渐变风格分类标签 ========== */
.content-type-tabs-gradient {
  background: linear-gradient(135deg,
      rgba(255, 240, 245, 0.95) 0%,
      rgba(255, 248, 250, 0.95) 100%);
  backdrop-filter: blur(10px);
  position: relative;
  overflow: hidden;
  border-bottom: 1px solid rgba(255, 105, 180, 0.1);
}

.content-type-tabs-gradient::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg,
      transparent,
      rgba(255, 105, 180, 0.3),
      transparent);
}

.gradient-tabs {
  display: flex;
  position: relative;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 50px;
  box-shadow:
    0 8px 32px rgba(255, 105, 180, 0.15),
    inset 0 1px 2px rgba(255, 255, 255, 0.8),
    0 0 0 1px rgba(255, 105, 180, 0.1);
}

.tabs-slider {
  position: absolute;
  height: calc(100% - 0.1px);
  background: linear-gradient(135deg,
      rgba(238, 26, 210, 0.95) 0%,
      rgba(15, 235, 235, 0.95) 100%);
  border-radius: 40px;
  transition: all 0.4s cubic-bezier(0.68, -0.55, 0.265, 1.55);
  box-shadow:
    0 4px 20px rgba(255, 105, 180, 0.25),
    inset 0 1px 2px rgba(255, 255, 255, 0.5);
  z-index: 1;
}

.gradient-tab {
  flex: 1;
  position: relative;
  z-index: 2;
  cursor: pointer;
  text-align: center;
  transition: all 0.3s ease;
}

.tab-glow {
  position: absolute;
  inset: 8px;
  border-radius: 40px;
  background: linear-gradient(135deg,
      rgba(255, 105, 180, 0) 0%,
      rgba(255, 105, 180, 0.15) 100%);
  opacity: 0;
  transition: opacity 0.3s;
}

.gradient-tab:hover .tab-glow {
  opacity: 1;
}

.tab-inner {
  position: relative;
  z-index: 3;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  transition: all 0.3s;
}

.gradient-tab i {
  font-size: 24px;
  color: #666;
  transition: all 0.4s cubic-bezier(0.68, -0.55, 0.265, 1.55);
}

.gradient-tab:hover i {
  transform: translateY(-2px) scale(1.1);
  color: #ff69b4;
}


.gradient-tab.active i {
  color: white;
  transform: translateY(-3px) scale(1.2);
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.2));
}

.tab-name {
  font-size: 16px;
  font-weight: 500;
  color: #666;
  transition: all 0.3s;
}

.gradient-tab.active .tab-name {
  color: white;
  font-weight: 600;
  transform: translateY(2px);
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}

.tab-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: rgba(255, 105, 180, 0.2);
  opacity: 0;
  transform: scale(0);
  transition: all 0.3s;
}

.gradient-tab.active .tab-dot {
  opacity: 1;
  transform: scale(1);
  background: white;
  box-shadow: 0 0 8px rgba(255, 255, 255, 0.5);
}

@keyframes float {

  0%,
  100% {
    transform: translateY(0) rotate(0deg);
  }

  50% {
    transform: translateY(-10px) rotate(180deg);
  }
}

.gradient-tabs::after {
  content: '🌸';
  position: absolute;
  top: -20px;
  left: 20%;
  font-size: 20px;
  opacity: 0.3;
  animation: float 6s ease-in-out infinite;
}

.gradient-tabs::before {
  content: '🌸';
  position: absolute;
  top: -15px;
  right: 30%;
  font-size: 16px;
  opacity: 0.3;
  animation: float 8s ease-in-out infinite reverse;
}

/* ========== 2. 状态筛选区域 ========== */
.status-filter {
  padding: 15px 20px;
  background: white;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #eee;
}

.filter-tabs {
  display: flex;
  gap: 8px;
}

.filter-tabs button {
  padding: 8px 16px;
  border: 1px solid #ddd;
  background: #f8f9fa;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.filter-tabs button.active {
  background: linear-gradient(135deg, #ff69b4 0%, #ff8ebd 100%);
  color: white;
  border-color: #ff69b4;
  box-shadow: 0 4px 12px rgba(255, 105, 180, 0.2);
}

.filter-tabs button:hover:not(.active) {
  background: rgba(255, 105, 180, 0.1);
  border-color: #ff8ebd;
}

.stats-info {
  display: flex;
  gap: 20px;
  font-size: 14px;
  color: #666;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 6px 12px;
  background: rgba(217, 149, 238, 0.3);
  color: #d88db3;
  border-radius: 20px;
}

.stat-item.active {
  background: rgba(40, 167, 69, 0.1);
  color: #28a745;
}

.stat-item.pending {
  background: rgba(255, 193, 7, 0.1);
  color: #ffc107;
}
.stat-item.hidden {
  background: rgba(255, 99, 132, 0.1);
  color: #dc3545;
}

/* ========== 3. 主内容区域 ========== */
.content-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #fff9f9;
  border-radius: 16px;
  margin: 10px 20px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(255, 105, 180, 0.1);
}

.content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 25px;
  background: white;
  border-bottom: 1px solid #f0f0f0;
}

.content-header h2 {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  color: #333;
  margin: 0;
  font-weight: 600;
}

.content-count {
  font-size: 14px;
  color: #ff69b4;
  font-weight: normal;
  margin-left: 8px;
  background: rgba(255, 105, 180, 0.1);
  padding: 4px 10px;
  border-radius: 12px;
}

.sort-options {
  display: flex;
  gap: 10px;
}

.sort-options select {
  padding: 8px 16px;
  border: 1px solid #ddd;
  border-radius: 20px;
  background: white;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
}

.sort-options select:hover {
  border-color: #ff8ebd;
}

.sort-options select:focus {
  outline: none;
  border-color: #ff69b4;
  box-shadow: 0 0 0 3px rgba(255, 105, 180, 0.1);
}

/* ========== 4. 滚动容器和网格布局 ========== */
.scroll-container {
  flex: 1;
  overflow-y: auto;
  max-height: calc(100vh - 500px);
  min-height: 300px;
}

.content-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 20px;
  padding: 5px;
}

/* ========== 5. 内容项样式 ========== */
.content-item {
  position: relative;
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background: white;
  box-shadow: 10px 10px 15px rgba(27, 204, 65, 0.7);
  border: 1px solid #f0f0f0;
  height: 300px;
  display: flex;
  flex-direction: column;
}

.content-item:hover {
  transform: translateY(20px);
  box-shadow: 10px 10px 15px rgba(171, 28, 207, 0.7);
  border-color: #ff8ebd;
}

.content-item.pending {
  border: 2px dashed #ffc107;
  background: linear-gradient(135deg, #fff9e6 0%, #fff9f9 100%);
}

.content-image,
.content-video {
  width: 100%;
  height: 160px;
  object-fit: cover;
  border-bottom: 1px solid #f5f5f5;
  transition: transform 0.5s ease;
}

.blur-image {
  filter: blur(4px);
}

.content-item:hover .content-image,
.content-item:hover .content-video {
  transform: scale(1.05);
}

.video-item {
  position: relative;
  overflow: hidden;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  cursor: pointer;
}

.video-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
}

.video-item.pending {
  cursor: not-allowed;
  opacity: 0.7;
}

.video-item.pending:hover {
  transform: none;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.video-thumbnail-wrapper {
  position: relative;
  width: 100%;
  height: 200px;
  overflow: hidden;
  border-radius: 12px 12px 0 0;
}

.content-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  background: #f5f5f5;
}

.content-video.blur-image {
  filter: blur(3px);
}



/* 播放按钮 - 修复版本 */
.play-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: all 0.3s ease;
  z-index: 1;
}

.video-item:hover .play-overlay {
  opacity: 1;
}

.play-button {
  width: 60px;
  height: 60px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
  transition: all 0.3s ease;
}

.video-item:hover .play-button {
  background: white;
  transform: scale(1.1);
  box-shadow: 0 6px 25px rgba(0, 0, 0, 0.4);
}

.play-svg {
  width: 24px;
  height: 24px;
  margin-left: 4px;
  /* 让三角形稍微靠右看起来更居中 */
}

/* 状态标签 */
.status-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  padding: 4px 10px;
  border-radius: 15px;
  font-size: 12px;
  font-weight: 600;
  color: white;
  z-index: 10;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  backdrop-filter: blur(4px);
}

.status-badge.active {
  background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
}

.status-badge.pending {
  background: linear-gradient(135deg, #ffc107 0%, #ffd54f 100%);
}

.status-badge.hidden {
  background: linear-gradient(135deg, #dc3545 0%, #e86a74 100%);
}

.content-meta {
  padding: 12px 15px;
  background: white;
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.meta-info {
  margin-bottom: 8px;
}

.meta-title {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: rgba(233, 69, 165, 0.8);
  text-shadow: 5px 5px 5px rgba(50, 232, 238, 0.7);
  margin-bottom: 6px;
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-box-orient: vertical;
}

.meta-size {
  font-size: 12px;
  color: #666;
  background: #f8f9fa;
  padding: 2px 8px;
  border-radius: 10px;
  display: inline-block;
}

.like-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 2px 5px;
  border: 1px solid #e0e0e0;
  background: white;
  border-radius: 20px;
  cursor: pointer;
  font-size: 13px;
  color: #666;
  transition: all 0.3s;
  align-self: flex-start;
}

.like-btn:hover {
  background: #fff5f5;
  border-color: #ff8ebd;
  color: #ff69b4;
}

.like-btn.liked {
  background: #ff69b4;
  border-color: #ff69b4;
  color: white;
}

.like-btn.liked:hover {
  background: #ff8ebd;
  border-color: #ff8ebd;
}

.public-hidden {
  display: flex;
  margin-top: -20px;
  justify-content: flex-end;
  font-size: 15px;
  font-family: 'Courier New', Courier, monospace;
  font-style: italic;
  color: #f041b6;
  font-weight: bold;
  pointer-events: none;
}

.public-hidden:hover {
  color: #ff69b4;
}

.view-count {
  position: absolute;
  top: 10px;
  left: 10px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 4px 10px;
  border-radius: 15px;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
  z-index: 10;
  backdrop-filter: blur(4px);
}

.video-duration {
  position: absolute;
  bottom: 10px;
  right: 10px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 3px 8px;
  border-radius: 12px;
  font-size: 12px;
  z-index: 10;
  backdrop-filter: blur(4px);
}


/* ========== 6. 分页样式 ========== */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 5px;
  padding: 8px;
  background: white;
  border-top: 1px solid #f0f0f0;
  border-radius: 0 0 16px 16px;
}

.pagination button {
  padding: 5px 10px;
  border: none;
  background: linear-gradient(135deg, #ff69b4 0%, #ff8ebd 100%);
  color: white;
  border-radius: 25px;
  cursor: pointer;
  transition: all 0.3s;
  font-weight: 500;
  font-size: 8px;
  min-width: 50px;
  box-shadow: 0 4px 15px rgba(5, 201, 142, 0.2);
}

.pagination button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 10px rgba(226, 10, 215, 0.8);
}

.pagination button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
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
  background: #84cad3;
  border-color: #fa94c3;
  color: #fa94c3;
}

.page-number.active {
  background: linear-gradient(135deg, #fa94c3 0%, #50b9b4 100%);
  color: rgb(62, 228, 159);
  border-color: transparent;
}

.page-ellipsis {
  color: #999;
  padding: 0 5px;
}

.page-total {
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

/* ========== 7. 空状态和错误状态 ========== */
.empty-state {
  text-align: center;
  padding: 80px 20px;
  color: #999;
  background: white;
  border-radius: 12px;
  margin: 20px;
  border: 2px dashed #eee;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 24px;
  opacity: 0.3;
  animation: float 3s ease-in-out infinite;
}

.error-state {
  text-align: center;
  padding: 5px 1px;
  color: #ff69b4;
  background: white;
  border-radius: 12px;
  border: 2px dashed #ff69b4;
}

.error-icon {
  font-size: 48px;
  margin-bottom: 20px;
  opacity: 0.5;
}

.error-state p {
  font-size: 16px;
  margin-bottom: 20px;
}

.error-state button {
  padding: 10px 24px;
  background: linear-gradient(135deg, #ff69b4 0%, #ff8ebd 100%);
  color: white;
  border: none;
  border-radius: 25px;
  cursor: pointer;
  transition: all 0.3s;
  font-weight: 500;
}

.error-state button:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(255, 105, 180, 0.3);
}

/* ========== 8. 滚动条样式 ========== */
.scroll-container {
  overflow-y: auto;
  height: 700px;
  padding: 20px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.7);
}

.scroll-container::-webkit-scrollbar {
  width: 8px;
}

.scroll-container::-webkit-scrollbar-track {
  background: #f5f5f5;
  border-radius: 4px;
}

.scroll-container::-webkit-scrollbar-thumb {
  background: linear-gradient(135deg, #ff69b4 0%, #ff8ebd 100%);
  border-radius: 4px;
}

.scroll-container::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(135deg, #ff8ebd 0%, #ffb3d9 100%);
}

/* ========== 9. 响应式设计 ========== */
@media (max-width: 1200px) {
  .content-grid {
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
    gap: 16px;
  }
}

@media (max-width: 768px) {
  .content-section {
    margin: 10px;
  }

  .content-type-tabs-gradient {
    padding: 20px 15px 10px;
  }

  .gradient-tab {
    padding: 12px 0;
  }

  .gradient-tab i {
    font-size: 20px;
  }

  .tab-name {
    font-size: 12px;
  }

  .status-filter {
    flex-direction: column;
    gap: 15px;
    align-items: stretch;
  }

  .filter-tabs {
    justify-content: center;
  }

  .stats-info {
    justify-content: center;
    flex-wrap: wrap;
    gap: 10px;
  }

  .content-header {
    flex-direction: column;
    align-items: stretch;
    gap: 15px;
    padding: 15px;
  }

  .content-grid {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 12px;
  }

  .content-item {
    height: 240px;
  }

  .content-image {
    height: 140px;
  }

  .pagination {
    flex-direction: column;
    gap: 15px;
  }

  .page-info {
    order: -1;
  }

  .sort-options {
    justify-content: center;
  }
}

@media (max-width: 480px) {
  .content-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .scroll-container {
    padding: 10px;
  }
}

/* 帖子内容样式 - 完善版 */
.post-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 10px;
}

.post-item {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  border: 1px solid #f0f0f0;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.post-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 32px rgba(255, 105, 180, 0.15);
  border-color: #ff8ebd;
}

.post-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 4px;
  height: 100%;
  background: linear-gradient(to bottom, #ff69b4, #ff8ebd);
  opacity: 0;
  transition: opacity 0.3s;
}

.post-item:hover::before {
  opacity: 1;
}

.post-header {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.post-avatar {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  margin-right: 15px;
  border: 2px solid #ff69b4;
  padding: 2px;
  background: white;
  object-fit: cover;
}

.post-user {
  flex: 1;
}

.post-author {
  font-weight: 600;
  color: #333;
  font-size: 17px;
  display: block;
  margin-bottom: 4px;
}

.post-time {
  font-size: 14px;
  color: #999;
  display: block;
}

.post-content {
  margin-bottom: 20px;
  line-height: 1.7;
  color: #555;
  font-size: 16px;
  padding: 15px;
  background: #f9f9f9;
  border-radius: 12px;
  border-left: 3px solid #ff69b4;
}

.post-actions {
  display: flex;
  gap: 15px;
  border-top: 1px solid #f5f5f5;
  padding-top: 20px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border: 1px solid #e0e0e0;
  background: white;
  border-radius: 25px;
  cursor: pointer;
  font-size: 14px;
  color: #666;
  transition: all 0.3s;
  font-weight: 500;
}

.action-btn:hover {
  background: #fff5f5;
  border-color: #ff8ebd;
  color: #ff69b4;
  transform: translateY(-2px);
}

.action-btn.liked {
  background: linear-gradient(135deg, #ff69b4 0%, #ff8ebd 100%);
  border-color: #ff69b4;
  color: white;
  box-shadow: 0 4px 15px rgba(255, 105, 180, 0.2);
}

.action-btn.liked:hover {
  background: linear-gradient(135deg, #ff8ebd 0%, #ffb3d9 100%);
  border-color: #ff8ebd;
  transform: translateY(-2px) scale(1.05);
}

.action-btn i {
  font-size: 16px;
}

.post-comments {
  margin-top: 25px;
  padding-top: 20px;
  border-top: 1px dashed #eee;
}

.comment-item {
  display: flex;
  align-items: flex-start;
  margin-bottom: 20px;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 12px;
  transition: all 0.3s;
}

.comment-item:hover {
  background: #f0f5ff;
  transform: translateX(5px);
}

.comment-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  margin-right: 12px;
  border: 1px solid #ddd;
  object-fit: cover;
}

.comment-content {
  flex: 1;
}

.comment-user {
  font-weight: 600;
  color: #333;
  font-size: 14px;
  margin-right: 8px;
  display: inline-block;
}

.comment-text {
  margin: 10px 0;
  color: #555;
  font-size: 14px;
  line-height: 1.5;
}

.comment-time {
  font-size: 12px;
  color: #999;
  display: block;
  margin-top: 5px;
}

/* 在主页面或全局 CSS 中添加 */
.clickable {
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
}

.clickable:hover {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.2);
}

.clickable:hover .play-icon {
  opacity: 1;
  transform: scale(1.1);
}

.content-item.video-item.pending {
  cursor: not-allowed;
  opacity: 0.7;
}

.content-item.video-item.pending:hover {
  transform: none;
  box-shadow: none;
}

.play-icon {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 60px;
  height: 60px;
  background: rgba(0, 0, 0, 0.7);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: all 0.3s ease;
  z-index: 10;
}

.play-icon i {
  color: white;
  font-size: 24px;
  margin-left: 5px;
  /* 为了让播放图标居中 */
}

/* 确保视频封面显示播放按钮 */
.content-video {
  position: relative;
  width: 100%;
  height: 200px;
  /* 或其他合适的高度 */
  object-fit: cover;
  border-radius: 8px;
}

</style>