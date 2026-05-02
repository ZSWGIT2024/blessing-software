// vipApi.js - 用户VIP相关接口
import request from '@/utils/requests'

/**
 * 获取VIP套餐列表（前端展示）
 * @returns {Promise} API响应
 */
export const getVipPlansService = () => {
  return request.get('/vip/configs/active')
}

/**
 * 获取用户当前VIP信息
 * @returns {Promise} API响应
 */
export const getCurrentVipService = () => {
  return request.get('/user/vip/current')
}

/**
 * 创建VIP订单
 * @param {number} vipType - VIP类型 (1-4)
 * @param {string} payType - 支付方式: alipay/wechat/balance
 * @returns {Promise} API响应
 */
export const createVipOrderService = (vipType, payType = 'alipay') => {
  return request.post('/user/vip/order/create', null, {
    params: { vipType, payType }
  })
}

/**
 * 查询订单状态
 * @param {string} orderNo - 订单号
 * @returns {Promise} API响应
 */
export const getOrderStatusService = (orderNo) => {
  return request.get('/user/vip/order/status', {
    params: { orderNo }
  })
}

/**
 * 取消订单
 * @param {string} orderNo - 订单号
 * @returns {Promise} API响应
 */
export const cancelOrderService = (orderNo) => {
  return request.post('/user/vip/order/cancel', null, {
    params: { orderNo }
  })
}

/**
 * 获取用户订单历史
 * @param {number} limit - 查询条数，默认10
 * @returns {Promise} API响应
 */
export const getOrderHistoryService = (limit = 10) => {
  return request.get('/user/vip/order/history', {
    params: { limit }
  })
}

/**
 * 续费VIP
 * @param {number} vipType - VIP类型
 * @param {string} payType - 支付方式
 * @returns {Promise} API响应
 */
export const renewVipService = (vipType, payType = 'alipay') => {
  return request.post('/user/vip/renew', null, {
    params: { vipType, payType }
  })
}

/**
 * 获取用户的VIP变更记录
 * @param {number} limit - 查询条数，默认10
 * @returns {Promise} API响应
 */
export const getVipRecordsService = (limit = 10) => {
  return request.get('/user/vip/records', {
    params: { limit }
  })
}


/**
 * 检查支付状态（轮询）
 * @param {string} orderNo - 订单号
 * @param {number} maxAttempts - 最大尝试次数，默认30
 * @param {number} interval - 轮询间隔(ms)，默认2000
 * @returns {Promise} 支付结果
 */
export const checkPaymentStatus = async (orderNo, maxAttempts = 30, interval = 2000) => {
  for (let i = 0; i < maxAttempts; i++) {
    try {
      const response = await getOrderStatusService(orderNo)
      if (response.code === 200) {
        const order = response.data
        if (order.status === 1) {
          return { success: true, order }
        } else if (order.status === 2 || order.status === 3) {
          return { success: false, message: '支付失败或已取消' }
        }
        // status = 0 继续等待
      }
    } catch (error) {
      console.error('检查支付状态失败:', error)
    }
    
    await new Promise(resolve => setTimeout(resolve, interval))
  }
  
  return { success: false, message: '支付超时' }
}