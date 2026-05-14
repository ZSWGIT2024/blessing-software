import request from '@/utils/requests'

// Admin endpoints
export const createSystemMessage = (data) => request.post('/admin/system-message/send', data)
export const getAdminSystemMessages = () => request.get('/admin/system-message/list')
export const updateSystemMessage = (messageId, data) => request.put(`/admin/system-message/${messageId}`, data)
export const deleteSystemMessage = (messageId) => request.delete(`/admin/system-message/${messageId}`)
export const deactivateSystemMessage = (messageId) => request.put(`/admin/system-message/${messageId}/deactivate`)

// User endpoint
export const getActiveSystemMessages = () => request.get('/system-message/active')
