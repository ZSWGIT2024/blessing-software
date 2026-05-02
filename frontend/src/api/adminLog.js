import request from '@/utils/requests'

// ==================== 仪表盘 ====================

// 获取系统仪表盘数据
export const adminGetDashboardService = () =>
  request.get('/admin/dashboard')

// ==================== 操作日志 ====================

// 分页查询操作日志
export const adminGetOperationLogsService = (params) =>
  request.get('/admin/logs/operation', { params })

// 查看操作日志详情
export const adminGetOperationLogDetailService = (id) =>
  request.get(`/admin/logs/operation/${id}`)

// 清理旧日志
export const adminCleanOperationLogsService = (beforeDate) =>
  request.delete('/admin/logs/operation/clean', { params: { beforeDate } })

// ==================== 登录记录 ====================

// 分页查询登录记录
export const adminGetLoginRecordsService = (params) =>
  request.get('/admin/logs/login', { params })

// 查看用户登录记录
export const adminGetUserLoginRecordsService = (userId, limit = 20) =>
  request.get(`/admin/logs/login/user/${userId}`, { params: { limit } })

// 检测异常登录
export const adminGetAbnormalLoginsService = (userId) =>
  request.get(`/admin/logs/login/abnormal/${userId}`)
