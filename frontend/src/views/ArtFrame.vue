<template>
  <div class="art-frame" @click="handleClick(artwork)">
    <!-- 图片容器添加相对定位 -->
    <div class="media-container">
      <!-- 如果是视频，添加视频覆盖层 -->
      <div class="media-wrapper" :class="{ 'video-wrapper': artwork.mediaType === 'video' }">
        <!-- 如果作品的category字段中包含'AI'，则添加AI标识 -->
        <div v-if="artwork.category.includes('AI')" class="ai-tag">
          <span>AI</span>
        </div>
        <img :src="artwork.mediaType === 'video' ? artwork.thumbnailPath : artwork.image" :alt="artwork.title"
          class="art-image" :class="{ 'blur-image': artwork.status === 'pending' }">

        <!-- 视频播放按钮 -->
        <div v-if="artwork.mediaType === 'video' && artwork.status === 'active'" class="video-play-overlay">
          <div class="video-play-button">
            <svg class="play-icon" viewBox="0 0 24 24">
              <path d="M8 5v14l11-7z" fill="white" />
            </svg>
          </div>
        </div>
      </div>

      <!-- 待审核遮罩层 -->
      <div v-if="artwork.status === 'pending'" class="pending-overlay">
        <span class="pending-text">待审核</span>
      </div>
    </div>

    <div class="art-info">
      <h3 class="art-title">{{ artwork.title }}</h3>
      <span class="art-artist">{{ artwork.artist }}</span>
      <div class="art-meta">
        <span class="art-type">{{ artwork.mediaType === 'video' ? '视频' : '图片' }}</span>
        <span class="art-year">{{ artwork.year }}</span>
      </div>
      <div class="like-container">
        <span @click.stop="toggleLike" class="like-icon" :class="{ 'liked': liked }"
          :title="liked ? '取消喜欢' : '喜欢'">❤</span>
        <span class="like-count">{{ likes }}</span>
        <span v-if="artwork.userId === userStore.currentUser.id" class="public-hidden">
          {{ artwork.isPublic ? '公开' : '隐私' }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useUserInfoStore } from '@/stores/userInfo'
import { toggleLikeService } from '@/api/media'
import { toggleLikeSubmitService } from '@/api/submit'
import { ElMessage } from 'element-plus'

const props = defineProps({
  artwork: {
    type: Object,
    required: true
  }
})

// 使用ref管理状态，避免直接修改props
const liked = ref(props.artwork.liked)
const likes = ref(props.artwork.likes || 0)

// 监听props变化，确保状态同步
watch(() => props.artwork, (newArtwork) => {
  if (newArtwork) {
    liked.value = newArtwork.liked
    likes.value = newArtwork.likes || 0
  }
}, { immediate: true })

const toggleLike = async () => {
  try {
    if (!props.artwork?.id || !props.artwork?.userId) {
      throw new Error('缺少必要的作品或用户信息')
    }

    console.log('操作作品:', props.artwork.id, '用户:', props.artwork.userId)
    
    // 根据category决定调用哪个API
    const isAI = props.artwork.category.includes('AI')
    const service = isAI ? toggleLikeService : toggleLikeSubmitService
    
    const response = await service(props.artwork.id, props.artwork.userId)
    
    if (response?.code === 0) {
      // 更新本地状态
      liked.value = !liked.value
      likes.value = liked.value ? likes.value + 1 : Math.max(0, likes.value - 1)
      
      ElMessage.success(liked.value ? '点赞成功' : '取消点赞成功')
    } else {
      throw new Error(response?.msg || '操作失败')
    }
  } catch (error) {
    console.error('点赞操作出错:', error)
    ElMessage.error(error.message || '操作失败，请稍后重试')
  }
}

const emit = defineEmits(['click'])
const userStore = useUserInfoStore()

// 点击事件处理函数
const handleClick = (artwork) => {
  // 如果作品状态为'pending'，则显示警告消息并返回
  if (artwork.status === 'pending') {
    ElMessage.warning('该作品正在审核中，暂时无法查看')
    return
  }
  emit('click', artwork)
}

</script>

<style scoped>
/* 保持原有样式不变 */
.art-frame {
  position: relative;
  height: 460px;
  width: 100%;
  border: 10px solid #dfb6b6;
  background: #ffffff;
  box-shadow: 0 10px 15px rgba(78, 240, 86, 0.5);
  transition: all 0.3s ease;
  cursor: pointer;
  overflow: hidden;
}

.art-frame:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 20px rgba(209, 146, 146, 0.2);
}

/* 媒体容器样式 */
.media-container {
  position: relative;
  height: 350px;
  width: 100%;
  overflow: hidden;
}

.media-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
}

.ai-tag {
  position: absolute;
  top: 10px;
  left: 10px;
  background-color: rgba(255, 0, 0, 0.7);
  box-shadow: 5px 5px 8px rgba(6, 240, 96, 0.8);
  color: white;
  padding: 6px 10px;
  border-radius: 4px;
  font-size: 12px;
}

/* 视频包装器 */
.video-wrapper {
  cursor: pointer;
}

.video-wrapper:hover .video-play-overlay {
  opacity: 1;
}

/* 视频播放覆盖层 */
.video-play-overlay {
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
  transition: opacity 0.3s ease;
}

.video-play-button {
  width: 60px;
  height: 60px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid rgba(255, 255, 255, 0.3);
  transition: all 0.3s ease;
}

.video-wrapper:hover .video-play-button {
  background: rgba(54, 196, 221, 0.7);
  transform: scale(1.1);
  border-color: rgba(255, 255, 255, 0.5);
}

.play-icon {
  width: 24px;
  height: 24px;
  margin-left: 4px;
}

.art-image {
  height: 100%;
  width: 100%;
  object-fit: cover;
  display: block;
  transition: filter 0.3s ease;
}

.blur-image {
  filter: blur(4px);
}

.pending-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: not-allowed;
}

.pending-text {
  color: rgb(18, 187, 253);
  font-size: 24px;
  font-weight: bold;
  font-family: 'Courier New', Courier, monospace;
  font-style: italic;
  text-shadow: 2px 4px 4px rgba(243, 43, 43, 0.8);
  background-color: rgba(0, 0, 0, 0.7);
  padding: 8px 16px;
  border-radius: 4px;
}

.art-info {
  width: auto;
  padding: 10px;
  background: white;
  opacity: 0.5;
}

.art-title {
  margin: 0;
  font-size: 12px;
  font-weight: bold;
  font-family: 'Courier New', Courier, monospace;
  font-style: italic;
  color: #ee1be3;
  text-shadow: 1px 1px 1px #2fc6e0;
}

.art-meta {
  display: flex;
  justify-content: space-between;
  margin-top: 5px;
  font-size: 12px;
}

.art-artist {
  font-size: 14px;
  font-weight: bold;
  font-family: 'Courier New', Courier, monospace;
  color: #e963c1;
}

.art-year {
  font-weight: bold;
  font-family: 'Courier New', Courier, monospace;
  font-style: italic;
  color: #1511ee;
}

.art-type {
  font-weight: bold;
  font-family: 'Courier New', Courier, monospace;
  font-style: italic;
  color: #1511ee;
}

.like-container {
  display: flex;
  align-items: center;
  margin-top: 8px;
  cursor: pointer;
}

.like-icon {
  font-size: 18px;
  color: #b2c5b7;
  transition: all 0.3s ease;
}

.like-icon:hover {
  transform: scale(1.3);
}

.like-icon.liked {
  color: #f00;
}

.like-count {
  margin-left: 5px;
  font-size: 14px;
  color: #3d033d;
}

.public-hidden {
  margin-left: 200px;
  font-size: 15px;
  color: #f53ef5;
}
</style>