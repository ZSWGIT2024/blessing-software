import { defineStore } from 'pinia'
import { ref } from 'vue'
// import { api } from '@/api' // 假设有一个api模块

export const useRoomStore = defineStore('room', () => {
  const rooms = ref([])
  const currentRoom = ref(null)
  
  const createRoom = async (roomData) => {
    try {
      const response = await api.post('/rooms', roomData)
      const newRoom = response.data
      rooms.value.push(newRoom)
      currentRoom.value = newRoom
      return newRoom
    } catch (error) {
      throw error
    }
  }
  
  const fetchRooms = async () => {
    try {
      const response = await api.get('/rooms')
      rooms.value = response.data
    } catch (error) {
      console.error('获取房间列表失败:', error)
    }
  }
  
  return {
    rooms,
    currentRoom,
    createRoom,
    fetchRooms
  }
})