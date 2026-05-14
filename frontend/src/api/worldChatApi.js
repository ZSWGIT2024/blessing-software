import request from '@/utils/requests'

export const sendWorldMessage = (data) => request.post('/world/message/send', data)
export const getWorldMessages = (params) => request.get('/world/messages', { params })
export const withdrawWorldMessage = (data) => request.post('/world/message/withdraw', data)
export const deleteWorldMessage = (messageId) => request.delete(`/world/message/${messageId}`)

export const uploadGroupChatFile = (formData) => request.post('/world/file/upload', formData, {
  headers: { 'Content-Type': 'multipart/form-data' }
})
export const getWorldFiles = (params) => request.get('/world/files', { params })
export const deleteWorldFile = (fileId) => request.delete(`/world/file/${fileId}`)
