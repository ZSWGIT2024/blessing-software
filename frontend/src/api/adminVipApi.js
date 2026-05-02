// adminVipApi.js - 管理员VIP相关接口
import request from '@/utils/requests'

/**
 * 获取所有VIP配置（管理后台用）
 * @returns {Promise} API响应
 */
export const getAllVipConfigsService = () => {
  return request.get('/vip/configs/all')
}

/**
 * 根据VIP类型获取配置
 * @param {number} vipType - VIP类型
 * @returns {Promise} API响应
 */
export const getVipConfigByTypeService = (vipType) => {
  return request.get(`/vip/configs/${vipType}`)
}

/**
 * 添加VIP配置
 * @param {Object} config - VIP配置数据
 * @returns {Promise} API响应
 */
export const addVipConfigService = (config) => {
  return request.post('/vip/configs', config)
}

/**
 * 更新VIP配置
 * @param {Object} config - VIP配置数据
 * @returns {Promise} API响应
 */
export const updateVipConfigService = (config) => {
  return request.put('/vip/configs', config)
}

/**
 * 删除VIP配置
 * @param {number} id - 配置ID
 * @returns {Promise} API响应
 */
export const deleteVipConfigService = (id) => {
  return request.delete(`/vip/configs/${id}`)
}

/**
 * 启用/禁用VIP配置
 * @param {number} id - 配置ID
 * @param {boolean} isActive - 是否启用
 * @returns {Promise} API响应
 */
export const toggleVipConfigStatusService = (id, isActive) => {
  return request.patch(`/vip/configs/${id}/status`, null, {
    params: { isActive }
  })
}

/**
 * 分页查询VIP订单（管理员）
 */
export const getVipOrdersAdminService = (params) => {
  return request.get('/user/vip/orders', { params })
}

/**
 * 导出VIP订单数据（管理员）
 */
export const exportVipOrdersService = (params) => {
  return request.get('/user/vip/orders/export', {
    params,
    responseType: 'blob'
  })
}
