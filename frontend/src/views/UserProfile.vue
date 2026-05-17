<template>
  <div class="content-section sakura-bg">
    <h2>🌸 用户信息</h2>
    <div class="profile-card">
      <div class="profile-header">
        <!-- 头像容器，包含头像和头像框 -->
        <div class="avatar-container">
          <img :src="userInfoStore.currentUser.avatar || defaultAvatar" class="profile-avatar" />
          <!-- 动态头像框 -->
          <img v-if="currentFrame" :src="currentFrame" class="avatar-frame" />
          <!-- 头像框选择按钮（仅对当前用户显示） -->
          <button v-if="isCurrentUser" class="change-frame-btn" @click="showFrameSelector = true">
            更换头像框
          </button>
        </div>
        <!-- 新增：头像下方的等级和进度条容器 -->
        <div class="level-container">
          <div class="profile-gender" title="性别">{{ userInfoStore.currentUser.gender == '男' ? '♂' : '♀' }}</div>
          <div class="level-display" title="等级">Lv{{ userInfoStore.currentUser.level || '1' }}</div>
          <div class="exp-display" title="当前经验">
            {{ currentExp }}/{{ currentExp > nextLevelExp ? '∞' : nextLevelExp }} ({{ expPercentage > 100 ? '100' : expPercentage }}%)
          </div>
          <div class="exp-bar">
            <div class="exp-progress" :style="{ width: expPercentage + '%' }"></div>
          </div>
          <div class="profile-coinBalance">
            <span class="label" title="积分总额">💰:</span>
            <span class="value">{{ formatCoinBalance(userInfoStore.currentUser.coinBalance) }}</span>
          </div>
        </div>

        <el-dialog v-model="showFrameSelector" title="选择头像框" width="70%" style="font-weight: bold; font-style: italic;">
          <!-- 分类标签页 -->
          <el-tabs type="border-card">
            <el-tab-pane label="全部">
              <FrameCategory :frames="allFrames" :current-frame="currentFrame" @select="selectFrame" />
            </el-tab-pane>

            <el-tab-pane label="等级解锁">
              <FrameCategory :frames="categorizedFrames.levelFrames" :current-frame="currentFrame"
                @select="selectFrame" />
            </el-tab-pane>

            <el-tab-pane label="VIP解锁">
              <FrameCategory :frames="categorizedFrames.vipFrames" :current-frame="currentFrame"
                @select="selectFrame" />
            </el-tab-pane>

            <el-tab-pane label="登录奖励">
              <FrameCategory :frames="categorizedFrames.loginFrames" :current-frame="currentFrame"
                @select="selectFrame" />
            </el-tab-pane>
          </el-tabs>
        </el-dialog>

        <div>
          <h3>
          <span class="vip-type" v-if="userInfoStore.currentUser.vipType == 1 " title="月度VIP">月VIP</span>
          <span class="vip-type" v-else-if="userInfoStore.currentUser.vipType == 2" title="季度VIP">季VIP</span>
          <span class="vip-type" v-else-if="userInfoStore.currentUser.vipType == 3" title="年度VIP">年VIP</span>
          <span class="vip-type" v-else="userInfoStore.currentUser.vipType == 4" title="永久VIP">永久VIP</span>
          @{{ userInfoStore.currentUser.username || '未命名用户' }}
          <span class="online-dot" title="在线" v-if="userInfoStore.currentUser.isOnline"></span>
          <span class="offline-dot" title="离线" v-else></span>
          </h3>
          <p>昵称：
          <span class="nickname-span" title="昵称">{{ userInfoStore.currentUser.nickname || '未命名昵称' }}</span></p>
          <p>IP属地：
          <span class="ip-location-span" title="IP属地">{{ userInfoStore.currentUser.registerLocation || userInfoStore.currentUser.lastLoginLocation }}</span></p>
          <button v-if="isCurrentUser" class="edit-btn" @click.prevent="handleEditProfile">编辑资料</button>
          <div class="profile-actions">
            <button v-if="!isCurrentUser" @click="handleFriendAction" :class="friendStatus">
              {{ friendButtonText }}
            </button>
            <button v-if="!isCurrentUser" @click="sendPrivateMessage" class="message-btn">
              发送私信
            </button>
          </div>
        </div>
      </div>

      <div class="profile-details">
        <div class="detail-item">
          <span class="label">个人签名:</span>
          <span class="value">{{ userInfoStore.currentUser.bio || '这个人很懒，什么都没写~' }}</span>
        </div>
        <div class="detail-item">
          <span class="label">出生日期:</span>
          <span class="value">{{ userInfoStore.currentUser.birthday?.substring(5, 10) || '未填写' }}</span>
        </div>
        <div class="detail-item">
          <span class="label">手机号码:</span>
          <span class="value">{{ userInfoStore.currentUser.phone || '未填写' }}</span>
        </div>
        <div class="detail-item">
          <span class="label">邮箱地址:</span>
          <span class="value">{{ userInfoStore.currentUser.email || '未填写' }}</span>
        </div>
        <div class="detail-item">
          <span class="label">兴趣爱好:</span>
          <span class="value">{{ userInfoStore.currentUser.hobbies || '未填写' }}</span>
        </div>
        <div class="detail-item">
          <span class="label">大好物:</span>
          <span class="value">{{ userInfoStore.currentUser.favoriteThings || '未填写' }}</span>
        </div>
        <div class="detail-item">
          <span class="label">血型:</span>
          <span class="value">{{ userInfoStore.currentUser.bloodType || '未填写' }}</span>
        </div>
        <div class="detail-item">
          <span class="label">星座:</span>
          <span class="value">{{ userInfoStore.currentUser.constellation || '未填写' }}</span>
        </div>
        <div class="detail-item">
          <span class="label">总登录天数:</span>
          <span class="value">{{ userInfoStore.currentUser.totalLoginDays || '未填写' }}</span>
        </div>
        <div class="detail-item">
          <span class="label">注册时间:</span>
          <span class="value">{{ userInfoStore.currentUser.createTime || '未知' }}</span>
        </div>
        <div class="detail-item">
          <span class="label">最后更新:</span>
          <span class="value">{{ userInfoStore.currentUser.updateTime || '未知' }}</span>
        </div>

      </div>
    </div>
  </div>

  <el-form class="userInfo-form" ref="userInfoFormRef" :model="userInfo" :rules="rules" v-if="showUserInfoForm">
    <el-button class="closeForm-btn" type="warning" @click="showUserInfoForm = false">关闭</el-button>
    <el-form-item label="用户名：" prop="username">
      <el-input v-model="userInfo.username" placeholder="请输入用户名" />
    </el-form-item>
    <el-form-item label="用户昵称：" prop="nickname">
      <el-input v-model="userInfo.nickname" placeholder="请输入用户昵称" />
    </el-form-item>
    <el-form-item label="性别：" prop="gender">
      <el-select v-model="userInfo.gender" placeholder="请选择性别">
        <el-option label="男" value="男" />
        <el-option label="女" value="女" />
      </el-select>
    </el-form-item>
    <el-form-item label="邮箱：" prop="email">
      <el-input v-model="userInfo.email" placeholder="请输入邮箱" />
    </el-form-item>
    <el-form-item label="生日：" prop="birthday">
      <el-date-picker v-model="userInfo.birthday" type="date" value-format="YYYY-MM-DD" placeholder="请选择生日" />
    </el-form-item>
    <el-form-item label="兴趣爱好：" prop="hobbies">
      <el-input v-model="userInfo.hobbies" style="width: 340px" :rows="4" type="textarea" placeholder="请输入你的兴趣爱好" />
    </el-form-item>
    <el-form-item label="大好物：" prop="favoriteThings">
      <el-input v-model="userInfo.favoriteThings" style="width: 340px" :rows="5" type="textarea"
        placeholder="请输入你的最爱❤" />
    </el-form-item>
    <el-form-item label="血型：" prop="bloodType">
      <el-select v-model="userInfo.bloodType" placeholder="请选择血型">
        <el-option label="A" value="A" />
        <el-option label="B" value="B" />
        <el-option label="AB" value="AB" />
        <el-option label="O" value="O" />
      </el-select>
    </el-form-item>
    <el-form-item label="星座：" prop="constellation">
      <el-select v-model="userInfo.constellation" placeholder="请选择星座">
        <el-option label="白羊座" value="白羊座" />
        <el-option label="金牛座" value="金牛座" />
        <el-option label="双子座" value="双子座" />
        <el-option label="巨蟹座" value="巨蟹座" />
        <el-option label="狮子座" value="狮子座" />
        <el-option label="处女座" value="处女座" />
        <el-option label="天秤座" value="天秤座" />
        <el-option label="天蝎座" value="天蝎座" />
        <el-option label="射手座" value="射手座" />
        <el-option label="摩羯座" value="摩羯座" />
        <el-option label="水瓶座" value="水瓶座" />
        <el-option label="双鱼座" value="双鱼座" />
      </el-select>
    </el-form-item>
    <el-form-item label="个人签名：" prop="bio">
      <el-input v-model="userInfo.bio" style="width: 340px" :rows="6" type="textarea" placeholder="请输入你的个人签名" />
    </el-form-item>
    <el-button class="submitBtn" type="primary" @click="submitForm()">提交</el-button>
    <el-button class="cancelBtn" @click="cancelForm()">取消</el-button>
  </el-form>

</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserInfoStore } from '@/stores/userInfo.js'
import { ElMessage, ElMessageBox } from 'element-plus'
import { updateUserInfoService } from '@/api/user.js'
import { getAllFramesWithStatus, changeAvatarFrame, unlockAvatarFrame } from '@/api/avatarFrame'
import FrameCategory from '@/views/FrameCategory.vue'
import { getCurrentFrame } from '@/api/avatarFrame'


const userInfoStore = useUserInfoStore()
const props = defineProps({
  userData: {
    type: Object,
    required: true
  },
  contentType: String,
  targetUser: Object
});

const emit = defineEmits([
  'contentChanged',  // 对应父组件的 @content-changed
  'openLightbox',    // 对应父组件的 @open-lightbox
  'playVideo'        // 对应父组件的 @play-video
]);

const showFrameSelector = ref(false)
const currentFrame = ref(null)
const allFrames = ref([])

// 分类后的头像框
const categorizedFrames = computed(() => {
  return {
    levelFrames: allFrames.value.filter(f => f.requiredLevel > 0),
    vipFrames: allFrames.value.filter(f => f.requiredVipType > 0),
    loginFrames: allFrames.value.filter(f => f.requiredDays > 0),
    defaultFrames: allFrames.value.filter(f => f.requiredLevel === 0 &&
      f.requiredVipType === 0 &&
      f.requiredDays === 0)
  }
})

//判断用户有没有达到解锁头像框的条件
const canUnlockFrame = (frame) => {
  if (frame.requiredLevel <= userInfoStore.currentUser?.level) {
    return true
  }
  if (frame.requiredVipType <= userInfoStore.currentUser?.vipType) {
    return true
  }
  if (frame.requiredDays <= userInfoStore.currentUser?.loginDays) {
    return true
  }
  return false
}

// 解锁头像框
const unlockFrame = async (frame) => {
  try {
    if (!canUnlockFrame(frame)) return
   const res = await unlockAvatarFrame(frame.id)
   if(res.code === 0){
    console.log("解锁了新头像框");
    await loadAllFrames() // 刷新列表
    } else {
      console.log("解锁失败");
    }
  } catch (error) {
    console.log("解锁失败");
  }
}

// 加载所有头像框（带状态）
const loadAllFrames = async () => {
  try {
    const res = await getAllFramesWithStatus()
    allFrames.value = res.data

    // 设置当前使用的头像框
    const usingFrame = res.data.find(f => f.isUsing)
    if (usingFrame) {
      currentFrame.value = usingFrame.url
    }
  } catch (error) {
    console.error('获取头像框失败:', error)
  }
}

// 更换头像框
const selectFrame = async (frame) => {
  if (!frame.unlocked) {
    ElMessage.warning(getUnlockRequirement(frame))
    return
  }

  try {
    await changeAvatarFrame(frame.id)
    currentFrame.value = frame.url
    showFrameSelector.value = false
    ElMessage.success('头像框更换成功')
    await loadAllFrames() // 刷新列表
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '更换失败')
  }
}

// 获取解锁条件描述
const getUnlockRequirement = (frame) => {
  const requirements = []
  if (frame.requiredLevel > 0) {
    requirements.push(`等级达到 Lv${frame.requiredLevel}`)
  }
  if (frame.requiredVipType > 0) {
    requirements.push(`VIP等级达到 ${frame.requiredVipType}`)
  }
  if (frame.requiredDays > 0) {
    requirements.push(`累计登录 ${frame.requiredDays}天`)
  }
  return `解锁条件: ${requirements.join(' 或 ')}`
}

// 从本地存储获取头像框
const avatarFrame = ref(null);
// 从接口获取用户头像框
const fetchAvatarFrame = async () => {
  try {
    const response = await getCurrentFrame()
    avatarFrame.value = response.data.url
  } catch (error) {
    console.error('Failed to fetch avatar frame:', error)
  }
}

onMounted(() => {
  loadAllFrames()
  fetchAvatarFrame();
})

// 等级经验映射表
const levelExpMap = {
  1: 0,
  2: 1000,
  3: 3000,
  4: 7000,
  5: 15000,
  6: 31000,
  7: 81000
}

// 获取等级经验的方法
const getLevelExp = (level) => {
  return levelExpMap[level] || 0
}

// 计算属性
const currentExp = computed(() => {
  return userInfoStore.currentUser?.exp || 0
})

const currentLevel = computed(() => {
  return userInfoStore.currentUser?.level || 1
})

const nextLevelExp = computed(() => {
  return getLevelExp(currentLevel.value + 1)
})

const expPercentage = computed(() => {
  const exp = currentExp.value
  const level = currentLevel.value

  // 获取当前等级所需经验
  const currentLevelExp = getLevelExp(level)
  // 获取下一等级所需经验
  const nextExp = getLevelExp(level + 1)

  // 满级情况
  if (level >= 7) return 100

  // 防止除零错误
  if (nextExp <= currentLevelExp) return 100

  // 计算百分比
  const percentage = ((exp - currentLevelExp) / (nextExp - currentLevelExp)) * 100
  return Math.round(percentage * 100) / 100 // 保留两位小数
})

const userInfo = ref({ ...userInfoStore.currentUser })

const formatCoinBalance = (balance) => {
  // 如果balance不存在或为0，显示0
  if (!balance && balance !== 0) return '0'
  // 转换为数字并格式化
  return Number(balance).toLocaleString('en-US')
}

const showUserInfoForm = ref(false)
const userInfoFormRef = ref(null)

const rules = {
  username: [
    { required: false, message: '请输入用户名' },
    { min: 2, max: 18, message: '用户名长度在 2 到 18 个字符', trigger: 'blur' }
  ],
  nickname: [
    { required: false, message: '请输入用户昵称' },
    { min: 2, max: 18, message: '用户昵称长度在 2 到 18 个字符', trigger: 'blur' }
  ],
  email: [
    { required: false, message: '请输入邮箱' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: ['blur', 'change'] }
  ],
  hobbies: [
    { required: false, message: '请输入兴趣爱好' },
    { min: 1, max: 66, message: '兴趣爱好长度在 1 到 66 个字符', trigger: 'blur' }
  ],
  favoriteThings: [
    { required: false, message: '请输入大好物' },
    { min: 1, max: 66, message: '大好物长度在 1 到 66 个字符', trigger: 'blur' }
  ],
  bio: [
    { required: false, message: '请输入个人签名' },
    { min: 1, max: 923, message: '个人签名长度在 1 到 923 个字符', trigger: 'blur' }
  ]
}

const defaultAvatar = computed(() => userInfoStore.currentUser.avatar)

const isCurrentUser = computed(() => userInfoStore.currentUser.id === props.userData?.id)
const friendStatus = computed(() => {
  if (isCurrentUser.value) return ''
  if (!props.userData?.id) return 'add'
  if (userInfoStore.isFriend(props.userData.id)) return 'friend'
  if (userInfoStore.hasPendingRequest(props.userData.id)) return 'pending'
  return 'add'
})

const friendButtonText = computed(() => {
  switch (friendStatus.value) {
    case 'friend': return '已添加'
    case 'pending': return '请求中'
    default: return '添加好友'
  }
})

const handleFriendAction = () => {
  if (!props.userData?.id) return

  if (friendStatus.value === 'add') {
    userInfoStore.sendFriendRequest(props.userData.id)
  } else if (friendStatus.value === 'pending') {
    userInfoStore.cancelFriendRequest(props.userData.id)
  } else {
    userInfoStore.removeFriend(props.userData.id)
  }
}

const sendPrivateMessage = () => {
  if (props.userData?.id) {
    userInfoStore.openPrivateChat(props.userData.id)
  }
}

const handleEditProfile = () => {
  showUserInfoForm.value = true
}

// 提交表单
const submitForm = async () => {
  // 验证表单
  if (!userInfoFormRef.value) return
  await userInfoFormRef.value.validate()
  //调用接口更新用户信息
  // 示例：检查数据
console.log('提交的数据:', userInfo.value);
  try {
    // userInfo.value.status = "active"
    let result = await updateUserInfoService(userInfo.value)
    // 示例：检查数据
console.log('提交的数据:', userInfo.value);
    if (result.code === 0) {
      ElMessage.success(result.msg ? result.msg : '更新成功')
       //修改pinia中的currentUser
    userInfoStore.setInfo(userInfo.value)
    showUserInfoForm.value = false
    } else {
      ElMessage.error(result.msg ? result.msg : '更新失败')
    }
  } catch (err) {
    ElMessage.error('更新失败')
  }
}

// 取消编辑
const cancelForm = () => {
  // 在这里处理表单重置的逻辑
  ElMessageBox.confirm('取消将丢失未保存的编辑，是否继续？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    // 重置表单
    userInfo.value = { ...userInfoStore.currentUser }
    showUserInfoForm.value = false
  }).catch(() => {
    // 取消操作
    ElMessage({
      type: 'info',
      message: '已取消'
    })
  })
}

</script>


<style scoped>
/* Your existing styles remain the same */


.profile-card {
  background: white;
  border-radius: 15px;
  padding: 20px;
  height: 100%;
  box-shadow: 0 4px 15px rgba(255, 182, 193, 0.3);

}

.profile-header {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.profile-header h3 {
  color:rgb(250, 173, 215);
  font-family:'Lucida Sans', 'Lucida Sans Regular', 'Lucida Grande', 'Lucida Sans Unicode', Geneva, Verdana, sans-serif;
  font-style: italic;
  font-size: 17px;
  font-weight: bold;
  text-shadow: 2px 2px 4px rgba(43, 230, 52, 0.8);
  user-select: none;
}
.vip-type {
  font-size: 17px;
  color: #08dbb8;
  margin-right: 10px;
  font-weight: bold;
  font-style: italic;
  font-family:Verdana, Geneva, Tahoma, sans-serif;
  text-shadow: 2px 2px 4px rgba(234, 104, 252, 0.8);
  user-select: none;
}
p {
  margin: 0 0 10px;
  font-size: 16px;
  font-family: 'Lucida Sans', 'Lucida Sans Regular', 'Lucida Grande', 'Lucida Sans Unicode', Geneva, Verdana, sans-serif;
  font-style: italic;
  font-weight: bold;
  color: rgb(233, 129, 193);
  text-shadow: 2px 2px 4px rgba(119, 235, 225, 0.7);
  user-select: none;
}

.nickname-span {
  color: #8a99eb;
  margin: 0 0 10px;
  font-size: 16px;
}
.ip-location-span {
  color: #07aa3d;
  border-radius: 35%;
  margin: 0 0 10px;
  font-size: 13px;
}

.online-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  background: #4caf50;
  border-radius: 50%;
}

.offline-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  background: #999;
  border-radius: 50%;
}

/* 头像容器 */
.avatar-container {
  position: relative;
  width: 80px;
  height: 80px;
  margin-right: 30px;
}

/* 头像 */
.profile-avatar {
  width: 70%;
  height: 70%;
  margin-left: 23px;
  margin-top: 8px;
  border-radius: 50%;
  object-fit: cover;
}

/* 头像框 */
.avatar-frame {
  position: absolute;
  top: -13px;
  left: 1px;
  width: 120%;
  height: 120%;
  pointer-events: none;
  opacity: 0.8;
}

/* 更换头像框按钮 */
.change-frame-btn {
  margin-top: 25px;
  margin-left: 10px;
  padding: 2px 5px;
  background-color: #25b495;
  color: white;
  border: none;
  border-radius: 15px;
  cursor: pointer;
  font-size: 11px;
  transition: all 0.3s;
}

.change-frame-btn:hover {
  background-color: #ff8c00;
}

.profile-actions {
  margin-top: 10px;
  display: flex;
  gap: 10px;
}

.profile-actions button {
  padding: 5px 12px;
  border-radius: 15px;
  border: none;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.profile-actions button.add {
  background: #ff69b4;
  color: white;
}

.profile-actions button.pending {
  background: #ffb6c1;
  color: white;
}

.profile-actions button.friend {
  background-color: #f0f0f0;
  color: #666;
}

.profile-actions button.message-btn {
  background-color: white;
  color: #ff69b4;
  border: 1px solid #ff69b4;
}

.profile-details {
  max-height: 450px;
  overflow-y: auto;
  padding-right: 8px;
}


.detail-item {
  display: flex;
  margin-bottom: 10px;
  padding: 8px 0;
  border-bottom: 1px dashed #ffb6c1;
}

.label {
  font-weight: bold;
  color: #ff69b4;
  width: 100px;
}

.value {
  color: #666666;
  flex: 1;
}

.content-tabs {
  display: flex;
  border-bottom: 1px solid #ffb6c1;
  margin: 20px 0 15px;
}

.content-tabs button {
  background: none;
  border: none;
  padding: 8px 16px;
  margin-right: 5px;
  border-radius: 5px 5px 0 0;
  cursor: pointer;
  color: #ff69b4;
  transition: all 0.3s;
}

.content-tabs button.active {
  background-color: #ffb6c1;
  color: white;
}

.profile-content-show {
  margin-top: 15px;
}

.edit-btn {
  background-color: rgb(93, 194, 106);
  color: #ff69b4;
  padding: 5px 12px;
  border-radius: 15px;
  border: none;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.edit-btn:hover {
  background-color: #ff69b4;
  color: white;
}

/* 新增的等级和进度条样式 */
.level-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-left: 20px;
  margin-right: 30px;
  min-width: 100px;
}

.profile-gender {
  color: #e2579d;
  font-size: 25px;
  font-weight: bold;
  text-shadow: 3px 3px 4px rgba(16, 224, 44, 0.8);
}

.level-display {
  font-size: 18px;
  font-weight: bold;
  font-family: 'Times New Roman', Times, serif;
  font-style: italic;
  color: #ff69b4;
  margin-bottom: 5px;
}

.exp-display {
  font-size: 12px;
  color: #666;
  margin-bottom: 3px;
}

.exp-bar {
  width: 100%;
  height: 8px;
  background-color: #f0f0f0;
  border-radius: 4px;
  overflow: hidden;
}

.exp-progress {
  height: 100%;
  background: linear-gradient(to right, #ff69b4, #ff8c00);
  transition: width 0.5s ease;
}

.profile-coinBalance {
  font-size: 18px;
  font-weight: bold;
  font-family: 'Times New Roman', Times, serif;
  font-style: italic;
  color: #ff69b4;
  margin-top: 3px;
}

/* 更改用户信息表单的样式 */
.userInfo-form {
  position: absolute;
  margin: 40px auto;
  top: -120px;
  left: 300px;
  background-image: url(../assets/images/editUserInfoBG.jpg);
  border-radius: 20px;
  background-size: cover;
  opacity: 90%;
  width: 550px;
  height: 850px;
  box-shadow: 0 4px 20px rgba(233, 38, 217, 0.7);
  z-index: 1000;

}

/* 修改后的深度选择器写法 */
.userInfo-form:deep(.el-form-item__label) {
  font-size: 14px;
  font-weight: bold;
  font-style: italic;
  color: #167ff8;
  text-shadow: 2px 2px 3px rgba(228, 68, 161, 0.7);
  width: 100px;
  text-align: right;
  padding-right: 12px;
  flex-shrink: 0;
}

.userInfo-form:deep(.el-form-item__content) {
  flex: 1;
  min-width: 0;
}

/* 调整整个表单的布局 */
.userInfo-form:deep(.el-form-item) {
  display: flex;
  align-items: center;
  margin-top: 10px;
  margin-left: -15px;
}

/* 调整输入框宽度 */
.userInfo-form:deep(.el-input),
.userInfo-form:deep(.el-select),
.userInfo-form:deep(.el-date-editor) {
  width: 60%;
}

.userInfo-form:deep(.el-textarea__inner) {
  width: 80%;
}


/* 关闭表单按钮 */
.closeForm-btn {
  position: absolute;
  top: 5px;
  right: 10px;
  border-radius: 15px;
  color: aqua;
  background-color: rgb(129, 151, 224);
  border: none;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
  z-index: 1001;
}

.closeForm-btn:hover {
  background-color: #ff69b4;
  color: white;
}

.submitBtn {
  position: relative;
  left: 130px;
  bottom: -20px;
  background-color: rgb(93, 194, 106);
  color: #ff69b4;
  padding: 5px 12px;
  border-radius: 15px;
  border: none;
  cursor: pointer;
  font-size: 18px;
  transition: all 0.3s;
}

.submitBtn:hover {
  background-color: #ff69b4;
  color: white;
}

.cancelBtn {
  position: relative;
  left: 220px;
  bottom: -20px;
  background-color: rgb(204, 169, 169);
  color: #ff69b4;
  padding: 5px 12px;
  border-radius: 15px;
  border: none;
  cursor: pointer;
  font-size: 18px;
  transition: all 0.3s;
}

.cancelBtn:hover {
  background-color: #ff69b4;
  color: white;
}
</style>