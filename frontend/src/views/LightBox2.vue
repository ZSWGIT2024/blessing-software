<template>
  <transition name="fade">
    <div v-if="isVisible" class="lightbox" @click.self="close">
      <div class="lightbox-content">
        <button class="close-btn" @click="close" aria-label="Close lightbox">
          &times;
        </button>

        <div class="lightbox-body">
          <img v-if="mediaType === 'image'" :src="currentMedia.url" :alt="currentMedia.alt || 'Enlarged media'"
            @load="handleLoad" />

          <!-- 修改后的caption -->
          <div v-if="showCaption && currentMedia.caption" class="caption">
            {{ currentMedia.caption }}
          </div>
        </div>

        <!-- 修改后的controls -->
        <div v-if="hasMultiple" class="lightbox-controls">
          <button class="nav-btn prev" @click="prevMedia" :disabled="currentIndex === 0" aria-label="Previous media">
            &larr;
          </button>
          <span class="counter">{{ currentIndex + 1 }} / {{ mediaItems.length }}</span>
          <button class="nav-btn next" @click="nextMedia" :disabled="currentIndex === mediaItems.length - 1"
            aria-label="Next media">
            &rarr;
          </button>
        </div>

        <div class="lightbox-actions">
          <a :href="currentMedia.url" download class="download-button" v-if="currentMedia.status === 'active'">
            <span class="iconfont icon-xiazai">下载</span>
          </a>
          <!-- 编辑按钮 -->
          <button v-if="currentMedia.userId === userStore.currentUser.id" class="edit-button" @click="showEditForm">
            <span class="iconfont icon-bianji">编辑</span>
          </button>
        </div>
      </div>
    </div>
  </transition>
  <!-- 编辑表单 - 放在模态框内部 -->
  <div v-if="isEditVisible" class="edit-modal-overlay" @click.self="hideEditForm">
    <div class="edit-form-container">
      <form class="edit-form" @submit.prevent="updateImage">
        <div class="form-header">
          <h3>编辑作品信息</h3>
          <button type="button" class="close-edit-btn" @click="hideEditForm">×</button>
        </div>

        <!-- 主题输入 -->
        <div class="form-group">
          <label class="form-label">主题：</label>
          <input v-model="filename" placeholder="请输入作品主题（选填）" class="form-input" maxlength="36">
          <div class="input-counter">{{ filename.length }}/36</div>
        </div>

        <!-- 描述输入 -->
        <div class="form-group">
          <label class="form-label">描述：</label>
          <textarea v-model="description" placeholder="请描述一下你的作品（选填）" class="form-textarea" maxlength="66"
            rows="3"></textarea>
          <div class="textarea-counter">{{ description.length }}/66</div>
        </div>

         <!-- 分类 -->
        <div class="form-group">
          <label class="form-label">分类:
          <strong class="form-category">{{ category }}</strong>
          </label>
        </div>

        <!-- 隐私设置 -->
        <div class="form-group">
          <label class="privacy-label">
            <input type="checkbox" v-model="isPublic" class="privacy-checkbox">
            <span class="privacy-text">公开分享</span>
          </label>
          <p class="privacy-hint">公开后其他用户可以查看和点赞你的作品</p>
        </div>

        <!-- 审核状态 -->
        <div class="form-group" v-if="isTagsVisible">
          <label class="form-label">审核失败详情：</label>
          <span class="form-tags" style="color: red; font-weight: bold;">{{ currentMedia.tags }}</span>
        </div>

        <!-- 按钮组 -->
        <div class="form-buttons">
          <button type="submit" class="form-submit"
            v-if="currentMedia.status === 'active'">保存修改</button>
          <button type="button" class="form-submit" v-if="currentMedia.status === 'pending' || currentMedia.status === 'hidden'" @click="isTagsVisible = true">查看详情</button>
          <button type="button" class="form-cancel" @click="hideEditForm"
            v-if="currentMedia.status === 'active'">取消</button>
          <button type="button" class="form-delete" @click="deleteImage">删除图片</button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useUserInfoStore } from '@/stores/userInfo'
import { ElMessage, ElMessageBox } from 'element-plus';
import { updateMediaService, deleteMediaService } from '@/api/media'  // 引入更新媒体信息的接口


const props = defineProps({
  mediaItems: {
    type: Array,
    validator: items => items.every(item => item.url)
  },
  initialIndex: Number,
  showCaption: Boolean,
  showActions: Boolean,
  image: String,       // 改为可选
  title: String        // 改为可选
})

const emit = defineEmits(['close', 'update:index', 'like', 'share', 'download'])
const userStore = useUserInfoStore()
const isVisible = ref(false)
const isTagsVisible = ref(false)
const currentIndex = ref(props.initialIndex)
const isLoading = ref(true)


const currentMedia = computed(() => props.mediaItems[currentIndex.value])
const mediaType = computed(() => {
  const url = currentMedia.value.url.toLowerCase()
  return url.match(/\.(mp4|webm|ogg)$/) ? 'video' : 'image'
})
const hasMultiple = computed(() => props.mediaItems.length > 1)

// 编辑表单状态
const isEditVisible = ref(false)
const filename = ref('')
const description = ref('')
const category = ref('')
const isPublic = ref(true)


// 显示编辑表单
const showEditForm = () => {
  // 设置表单初始值
  filename.value = currentMedia.value.filename || ''
  description.value = currentMedia.value.description || ''
  category.value = currentMedia.value.category || ''
  isPublic.value = currentMedia.value.isPublic !== false // 默认true
  isEditVisible.value = true
}

// 隐藏编辑表单
const hideEditForm = () => {
  isEditVisible.value = false
}

// 更新图片信息
const updateImage = async () => {
  try {
    // 构建更新数据
    const updateData = {
      userId: currentMedia.value.userId,
      filename: filename.value,
      description: description.value,
      category: category.value,
      isPublic: isPublic.value
    }
    //如果是查看审核失败详情，则显示
    if (currentMedia.value.status !== 'active') {
      isTagsVisible.value = true
      return
    } else {
      isTagsVisible.value = false
    }

    // 调用接口，发送更新请求
      const res = await updateMediaService(currentMedia.value.id, updateData)
      if (res.code === 0) {
        ElMessage.success('更新成功')
        // 隐藏表单
        hideEditForm()
        // 重新获取媒体列表
        emit('close')
      } else {
        ElMessage.error('更新失败')
      }
  } catch (error) {
    ElMessage.error('更新失败')
    console.error('更新失败:', error)
    // 可以添加错误提示
  }
}

// 删除图片
const deleteImage = async () => {
  await ElMessageBox.confirm('确定要删除这张图片吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    // 调用接口，发送删除请求
    const res = await deleteMediaService(currentMedia.value.id, currentMedia.value.userId)
    if (res.code === 0) {
      ElMessage.success('删除成功')
      // 隐藏表单
      hideEditForm()
      // 重新获取媒体列表
      emit('close')
    } else {
      ElMessage.error('删除失败')
    }
  }).catch(() => {
    ElMessage.info('已取消删除')
  })
}

// 修改 open 方法
function open(index = 0) {
  if (props.mediaItems.length === 0) return

  currentIndex.value = Math.max(0, Math.min(index, props.mediaItems.length - 1))
  isVisible.value = true
  document.body.style.overflow = 'hidden'
  addKeyListeners()
  console.log('mediaItems:', props.mediaItems);

}

function close() {
  isVisible.value = false
  document.body.style.overflow = ''
  removeKeyListeners()
  emit('close')
}

function nextMedia() {
  if (currentIndex.value < props.mediaItems.length - 1) {
    currentIndex.value++
    isLoading.value = true
    emit('update:index', currentIndex.value)
  }
}

function prevMedia() {
  if (currentIndex.value > 0) {
    currentIndex.value--
    isLoading.value = true
    emit('update:index', currentIndex.value)
  }
}

function handleLoad() {
  isLoading.value = false
}

function handleKeyDown(e) {
  switch (e.key) {
    case 'Escape':
      close()
      break
    case 'ArrowRight':
      nextMedia()
      break
    case 'ArrowLeft':
      prevMedia()
      break
  }
}

function addKeyListeners() {
  window.addEventListener('keydown', handleKeyDown)
}

function removeKeyListeners() {
  window.removeEventListener('keydown', handleKeyDown)
}

onMounted(() => {
  if (props.initialIndex >= 0) {
    open(props.initialIndex)
  }
})

onBeforeUnmount(() => {
  removeKeyListeners()
  document.body.style.overflow = ''
})

defineExpose({ open, close })
</script>

<style scoped>
.lightbox {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(216, 116, 155, 0.6);
  box-shadow: 0 0 10px rgba(37, 228, 62, 0.8);
  z-index: 1000;
}

.lightbox-content {
  position: relative;
  max-width: 100%;
  max-height: 100%;
}

.lightbox-body {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.lightbox-body img {
  position: fixed;
  width: auto;
  height: 850px;
  top: 2%;

}

.caption {
  position: fixed;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(143, 116, 116, 0.4);
  padding: 8px 16px;
  border-radius: 4px;
  max-width: 80%;
}

.lightbox-controls {
  position: fixed;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  background: rgba(90, 74, 74, 0.7);
  padding: 8px 16px;
  border-radius: 4px;
}

.close-btn {
  position: absolute;
  top: 10px;
  right: 30px;
  background: none;
  border: none;
  color: rgb(39, 211, 159);
  font-size: 4rem;
  cursor: pointer;
}

.close-btn:hover {
  color: rgb(252, 48, 241);
}


.nav-btn {
  font-family: 'Noto Sans SC', sans-serif;
  background: rgba(255, 255, 255, 0.0001);
  border: none;
  color: rgb(214, 26, 26);
  padding: 0.5rem 1rem;
  margin: 0 1rem;
  border-radius: 4px;
  cursor: pointer;
  font-size: 2rem;
}

.nav-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.counter {
  margin: 0 1rem;
  color: rgb(42, 223, 230);
}


.lightbox-actions {
  display: flex;
  justify-content: center;
  margin-top: 50px;
  gap: 1rem;
}


.lightbox-actions button:hover {
  background: rgba(214, 172, 172, 0.6);
}

.icon-like.liked {
  color: #ff4757;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media (max-width: 768px) {
  .lightbox-content {
    max-width: 95%;
  }
}

.download-button {
  position: absolute;
  top: 20px;
  left: 30px;
  background: rgba(50, 17, 236, 0.5);
  color: rgb(8, 240, 124);
  border: 1px solid white;
  padding: 8px 15px;
  border-radius: 10px;
  cursor: pointer;
  text-decoration: none;
  transition: all 0.3s ease;
  font-family: 'Noto Sans SC', sans-serif;
  font-size: 1.2rem;
  letter-spacing: 0.05em;
  box-shadow: 10px 10px 15px rgba(18, 226, 105, 0.8);
  z-index: 1000;
}

.download-button:hover {
  background: rgba(14, 216, 206, 0.5);
}

.edit-button {
  position: absolute;
  top: 100px;
  left: 30px;
  background: rgba(50, 17, 236, 0.5);
  color: rgb(8, 240, 124);
  border: 1px solid white;
  padding: 8px 10px;
  border-radius: 10px;
  cursor: pointer;
  text-decoration: none;
  transition: all 0.3s ease;
  font-family: 'Noto Sans SC', sans-serif;
  font-size: 1rem;
  letter-spacing: 0.05em;
  box-shadow: 10px 10px 15px rgba(18, 226, 105, 0.8);
  z-index: 1000;
  transition: all 0.3s ease;
}

.edit-button:hover {
  background: rgba(211, 102, 102, 0.3);
}

.iconfont.icon-bianji {
  font-size: 20px;
  font-weight: bold;
  color: #42e9e0;
}

/* 编辑表单模态框样式 */
.edit-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 2000;
}

.edit-form-container {
  width: 450px;
  max-height: 80vh;
  overflow-y: auto;
}

.edit-form {
  background: #da96bb;
  padding: 25px;
  border-radius: 12px;
  box-shadow: 0 0 30px rgba(22, 230, 230, 0.7);
}

.form-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
}

.form-header h3 {
  margin: 0;
  color: #3edfd7;
}

.close-edit-btn {
  background: white;
  border: 1px solid #50bddf;
  font-size: 40px;
  color: #f12df1;
  cursor: pointer;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-edit-btn:hover {
  background: #f0f0f0;
  color: #333;
}

.form-group {
  margin-bottom: 20px;
}

.form-label {
  display: block;
  margin-bottom: 8px;
  color: #bbdf58;
  font-weight: 500;
}

.form-input {
  width: 95%;
  padding: 12px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  transition: border-color 0.3s;
}

.form-input:focus {
  outline: none;
  border-color: #4a90e2;
}

.form-input::placeholder {
  color: #999;
}

.form-input.error {
  border-color: #ff6b6b;
}

.form-input.error:focus {
  border-color: #ff6b6b;
}

.input-counter {
  text-align: right;
  margin-top: 5px;
  font-size: 15px;
  color: #3ee768;
}

.form-textarea {
  width: 95%;
  padding: 12px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  resize: vertical;
  transition: border-color 0.3s;
}

.form-textarea:focus {
  outline: none;
  border-color: #4a90e2;
}

.textarea-counter {
  text-align: right;
  margin-top: 5px;
  font-size: 15px;
  color: #3ee768;
}

.form-category {
  margin-left: 20px;
  font-size: 18px;
  font-family: 'Courier New', Courier, monospace;
  font-style: italic;
  color: #fd1187;
  font-weight: bold;
  text-shadow: 1px 5px 5px rgba(227, 25, 245, 0.5);
}

.privacy-label {
  display: flex;
  align-items: center;
  cursor: pointer;
  margin-bottom: 5px;
}

.privacy-checkbox {
  margin-right: 10px;
  width: 18px;
  height: 18px;
  cursor: pointer;
}

.privacy-text {
  color: #333;
  font-weight: 500;
}

.privacy-hint {
  margin-left: 28px;
  font-size: 13px;
  color: #1a61cc;
  margin-top: 4px;
}

.form-buttons {
  display: flex;
  gap: 15px;
  margin-top: 25px;
}

.form-submit {
  flex: 1;
  padding: 12px;
  background: #4a90e2;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.form-submit:hover {
  background: #357abd;
  transform: translateY(-2px);
}


.form-cancel {
  flex: 1;
  padding: 12px;
  background: #f0f0f0;
  color: #666;
  border: none;
  border-radius: 8px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.form-cancel:hover {
  background: #e0e0e0;
  color: #333;
}

.form-delete {
  flex: 1;
  padding: 12px;
  background: #ff6b6b;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.form-delete:hover {
  background: #e05757;
  transform: translateY(-2px);
}
</style>