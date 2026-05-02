<template>
  <div class="social-container">
    <!-- 头部标签页 -->
    <div class="tab-header">
      <button v-for="tab in tabs" :key="tab.id" :class="['tab-btn', { 'active': activeTab === tab.id }]"
        @click="switchTab(tab.id)">
        {{ tab.name }}
        <span v-if="tab.id === 'messages' && unreadCount > 0" class="unread-badge">
          {{ unreadCount }}
        </span>
      </button>
    </div>

    <!-- 内容区域 -->
    <div class="tab-content">
      <!-- 消息列表 -->
      <div v-if="activeTab === 'messages'" class="messages-section">
        <div class="section-header">
          <h3>消息列表</h3>
          <div class="unread-total">
            未读消息: <span class="unread-count">{{ unreadCount }}</span>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-if="recentChats.length === 0" class="empty-state">
          <img src="@/assets/no-chat.webp" alt="暂无聊天" />
          <p>还没有聊天记录</p>
        </div>

        <div v-for="chat in recentChats" :key="chat.relatedUserId" class="chat-item"
          :class="{ 'unread': chat.unreadCount > 0 }" @click="openChat(chat)">
          <div class="avatar-wrapper" @click.stop="showUserDetail(chat.relatedUserId)">
            <img :src="chat.avatar" class="chat-avatar" />
            <img v-if="chat.avatarFrame" :src="chat.avatarFrame" class="avatar-frame" />
          </div>
          <div class="chat-info">
            <div class="chat-name" title="用户名">
              {{ chat.username || `未知用户` }}
              <span v-if="chat.isStarred" class="star-icon" title="星标好友">★</span>
              <span v-if="chat.isFriend" class="friend-tag" title="好友">好友</span>
              <span v-if="chat.isFollowing" class="following-tag" title="关注">已关注</span>
              <span v-if="chat.remark" class="remark-tag" title="备注">{{ chat.remark }}</span>
            </div>
            <div class="last-message">
              {{ chat.lastMessage || '暂无消息' }}
            </div>
          </div>
          <div class="chat-meta">
            <div class="chat-time">{{ formatTime(chat.lastTime) }}</div>
            <div v-if="chat.unreadCount > 0" class="chat-unread">
              {{ chat.unreadCount }}
            </div>
          </div>
        </div>
      </div>

      <!-- 好友列表 -->
      <div v-else-if="activeTab === 'friends'" class="friends-section">
        <div class="section-header">
          <h3>好友列表 ({{ friends.length }})</h3>
          <div class="group-actions">
            <button @click="showCreateGroupDialog" class="group-btn">
              <span>➕</span> 新建分组
            </button>
            <button @click="showEditGroupDialog" class="group-btn">
              <span>✏️</span> 编辑分组
            </button>
          </div>
        </div>

        <!-- 新建分组表单 -->
        <div v-if="showGroupForm" class="group-form">
          <h4>{{ groupFormMode === 'create' ? '新建分组' : '编辑分组' }}</h4>
          <div v-if="groupFormMode === 'edit'" class="form-group">
            <label>选择分组</label>
            <select v-model="selectedGroupId" @change="loadGroupData">
              <option value="">请选择分组</option>
              <option v-for="group in friendGroups" :key="group.id" :value="group.id">
                {{ group.groupName }}
              </option>
            </select>
          </div>
          <div class="form-group">
            <label>分组名称 <span class="required">*</span></label>
            <input v-model="groupForm.groupName" placeholder="请输入分组名称" />
          </div>

          <div class="form-group">
            <label>分组排序</label>
            <input type="number" v-model="groupForm.sortOrder" placeholder="请输入排序数字，数字越大越靠后" />
          </div>

          <div class="form-group">
            <label>分组颜色</label>
            <input type="color" v-model="groupForm.color" />
          </div>
          <div class="form-group">
            <label>分组描述</label>
            <textarea v-model="groupForm.description" placeholder="请输入分组描述" rows="3"></textarea>
          </div>

          <div class="form-actions">
            <button @click="submitGroupForm" class="submit-btn" :disabled="!groupForm.groupName">
              保存
            </button>
            <button v-if="groupFormMode === 'edit' && selectedGroupId" @click="confirmDeleteGroup" class="delete-btn">
              删除分组
            </button>
            <button @click="cancelGroupForm" class="cancel-btn">取消</button>
          </div>
        </div>

        <!-- 好友搜索 -->
        <div class="search-box">
          <input v-model="friendSearch" placeholder="搜索好友..." @input="filterFriends" />
        </div>

        <!-- 全部好友列表 - 可折叠 -->
        <div class="collapse-panel">
          <div class="panel-header" @click="togglePanel('friends-all')">
            <h4>全部好友 ({{ friends.length }})</h4>
            <i :class="`arrow ${panels['friends-all'] ? 'down' : 'right'}`"></i>
          </div>
          <div v-show="panels['friends-all']" class="panel-content">
            <div v-if="filteredAllFriends.length === 0" class="empty-state">
              <p>暂无好友</p>
            </div>
            <div v-for="friend in filteredAllFriends" :key="friend.friendId" class="friend-item">
              <div class="avatar-wrapper" @click.stop="showUserDetail(friend.friendId)">
                <img :src="friend.friendAvatar" class="friend-avatar" />
                <img v-if="friend.friendAvatarFrame" :src="friend.friendAvatarFrame" class="avatar-frame" />
              </div>
              <div class="friend-info">
                <div class="friend-name" title="好友用户名">
                  {{ friend.friendUsername || friend.friendNickname }}
                  <span v-if="friend.isStarred" class="star-icon" title="星标好友">★</span>
                  <span v-if="friend.isFollowing" class="following-tag" title="好友是否关注">已关注</span>
                  <span v-if="friend.remark" class="remark-tag" title="好友备注">备注: {{ friend.remark }}</span>
                </div>
                <div class="friend-status" :class="friend.status">
                  {{ friend.status === 'online' ? '在线' : '离线' }}
                </div>
              </div>
              <div class="friend-actions-dropdown">
                <button @click="toggleFriendActions(friend, 'all')" class="action-btn more">
                  操作 ⋮
                </button>
                <div v-if="activeFriendMenuItem === `all-${friend.friendId}`" class="dropdown-menu">
                  <button @click="startChatWithFriend(friend)" class="dropdown-item">
                    💬 发送消息
                  </button>
                  <button @click="editFriendRemark(friend)" class="dropdown-item">
                    ✏️ 修改备注
                  </button>
                  <button @click="toggleStarFriend(friend)" class="dropdown-item">
                    {{ friend.isStarred ? '⭐ 取消星标' : '🌟 设为星标' }}
                  </button>
                  <button @click="toggleBlockFriend(friend)" class="dropdown-item">
                    {{ friend.isBlocked ? '🔓 取消拉黑' : '🚫 拉黑' }}
                  </button>
                  <button @click="showAddToGroupDialog(friend)" class="dropdown-item">
                    📁 添加到分组
                  </button>
                  <button @click="confirmDeleteFriend(friend)" class="dropdown-item delete">
                    🗑️ 删除好友
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 黑名单列表 -->
        <div class="blacklist-section">
          <div class="section-header" @click="toggleBlacklist">
            <span class="section-title">🚫 黑名单({{ blacklist.length }})</span>
            <span class="toggle-icon">{{ showBlacklist ? '▼' : '▶' }}</span>
          </div>

          <div v-if="showBlacklist" class="blacklist-content">
            <div v-if="blacklistLoading" class="loading">加载中...</div>
            <div v-else-if="blacklist.length === 0" class="empty-list">
              暂无黑名单用户
            </div>
            <div v-else class="blacklist-grid">
              <div v-for="item in blacklist" :key="item.friendId" class="blacklist-item">
                <div class="avatar-wrapper" @click.stop="showUserDetail(item.friendId)">
                  <img :src="item.friendAvatar" class="friend-avatar" />
                  <img v-if="item.friendAvatarFrame" :src="item.friendAvatarFrame" class="avatar-frame" />
                </div>
                <div class="item-info">
                  <div class="item-name" title="用户名">{{ item.friendUsername }}
                    <span v-if="item.remark" class="remark-tag" title="用户备注">备注: {{ item.remark }}</span>
                  </div>
                  <div class="item-account">{{ item.isOnline === 'online' ? '在线' : '离线' }}</div>
                </div>
                <div class="item-actions">
                  <el-button type="danger" size="small" @click="toggleBlockFriend(item)"
                    :loading="removingId === item.friendId">
                    移出黑名单
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 好友分组列表 - 可折叠 -->
        <div v-for="group in friendGroups" :key="group.id" class="collapse-panel">
          <div class="panel-header" @click="togglePanel(`group-${group.id}`)">
            <h4 :style="{ color: group.color }">{{ group.groupName }} ({{ group.friends?.length || 0 }})</h4>
            <i :class="`arrow ${panels[`group-${group.id}`] ? 'down' : 'right'}`"></i>
          </div>
          <div v-show="panels[`group-${group.id}`]" class="panel-content">
            <div v-if="group.friends && group.friends.length > 0">
              <div v-for="friend in group.friends" :key="friend.friendId" class="friend-item">
                <div class="avatar-wrapper" @click.stop="showUserDetail(friend.friendId)">
                  <img :src="friend.friendAvatar" class="friend-avatar" />
                  <img v-if="friend.friendAvatarFrame" :src="friend.friendAvatarFrame" class="avatar-frame" />
                </div>
                <div class="friend-info">
                  <div class="friend-name" title="好友用户名">
                    {{ friend.friendUsername || friend.friendNickname }}
                    <span v-if="friend.isStarred" class="star-icon" title="星标好友">★</span>
                    <span v-if="friend.isFollowing" class="following-tag" title="好友是否关注">已关注</span>
                    <span v-if="friend.remark" class="remark-tag" title="好友备注">备注: {{ friend.remark }}</span>
                  </div>
                  <div class="friend-status" :class="friend.status">
                    {{ friend.status === 'online' ? '在线' : '离线' }}
                  </div>
                </div>
                <div class="friend-actions-dropdown">
                  <button @click="toggleFriendActions(friend, `group-${group.id}`)" class="action-btn more">
                    操作 ⋮
                  </button>
                  <div v-if="activeFriendMenuItem === `group-${group.id}-${friend.friendId}`" class="dropdown-menu">
                    <button @click="startChatWithFriend(friend)" class="dropdown-item">
                      💬 发送消息
                    </button>
                    <button @click="editFriendRemark(friend)" class="dropdown-item">
                      ✏️ 修改备注
                    </button>
                    <button @click="toggleStarFriend(friend)" class="dropdown-item">
                      {{ friend.isStarred ? '⭐ 取消星标' : '🌟 设为星标' }}
                    </button>
                    <button @click="toggleBlockFriend(friend)" class="dropdown-item">
                      {{ friend.isBlocked ? '🔓 取消拉黑' : '🚫 拉黑' }}
                    </button>
                    <button @click="removeFromGroup(friend)" class="dropdown-item">
                      📤 从分组移除
                    </button>
                    <button @click="confirmDeleteFriend(friend)" class="dropdown-item delete">
                      🗑️ 删除好友
                    </button>
                  </div>
                </div>
              </div>
            </div>
            <div v-else class="empty-state">
              <p>此分组暂无好友</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 关注列表 -->
      <div v-else-if="activeTab === 'following'" class="following-section">
        <div class="section-header">
          <h3>关注列表 ({{ following.length }})</h3>
        </div>

        <!-- 关注列表 -->
        <div class="following-tab-content">
          <div class="filter-tabs">
            <button v-for="tab in followingTabs" :key="tab.value"
              :class="['filter-tab', { active: activeFollowingTab === tab.value }]"
              @click="activeFollowingTab = tab.value">
              {{ tab.label }} ({{ tab.count }})
            </button>
          </div>

          <div v-if="filteredFollowing.length === 0" class="empty-state">
            <p>暂无数据</p>
          </div>

          <div v-for="follow in filteredFollowing" :key="follow.followingId" class="following-item">
            <div class="avatar-wrapper" @click.stop="showUserDetail(follow.followingId)">
              <img :src="follow.followingAvatar" class="following-avatar" />
              <img v-if="follow.followingAvatarFrame" :src="follow.followingAvatarFrame" class="avatar-frame" />
            </div>
            <div class="following-info">
              <div class="following-name" title="关注用户名">
                {{ follow.followingUsername || follow.followingNickname }}
                <span v-if="follow.isFriend" class="friend-tag" title="好友">好友</span>
                <span v-if="follow.relationType === 'special'" class="following-tag" title="互相关注的好友">{{
                  follow.relationType }}</span>
                <span v-if="follow.remark" class="remark-tag" title="好友备注">备注: {{ follow.remark }}</span>
              </div>
              <div class="following-time" title="关注时间">
                关注于 {{ formatTime(follow.createTime) }}
              </div>
            </div>
            <!-- 我的关注列表中的操作按钮 -->
            <div class="following-actions-dropdown">
              <button @click="toggleFollowingActions(follow.followingId, 'following')" class="action-btn more">
                操作 ⋮
              </button>
              <div v-if="activeFollowingId === `following-${follow.followingId}`" class="dropdown-menu">
                <button v-if="!follow.isFriend" @click="sendFriendRequest(follow.followingId)" class="dropdown-item">
                  👥 加为好友
                </button>
                <button @click="startChatWithStranger(follow)" class="dropdown-item">
                  💬 发送消息
                </button>
                <button @click="unfollowUser(follow)" class="dropdown-item delete">
                  ❌ 取消关注
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- 粉丝列表 -->
        <div class="follower-section">
          <div class="section-header">
            <h3>粉丝列表 ({{ followers.length }})</h3>
          </div>

          <div class="filter-tabs">
            <button v-for="tab in followerTabs" :key="tab.value"
              :class="['filter-tab', { active: activeFollowerTab === tab.value }]"
              @click="activeFollowerTab = tab.value">
              {{ tab.label }} ({{ tab.count }})
            </button>
          </div>

          <div v-if="filteredFollowers.length === 0" class="empty-state">
            <p>暂无数据</p>
          </div>

          <div v-for="follower in filteredFollowers" :key="follower.followerId" class="following-item">
            <div class="avatar-wrapper" @click.stop="showUserDetail(follower.followerId)">
              <img :src="follower.followerAvatar" class="following-avatar" />
              <img v-if="follower.followerAvatarFrame" :src="follower.followerAvatarFrame" class="avatar-frame" />
            </div>
            <div class="following-info">
              <div class="following-name" title="粉丝用户名">
                {{ follower.followerUsername || follower.followerNickname }}
                <span v-if="follower.isFriend" class="friend-tag" title="好友">好友</span>
                <span v-if="follower.relationType === 'special'" class="following-tag" title="互相关注的好友">{{
                  follower.relationType }}</span>
                <span v-if="follower.remark" class="remark-tag" title="备注">备注: {{ follower.remark }}</span>
              </div>
              <div class="following-time" title="关注时间">
                关注于 {{ formatTime(follower.createTime) }}
              </div>
            </div>
            <!-- 我的粉丝列表中的操作按钮 -->
            <div class="following-actions-dropdown">
              <button @click="toggleFollowingActions(follower.followerId, 'follower')" class="action-btn more">
                操作 ⋮
              </button>
              <div v-if="activeFollowingId === `follower-${follower.followerId}`" class="dropdown-menu">
                <button v-if="!follower.isFriend" @click="sendFriendRequest(follower.followerId)" class="dropdown-item">
                  👥 加为好友
                </button>
                <button @click="startChatWithStranger(follower)" class="dropdown-item">
                  💬 发送消息
                </button>
                <button v-if="!follower.isFollowing" @click="followUser(follower)" class="dropdown-item">
                  ➕ 关注
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 好友申请 -->
      <div v-else-if="activeTab === 'requests'" class="requests-section">
        <!-- 收到的申请 -->
        <div class="collapse-panel">
          <div class="panel-header" @click="togglePanel('requests-received')">
            <h3>收到的申请 ({{ pendingApplies.length }})</h3>
            <i :class="`arrow ${panels['requests-received'] ? 'down' : 'right'}`"></i>
          </div>
          <div v-show="panels['requests-received']" class="panel-content">
            <div class="filter-tabs">
              <button v-for="tab in receivedRequestTabs" :key="tab.value"
                :class="['filter-tab', { active: activeReceivedRequestTab === tab.value }]"
                @click="activeReceivedRequestTab = tab.value">
                {{ tab.label }} ({{ tab.count }})
              </button>
            </div>

            <div v-if="filteredReceivedApplies.length === 0" class="empty-state">
              <p>暂无好友申请</p>
            </div>
            <div v-for="apply in filteredReceivedApplies" :key="apply.id" class="request-item">
              <div class="avatar-wrapper" @click.stop="showUserDetail(apply.applicantId)">
                <img :src="apply.applicantAvatar" class="request-avatar" />
                <img v-if="apply.applicantAvatarFrame" :src="apply.applicantAvatarFrame" class="avatar-frame" />
              </div>
              <div class="request-info">
                <div class="request-name" title="申请人用户名">{{ apply.applicantUsername }}</div>
                <div class="request-msg" title="好友申请消息">
                  <span v-if="apply.applyMsg" class="request-msg-prefix">好友申请消息：</span>
                  {{ apply.applyMsg || '想添加你为好友' }}
                </div>
                <div class="request-time" title="好友申请时间">{{ formatTime(apply.createTime) }}</div>
                <div class="request-status" v-if="apply.status !== 'pending'">
                  <span :class="apply.status">{{ getStatusText(apply.status) }}</span>
                </div>
              </div>
              <div class="request-actions" v-if="apply.status === 'pending'">
                <button @click="acceptApply(apply.id)" class="accept-btn">同意</button>
                <button @click="rejectApply(apply.id)" class="reject-btn">拒绝</button>
              </div>
            </div>
          </div>
        </div>

        <!-- 发出的申请 -->
        <div class="collapse-panel">
          <div class="panel-header" @click="togglePanel('requests-sent')">
            <h3>发出的申请 ({{ myApplies.length }})</h3>
            <i :class="`arrow ${panels['requests-sent'] ? 'down' : 'right'}`"></i>
          </div>
          <div v-show="panels['requests-sent']" class="panel-content">
            <div class="filter-tabs">
              <button v-for="tab in sentRequestTabs" :key="tab.value"
                :class="['filter-tab', { active: activeSentRequestTab === tab.value }]"
                @click="activeSentRequestTab = tab.value">
                {{ tab.label }} ({{ tab.count }})
              </button>
            </div>

            <div v-if="filteredSentApplies.length === 0" class="empty-state">
              <p>暂无发出的申请</p>
            </div>
            <div v-for="apply in filteredSentApplies" :key="apply.id" class="request-item">
              <div class="avatar-wrapper" @click.stop="showUserDetail(apply.receiverId)">
                <img :src="apply.receiverAvatar" class="request-avatar" />
                <img v-if="apply.receiverAvatarFrame" :src="apply.receiverAvatarFrame" class="avatar-frame" />
              </div>
              <div class="request-info">
                <div class="request-name" title="申请人用户名">{{ apply.receiverUsername }}</div>
                <div class="request-msg" title="好友申请消息">
                  <span v-if="apply.applyMsg" class="request-msg-prefix">我的申请消息：</span>
                  {{ apply.applyMsg || '想添加你为好友' }}
                </div>
                <div class="request-time" title="好友申请时间">{{ formatTime(apply.createTime) }}</div>
                <div class="request-status" v-if="apply.status !== 'pending'">
                  <span :class="apply.status">{{ getStatusText(apply.status) }}</span>
                </div>
              </div>
              <div class="request-actions" v-if="apply.status === 'pending'">
                <span class="pending">等待对方同意</span>
                <button @click="cancelApply(apply.id)" class="cancel-btn">取消</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 添加到分组对话框 -->
    <div v-if="showGroupSelector" class="modal-overlay" @click.self="closeGroupSelector">
      <div class="modal-content">
        <div class="modal-header">
          <h3>选择分组</h3>
          <button @click="closeGroupSelector" class="close-btn">&times;</button>
        </div>
        <div class="modal-body">
          <div v-for="group in friendGroups" :key="group.id" class="group-option">
            <label>
              <input type="radio" v-model="selectedGroupForFriend" :value="group.groupName" />
              <span :style="{ color: group.color }">{{ group.groupName }}</span>
            </label>
          </div>
        </div>
        <div class="modal-actions">
          <button @click="addToGroup" class="confirm-btn" :disabled="!selectedGroupForFriend">
            确认
          </button>
          <button @click="closeGroupSelector" class="cancel-btn">取消</button>
        </div>
      </div>
    </div>

    <!-- 聊天窗口 -->
    <PrivateMessage v-if="activeChat" :visible="showChatModal" :targetUser="activeChat"
      @update:visible="handleChatVisibleChange" @close="closeChat" />

    <!-- 用户详情组件 -->
    <UserDetail v-model:visible="showUserDetailModal" :friendId="selectedUserId" @close="selectedUserId = null" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useUserInfoStore } from '@/stores/userInfo'
import PrivateMessage from './PrivateMessage.vue'
import UserDetail from './UserDetail.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as socialApi from '@/api/socialApi'

const userStore = useUserInfoStore()

// 状态定义
const activeTab = ref('friends')
const friendSearch = ref('')
const activeChat = ref(null)
const showUserDetailModal = ref(false)
const selectedUserId = ref(null)
// 添加状态
const showChatModal = ref(false)
// 黑名单相关
const showBlacklist = ref(false)
const blacklist = ref([])
const blacklistLoading = ref(false)
const removingId = ref(null)

// 分组相关
const showGroupForm = ref(false)
const groupFormMode = ref('create')
const groupForm = ref({
  groupName: '',
  sortOrder: 0,
  color: '#667eea',
  description: ''
})
const friendGroups = ref([])
const selectedGroupId = ref('')

// 操作下拉菜单
const activeFriendMenuItem = ref(null)
const activeFollowingId = ref(null)

// 添加到分组
const showGroupSelector = ref(false)
const selectedFriendForGroup = ref(null)
const selectedGroupForFriend = ref('')

// 数据
const friends = ref([])
const following = ref([])
const followers = ref([])
const pendingApplies = ref([])
const myApplies = ref([])
const recentChats = ref([])
const unreadCount = ref(0)

// 折叠面板状态
const panels = ref({
  'friends-all': true,
  'requests-received': true,
  'requests-sent': false
})

// 过滤标签状态
const activeReceivedRequestTab = ref('pending')
const activeSentRequestTab = ref('pending')
const activeFollowingTab = ref('all')
const activeFollowerTab = ref('all')

// 标签页配置
const tabs = [
  { id: 'messages', name: '最近消息' },
  { id: 'friends', name: '我的好友' },
  { id: 'following', name: '社交关系' },
  { id: 'requests', name: '好友申请' }
]

// 关注/粉丝标签
const followingTabs = computed(() => [
  { value: 'all', label: '全部', count: following.value.length },
  { value: 'friend', label: '互为好友', count: following.value.filter(f => f.isFriend).length },
  { value: 'special', label: '特别关注', count: following.value.filter(f => f.relationType === 'special').length }
])

const followerTabs = computed(() => [
  { value: 'all', label: '全部', count: followers.value.length },
  { value: 'friend', label: '互为好友', count: followers.value.filter(f => f.isFriend).length },
  { value: 'special', label: '特别关注', count: followers.value.filter(f => f.relationType === 'special').length }
])

// 申请标签
const receivedRequestTabs = computed(() => [
  { value: 'pending', label: '待处理', count: pendingApplies.value.filter(a => a.status === 'pending').length },
  { value: 'accepted', label: '已同意', count: pendingApplies.value.filter(a => a.status === 'accepted').length },
  { value: 'rejected', label: '已拒绝', count: pendingApplies.value.filter(a => a.status === 'rejected').length }
])

const sentRequestTabs = computed(() => [
  { value: 'pending', label: '等待中', count: myApplies.value.filter(a => a.status === 'pending').length },
  { value: 'accepted', label: '已同意', count: myApplies.value.filter(a => a.status === 'accepted').length },
  { value: 'rejected', label: '已拒绝', count: myApplies.value.filter(a => a.status === 'rejected').length }
])

// 计算属性
const filteredAllFriends = computed(() => {
  if (!friendSearch.value) return friends.value
  return friends.value.filter(friend =>
    friend.friendNickname?.includes(friendSearch.value) ||
    friend.friendUsername?.includes(friendSearch.value) ||
    friend.remark?.includes(friendSearch.value)
  )
})

const filteredFollowing = computed(() => {
  let filtered = following.value
  if (activeFollowingTab.value === 'friend') {
    filtered = filtered.filter(f => f.isFriend)
  } else if (activeFollowingTab.value === 'special') {
    filtered = filtered.filter(f => f.relationType === 'special')
  }
  return filtered
})

const filteredFollowers = computed(() => {
  let filtered = followers.value
  if (activeFollowerTab.value === 'friend') {
    filtered = filtered.filter(f => f.isFriend)
  } else if (activeFollowerTab.value === 'special') {
    filtered = filtered.filter(f => f.relationType === 'special')
  }
  return filtered
})

const filteredReceivedApplies = computed(() => {
  return pendingApplies.value.filter(a => a.status === activeReceivedRequestTab.value)
})

const filteredSentApplies = computed(() => {
  return myApplies.value.filter(a => a.status === activeSentRequestTab.value)
})

// 初始化分组面板
const initGroupPanels = () => {
  friendGroups.value.forEach(group => {
    panels.value[`group-${group.id}`] = false
  })
}

// 折叠面板切换
const togglePanel = (panelId) => {
  panels.value[panelId] = !panels.value[panelId]
}

// 方法
const switchTab = (tabId) => {
  activeTab.value = tabId
  activeFriendMenuItem.value = null
  activeFollowingId.value = null
  if (tabId === 'friends') {
    loadFriends()
    loadFriendGroups()
  } else if (tabId === 'following') {
    loadFollowing()
    loadFollowers()
  } else if (tabId === 'requests') {
    loadPendingApplies()
  } else if (tabId === 'messages') {
    loadRecentChats()
  }
}

const loadSocialInfo = async () => {
  try {
    const res = await socialApi.getSocialInfo()
    const data = res.data

    friends.value = data.friends || []
    following.value = data.following || []
    pendingApplies.value = data.pendingApplies || []
    recentChats.value = data.recentChats || []
    unreadCount.value = data.unreadCount || 0

  } catch (error) {
    console.error('加载社交信息失败:', error)
  }
}

const loadFriends = async () => {
  try {
    const res = await socialApi.getFriendList()
    friends.value = res.data || []
  } catch (error) {
    console.error('加载好友列表失败:', error)
  }
}

const loadFriendGroups = async () => {
  try {
    const res = await socialApi.getFriendGroups()
    friendGroups.value = res.data || []
    initGroupPanels()
  } catch (error) {
    console.error('加载好友分组失败:', error)
  }
}

const loadFollowing = async () => {
  try {
    const res = await socialApi.getFollowingList()
    following.value = res.data || []
  } catch (error) {
    console.error('加载关注列表失败:', error)
  }
}

const loadFollowers = async () => {
  try {
    const res = await socialApi.getFollowerList()
    followers.value = res.data || []
  } catch (error) {
    console.error('加载粉丝列表失败:', error)
  }
}

const loadPendingApplies = async () => {
  try {
    loadMyAllApplies()
    const res = await socialApi.getPendingApplies()
    pendingApplies.value = res.data || []
  } catch (error) {
    console.error('加载好友申请失败:', error)
  }
}

const loadMyAllApplies = async () => {
  try {
    const res = await socialApi.getAllMyApplies()
    myApplies.value = res.data || []
  } catch (error) {
    console.error('加载我的好友申请失败:', error)
  }
}

const loadRecentChats = async () => {
  try {
    const res = await socialApi.getRecentChats()
    recentChats.value = res.data || []
  } catch (error) {
    console.error('加载最近聊天失败:', error)
  }
}

// 切换黑名单显示
const toggleBlacklist = async () => {
  showBlacklist.value = !showBlacklist.value

  if (showBlacklist.value && blacklist.value.length === 0) {
    await loadBlacklist()
  }
}

// 加载黑名单
const loadBlacklist = async () => {
  blacklistLoading.value = true
  try {
    const res = await socialApi.getBlacklist()
    if (res.code === 0) {
      blacklist.value = res.data || []
    } else {
      ElMessage.error(res.msg || '加载黑名单失败')
    }
  } catch (error) {
    ElMessage.error('加载黑名单失败')
    console.error('加载黑名单失败:', error)
  } finally {
    blacklistLoading.value = false
  }
}

const getStatusText = (status) => {
  const map = {
    'pending': '待处理',
    'accepted': '已同意',
    'rejected': '已拒绝'
  }
  return map[status] || status
}

// 分组相关方法
const showCreateGroupDialog = () => {
  groupFormMode.value = 'create'
  groupForm.value = {
    groupName: '',
    sortOrder: 0,
    color: '#667eea',
    description: ''
  }
  selectedGroupId.value = ''
  showGroupForm.value = true
}

const showEditGroupDialog = () => {
  groupFormMode.value = 'edit'
  groupForm.value = {
    groupName: '',
    sortOrder: 0,
    color: '#667eea',
    description: ''
  }
  selectedGroupId.value = ''
  showGroupForm.value = true
}

const loadGroupData = async () => {
  if (!selectedGroupId.value) {
    groupForm.value = {
      groupName: '',
      sortOrder: 0,
      color: '#667eea',
      description: ''
    }
    return
  }

  try {
    const res = await socialApi.getFriendGroup(selectedGroupId.value)
    const group = res.data
    groupForm.value = {
      groupName: group.groupName,
      sortOrder: group.sortOrder || 0,
      color: group.color || '#667eea',
      description: group.description || ''
    }
  } catch (error) {
    ElMessage.error('加载分组信息失败')
  }
}

const submitGroupForm = async () => {
  if (!groupForm.value.groupName) {
    ElMessage.warning('请输入分组名称')
    return
  }

  try {
    if (groupFormMode.value === 'create') {
      await socialApi.createFriendGroup(groupForm.value)
      ElMessage.success('分组创建成功')
    } else {
      await socialApi.updateFriendGroup(selectedGroupId.value, groupForm.value)
      ElMessage.success('分组更新成功')
    }
    await loadFriendGroups()
    cancelGroupForm()
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '操作失败')
  }
}

const cancelGroupForm = () => {
  showGroupForm.value = false
  groupForm.value = {
    groupName: '',
    sortOrder: 0,
    color: '#667eea',
    description: ''
  }
  selectedGroupId.value = ''
}

const confirmDeleteGroup = async () => {
  if (!selectedGroupId.value) return

  try {
    await ElMessageBox.confirm(
      '确定要删除这个分组吗？分组中的好友将变为未分组状态。',
      '删除分组',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    try {
      await socialApi.deleteFriendGroup(selectedGroupId.value)
      ElMessage.success('分组删除成功')
      await loadFriendGroups()
      cancelGroupForm()
    } catch (error) {
      ElMessage.error(error.response?.data?.msg || '删除失败')
    }
  } catch (error) {
    // 用户点击取消，不做任何处理
  }
}

const removeFromGroup = async (friend) => {
  try {
    await ElMessageBox.confirm(
      `确定要将 ${friend.friendNickname || friend.friendUsername} 从当前分组移除吗？`,
      '移出分组',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    try {
      await socialApi.removeFriendFromGroup(friend.friendId)
      ElMessage.success('已从分组移除')
      await loadFriends()
      await loadFriendGroups()
      activeFriendMenuItem.value = null
    } catch (error) {
      ElMessage.error(error.response?.data?.msg || '操作失败')
    }
  } catch (error) {
    // 用户点击取消，不做任何处理
  }
}

const confirmDeleteFriend = async (friend) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除好友 ${friend.remark || friend.friendNickname || friend.friendUsername} 吗？`,
      '删除好友',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await socialApi.deleteFriend(friend.friendId)
    await loadFriends()
    ElMessage.success('好友删除成功')
    activeFriendMenuItem.value = null
  } catch (error) {
    // 用户点击取消，不做任何处理
  }
}

const toggleBlockFriend = async (friend) => {
  const action = friend.isBlocked ? '取消拉黑' : '拉黑'

  try {
    await ElMessageBox.confirm(
      `确定要${action} ${friend.friendUsername} 吗？`,
      `${action}好友`,
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    try {
      await socialApi.blockFriend({
        friendId: friend.friendId,
        isBlocked: !friend.isBlocked
      })
      ElMessage.success(`${action}成功`)
      await loadFriends()
      await loadBlacklist()
      activeFriendMenuItem.value = null
    } catch (error) {
      ElMessage.error(error.response?.data?.msg || '操作失败')
    }
  } catch (error) {
    // 用户点击取消，不做任何处理
  }
}

// 新增：设为星标/取消星标
const toggleStarFriend = async (friend) => {
  const action = friend.isStarred ? '取消星标' : '设为星标'

  try {
    await socialApi.starFriend({
      friendId: friend.friendId,
      isStarred: !friend.isStarred
    })
    ElMessage.success(`${action}成功`)
    await loadFriends()
    activeFriendMenuItem.value = null
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '操作失败')
  }
}

const unfollowUser = async (follow) => {
  try {
    await ElMessageBox.confirm(
      '确定要取消关注吗？',
      '取消关注',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    try {
      const res = await socialApi.unfollowUser(follow.followingId)
      if (res.code === 0) {
        ElMessage.success('已取消关注')
        await loadFollowing()
        activeFollowingId.value = null
      } else {
        ElMessage.error(res.msg || '取消失败')
      }
    } catch (error) {
      ElMessage.error(error.response?.data?.msg || '取消失败')
    }
  } catch (error) {
    // 用户点击取消，不做任何处理
  }
}

const followUser = async (follower) => {
  try {
    const res = await socialApi.followUser(follower.followerId)
    if (res.code === 0) {
      ElMessage.success('关注成功')
      await loadFollowers()
      activeFollowingId.value = null
    } else {
      ElMessage.error(res.msg || '关注失败')
    }

  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '关注失败')
  }
}

// 操作下拉菜单
const toggleFriendActions = (friend, listType) => {
  const menuId = `${listType}-${friend.friendId}`
  if (activeFriendMenuItem.value === menuId) {
    activeFriendMenuItem.value = null
  } else {
    activeFriendMenuItem.value = menuId
    activeFollowingId.value = null
  }
}

// 切换关注/粉丝列表的操作菜单
const toggleFollowingActions = (id, type) => {
  const menuId = `${type}-${id}`
  activeFollowingId.value = activeFollowingId.value === menuId ? null : menuId
}

// 添加到分组
const showAddToGroupDialog = (friend) => {
  selectedFriendForGroup.value = friend
  selectedGroupForFriend.value = ''
  showGroupSelector.value = true
  activeFriendMenuItem.value = null
}

const closeGroupSelector = () => {
  showGroupSelector.value = false
  selectedFriendForGroup.value = null
  selectedGroupForFriend.value = ''
}

const addToGroup = async () => {
  if (!selectedFriendForGroup.value || !selectedGroupForFriend.value) return

  try {
    await socialApi.addFriendToGroup(
      selectedFriendForGroup.value.friendId,
      selectedGroupForFriend.value
    )
    ElMessage.success('已添加到分组')
    await loadFriends()
    await loadFriendGroups()
    closeGroupSelector()
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '操作失败')
  }
}

// 用户详情
const showUserDetail = (userId) => {
  if (!userId) return
  selectedUserId.value = userId
  showUserDetailModal.value = true
  // 关闭所有下拉菜单
  activeFriendMenuItem.value = null
  activeFollowingId.value = null
}

// 其他现有方法...
const acceptApply = async (applyId) => {
  try {
    await socialApi.acceptFriendApply(applyId)
    await loadPendingApplies()
    await loadFriends()
    ElMessage.success('已接受好友申请')
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '操作失败')
  }
}

const rejectApply = async (applyId) => {
  try {
    await socialApi.rejectFriendApply(applyId)
    await loadPendingApplies()
    ElMessage.success('已拒绝好友申请')
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '操作失败')
  }
}

const editFriendRemark = async (friend) => {
  const currentRemark = friend.remark

  try {
    const { value } = await ElMessageBox.prompt('请输入备注名', '修改备注', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputValue: currentRemark,
      inputPattern: /.+/,
      inputErrorMessage: '备注名不能为空'
    })
    // 构建请求参数 - 确保是扁平结构
    const requestParams = {
      friendId: friend.friendId,  // 直接使用数字
      remark: value  // 直接使用字符串
    }

    // 发送请求
    const res = await socialApi.updateFriendRemark(requestParams)

    if (res && res.code === 0) {
      ElMessage.success('备注修改成功')
      friend.remark = value
    } else {
      ElMessage.error(res?.msg || '备注修改失败')
    }
  } catch (error) {
    console.error('修改备注出错:', error)
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  } finally {
    activeFriendMenuItem.value = null
  }
}


const sendFriendRequest = async (userId) => {
  try {
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
    activeFollowingId.value = null
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.msg || '发送失败')
    }
  }
}

const cancelApply = async (applyId) => {
  try {
    await socialApi.cancelFriendApply(applyId)
    ElMessage.success('已取消申请')
    await loadMyAllApplies()
  } catch (error) {
    ElMessage.error(error.response?.data?.msg || '取消失败')
  }
}

const openChat = (chat) => {
  activeChat.value = {
    userId: chat.relatedUserId,
    username: chat.username,
    nickname: chat.nickname,
    avatar: chat.avatar,
    avatarFrame: chat.avatarFrame,
    isFriend: chat.isFriend
  }
  showChatModal.value = true
}

const startChatWithFriend = (friend) => {
  activeChat.value = {
    userId: friend.friendId,
    username: friend.friendUsername,
    nickname: friend.friendNickname,
    avatar: friend.friendAvatar,
    avatarFrame: friend.friendAvatarFrame,
    isFriend: true
  }
  showChatModal.value = true
  activeFriendMenuItem.value = null
}

const startChatWithStranger = (stranger) => {
  const userId = stranger.followingId
  activeChat.value = {
    userId: userId,
    username: stranger.followingUsername,
    nickname: stranger.followingNickname,
    avatar: stranger.followingAvatar,
    avatarFrame: stranger.followingAvatarFrame,
    isFriend: false
  }
  showChatModal.value = true
  activeFollowingId.value = null
}

// 处理聊天窗口可见性变化
const handleChatVisibleChange = (val) => {
  showChatModal.value = val
  if (!val) {
    activeChat.value = null
  }
}

const closeChat = () => {
  showChatModal.value = false
  activeChat.value = null
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()

  if (date.toDateString() === now.toDateString()) {
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  }

  const diffDays = Math.floor((now - date) / (1000 * 60 * 60 * 24))
  if (diffDays < 7) {
    return `${diffDays}天前`
  }

  return date.toLocaleDateString()
}

const filterFriends = () => {
  // 搜索逻辑已经在计算属性中处理
}

// 点击其他地方关闭下拉菜单
const handleClickOutside = (e) => {
  if (!e.target.closest('.friend-actions-dropdown') && !e.target.closest('.following-actions-dropdown')) {
    activeFriendMenuItem.value = null
    activeFollowingId.value = null
  }
}

// 初始化
onMounted(() => {
  loadSocialInfo()
  loadFriendGroups()
  loadFollowers()
  loadBlacklist()
  document.addEventListener('click', handleClickOutside)

  // 定时刷新未读消息
  setInterval(() => {
    socialApi.getUnreadCount().then(res => {
      unreadCount.value = res.data
    })
  }, 100000)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})

// 监听分组数据变化
watch(friendGroups, () => {
  initGroupPanels()
}, { deep: true })
</script>


<style scoped>
.social-container {
  height: 90%;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
}

.tab-header {
  display: flex;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 0 20px;
  border-bottom: 1px solid #eaeaea;
}

.tab-btn {
  padding: 15px 25px;
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
  cursor: pointer;
  position: relative;
  transition: all 0.3s;
}

.tab-btn:hover {
  color: white;
  background: rgba(255, 255, 255, 0.1);
}

.tab-btn.active {
  color: white;
  border-bottom: 3px solid white;
}

.unread-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  background: #ff4757;
  color: white;
  font-size: 12px;
  min-width: 18px;
  height: 18px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
}

.tab-content {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #f0f0f0;
}

.section-header h3 {
  margin: 0;
  color: #333;
  font-size: 18px;
}

.section-subheader {
  margin: 15px 0 10px;
}

.section-subheader h4 {
  margin: 0;
  color: #666;
  font-size: 16px;
}

.group-actions {
  display: flex;
  gap: 10px;
}

.group-btn {
  background: none;
  border: 1px solid #667eea;
  color: #667eea;
  padding: 5px 12px;
  border-radius: 20px;
  cursor: pointer;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 4px;
  transition: all 0.3s;
}

.group-btn:hover {
  background: #667eea;
  color: white;
}

.group-form {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
  border: 1px solid #e0e0e0;
}

.group-form h4 {
  margin: 0 0 15px;
  color: #333;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  color: #666;
  font-size: 14px;
}

.form-group .required {
  color: #ff4757;
}

.form-group input[type="text"],
.form-group input[type="color"],
.form-group textarea,
.form-group select {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  font-size: 14px;
}

.form-group input[type="color"] {
  height: 40px;
  padding: 2px;
}

.form-actions {
  display: flex;
  gap: 10px;
  margin-top: 20px;
}

.submit-btn,
.delete-btn,
.cancel-btn {
  padding: 8px 20px;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.submit-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.submit-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.delete-btn {
  background: #ff4757;
  color: white;
}

.cancel-btn {
  background: #f0f0f0;
  color: #666;
}

.search-box {
  margin-bottom: 20px;
}

.search-box input {
  width: 95%;
  padding: 10px 15px;
  border: 1px solid #8dbedf;
  border-radius: 25px;
  font-size: 14px;
  outline: none;
  transition: border 0.3s;
}

.search-box input:focus {
  border-color: #667eea;
}

.search-box input::placeholder {
  color: #ec7ad4;
  cursor: pointer;
}


.all-friends-section {
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 2px solid #f0f0f0;
}

/* 聊天项样式 */
.chat-item,
.friend-item,
.following-item,
.request-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border-radius: 8px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: background 0.2s;
  position: relative;
}

.chat-item:hover,
.friend-item:hover,
.following-item:hover,
.request-item:hover {
  background: #f8f9fa;
}

.chat-item.unread {
  background: #e6f7ff;
}

.avatar-wrapper {
  position: relative;
  width: 50px;
  height: 50px;
  margin-right: 12px;
  cursor: pointer;
}

.chat-avatar,
.friend-avatar,
.following-avatar,
.request-avatar {
  width: 80%;
  height: 80%;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-frame {
  position: absolute;
  top: -15px;
  left: -15px;
  width: 70px;
  height: 70px;
  pointer-events: none;
  opacity: 70%;
}

.chat-info,
.friend-info,
.following-info,
.request-info {
  flex: 1;
  min-width: 0;
}

.chat-name,
.friend-name,
.following-name,
.request-name {
  font-weight: 500;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.friend-tag {
  background: #4caf50;
  color: white;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 10px;
}

.following-tag {
  background: #667eea;
  color: white;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 10px;
}

.remark-tag {
  background: #fcb0df;
  color: #333;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 10px;
}

.last-message,
.request-msg {
  font-size: 13px;
  color: #666;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.request-msg-prefix {
  font-weight: 500;
  color: #eb5b97;
}

.chat-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 5px;
}

.chat-time,
.following-time,
.request-time {
  font-size: 12px;
  color: #999;
}

.request-status {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
  display: inline-block;
}

.request-status .accepted {
  background: #4caf50;
  color: white;
}

.request-status .rejected {
  background: #ff4757;
  color: white;
}

.chat-unread {
  background: #ff4757;
  color: white;
  font-size: 12px;
  min-width: 20px;
  height: 20px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.friend-status {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
  display: inline-block;
}

.friend-status.online {
  background: #e8f5e9;
  color: #4caf50;
}

.friend-status.offline {
  background: #f5f5f5;
  color: #999;
}

.star-icon {
  color: #ffd700;
  font-size: 16px;
}

.friend-actions-dropdown,
.following-actions-dropdown {
  position: relative;
  overflow: visible !important;
  display: inline-block;
  z-index: 2;
}


/* 下拉菜单样式优化 */
.dropdown-menu {
  position: absolute;
  bottom: 10%;
  /* 显示在按钮上方 */
  right: 60px;
  margin-bottom: 5px;
  background: white;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
  z-index: 9999;
  /* 提高层级 */
  min-width: 120px;
  white-space: nowrap;
}

/* 如果需要显示在下方 */
.dropdown-menu.bottom {
  bottom: auto;
  top: 100%;
  margin-top: 5px;
  margin-bottom: 0;
}

.action-btn.more {
  background: none;
  border: 1px solid #e0e0e0;
  padding: 5px 10px;
  border-radius: 15px;
  font-size: 13px;
  cursor: pointer;
  color: #666;
}

.action-btn.more:hover {
  background: #f0f0f0;
}

.dropdown-item {
  width: 100%;
  padding: 10px 15px;
  border: none;
  background: none;
  text-align: left;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: background 0.2s;
}

.dropdown-item:hover {
  background: #f5f5f5;
}

.dropdown-item.delete:hover {
  background: #fff2f0;
  color: #ff4757;
}

/* 分组样式 */
.friend-group {
  margin-bottom: 25px;
}

.group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  padding: 0 5px;
}

.group-name {
  font-weight: 500;
  color: #459beb;
  font-size: 16px;
  font-style: italic;
}

.group-count {
  background: #f0f0f0;
  color: #ec3d3d;
  font-size: 14px;
  padding: 2px 8px;
  border-radius: 10px;
}

/* 模态框 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 12px;
  width: 400px;
  max-width: 90%;
  overflow: hidden;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.modal-header h3 {
  margin: 0;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #999;
}

.modal-body {
  padding: 20px;
}

.group-option {
  margin-bottom: 10px;
}

.group-option label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.group-option input[type="radio"] {
  width: 16px;
  height: 16px;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 20px;
  border-top: 1px solid #f0f0f0;
}

.cancel-btn,
.confirm-btn {
  padding: 8px 20px;
  border-radius: 20px;
  border: none;
  cursor: pointer;
  font-size: 14px;
}

.cancel-btn {
  background: #f5f5f5;
  color: #666;
}

.confirm-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.confirm-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #999;
}

.empty-state img {
  width: 120px;
  opacity: 0.5;
  margin-bottom: 20px;
}

.unread-total {
  font-size: 14px;
  color: #666;
}

.unread-count {
  color: #ff4757;
  font-weight: bold;
}

.request-actions {
  display: flex;
  gap: 5px;
}

.request-actions .pending {
  background: white;
  color: #a89804;
}

.request-actions .cancel-btn {
  background: #f5f5f5;
  color: #666;
}

.request-actions .cancel-btn:hover {
  background: #dab8cd;
  color: #d65252;
}

.accept-btn,
.reject-btn {
  padding: 5px 12px;
  border-radius: 12px;
  border: none;
  cursor: pointer;
  font-size: 12px;
}

.accept-btn {
  background: #52c41a;
  color: white;
}

.reject-btn {
  background: #f5222d;
  color: white;
}

.blacklist-section {
  margin-top: 20px;
  border-top: 1px solid #e8e8e8;
  padding-top: 15px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  cursor: pointer;
  background-color: #f5f5f5;
  border-radius: 4px;
}

.section-header:hover {
  background-color: #e8e8e8;
}

.section-title {
  font-weight: bold;
  color: #666;
}

.toggle-icon {
  color: #999;
}

.blacklist-content {
  margin-top: 10px;
  padding: 10px;
}

.blacklist-grid {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.blacklist-item {
  display: flex;
  align-items: center;
  padding: 10px;
  background-color: #fafafa;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.blacklist-item:hover {
  background-color: #f0f0f0;
}

.item-info {
  flex: 1;
  margin-left: 10px;
}

.item-name {
  font-weight: bold;
  color: #333;
}

.item-account {
  font-size: 12px;
  color: #999;
}

.item-actions {
  margin-left: 10px;
}

.loading,
.empty-list {
  text-align: center;
  padding: 20px;
  color: #999;
}

/* 折叠面板通用样式 */
.collapse-panel {
  margin-bottom: 12px;
  border: 1px solid #e1e1e1;
  border-radius: 8px;
  overflow: visible !important;
  background: #fff;
  z-index: 1;
}

.panel-header {
  padding: 12px 16px;
  background-color: #f8f9fa;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: background-color 0.2s;
}

.panel-header:hover {
  background-color: #f1f3f5;
}

.panel-header h3,
.panel-header h4 {
  margin: 0;
  font-size: 16px;
}

.arrow {
  display: inline-block;
  width: 0;
  height: 0;
  margin-left: 8px;
  transition: transform 0.2s;
}

.arrow.right {
  border-top: 5px solid transparent;
  border-bottom: 5px solid transparent;
  border-left: 5px solid #666;
}

.arrow.down {
  border-left: 5px solid transparent;
  border-right: 5px solid transparent;
  border-top: 5px solid #666;
}



/* 修复折叠动画 */
.slide-enter-active,
.slide-leave-active {
  transition: max-height 0.3s ease, opacity 0.2s ease;
  overflow: hidden;
}

.slide-enter-from,
.slide-leave-to {
  max-height: 0;
  opacity: 0;
}

.slide-enter-to,
.slide-leave-from {
  max-height: 1000px;
  /* 足够大的值 */
  opacity: 1;
}


.filter-tabs {
  display: flex;
  gap: 10px;
  margin-bottom: 15px;
  padding: 0 10px;
  flex-wrap: wrap;
}

.filter-tab {
  padding: 5px 15px;
  border: 1px solid #e0e0e0;
  border-radius: 20px;
  background: white;
  color: #666;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.filter-tab:hover {
  background: #f5f5f5;
}

.filter-tab.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-color: transparent;
}

.following-tab-content,
.follower-section {
  margin-bottom: 30px;
}

.follower-section {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 2px solid #f0f0f0;
}

.request-status .pending {
  background: #faad14;
  color: white;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
}

.request-status .accepted {
  background: #52c41a;
  color: white;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
}

.request-status .rejected {
  background: #f5222d;
  color: white;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
}

/* 修复折叠面板样式 */
.panel-content {
  transition: opacity 0.2s ease;
  padding: 10px;
  height: auto;
}
</style>