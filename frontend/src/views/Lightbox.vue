<template>
  <div class="lightbox-overlay" @click.self="close">
    <div class="lightbox-content">
      <button class="close-button" @click="close">×</button>
      <button class="nav-button prev-button" @click="prevImage">❮</button>
      <img :src="currentImage.image" :alt="currentImage.title" class="lightbox-image" />
      <button class="nav-button next-button" @click="nextImage">❯</button>
      <div class="lightbox-info">
        <h2>作品标题: {{ currentImage.title }}</h2>
        <p class="artist">作者: {{currentImage.artist || userStore.currentUser.username }}</p>
        <p class="description">作品描述: {{ currentImage.description }}</p>
        <!-- 切换按钮 -->
        <button v-if="currentImage.userId === userStore.currentUser.id" class="detail-toggle-button" @click="showDetails = !showDetails">
          <span class="iconfont">{{ showDetails ? '收起详情' : '显示详情' }}</span>
        </button>
        
        <!-- 折叠的内容区域 -->
        <div v-if="showDetails" class="detail-content">
          <p class="category">作品分类: {{ currentImage.category }}</p>
          <p class="resolution">分辨率: {{ currentImage.resolution }}</p>
          <p class="size">文件大小: {{ currentImage.fileSize }}</p>
          <p class="views">浏览量: {{ currentImage.viewCount }}</p>
          <p class="year">上传时间: {{ currentImage.year }}</p>
        </div>
      </div>
      <div class="button-items">
        <a :href="currentImage.image" download class="download-button">
          <span class="iconfont icon-xiazai">下载</span>
        </a>
        <!-- 编辑按钮 -->
        <button v-if="imageInfo.userId === userStore.currentUser.id" class="edit-button" @click="showEditForm">
        <span class="iconfont icon-bianji">编辑</span>
        </button>
        </div>
    </div>
  </div>
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
          <strong class="form-category">{{ currentImage.category }}</strong>
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

        <!-- 按钮组 -->
        <div class="form-buttons">
          <button type="submit" class="form-submit">保存修改</button>
          <button type="button" class="form-cancel" @click="hideEditForm">取消</button>
          <button type="button" class="form-delete" @click="deleteImage">删除图片</button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useUserInfoStore } from '@/stores/userInfo'
import { ElMessage, ElMessageBox } from 'element-plus';
import { updateMediaService, deleteMediaService } from '@/api/media'  // 引入更新媒体信息的接口

const props = defineProps({
  images: {
    type: Array,
    required: true
  },
  currentIndex: {
    type: Number,
    required: true
  }
})

const emit = defineEmits(['close', 'change-image'])
const userStore = useUserInfoStore()
const currentImage = computed(() => {
  // 确保只返回图片类型的数据
  const image = props.images[props.currentIndex]
  if (image && image.mediaType === 'video') {
    console.warn('Lightbox 不应该接收到视频数据')
    return {}
  }
  return image || {}
})
//获取当前图片所有参数信息
const imageInfo = ref({...currentImage.value})
const showDetails = ref(false)
// 编辑表单状态
const isEditVisible = ref(false)
const filename = ref('')
const description = ref('')
const category = ref('')
const isPublic = ref(true)


// 显示编辑表单
const showEditForm = () => {
  // 设置表单初始值
  filename.value = currentImage.value.title || ''
  description.value = currentImage.value.description || ''
  category.value = currentImage.value.category || ''
  isPublic.value = currentImage.value.isPublic !== false // 默认true
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
      userId: currentImage.value.userId,
      filename: filename.value,
      description: description.value,
      isPublic: isPublic.value
    }

    // 调用接口，发送更新请求
    const res = await updateMediaService(currentImage.value.id, updateData)
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
    const res = await deleteMediaService(currentImage.value.id, currentImage.value.userId)
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

const close = () => {
  emit('close')
}

const prevImage = () => {
  if (props.currentIndex > 0) {
    emit('change-image', props.currentIndex - 1)
  }
}

const nextImage = () => {
  if (props.currentIndex < props.images.length - 1) {
    emit('change-image', props.currentIndex + 1)
  }
}
</script>

<style scoped>
.lightbox-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.9);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.lightbox-content {
  position: relative;
  max-width: 90%;
  max-height: 90%;
  display: flex;
  align-items: center;
}

.lightbox-image {

  max-width: 1920px;
  height: 820px;
  display: block;
  box-shadow: 0 0 30px rgba(22, 230, 230, 0.7);
}

.close-button {
  position: absolute;
  top: -50px;
  right: -50px;
  background: none;
  border: none;
  color: rgb(21, 226, 82);
  font-size: 3rem;
  cursor: pointer;
  z-index: 1;
}

.close-button:hover {
  background: rgba(228, 27, 27, 0.3);
}

.nav-button {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  background: rgba(0, 0, 0, 0.5);
  color: white;
  border: none;
  font-size: 2rem;
  width: 50px;
  height: 100px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.3s;
}

.nav-button:hover {
  background: rgba(0, 0, 0, 0.8);
}

.prev-button {
  position: fixed;
  left: 20px;
  font-size: 70px;
  z-index: 1;
}

.prev-button:hover {
  background: rgba(30, 209, 125, 0.8);
}

.next-button {
  position: fixed;
  right: 20px;
  font-size: 70px;
  z-index: 1;
}

.next-button:hover {
  background: rgba(30, 209, 125, 0.8);
}

.lightbox-info {
  position: fixed;
  top: -1%;
  left: 1%;
  font-weight: bold;
  font-family: 'Courier New', Courier, monospace;
  font-style: italic;
  font-size: 16px;
  right: 80px;
  color: rgb(66, 206, 199);

}

.button-items {
  position: fixed;
  top: 50px;
  right: 10px;
  width: 100px;
  height: 300px;
}
.download-button {
  position: relative;
  display: inline-block;
  background: rgba(255, 255, 255, 0.2);
  color: rgb(67, 195, 218);
  border: 1px solid white;
  margin-top: 50px;
  padding: 8px 15px;
  border-radius: 10px;
  cursor: pointer;
  text-decoration: none;
  transition: all 0.3s ease;
}

.download-button:hover {
  background: rgba(211, 102, 102, 0.3);
}

.iconfont.icon-xiazai {
  font-size: 20px;
  font-weight: bold;
  color: #42e9e0;

}

.edit-button {
  position: relative;
  display: inline-block;
  background: rgba(255, 255, 255, 0.2);
  color: rgb(67, 195, 218);
  border: 1px solid white;
  margin-top: 20px;
  padding: 8px 15px;
  border-radius: 10px;
  cursor: pointer;
  text-decoration: none;
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


.detail-toggle-button {
  background: rgba(255, 255, 255, 0.2);
  color: rgb(67, 195, 218);
  border: 1px solid white;
  padding: 8px 5px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.detail-toggle-button:hover {
  background: rgba(211, 102, 102, 0.3);
}

.detail-content {
  width: 300px;
  height: 100%;
  background: #8f8b8b;
  border-radius: 4px;
  border-left: 3px solid #007bff;
  animation: fadeIn 0.3s ease;
}

/* 淡入动画 */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>