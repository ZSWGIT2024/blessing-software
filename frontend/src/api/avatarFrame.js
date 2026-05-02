import request from '@/utils/requests'

/**
 * 头像框相关API
 */

// 获取用户当前使用的头像框
export const getCurrentFrame = () => {
  return request.get('/avatar-frames/current')
}

 /**
   根据用户ID获取其他用户当前使用的头像框
  */
export const getOtherUserFrame = (userId) => {
  return request.get(`/avatar-frames/other/${userId}`)
}

// 获取所有头像框（带解锁状态）
export const getAllFramesWithStatus = () => {
  return request.get('/avatar-frames/all-with-status', {
    params: {
      _t: new Date().getTime()
    }
  })
}


// 获取用户可用的头像框列表
export const getAvailableFrames = () => {
  return request.get('/avatar-frames/available', {
    params: {
      // 可以添加缓存控制参数（根据需求可选）
      _t: new Date().getTime() // 防止缓存
    }
  })
}

// 更换用户头像框
export const changeAvatarFrame = (frameId) => {
  return request.post(`/avatar-frames/change/${frameId}`)
}

// 解锁新头像框（系统自动调用）
export const unlockAvatarFrame = (frameId) => {
  return request.post(`/avatar-frames/unlock/${frameId}`)
}
