<!-- UserDetail.vue 修复版 -->
<template>
  <div class="user-detail-modal" v-if="visible" @click.self="close">
    <div class="user-detail-content">
      <div class="user-detail-header">
        <button class="close-btn" @click="close">&times;</button>
      </div>
      
      <div class="user-detail-body" v-if="userData">
        <!-- 头像容器 -->
        <div class="avatar-container">
          <img :src="userData.avatar || defaultAvatar" class="profile-avatar" />
          <img v-if="userData.avatarFrame" :src="userData.avatarFrame" class="avatar-frame" />
        </div>
        
        <div class="vip-tag">
        <div class="profile-gender">
        <span class="gender-icon">{{ userData.gender == '男' ? '♂' : '♀' }}</span>
        </div>
            <span v-if="userData.vipType === 1" title="月度VIP" class="vip-monthly">VIP</span>
            <span v-else-if="userData.vipType === 2" title="季度VIP" class="vip-quarterly">VIP</span>
            <span v-else-if="userData.vipType === 3" title="年度VIP" class="vip-annual">VIP</span>
            <span v-else-if="userData.vipType === 4" title="终身VIP" class="vip-life">终身VIP</span>
            <span v-else title="普通用户" class="vip-none">普通用户</span>
          </div>
           <!-- 等级和进度条 -->
        <div class="level-container" v-if="userData.level">
          <div class="level-display">Lv{{ userData.level }}</div>
          <div class="exp-display" v-if="userData.exp">
            {{ userData.exp }}/{{ userData.nextLevelExp || '∞' }}
          </div>
          <div class="exp-bar" v-if="userData.exp && userData.nextLevelExp">
            <div class="exp-progress" :style="{ width: expPercentage + '%' }"></div>
          </div>
          <div class="coin-display" title="积分总额">💰:{{ userData.coinBalance || 0}}</div>
        </div>

        <div class="user-info">
          <h3 class="username">用户名：
          <span class="username-span">{{ userData.username }}</span></h3>
          <p>昵称：
          <span class="nickname-span">{{ userData.nickname }}</span></p>
          <p class="bio">个性签名：
          <span class="bio-span">{{ userData.bio || '这个人很懒，什么都没写~' }}</span></p>
        </div>

        <div class="user-stats">
          <div class="stat-item">
            <span class="label">粉丝数</span>
            <span class="value">{{ userData.followerCount || 0 }}</span>
          </div>
          <div class="stat-item">
            <span class="label">作品数</span>
            <span class="value">{{ userData.uploadCount || 0 }}</span>
          </div>
          <div class="stat-item">
            <span class="label">获赞量</span>
            <span class="value">{{ userData.likedCount || 0 }}</span>
          </div>
        </div>

        <div class="user-details">
          <div class="detail-item" v-if="userData.birthday">
            <span class="label">出生日期:</span>
            <span class="value">{{ formatDate(userData.birthday) }}</span>
          </div>
          <div class="detail-item" v-if="userData.hobbies">
            <span class="label">兴趣爱好:</span>
            <span class="value">{{ userData.hobbies }}</span>
          </div>
          <div class="detail-item" v-if="userData.favoriteThings">
            <span class="label">大好物:</span>
            <span class="value">{{ userData.favoriteThings }}</span>
          </div>
          <div class="detail-item" v-if="userData.bloodType">
            <span class="label">血型:</span>
            <span class="value">{{ userData.bloodType }}</span>
          </div>
          <div class="detail-item" v-if="userData.constellation">
            <span class="label">星座:</span>
            <span class="value">{{ userData.constellation }}</span>
          </div>
          <div class="detail-item">
            <span class="label">注册时间:</span>
            <span class="value">{{ formatDateTime(userData.createTime) }}</span>
          </div>
          <div class="detail-item" v-if="userData.lastActiveTime">
            <span class="label">最后活跃:</span>
            <span class="value">{{ formatDateTime(userData.lastActiveTime) }}</span>
          </div>
        </div>

        <div class="user-actions" v-if="!isCurrentUser">
          <button 
            @click="handleFriendAction(userData.id)" 
            :class="['action-btn', friendStatus]"
          >
            {{ friendButtonText }}
          </button>
          <button 
            @click="handleFollowAction(userData.id)" 
            :class="['action-btn', followStatus]"
          >
            {{ followButtonText }}
          </button>
          <button @click="sendPrivateMessage" class="action-btn message">
            发送私信
          </button>
        </div>
      </div>
      <div v-else class="loading-state">
        <p>加载中...</p>
      </div>
    </div>
     <!-- 聊天窗口 -->
    <PrivateMessage v-if="userData" :visible="showChatModal" :targetUser="userData || friendId"
      @update:visible="handleChatVisibleChange" @close="closeChat" />
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PrivateMessage from './PrivateMessage.vue'
import { useUserInfoStore } from '@/stores/userInfo'
import * as socialApi from '@/api/socialApi'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  friendId: {
    type: Number,
    default: null  // 改为默认 null
  }
})

const emit = defineEmits(['update:visible', 'close'])

const userInfoStore = useUserInfoStore()
const userData = ref(null)
// 添加状态
const showChatModal = ref(false)
// 计算属性
const isCurrentUser = computed(() => {
  return userInfoStore.currentUser?.id === Number(props.friendId)
})

const expPercentage = computed(() => {
  if (!userData.value?.exp || !userData.value?.nextLevelExp) return 0
  return Math.min(100, (userData.value.exp / userData.value.nextLevelExp) * 100)
})

const friendButtonText = computed(() => {
  return userData.value?.isFriend ? '删除好友' : '添加好友'
})

const friendStatus = computed(() => {
  return userData.value?.isFriend ? 'friend' : 'stranger'
})

const followStatus = computed(() => {
  return userData.value?.isFollowed ? 'following' : 'follow'
})

const followButtonText = computed(() => {
  return userData.value?.isFollowed ? '取消关注' : '关注'
})

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'


// 处理聊天窗口可见性变化
const handleChatVisibleChange = (val) => {
  showChatModal.value = val
}

const closeChat = () => {
  showChatModal.value = false
}
// 方法
const formatDate = (date) => {
  if (!date) return '未知'
  return new Date(date).toLocaleDateString()
}

const formatDateTime = (dateTime) => {
  if (!dateTime) return '未知'
  return new Date(dateTime).toLocaleString()
}

const loadUserDetail = async () => {
  if (!props.friendId) {
    console.warn('friendId 为空，无法加载用户详情')
    return
  }
  
  try {
    const res = await socialApi.getUserDetail(props.friendId)
    userData.value = res.data
  } catch (error) {
    console.error('加载用户详情失败:', error)
    ElMessage.error('加载用户详情失败')
  }
}

const handleFriendAction = async (userId) => {
  if (!userData.value) return

  try {
    if (friendStatus.value === 'friend') {
      ElMessageBox.confirm('确定要删除好友吗？', '删除好友', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
       const res = await socialApi.deleteFriend(userId)
       if (res.code === 0) {
        ElMessage.success('已删除好友')
        await loadUserDetail()
      } else {
        ElMessage.error('删除好友失败')
      }
      })
    } else if (friendStatus.value === 'stranger') {
      const { value: msg } = await ElMessageBox.prompt('请输入申请留言（可选）', '发送好友申请', {
      confirmButtonText: '发送',
      cancelButtonText: '取消',
      inputPlaceholder: '输入留言...'
    })
    await socialApi.sendFriendApply({
      receiverId: userId,
      applyMsg: msg || ''
    })
    ElMessage.success('好友申请已发送')
    await loadUserDetail()
    } 
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.msg || '发送失败')
    }
  }
}

const handleFollowAction = async () => {
  if (!userData.value) return

  try {
    if (userData.value.isFollowed) {
      await socialApi.unfollowUser(props.friendId)  
      ElMessage.success('已取消关注')
    } else {
      await socialApi.followUser(props.friendId)   
      ElMessage.success('关注成功')
    }
    await loadUserDetail()
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '操作失败')
  }
}

const sendPrivateMessage = () => {
  showChatModal.value = true
}

const close = () => {
  emit('update:visible', false)
  emit('close')
}

watch(() => props.visible, (newVal) => {
  if (newVal && props.friendId) {
    loadUserDetail()
  }
})

// 修复：监听 friendId 而不是 userId
watch(() => props.friendId, (newVal) => {
  if (props.visible && newVal) {
    loadUserDetail()
  }
})
</script>

<style scoped>
.user-detail-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1001;
}

.user-detail-content {
  background: white;
  border-radius: 16px;
  width: 500px;
  max-width: 90%;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

.user-detail-header {
  padding: 15px 20px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: flex-end;
}

.close-btn {
  background: none;
  border: none;
  font-size: 28px;
  cursor: pointer;
  color: #999;
  line-height: 1;
}

.close-btn:hover {
  color: #333;
}

.user-detail-body {
  padding: 20px;
}

.avatar-container {
  position: relative;
  width: 120px;
  height: 120px;
  margin: 0 auto 20px;
}

.profile-avatar {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid #fff;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.avatar-frame {
  position: absolute;
  top: -10px;
  left: -10px;
  width: 140px;
  height: 140px;
  pointer-events: none;
}

.vip-tag {
  margin: 0 auto 1px;
  width: fit-content;
  font-size:medium;
  font-weight: bold;
  font-style: italic;
  font-family:'Courier New', Courier, monospace;
}

.vip-monthly {
  color: #ec71af;
}
.vip-quarterly {
  color: #ff6600;
}
.vip-annual {
  color: #cc3300;
}
.vip-life {
  color: #0beb68;
}
.vip-none {
  color: #999;
}

.level-container {
  text-align: center;
  margin-bottom: 20px;
}

.profile-gender {
  display: inline-block;
  color: #e2579d;
  text-shadow: 3px 3px 4px rgba(16, 224, 44, 0.8);
  margin-right: 10px;
}
.gender-icon {
  font-size: 25px;
}
.coin-display {
  font-size: 18px;
  font-weight: bold;
  font-family: 'Times New Roman', Times, serif;
  font-style: italic;
  color: #ff69b4;
}

.level-display {
  display: inline-block;
  font-size: 18px;
  font-weight: bold;
  font-family: 'Times New Roman', Times, serif;
  font-style: italic;
  color: #ff69b4;
  margin-right: 10px;
}

.exp-display {
  font-size: 12px;
  color: #666;
  margin: 5px 0;
}

.exp-bar {
  height: 6px;
  background: #f0f0f0;
  border-radius: 3px;
  overflow: hidden;
  width: 200px;
  margin: 5px auto;
}

.exp-progress {
  height: 100%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 3px;
  transition: width 0.3s;
}

.user-info {
  text-align: center;
  margin-bottom: 20px;
}

.user-info h3 {
  margin: 0 0 5px;
  font-size: 20px;
  font-weight: bold;
  color: #333;
}

.user-info p {
  margin: 0 0 10px;
  font-size: 18px;
  color: #666;
}

.username-span {
  color:rgb(250, 173, 215);
  font-family:'Lucida Sans', 'Lucida Sans Regular', 'Lucida Grande', 'Lucida Sans Unicode', Geneva, Verdana, sans-serif;
  font-style: italic;
  font-size: 17px;
  font-weight: bold;
  text-shadow: 2px 2px 4px rgba(43, 230, 52, 0.5);
  margin: 0 0 10px;
}
.nickname-span {
  color: #8a99eb;
  margin: 0 0 10px;
  font-size: 16px;
}

.bio-span {
  color: #3ead9b;
  margin: 0;
  font-size: 14px;
  line-height: 1.6;
}

.user-stats {
  display: flex;
  justify-content: space-around;
  padding: 15px 0;
  border-top: 1px solid #f0f0f0;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 15px;
}

.stat-item {
  text-align: center;
}

.stat-item .label {
  display: block;
  color: #999;
  font-size: 12px;
  margin-bottom: 5px;
}

.stat-item .value {
  font-size: 18px;
  font-weight: bold;
  color: #333;
}

.user-details {
  margin-bottom: 20px;
}

.detail-item {
  display: flex;
  padding: 8px 0;
  border-bottom: 1px dashed #f0f0f0;
}

.detail-item:last-child {
  border-bottom: none;
}

.detail-item .label {
  width: 80px;
  color: #999;
  font-size: 14px;
}

.detail-item .value {
  flex: 1;
  color: #333;
  font-size: 14px;
}

.user-actions {
  display: flex;
  gap: 10px;
  justify-content: center;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}

.action-btn {
  padding: 8px 20px;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.action-btn.friend, .action-btn.following {
  background: #f0f0f0;
  color: #666;
}

.action-btn.stranger, .action-btn.follow {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.action-btn.message {
  background: #52c41a;
  color: white;
}

.action-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(216, 116, 116, 0.8);
}

/* 保持原有样式不变，添加加载状态 */
.loading-state {
  padding: 40px;
  text-align: center;
  color: #999;
}
</style>