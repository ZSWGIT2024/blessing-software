import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useTokenStore = defineStore('token', () => {
    // 双Token状态
    const accessToken = ref(localStorage.getItem('accessToken') || '')
    const refreshToken = ref(localStorage.getItem('refreshToken') || '')
    const tokenType = ref(localStorage.getItem('tokenType') || 'Bearer')

    // 设置双Token
    const setTokens = (data) => {
        if (data.accessToken) {
            accessToken.value = data.accessToken
            localStorage.setItem('accessToken', data.accessToken)
        }
        if (data.refreshToken) {
            refreshToken.value = data.refreshToken
            localStorage.setItem('refreshToken', data.refreshToken)
        }
        if (data.tokenType) {
            tokenType.value = data.tokenType
            localStorage.setItem('tokenType', data.tokenType)
        }
    }

    // 兼容旧版：只更新accessToken
    const setToken = (newToken) => {
        accessToken.value = newToken
        localStorage.setItem('accessToken', newToken)
    }

    // 清除所有Token
    const removeToken = () => {
        accessToken.value = ''
        refreshToken.value = ''
        localStorage.removeItem('accessToken')
        localStorage.removeItem('refreshToken')
        localStorage.removeItem('tokenType')
    }

    return {
        accessToken,
        refreshToken,
        tokenType,
        setTokens,
        setToken,
        removeToken
    }
}, { persist: true })
