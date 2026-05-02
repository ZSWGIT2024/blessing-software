<template>
  <div class="nav" :class="{ 'nav-collapsed': isCollapsed }">
    <!-- 在 nav 内部最后添加切换按钮 -->
    <button class="toggle-btn" @click="toggleSidebar" :title="isCollapsed ? '显示导航栏' : '隐藏导航栏'">
      {{ isCollapsed ? '▶' : '◀' }}
    </button>
    <div class="profile-editor" v-if="!isCollapsed">
      <!-- 头像编辑部分 -->
      <div class="avatar-section">
        <img id="user-avatar" :src="userInfoStore.currentUser.avatar" @error="handleAvatarError"
          @click="triggerAvatarInput" class="avatar" :class="{ 'avatar-loading': isUploading }" alt="用户头像"
          title="点击更换头像" />
        <div v-if="isUploading" class="avatar-overlay">
          <span class="loading-spinner"></span>
          <span class="loading-text">上传中 {{ uploadProgress }}%</span>
        </div>
        <input id="avatar-input" type="file" action='' ref="avatarInput" @change="handleAvatarChange"
          accept="image/jpeg,image/png,image/webp" style="display: none" />
      </div>

      <!-- 用户名编辑部分 -->
      <div class="username-section">
        <span id="username" @dblclick="editUsername">{{ userInfoStore.currentUser.username }}</span>
        <button id="edit-btn" @click="editUsername" class="edit-button">
          <i class="edit-icon" title="编辑用户名">✏️</i>
        </button>
      </div>

      <!-- 自定义弹窗 -->
      <div v-if="showDialog" class="custom-dialog">
        <div class="dialog-content">
          <h3>{{ dialogTitle }}</h3>
          <p>{{ dialogMessage }}</p>
          <input v-if="dialogType === 'prompt'" v-model="inputValue" @keyup.enter="confirmDialog" ref="dialogInput">
          <div class="dialog-actions">
            <button @click="cancelDialog">取消</button>
            <button @click="confirmDialog">确认</button>
          </div>
        </div>
      </div>

      <!-- 通知提示 -->
      <div v-if="showToast" class="toast" :class="toastType">
        {{ toastMessage }}
      </div>
    </div>
    <div class="line"></div>
    <div class="title">
      <p>基本情报</p>
    </div>
    <div class="menu">
      <div class="item" :class="{ active: activeTab === 'UserProfile' }" @click="switchTab('UserProfile')">
        <div class="light"></div>
        <div class="licon">
          <span class="iconfont icon-zhanghao"></span>
        </div>
        <div class="con">用户信息</div>
        <div class="ricon"></div>
      </div>
      <div class="item" :class="{ active: activeTab === 'SocialInfo' }" @click="switchTab('SocialInfo')">
        <div class="light"></div>
        <div class="licon">
          <span class="iconfont icon-yonghu1"></span>
        </div>
        <div class="con">社交信息</div>
        <div class="ricon"></div>
      </div>
      <div class="item" :class="{ active: activeTab === 'ContentShow' }" @click="switchTab('ContentShow')">
        <div class="light"></div>
        <div class="licon">
          <span class="iconfont icon-fenlei"></span>
        </div>
        <div class="con">内容展示</div>
        <div class="ricon"></div>
      </div>
      <div class="item" :class="{ active: activeTab === 'PrivateMessage' }" @click="switchTab('PrivateMessage')">
        <div class="light"></div>
        <div class="licon">
          <span class="iconfont icon-a-weixin_huaban1"></span>
        </div>
        <div class="con">私信消息</div>
        <div class="ricon"></div>
      </div>
    </div>

    <div class="line"></div>
    <div class="title">
      <p>用户服务</p>
    </div>

    <!-- 第二组菜单 (serve) - 只添加点击事件 -->
    <div class="serve">
      <div class="item" :class="{ active: activeTab === 'MemberBenefits' }" @click="switchTab('MemberBenefits')">
        <div class="light"></div>
        <div class="licon">
          <span class="iconfont icon-huodong"></span>
        </div>
        <div class="con">会员权益</div>
        <div class="ricon"></div>
      </div>
      <div class="item" :class="{ active: activeTab === 'PrivacySettings' }" @click="switchTab('PrivacySettings')">
        <div class="light"></div>
        <div class="licon">
          <span class="iconfont icon-clock"></span>
        </div>
        <div class="con">隐私设置</div>
        <div class="ricon"></div>
      </div>
      <div class="item" :class="{ active: activeTab === 'FeedBack' }" @click="switchTab('FeedBack')">
        <div class="light"></div>
        <div class="licon">
          <span class="iconfont icon-tishi"></span>
        </div>
        <div class="con">意见建议</div>
        <div class="ricon"></div>
      </div>
      <div class="item" :class="{ active: activeTab === 'UserSettings' }" @click="switchTab('UserSettings')">
        <div class="light"></div>
        <div class="licon">
          <span class="iconfont icon-shezhi"></span>
        </div>
        <div class="con">用户设置</div>
        <div class="ricon"></div>
      </div>
    </div>

    <div class="line"></div>
    <div>
      <button class='loginout' @click="showLoginModal = true; isRegister = false">登录</button>
      <button class='loginout1' @click="showLoginModal = true; isRegister = true">注册</button>
      <LoginModal v-model:visible="showLoginModal" v-model:isRegister="isRegister" />
    </div>

  </div>

  <!-- 新增内容区域 -->
  <div class="tab-content-container" ref="draggableContainer" v-show="tabContentVisible" :style="tabContentStyle">
    <div class="tab-header" @mousedown="startDrag">
      <h2>冴えない🌸花嫁🌸の育てかた</h2>
      <div class="tab-actions">
        <button class="close-btn" @click="handleClose">×</button>
      </div>
    </div>

    <!-- 动态组件渲染区（仅添加事件监听） -->
    <component :is="activeTab" :key="activeTab.__name" :user-data="{ ...currentUserData }" :targetUser="getTargetUser()"
      v-if="tabContentVisible" ref="activeComponent" @content-changed="handleContentChange"
      @open-lightbox="openMediaViewer" @play-video="openVideoPlayer" />
  </div>
  <!-- 单独处理 PrivateMessage -->
  <PrivateMessage v-if="activeTab === 'PrivateMessage'" :key="'private-message-' + privateMessageVisible"
    v-model:visible="privateMessageVisible" :targetUser="currentChatUser"
    @update:visible="handlePrivateMessageVisibleChange" @close="handleClosePrivateMessage" />

  <!-- 确认对话框 -->
  <ConfirmDialog v-if="showConfirmDialog" :message="'您有未保存的更改，是否要保存？'" @confirm="confirmClose(true)"
    @cancel="confirmClose(false)" @close="showConfirmDialog = false" />

</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'


// Import child components
import LoginModal from '../views/LoginModal.vue'
import UserProfile from '@/views/UserProfile.vue' // 匹配文件名
import SocialInfo from '../views/SocialInfo.vue'
import ContentShow from '../views/ContentShow.vue'
import PrivateMessage from '../views/PrivateMessage.vue'
import MemberBenefits from '../views/MemberBenefits.vue'
import PrivacySettings from '../views/PrivacySettings.vue'
import FeedBack from '../views/FeedBack.vue'
import UserSettings from '../views/UserSettings.vue'
import ConfirmDialog from '../views/ConfirmDialog.vue'
import { shallowRef } from 'vue'
import { userInfoService } from '@/api/user'
import { useUserInfoStore } from '@/stores/userInfo'
import { ElMessage } from 'element-plus'
import { updateUserNameService, useUpdateAvatarService } from '@/api/user.js'
import { useTokenStore } from '@/stores/token'

const components = {
  UserProfile,
  SocialInfo,
  ContentShow,
  PrivateMessage,
  MemberBenefits,
  PrivacySettings,
  FeedBack,
  UserSettings,



  // 其他组件...
}
const privateMessageVisible = ref(false);

const userInfoStore = useUserInfoStore();
const currentUserInfo = ref({});
//调用函数，获取用户详细信息
const getUserInfo = async () => {
  //调用接口
  const result = await userInfoService();
  if (result.code === 0) {
    currentUserInfo.value = result.data;
    // 将用户信息存储到store
    userInfoStore.setInfo(currentUserInfo.value);
  }
}
getUserInfo();
const currentUserData = computed(() => userInfoStore.currentUser)

//调用函数更新用户头像
const handleAvatarChange = async (event) => {
  const file = event.target.files[0];
  if (!file) return;
  event.target.value = '';

  try {
    // 验证文件类型和大小
    if (!['image/jpeg', 'image/png', 'image/webp', 'image/gif'].includes(file.type)) {
      throw new Error('请上传JPEG/PNG/WEBP/GIF格式图片');
    }
    if (file.size > 15 * 1024 * 1024) {
      throw new Error('图片大小不能超过15MB');
    }

    isUploading.value = true;
    uploadProgress.value = 0;

    // 压缩图片
    const compressedFile = await compressImage(file);
    //获取token状态
    const tokenStore = useTokenStore()
    const formData = new FormData();
    formData.append('file', compressedFile);
    // 上传图片到服务器
    const response = await fetch('/api/upload', { // 修改为与后端一致的路径
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${tokenStore.accessToken}`
        // 不要手动设置Content-Type，浏览器会自动添加正确的multipart边界
      },
      body: formData // 使用FormData作为body
    });

    // 处理服务器响应
    if (response.ok) {
      const data = await response.json();
      // 构建完整URL
      const fullAvatarUrl = data.data
      let result = await useUpdateAvatarService(fullAvatarUrl);
      if (result.code === 0) {
        // 更新store和本地状态
        userInfoStore.currentUser.avatar = fullAvatarUrl;
        localStorage.setItem('avatar', fullAvatarUrl);
        showToastMessage('头像更新成功', 'success');
      } else {
        ElMessage.error(result.msg ? result.msg : '头像更新失败'); // 服务器返回的错误信息
      }
    } else {
      throw new Error(data.message); // 服务器返回的错误信息
    }

  } catch (error) {
    console.error('头像上传错误:', error);
    ElMessage.error('账户异常！头像更新失败');
    handleAvatarError();
    avatar.value = 'https://web-aliyun-zsw.oss-cn-shanghai.aliyuncs.com/%E6%83%A0%E5%A4%A9%E4%B8%8BLOGO.png';
  } finally {
    isUploading.value = false;
    uploadProgress.value = 0;
  }
};

// 在现有的 reactive 变量后面添加
const isCollapsed = ref(false) // 新增：控制侧边栏是否折叠

// 在现有的 methods 区域添加
const toggleSidebar = () => {
  isCollapsed.value = !isCollapsed.value
}


//调用函数，获取用户详细信息
const avatar = userInfoStore.currentUser.avatar;
const avatarInput = ref(null);
const isUploading = ref(false);
const uploadProgress = ref(0);
// const userStore = useUserStore()
const currentContentType = ref('photos') // 默认显示照片
const tabContentStyle = ref({
  top: '100px',
  left: '0px',
})

onMounted(async () => {
  await userInfoStore.initUserData() // 确保初始化用户数据
})

// 修改 getTargetUser 方法，确保返回正确的格式
const getTargetUser = () => {
  null
}

// 在 data 部分添加 currentChatUser 的初始化
const currentChatUser = ref(null)

const handleClosePrivateMessage = () => {
  // 关闭私信窗口，可以切换回其他标签
  switchTab('UserInfo')
}


// 媒体查看器状态
const lightbox = ref(null)
const videoplayer = ref(null)
const mediaItems = ref([])

const openMediaViewer = (item) => {
  mediaItems.value = [{
    url: item.image,
    type: 'image',
    caption: item.description || ''
  }]
  nextTick(() => {
    lightbox.value.open(0)
  })
}

const openVideoPlayer = (video) => {
  videoplayer.value.show()
  videoplayer.value.setVideo({  // 使用暴露的方法设置视频
    src: video.video,
    poster: video.thumbnail
  })
}

// Navigation state
const showLoginModal = ref(false)
const isRegister = ref(false)

// Tab management
const activeTab = shallowRef(null) // ✅ 组件不会被深度响应式化
const pendingTab = ref(null)
const tabContentVisible = ref(false)
const hasChanges = ref(false)
const showConfirmDialog = ref(false)
const draggableContainer = ref(null)
const activeComponent = ref(null)

// Dragging state
const dragStartX = ref(0)
const dragStartY = ref(0)
const isDragging = ref(false)

// User data

const username = ref('')

// Dialog state
const showDialog = ref(false)
const dialogTitle = ref('')
const dialogMessage = ref('')
const dialogType = ref('alert')
const inputValue = ref('')
let dialogResolve = null

// Toast state
const showToast = ref(false)
const toastMessage = ref('')
const toastType = ref('success')
let toastTimeout = null

const showToastMessage = (message, type = 'success', duration = 3000) => {
  toastMessage.value = message
  toastType.value = type
  showToast.value = true

  if (toastTimeout) clearTimeout(toastTimeout)
  toastTimeout = setTimeout(() => {
    showToast.value = false
  }, duration)
}

const showCustomDialog = (type, title, message, defaultValue = '') => {
  dialogType.value = type
  dialogTitle.value = title
  dialogMessage.value = message
  inputValue.value = defaultValue
  showDialog.value = true

  return new Promise((resolve) => {
    dialogResolve = resolve
  })
}

const confirmDialog = () => {
  //如果用户名输入不合法，不关闭对话框
  if (validateUsername(inputValue.value) !== true) {
    ElMessage.warning('用户名不合法，请重新输入');
    return;
  }
  showDialog.value = false
  if (dialogResolve) {
    dialogResolve(dialogType.value === 'prompt' ? inputValue.value : true)
  }
}

const cancelDialog = () => {
  showDialog.value = false
  if (dialogResolve) {
    dialogResolve(dialogType.value === 'prompt' ? null : false)
  }
}

// 头像加载失败处理 - 放在methods区域
const handleAvatarError = () => {
  avatar.value = 'https://web-aliyun-zsw.oss-cn-shanghai.aliyuncs.com/%E6%83%A0%E5%A4%A9%E4%B8%8BLOGO.png';
  localStorage.removeItem('avatar');
};

// 触发文件选择 - 保持原有位置
const triggerAvatarInput = () => {
  avatarInput.value.click();
};

// 优化后的图片压缩方法
const compressImage = async (file, options = {}) => {
  const {
    maxWidth = 800,
    maxHeight = 800,
    quality = 0.8,
    mimeType = 'image/webp'
  } = options;

  if (file.size <= 1 * 1024 * 1024) return file;

  return new Promise((resolve) => {
    const img = new Image();
    const reader = new FileReader();

    reader.onload = (e) => (img.src = e.target.result);
    img.onload = () => {
      const canvas = document.createElement('canvas');
      let { width, height } = img;

      if (width > maxWidth || height > maxHeight) {
        const ratio = Math.min(maxWidth / width, maxHeight / height);
        width = Math.floor(width * ratio);
        height = Math.floor(height * ratio);
      }

      canvas.width = width;
      canvas.height = height;
      const ctx = canvas.getContext('2d');
      ctx.drawImage(img, 0, 0, width, height);

      canvas.toBlob(
        (blob) => resolve(new File([blob], file.name.replace(/\.[^/.]+$/, '.webp'), {
          type: mimeType,
          lastModified: Date.now()
        })),
        mimeType,
        quality
      );
    };
    reader.readAsDataURL(file);
  });
};

const validateUsername = (name) => {
  if (!name || name.trim().length < 2) return '用户名不能小于2个字符'
  if (name.length > 18) return '用户名不能超过18个字符'
  //可以包含更多标点符合的校验规则，如—，@，￥，%，#等
  // 允许使用中划线、下划线、点号、逗号、感叹号、问号、括号
  if (!/^[\u4e00-\u9fa5a-zA-Z0-9-_.，！？、@#$%^&*()]+$/g.test(name))
    return '用户名只能包含中文、字母、数字、中划线、下划线、点号、逗号、感叹号、问号、括号、@#$%^&*等'
  return true
}

const editUsername = async () => {
  try {
    const newName = await showCustomDialog(
      'prompt',
      '修改用户名',
      '请输入新用户名 (2-18个字符)',
      userInfoStore.currentUser.username
    );

    if (newName === null) return;
    if (newName === userInfoStore.currentUser.username) {
      return;
    }

    const validationError = validateUsername(newName);
    if (validationError !== true) {
      showCustomDialog('error', '用户名不合法', validationError)
      return;
    }

    const trimmedName = newName.trim();
    //调用接口更新用户名信息
    let result = await updateUserNameService(trimmedName);
    ElMessage.success(result.msg ? result.msg : '更新成功')
    //修改pinia中的currentUser
    userInfoStore.currentUser.username = trimmedName
  } catch (error) {
    console
      .error('用户名修改失败:', error);
    // 失败时恢复之前的用户名
    username.value = userInfoStore.currentUser.username;
    ElMessage.error('用户名修改失败');
  }
}// Tab management methods
const startDrag = (e) => {
  if (e.target.classList.contains('tab-content') ||
    e.target.tagName === 'H2' ||
    e.target.classList.contains('tab-header')) {
    isDragging.value = true
    const container = draggableContainer.value
    const rect = container.getBoundingClientRect()
    dragStartX.value = e.clientX - rect.left
    dragStartY.value = e.clientY - rect.top

    document.addEventListener('mousemove', handleDrag)
    document.addEventListener('mouseup', stopDrag)
    e.preventDefault()
  }
}

const handleDrag = (e) => {
  if (isDragging.value) {
    const container = draggableContainer.value
    container.style.left = `${e.clientX - dragStartX.value}px`
    container.style.top = `${e.clientY - dragStartY.value}px`
    container.style.transform = 'none'
  }
}

const stopDrag = () => {
  isDragging.value = false
  document.removeEventListener('mousemove', handleDrag)
  document.removeEventListener('mouseup', stopDrag)
}

const switchTab = (tabName) => {
  nextTick(() => {
    try {
      // 特殊处理内容展示标签
      if (tabName === 'ContentShow') {
        currentContentType.value = 'photos' // 设置默认内容类型
      }
      if (activeTab.value === components[tabName]) {
        tabContentVisible.value = !tabContentVisible.value
        return
      }

      if (hasChanges.value) {
        showConfirmDialog.value = true
        pendingTab.value = components[tabName]
        return
      }

      activeTab.value = components[tabName]
      tabContentVisible.value = true
      hasChanges.value = false
      if (draggableContainer.value && draggableContainer.value.$el) {
        const el = draggableContainer.value.$el || draggableContainer.value
        el.style.left = '250px'
        el.style.top = '100px'
      }
    } catch (error) {
      console.error('切换标签出错:', error)
    }
  })
}

const handleClose = () => {
  if (!tabContentVisible.value) return // 已经关闭则不再处理

  if (hasChanges.value) {
    showConfirmDialog.value = true
    return
  }

  // 清除可能引起问题的引用
  if (draggableContainer.value) {
    draggableContainer.value = null
  }

  tabContentVisible.value = false
}

const confirmClose = (shouldSave) => {
  showConfirmDialog.value = false

  try {
    if (shouldSave) {
      saveChanges()
    }

    if (pendingTab.value) {
      activeTab.value = pendingTab.value
      pendingTab.value = null
      tabContentVisible.value = true
    } else {
      tabContentVisible.value = false
    }

    hasChanges.value = false
  } catch (error) {
    console.error('确认关闭出错:', error)
    tabContentVisible.value = false
  }
}
const saveChanges = () => {
  activeComponent.value?.save()
    .then(() => {
      hasChanges.value = false
    })
    .catch(error => {
      console.error('保存失败:', error)
    })
}

const handleContentChange = (hasChangesValue) => {
  hasChanges.value = hasChangesValue
}

let syncInterval = null
onUnmounted(() => {
  if (syncInterval) {
    clearInterval(syncInterval)
  }
})
</script>


<style scoped>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.nav {
  position: fixed;
  left: 20px;
  width: 230px;
  height: 700px;
  margin-top: 4%;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 20px;
  overflow: hidden;
  transition: all 2s cubic-bezier(0.4, 0, 0.2, 1);
  /* 修改原有过渡属性 */
  box-shadow: 15px 15px 25px rgba(10, 202, 236, 0.7);
  border: 1px solid rgba(200, 11, 238, 0.7);
  z-index: 100;
  opacity: 0.9;
}

.btn {
  width: 60px;
  height: 10px;
  display: flex;
  justify-content: space-around;
  margin-top: 25px;
  margin-left: 25px;

}

.btn-item {
  margin-top: -10px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.btn-item:nth-child(1) {
  background: #d81c1c;
}

.btn-item:nth-child(2) {
  background: #2ae496;
}

.btn-item:nth-child(3) {
  background: #eccb0e;
}

.line {
  width: 60px;
  height: 3px;
  background: rgba(245, 253, 255, 0.5);
  margin: 20px, 25px;
  transition: 0.5s;
}

.nav:hover .line {
  width: 150px;
}

.title {
  width: 70px;
  margin-top: 10px;
  margin-left: 25px;
  margin-bottom: 20px;
  color: rgb(252, 29, 151);

}

.title p {
  font-size: 15px;
}

.menu {
  width: 230px;
  margin-left: 25px;
}

.serve {
  width: 230px;
  margin-left: 25px;
}

.item {
  display: flex;
  position: relative;
  border-radius: 6px;
  transition: 0.5s;

}

.item:hover {
  background: rgba(255, 255, 255, 0.1);
  margin-left: 25px;

}

.licon {

  width: 60px;
  height: 50px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.con {

  height: 50px;
  display: flex;
  justify-content: center;
  align-items: center;
  transition: 0.5s;
  overflow: hidden;
  position: relative;
  left: -20px;
  opacity: 1;
  color: rgb(238, 145, 222);

}

.nav:hover.con {
  width: 160px;
  display: flex;
  justify-items: center;
  align-items: center;
  opacity: 1;

}

.ricon {
  width: 0px;
  height: 50px;
  transition: 0.5s;
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
  opacity: 0;

}

.nav:hover.ricon {
  width: 60px;
  display: flex;
  justify-items: center;
  align-items: center;
  opacity: 1;
}

.iconfont {
  font-size: 26px;
}

.ricon .iconfont {
  color: #62cb44;
}

.light {
  width: 3px;
  height: 50px;
  background: #eb5a56;
  position: absolute;
  left: -25px;
  transition: 0.5s;
  border-top-right-radius: 4px;
  border-bottom-right-radius: 4px;

}

.iconfont.icon-bianji {
  position: relative;
  margin-top: 50px;
  font-size: 25px;
  margin-left: 50px;
  color: #e0e6e5;
}

.iconfont.icon-zhanghao {
  font-size: 30px;
  margin-left: -30px;
  color: #42e9e0;
}

.iconfont.icon-yonghu1 {
  font-size: 30px;
  margin-left: -30px;
  color: #42e9e0;
}

.iconfont.icon-fenlei {
  font-size: 30px;
  margin-left: -30px;
  color: #42e9e0;
}

.iconfont.icon-a-weixin_huaban1 {
  font-size: 30px;
  margin-left: -30px;
  color: #42e9e0;
}

.iconfont.icon-huodong {
  font-size: 30px;
  margin-left: -30px;
  color: #42e9e0;
}

.iconfont.icon-clock {
  font-size: 30px;
  margin-left: -30px;
  color: #42e9e0;
}

.iconfont.icon-tishi {
  font-size: 30px;
  margin-left: -30px;
  color: #42e9e0;
}

.iconfont.icon-shezhi {
  font-size: 30px;
  margin-left: -30px;
  color: #42e9e0;
}

/*用户编辑头像和用户名的样式 */
.profile-editor {
  display: flex;
  flex-direction: row;
  justify-content: center;
  /* align-items: center; */
  padding: 10px;
  position: relative;
}

.avatar-section {
  position: relative;
}

.avatar {
  position: relative;
  width: 80px;
  height: 80px;
  right: 20px;
  border-radius: 50%;
  object-fit: cover;
  cursor: pointer;
  border: 3px solid #eee;
  transition: all 0.3s ease;
}

.avatar:hover {
  transform: scale(1.05);
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.2);
}

.avatar-loading {
  opacity: 0.7;
}

.avatar-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background-color: rgba(0, 0, 0, 0.3);
}

.loading-spinner {
  width: 30px;
  height: 30px;
  border: 3px solid rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  border-top-color: #fff;
  animation: spin 1s ease-in-out infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.username-section {
  display: flex;
  align-items: center;
  flex-direction: row-reverse;
  gap: 10px;
  position: relative;
  overflow: visible;
}

#username {
  position: absolute;
  top: -10%;
  font-size: 1.5rem;
  font-style: italic;
  font-weight: bold;
  left: 100%;
  width: 50px;
  height: 700px;
  color: rgba(207, 61, 176, 0.8);
  background: linear-gradient(135deg, #fa94c3 0%, #50b9b4 100%);
  writing-mode: vertical-rl;
  /* 竖排 */
  text-orientation: upright;
  /* 强制所有字符直立 */
  font-family: Verdana, Geneva, Tahoma, sans-serif;
  cursor: pointer;
  padding: 5px;
  border-radius: 20px;
  transition: background-color 0.2s;
}

#username:hover {
  background-color: #ddadad;
}

.edit-button {
  position: relative;
  background: none;
  border: none;
  cursor: pointer;
  padding: 10px;
  right: 15px;
  border-radius: 50%;
  transition: all 0.2s;
}

.edit-button:hover {
  background-color: #d49595;
  transform: rotate(15deg);
}

.edit-icon {
  font-size: 1rem;
}

/* 自定义对话框样式 */
.custom-dialog {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.dialog-content {
  background-image: url(../assets/images/changeUsernameBG.jpg);
  background-size: cover;
  opacity: 90%;
  border-radius: 20px;
  width: 60%;
  height: 70%;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}

.dialog-content h3 {
  margin-top: 15px;
  font-size: 30px;
  font-weight: bold;
  font-family: 'Noto Sans JP', sans-serif;
  font-style: italic;
  text-align: center;
  color: #fc48c0;
}

.dialog-content p {
  margin-top: 45px;
  font-size: 30px;
  font-weight: bold;
  font-family: 'Courier New', Courier, monospace;
  font-style: italic;
  text-align: center;
  color: #fc48c0;
}

.dialog-content input {
  width: 40%;
  padding: 10px;
  margin-top: 200px;
  margin-left: 30%;
  border: 1px solid #32e07b;
  border-radius: 10px;
}

.dialog-actions {
  display: flex;
  margin-top: 50px;
  justify-content: center;
  gap: 150px;
}

.dialog-actions button {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.dialog-actions button:first-child {
  background-color: #f0f0f0;
}

.dialog-actions button:last-child {
  background-color: #3498db;
  color: white;
}

.dialog-actions button:hover {
  opacity: 0.9;
}

/*用户编辑头像和用户名的样式 */
.loginout {
  position: absolute;
  left: 30px;
  bottom: 30px;
  border-radius: 10px;
  cursor: text;
  font-size: 23px;
  color: #f51d77;
  background-color: rgba(184, 161, 161, 0.5);
  opacity: 0.9;
}

.loginout:hover {
  background-color: #287025;
}

.loginout1 {
  position: absolute;
  right: 90px;
  bottom: 30px;
  border-radius: 10px;
  cursor: text;
  font-size: 23px;
  color: #e1f00d;
  background-color: rgba(184, 161, 161, 0.5);
  opacity: 0.9;

}

.loginout1:hover {
  background-color: #287025;
}

/* 新增Toast样式 */
.toast {
  position: fixed;
  top: 50px;
  left: 50%;
  transform: translateX(-50%);
  padding: 12px 24px;
  border-radius: 4px;
  color: white;
  background-color: #4CAF50;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
  z-index: 1000;
  animation: fadeIn 0.3s;
}

.toast.error {
  background-color: #f44336;
}

.toast.warning {
  background-color: #ff9800;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateX(-50%) translateY(20px);
  }

  to {
    opacity: 1;
    transform: translateX(-50%) translateY(0);
  }
}

/* 加载指示器 */
.loading-indicator {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background-color: #2196F3;
  z-index: 1000;
  animation: loading 2s infinite;
}

@keyframes loading {
  0% {
    width: 0%;
    left: 0;
    right: 100%;
  }

  50% {
    width: 100%;
    left: 0;
    right: 0;
  }

  100% {
    width: 0%;
    left: 100%;
    right: 0;
  }
}

/* 新增样式 */
.tab-content-container {
  position: fixed;
  margin-left: 240px;
  margin-top: -20px;
  width: 70%;
  height: 750px;
  background: rgba(255, 240, 245, 0.95);
  border-radius: 25px 25px 0 0;
  box-shadow: 0 0 30px rgba(255, 182, 193, 0.6);
  z-index: 500;
  cursor: move;
}

.tab-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  background: linear-gradient(135deg, #fa94c3 0%, #50b9b4 100%);
  color: white;
  user-select: none;
  border-radius: 25px 25px 0 0;
}

.tab-header h2 {
  color: rgb(236, 110, 199);
  font-style: italic;
  font-size: 30px;
  font-weight: bold;
  text-shadow: 2px 2px 4px rgba(43, 230, 52, 0.8);
  user-select: none;
}

.tab-actions {
  display: flex;
  gap: 10px;
}

.save-btn {
  padding: 5px 15px;
  background: #ff69b4;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.save-btn:hover {
  background: #ff1493;
}

.tab-content {
  height: calc(100% - 60px);
  padding: 20px;
  overflow-y: auto;
}



.close-btn {
  position: absolute;
  top: 20px;
  right: 20px;
  width: 30px;
  height: 30px;
  background: #ff69b4;
  color: white;
  border: none;
  border-radius: 50%;
  font-size: 50px;
  line-height: 30px;
  text-align: center;
  cursor: pointer;
  z-index: 10;
  transition: all 0.3s;
}

.close-btn:hover {
  background: #ff1493;
  transform: scale(1.1);
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 活动菜单项样式 - 与原有样式兼容 */
.item.active {
  background: rgba(255, 182, 193, 0.3);
}

.item.active .light {
  background: #ff1493;
  box-shadow: 0 0 10px #ff1493;
}

.item.active .con {
  color: #ff1493;
  font-weight: bold;
}

/* 在 style 部分最后添加以下样式 */
.nav-collapsed {
  transform: translateX(-100%);
  width: 30px;
  height: 80px;
}

.toggle-btn {
  position: absolute;
  transform: translateY(-50%);
  margin-right: 1px;
  margin-top: 40px;
  width: 30px;
  height: 80px;
  background: rgba(97, 107, 88, 0.5);
  border-radius: 40%;
  cursor: pointer;
  z-index: 1001;
  transition: all 0.3s ease;
  color: rgb(235, 22, 164);
}

.toggle-btn:hover {
  background: rgba(31, 202, 245, 0.7);
}


/* 保持其他所有样式不变 */
</style>