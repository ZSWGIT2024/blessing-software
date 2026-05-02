// emojiApi.js
import request from '@/utils/requests'

/**
 * 获取系统Emoji分类
 */
export const getSystemEmojiCategories = () => {
  return request.get('/emoji/system/categories')
}

/**
 * 获取分类下的系统Emoji
 * @param {String} category 分类ID
 */
export const getSystemEmojis = (category) => {
  return request.get(`/emoji/system/${category}`)
}

/**
 * 获取表情包列表
 */
export const getEmojiPacks = () => {
  return request.get('/emoji/packs')
}

/**
 * 获取表情包详情
 * @param {Number} packId 表情包ID
 */
export const getEmojiPackItems = (packId) => {
  return request.get(`/emoji/packs/${packId}/items`)
}


/**
 * 创建表情包
 * @param {Object} data { packName, coverUrl, description }
 */
export const createEmojiPack = (data) => {
  return request.post('/emoji/packs', data)
}

/**
 * 更新表情包
 * @param {Number} packId 表情包ID
 * @param {Object} data { packName, coverUrl, description }
 */
export const updateEmojiPack = (packId, data) => {
  return request.put(`/emoji/packs/${packId}`, data)
}

/**
 * 删除表情包
 * @param {Number} packId 表情包ID
 */
export const deleteEmojiPack = (packId) => {
  return request.delete(`/emoji/packs/${packId}`)
}

/**
 * 上传表情包封面
 * @param {File} file 封面图片文件
 */
export const uploadPackCover = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/emoji/packs/cover', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 批量上传表情包图片
 * @param {Number} packId 表情包ID
 * @param {Array} files 图片文件数组
 * @param {Array} descriptions 图片描述数组（可选）
 */
export const uploadPackItems = (packId, files, descriptions = []) => {
  const formData = new FormData()
  files.forEach(file => {
    formData.append('files', file)
  })
  if (descriptions.length > 0) {
    descriptions.forEach(desc => {
      formData.append('descriptions', desc || '')
    })
  }
  return request.post(`/emoji/packs/${packId}/items/upload`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 更新表情包图片信息
 * @param {Number} itemId 图片ID
 * @param {Object} data { description }
 */
export const updatePackItem = (itemId, data) => {
  return request.put(`/emoji/packs/items/${itemId}`, data)
}

/**
 * 删除表情包图片
 * @param {Number} itemId 图片ID
 */
export const deletePackItem = (itemId) => {
  return request.delete(`/emoji/packs/items/${itemId}`)
}

/**
 * 批量删除表情包图片
 * @param {Array} itemIds 图片ID数组
 */
export const batchDeletePackItems = (itemIds) => {
  return request.delete('/emoji/packs/items/batch', {
    params: { itemIds: itemIds.join(',') },
    paramsSerializer: params => {
      return params.itemIds
    }
  })
}

/**
 * 获取收藏列表
 * @param {Number} type 收藏类型 1:Emoji 2:表情包图片
 * @param {Number} page 页码
 * @param {Number} size 每页大小
 */
export const getFavorites = (type, page = 1, size = 20) => {
  return request.get('/emoji/favorites', {
    params: { type, page, size }
  })
}

/**
 * 添加收藏
 * @param {Object} data { type, emojiCode, packItemId }
 */
export const addFavorite = (data) => {
  return request.post('/emoji/favorites', data)
}

/**
 * 取消收藏
 * @param {Number} favoriteId 收藏记录ID
 */
export const removeFavorite = (favoriteId) => {
  return request.delete(`/emoji/favorites/${favoriteId}`)
}

/**
     * 根据emoji名称取消收藏
*/
export const removeFavoriteByEmojiName = (emojiName) => {
  return request.delete(`/emoji/favorites/emoji/${emojiName}`)
}

/**
 * 获取最近使用
 * @param {Number} limit 获取数量
 */
export const getRecentEmojis = (limit = 20) => {
  return request.get('/emoji/recent', {
    params: { limit }
  })
}

/**
 * 记录使用
 * @param {Object} data { type, emojiCode, packItemId }
 */
export const recordUsage = (data) => {
  return request.post('/emoji/usage', data)
}

/**
 * 清除最近使用
 * @param {Number} type 使用类型 1:Emoji 2:表情包图片
 */
export const clearRecent = (type) => {
  return request.delete('/emoji/recent', {
    params: { type }
  })
}