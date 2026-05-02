<template>

  <div class="sakura-container">
    <img v-for="(sakura, index) in sakuraList" :key="index" :src="sakura.image" :style="getSakuraStyle(sakura)"
      @load="handleImageLoad" @error="handleImageError" />
  </div>
  <!-- 视频背景容器 -->
  <div class="video-container">
    <video v-for="(video, index) in videos" :key="index" :ref="el => videoRefs[index] = el" class="background-video"
      :class="{ active: currentVideoIndex === index }" loop :muted="isVideoMuted" playsinline
      @loadedmetadata="onVideoLoaded(index)">
      <source :src="video.src" type="video/mp4">
    </video>
  </div>



  <!-- 媒体控制面板 -->
  <div class="media-controls">
    <!-- 视频切换按钮 -->
    <div class="video-controls">
      <button v-for="(video, index) in videos" :key="'video-btn-' + index" @click="switchVideo(index)"
        class="control-btn" :class="{ active: currentVideoIndex === index }" :title="`${video.title}`">
        <!-- 这里使用自定义图标组件 -->
        <CustomIcon :icon="video.icon" />
      </button>

      <!-- 视频音量控制 -->
      <div class="volume-control">
        <button @click="isVideoMuted = !isVideoMuted" class="control-btn" :title="isVideoMuted ? '取消静音' : '静音'">
          <CustomIcon :icon="isVideoMuted ? 'volume-off' : 'volume-up'" />
        </button>
        <input type="range" v-model="videoVolume" min="0" max="1" step="0.01" @input="setVideoVolume"
          class="volume-slider">
      </div>
    </div>

    <!-- 音乐播放器 -->
    <div>
      <div class="music-player" :class="{ collapsed: isPlayerCollapsed }">
        <div class="player-header" @click="togglePlayer">
          <i class="player-icon" :class="isPlayerCollapsed ? 'fa-music' : 'fa-times'"></i>
          <span v-if="!isPlayerCollapsed">音乐播放器</span>
        </div>

        <div class="player-body" v-if="!isPlayerCollapsed">
          <div class="now-playing">
            {{ currentSong.title }} - {{ currentSong.artist }}
          </div>

          <div class="controls">
            <button @click="prevSong" class="control-btn">
              <CustomIcon icon="step-backward" />
            </button>
            <button @click="togglePlay" class="control-btn play-btn">
              <CustomIcon :icon="isPlaying ? 'pause' : 'play'" />
            </button>
            <button @click="nextSong" class="control-btn">
              <CustomIcon icon="step-forward" />
            </button>

            <!-- 播放模式切换 -->
            <button @click="changePlayMode" class="control-btn mode-btn">
              <CustomIcon :icon="{
                'list': 'list',
                'random': 'random',
                'loop': 'repeat',
              }[playMode]" />
            </button>
          </div>

          <div class="progress-bar">
            <input type="range" v-model="currentTime" min="0" :max="duration" @input="seekAudio"
              class="progress-slider">
            <div class="time">
              {{ formatTime(currentTime) }} / {{ formatTime(duration) }}
            </div>
          </div>

          <!-- 音乐音量控制 -->
          <!-- ... 其他代码保持不变 ... -->
          <div class="volume-control">
            <button @click="toggleMute" class="control-btn">
              <CustomIcon :icon="isMuted ? 'volume-off' : 'volume-up'" />
            </button>
            <input type="range" v-model="volume" min="0" max="1" step="0.01" @input="setVolume" class="volume-slider">
          </div>
        </div>

        <div class="playlist">
          <div v-for="(song, index) in songs" :key="'song-' + index" @click="playSong(index)"
            :class="{ active: currentSongIndex === index }" class="playlist-item">
            {{ song.title }}
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- 图片门户  -->
  <div>
    <ImagePortal :initialImage="{ url: '/path/to/your/image.gif', alt: 'Gallery' }" :initialText="'画廊'" />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onBeforeUnmount, onMounted, onUnmounted } from 'vue'
import { createSakura, updateSakura } from '@/utils/sakura.js'
import { h, watch } from 'vue'; // 引入 h 函数
import ImagePortal from '@/components/ImagePortal.vue'



// 从localStorage初始化数据
const userInput = ref(JSON.parse(localStorage.getItem('userInput')) || '')

// 监听userInput变化并保存到localStorage
watch(userInput, (newVal) => {
  localStorage.setItem('userInput', JSON.stringify(newVal))
})



// 响应式数据
const sakuraImages = ref([])
const sakuraList = ref([])
const loadedImages = ref(0)
let animationFrameId = null
let windDirection = 1
let windChangeTimer = 0

// 动态加载所有樱花图片
const loadSakuraImages = async () => {
  try {
    const imageModules = await Promise.all([
      import('@/assets/sakura/sakura-1.png'),
      import('@/assets/sakura/sakura-2.png'),
      import('@/assets/sakura/sakura-3.png'),
      import('@/assets/sakura/sakura-4.png'),
      import('@/assets/sakura/sakura-5.png'),
      import('@/assets/sakura/sakura-6.png'),
      import('@/assets/sakura/sakura-7.png'),
      import('@/assets/sakura/sakura-8.png')
    ])

    sakuraImages.value = imageModules.map(module => module.default)
    // console.log('樱花图片加载完成:', sakuraImages.value)
  } catch (error) {
    console.error('加载樱花图片失败:', error)
    // 加载备用图片
    const fallback = await import('@/assets/sakura/fallback.png')
    sakuraImages.value = Array(8).fill(fallback.default)
  }
}

// 图片加载事件处理
const handleImageLoad = () => {
  loadedImages.value++
}

const handleImageError = (e) => {
  console.error('图片加载失败:', e.target.src)
  e.target.style.display = 'none'
}

// 获取随机樱花图片
const getRandomImage = () => {
  if (sakuraImages.value.length === 0) return ''
  return sakuraImages.value[Math.floor(Math.random() * sakuraImages.value.length)]
}

// 樱花样式计算
const getSakuraStyle = (sakura) => ({
  left: `${sakura.x}px`,
  top: `${sakura.y}px`,
  transform: `rotate(${sakura.rotation}deg) scale(${sakura.scale * sakura.depth})`,
  opacity: sakura.opacity,
  position: 'fixed',
  pointerEvents: 'none',
  zIndex: Math.floor(sakura.depth * 10),
  width: `${sakura.size}px`,
  height: `${sakura.size}px`,
  transition: 'transform 0.3s ease, opacity 0.5s ease',
  filter: `blur(${(1 - sakura.depth) * 1.5}px)`,
  mixBlendMode: sakura.depth > 0.7 ? 'normal' : 'overlay'
})

// 初始化樱花
const initSakura = () => {
  sakuraList.value = Array.from({ length: 30 }, () => ({
    ...createSakura(window.innerWidth, window.innerHeight),
    image: getRandomImage(),
    size: 30 + Math.random() * 50
  }))
}

// 动画循环
const animate = () => {
  windChangeTimer++
  if (windChangeTimer > 300) {
    windDirection = Math.random() > 0.5 ? 1 : -1
    windChangeTimer = 0
  }

  sakuraList.value = sakuraList.value.map(sakura => {
    if (sakura.delay > 0) {
      return { ...sakura, delay: sakura.delay - 16 }
    }

    let updated = updateSakura(sakura, window.innerWidth, window.innerHeight)
    updated.x += windDirection * 0.3 * (1 - updated.depth)

    if (updated.y > window.innerHeight + 50 ||
      updated.x < -50 ||
      updated.x > window.innerWidth + 50) {
      return {
        ...createSakura(window.innerWidth, window.innerHeight),
        image: getRandomImage(),
        size: 30 + Math.random() * 50
      }
    }

    return updated
  })

  animationFrameId = requestAnimationFrame(animate)
}

// 添加自定义图标组件
const CustomIcon = (props) => {
  // 这里定义你的图标映射
  const iconMap = {
    // 播放控制图标
    'play': '▶',       // 可以替换为你的自定义图标或Unicode
    'pause': '⏸',      // 可以替换为你的自定义图标或Unicode
    'step-backward': '⏮',
    'step-forward': '⏭',

    // 音量控制图标
    'volume-up': '🔊',  // 可以替换为你的自定义图标或Unicode
    'volume-off': '🔇', // 可以替换为你的自定义图标或Unicode

    // 播放模式图标
    'list': '📋',       // 可以替换为你的自定义图标或Unicode
    'random': '🔀',     // 可以替换为你的自定义图标或Unicode
    'repeat': '🔁',     // 可以替换为你的自定义图标或Unicode

    // 视频背景图标 (与videos数组中的icon属性对应)
    'tree': '🌳',       // 可以替换为你的自定义图标或Unicode
    'leaf': '🍃',       // 可以替换为你的自定义图标或Unicode
    'moon': '🌙',        // 可以替换为你的自定义图标或Unicode
    'sun': '☀️',       // 可以替换为你的自定义图标或Unicode
    'cloud': '☁️',       // 可以替换为你的自定义图标或Unicode
    'rain': '🌧️',       // 可以替换为你的自定义图标或Unicode
    'snow': '❄️',       // 可以替换为你的自定义图标或Unicode
    'fire': '🔥',       // 可以替换为你的自定义图标或Unicode
    'water': '💧',       // 可以替换为你的自定义图标或Unicode
    'earth': '🌍',       // 可以替换为你的自定义图标或Unicode
    'air': '🌬️',       // 可以替换为你的自定义图标或Unicode
    'heart': '❤️',       // 可以替换为你的自定义图标或Unicode
    'star': '⭐',       // 可以替换为你的自定义图标或Unicode
    'diamond': '💎',       // 可以替换为你的自定义图标或Unicode
    'crown': '👑',       // 可以替换为你的自定义图标或Unicode
    'trophy': '🏆',       // 可以替换为你的自定义图标或Unicode
    'medal': '🏅',       // 可以替换为你的自定义图标或Unicode
    'gem': '💎',       // 可以替换为你的自定义图标或Unicode
    'coin': '💰',       // 可以替换为你的自定义图标或Unicode
    'bomb': '💣',       // 可以替换为你的自定义图标或Unicode
    'skull': '💀',       // 可以替换为你的自定义图标或Unicode
    'ghost': '👻',       // 可以替换为你的自定义图标或Unicode
    'zombie': '🧟',       // 可以替换为你的自定义图标或Unicode
    'alien': '👽',       // 可以替换为你的自定义图标或Unicode
    'dragon': '🐉',       // 可以替换为你的自定义图标或Unicode
    'unicorn': '🦄',       // 可以替换为你的自定义图标或Unicode
    'devil': '😈',
    'angel': '👼',       // 可以替换为你的自定义图标或Unicode
    'princess': '👸'        // 可以替换为你的自定义图标或Unicode

  };

  // 返回一个简单的span元素显示图标
  return h('span', { class: 'custom-icon' }, iconMap[props.icon] || props.icon);
};
// 视频背景数据
const videos = reactive([
  {
    src: new URL('@/assets/videos/sakura1.mp4', import.meta.url).href,
    icon: 'sun',
    volume: 0.5,
    title: 'CherryBlessing'
  },
  {
    src: new URL('@/assets/videos/sakura2.mp4', import.meta.url).href,
    icon: 'moon',
    volume: 0.5,
    title: '花嫁'
  },
  {
    src: new URL('@/assets/videos/sakura3.mp4', import.meta.url).href,
    icon: 'cloud',
    volume: 0.5,
    title: '云'
  },
  {
    src: new URL('@/assets/videos/sakura4.mp4', import.meta.url).href,
    icon: 'rain',
    volume: 0.5,
    title: '雨'
  },
  {
    src: new URL('@/assets/videos/sakura5.mp4', import.meta.url).href,
    icon: 'snow',
    volume: 0.5,
    title: '雪'
  },
  {
    src: new URL('@/assets/videos/sakura6.mp4', import.meta.url).href,
    icon: 'fire',
    volume: 0.5,
    title: '火'
  },
  {
    src: new URL('@/assets/videos/sakura7.mp4', import.meta.url).href,
    icon: 'water',
    volume: 0.5,
    title: '水'
  },
  {
    src: new URL('@/assets/videos/sakura8.mp4', import.meta.url).href,
    icon: 'diamond',
    volume: 0.5,
    title: '钻石'
  },
  {
    src: new URL('@/assets/videos/sakura9.mp4', import.meta.url).href,
    icon: 'crown',
    volume: 0.5,
    title: ' crown'
  },
  {
    src: new URL('@/assets/videos/sakura10.mp4', import.meta.url).href,
    icon: 'star',
    volume: 0.5,
    title: 'star'
  },
  {
    src: new URL('@/assets/videos/sakura11.mp4', import.meta.url).href,
    icon: 'heart',
    volume: 0.5,
    title: 'heart'
  },
  {
    src: new URL('@/assets/videos/sakura12.mp4', import.meta.url).href,
    icon: 'medal',
    volume: 0.5,
    title: 'medal'
  },
  {
    src: new URL('@/assets/videos/sakura13.mp4', import.meta.url).href,
    icon: 'trophy',
    volume: 0.5,
    title: '宝石'
  },
  {
    src: new URL('@/assets/videos/sakura14.mp4', import.meta.url).href,
    icon: 'zombie',
    volume: 0.5,

  },
  {
    src: new URL('@/assets/videos/sakura15.mp4', import.meta.url).href,
    icon: 'skull',
    volume: 0.5,
    title: '宝石'
  },
  {
    src: new URL('@/assets/videos/sakura16.mp4', import.meta.url).href,
    icon: 'ghost',
    volume: 0.5
  },
  {
    src: new URL('@/assets/videos/sakura17.mp4', import.meta.url).href,
    icon: 'alien',
    volume: 0.5
  },
  {
    src: new URL('@/assets/videos/sakura18.mp4', import.meta.url).href,
    icon: 'tree',
    volume: 0.5
  },
  {
    src: new URL('@/assets/videos/sakura19.mp4', import.meta.url).href,
    icon: 'earth',
    volume: 0.5
  },
  {
    src: new URL('@/assets/videos/sakura20.mp4', import.meta.url).href,
    icon: 'air',
    volume: 0.5
  },
  {
    src: new URL('@/assets/videos/sakura21.mp4', import.meta.url).href,
    icon: 'leaf',
    volume: 0.5
  },
  {
    src: new URL('@/assets/videos/sakura22.mp4', import.meta.url).href,
    icon: 'star',
    volume: 0.5
  },
  {
    src: new URL('@/assets/videos/sakura23.mp4', import.meta.url).href,
    icon: 'devil',
    volume: 0.5
  },
  {
    src: new URL('@/assets/videos/sakura24.mp4', import.meta.url).href,
    icon: 'dragon',
    volume: 0.5
  },
  {
    src: new URL('@/assets/videos/sakura25.mp4', import.meta.url).href,
    icon: 'unicorn',
    volume: 0.5
  },
  {
    src: new URL('@/assets/videos/sakura26.mp4', import.meta.url).href,
    icon: 'bomb',
    volume: 0.5
  },
  {
    src: new URL('@/assets/videos/sakura27.mp4', import.meta.url).href,
    icon: 'coin',
    volume: 0.5
  },
  {
    src: new URL('@/assets/videos/sakura28.mp4', import.meta.url).href,
    icon: 'gem',
    volume: 0.5
  }
])

// 音乐数据
const songs = reactive([
  {
    title: 'GLISTENING♭',
    artist: '安野希世乃',
    src: new URL('@/assets/music/安野希世乃 - GLISTENING♭.flac', import.meta.url).href
  },
  {
    title: 'M♭',
    artist: '安野希世乃',
    src: new URL('@/assets/music/安野希世乃 - M♭.flac', import.meta.url).href
  },
  {
    title: 'I still believe ~ため息~ (我始终相信 ~叹息~)',
    artist: '滴草由実',
    src: new URL('@/assets/music/滴草由実 - I still believe ~ため息~ (我始终相信 ~叹息~).mp3', import.meta.url).href
  },
  {
    title: '分飞',
    artist: '徐怀钰',
    src: new URL('@/assets/music/徐怀钰 - 分飞.mp3', import.meta.url).href
  },
  {
    title: 'そんなゆめをみたの~lonely dreaming girl~',
    artist: 'たま',
    src: new URL('@/assets/music/たま - そんなゆめをみたの~lonely dreaming girl~.mp3', import.meta.url).href
  },
  {
    title: '終わりの世界から (始于终焉世界)',
    artist: 'やなぎなぎ',
    src: new URL('@/assets/music/やなぎなぎ - 終わりの世界から (始于终焉世界).mp3', import.meta.url).href
  },
  {
    title: 'ETERNAL♭ (永恒))',
    artist: '安野希世乃',
    src: new URL('@/assets/music/安野希世乃 - ETERNAL♭ (永恒).mp3', import.meta.url).href
  },
  {
    title: 'ULTIMATE',
    artist: '安野希世乃',
    src: new URL('@/assets/music/安野希世乃 - ULTIMATE.mp3', import.meta.url).href
  },
  {
    title: 'glory days',
    artist: '春奈るな',
    src: new URL('@/assets/music/春奈るな - glory days.mp3', import.meta.url).href
  },
  {
    title: 'カラフル. (色彩缤纷)',
    artist: '沢井美空',
    src: new URL('@/assets/music/沢井美空 - カラフル. (色彩缤纷。).mp3', import.meta.url).href
  },
  {
    title: '青春プロローグ',
    artist: '妄想キャリブレーション',
    src: new URL('@/assets/music/青春プロローグ.mp3', import.meta.url).href
  },
  {
    title: '再スタート',
    artist: '百石元',
    src: new URL('@/assets/music/百石元 - 再スタート.mp3', import.meta.url).href
  },
])

// 视频控制
const currentVideoIndex = ref(0)
const videoRefs = ref([])
const videoVolume = ref(0.5)
const isVideoMuted = ref(true)


function switchVideo(index) {
  currentVideoIndex.value = index
  videoRefs.value.forEach((video, i) => {
    if (video) {
      if (i === index) {

        // 使用Promise处理可能的错误       
        video.play().catch(e => {
          console.log('自动播放被阻止:', e)
          // 可以在这里显示"点击播放"按钮  
        })
        video.volume = videos[index].volume
      } else {
        video.pause()
      }
    }
  })
}

function onVideoLoaded(index) {
  const video = videoRefs.value[index]
  if (video) {
    video.volume = videos[index].volume
  }
}

function setVideoVolume() {
  videos[currentVideoIndex.value].volume = videoVolume.value
  const video = videoRefs.value[currentVideoIndex.value]
  if (video) {
    video.volume = videoVolume.value
  }
}

// 音乐播放器状态
const isPlayerCollapsed = ref(false)
const isPlaying = ref(false)
const currentSongIndex = ref(0)
const currentTime = ref(0)
const duration = ref(0)
const volume = ref(0.5)
const isMuted = ref(false)
const playMode = ref('list') // 'loop' | 'list' | 'random'
const audioRef = ref(new Audio())

const currentSong = computed(() => songs[currentSongIndex.value])

function togglePlayer() {
  isPlayerCollapsed.value = !isPlayerCollapsed.value
}

function togglePlay() {
  if (isPlaying.value) {
    audioRef.value.pause()
  } else {
    audioRef.value.play()
  }
  isPlaying.value = !isPlaying.value
}

function prevSong() {
  if (playMode.value === 'random') {
    currentSongIndex.value = Math.floor(Math.random() * songs.length)
  } else {
    currentSongIndex.value = (currentSongIndex.value - 1 + songs.length) % songs.length
  }
  playCurrentSong()
}

function nextSong() {
  if (playMode.value === 'random') {
    currentSongIndex.value = Math.floor(Math.random() * songs.length)
  } else {
    currentSongIndex.value = (currentSongIndex.value + 1) % songs.length
  }
  playCurrentSong()
}

function playSong(index) {
  currentSongIndex.value = index
  playCurrentSong()
}

function playCurrentSong() {
  audioRef.value.src = currentSong.value.src
  audioRef.value.currentTime = 0
  audioRef.value.play()
  isPlaying.value = true
}

function seekAudio(e) {
  audioRef.value.currentTime = e.target.value
}

// 音乐静音控制
function toggleMute() {
  isMuted.value = !isMuted.value
  audioRef.value.muted = isMuted.value
}

function setVolume() {
  audioRef.value.volume = volume.value
  isMuted.value = volume.value === 0
}

function changePlayMode() {
  const modes = ['list', 'random', 'loop']
  const currentIndex = modes.indexOf(playMode.value)
  playMode.value = modes[(currentIndex + 1) % modes.length]
}

function formatTime(seconds) {
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins}:${secs < 10 ? '0' : ''}${secs}`
}


onMounted(async () => {
  await loadSakuraImages()
  initSakura()
  animate()
})

onBeforeUnmount(() => {
  cancelAnimationFrame(animationFrameId)
})

// 初始化
onMounted(() => {


  // 初始化视频
  switchVideo(0)


  // 初始化音频
  audioRef.value.src = currentSong.value.src
  audioRef.value.volume = volume.value

  audioRef.value.addEventListener('timeupdate', () => {
    currentTime.value = audioRef.value.currentTime
  })

  audioRef.value.addEventListener('loadedmetadata', () => {
    duration.value = audioRef.value.duration
  })

  audioRef.value.addEventListener('ended', () => {
    if (playMode.value === 'loop') {
      audioRef.value.currentTime = 0
      audioRef.value.play()
    } else {
      nextSong()
    }
  })

  // 响应式调整
  window.addEventListener('resize', handleResize)
  handleResize()
})

function handleResize() {
  const isMobile = window.innerWidth < 768
  isPlayerCollapsed.value = isMobile
}

// 清理
onUnmounted(() => {
  audioRef.value.pause()
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
/* 添加自定义图标样式 */
.custom-icon {
  display: inline-block;
  font-size: inherit;
  line-height: 1;
  vertical-align: middle;
}

/* 可以根据需要调整不同位置图标的样式 */
.play-btn .custom-icon {
  font-size: 1.3em;
}

.mode-btn .custom-icon {
  font-size: 1.5em;
}

/* ... 其他样式保持不变 ... */
/* 基础变量 */
:root {
  --primary-color: #ff6b9e;
  --secondary-color: #ff8fab;
  --text-color: white;
  --player-bg: rgba(0, 0, 0, 0.7);
  --control-bg: rgb(22, 224, 224);
  --control-active: rgba(247, 105, 192, 0.3);
}

/* 视频背景样式 */
.video-container {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: -1;
}

.background-video {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0;
  transition: opacity 1s ease;
}

.background-video.active {
  opacity: 1;
}



/* 媒体控制面板 */
.media-controls {
  position: fixed;
  top: 20px;
  right: 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  z-index: 10;
  transition: width 5s ease-in-out;
  right: 0;
  width: 20px;
}

.media-controls:hover {
  transition: width 3s ease;
  width: 1400px;
}

/* 媒体播放器背景 */
.video-controls {
  display: flex;
  gap: 10px;
  opacity: 50%;
  /* padding: 10px; */
  border-radius: 20px;
  backdrop-filter: blur(10px);
  background: linear-gradient(135deg, #fa94c3 0%, #50b9b4 100%);
}

/* .video-controls.video-controls-open {
width: auto;
  
} */


.control-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgb(167, 236, 236);
  border: none;
  color: rgb(233, 160, 160);
  font-size: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.control-btn.active,
.control-btn:hover {
  background: rgb(243, 128, 181);
  transform: scale(1.1);
}

.play-btn {
  background: rgb(167, 236, 236);
  width: 50px;
  height: 50px;
  font-size: 20px;
}

.volume-control {
  display: flex;
  align-items: center;
  gap: 5px;
}

.volume-slider,
.progress-slider {
  width: 80px;
  height: 4px;
  appearance: none;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 2px;
  outline: none;
}

.volume-slider::-webkit-slider-thumb,
.progress-slider::-webkit-slider-thumb {
  appearance: none;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: rgb(204, 72, 72);
  cursor: pointer;
}


.music-player {
  position: fixed;
  top: 12%;
  right: 0;
  width: 300px;
  opacity: 70%;
  background: linear-gradient(135deg, #50b9b4 0%, #fa94c3 100%);
  border-radius: 15px 0 0 15px;
  color: paleturquoise;
  overflow: hidden;
  transition: all 2.5s cubic-bezier(0.4, 0, 0.2, 1);
  /* 更平滑的动画曲线 */
  backdrop-filter: blur(8px);
  box-shadow: -15px 15px 25px rgba(48, 240, 112, 0.7);
  border: 1px solid rgba(241, 82, 202, 0.7);
  z-index: 1001;
}

/* 折叠状态 - 完美贴边 */
.music-player.collapsed {
  transform: translateY(-50%) translateX(calc(100% - 40px));
  /* 精确贴边 */
  width: 40px;
  height: 80px;
  border-radius: 15px 0 0 15px;
  /* 保持圆角 */
  background: rgba(211, 42, 174, 0.6);
  /* 折叠后颜色更深 */
}

/* 悬停效果 */
.music-player.collapsed:hover {
  transform: translateY(-50%) translateX(calc(100% - 45px));
  /* 轻微弹出 */
  background: rgba(211, 42, 174, 0.7);
}

/* 展开时内部元素布局 */
.player-body {
  padding: 15px;
  transition: opacity 0.2s ease;
}

.music-player.collapsed .player-body {
  opacity: 0;
  pointer-events: none;
  /* 防止折叠时误触 */
}

/* 折叠按钮样式 */
.collapse-btn {
  position: absolute;
  left: 10px;
  top: 10px;
  color: paleturquoise;
  cursor: pointer;
  z-index: 2;
  transition: transform 0.3s ease;
}

.music-player.collapsed .collapse-btn {
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);

}

.player-header {
  padding: 10px 15px;
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  background: rgba(235, 23, 146, 0.3);
}

.player-icon {
  font-size: 16px;
}

.player-body {
  padding: 15px;
}

.now-playing {
  margin-bottom: 15px;
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.controls {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 15px;
  margin-bottom: 15px;
}

.mode-btn {
  margin-left: 10px;
  font-size: 14px;
}

.progress-bar {
  margin-bottom: 15px;
}

.progress-slider {
  width: 100%;
  margin-bottom: 5px;
}

.time {
  font-size: 12px;
  text-align: center;
  opacity: 0.7;
}

.playlist {
  max-height: 200px;
  overflow-y: auto;
  margin-top: 10px;
}

.playlist-item {
  padding: 8px 12px;
  cursor: pointer;
  border-radius: 5px;
  font-size: 14px;
  transition: all 0.2s ease;
}

.playlist-item:hover {
  background: rgba(240, 141, 215, 0.1);
}

.playlist-item.active {
  background: var(--primary-color);
  color: white;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .media-controls {
    top: 10px;
    right: 10px;
  }

  .video-controls {
    flex-wrap: wrap;
    justify-content: center;
    padding: 8px;
    gap: 8px;
  }

  .control-btn {
    width: 36px;
    height: 36px;
    font-size: 14px;
  }

  .play-btn {
    width: 44px;
    height: 44px;
  }

  .music-player {
    width: 280px;
  }

  .kanji.main {
    font-size: 4rem;
  }

  .kanji.small {
    font-size: 2.5rem;
  }
}

@media (max-width: 480px) {
  .art-text-container {
    top: 3%;
  }

  .kanji.main {
    font-size: 3rem;
  }

  .kanji.small {
    font-size: 2rem;
  }
}

/* 樱花飘落动画 */

.sakura-container {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 1;
}
</style>
