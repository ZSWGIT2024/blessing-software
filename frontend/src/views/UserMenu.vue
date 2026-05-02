<template>
     <div 
    
    v-if="user"
    class="user-menu"
    :style="menuStyle"
    @click.stop
  >
      <div class="menu-header">
        <img :src="user.avatar" class="user-avatar">
        <span class="user-name">{{ user.name }}</span>
      </div>
      <div class="menu-item" @click="viewProfile">
        <span class="icon">👤</span> 查看资料
      </div>
      <div class="menu-item" @click="addFriend">
        <span class="icon">➕</span> 添加好友
      </div>
      <div class="menu-item" @click="blockUser">
        <span class="icon">🚫</span> 屏蔽玩家
      </div>
      <div class="menu-item" @click="sendPrivateMessage">
        <span class="icon">💬</span> 发送私信
      </div>
    </div>
  </template>
  
  <script>
  export default {
    name: 'UserMenu',
  props: {
    user: {
      type: Object,
      required: true,
      validator: (user) => user && typeof user.id !== 'undefined' && user.name
    },
    position: {
      type: Object,
      required: true,
      default: () => ({ x: 0, y: 0 })
    }
  },
  emits: ['close', 'view-profile', 'add-friend', 'block-user', 'private-message'],
  computed: {
    menuStyle() {
      return {
        left: `${this.position.x}px`,
        top: `${this.position.y}px`,
        position: 'fixed',
        'z-index': 1000
      }
    }
  },
  mounted() {
    // 确保菜单在可视区域内
    const menuEl = this.
$el
    if (menuEl) {
      const rect = menuEl.getBoundingClientRect()
      if (rect.right > window.innerWidth) {
        menuEl
.style.left = `${window.innerWidth - rect.width - 5}px`
      }
      if (rect.bottom > window.innerHeight) {
        menuEl
.style.top = `${window.innerHeight - rect.height - 5}px`
      }
    }
  },
  // 明确声明 emits
  
  methods: {
    
close() {
      this.$emit('close')
    },
    viewProfile() {
      this.$emit('view-profile', this.user)
      this.close()
    },
    addFriend() {
      this.$emit('add-friend', this.user)
      this.close()
    },
    blockUser() {
      this.$emit('block-user', this.user)
      this.close()
    },
    sendPrivateMessage() {
      this.$emit('private-message', this.user)
      this.close()
    }
  }
  }
  </script>
  
  <style scoped>
  .user-menu {
    position: fixed;
    background: white;
    border-radius: 8px;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
    z-index: 1001;
    min-width: 180px;
    overflow: hidden;
  }
  
  .menu-header {
    padding: 10px;
    display: flex;
    align-items: center;
    border-bottom: 1px solid #eee;
    background: linear-gradient(135deg, #ff9a9e 0%, #fad0c4 100%);
    color: white;
  }
  
  .user-avatar {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    margin-right: 10px;
  }
  
  .user-name {
    font-weight: bold;
  }
  
  .menu-item {
    padding: 10px 15px;
    cursor: pointer;
    display: flex;
    align-items: center;
  }
  
  .menu-item:hover {
    background: #f5f5f5;
  }
  
  .menu-item .icon {
    margin-right: 8px;
    font-size: 18px;
  }
  </style>