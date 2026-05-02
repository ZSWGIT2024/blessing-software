<template>

  <!-- 只在首页显示 -->
  <!--   v-if="isHomePage" 加在下方div标签里的，现在不需要-->
  <div class="image-portal-container" @click="navigateToGallery">
    <!-- 正确绑定图片URL -->
    <img :src="portalImage.url" :alt="portalImage.alt" />
    <div class="text-overlay">{{ displayText }}</div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

// import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import galleryPreview from '@/assets/images/preview.gif'
const route = useRoute()

// 计算属性判断当前是否是首页
// const isHomePage = computed(() => route.path === '/')

const props = defineProps({
  initialImage: {
    type: Object,
    default: () => ({
      url: galleryPreview, // 使用导入的引用
      alt: '默认图片'
    })
  },
  initialText: {
    type: String,
    default: ''
  }
})

const portalImage = ref({
  url: galleryPreview, // 确保使用导入的引用
  alt: props.initialImage.alt
})



const router = useRouter()
const currentImage = ref(props.initialImage)
const displayText = ref(props.initialText)

// 跳转到画廊页面
const navigateToGallery = () => {
  router.push('/gallery')
}

// 暴露方法允许父组件更新内容和图片
defineExpose({
  updateContent: (newImage, newText) => {
    currentImage.value = newImage
    displayText.value = newText
  },
})

</script>

<style scoped>
.image-portal-container {
  position: fixed;
  align-items: right;
  display: flex;
  left: 10px;
  top: 160px;
  width: 150px;
  /*调整宽度 */
  height: 150px;
  /* 调整高度 */
  border-radius: 30%;
  overflow: hidden;
  cursor: pointer;
  z-index: 100;
  transition: all 0.3s ease;
  box-shadow: 0 10px 20px  rgba(200, 10, 238, 0.5);
}

.image-portal-container:hover {
  transform: scale(1.1);
  filter: drop-shadow(0 20px 20px rgba(31, 224, 64, 0.7));
}

.image-frame {
  width: 100%;
  height: 100%;

  border-radius: 20px;
  overflow: hidden;
  position: relative;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.1);
}

.portal-image {
  width: 100%;
  height: 100%;

  object-fit: cover;
  transition: transform 0.5s ease;
}

.text-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 15px;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.7), transparent);
  color: rgb(14, 218, 207);
  font-family: 'Noto Sans SC', sans-serif;
  font-size: 1.2rem;
  font-weight: bold;
  text-align: center;
  text-shadow: 1px 1px 3px rgba(0, 0, 0, 0.5);
}
</style>