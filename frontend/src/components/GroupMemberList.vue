<template>
  <div class="member-list">
    <div class="member-search">
      <input v-model="search" placeholder="搜索成员...">
    </div>
    <div v-for="member in filteredMembers" :key="member.userId" class="member-item" @contextmenu.prevent="onContextMenu(member, $event)">
      <div class="member-avatar">
        <img :src="member.avatar || '/default-avatar.png'" :alt="member.username">
        <span :class="['online-dot', member.isOnline ? 'online' : 'offline']"></span>
      </div>
      <div class="member-info">
        <div class="member-name">
          {{ member.nicknameInGroup || member.username }}
          <span v-if="member.role === 'owner'" class="badge badge-owner">群主</span>
          <span v-else-if="member.role === 'admin'" class="badge badge-admin">群管理员</span>
        </div>
        <div class="member-status" v-if="member.isMuted">🔇 已禁言</div>
      </div>
    </div>

    <div v-if="showContextMenu" class="context-menu" :style="menuStyle" @click.stop>
      <div class="menu-item" @click="handleMenuAction('viewProfile', contextTarget)">
        <span>查看资料</span>
      </div>
      <div v-if="canManage && contextTarget?.role !== 'owner' && contextTarget?.role !== 'admin'" class="menu-item" @click="handleMenuAction('muteOrUnmute', contextTarget)">
        <span>{{ contextTarget?.isMuted ? '取消禁言' : '禁言'}}</span>
      </div>
      <div v-if="canManage && contextTarget?.role !== 'owner' && contextTarget?.role !== 'admin'" class="menu-item" @click="handleMenuAction('kick', contextTarget)">
        <span>移出群聊</span>
      </div>
      <div v-if="isOwner && contextTarget?.role !== 'owner'" class="menu-item" @click="handleMenuAction('changeMemberRole', contextTarget)">
        <span>{{ contextTarget?.role === 'admin' ? '取消管理员' : '设为管理员' }}</span>
      </div>
      <div v-if="isOwner && contextTarget?.role !== 'owner'" class="menu-item" @click="handleMenuAction('transferOwner', contextTarget)">
        <span>转让群主</span>
      </div>
      <div v-if="isOwner && contextTarget?.role !== 'owner' && contextTarget?.role === 'admin'" class="menu-item" @click="handleMenuAction('demoteSelf', contextTarget)">
        <span>降级为普通成员</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  members: { type: Array, default: () => [] },
  myRole: { type: String, default: 'member' }
})
const emit = defineEmits(['viewProfile', 'muteOrUnmute', 'kick', 'changeMemberRole', 'transferOwner', 'demoteSelf'])

const search = ref('')
const showContextMenu = ref(false)
const contextTarget = ref(null)
const menuStyle = ref({})

const isOwner = computed(() => props.myRole === 'owner')
const canManage = computed(() => props.myRole === 'owner' || props.myRole === 'admin')

const filteredMembers = computed(() => {
  if (!search.value.trim()) return props.members
  const kw = search.value.toLowerCase()
  return props.members.filter(m =>
    (m.username || '').toLowerCase().includes(kw) ||
    (m.nickname || '').toLowerCase().includes(kw) ||
    (m.nicknameInGroup || '').toLowerCase().includes(kw)
  )
})

const onContextMenu = (member, event) => {
  contextTarget.value = member
  menuStyle.value = { left: event.clientX + 'px', top: event.clientY + 'px' }
  showContextMenu.value = true
}

const handleMenuAction = (action, member) => {
  showContextMenu.value = false
  emit(action, member)
}

const handleClickOutside = () => {
  showContextMenu.value = false
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})

defineExpose({ showContextMenu })
</script>

<style scoped>
.member-list { max-height: 100%; overflow-y: auto; }
.member-search { padding: 8px; }
.member-search input {
  width: 100%; padding: 6px 10px; border: 1px solid #ddd; border-radius: 6px;
  font-size: 13px; box-sizing: border-box;
}
.member-item {
  display: flex; align-items: center; padding: 8px 12px; cursor: pointer;
  transition: background 0.2s;
}
.member-item:hover { background: rgba(250, 148, 195, 0.1); }
.member-avatar { position: relative; margin-right: 10px; }
.member-avatar img { width: 36px; height: 36px; border-radius: 50%; }
.online-dot { position: absolute; bottom: 0; right: 0; width: 8px; height: 8px; border-radius: 50%; border: 2px solid white; }
.online-dot.online { background: #4caf50; }
.online-dot.offline { background: #bbb; }
.member-info { flex: 1; }
.member-name { font-size: 14px; font-weight: 500; }
.member-status { font-size: 12px; color: #999; }
.badge { font-size: 10px; padding: 1px 4px; border-radius: 3px; margin-left: 4px; color: white; }
.badge-owner { background: #ff9800; }
.badge-admin { background: #2196f3; }
.context-menu {
  position: fixed; background: white; border: 1px solid #ddd; border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1); z-index: 3000; min-width: 120px;
  padding: 4px 0; margin-left: -120px; margin-top: -40px;
}
.menu-item { padding: 8px 16px; cursor: pointer; font-size: 13px; }
.menu-item:hover { background: #f5f5f5; }
</style>
