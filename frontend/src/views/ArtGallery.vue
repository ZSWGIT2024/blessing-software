<template>
  <div class="gallery-container">
    <!-- 左侧导航栏 -->
    <div class="sidebar">
      <div v-for="(item, index) in navItems" :key="index" class="nav-item"
        :class="{ active: currentRoute === item.route }" @click="navigateTo(item.route)">
        {{ item.title }}
        <span class="hover-effect"></span>
      </div>
    </div>

    <!-- 顶部排序选项 -->
    <div class="sort-options">
      <button class="sort-btn" :class="{ active: sortBy === 'latest' }" @click="changeSort('latest')">
        最新
      </button>
      <button class="sort-btn" :class="{ active: sortBy === 'popular' }" @click="changeSort('popular')">
        人气
      </button>
    </div>

    <!-- 返回按钮 -->
    <button class="back-button" @click="goBack">
      <i class="arrow-icon">←</i> 返回主页
    </button>

    <!-- 画廊主体 -->
    <div class="gallery-hall">
      <!-- 左侧画框 -->
      <div class="gallery-wall left-wall">
        <ArtFrame v-for="(art, index) in sortedLeftArts" :key="'left-' + art.id" :artwork="art"
          @click="openLightbox(art)" @like="handleLike" />
      </div>

      <!-- 画廊走廊 -->
      <div class="gallery-corridor">
        <div class="scrollBox">
          <h3> 路人女主的养成方法 相关作品展示</h3>
        </div>
      </div>

      <!-- 右侧画框 -->
      <div class="gallery-wall right-wall">
        <ArtFrame v-for="(art, index) in sortedRightArts" :key="'right-' + art.id" :artwork="art"
          @click="openLightbox(art)" @like="handleLike" />
      </div>
    </div>

    <!-- 图片查看器 -->
    <!-- 在template部分修改Lightbox的使用方式 -->
    <Lightbox v-if="showLightbox" :images="sortedArts" :current-index="currentIndex" @close="closeLightbox"
      @change-image="newIndex => currentIndex = newIndex" />
  </div>

</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import ArtFrame from './ArtFrame.vue'
import Lightbox from './Lightbox.vue'

const router = useRouter()
const route = useRoute()
const currentIndex = ref(0)
// 导航项
const navItems = [
  { title: '路人女主专区', route: '/gallery' },
  { title: 'AI绘画', route: '/AIGallery' },
  { title: '网络作品', route: '/userSubmission' }
]

// 排序方式
const sortBy = ref('latest') // 默认按最新排序

// 当前路由
const currentRoute = computed(() => route.path)

// 示例艺术作品数据
const artworks = ref([
  {
    id: 1,
    title: '加藤惠',
    artist: '未知艺术家',
    image: '/images/x (1).jpg',
    year: '2023',
    category: '二次元',
    description: '路人女主的养成方法',
    wall: 'left'
  },
  {
    id: 2,
    title: '秋叶飘落',
    artist: '未知艺术家',
    image: '/images/x (1).png',
    year: '2023',
    category: '二次元',
    description: '路人女主的养成方法',
    wall: 'left'
  },
  {
    id: 3,
    title: '冬日雪景',
    artist: '未知艺术家',
    image: '/images/x (2).png',
    year: '2023',
    category: '二次元',
    description: '路人女主的养成方法',
    wall: 'left'
  },
  {
    id: 4,
    title: '星空夜',
    artist: '未知艺术家',
    image: '/images/x (3).png',
    year: '2023',
    category: '二次元',
    description: '路人女主的养成方法',
    wall: 'left'
  },
  {
    id: 5,
    title: '日出东方',
    artist: '未知艺术家',
    image: '/images/x (4).png',
    year: '2023',
    category: '二次元',
    description: '路人女主的养成方法',
    wall: 'left'
  },
  // 可以添加更多艺术作品...
  {
    id: 6,
    title: '山水之间',
    artist: '未知艺术家',
    image: '/images/x (5).png',
    year: '2022',
    category: '二次元',
    description: '路人女主的养成方法',
    wall: 'left'
  },
  {

    id: 7,
    title: '四季变化',
    artist: '未知艺术家',
    image: '/images/x (6).png',
    year: '2022',
    category: '二次元',
    description: '路人女主的养成方法',
    wall: 'left'
  },
  {
    id: 8,
    title: '星空下的思考',
    artist: '未知艺术家',
    image: '/images/x (7).png',
    year: '2022',
    category: '二次元',
    description: '路人女主的养成方法',
    wall: 'left'
  },
  {
    id: 9,
    title: '日出东方的希望',
    artist: '未知艺术家',
    image: '/images/x (8).png',
    year: '2022',
    category: '二次元',
    description: '路人女主的养成方法',
    wall: 'left'
  },
  {
    id: 10,
    title: '山水之间的和谐',
    artist: '未知艺术家',
    image: '/images/x (9).png',
    year: '2022',
    category: '二次元',
    description: '路人女主的养成方法',
    wall: 'right'
  },
  {
    id: 10,
    title: '山水之间的和谐',
    artist: '未知艺术家',
    image: '/images/x (10).png',
    year: '2022',
    category: '二次元',
    description: '路人女主的养成方法',
    wall: 'right'
  },
  {
    id: 11,
    title: '山水之间的和谐',
    artist: '未知艺术家',
    image: '/images/x (11).png',
    year: '2022',
    category: '二次元',
    description: '路人女主的养成方法',
    wall: 'right'
  },
  {
    id: 12,
    title: '山水之间的和谐',
    artist: '未知艺术家',
    image: '/images/x (12).png',
    year: '2022',
    category: '二次元',
    description: '路人女主的养成方法',
    wall: 'right'
  },
  {
    id: 13,
    title: '山水之间的和谐',
    artist: '未知艺术家',
    image: '/images/x (2).jpg',
    year: '2022',
    category: '二次元',
    description: '路人女主的养成方法',
    wall: 'left'
  },
  {
    id: 14,
    title: '山水之间的和谐',
    artist: '未知艺术家',
    image: '/images/x (3).jpg',
    year: '2022',
    category: '二次元',
    description: '路人女主的养成方法',
    wall: 'left'
  },
  {
    id: 15,
    title: '山水之间的和谐',
    artist: '未知艺术家',
    image: '/images/x (4).jpg',
    year: '2022',
    category: '二次元',
    description: '路人女主的养成方法',
    wall: 'left'
  },
  {
    id: 16,
    title: '山水之间的和谐',
    artist: '未知艺术家',
    image: '/images/x (5).jpg',
    year: '2022',
    category: '二次元',
    description: '路人女主的养成方法',
    wall: 'left'
  },
  {
    id: 17,
    title: '山水之间的和谐',
    artist: '未知艺术家',
    image: '/images/x (6).jpg',
    year: '2022',
    category: '二次元',
    description: '路人女主的养成方法',
    wall: 'left'
  },
  {
    id: 18,
    title: '山水之间的和谐',
    artist: '未知艺术家',
    image: '/images/路人女主 (1).jpg',
    year: '2022',
    category: '二次元',
    description: '路人女主的养成方法',
    wall: 'right'
  },
  {
    id: 19,
    title: '山水之间的和谐',
    artist: '未知艺术家',
    image: '/images/路人女主 (2).jpg',
    year: '2022',
    category: '二次元',
    description: '路人女主的养成方法',
    wall: 'right'
  },
  {
    id: 20,
    title: '山水之间的和谐',
    artist: '未知艺术家',
    image: '/images/路人女主 (3).jpg',
    year: '2022',
    category: '二次元',
    description: '路人女主的养成方法',
    wall: 'right'
  },
  {
    id: 21,
    title: '山水之间的和谐',
    artist: '未知艺术家',
    image: '/images/路人女主 (4).jpg',
    year: '2022',
    category: '二次元',
    description: '路人女主的养成方法',
    wall: 'right'
  },
  {
    id: 22,
    title: '山水之间的和谐',
    artist: '未知艺术家',
    image: '/images/路人女主 (5).jpg',
    year: '2022',
    category: '二次元',
    description: '路人女主的养成方法',
    wall: 'right'
  },
  {
    id: 23,
    title: '山水之间的和谐',
    artist: '未知艺术家',
    image: '/images/路人女主 (1).png',
    year: '2022',
    category: '二次元',
    description: '路人女主的养成方法',
    wall: 'right'
  },
  {
    id: 24,
    title: '山水之间的和谐',
    artist: '未知艺术家',
    image: '/images/路人女主 (2).png',
    year: '2022',
    category: '二次元',
    description: '路人女主的养成方法',
    wall: 'right'
  },
  {
    id: 25,
    title: '山水之间的和谐',
    artist: '未知艺术家',
    image: '/images/路人女主 (3).png',
    year: '2022',
    category: '二次元',
    description: '路人女主的养成方法',
    wall: 'right'
  },
  {
    id: 26,
    title: '山水之间的和谐',
    artist: '未知艺术家',
    image: '/images/路人女主 (4).png',
    year: '2022',
    category: '二次元',
    description: '路人女主的养成方法',
    wall: 'right'
  },

])
//画廊页面的尝试代码
// 处理点赞
// 优化后的排序函数
const sortedArts = computed(() => {
  // 从localStorage获取点赞数据
  const likesData = JSON.parse(localStorage.getItem('artworkLikes')) || {}

  // 创建带有点赞数据的作品数组
  const artsWithLikes = artworks.value.map(art => {
    const storedLike = likesData[art.id] || { liked: false, count: 0 }
    return {
      ...art,
      liked: storedLike.liked,
      likes: storedLike.count
    }
  })

  // 根据排序类型排序
  if (sortBy.value === 'latest') {
    // 按年份降序排列（最新在前）
    return [...artsWithLikes].sort((a, b) => parseInt(b.year) - parseInt(a.year))
  } else {
    // 按点赞数降序排列（人气在前）
    return [...artsWithLikes].sort((a, b) => b.likes - a.likes)
  }
})

// 优化后的点赞处理函数
const handleLike = (artId) => {
  const likesData = JSON.parse(localStorage.getItem('artworkLikes')) || {}
  const currentLike = likesData[artId] || { liked: false, count: 0 }

  // 更新点赞状态
  const newLiked = !currentLike.liked
  const newCount = newLiked ? currentLike.count + 1 : Math.max(0, currentLike.count - 1)

  // 保存到localStorage
  likesData[artId] = {
    liked: newLiked,
    count: newCount
  }
  localStorage.setItem('artworkLikes', JSON.stringify(likesData))

  // 触发重新排序
  sortBy.value = sortBy.value // 通过重新赋值触发computed更新
}

// 左侧墙的艺术作品
const sortedLeftArts = computed(() => sortedArts.value.filter(art => art.wall === 'left'))

// 右侧墙的艺术作品
const sortedRightArts = computed(() => sortedArts.value.filter(art => art.wall === 'right'))

// 灯箱控制
const showLightbox = ref(false)
const currentArt = ref({})

// 导航方法
const navigateTo = (path) => {
  router.push(path)
}

// 改变排序方式
const changeSort = (type) => {
  sortBy.value = type
}


const openLightbox = (art) => {
  currentArt.value = art
  currentIndex.value = sortedArts.value.findIndex(item => item.id === art.id)
  showLightbox.value = true
}

const closeLightbox = () => {
  showLightbox.value = false
}

const goBack = () => {
  router.push('/')
}
</script>

<style scoped>
.gallery-container {
  position: relative;
  width: 100%;
  min-height: 100vh;
  background-color: #f3c4d4;
  padding: 20px;
  z-index: 200;
  opacity: 90%;

}

/* 左侧导航栏样式 */
.sidebar {
  position: fixed;
  left: 0;
  top: 0;
  width: 200px;
  height: 100vh;
  padding: 20px 0;
  border-right: 1px solid rgba(255, 255, 255, 0.3);
  z-index: 300;
  background: linear-gradient(135deg, #fa94c3 0%, #50b9b4 100%);
}

.nav-item {
  position: relative;
  padding: 15px 25px;
  margin: 5px 0;
  color: #333;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 16px;
  font-weight: 500;
}

.nav-item:hover {
  background-color: rgba(255, 255, 255, 0.3);
  transform: translateX(5px);
}

.nav-item.active {
  background-color: rgba(255, 255, 255, 0.4);
  color: #d63384;
}

.nav-item:hover .hover-effect {
  width: 100%;
}

.hover-effect {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 0;
  height: 2px;
  background-color: #d63384;
  transition: width 0.3s ease;
}

/* 排序选项样式 */
.sort-options {
  position: fixed;
  top: 20px;
  right: 20px;
  display: flex;
  gap: 10px;
  z-index: 100;
}

.sort-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 20px;
  background-color: rgba(255, 255, 255, 0.3);
  cursor: pointer;
  transition: all 0.3s ease;
}

.sort-btn:hover {
  background-color: rgba(255, 255, 255, 0.5);
}

.sort-btn.active {
  background-color: #d63384;
  color: white;
}

/* 原有画廊样式 */
.back-button {
  position: fixed;
  top: 20px;
  left: 220px;
  /* 调整位置以适应侧边栏 */
  padding: 10px 15px;
  background: linear-gradient(135deg, #fa94c3 0%, #50b9b4 100%);
  border: 1px solid #98e3f0;
  border-radius: 30px;
  cursor: pointer;
  z-index: 100;
  display: flex;
  align-items: center;
  font-family: 'Noto Sans SC', sans-serif;
  transition: all 0.3s ease;
  font-size: 0.7rem;
  box-shadow: 0 2px 10px rgba(17, 236, 145, 0.8)
}

.back-button:hover {
  background: rgb(253, 170, 225);
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.arrow-icon {
  margin-right: 5px;
  font-size: 0.8rem;
}

.gallery-hall {
  display: flex;
  justify-content: space-between;
  width: 90%;
  max-width: 1400px;
  margin: 0 auto;
  padding-top: 60px;
  perspective: 1200px;
  margin-left: 220px;
  /* 为侧边栏留出空间 */

}

.gallery-wall {
  width: 100%;
  height: auto;
  display: grid; 
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 30px;
  padding: 20px;
}

.left-wall {
  transform: rotateY(0deg);
}

.right-wall {
  transform: rotateY(0deg);
}

.gallery-corridor {
  width: 30%;
  background: linear-gradient(to bottom, #ebd9a9, #746f5b);
  box-shadow: inset 0 0 30px rgba(45, 240, 181, 0.5);
  border-radius: 45% 45% 15% 15%;
}

.gallery-corridor .scrollBox h3 {
  position: absolute;
  top: 15%;
  left: 50%;
  transform: translate(-50%, -50%);
  line-height: 100px;
  z-index: 100;
  width: 40px;
  height: 70px;
  cursor: pointer;
  font-style: italic;
  font-weight: bold;
  text-align: center;
  font-size: 3rem;
  text-shadow: #98e3f0 0 0 10px;
  color: #dd61c8;
  font-family: 'Courier New', Courier, monospace;


}
</style>