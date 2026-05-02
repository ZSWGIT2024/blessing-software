// requests.js - 修复版本
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useTokenStore } from '@/stores/token'
import router from '@/router'

const baseUrl = '/api'
const instance = axios.create({
    baseURL: baseUrl,
    timeout: 300000
})

// 请求拦截器 - 自动附加 Bearer Token
instance.interceptors.request.use(
    (config) => {
        const tokenStore = useTokenStore()
        if (tokenStore.accessToken) {
            // 如果还没有Bearer前缀，自动添加
            const token = tokenStore.accessToken.startsWith('Bearer ')
                ? tokenStore.accessToken
                : `Bearer ${tokenStore.accessToken}`
            config.headers.Authorization = token
        }
        return config
    },
    (err) => {
        return Promise.reject(err)
    }
)

// 响应拦截器 - 含Token自动刷新
let isRefreshing = false
let refreshSubscribers = []

function subscribeTokenRefresh(cb) {
  refreshSubscribers.push(cb)
}
function onTokenRefreshed(newToken) {
  refreshSubscribers.forEach(cb => cb(newToken))
  refreshSubscribers = []
}

instance.interceptors.response.use(
    result => {
        if (!result || !result.data) {
            console.error('响应数据异常:', result)
            ElMessage.error('服务器响应异常')
            return Promise.reject(new Error('服务器响应异常'))
        }

        if (result.data.code === 0) {
            return result.data
        }

        const errorMsg = result.data.msg || result.data.message || '操作失败'
        ElMessage.error(errorMsg)
        const error = new Error(errorMsg)
        error.code = result.data.code
        error.data = result.data.data
        error.fullResponse = result.data
        return Promise.reject(error)
    },
    async error => {
        const status = error.response?.status
        const originalRequest = error.config

        // 401自动刷新Token
        if (status === 401 && !originalRequest._retry && !originalRequest.url?.includes('/user/refreshToken')) {
            const tokenStore = useTokenStore()
            if (tokenStore.refreshToken) {
                originalRequest._retry = true
                if (!isRefreshing) {
                    isRefreshing = true
                    return instance.post('/user/refreshToken', null, {
                        headers: { 'X-Refresh-Token': tokenStore.refreshToken }
                    }).then(res => {
                        if (res.code === 0 && res.data?.accessToken) {
                            tokenStore.setToken(res.data.accessToken)
                            onTokenRefreshed(res.data.accessToken)
                            originalRequest.headers.Authorization = 'Bearer ' + res.data.accessToken
                            return instance(originalRequest)
                        }
                        throw new Error('refresh failed')
                    }).catch(() => {
                        tokenStore.removeToken()
                        isRefreshing = false
                        refreshSubscribers = []
                        router.push('/login')
                        return Promise.reject(error)
                    }).finally(() => { isRefreshing = false })
                } else {
                    return new Promise(resolve => {
                        subscribeTokenRefresh(newToken => {
                            originalRequest.headers.Authorization = 'Bearer ' + newToken
                            resolve(instance(originalRequest))
                        })
                    })
                }
            }
            ElMessage.error('请先登录!')
            tokenStore.removeToken()
            router.push('/login')
        } else if (status === 401) {
            ElMessage.error('请先登录!')
            const tokenStore = useTokenStore()
            tokenStore.removeToken()
            router.push('/login')
        } else if (error.code === 'ECONNABORTED') {
            ElMessage.error('请求超时，请检查网络连接')
        } else if (error.message === 'Network Error') {
            ElMessage.error('网络连接失败，请检查网络')
        }

        error.isHttpError = !!status
        error.status = status
        return Promise.reject(error)
    }
)

export default instance