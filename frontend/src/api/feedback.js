import request from '@/utils/requests'

// ==================== User ====================

export const submitFeedback = (data) => request.post('/feedback', data)

export const getMyFeedbacks = () => request.get('/feedback/my')

export const updateFeedback = (id, data) => request.put(`/feedback/${id}`, data)

export const deleteFeedback = (id) => request.delete(`/feedback/${id}`)

// ==================== Admin ====================

export const getFeedbackList = (params) => request.get('/feedback/admin/list', {
  params: { page: params.page || 1, size: params.size || 20, status: params.status }
})

export const handleFeedback = (id, status, adminReply) =>
  request.put(`/feedback/admin/${id}/handle`, { status, adminReply })

export const adminDeleteFeedback = (id) => request.delete(`/feedback/admin/${id}`)

export const getFeedbackStats = () => request.get('/feedback/admin/stats')
