import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { useTokenStore } from '@/stores/token'
import request from '@/utils/requests'
import websocketService from '@/utils/websocket'
import * as socialApi from '@/api/socialApi'


export const useUserInfoStore = defineStore('userInfo', () => {
    // ========== 状态定义 ==========
    const isAuthenticated = ref(localStorage.getItem('isAuthenticated') === 'true')
    const isAdmin = ref(localStorage.getItem('isAdmin') === 'true')
    const id = ref(localStorage.getItem('id') || `user_${Date.now()}`)
    //定义状态相关的内容
    const tokenStore = useTokenStore()

    // 用户资料
    const currentUser = ref({})
    const setInfo = (newInfo) => {
        currentUser.value = newInfo
    }

    const removeInfo = () => {
        currentUser.value = {}
    }


    const friendRequests = ref([])
    const blockedUsers = ref([])
    const chats = ref({})
    const notifications = ref([])

    // ========== 新增：WebSocket 相关状态 ==========
    const wsConnected = ref(false)
    const typingUsers = ref({}) // { userId: timestamp }
    const messageQueue = ref([]) // 离线消息队列

    // ========== 新增：初始化 WebSocket ==========
    const initWebSocket = (userId, token) => {
        if (!userId || !token) return

        try {
            websocketService.connect(userId, token)

            websocketService.onStatusChange((status) => {
                wsConnected.value = status === 'connected'

                if (status === 'connected') {
                    sendOfflineMessages()
                } else if (status === 'error' || status === 'disconnected') {
                    console.warn('WebSocket连接失败，将使用HTTP模式')
                    wsConnected.value = false
                }
            })

            // 监听新消息
            websocketService.on('newMessage', (message) => {
                handleNewMessage(message)
            })

            // 监听消息已读
            websocketService.on('messageRead', (data) => {
                handleMessageRead(data)
            })

            // 监听用户在线状态
            websocketService.on('userOnline', (data) => {
                handleUserOnline(data)
            })

            // 监听输入状态
            websocketService.onTyping((userId, isTyping) => {
                handleTypingStatus(userId, isTyping)
            })
        } catch (error) {
            console.error('WebSocket初始化失败，将使用HTTP模式', error)
            wsConnected.value = false
        }
    }

    // ========== 新增：处理新消息 ==========
    const handleNewMessage = (message) => {
        // 更新最近聊天列表
        updateRecentChats(message)

        // 如果是当前活跃的聊天，直接添加到消息列表
        if (activeChat.value && activeChat.value.userId === message.senderId) {
            // 添加到当前聊天消息列表
            const chatMessages = getChatMessages(message.senderId)
            chatMessages.push(message)

            // 标记已读
            markMessageAsRead(message.senderId, message.messageId)
            websocketService.sendReadReceipt(message.senderId, message.messageId)
        } else {
            // 否则增加未读计数
            unreadCount.value++

            // 更新联系人的未读计数
            updateUnreadCount(message.senderId)
        }

        // 触发通知（如果页面不在焦点）
        if (document.hidden) {
            showNotification(message)
        }
    }

    // ========== 新增：处理消息已读 ==========
    const handleMessageRead = (data) => {
        const { senderId, messageId } = data
        const chatMessages = getChatMessages(senderId)

        const message = chatMessages.find(m => m.messageId === messageId)
        if (message) {
            message.status = 'read'
        }
    }

    // ========== 新增：处理用户在线状态 ==========
    const handleUserOnline = (data) => {
        const { userId, online } = data

        // 更新好友列表中的在线状态
        const friend = socialInfo.value.friends.find(f => f.friendId === userId)
        if (friend) {
            friend.isOnline = online ? 'online' : 'offline'
        }

        // 更新最近聊天中的在线状态
        const recentChat = socialInfo.value.recentChats.find(c => c.userId === userId)
        if (recentChat) {
            recentChat.isOnline = online ? 'online' : 'offline'
        }
    }

    // ========== 新增：处理输入状态 ==========
    const handleTypingStatus = (userId, isTyping) => {
        if (isTyping) {
            typingUsers.value[userId] = Date.now()
        } else {
            delete typingUsers.value[userId]
        }
    }

    // ========== 新增：检查用户是否正在输入 ==========
    const isUserTyping = (userId) => {
        const lastTyping = typingUsers.value[userId]
        if (!lastTyping) return false
        // 5秒内算正在输入
        return Date.now() - lastTyping < 5000
    }

    // ========== 新增：发送输入状态 ==========
    const sendTypingStatus = (receiverId, isTyping) => {
        websocketService.sendTypingStatus(receiverId, isTyping)
    }

    // ========== 新增：发送消息（支持离线队列） ==========
    // 在 userInfo.js 中修改 sendMessage 方法
    const sendMessage = async (messageData) => {
        if (websocketService.isConnected()) {
            // WebSocket 在线，通过 WebSocket 发送实时消息
            const success = websocketService.sendMessage(messageData)
            if (!success) {
                // WebSocket 发送失败，回退到 HTTP
                messageQueue.value.push({
                    ...messageData,
                    timestamp: Date.now()
                })
                // 同时尝试 HTTP 发送作为备份
                return sendMessageViaHttp(messageData)
            }
            return success
        } else {
            // WebSocket 离线，加入队列并通过 HTTP 发送
            messageQueue.value.push({
                ...messageData,
                timestamp: Date.now()
            })
            // 尝试 HTTP 发送
            return sendMessageViaHttp(messageData)
        }
    }

    // 新增：通过 HTTP 发送消息
    const sendMessageViaHttp = async (messageData) => {
        try {
            const res = await socialApi.sendMessage(messageData)
            return res.code === 0
        } catch (error) {
            console.error('HTTP消息发送失败:', error)
            return false
        }
    }

    // ========== 新增：发送离线消息 ==========
    const sendOfflineMessages = () => {
        if (!websocketService.isConnected() || messageQueue.value.length === 0) return

        const queue = [...messageQueue.value]
        messageQueue.value = []

        queue.forEach(msg => {
            websocketService.sendMessage(msg)
        })
    }

    // ========== 新增：清理过期的输入状态 ==========
    setInterval(() => {
        const now = Date.now()
        Object.entries(typingUsers.value).forEach(([userId, timestamp]) => {
            if (now - timestamp > 5000) {
                delete typingUsers.value[userId]
            }
        })
    }, 3000)

    // ========== 新增：页面可见性变化处理 ==========
    const handleVisibilityChange = () => {
        if (!document.hidden && activeChat.value) {
            // 页面重新可见时，标记当前聊天为已读
            markChatAsRead(activeChat.value.userId)
        }
    }

    document.addEventListener('visibilitychange', handleVisibilityChange)

    // ========== 新增：显示浏览器通知 ==========
    const showNotification = (message) => {
        if (!('Notification' in window)) return

        if (Notification.permission === 'granted') {
            new Notification('新消息', {
                body: `${message.senderName}: ${message.content}`,
                icon: message.senderAvatar
            })
        } else if (Notification.permission !== 'denied') {
            Notification.requestPermission()
        }
    }

    // ========== 计算属性 ==========
    const unreadNotifications = computed(() => notifications.value.filter(n => !n.read))
    const userProfile = computed(() => ({
        ...currentUser.value,
        isAdmin: isAdmin.value,
        friendCount: friends.value.length
    }))
    const isCurrentUser = computed(() => (id) => id === id.value)


    // 添加社交相关状态
    const socialInfo = ref({
        friends: [],
        following: [],
        followers: [],
        pendingApplies: [],
        recentChats: [],
        notifications: []
    })

    const unreadCount = ref(0)
    const activeChat = ref(null)

    // 社交相关计算属性
    const onlineFriends = computed(() =>
        socialInfo.value.friends.filter(f => f.isOnline)
    )

    const unreadMessages = computed(() => {
        let count = 0
        socialInfo.value.recentChats.forEach(chat => {
            count += chat.unreadCount || 0
        })
        return count
    })

    // 社交相关方法
    const loadSocialInfo = async () => {
        try {
            const res = await socialApi.getSocialInfo()
            socialInfo.value = {
                ...socialInfo.value,
                ...res.data
            }
            updateUnreadCount()
            return res.data
        } catch (error) {
            console.error('加载社交信息失败:', error)
            return null
        }
    }

    const updateUnreadCount = async () => {
        try {
            const res = await socialApi.getUnreadCount()
            unreadCount.value = res.data || 0
        } catch (error) {
            console.error('获取未读消息数失败:', error)
        }
    }

    const markChatAsRead = async (userId) => {
        try {
            // 更新本地状态
            const chat = socialInfo.value.recentChats.find(c => c.userId === userId)
            if (chat) {
                chat.unreadCount = 0
            }

            // 调用API
            await updateUnreadCount()
        } catch (error) {
            console.error('标记聊天已读失败:', error)
        }
    }

    const acceptFriendRequest = async (applyId) => {
        try {
            await socialApi.acceptFriendApply(applyId)

            // 更新本地状态
            socialInfo.value.pendingApplies = socialInfo.value.pendingApplies.filter(
                apply => apply.id !== applyId
            )

            // 重新加载好友列表
            await loadFriends()

            return true
        } catch (error) {
            console.error('接受好友申请失败:', error)
            throw error
        }
    }

    const rejectFriendRequest = async (applyId) => {
        try {
            await socialApi.rejectFriendApply(applyId)

            // 更新本地状态
            socialInfo.value.pendingApplies = socialInfo.value.pendingApplies.filter(
                apply => apply.id !== applyId
            )

            return true
        } catch (error) {
            console.error('拒绝好友申请失败:', error)
            throw error
        }
    }

    const loadFriends = async () => {
        try {
            const res = await socialApi.getFriendList()
            socialInfo.value.friends = res.data || []
            return socialInfo.value.friends
        } catch (error) {
            console.error('加载好友列表失败:', error)
            return []
        }
    }

    const loadFollowing = async () => {
        try {
            const res = await socialApi.getFollowingList()
            socialInfo.value.following = res.data || []
            return socialInfo.value.following
        } catch (error) {
            console.error('加载关注列表失败:', error)
            return []
        }
    }

    const loadNotifications = async (pageNum = 1, pageSize = 20) => {
        try {
            const res = await socialApi.getNotifications({ pageNum, pageSize })
            socialInfo.value.notifications = res.data || []
            return socialInfo.value.notifications
        } catch (error) {
            console.error('加载通知失败:', error)
            return []
        }
    }

    const followUser = async (followingId, remark = '') => {
        try {
            await socialApi.followUser({ followingId, remark })

            // 重新加载关注列表
            await loadFollowing()

            return true
        } catch (error) {
            console.error('关注用户失败:', error)
            throw error
        }
    }

    const unfollowUser = async (followingId) => {
        try {
            await socialApi.unfollowUser(followingId)

            // 重新加载关注列表
            await loadFollowing()

            return true
        } catch (error) {
            console.error('取消关注失败:', error)
            throw error
        }
    }

    const deleteFriend = async (friendId) => {
        try {
            await socialApi.deleteFriend(friendId)

            // 更新本地状态
            socialInfo.value.friends = socialInfo.value.friends.filter(
                friend => friend.friendId !== friendId
            )

            return true
        } catch (error) {
            console.error('删除好友失败:', error)
            throw error
        }
    }

    const setActiveChat = (chatUser) => {
        activeChat.value = chatUser
    }

    const clearActiveChat = () => {
        activeChat.value = null
    }

    // ========== 方法实现 ==========


    function login(userData) {
        isAuthenticated.value = true
        if (userData.role === 'admin') {
            isAdmin.value = true
        } else {
            isAdmin.value = false
        }
        persistToLocalStorage()
        //刷新页面
        window.location.reload()
        return true
    }

    function logout() {
        websocketService.disconnect()
        isAuthenticated.value = false
        isAdmin.value = false
        tokenStore.removeToken()
        clearLocalStorage()
        removeInfo()
        initUserData()
        return Promise.resolve()
    }
    async function initUserData() {
        if (!tokenStore.accessToken) {
            console.log('未登录，跳过获取用户信息');
            currentUser.value = {
                ...currentUser.value,
                username: '当前用户',
                avatar: 'https://web-aliyun-zsw.oss-cn-shanghai.aliyuncs.com/%E6%83%A0%E5%A4%A9%E4%B8%8BLOGO.png',
                status: 'offline'
            }
            return
        }
        try {
            const res = await request.get('/user/userInfo')
            if (res.code === 0 && res.data) {
                const data = res.data
                localStorage.setItem('userInfo', JSON.stringify(data));
                currentUser.value = data
                console.log(data);
                

                if (data.id && tokenStore.accessToken) {
                    initWebSocket(data.id, tokenStore.accessToken)
                }

                if ('Notification' in window && Notification.permission === 'default') {
                    Notification.requestPermission()
                }

                setInterval(updateUnreadCount, 30000)
            }
        } catch {
            console.log('服务器获取用户信息失败，加载本地存储数据。');
            currentUser.value = {
                ...currentUser.value,
                username: '当前用户',
                avatar: 'https://web-aliyun-zsw.oss-cn-shanghai.aliyuncs.com/%E6%83%A0%E5%A4%A9%E4%B8%8BLOGO.png',
                status: 'offline'
            }
        }
    }

    function persistToLocalStorage() {
        localStorage.setItem('isAuthenticated', isAuthenticated.value);
        localStorage.setItem('isAdmin', currentUser.value.userType === 1 ? 'true' : 'false');
        localStorage.setItem('id', currentUser.value.id);
        localStorage.setItem('username', currentUser.value.username);
        localStorage.setItem('avatar', currentUser.value.avatar); // 新增
        localStorage.setItem('avatarFrame', currentUser.value.avatarFrame); // 新增
        localStorage.setItem('phone', currentUser.value.phone);
        localStorage.setItem('createTime', currentUser.value.createTime);
        localStorage.setItem('updateTime', currentUser.value.updateTime);
        localStorage.setItem('bio', currentUser.value.bio);
        localStorage.setItem('isOnline', currentUser.value.isOnline);
        localStorage.setItem('level', currentUser.value.level);
        localStorage.setItem('loginDays', currentUser.value.loginDays);
        localStorage.setItem('totalLoginDays', currentUser.value.totalLoginDays);
        localStorage.setItem('userType', currentUser.value.userType);
        localStorage.setItem('vipType', currentUser.value.vipType);
        localStorage.setItem('vipExpireTime', currentUser.value.vipExpireTime);
        localStorage.setItem('userInfo', JSON.stringify(currentUser.value));
    }

    function clearLocalStorage() {
        localStorage.removeItem('isAuthenticated');
        localStorage.removeItem('isAdmin');
        localStorage.removeItem('id');
        localStorage.removeItem('username');
        localStorage.removeItem('avatar'); // 新增
        localStorage.removeItem('avatarFrame'); // 新增
        localStorage.removeItem('phone');
        localStorage.removeItem('createTime');
        localStorage.removeItem('updateTime');
        localStorage.removeItem('bio');
        localStorage.removeItem('isOnline');
        localStorage.removeItem('level');
        localStorage.removeItem('loginDays');
        localStorage.removeItem('totalLoginDays');
        localStorage.removeItem('userType');
        localStorage.removeItem('vipType');
        localStorage.removeItem('vipExpireTime');
        localStorage.removeItem('userInfo');
    }

    // 社交关系方法
    function sendFriendRequest(toUserId) {
        if (isCurrentUser.value(toUserId)) return

        friendRequests.value.push({
            id: Date.now().toString(),
            from: { ...currentUser.value },
            to: toUserId,
            time: new Date().toISOString()
        })
    }

    function removeFriend(id) {
        friends.value = friends.value.filter(f => f.id !== id)
    }

    function blockUser(id) {
        if (!blockedUsers.value.includes(id)) {
            blockedUsers.value.push(id)
            removeFriend(id)
        }
    }

    function openPrivateChat(id) {
        if (!chats.value[id]) {
            chats.value[id] = []
        }
        return chats.value[id]
    }

    // 修改 getChatMessages 确保始终返回数组
    function getChatMessages(targetUserId) {
        if (!targetUserId) return []
        if (!chats.value[targetUserId]) {
            chats.value[targetUserId] = []
        }
        return chats.value[targetUserId] || []
    }


    function markMessageAsRead(chatId, messageId) {
        const message = this.chats[chatId]?.find(m => m.id === messageId)
        if (message) {
            message.read = true
        }
    }

    function sendNotification(notification) {
        notifications.value.push({
            ...notification,
            id: Date.now().toString(),
            read: false
        })
    }

    function markNotificationAsRead(notificationId) {
        const notification = notifications.value.find(n => n.id === notificationId)
        if (notification) {
            notification.read = true
        }
    }

    // ========== 暴露状态和方法 ==========
    return {
        // 状态
        isAuthenticated,
        isAdmin,
        id,
        currentUser,
        friendRequests,
        blockedUsers,
        chats,
        notifications,

        // 计算属性
        onlineFriends,
        unreadNotifications,
        userProfile,
        isCurrentUser,

        // 社交状态
        socialInfo,
        unreadCount,
        activeChat,

        // 新增 WebSocket 相关
        wsConnected,
        typingUsers,
        messageQueue,

        // 社交计算属性
        onlineFriends,
        unreadMessages,

        // 社交方法
        loadSocialInfo,
        updateUnreadCount,
        markChatAsRead,
        loadFriends,
        loadFollowing,
        loadNotifications,
        followUser,
        unfollowUser,
        deleteFriend,
        setActiveChat,
        clearActiveChat,

        // 认证方法
        login,
        logout,
        initUserData,
        setInfo,
        removeInfo,
        // 社交方法
        sendFriendRequest,
        acceptFriendRequest,
        rejectFriendRequest,
        removeFriend,
        blockUser,
        openPrivateChat,
        getChatMessages,
        markMessageAsRead,
        // 通知方法
        sendNotification,
        markNotificationAsRead,

        // 新增 WebSocket 相关
        isUserTyping,
        sendTypingStatus,
        sendMessage,

        // 状态检查方法
        isFriend: (id) => friends.value.some(f => f.id === id),
        hasPendingRequest: (id) => friendRequests.value.some(r => r.from.id === id),
        getChatMessages: (id) => chats.value[id] || []
    }
}, { persist: true })