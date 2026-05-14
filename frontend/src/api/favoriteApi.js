import request from '@/utils/requests'

// Folders
export const getFolders = () => request.get('/favorite/folders')
export const createFolder = (data) => request.post('/favorite/folder', data)
export const updateFolder = (id, data) => request.put(`/favorite/folder/${id}`, data)
export const deleteFolder = (id) => request.delete(`/favorite/folder/${id}`)

// Favorites
export const toggleFavorite = (data) => request.post('/favorite/toggle', data)
export const checkFavorite = (mediaId) => request.get('/favorite/check', { params: { mediaId } })
export const getFavorites = (params) => request.get('/favorite/list', { params })
export const getFavoriteMediaIds = () => request.get('/favorite/media-ids')
export const moveFavorite = (id, data) => request.put(`/favorite/${id}/move`, data)
export const removeFromFolder = (id) => request.put(`/favorite/${id}/remove-folder`)
export const deleteFavorite = (id) => request.delete(`/favorite/${id}`)
export const batchMoveFavorites = (data) => request.post('/favorite/batch-move', data)

// Media favorite toggle (via submit controller)
export const toggleMediaFavorite = (id, userId) => request.post(`/submit/favorite/${id}`, { userId })
