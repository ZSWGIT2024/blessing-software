// sensitiveApi.js
import request from '@/utils/request'

// 检查敏感词
export const checkSensitiveWords = (text) => {
  return request.post('/sensitive/check', { text })
}

// 过滤文本
export const filterText = (text, strategy = 'replace') => {
  return request.post('/sensitive/filter', { text, strategy })
}

// 添加敏感词
export const addSensitiveWord = (word) => {
  return request.post('/sensitive/add', { word })
}

// 批量添加敏感词
export const batchAddSensitiveWords = (words) => {
  return request.post('/sensitive/batch-add', { words })
}

// 删除敏感词
export const removeSensitiveWord = (word) => {
  return request.delete(`/sensitive/remove/${encodeURIComponent(word)}`)
}

// 重新加载词库
export const reloadSensitiveWords = () => {
  return request.post('/sensitive/reload')
}

// 获取统计信息
export const getStatistics = () => {
  return request.get('/sensitive/statistics')
}

// 测试过滤
export const testFilter = (params) => {
  return request.post('/sensitive/test', params)
}