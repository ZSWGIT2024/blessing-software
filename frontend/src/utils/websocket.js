// src/utils/websocket.js
import { ElMessage } from 'element-plus'

class WebSocketService {
    constructor() {
        this.ws = null
        this.reconnectAttempts = 0
        this.maxReconnectAttempts = 10
        this.reconnectInterval = 3000 // 3秒
        this.heartbeatInterval = 30000 // 30秒
        this.heartbeatTimer = null
        this.reconnectTimer = null
        this.messageHandlers = new Map()
        this.statusHandlers = new Set()
        this.typingHandlers = new Set()
        this.userId = null
        this.token = null
        this.isConnecting = false
    }

    // 初始化连接
    connect(userId, token) {
        if (this.ws && (this.ws.readyState === WebSocket.OPEN || this.ws.readyState === WebSocket.CONNECTING)) {
            console.log('WebSocket 已连接或正在连接中')
            return
        }

        this.userId = userId
        this.token = token
        this.isConnecting = true

        // 构建 WebSocket URL - 根据您的后端地址修改
        const wsUrl = `ws://localhost:8080/ws/chat?userId=${userId}&token=${token}`
        
        try {
            this.ws = new WebSocket(wsUrl)
            this.initEventHandlers()
        } catch (error) {
            console.error('WebSocket 连接失败:', error)
            this.handleReconnect()
        }
    }

    // 初始化事件处理器
    initEventHandlers() {
        this.ws.onopen = () => {
            console.log('WebSocket 连接成功')
            this.reconnectAttempts = 0
            this.isConnecting = false
            this.startHeartbeat()
            this.notifyStatusChange('connected')
        }

        this.ws.onmessage = (event) => {
            try {
                const data = JSON.parse(event.data)
                this.handleMessage(data)
            } catch (error) {
                console.error('解析消息失败:', error)
            }
        }

        this.ws.onclose = (event) => {
            console.log('WebSocket 连接关闭:', event.code, event.reason)
            this.stopHeartbeat()
            this.notifyStatusChange('disconnected')
            
            // 非正常关闭时尝试重连
            if (event.code !== 1000) {
                this.handleReconnect()
            }
        }

        this.ws.onerror = (error) => {
            console.error('WebSocket 错误:', error)
            this.notifyStatusChange('error')
        }
    }

    // 处理接收到的消息
    handleMessage(data) {
        const { type, content, senderId, receiverId, timestamp, messageId } = data

        switch (type) {
            case 'message':
                // 新消息
                this.notifyMessageHandlers('newMessage', {
                    messageId,
                    senderId,
                    receiverId,
                    content,
                    createTime: timestamp,
                    status: 'sent'
                })
                break

            case 'typing':
                // 输入状态
                this.notifyTypingHandlers(senderId, content.isTyping)
                break

            case 'read':
                // 消息已读
                this.notifyMessageHandlers('messageRead', {
                    senderId,
                    receiverId,
                    messageId: content.messageId
                })
                break

            case 'online':
                // 用户在线状态
                this.notifyMessageHandlers('userOnline', {
                    userId: senderId,
                    online: true
                })
                break

            case 'offline':
                // 用户离线状态
                this.notifyMessageHandlers('userOnline', {
                    userId: senderId,
                    online: false
                })
                break

            case 'pong':
                // 心跳响应
                console.log('收到心跳响应')
                break

            default:
                console.log('未知消息类型:', type, data)
        }
    }

    // 发送消息
    sendMessage(messageData) {
        if (!this.isConnected()) {
            console.error('WebSocket 未连接')
            return false
        }

        try {
            const data = {
                type: 'message',
                ...messageData,
                timestamp: new Date().toISOString()
            }
            this.ws.send(JSON.stringify(data))
            return true
        } catch (error) {
            console.error('发送消息失败:', error)
            return false
        }
    }

    // 发送输入状态
    sendTypingStatus(receiverId, isTyping) {
        if (!this.isConnected()) return

        try {
            const data = {
                type: 'typing',
                receiverId,
                content: { isTyping },
                timestamp: new Date().toISOString()
            }
            this.ws.send(JSON.stringify(data))
        } catch (error) {
            console.error('发送输入状态失败:', error)
        }
    }

    // 标记消息已读
    sendReadReceipt(senderId, messageId) {
        if (!this.isConnected()) return

        try {
            const data = {
                type: 'read',
                receiverId: senderId,
                content: { messageId },
                timestamp: new Date().toISOString()
            }
            this.ws.send(JSON.stringify(data))
        } catch (error) {
            console.error('发送已读回执失败:', error)
        }
    }

    // 开始心跳
    startHeartbeat() {
        this.stopHeartbeat()
        this.heartbeatTimer = setInterval(() => {
            if (this.isConnected()) {
                this.ws.send(JSON.stringify({ type: 'ping' }))
            }
        }, this.heartbeatInterval)
    }

    // 停止心跳
    stopHeartbeat() {
        if (this.heartbeatTimer) {
            clearInterval(this.heartbeatTimer)
            this.heartbeatTimer = null
        }
    }

    // 处理重连
    handleReconnect() {
        if (this.reconnectAttempts >= this.maxReconnectAttempts) {
            console.error('WebSocket 重连次数已达上限')
            ElMessage.error('连接失败，请刷新页面重试')
            return
        }

        if (this.reconnectTimer) {
            clearTimeout(this.reconnectTimer)
        }

        this.reconnectAttempts++
        console.log(`尝试重连 (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`)

        this.reconnectTimer = setTimeout(() => {
            if (this.userId && this.token) {
                this.connect(this.userId, this.token)
            }
        }, this.reconnectInterval * this.reconnectAttempts)
    }

    // 检查连接状态
    isConnected() {
        return this.ws && this.ws.readyState === WebSocket.OPEN
    }

    // 注册消息处理器
    on(event, handler) {
        if (!this.messageHandlers.has(event)) {
            this.messageHandlers.set(event, new Set())
        }
        this.messageHandlers.get(event).add(handler)
    }

    // 移除消息处理器
    off(event, handler) {
        if (this.messageHandlers.has(event)) {
            this.messageHandlers.get(event).delete(handler)
        }
    }

    // 通知消息处理器
    notifyMessageHandlers(event, data) {
        if (this.messageHandlers.has(event)) {
            this.messageHandlers.get(event).forEach(handler => {
                try {
                    handler(data)
                } catch (error) {
                    console.error(`执行消息处理器失败 (${event}):`, error)
                }
            })
        }
    }

    // 注册输入状态处理器
    onTyping(handler) {
        this.typingHandlers.add(handler)
    }

    // 移除输入状态处理器
    offTyping(handler) {
        this.typingHandlers.delete(handler)
    }

    // 通知输入状态处理器
    notifyTypingHandlers(userId, isTyping) {
        this.typingHandlers.forEach(handler => {
            try {
                handler(userId, isTyping)
            } catch (error) {
                console.error('执行输入状态处理器失败:', error)
            }
        })
    }

    // 注册连接状态处理器
    onStatusChange(handler) {
        this.statusHandlers.add(handler)
    }

    // 移除连接状态处理器
    offStatusChange(handler) {
        this.statusHandlers.delete(handler)
    }

    // 通知连接状态处理器
    notifyStatusChange(status) {
        this.statusHandlers.forEach(handler => {
            try {
                handler(status)
            } catch (error) {
                console.error('执行状态处理器失败:', error)
            }
        })
    }

    // 手动断开连接
    disconnect() {
        this.stopHeartbeat()
        if (this.reconnectTimer) {
            clearTimeout(this.reconnectTimer)
            this.reconnectTimer = null
        }
        
        if (this.ws) {
            this.ws.close(1000, '正常关闭')
            this.ws = null
        }
        
        this.reconnectAttempts = 0
        this.isConnecting = false
        this.notifyStatusChange('disconnected')
    }
}

// 创建单例
const websocketService = new WebSocketService()
export default websocketService