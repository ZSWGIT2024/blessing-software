// adminUser.js - 管理员用户管理相关接口（对应 UserAdminController）
import request from '@/utils/requests'

// ==================== 用户管理 ====================

// 获取用户列表（分页+筛选）
export const getUserListService = (params) => {
  return request.get('/admin/user/list', {
    params: {
      page: params.page || 1,
      size: params.size || 20,
      keyword: params.keyword || '',
      vipType: params.vipType,
      status: params.status,
      minLevel: params.minLevel,
      maxLevel: params.maxLevel
    }
  })
}

// 获取用户详情（管理员视角）
export const getUserDetailService = (id) => {
  return request.get(`/admin/user/detail/${id}`)
}

// 更新用户状态（封禁/解封）
export const updateUserStatusService = (id, status) => {
  return request.patch(`/admin/user/status/${id}`, null, {
    params: { status }
  })
}

// 更新用户VIP信息
export const updateUserVipService = (id, vipType, days = null) => {
  const params = { vipType }
  if (days !== null) params.days = days
  return request.post(`/admin/user/vip/${id}`, null, { params })
}

// 调整用户积分
export const updateUserCoinsService = (id, amount, reason) => {
  return request.post(`/admin/user/coins/${id}`, null, {
    params: { amount, reason }
  })
}

// 修改用户等级
export const updateUserLevelService = (id, level, exp = 0) => {
  return request.post(`/admin/user/level/${id}`, null, {
    params: { level, exp }
  })
}

// 解锁账户
export const unlockUserService = (id) => {
  return request.post(`/admin/user/unlock/${id}`)
}

// 搜索用户
export const searchUsersService = (keyword) => {
  return request.get('/admin/user/search', {
    params: { keyword }
  })
}

/**
 * 获取用户统计信息
 * @returns {Promise} API响应
 */
export const getUserStatsService = () => {
  return request.get('/admin/user/stats')
}

/**
 * 获取实时统计信息
 * @returns {Promise} API响应
 */
export const getRealTimeStatisticsService = () => {
  return request.get('/admin/statistics/realtime')
}

/**
 * 获取用户增长趋势
 * @param {number} days - 天数，默认30
 * @returns {Promise} API响应
 */
export const getUserGrowthTrendService = (days = 30) => {
  return request.get('/admin/statistics/growth-trend', {
    params: { days }
  })
}

/**
 * 获取活跃用户趋势
 * @param {number} days - 天数，默认30
 * @returns {Promise} API响应
 */
export const getActiveUserTrendService = (days = 30) => {
  return request.get('/admin/statistics/active-trend', {
    params: { days }
  })
}

/**
 * 获取指定日期统计
 * @param {string} date - 日期，格式：YYYY-MM-DD
 * @returns {Promise} API响应
 */
export const getStatisticsByDateService = (date) => {
  return request.get('/admin/statistics/by-date', {
    params: { date }
  })
}

// 获取系统信息（JVM/服务器）
export const adminGetSystemInfoService = () => {
  return request.get('/admin/system/info')
}
