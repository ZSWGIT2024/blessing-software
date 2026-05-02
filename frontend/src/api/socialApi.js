// socialApi.js
import request from '@/utils/requests'

// 获取社交信息汇总
export const getSocialInfo = () => {
  return request.get('/social/info')
}

// 发送好友申请
export const sendFriendApply = (data) => {
  return request.post('/social/friend/apply', data)
}

// 获取所有收到的好友申请
export const getPendingApplies = () => {
  return request.get('/social/friend/applies')
}

// 获取所有我发出的好友申请请求
export const getAllMyApplies = () => {
  return request.get('/social/friend/myApplies')
}

// 接受好友申请
export const acceptFriendApply = (applyId) => {
  return request.post(`/social/friend/accept/${applyId}`)
}

// 拒绝好友申请
export const rejectFriendApply = (applyId) => {
  return request.post(`/social/friend/reject/${applyId}`)
}

// 取消好友申请
export const cancelFriendApply = (applyId) => {
  return request.delete(`/social/friend/cancel/${applyId}`)
}

// 获取好友列表
export const getFriendList = () => {
  return request.get('/social/friends')
}

/**
 * 获取好友分组列表
 */
export const getFriendGroups = () => {
  return request.get('/social/groups')
}

/**
 * 创建好友分组
 * @param {Object} data - { groupName, color, description }
 */
export const createFriendGroup = (data) => {
  return request.post('/social/groups', data)
}

/**
 * 更新好友分组
 * @param {Number} groupId - 分组ID
 * @param {Object} data - { groupName, color, description }
 */
export const updateFriendGroup = (groupId, data) => {
  return request.put(`/social/groups/${groupId}`, data)
}

/**
 * 删除好友分组
 * @param {Number} groupId - 分组ID
 */
export const deleteFriendGroup = (groupId) => {
  return request.delete(`/social/groups/${groupId}`)
}

/**
 * 获取分组详情
 * @param {Number} groupId - 分组ID
 */
export const getFriendGroup = (groupId) => {
  return request.get(`/social/groups/${groupId}`)
}

/**
 * 添加好友到分组
 * @param {Number} friendId - 好友ID
 * @param {String} groupName - 分组名称
 */
export const addFriendToGroup = (friendId, groupName) => {
  return request.post(`/social/friends/${friendId}/group`, null, {
    params: { groupName }
  })
}

/**
 * 从分组移除好友
 * @param {Number} friendId - 好友ID
 */
export const removeFriendFromGroup = (friendId) => {
  return request.delete(`/social/friends/${friendId}/group`)
}

/**
 * 设为星标/取消星标好友
 * @param {Object} data - { friendId, isStarred }
 */
export const starFriend = (data) => {
  return request.put('/social/friend/star', data)
}

// ==================== 用户详情 ====================

/**
 * 获取用户详情
 * @param {Number} userId - 用户ID
 */
export const getUserDetail = (userId) => {
  return request.get(`/social/user/${userId}/detail`)
}

// 修改好友备注
export const updateFriendRemark = (data) => {
  return request.post('/social/friend/remark', data)
}

// 删除好友
export const deleteFriend = (friendId) => {
  return request.delete(`/social/friend/${friendId}`)
}

// 获取黑名单列表
export const getBlacklist = () => {
  return request.get('/social/blacklist')
}

// 拉黑/取消拉黑好友
export const blockFriend = (data) => {
  return request.put('/social/friend/block', data)
}

// 关注用户
export const followUser = (followingId) => {
  return request.post(`/social/follow/${followingId}`)
}

// 获取关注列表
export const getFollowingList = () => {
  return request.get('/social/following')
}

// 获取粉丝列表
export const getFollowerList = () => {
  return request.get('/social/follower')
}

// 取消关注
export const unfollowUser = (followingId) => {
  return request.delete(`/social/follow/${followingId}`)
}

// 发送消息
export const sendMessage = (data) => {
  return request.post('/social/message/send', data)
}

// 获取通知列表
export const getNotifications = (params) => {
  return request.get('/social/notifications', { params })
}

// 获取未读消息数量
export const getUnreadCount = () => {
  return request.get('/social/notifications/unread/count')
}

// 标记通知为已读
export const markAsRead = (notificationId) => {
  return request.put(`/social/notification/read/${notificationId}`)
}

// 获取最近聊天列表
export const getRecentChats = () => {
  return request.get('/social/chats/recent')
}

// 获取聊天历史
//使用params对象（推荐）
export const getChatHistory = (relatedUserId, params) => {
    return request.get(`/social/chats/history/${relatedUserId}`, { 
            params: {
                lastMessageId: params.lastMessageId || null,
                pageSize: params.pageSize || 20
            }
        });
}

//删除聊天记录
export const deleteChatHistory = (friendId) => {
  return request.delete(`/social/chats/history/${friendId}`)
}

// 搜索用户
export const searchUsers = (params) => {
  return request.get('/social/search/users', { params })
}

// socialApi.js - 扩展新接口

// 消息撤回
export const withdrawMessage = (data) => {
  return request.post('/social/message/withdraw', data)
}

// 获取消息状态
export const getMessageStatus = (messageId) => {
  return request.get(`/social/message/status/${messageId}`)
}

// 更新消息状态
export const updateMessageStatus = (data) => {
  return request.put('/social/message/status', data)
}

// 上传聊天文件
export const uploadChatFile = (formData) => {
  return request.post('/social/file/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 获取聊天文件列表
export const getChatFiles = (params) => {
  // 确保参数格式正确
  const requestParams = {
    fileType: params.fileType || null,
    pageNum: params.pageNum || 1,
    pageSize: params.pageSize || 20
  }
  return request.get('/social/files', { params: requestParams })
}

//删除消息信息
export const deleteMessage = (messageId) => {
  return request.delete(`/social/message/delete/${messageId}`)
}

// 删除聊天文件
export const deleteChatFile = (fileId) => {
  return request.delete(`/social/file/${fileId}`)
}

// 批量标记消息为已读
export const markMessagesAsRead = (friendId) => {
  return request.put(`/social/messages/read/${friendId}`)
}