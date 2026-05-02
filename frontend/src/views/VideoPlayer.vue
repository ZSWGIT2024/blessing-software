<template>
  <div class="video-player-container" v-show="visible">
    <div class="video-wrapper" :class="{
      'fullscreen': isFullscreen,
      'landscape': videoOrientation === 'landscape',
      'portrait': videoOrientation === 'portrait',
      'loading': isLoading
    }">
      <video ref="videoElement" :src="src" :poster="poster" @click="togglePlay" @dblclick="toggleFullscreen"
        @timeupdate="handleTimeUpdate" @loadedmetadata="handleLoadedMetadata" @play="handlePlay" @pause="handlePause"
        @ended="handleEnded" @volumechange="handleVolumeChange" @resize="checkOrientation"></video>

      <div class="controls" v-show="showControls || !isPlaying">
        <div class="progress-bar" @click="seek" @mousemove="updateHoverPosition" @mouseenter="isHoveringProgress = true"
          @mouseleave="isHoveringProgress = false">
          <div class="progress" :style="{ width: progressPercentage + '%' }"></div>
          <div class="buffered" :style="{ width: bufferedPercentage + '%' }"></div>
          <div class="seek-hover" :style="{ left: hoverPosition + '%' }" v-show="isHoveringProgress"></div>
          <div class="hover-time" :style="{ left: hoverPosition + '%' }" v-show="isHoveringProgress">
            {{ formatTime(hoverTime) }}
          </div>
        </div>

        <div class="main-controls">
          <button @click="togglePlay" :aria-label="isPlaying ? 'Pause' : 'Play'">
            <svg class="control-icon" viewBox="0 0 24 24">
              <path v-if="isPlaying" d="M6 19h4V5H6v14zm8-14v14h4V5h-4z" fill="currentColor" />
              <path v-else d="M8 5v14l11-7z" fill="currentColor" />
            </svg>
          </button>

          <div class="time-display">
            {{ formatTime(currentTime) }} / {{ formatTime(duration) }}
          </div>

          <div class="right-controls">
            <!-- 替换原来的音量控制部分 -->
            <div class="volume-container">
              <button @click="toggleMute" :aria-label="isMuted ? 'Unmute' : 'Mute'">
                <svg class="control-icon" viewBox="0 0 24 24">
                  <path v-if="isMuted"
                    d="M16.5 12c0-1.77-1.02-3.29-2.5-4.03v2.21l2.45 2.45c.03-.2.05-.41.05-.63zm2.5 0c0 .94-.2 1.82-.54 2.64l1.51 1.51C20.63 14.91 21 13.5 21 12c0-4.28-2.99-7.86-7-8.77v2.06c2.89.86 5 3.54 5 6.71zM4.27 3L3 4.27 7.73 9H3v6h4l5 5v-6.73l4.25 4.25c-.67.52-1.42.93-2.25 1.18v2.06c1.38-.31 2.63-.95 3.69-1.81L19.73 21 21 19.73l-9-9L4.27 3zM12 4L9.91 6.09 12 8.18V4z"
                    fill="currentColor" />
                  <path v-else
                    d="M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02zM14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z"
                    fill="currentColor" />
                </svg>
              </button>

              <input type="range" min="0" max="1" step="0.01" v-model="volume" class="simple-volume-slider"
                @input="handleVolumeChange" />
            </div>

            <button @click="toggleSpeed" :aria-label="'Playback speed: ' + playbackRate + 'x'" class="speed-btn">
              {{ playbackRate }}x
              <svg class="speed-icon" viewBox="0 0 24 24">
                <path d="M16.59 8.59L12 13.17 7.41 8.59 6 10l6 6 6-6z" fill="currentColor" />
              </svg>
            </button>

            <button @click="toggleFullscreen" :aria-label="isFullscreen ? 'Exit fullscreen' : 'Enter fullscreen'">
              <svg class="control-icon" viewBox="0 0 24 24">
                <path v-if="isFullscreen"
                  d="M5 16h3v3h2v-5H5v2zm3-8H5v2h5V5H8v3zm6 11h2v-3h3v-2h-5v5zm2-11V5h-2v5h5V8h-3z"
                  fill="currentColor" />
                <path v-else d="M7 14H5v5h5v-2H7v-3zm-2-4h2V7h3V5H5v5zm12 7h-3v2h5v-5h-2v3zM14 5v2h3v3h2V5h-5z"
                  fill="currentColor" />
              </svg>
            </button>
          </div>
        </div>
      </div>

      <div class="loading-spinner" v-if="isLoading">
        <div class="spinner"></div>
        <div class="loading-text">正在加载...</div>
      </div>

      <div class="big-play-button" v-if="!isPlaying" @click="togglePlay">
        <div class="play-circle">
          <svg class="play-icon-svg" viewBox="0 0 24 24">
            <path d="M8 5v14l11-7z" fill="white" />
          </svg>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: true
  },
  src: {
    type: String,
    required: false,
    default: ''
  },
  poster: {
    type: String,
    default: ''
  },
  autoplay: {
    type: Boolean,
    default: false
  },
  controls: {
    type: Boolean,
    default: true
  },
  loop: {
    type: Boolean,
    default: false
  },
  muted: {
    type: Boolean,
    default: false
  },
  playbackRate: {
    type: Number,
    default: 1.0
  }
})

const emit = defineEmits([
  'play',
  'pause',
  'ended',
  'timeupdate',
  'volumechange',
  'fullscreenchange',
  'close'
])

// Refs
const videoElement = ref(null)
const isPlaying = ref(false)
const isMuted = ref(props.muted)
const isFullscreen = ref(false)
const isLoading = ref(true)
const showControls = ref(props.controls)
const currentTime = ref(0)
const duration = ref(0)
const buffered = ref(0)
const volume = ref(props.muted ? 0 : 0.7)
const playbackRate = ref(props.playbackRate)
const isHoveringProgress = ref(false)
const hoverPosition = ref(0)
const hoverTime = ref(0)
const controlsTimeout = ref(null)
const videoOrientation = ref('landscape')
const isVolumeSliderVisible = ref(false)

// Computed
const progressPercentage = computed(() => {
  return duration.value > 0 ? (currentTime.value / duration.value) * 100 : 0
})

const bufferedPercentage = computed(() => {
  return duration.value > 0 ? (buffered.value / duration.value) * 100 : 0
})

const speedOptions = [0.5, 0.75, 1.0, 1.25, 1.5, 2.0]

// Methods
function togglePlay() {
  if (isPlaying.value) {
    videoElement.value.pause()
  } else {
    videoElement.value.play()
  }
}

function toggleMute() {
  isMuted.value = !isMuted.value
  videoElement.value.muted = isMuted.value
  if (!isMuted.value && volume.value === 0) {
    volume.value = 0.7
    videoElement.value.volume = 0.7
  }
}

function toggleFullscreen() {
  if (!isFullscreen.value) {
    const elem = videoElement.value.parentElement
    if (elem.requestFullscreen) {
      elem.requestFullscreen()
    } else if (elem.webkitRequestFullscreen) {
      elem.webkitRequestFullscreen()
    } else if (elem.msRequestFullscreen) {
      elem.msRequestFullscreen()
    }
  } else {
    if (document.exitFullscreen) {
      document.exitFullscreen()
    } else if (document.webkitExitFullscreen) {
      document.webkitExitFullscreen()
    } else if (document.msExitFullscreen) {
      document.msExitFullscreen()
    }
  }
}

function toggleSpeed() {
  const currentIndex = speedOptions.indexOf(playbackRate.value)
  const nextIndex = (currentIndex + 1) % speedOptions.length
  playbackRate.value = speedOptions[nextIndex]
  videoElement.value.playbackRate = playbackRate.value
}

function handleTimeUpdate() {
  currentTime.value = videoElement.value.currentTime
  if (videoElement.value.buffered.length > 0) {
    buffered.value = videoElement.value.buffered.end(0)
  }
  emit('timeupdate', currentTime.value)
}

function handleLoadedMetadata() {
  duration.value = videoElement.value.duration
  videoElement.value.volume = volume.value
  videoElement.value.playbackRate = playbackRate.value

  // 检测视频方向
  checkOrientation()

  if (props.autoplay) {
    videoElement.value.play()
  }
  isLoading.value = false
}

function checkOrientation() {
  if (videoElement.value) {
    const video = videoElement.value
    if (video.videoWidth && video.videoHeight) {
      const ratio = video.videoWidth / video.videoHeight
      videoOrientation.value = ratio >= 1 ? 'landscape' : 'portrait'
    }
  }
}

function handlePlay() {
  isPlaying.value = true
  emit('play')
}

function handlePause() {
  isPlaying.value = false
  emit('pause')
}

function handleEnded() {
  isPlaying.value = false
  if (props.loop) {
    videoElement.value.currentTime = 0
    videoElement.value.play()
  }
  emit('ended')
}

function handleVolumeChange(e) {
  volume.value = parseFloat(e.target.value)
  if (videoElement.value) {
    videoElement.value.volume = volume.value
    videoElement.value.muted = volume.value === 0
    isMuted.value = volume.value === 0
  }
}
function seek(e) {
  if (!duration.value) return

  const rect = e.currentTarget.getBoundingClientRect()
  const pos = (e.clientX - rect.left) / rect.width
  const time = pos * duration.value
  videoElement.value.currentTime = Math.min(Math.max(time, 0), duration.value)
}

function updateHoverPosition(e) {
  if (!duration.value) return

  const rect = e.currentTarget.getBoundingClientRect()
  const pos = Math.min(Math.max((e.clientX - rect.left) / rect.width, 0), 1)
  hoverPosition.value = pos * 100
  hoverTime.value = pos * duration.value
}

function formatTime(seconds) {
  if (isNaN(seconds)) return '00:00'

  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const secs = Math.floor(seconds % 60)

  if (hours > 0) {
    return `${hours}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
  } else {
    return `${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
  }
}

function handleFullscreenChange() {
  isFullscreen.value = !!(
    document.fullscreenElement ||
    document.webkitFullscreenElement ||
    document.msFullscreenElement
  )
  emit('fullscreenchange', isFullscreen.value)
}

function resetControlsTimeout() {
  if (controlsTimeout.value) {
    clearTimeout(controlsTimeout.value)
  }

  showControls.value = true
  controlsTimeout.value = setTimeout(() => {
    if (isPlaying.value && !isHoveringProgress.value) {
      showControls.value = false
    }
  }, 3000)
}

function handleMouseMove() {
  if (!props.controls) return
  resetControlsTimeout()
}

function handleMouseLeave() {
  if (!props.controls) return
  if (controlsTimeout.value) {
    clearTimeout(controlsTimeout.value)
  }
  showControls.value = false
  isVolumeSliderVisible.value = false
}

// Lifecycle
onMounted(() => {
  document.addEventListener('fullscreenchange', handleFullscreenChange)
  document.addEventListener('webkitfullscreenchange', handleFullscreenChange)
  document.addEventListener('MSFullscreenChange', handleFullscreenChange)

  if (props.controls) {
    const wrapper = videoElement.value.parentElement
    if (wrapper) {
      wrapper.addEventListener('mousemove', handleMouseMove)
      wrapper.addEventListener('mouseleave', handleMouseLeave)
    }
  }

  // Initialize video properties
  if (videoElement.value) {
    videoElement.value.muted = props.muted
    videoElement.value.loop = props.loop
    videoElement.value.playbackRate = props.playbackRate
  }
})

onBeforeUnmount(() => {
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
  document.removeEventListener('webkitfullscreenchange', handleFullscreenChange)
  document.removeEventListener('MSFullscreenChange', handleFullscreenChange)

  if (controlsTimeout.value) {
    clearTimeout(controlsTimeout.value)
  }

  if (props.controls && videoElement.value?.parentElement) {
    const wrapper = videoElement.value.parentElement
    wrapper.removeEventListener('mousemove', handleMouseMove)
    wrapper.removeEventListener('mouseleave', handleMouseLeave)
  }
})

// Watchers
watch(() => props.src, (newSrc) => {
  if (videoElement.value) {
    isLoading.value = true
    videoElement.value.src = newSrc
    if (props.autoplay) {
      nextTick(() => {
        videoElement.value.play()
      })
    }
  }
})

watch(() => volume.value, (newVolume) => {
  if (newVolume > 0 && isMuted.value) {
    isMuted.value = false
  }
})

// Expose methods
const setVideo = (params) => {
  if (videoElement.value) {
    videoElement.value.src = params.src
    if (params.poster) {
      videoElement.value.poster = params.poster
    }
  }
}

defineExpose({
  setVideo,
  play: () => videoElement.value?.play(),
  pause: () => videoElement.value?.pause(),
  seekTo: (time) => {
    if (videoElement.value) {
      videoElement.value.currentTime = time
    }
  },
  setVolume: (vol) => {
    if (videoElement.value) {
      videoElement.value.volume = vol
      volume.value = vol
      videoElement.value.muted = vol === 0
      isMuted.value = vol === 0
    }
  },
  toggleFullscreen,
  enterFullscreen: () => {
    if (!isFullscreen.value) toggleFullscreen()
  },
  exitFullscreen: () => {
    if (isFullscreen.value) toggleFullscreen()
  },
  show: () => {
    if (videoElement.value) {
      videoElement.value.style.display = 'block'
      videoElement.value.load()
    }
  },
  hide: () => {
    if (videoElement.value) {
      videoElement.value.pause()
      videoElement.value.style.display = 'none'
    }
  }
})
</script>

<style scoped>
/* 智能视频容器 */
.video-player-container {
  position: relative;
  width: 100%;
  height: 100%;
  background: #000;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
}

.video-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
  background-color: #000;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 0;
  transition: all 0.3s ease;
}

.video-wrapper:hover .big-play-button {
  opacity: 1;
  transform: translate(-50%, -50%) scale(1);
}

.video-wrapper video {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  transition: all 0.3s ease;
}

/* 横屏视频适配 */
.video-wrapper.landscape video {
  width: 100%;
  height: auto;
}

/* 竖屏视频适配 */
.video-wrapper.portrait video {
  width: auto;
  height: 100%;
}

/* 全屏模式 */
.video-wrapper.fullscreen {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: 9999;
  border-radius: 0;
}

/* 控制条 */
.controls {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(transparent, rgba(0, 0, 0, 0.8));
  padding: 15px 20px;
  transition: all 0.3s ease;
  opacity: 1;
  transform: translateY(0);
}

.controls.hidden {
  opacity: 0;
  transform: translateY(10px);
}

/* 进度条 */
.progress-bar {
  height: 6px;
  background: rgba(37, 231, 95, 0.4);
  position: relative;
  margin-bottom: 15px;
  cursor: pointer;
  border-radius: 3px;
  overflow: hidden;
}

.progress-bar:hover {
  height: 8px;
}

.progress {
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  background: linear-gradient(90deg, #ff4757, #ff6b81);
  z-index: 2;
  border-radius: 3px;
  transition: width 0.1s linear;
}

.buffered {
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  background: rgba(255, 255, 255, 0.3);
  z-index: 1;
  border-radius: 3px;
}

.seek-hover {
  position: absolute;
  top: 0;
  height: 100%;
  width: 3px;
  background: white;
  z-index: 3;
  transform: translateX(-50%);
  pointer-events: none;
}

.hover-time {
  position: absolute;
  bottom: 20px;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.8);
  color: rgb(255, 255, 255);
  padding: 5px 10px;
  border-radius: 4px;
  font-size: 12px;
  white-space: nowrap;
  pointer-events: none;
  z-index: 4;
}

/* 主控制区 */
.main-controls {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 15px;
}

.main-controls button {
  background: none;
  border: none;
  color: white;
  cursor: pointer;
  padding: 8px;
  border-radius: 4px;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.main-controls button:hover {
  background: rgba(255, 255, 255, 0.1);
  transform: scale(1.1);
}

.control-icon {
  width: 20px;
  height: 20px;
  fill: currentColor;
}

.time-display {
  color: white;
  font-size: 14px;
  font-family: 'Courier New', monospace;
  user-select: none;
  flex-shrink: 0;
}

.right-controls {
  display: flex;
  align-items: center;
  gap: 15px;
}

.volume-container {
  display: flex;
  align-items: center;
  gap: 10px;
}

.simple-volume-slider {
  width: 80px;
  height: 6px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 2px;
  outline: none;
  cursor: pointer;
}

.simple-volume-slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: white;
  cursor: pointer;
  border: 2px solid #ff4757;
}

.simple-volume-slider::-moz-range-thumb {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: white;
  cursor: pointer;
  border: 2px solid #ff4757;
}

.speed-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 14px;
}

.speed-icon {
  width: 16px;
  height: 16px;
  fill: currentColor;
}

/* 加载动画 */
.loading-spinner {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 10;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 15px;
}

.spinner {
  width: 50px;
  height: 50px;
  border: 3px solid rgba(255, 255, 255, 0.1);
  border-radius: 50%;
  border-top-color: #ff4757;
  animation: spin 1s ease-in-out infinite;
}

.loading-text {
  color: white;
  font-size: 14px;
  opacity: 0.8;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* 大播放按钮 */
.big-play-button {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  cursor: pointer;
  z-index: 5;
  opacity: 0;
  transition: all 0.3s ease;
}

.play-circle {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.15), rgba(255, 255, 255, 0.05));
  backdrop-filter: blur(10px);
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10px 30px rgba(223, 52, 223, 0.6);
  transition: all 0.3s ease;
}

.big-play-button:hover .play-circle {
  background: linear-gradient(135deg, rgba(156, 77, 136, 0.4), rgba(45, 190, 235, 0.5));
  border-color: rgba(255, 255, 255, 0.5);
  transform: scale(1.1);
  box-shadow: 0 15px 40px rgba(0, 0, 0, 0.4);
}

.play-icon-svg {
  width: 30px;
  height: 30px;
  margin-left: 5px;
  fill: rgb(29, 211, 243);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .video-player-container {
    border-radius: 0;
  }

  .controls {
    padding: 10px 15px;
  }

  .progress-bar {
    margin-bottom: 12px;
  }

  .main-controls {
    gap: 10px;
  }

  .main-controls button {
    padding: 6px;
  }

  .control-icon {
    width: 18px;
    height: 18px;
  }

  .time-display {
    font-size: 12px;
  }

  .right-controls {
    gap: 10px;
  }

  .play-circle {
    width: 60px;
    height: 60px;
  }

  .play-icon-svg {
    width: 24px;
    height: 24px;
  }

  .spinner {
    width: 40px;
    height: 40px;
  }

  .loading-text {
    font-size: 12px;
  }
}

@media (max-width: 480px) {
  .controls {
    padding: 8px 12px;
  }

  .main-controls button {
    padding: 4px;
  }

  .speed-btn span {
    display: none;
  }

  .speed-icon {
    width: 18px;
    height: 18px;
  }

  .play-circle {
    width: 50px;
    height: 50px;
  }

  .play-icon-svg {
    width: 20px;
    height: 20px;
  }
}

/* 键盘快捷键提示 */
.video-wrapper:focus {
  outline: 2px solid #ff4757;
  outline-offset: -2px;
}
</style>