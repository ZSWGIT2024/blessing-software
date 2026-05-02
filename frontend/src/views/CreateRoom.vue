<template>
  <div class="create-room">
    <h2>创建新房间</h2>
    <form @submit.prevent="createRoom">
      <div class="form-group">
        <label for="roomName">房间名称</label>
        <input id="roomName" v-model="roomName" type="text" required placeholder="输入房间名称" />
      </div>

      <div class="form-group">
        <label for="roomType">房间类型</label>
        <select id="roomType" v-model="roomType">
          <option value="public">公开</option>
          <option value="private">私密</option>
        </select>
      </div>

      <div class="form-group" v-if="roomType === 'private'">
        <label for="roomPassword">房间密码</label>
        <input id="roomPassword" v-model="roomPassword" type="password" placeholder="设置房间密码" />
      </div>

      <div class="form-group">
        <label for="maxUsers">最大人数</label>
        <input id="maxUsers" v-model.number="maxUsers" type="number" min="2" max="20" placeholder="2-20" />
      </div>

      <button type="submit" class="create-button">创建房间</button>
    </form>
  </div>
</template>

<script>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useRoomStore } from '@/stores/room'

export default {
  setup() {
    const roomStore = useRoomStore()
    const router = useRouter()

    const roomName = ref('')
    const roomType = ref('public')
    const roomPassword = ref('')
    const maxUsers = ref(10)

    const createRoom = async () => {
      try {
        const room = await roomStore.createRoom({
          name: roomName.value,
          type: roomType.value,
          password: roomPassword.value,
          maxUsers: maxUsers.value
        })

        router.push(`/room/${room.id}`)
      } catch (error) {
        console.error('创建房间失败:', error)
        // 显示错误提示
      }
    }

    return {
      roomName,
      roomType,
      roomPassword,
      maxUsers,
      createRoom
    }
  }
}
</script>

<style scoped>
.create-room {
  max-width: 500px;
  margin: 0 auto;
  padding: 20px;
  background: white;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

h2 {
  text-align: center;
  margin-bottom: 20px;
  color: #ff85a2;
}

.form-group {
  margin-bottom: 15px;
}

label {
  display: block;
  margin-bottom: 5px;
  font-weight: bold;
}

input,
select {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 16px;
}

.create-button {
  width: 100%;
  padding: 12px;
  background-color: #ff85a2;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.create-button:hover {
  background-color: #ff6b8b;
}
</style>