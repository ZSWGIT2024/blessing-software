<template>
  <div class="fav-manager-overlay" @click.self="$emit('close')">
    <div class="fav-manager">
      <div class="fav-header">
        <h3>{{ selectMode ? '选择背景图片' : '收藏管理' }}</h3>
        <button class="close-btn" @click="$emit('close')">&times;</button>
      </div>

      <div class="fav-body">
        <!-- Left: Folders sidebar -->
        <div class="fav-sidebar">
          <div class="folder-list">
            <div class="folder-item" :class="{ active: currentFolderId === null && !showAllFavs }"
              @click="showAllFavs = false; currentFolderId = null; loadFavorites()">
              <span>📁 我的收藏</span>
              <span class="count">{{ unfolderCount }}</span>
            </div>
            <div class="folder-item" :class="{ active: showAllFavs }"
              @click="showAllFavs = true; currentFolderId = null">
              <span>📂 我的收藏夹</span>
            </div>
            <div v-if="showAllFavs" class="sub-folders">
              <div v-for="f in folders" :key="f.id" class="folder-item sub"
                :class="{ active: currentFolderId === f.id && !showAllFavs }"
                @click="showAllFavs = false; currentFolderId = f.id; loadFavorites()">
                <span>{{ f.folderName }}</span>
                <span class="count">{{ f.favoriteCount || 0 }}</span>
              </div>
            </div>
          </div>
          <button class="new-folder-btn" @click="showCreateFolder = true">+ 新建收藏夹</button>
        </div>

        <!-- Right: Favorites grid -->
        <div class="fav-content">
          <!-- Select mode hint -->
          <div v-if="selectMode" class="select-hint">点击图片设置为背景</div>

          <div v-if="loading" class="loading-text">加载中...</div>

          <div v-else-if="favorites.length === 0" class="empty-text">
            {{ currentFolderId ? '该收藏夹暂无内容' : '暂无收藏' }}
          </div>

          <div v-else class="fav-grid">
            <div v-for="fav in favorites" :key="fav.id" class="fav-card"
              :class="{ selected: selectMode && selectedBgUrl === (fav.mediaFilePath || fav.thumbnailPath) }"
              @click="selectMode ? selectBg(fav) : null">
              <img :src="fav.mediaThumbPath || fav.mediaFilePath || '/default-avatar.png'" class="fav-img">
              <div class="fav-card-info">
                <div class="fav-name">{{ fav.mediaFilename || '未命名' }}</div>
                <div class="fav-meta">
                  <span class="fav-type">{{ fav.mediaType === 'video' ? '🎬' : '🖼️' }}</span>
                  <span v-if="!currentFolderId && fav.folderName" class="fav-folder-tag">{{ fav.folderName }}</span>
                </div>
              </div>
              <div v-if="!selectMode" class="fav-actions">
                <button v-if="!currentFolderId && folders.length > 0" @click.stop="showMoveMenu(fav)"
                  class="fav-act-btn" title="移动到">📁</button>
                <button v-if="currentFolderId" @click.stop="removeFromCurrent(fav.id)"
                  class="fav-act-btn" title="移出收藏夹">↩</button>
                <button @click.stop="deleteFav(fav.id)" class="fav-act-btn danger" title="删除">✕</button>
              </div>
            </div>
          </div>

          <!-- Pagination -->
          <div v-if="totalPages > 1" class="pagination">
            <button :disabled="page === 1" @click="page--; loadFavorites()">上一页</button>
            <span>{{ page }} / {{ totalPages }}</span>
            <button :disabled="page >= totalPages" @click="page++; loadFavorites()">下一页</button>
          </div>
        </div>
      </div>

      <!-- Create folder dialog -->
      <div v-if="showCreateFolder" class="dialog-overlay" @click.self="showCreateFolder = false">
        <div class="dialog-box">
          <h4>新建收藏夹</h4>
          <input v-model="newFolderName" placeholder="收藏夹名称" maxlength="50">
          <input v-model="newFolderDesc" placeholder="描述（可选）" maxlength="200">
          <div class="dialog-actions">
            <button @click="showCreateFolder = false">取消</button>
            <button @click="createNewFolder" :disabled="!newFolderName.trim()">创建</button>
          </div>
        </div>
      </div>

      <!-- Move to folder dialog -->
      <div v-if="showMoveDialog" class="dialog-overlay" @click.self="showMoveDialog = false">
        <div class="dialog-box">
          <h4>移动到收藏夹</h4>
          <div class="folder-options">
            <div v-for="f in folders" :key="f.id" class="move-option"
              @click="moveToFolder(moveTargetId, f.id)">
              {{ f.name }}
            </div>
          </div>
          <button @click="showMoveDialog = false" class="cancel-link">取消</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import * as favoriteApi from '@/api/favoriteApi'

const props = defineProps({
  selectMode: { type: Boolean, default: false },
  visible: { type: Boolean, default: false }
})
const emit = defineEmits(['close', 'select'])

const folders = ref([])
const favorites = ref([])
const currentFolderId = ref(null)
const showAllFavs = ref(false)
const unfolderCount = ref(0)
const loading = ref(false)
const page = ref(1)
const totalPages = ref(1)
const pageSize = 20

const showCreateFolder = ref(false)
const newFolderName = ref('')
const newFolderDesc = ref('')

const showMoveDialog = ref(false)
const moveTargetId = ref(null)
const selectedBgUrl = ref('')

const loadFolders = async () => {
  try {
    const res = await favoriteApi.getFolders()
    if (res.code === 0) folders.value = res.data || []
  } catch (e) { console.error('加载收藏夹失败', e) }
}

const loadFavorites = async () => {
  loading.value = true
  try {
    const params = { pageNum: page.value, pageSize }
    if (currentFolderId.value) params.folderId = currentFolderId.value
    const res = await favoriteApi.getFavorites(params)
    if (res.code === 0) {
      favorites.value = res.data?.items || res.data || []
      totalPages.value = Math.ceil((res.data?.total || favorites.value.length) / pageSize) || 1
    }
    // Also get unfolder count
    if (!currentFolderId.value) {
      unfolderCount.value = res.data?.total || favorites.value.length
    }
  } catch (e) { console.error('加载收藏失败', e) }
  loading.value = false
}

const createNewFolder = async () => {
  if (!newFolderName.value.trim()) return
  try {
    const res = await favoriteApi.createFolder({ folderName: newFolderName.value.trim(), description: newFolderDesc.value })
    if (res.code === 0) {
      ElMessage.success('收藏夹已创建')
      showCreateFolder.value = false
      newFolderName.value = ''
      newFolderDesc.value = ''
      await loadFolders()
    } else ElMessage.error(res.msg)
  } catch (e) { ElMessage.error('创建失败') }
}

const showMoveMenu = (fav) => {
  moveTargetId.value = fav.id
  showMoveDialog.value = true
}

const moveToFolder = async (favId, folderId) => {
  try {
    await favoriteApi.moveFavorite(favId, { folderId })
    ElMessage.success('已移动')
    showMoveDialog.value = false
    loadFavorites()
    loadFolders()
  } catch (e) { ElMessage.error('移动失败') }
}

const removeFromCurrent = async (favId) => {
  try {
    await favoriteApi.removeFromFolder(favId)
    ElMessage.success('已移出收藏夹')
    loadFavorites()
    loadFolders()
  } catch (e) { ElMessage.error('操作失败') }
}

const deleteFav = async (favId) => {
  if (!confirm('确定删除该收藏吗？')) return
  try {
    await favoriteApi.deleteFavorite(favId)
    ElMessage.success('已删除')
    loadFavorites()
    loadFolders()
  } catch (e) { ElMessage.error('删除失败') }
}

const selectBg = (fav) => {
  const url = fav.mediaFilePath || fav.mediaThumbPath
  if (url) {
    selectedBgUrl.value = url
    emit('select', url)
  }
}

onMounted(async () => {
  await loadFolders()
  await loadFavorites()
})
</script>

<style scoped>
.fav-manager-overlay { position: absolute; inset: 0; background: rgba(0,0,0,0.3); z-index: 2500; display: flex; align-items: center; justify-content: center; }
.fav-manager { width: 800px; height: 550px; background: white; border-radius: 12px; display: flex; flex-direction: column; overflow: hidden; }
.fav-header { display: flex; justify-content: space-between; align-items: center; padding: 14px 20px; border-bottom: 1px solid #eee; }
.fav-header h3 { margin: 0; color: #f1177d; font-size: 16px; }
.close-btn { background: none; border: none; font-size: 20px; cursor: pointer; color: #999; }
.fav-body { display: flex; flex: 1; overflow: hidden; }

.fav-sidebar { width: 200px; border-right: 1px solid #eee; padding: 10px; display: flex; flex-direction: column; overflow-y: auto; }
.folder-list { flex: 1; }
.folder-item { padding: 8px 10px; cursor: pointer; border-radius: 6px; display: flex; justify-content: space-between; font-size: 13px; }
.folder-item:hover { background: #fff0f5; }
.folder-item.active { background: rgba(80,185,180,0.15); color: #f1177d; font-weight: 500; }
.folder-item.sub { padding-left: 24px; font-size: 12px; }
.count { font-size: 11px; color: #999; background: #f0f0f0; padding: 0 6px; border-radius: 8px; }
.new-folder-btn { margin-top: 8px; padding: 8px; background: #fff0f5; border: 1px dashed #ff69b4; border-radius: 6px; color: #ff69b4; cursor: pointer; font-size: 12px; text-align: center; }

.fav-content { flex: 1; padding: 12px; overflow-y: auto; }
.select-hint { text-align: center; color: #f1177d; font-size: 13px; margin-bottom: 8px; padding: 4px; background: #fff0f5; border-radius: 4px; }
.loading-text, .empty-text { text-align: center; color: #999; padding: 40px; }
.fav-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; }
.fav-card { border: 2px solid transparent; border-radius: 8px; overflow: hidden; background: #fafafa; cursor: pointer; transition: all 0.2s; position: relative; }
.fav-card:hover { border-color: #ff69b4; transform: translateY(-2px); }
.fav-card.selected { border-color: #f1177d; box-shadow: 0 0 8px rgba(241,23,125,0.3); }
.fav-img { width: 100%; height: 120px; object-fit: cover; }
.fav-card-info { padding: 6px 8px; }
.fav-name { font-size: 12px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.fav-meta { display: flex; gap: 4px; margin-top: 2px; font-size: 11px; }
.fav-folder-tag { color: #ff69b4; background: #fff0f5; padding: 0 4px; border-radius: 4px; }
.fav-actions { position: absolute; top: 4px; right: 4px; display: flex; gap: 2px; }
.fav-act-btn { width: 22px; height: 22px; border-radius: 50%; border: none; background: rgba(255,255,255,0.9); cursor: pointer; font-size: 11px; display: flex; align-items: center; justify-content: center; }
.fav-act-btn:hover { background: #f0f0f0; }
.fav-act-btn.danger:hover { background: #ffebee; color: #ff4757; }

.pagination { display: flex; justify-content: center; align-items: center; gap: 10px; margin-top: 12px; font-size: 12px; }
.pagination button { padding: 4px 12px; border: 1px solid #ddd; border-radius: 4px; background: white; cursor: pointer; font-size: 12px; }
.pagination button:disabled { opacity: 0.4; cursor: not-allowed; }

.dialog-overlay { position: absolute; inset: 0; background: rgba(0,0,0,0.3); display: flex; align-items: center; justify-content: center; z-index: 2600; }
.dialog-box { background: white; border-radius: 10px; padding: 20px; width: 300px; }
.dialog-box h4 { margin: 0 0 12px; color: #f1177d; }
.dialog-box input { width: 100%; padding: 8px; border: 1px solid #ddd; border-radius: 6px; margin-bottom: 8px; font-size: 13px; box-sizing: border-box; }
.dialog-actions { display: flex; gap: 8px; justify-content: flex-end; }
.dialog-actions button { padding: 6px 14px; border: none; border-radius: 6px; cursor: pointer; font-size: 13px; }
.dialog-actions button:first-child { background: #f5f5f5; }
.dialog-actions button:last-child { background: #ff69b4; color: white; }
.dialog-actions button:disabled { opacity: 0.5; }
.folder-options { max-height: 200px; overflow-y: auto; }
.move-option { padding: 10px; cursor: pointer; border-radius: 6px; font-size: 13px; }
.move-option:hover { background: #fff0f5; }
.cancel-link { display: block; width: 100%; padding: 8px; background: none; border: none; color: #999; cursor: pointer; margin-top: 8px; font-size: 12px; }
</style>
