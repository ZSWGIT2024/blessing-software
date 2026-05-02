//导入request.js文件
import request from '@/utils/requests'

// 批量上传接口
export const uploadSubmitMediaService = (formData) => {
  return request.post('/submit/batch-upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 获取媒体文件列表
export const getSubmitListService = (params) => {
  return request.get('/submit/submitList', {
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
export const getSubmitByUserAndStatusService = (params) => {
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
  
  return request.get(`/submit/user/${params.userId}/submitList`, {
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
export const getAllStatusSubmitByUserService = (params) => {
  // 验证必需参数
  if (!params.userId) {
    return Promise.reject(new Error('用户ID不能为空'));
  }
  
  return request.get(`/submit/user/${params.userId}/all-status`, {
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
export const getUserActiveSubmitService = (userId, options = {}) => {
  // 确保userId是数字类型
  const numericUserId = Number(userId);
  if (isNaN(numericUserId)) {
    return Promise.reject(new Error('用户ID必须是数字'));
  }

  return getSubmitByUserAndStatusService({
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
export const getUserPendingSubmitService = (userId, options = {}) => {
  // 确保userId是数字类型
  const numericUserId = Number(userId);
  if (isNaN(numericUserId)) {
    return Promise.reject(new Error('用户ID必须是数字'));
  }

  return getSubmitByUserAndStatusService({
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
export const getUserHiddenSubmitService = (userId, options = {}) => {
  // 确保userId是数字类型
  const numericUserId = Number(userId);
  if (isNaN(numericUserId)) {
    return Promise.reject(new Error('用户ID必须是数字'));
  }

  return getSubmitByUserAndStatusService({
    userId: numericUserId, // 明确传递数字类型的userId
    status: 'hidden',
    ...options
  })
}


// 删除媒体文件
export const deleteSubmitService = (id, userId) => {
  return request.delete(`/submit/${id}`, {
    params: { userId }
  })
}

// 更新媒体信息
export const updateSubmitService = (id, data) => {
  return request.put(`/submit/${id}`, data, {
    headers: {
      'Content-Type': 'application/json'
    }
  })
}

//更新媒体状态
export const updateSubmitStatusService = (id, status) => {
   return request.put(`/submit/${id}/status?status=${status}`)
}


// 批量更新媒体状态
export const batchUpdateSubmitStatusService = (ids, status, tags) => {
  const config = {
    url: '/submit/batch-status',
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
  
  console.log('请求配置:', config)
  console.log('请求URL:', `${config.url}?${new URLSearchParams(config.params).toString()}`)
  console.log('请求体:', config.data)
  
  return request(config)
}



// 点赞/取消点赞
export const toggleLikeSubmitService = (id, userId) => {
  return request.post(`/submit/like/${id}?userId=${userId}`)
}


// 最近上传
export const getRecentSubmitService = (userId, limit) => {
  return request.get(`/submit/user/${userId}/recent?limit=${limit}`)
}
