<template>
  <div class="image-viewer" ref="viewerContainer" v-if="visible">
    <div class="viewer-header" @mousedown="startDrag">
      <h3>{{ imageTitle }}</h3>
      <div class="viewer-actions">
        <button @click="toggleFullscreen">
          {{ isFullscreen ? '退出全屏' : '全屏' }}
        </button>
        <button class="close-btn" @click="close">×</button>
      </div>
    </div>
    <div class="image-container">
      <img :src="imageSrc" alt="预览图片" ref="imageElement">
    </div>
    <div class="viewer-controls">
      <button @click="zoomIn">放大</button>
      <button @click="zoomOut">缩小</button>
      <button @click="resetZoom">重置</button>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, onUnmounted } from 'vue'

export default {
  props: {
    visible: Boolean,
    imageSrc: String,
    imageTitle: String
  },
  setup(props, { emit }) {
    const viewerContainer = ref(null)
    const imageElement = ref(null)
    const isFullscreen = ref(false)
    const isDragging = ref(false)
    const dragStartPos = ref({ x: 0, y: 0 })
    const scale = ref(1)

    const close = () => {
      emit('close')
    }

    const toggleFullscreen = () => {
      if (!isFullscreen.value) {
        viewerContainer.value.requestFullscreen()
          .catch(err => {
            console.error('全屏错误:', err)
          })
      } else {
        document.exitFullscreen()
      }
    }

    const zoomIn = () => {
      scale.value = Math.min(scale.value + 0.2, 3)
      applyZoom()
    }

    const zoomOut = () => {
      scale.value = Math.max(scale.value - 0.2, 0.5)
      applyZoom()
    }

    const resetZoom = () => {
      scale.value = 1
      applyZoom()
    }

    const applyZoom = () => {
      if (imageElement.value) {
        imageElement.value.style.transform = `scale(${scale.value})`
      }
    }

    const startDrag = (e) => {
      if (e.target.classList.contains('viewer-header')) {
        isDragging.value = true
        const rect = viewerContainer.value.getBoundingClientRect()
        dragStartPos.value = {
          x: e.clientX - rect.left,
          y: e.clientY - rect.top
        }
        document.addEventListener('mousemove', handleDrag)
        document.addEventListener('mouseup', stopDrag)
        e.preventDefault()
      }
    }

    const handleDrag = (e) => {
      if (isDragging.value) {
        viewerContainer.value.style.left = `${e.clientX - dragStartPos.value.x}px`
        viewerContainer.value.style.top = `${e.clientY - dragStartPos.value.y}px`
        viewerContainer.value.style.transform = 'none'
      }
    }

    const stopDrag = () => {
      isDragging.value = false
      document.removeEventListener('mousemove', handleDrag)
      document.removeEventListener('mouseup', stopDrag)
    }

    const handleFullscreenChange = () => {
      isFullscreen.value = !!document.fullscreenElement
    }

    onMounted(() => {
      document.addEventListener('fullscreenchange', handleFullscreenChange)
    })

    onUnmounted(() => {
      document.removeEventListener('fullscreenchange', handleFullscreenChange)
    })

    return {
      viewerContainer,
      imageElement,
      isFullscreen,
      scale,
      close,
      toggleFullscreen,
      zoomIn,
      zoomOut,
      resetZoom,
      startDrag,
      handleDrag,
      stopDrag
    }
  }
}
</script>

<style scoped>
.image-viewer {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 70vw;
  height: 80vh;
  background: rgba(255, 240, 245, 0.95);
  border-radius: 15px;
  box-shadow: 0 0 30px rgba(255, 182, 193, 0.6);
  z-index: 1100;
  overflow: hidden;
  cursor: default;
  display: flex;
  flex-direction: column;
}

.viewer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 15px;
  background: linear-gradient(135deg, #ff9a9e 0%, #fad0c4 100%);
  color: white;
  cursor: move;
  user-select: none;
}

.viewer-actions {
  display: flex;
  gap: 10px;
}

.image-container {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: auto;
  padding: 20px;
}

.image-container img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  transition: transform 0.3s ease;
}

.viewer-controls {
  padding: 10px;
  display: flex;
  justify-content: center;
  gap: 10px;
  border-top: 1px solid #ffcce0;
}

.close-btn {
  width: 30px;
  height: 30px;
  background: #ff69b4;
  color: white;
  border: none;
  border-radius: 50%;
  font-size: 20px;
  line-height: 30px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
}

.close-btn:hover {
  background: #ff1493;
  transform: scale(1.1);
}
</style>