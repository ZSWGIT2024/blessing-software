import request from '@/utils/requests'

// Group CRUD
export const createGroup = (data) => request.post('/group/create', data)
export const updateGroup = (groupId, data) => request.put(`/group/${groupId}`, data)
export const dissolveGroup = (groupId) => request.delete(`/group/${groupId}`)

// Membership
export const joinGroup = (groupId, data) => request.post(`/group/${groupId}/join`, data)
export const leaveGroup = (groupId) => request.post(`/group/${groupId}/leave`)
export const kickMember = (groupId, userId) => request.post(`/group/${groupId}/kick`, { userId })

// Roles
export const changeMemberRole = (groupId, userId, role) => request.put(`/group/${groupId}/member/role`, { userId, role })
export const transferOwnership = (groupId, newOwnerId) => request.put(`/group/${groupId}/transfer`, { newOwnerId })

// Mute
export const muteMember = (groupId, userId, durationMinutes) =>
  request.put(`/group/${groupId}/member/mute`, { userId, durationMinutes })
export const unmuteMember = (groupId, userId) => request.put(`/group/${groupId}/member/unmute`, { userId })
export const setMuteAll = (groupId, isMutedAll) => request.put(`/group/${groupId}/mute-all`, { isMutedAll })

// Messages
export const sendGroupMessage = (groupId, data) => request.post(`/group/${groupId}/message/send`, data)
export const getGroupHistory = (groupId, params) => request.get(`/group/${groupId}/messages`, { params })
export const withdrawGroupMessage = (groupId, data) => request.post(`/group/${groupId}/message/withdraw`, data)
export const deleteGroupMessage = (groupId, messageId) => request.delete(`/group/${groupId}/message/${messageId}`)

// Read Status
export const markAsRead = (groupId, messageId) => request.put(`/group/${groupId}/read/${messageId}`)
export const getReadUsers = (groupId, messageId) => request.get(`/group/${groupId}/message/${messageId}/read-users`)
export const getGroupUnreadCount = (groupId) => request.get(`/group/${groupId}/unread`)

// Query
export const getMyGroups = () => request.get('/group/my')
export const getGroupInfo = (groupId) => request.get(`/group/${groupId}`)
export const searchGroups = (keyword) => request.get('/group/search', { params: { keyword } })
export const getGroupMembers = (groupId) => request.get(`/group/${groupId}/members`)
export const getOnlineMembers = (groupId) => request.get(`/group/${groupId}/members/online`)

// Invite
export const generateInviteCode = (groupId) => request.get(`/group/${groupId}/invite-code`)

// Events
export const getGroupEvents = (groupId) => request.get(`/group/${groupId}/events`)

// Nickname
export const setMyNickname = (groupId, nickname) => request.put(`/group/${groupId}/nickname`, { nickname })

// Join Requests
export const getJoinRequests = (groupId) => request.get(`/group/${groupId}/join-requests`)
export const approveJoinRequest = (groupId, userId) => request.post(`/group/${groupId}/approve/${userId}`)
export const rejectJoinRequest = (groupId, userId) => request.post(`/group/${groupId}/reject/${userId}`)

// Check group name availability
export const checkGroupName = (name, excludeGroupId) => {
  const params = { name }
  if (excludeGroupId) params.excludeGroupId = excludeGroupId
  return request.get('/group/checkName', { params })
}

// Demote self
export const demoteSelf = (groupId) => request.put(`/group/${groupId}/member/demote`)
