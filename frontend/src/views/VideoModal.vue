<!-- VideoModal.vue -->
<template>
  <!-- 使用 v-show 而不是 v-if 来保持组件状态 -->
  <div class="video-modal" v-show="show" @click.self="close">
    <div class="video-modal-content">
      <button class="video-modal-close" @click="close">×</button>
      <!-- 确保 VideoPlayer 组件被正确引用和传递 props -->
      <VideoPlayer
        ref="videoPlayerRef"
        :src="video?.filePath"
        :poster="video?.thumbnailPath"
        :autoplay="true"
        :visible="show"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import VideoPlayer from './VideoPlayer.vue'

const props = defineProps({
  mediaItems: {
    type: Array,
    validator: items => items.every(item => item.url)
  },
  show: Boolean,
  video: Object
})

const emit = defineEmits(['close'])
const videoPlayerRef = ref(null)

function close() {
  if (videoPlayerRef.value) {
    videoPlayerRef.value.pause()
  }
  emit('close')
}

// 监听视频变化，当视频改变时重置播放器
watch(() => props.video, (newVideo) => {
  if (newVideo && videoPlayerRef.value) {
    // 重置播放器状态
    videoPlayerRef.value.setVideo({
      src: newVideo.filePath,
      poster: newVideo.thumbnailPath
    })
  }
})
</script>

<style scoped>
.video-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.9);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  animation: fadeIn 0.3s forwards;
}

@keyframes fadeIn {
  to {
    opacity: 1;
  }
}

.video-modal-content {
  position: relative;
  width: 90vw;  /* 使用视口宽度 */
  height: 90vh; /* 使用视口高度 */
  max-width: 1600px;
  max-height: 900px;
  display: flex;
  flex-direction: column;
}

@keyframes scaleIn {
  from {
    transform: scale(0.9);
  }
  to {
    transform: scale(1);
  }
}

.video-modal-close {
  position: absolute;
  top: -50px;
  right: 0;
  background: none;
  border: none;
  color: rgb(37, 238, 171);
  font-size: 50px;
  cursor: pointer;
  z-index: 10000;
  opacity: 0.8;
  transition: opacity 0.3s;
}

.video-modal-close:hover {
  opacity: 1;
  color: rgb(226, 66, 173);
}
</style>