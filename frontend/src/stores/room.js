import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as groupChatApi from '@/api/groupChatApi'
import websocketService from '@/utils/websocket'

/**
 * Normalize incoming messages — expand extraData into top-level fields
 * so components can render file/image/video messages uniformly.
 */
function normalizeMessage(msg) {
  if (!msg) return msg
  const m = { ...msg }
  if (m.extraData && typeof m.extraData === 'object') {
    if (m.extraData.fileUrl) m.fileUrl = m.extraData.fileUrl
    if (m.extraData.fileName) m.fileName = m.extraData.fileName
    if (m.extraData.fileSize) m.fileSize = m.extraData.fileSize
    if (m.extraData.fileType) m.fileType = m.extraData.fileType
    if (m.extraData.thumbnailUrl) m.thumbnailUrl = m.extraData.thumbnailUrl
    if (m.extraData.imageUrl && !m.fileUrl) m.fileUrl = m.extraData.imageUrl
    if (m.extraData.quotedMessage) m.quotedMessage = m.extraData.quotedMessage
  }
  return m
}

export const useRoomStore = defineStore('room', () => {
  const myGroups = ref([])
  const currentGroupId = ref(null)
  const groupMessages = ref({})
  const groupMembers = ref({})
  const unreadCounts = ref({})
  const groupInfoCache = ref({})

  const currentGroup = computed(() => {
    if (!currentGroupId.value) return null
    return myGroups.value.find(g => g.groupId === currentGroupId.value)
        || groupInfoCache.value[currentGroupId.value]
        || null
  })

  const currentMessages = computed(() => {
    if (!currentGroupId.value) return []
    return (groupMessages.value[currentGroupId.value] || []).map(m => normalizeMessage(m))
  })

  const currentMembers = computed(() => {
    if (!currentGroupId.value) return []
    return groupMembers.value[currentGroupId.value] || []
  })

  const totalUnread = computed(() => {
    return Object.values(unreadCounts.value).reduce((sum, c) => sum + c, 0)
  })

  // ==================== Data Fetching ====================

  const fetchMyGroups = async () => {
    try {
      const res = await groupChatApi.getMyGroups()
      if (res.code === 0) {
        myGroups.value = res.data || []
        res.data.forEach(g => {
          if (g.unreadCount) unreadCounts.value[g.groupId] = g.unreadCount
        })
      }
    } catch (e) {
      console.error('获取群列表失败:', e)
    }
  }

  const fetchGroupInfo = async (groupId) => {
    try {
      const res = await groupChatApi.getGroupInfo(groupId)
      if (res.code === 0) {
        groupInfoCache.value[groupId] = res.data
        return res.data
      }
    } catch (e) {
      console.error('获取群信息失败:', e)
    }
    return null
  }

  const fetchGroupMembers = async (groupId) => {
    try {
      const res = await groupChatApi.getGroupMembers(groupId)
      if (res.code === 0) {
        groupMembers.value[groupId] = res.data || []
      }
    } catch (e) {
      console.error('获取群成员失败:', e)
    }
  }

  const fetchGroupHistory = async (groupId, lastMessageId = null, pageSize = 20) => {
    try {
      const res = await groupChatApi.getGroupHistory(groupId, {
        lastMessageId,
        pageSize
      })
      if (res.code === 0) {
        const msgs = (groupMessages.value[groupId] || [])
        const newMsgs = (res.data || []).reverse().map(m => normalizeMessage(m))
        const existingIds = new Set(msgs.map(m => m.messageId))
        const uniqueNew = newMsgs.filter(m => !existingIds.has(m.messageId))
        groupMessages.value[groupId] = [...uniqueNew, ...msgs]
        return res.data && res.data.length === pageSize
      }
    } catch (e) {
      console.error('获取群聊历史失败:', e)
    }
    return false
  }

  // ==================== Actions ====================

  const searchGroups = async (keyword) => {
    const res = await groupChatApi.searchGroups(keyword)
    if (res.code === 0) {
      return res.data || []
    }
    throw new Error(res.msg || '搜索群聊失败')
  }

  const isCurrentMember = computed(() => {
    if (!currentGroupId.value) return false
    return myGroups.value.some(g => g.groupId === currentGroupId.value)
  })

  const setCurrentGroup = async (groupId) => {
    currentGroupId.value = groupId
    if (groupId) {
      const info = await fetchGroupInfo(groupId)
      // Always fetch members, but history only for members
      await fetchGroupMembers(groupId).catch(() => {})
      if (info && info.myRole && info.myRole !== 'guest') {
        await fetchGroupHistory(groupId).catch(() => {})
      }
    }
  }

  const createGroup = async (data) => {
    const res = await groupChatApi.createGroup(data)
    if (res.code === 0) {
      await fetchMyGroups()
      return res.data
    }
    throw new Error(res.msg || '创建群聊失败')
  }

  const joinGroup = async (groupId, data) => {
    const res = await groupChatApi.joinGroup(groupId, data)
    if (res.code === 0) {
      await fetchMyGroups()
      return res.data
    }
    throw new Error(res.msg || '加入群聊失败')
  }

  const leaveGroup = async (groupId) => {
    const res = await groupChatApi.leaveGroup(groupId)
    if (res.code === 0) {
      myGroups.value = myGroups.value.filter(g => g.groupId !== groupId)
      if (currentGroupId.value === groupId) {
        currentGroupId.value = null
      }
    }
    return res
  }

  const dissolveGroup = async (groupId) => {
    const res = await groupChatApi.dissolveGroup(groupId)
    if (res.code === 0) {
      myGroups.value = myGroups.value.filter(g => g.groupId !== groupId)
      if (currentGroupId.value === groupId) {
        currentGroupId.value = null
      }
    }
    return res
  }

  const sendGroupMessage = async (groupId, data) => {
    // Always go through HTTP first to ensure persistence; WebSocket broadcasts to others
    const res = await groupChatApi.sendGroupMessage(groupId, data)
    if (res.code !== 0) throw new Error(res.msg || '发送失败')

    // Also try WebSocket for real-time delivery to others
    try {
      websocketService.sendGroupMessage(groupId, data)
    } catch (_) { /* WebSocket best-effort */ }

    return true
  }

  const kickMember = async (groupId, userId) => {
    const res = await groupChatApi.kickMember(groupId, userId)
    if (res.code === 0) {
      await fetchGroupMembers(groupId)
    }
    return res
  }

  const muteMember = async (groupId, userId, durationMinutes) => {
    const res = await groupChatApi.muteMember(groupId, userId, durationMinutes)
    if (res.code === 0) {
      await fetchGroupMembers(groupId)
    }
    return res
  }

  const unmuteMember = async (groupId, userId) => {
    const res = await groupChatApi.unmuteMember(groupId, userId)
    if (res.code === 0) {
      await fetchGroupMembers(groupId)
    }
    return res
  }

  const promoteToAdmin = async (groupId, userId) => {
    const res = await groupChatApi.changeMemberRole(groupId, userId, 'admin')
    if (res.code === 0) {
      await fetchGroupMembers(groupId)
    }
    return res
  }

  const changeMemberRole = async (groupId, userId, role) => {
    const res = await groupChatApi.changeMemberRole(groupId, userId, role)
    if (res.code === 0) {
      await fetchGroupMembers(groupId)
    }
    return res
  }

  const transferOwnership = async (groupId, newOwnerId) => {
    const res = await groupChatApi.transferOwnership(groupId, newOwnerId)
    if (res.code === 0) {
      await fetchMyGroups()
      await fetchGroupMembers(groupId)
      await fetchGroupInfo(groupId)
    }
    return res
  }

  const demoteSelf = async (groupId) => {
    const res = await groupChatApi.demoteSelf(groupId)
    if (res.code === 0) {
      await fetchGroupMembers(groupId)
    }
    return res
  }

  const updateGroupSettings = async (groupId, data) => {
    const res = await groupChatApi.updateGroup(groupId, data)
    if (res.code === 0) {
      await fetchMyGroups()
      await fetchGroupInfo(groupId)
    }
    return res
  }

  const markGroupAsRead = async (groupId, messageId) => {
    if (messageId) {
      await groupChatApi.markAsRead(groupId, messageId)
    }
    unreadCounts.value[groupId] = 0
  }

  // ==================== WebSocket Handlers ====================

  const handleGroupMessage = (msg) => {
    const norm = normalizeMessage(msg)
    if (!groupMessages.value[norm.groupId]) {
      groupMessages.value[norm.groupId] = []
    }
    // Update existing message (e.g. withdrawn status change)
    const idx = groupMessages.value[norm.groupId].findIndex(m => m.messageId === norm.messageId)
    if (idx !== -1) {
      groupMessages.value[norm.groupId][idx] = { ...groupMessages.value[norm.groupId][idx], ...norm }
    } else {
      groupMessages.value[norm.groupId].push(norm)
    }

    if (currentGroupId.value !== norm.groupId) {
      unreadCounts.value[norm.groupId] = (unreadCounts.value[norm.groupId] || 0) + 1
    } else {
      websocketService.sendGroupReadReceipt(norm.groupId, norm.messageId)
    }
  }

  const handleGroupEvent = (event) => {
    if (groupMessages.value[event.groupId]) {
      groupMessages.value[event.groupId].push({
        messageId: 'event_' + Date.now(),
        groupId: event.groupId,
        senderId: 0,
        senderName: '系统',
        contentType: 'system',
        content: event.content || formatEventText(event),
        createTime: event.createTime || Date.now()
      })
    }

    if (['member_joined', 'member_left', 'member_kicked', 'role_changed'].includes(event.eventType)) {
      fetchGroupMembers(event.groupId)
    }
    if (['group_edited', 'group_dissolved'].includes(event.eventType)) {
      fetchMyGroups()
    }
  }

  const handleGroupRead = () => { /* handled by component */ }

  const formatEventText = (event) => {
    const map = {
      member_joined: '有新成员加入了群聊',
      member_left: '有成员退出了群聊',
      member_kicked: '有成员被移出群聊',
      group_created: '群聊已创建',
      group_dissolved: '群聊已解散',
      role_changed: '成员角色已变更',
      member_muted: '有成员被禁言',
      member_unmuted: '有成员被解除禁言',
      mute_all: '已开启全员禁言',
      unmute_all: '已关闭全员禁言',
      group_edited: '群信息已更新'
    }
    return map[event.eventType] || event.eventType
  }

  return {
    myGroups,
    currentGroupId,
    currentGroup,
    groupMessages,
    currentMessages,
    groupMembers,
    currentMembers,
    unreadCounts,
    totalUnread,
    groupInfoCache,
    isCurrentMember,
    searchGroups,
    fetchMyGroups,
    fetchGroupInfo,
    fetchGroupMembers,
    fetchGroupHistory,
    setCurrentGroup,
    createGroup,
    joinGroup,
    leaveGroup,
    dissolveGroup,
    sendGroupMessage,
    kickMember,
    muteMember,
    unmuteMember,
    promoteToAdmin,
    changeMemberRole,
    transferOwnership,
    demoteSelf,
    updateGroupSettings,
    markGroupAsRead,
    handleGroupMessage,
    handleGroupEvent,
    handleGroupRead
  }
})
