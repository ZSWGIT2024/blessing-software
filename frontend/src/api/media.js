import request from '@/utils/requests'

// 两阶段提交：AI作品上传到OSS后批量提交元数据写入DB
export const batchMediaService = (items) => {
  return request.post('/media/batch', { items })
}

// 批量上传接口（旧版，保留兼容）
export const uploadMediaService = (formData) => {
  return request.post('/media/batch-upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 获取媒体文件列表
export const getMediaListService = (params) => {
  return request.get('/media/list', {
    params: {
      mediaType: params.mediaType || null,
      category: params.category || null,
      status: params.status || null,
      isPublic: params.isPublic || null,
      pageNum: params.pageNum || 1,
      pageSize: params.pageSize || 20,
      keyword: params.keyword || null,
      orderBy: params.orderBy || 'upload_time',
      order: params.order || 'DESC'
    }
  })
}


/**
 * 根据用户ID和状态获取媒体文件列表
 * @param {Object} params 参数对象
 * @param {number} params.userId 用户ID（必需）
 * @param {string} params.status 状态：active/pending（必需）
 * @param {string} params.mediaType 媒体类型
 * @param {string} params.category 分类
 * @param {number} params.pageNum 页码
 * @param {number} params.pageSize 每页大小
 * @param {string} params.keyword 关键词
 * @param {string} params.orderBy 排序字段
 * @param {string} params.order 排序方式
 * @returns Promise
 */
export const getMediaByUserAndStatusService = (params) => {
  // 验证必需参数
  if (!params.userId) {
    return Promise.reject(new Error('用户ID不能为空'));
  }
  if (!params.status) {
    return Promise.reject(new Error('状态不能为空'));
  }
  if (params.status !== 'active' && params.status !== 'pending' && params.status !== 'hidden') {
    return Promise.reject(new Error('状态参数必须是 active, pending, hidden'));
  }
  
  return request.get(`/media/user/${params.userId}/mediaList`, {
    params: {
      mediaType: params.mediaType || null,
      category: params.category || null,
      status: params.status, // 状态参数
      pageNum: params.pageNum || 1,
      pageSize: params.pageSize || 20,
      keyword: params.keyword || null,
      orderBy: params.orderBy || 'upload_time',
      order: params.order || 'DESC',
      startDate: params.startDate || null,
      endDate: params.endDate || null
    }
  })
}

/**
 * 获取用户所有状态的媒体文件列表（不筛选状态）
 * @param {Object} params 参数对象
 * @param {number} params.userId 用户ID（必需）
 * @param {string} params.mediaType 媒体类型
 * @param {string} params.category 分类
 * @param {number} params.pageNum 页码
 * @param {number} params.pageSize 每页大小
 * @param {string} params.keyword 关键词
 * @param {string} params.orderBy 排序字段
 * @param {string} params.order 排序方式
 * @returns Promise
 */
export const getAllStatusMediaByUserService = (params) => {
  // 验证必需参数
  if (!params.userId) {
    return Promise.reject(new Error('用户ID不能为空'));
  }
  
  return request.get(`/media/user/${params.userId}/all-status`, {
    params: {
      mediaType: params.mediaType || null,
      category: params.category || null,
      pageNum: params.pageNum || 1,
      pageSize: params.pageSize || 20,
      keyword: params.keyword || null,
      orderBy: params.orderBy || 'upload_time',
      order: params.order || 'DESC',
      startDate: params.startDate || null,
      endDate: params.endDate || null
    }
  })
}

/**
 * 获取用户active状态的媒体文件列表（快捷方法）
 * @param {number} userId 用户ID（必须是数字）
 * @param {Object} options 其他可选参数
 * @returns Promise
 */
export const getUserActiveMediaService = (userId, options = {}) => {
  // 确保userId是数字类型
  const numericUserId = Number(userId);
  if (isNaN(numericUserId)) {
    return Promise.reject(new Error('用户ID必须是数字'));
  }

  return getMediaByUserAndStatusService({
    userId: numericUserId, // 明确传递数字类型的userId
    status: 'active',
    ...options
  })
}

/**
 * 获取用户pending状态的媒体文件列表（快捷方法）
 * @param {number} userId 用户ID（必须是数字）
 * @param {Object} options 其他可选参数
 * @returns Promise
 */
export const getUserPendingMediaService = (userId, options = {}) => {
  // 确保userId是数字类型
  const numericUserId = Number(userId);
  if (isNaN(numericUserId)) {
    return Promise.reject(new Error('用户ID必须是数字'));
  }

  return getMediaByUserAndStatusService({
    userId: numericUserId, // 明确传递数字类型的userId
    status: 'pending',
    ...options
  })
}

/**
 * 获取用户hidden状态的媒体文件列表（快捷方法）
 * @param {number} userId 用户ID（必须是数字）
 * @param {Object} options 其他可选参数
 * @returns Promise
 */
export const getUserHiddenMediaService = (userId, options = {}) => {
  // 确保userId是数字类型
  const numericUserId = Number(userId);
  if (isNaN(numericUserId)) {
    return Promise.reject(new Error('用户ID必须是数字'));
  }

  return getMediaByUserAndStatusService({
    userId: numericUserId, // 明确传递数字类型的userId
    status: 'hidden',
    ...options
  })
}

// 获取媒体详情
export const getMediaDetailService = (id, userId) => {
  return request.get(`/media/detail/${id}`, {
    params: { userId }
  })
}

// 删除媒体文件
export const deleteMediaService = (id, userId) => {
  return request.delete(`/media/${id}`, {
    params: { userId }
  })
}

// 更新媒体信息
export const updateMediaService = (id, data) => {
  return request.put(`/media/${id}`, data, {
    headers: {
      'Content-Type': 'application/json'
    }
  })
}

//更新媒体状态
export const updateMediaStatusService = (id, status) => {
   return request.put(`/media/${id}/status?status=${status}`)
}


// 批量更新媒体状态
export const batchUpdateMediaStatusService = (ids, status, tags) => {
  const config = {
    url: '/media/batch-status',
    method: 'put',
    data: ids,
    headers: {
      'Content-Type': 'application/json'
    }
  }
  
  // 构建params
  config.params = { status }
  if (tags !== undefined && tags !== null && tags !== '') {
    config.params.tags = tags
  }
  
  // console.log('请求配置:', config)
  // console.log('请求URL:', `${config.url}?${new URLSearchParams(config.params).toString()}`)
  // console.log('请求体:', config.data)
  
  return request(config)
}



// 点赞/取消点赞
export const toggleLikeService = (id, userId) => {
  return request.post(`/media/like/${id}?userId=${userId}`)
}

//最近上传
export const getRecentMediaService = (userId, limit) => {
  return request.get(`/media/user/${userId}/recent?limit=${limit}`)
}
