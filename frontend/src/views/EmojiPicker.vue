<template>
  
    <Transition name="fade">
      <div v-if="visible" class="emoji-picker" ref="pickerRef" :style="positionStyle">
        <!-- 主内容区域 -->
        <div class="emoji-picker-content">
          <!-- 顶部标签页 -->
          <div class="emoji-picker-tabs">
            <button 
              v-for="tab in tabs" 
              :key="tab.id"
              :class="['tab-btn', { active: activeTab === tab.id }]"
              @click="switchTab(tab.id)"
            >
              <span class="tab-icon">{{ tab.icon }}</span>
              <span class="tab-text">{{ tab.name }}</span>
            </button>
          </div>

          <!-- 最近使用标签页 -->
          <div v-show="activeTab === 'recent'" class="tab-panel">
            <div class="panel-header">
              <h4>最近使用</h4>
              <button v-if="recentEmojis.length > 0 || recentPackItems.length > 0" @click="clearRecent" class="clear-btn">清除</button>
            </div>
            
            <!-- 最近使用分类切换 -->
            <div class="sub-tabs">
              <button 
                v-for="type in recentTypes" 
                :key="type.value"
                :class="['sub-tab-btn', { active: recentActiveType === type.value }]"
                @click="recentActiveType = type.value"
              >
                {{ type.label }}
              </button>
            </div>

            <!-- 最近使用内容 -->
            <div class="recent-content">
              <div v-if="loading.recent" class="loading-state">
                <span class="spinner"></span> 加载中...
              </div>
              
              <template v-else>
                <!-- 最近使用的Emoji -->
                <div v-if="recentActiveType === 'emoji'" class="emoji-grid">
                  <div 
                    v-for="item in recentEmojis" 
                    :key="item.id"
                    class="emoji-item"
                    @click="selectEmoji(item)"
                    :title="item.emojiName"
                  >
                    {{ item.emojiCode }}
                  </div>
                  <div v-if="recentEmojis.length === 0" class="empty-state">
                    暂无最近使用的Emoji
                  </div>
                </div>

                <!-- 最近使用的表情包 -->
                <div v-if="recentActiveType === 'pack'" class="pack-grid">
                  <div 
                    v-for="item in recentPackItems" 
                    :key="item.id"
                    class="pack-item"
                    @click="selectPackItem(item)"
                    :title="item.description"
                  >
                    <img :src="item.imageUrl" :alt="item.description" loading="lazy" />
                  </div>
                  <div v-if="recentPackItems.length === 0" class="empty-state">
                    暂无最近使用的表情包
                  </div>
                </div>
              </template>
            </div>
          </div>

          <!-- 默认表情标签页 -->
          <div v-show="activeTab === 'default'" class="tab-panel">
            <div class="panel-header">
              <h4>默认表情</h4>
            </div>
            
            <!-- 表情分类 -->
            <div class="emoji-categories">
              <button 
                v-for="category in systemEmojiCategories" 
                :key="category.id"
                :class="['category-btn', { active: activeEmojiCategory === category.id }]"
                @click="loadSystemEmojis(category.id)"
              >
                {{ category.icon }} {{ category.name }}
              </button>
            </div>

            <!-- 表情内容 -->
            <div class="emoji-content">
              <div v-if="loading.systemEmojis" class="loading-state">
                <span class="spinner"></span> 加载中...
              </div>
              
              <div v-else class="emoji-grid">
                <div 
                  v-for="emoji in systemEmojis" 
                  :key="emoji.name"
                  class="emoji-item"
                  @click="selectEmoji(emoji)"
                  :title="emoji.name"
                >
                <button 
                      @click.stop="togglePackItemFavorite(emoji)"
                      class="favorite-btn"
                      :class="{ active: emoji.isFavorite }"
                    >
                      {{ emoji.isFavorite ? '★' : '☆' }}
                    </button>
                  {{ emoji.code }}
                </div>
              </div>
            </div>
          </div>

          <!-- 我的收藏标签页 -->
          <div v-show="activeTab === 'favorite'" class="tab-panel">
            <div class="panel-header">
              <h4>我的收藏</h4>
            </div>
            
            <!-- 收藏分类切换 -->
            <div class="sub-tabs">
              <button 
                v-for="type in favoriteTypes" 
                :key="type.value"
                :class="['sub-tab-btn', { active: favoriteActiveType === type.value }]"
                @click="switchFavoriteType(type.value)"
              >
                {{ type.label }}
              </button>
            </div>

            <!-- 收藏内容 -->
            <div class="favorite-content">
              <div v-if="loading.favorites" class="loading-state">
                <span class="spinner"></span> 加载中...
              </div>
              
              <template v-else>
                <!-- 收藏的Emoji -->
                <div v-if="favoriteActiveType === 'emoji'" class="emoji-grid">
                  <div 
                    v-for="item in favoriteEmojis" 
                    :key="item.id"
                    class="emoji-item with-actions"
                  >
                    <span class="emoji-code" @click="selectEmoji(item)">{{ item.emojiCode }}</span>
                    <button @click.stop="removeFromFavorites(item)" class="remove-btn" title="取消收藏">×</button>
                  </div>
                  
                  <!-- 分页 -->
                  <div v-if="favoritePagination.total > favoritePagination.pageSize" class="pagination">
                    <button 
                      @click="loadFavoriteEmojis(favoritePagination.page - 1)" 
                      :disabled="favoritePagination.page === 1"
                      class="page-btn"
                    >←</button>
                    <span class="page-info">{{ favoritePagination.page }}/{{ favoritePagination.totalPages }}</span>
                    <button 
                      @click="loadFavoriteEmojis(favoritePagination.page + 1)" 
                      :disabled="favoritePagination.page === favoritePagination.totalPages"
                      class="page-btn"
                    >→</button>
                  </div>
                </div>

                <!-- 收藏的表情包 -->
                <div v-if="favoriteActiveType === 'pack'" class="pack-grid">
                  <div 
                    v-for="item in favoritePackItems" 
                    :key="item.id"
                    class="pack-item with-actions"
                  >
                    <img 
                      :src="item.imageUrl" 
                      :alt="item.description" 
                      @click="selectPackItem(item)"
                      loading="lazy"
                    />
                    <button @click.stop="removeFromFavorites(item)" class="remove-btn" title="取消收藏">×</button>
                  </div>
                  
                  <!-- 分页 -->
                  <div v-if="packPagination.total > packPagination.pageSize" class="pagination">
                    <button 
                      @click="loadFavoritePackItems(packPagination.page - 1)" 
                      :disabled="packPagination.page === 1"
                      class="page-btn"
                    >←</button>
                    <span class="page-info">{{ packPagination.page }}/{{ packPagination.totalPages }}</span>
                    <button 
                      @click="loadFavoritePackItems(packPagination.page + 1)" 
                      :disabled="packPagination.page === packPagination.totalPages"
                      class="page-btn"
                    >→</button>
                  </div>
                </div>

                <div v-if="favoriteActiveType === 'emoji' && favoriteEmojis.length === 0" class="empty-state">
                  暂无收藏的Emoji
                </div>
                <div v-if="favoriteActiveType === 'pack' && favoritePackItems.length === 0" class="empty-state">
                  暂无收藏的表情包
                </div>
              </template>
            </div>
          </div>

          <!-- 表情包标签页 -->
          <div v-show="activeTab === 'packs'" class="tab-panel">
            <div class="panel-header">
              <h4>表情包</h4>
            </div>
            
 <!-- 表情包详情视图 -->
            <div v-if="selectedPack" class="pack-detail-view">
              <div class="pack-detail-header">
                <button @click="selectedPack = null" class="back-btn">← 返回</button>
                <h4>{{ selectedPack.packName }}</h4>
              </div>
              
              <div class="pack-items">
                <div v-if="loading.packItems" class="loading-state">
                  <span class="spinner"></span> 加载中...
                </div>
                
                <div v-else class="pack-items-grid">
                  <div 
                    v-for="item in packItems" 
                    :key="item.id"
                    class="pack-item with-actions"
                  >
                    <img 
                      :src="item.imageUrl" 
                      :alt="item.description"
                      @click="selectPackItem(item)"
                      loading="lazy"
                    />
                    <button 
                      @click.stop="togglePackItemFavorite(item)"
                      class="favorite-btn"
                      :class="{ active: item.isFavorite }"
                    >
                      {{ item.isFavorite ? '★' : '☆' }}
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <!-- 表情包列表 -->
            <div class="packs-list">
              <div v-if="loading.packs" class="loading-state">
                <span class="spinner"></span> 加载中...
              </div>
              
              <div v-else class="packs-grid">
                <div 
                  v-for="pack in emojiPacks" 
                  :key="pack.id"
                  class="pack-card"
                  @click="selectPack(pack)"
                >
                  <div class="pack-cover">
                    <img :src="pack.coverUrl" :alt="pack.packName" />
                  </div>
                  <div class="pack-info">
                    <div class="pack-name">{{ pack.packName }}</div>
                    <div class="pack-count">{{ pack.itemCount }}个表情</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 底部预览 -->
        <div class="emoji-picker-footer">
          <div class="preview-area" v-if="previewEmoji">
            <span class="preview-label">预览：</span>
            <span v-if="previewEmoji.type === 'emoji'" class="preview-emoji">{{ previewEmoji.code }}</span>
            <img v-else :src="previewEmoji.imageUrl" class="preview-image" />
          </div>
          <button @click="close" class="close-btn">关闭</button>
        </div>
      </div>
    </Transition>
  
</template>

<script setup>
import { ref, onMounted, onUnmounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import * as emojiApi from '@/api/emojiApi'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  targetElement: {
    type: HTMLElement,
    default: null
  }
})

const emit = defineEmits(['select', 'select-pack', 'close', 'update:visible'])

// 状态
const activeTab = ref('recent')
const pickerRef = ref(null)
const positionStyle = ref({})
const previewEmoji = ref(null)

// 加载状态
const loading = ref({
  recent: false,
  systemEmojis: false,
  favorites: false,
  packs: false,
  packItems: false
})

// 标签页配置
const tabs = [
  { id: 'recent', icon: '🕒', name: '最近' },
  { id: 'default', icon: '😀', name: '默认' },
  { id: 'favorite', icon: '⭐', name: '收藏' },
  { id: 'packs', icon: '📦', name: '表情包' }
]

// ==================== 最近使用 ====================
const recentTypes = [
  { value: 'emoji', label: 'Emoji' },
  { value: 'pack', label: '表情包' }
]
const recentActiveType = ref('emoji')
const recentEmojis = ref([])
const recentPackItems = ref([])

// 加载最近使用
const loadRecentEmojis = async () => {
  loading.value.recent = true
  try {
    const response = await emojiApi.getRecentEmojis(20)
    recentEmojis.value = response.data.emojis || []
    recentPackItems.value = response.data.packs || []
  } catch (error) {
    console.error('加载最近使用失败:', error)
  } finally {
    loading.value.recent = false
  }
}

// 记录使用
const recordUsage = async (type, data) => {
  try {
    if (type === 'emoji') {
     await emojiApi.recordUsage({
        type: 1,
        emojiCode: data.code,
        emojiName: data.name
      })
    } else {
      await emojiApi.recordUsage({
        type: 2,
        packItemId: data.id
      })
    }
    // 刷新最近使用
    loadRecentEmojis()
  } catch (error) {
    console.error('记录使用失败:', error)
  }
}

// 清除最近使用
const clearRecent = async () => {
  try {
    const type = recentActiveType.value === 'emoji' ? 1 : 2
    await emojiApi.clearRecent(type)
    if (recentActiveType.value === 'emoji') {
      recentEmojis.value = []
    } else {
      recentPackItems.value = []
    }
    ElMessage.success('清除成功')
  } catch (error) {
    ElMessage.error('清除失败')
  }
}

// ==================== 系统表情 ====================
const systemEmojiCategories = ref([])
const activeEmojiCategory = ref('')
const systemEmojis = ref([])

// 加载系统表情分类
const loadSystemEmojiCategories = async () => {
  try {
    const response = await emojiApi.getSystemEmojiCategories()
    systemEmojiCategories.value = response.data
    if (systemEmojiCategories.value.length > 0) {
      activeEmojiCategory.value = systemEmojiCategories.value[0].id
      loadSystemEmojis(activeEmojiCategory.value)
    }
  } catch (error) {
    console.error('加载表情分类失败:', error)
  }
}

// 加载系统表情
const loadSystemEmojis = async (category) => {
  activeEmojiCategory.value = category
  loading.value.systemEmojis = true
  try {
    const response = await emojiApi.getSystemEmojis(category)
    // 注入 type 和 isFavorite 字段
    systemEmojis.value = (response.data || []).map(e => ({
      ...e, type: 'emoji', isFavorite: e.isFavorite || false
    }))
  } catch (error) {
    console.error('加载系统表情失败:', error)
  } finally {
    loading.value.systemEmojis = false
  }
}

// ==================== 收藏 ====================
const favoriteTypes = [
  { value: 'emoji', label: 'Emoji' },
  { value: 'pack', label: '表情包' }
]
const favoriteActiveType = ref('emoji')
const favoriteEmojis = ref([])
const favoritePackItems = ref([])

const favoritePagination = ref({
  page: 1,
  pageSize: 20,
  total: 0,
  totalPages: 0
})

const packPagination = ref({
  page: 1,
  pageSize: 20,
  total: 0,
  totalPages: 0
})

// 切换收藏类型
const switchFavoriteType = (type) => {
  favoriteActiveType.value = type
  if (type === 'emoji') {
    loadFavoriteEmojis(1)
  } else {
    loadFavoritePackItems(1)
  }
}

// 加载收藏的Emoji
const loadFavoriteEmojis = async (page = 1) => {
  loading.value.favorites = true
  try {
    const response = await emojiApi.getFavorites(1, page, favoritePagination.value.pageSize)
    favoriteEmojis.value = response.data.items
    favoritePagination.value = {
      page: response.data.page,
      pageSize: response.data.pageSize,
      total: response.data.total,
      totalPages: response.data.totalPages
    }
  } catch (error) {
    console.error('加载收藏Emoji失败:', error)
  } finally {
    loading.value.favorites = false
  }
}

// 加载收藏的表情包
const loadFavoritePackItems = async (page = 1) => {
  loading.value.favorites = true
  try {
    const response = await emojiApi.getFavorites(2, page, packPagination.value.pageSize)
    favoritePackItems.value = response.data.items
    packPagination.value = {
      page: response.data.page,
      pageSize: response.data.pageSize,
      total: response.data.total,
      totalPages: response.data.totalPages
    }
  } catch (error) {
    console.error('加载收藏表情包失败:', error)
  } finally {
    loading.value.favorites = false
  }
}

// 添加收藏
const addToFavorites = async (item) => {
  try {
    if (item.type === 'emoji') {
      await emojiApi.addFavorite({
        type: 1,
        emojiCode: item.emojiCode || item.code,
        emojiName: item.emojiName || item.name
      })
    } else {
      await emojiApi.addFavorite({
        type: 2,
        packItemId: item.id
      })
    }
    
    // 刷新收藏列表
    if (favoriteActiveType.value === 'emoji') {
      loadFavoriteEmojis(favoritePagination.value.page)
    } else {
      loadFavoritePackItems(packPagination.value.page)
    }
  } catch (error) {
    ElMessage.error('收藏失败')
  }
}

// 取消收藏
const removeFromFavorites = async (item) => {
  try {
    if (item.type === 'emoji') {
      await emojiApi.removeFavoriteByEmojiName(item.emojiName || item.name)
    } else {
      await emojiApi.removeFavorite(item.favoriteId || item.id)
    }
    
    if (item.type === 'emoji') {
      favoriteEmojis.value = favoriteEmojis.value.filter(e => e.id !== item.id)
    } else {
      favoritePackItems.value = favoritePackItems.value.filter(e => e.id !== item.id)
    }
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

// ==================== 表情包 ====================
const emojiPacks = ref([])
const selectedPack = ref(null)
const packItems = ref([])

// 加载表情包列表
const loadEmojiPacks = async () => {
  loading.value.packs = true
  try {
    const response = await emojiApi.getEmojiPacks()
    emojiPacks.value = response.data
  } catch (error) {
    console.error('加载表情包列表失败:', error)
  } finally {
    loading.value.packs = false
  }
}

// 选择表情包
const selectPack = async (pack) => {
  selectedPack.value = pack
  await loadPackItems(pack.id)
}

// 加载表情包详情
const loadPackItems = async (packId) => {
  loading.value.packItems = true
  try {
    const response = await emojiApi.getEmojiPackItems(packId)
    // 注入 type 和 isFavorite 字段
    packItems.value = (response.data || []).map(item => ({
      ...item, type: 'pack', isFavorite: item.isFavorite || false
    }))
  } catch (error) {
    console.error('加载表情包详情失败:', error)
  } finally {
    loading.value.packItems = false
  }
}

// 切换表情包项收藏状态
const togglePackItemFavorite = async (item) => {
  const itemType = item.type || (item.imageUrl ? 'pack' : 'emoji')
  if (item.isFavorite) {
    await removeFromFavorites({ ...item, type: itemType })
    item.isFavorite = false
    ElMessage.success('已取消收藏')
  } else {
    await addToFavorites({ ...item, type: itemType })
    item.isFavorite = true
    ElMessage.success('已添加收藏')
  }
}

// ==================== 选择事件 ====================
const selectEmoji = (emoji) => {
  const emojiData = {
    type: 'emoji',
    code: emoji.code || emoji.emojiCode,
    name: emoji.name || emoji.emojiName,
  }
  console.log('emojiData',emojiData);
  
  previewEmoji.value = emojiData
  recordUsage('emoji', emojiData)
  emit('select', emojiData.code) // 只传递Emoji字符
  close()
}

const selectPackItem = (item) => {
  const packData = {
    type: 'pack',
    id: item.id,
    imageUrl: item.imageUrl,
    description: item.description
  }
  previewEmoji.value = packData
  recordUsage('pack', packData)
  emit('select-pack', packData) // 传递完整的表情包数据
  close()
}

// ==================== 位置和关闭 ====================

// 修改 updatePosition，添加防抖
let updateTimeout = null

const updatePosition = () => {
  if (updateTimeout) clearTimeout(updateTimeout)
  
  updateTimeout = setTimeout(() => {
    if (!props.targetElement || !props.visible) return
    
    const rect = props.targetElement.getBoundingClientRect()
    const viewportHeight = window.innerHeight
    const pickerHeight = 400
    
    let top = rect.bottom + 10
    let left = rect.left
    
    if (top + pickerHeight > viewportHeight) {
      top = Math.max(10, viewportHeight - pickerHeight - 10)
    }
    
    if (left + 420 > window.innerWidth) {
      left = window.innerWidth - 430
    }
    
    if (left < 10) {
      left = 10
    }
    
    positionStyle.value = {
      position: 'fixed',
      top: top + 'px',
      left: left + 'px',
      maxHeight: '500px',
      overflowY: 'auto'
    }
    
    console.log('位置已更新:', positionStyle.value)
  }, 50)
}

// 修改 handleClickOutside，使用防抖并避免重复关闭
let clickOutsideTimer = null

const handleClickOutside = (event) => {
  // 如果组件没有显示，直接返回
  if (!props.visible) return
  
  // 如果点击的是组件内部或目标元素，不关闭
  if (pickerRef.value?.contains(event.target) || 
      props.targetElement?.contains(event.target)) {
    return
  }
  
  // 清除之前的定时器
  if (clickOutsideTimer) clearTimeout(clickOutsideTimer)
  
  // 延迟关闭，避免与父组件事件冲突
  clickOutsideTimer = setTimeout(() => {
    // 再次检查 visible 状态，避免重复关闭
    if (props.visible) {
      console.log('EmojiPicker handleClickOutside 关闭')
      emit('close')
      emit('update:visible', false)
    }
    clickOutsideTimer = null
  }, 50)
}

// 修改 close 方法，避免重复调用
const close = () => {
  console.log('EmojiPicker close 被调用，当前 visible:', props.visible)
  if (!props.visible) return // 如果已经不可见，直接返回
  
  emit('close')
  emit('update:visible', false)
}


// ==================== 标签页切换 ====================
const switchTab = (tabId) => {
  activeTab.value = tabId
  
  // 加载对应数据
  switch (tabId) {
    case 'recent':
      loadRecentEmojis()
      break
    case 'default':
      if (systemEmojiCategories.value.length === 0) {
        loadSystemEmojiCategories()
      } else if (activeEmojiCategory.value) {
        loadSystemEmojis(activeEmojiCategory.value)
      }
      break
    case 'favorite':
      switchFavoriteType(favoriteActiveType.value)
      break
    case 'packs':
      loadEmojiPacks()
      break
  }
}

// ==================== 生命周期 ====================
// 修改 onBeforeUnmount，只在异常时打印
onBeforeUnmount(() => {
  console.log('EmojiPicker 即将被销毁，当前 visible:', props.visible)
  // 如果 visible 为 true 但组件被销毁，这是父组件的问题，我们只需要清理
  if (props.visible) {
    console.warn('⚠️ 父组件正在强制销毁 EmojiPicker，清理资源中...')
  }
  // 清理定时器
  if (clickOutsideTimer) clearTimeout(clickOutsideTimer)
})

// 在 EmojiPicker.vue 的 onMounted 中
onMounted(() => {
  console.log('✅ EmojiPicker 组件已挂载')
  
  // 使用捕获阶段监听，避免与父组件冲突
  document.addEventListener('click', handleClickOutside, true) // 改为捕获阶段
  
  window.addEventListener('resize', updatePosition)
  
  // 加载数据
  loadRecentEmojis()
  loadSystemEmojiCategories()
})

onUnmounted(() => {
  console.log('❌ EmojiPicker 组件已销毁')
  // 移除捕获阶段的监听
  document.removeEventListener('click', handleClickOutside, true)
  window.removeEventListener('resize', updatePosition)
  
  if (clickOutsideTimer) clearTimeout(clickOutsideTimer)
})

watch(() => props.visible, (newVal) => {
  if (newVal) {
    nextTick(() => {
      updatePosition()
      loadRecentEmojis()
    })
  }
})

watch(() => props.targetElement, () => {
  if (props.visible) {
    nextTick(updatePosition)
  }
})
</script>

<style scoped>
/* 样式保持不变 */
.emoji-picker {
  position: fixed !important;
  width: 620px;
  max-width: 90vw;
  background: white;
  border-radius: 16px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.3);
  border: 2px solid #ff69b4; /* 添加明显边框用于调试 */
  overflow: hidden;
  z-index: 2147483647 !important; /* 最大 z-index */
  pointer-events: auto !important;
  visibility: visible !important;
  opacity: 1 !important;
  display: block !important;
}

/* 确保所有子元素也可见 */
.emoji-picker * {
  visibility: visible !important;
  opacity: 1 !important;
}

.emoji-picker-content {
  max-height: 500px;
  overflow-y: auto;
  padding: 16px;
}

/* 标签页 */
.emoji-picker-tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 2px solid #ffecef;
}

.tab-btn {
  flex: 1;
  padding: 8px 4px;
  border: none;
  background: transparent;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.tab-btn:hover {
  background: #fff0f5;
}

.tab-btn.active {
  background: #ff69b4;
  color: white;
}

.tab-icon {
  font-size: 20px;
}

.tab-text {
  font-size: 12px;
}

/* 面板头部 */
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.panel-header h4 {
  margin: 0;
  font-size: 14px;
  color: #333;
}

/* 子标签页 */
.sub-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  background: #fff9fa;
  padding: 4px;
  border-radius: 8px;
}

.sub-tab-btn {
  flex: 1;
  padding: 6px 12px;
  border: none;
  background: transparent;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s;
}

.sub-tab-btn:hover {
  background: #ffecef;
}

.sub-tab-btn.active {
  background: #ff69b4;
  color: white;
}

/* 表情分类 */
.emoji-categories {
  display: flex;
  gap: 4px;
  margin-bottom: 12px;
  overflow-x: auto;
  padding-bottom: 4px;
}

.emoji-categories::-webkit-scrollbar {
  height: 4px;
}

.category-btn {
  flex-shrink: 0;
  padding: 3px 6px;
  border: none;
  background: #fff9fa;
  border-radius: 20px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s;
  white-space: nowrap;
}

.category-btn:hover {
  background: #ffecef;
}

.category-btn.active {
  background: #ff69b4;
  color: white;
}

/* 网格布局 */
.emoji-grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 4px;
}

.pack-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
}

.emoji-item {
  font-size: 18px;
  text-align: center;
  padding: 10px;
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.2s;
  user-select: none;
}

.emoji-item:hover {
  background: #ffecef;
  transform: scale(1.1);
}

.emoji-item.with-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 8px;
}

.emoji-code {
  font-size: 24px;
  cursor: pointer;
}

.pack-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s;
}

.pack-item:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(255, 105, 180, 0.3);
}

.pack-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.pack-item.with-actions:hover .favorite-btn,
.pack-item.with-actions:hover .remove-btn {
  opacity: 1;
}

/* 操作按钮 */
.favorite-btn,
.remove-btn {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.9);
  cursor: pointer;
  opacity: 0;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}

.favorite-btn.active {
  opacity: 1;
  color: #ff69b4;
}

.remove-btn {
  background: rgba(255, 77, 79, 0.9);
  color: white;
  font-size: 16px;
}

.remove-btn:hover {
  background: #ff4d4f;
}

/* 清除按钮 */
.clear-btn {
  padding: 4px 12px;
  border: 1px solid #ffd6e7;
  background: white;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  color: #666;
}

.clear-btn:hover {
  background: #fff0f5;
}

/* 空状态 */
.empty-state {
  grid-column: 1 / -1;
  text-align: center;
  padding: 32px 0;
  color: #999;
  font-size: 14px;
}

/* 加载状态 */
.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 32px 0;
  color: #ff69b4;
}

.spinner {
  width: 20px;
  height: 20px;
  border: 2px solid #ffecef;
  border-top-color: #ff69b4;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

/* 分页 */
.pagination {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 16px;
}

.page-btn {
  width: 32px;
  height: 32px;
  border: 1px solid #ffd6e7;
  background: white;
  border-radius: 6px;
  cursor: pointer;
  font-size: 16px;
  transition: all 0.2s;
}

.page-btn:hover:not(:disabled) {
  background: #ffecef;
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info {
  font-size: 14px;
  color: #666;
}

/* 表情包卡片 */
.packs-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
}

.pack-card {
  background: #fff9fa;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid #ffecef;
}

.pack-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 105, 180, 0.2);
}

.pack-cover {
  aspect-ratio: 1;
  overflow: hidden;
}

.pack-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.pack-info {
  padding: 8px;
}

.pack-name {
  font-weight: 500;
  font-size: 13px;
  margin-bottom: 4px;
  color: #333;
}

.pack-count {
  font-size: 11px;
  color: #999;
}

/* 表情包详情视图 */
.pack-detail-view {
  margin-top: 16px;
  border-top: 1px solid #ffecef;
  padding-top: 16px;
}

.pack-detail-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.back-btn {
  padding: 4px 12px;
  border: none;
  background: #fff0f5;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  color: #ff69b4;
}

.pack-items-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  max-height: 300px;
  overflow-y: auto;
  padding: 8px;
}

/* 底部预览 */
.emoji-picker-footer {
  padding: 12px 16px;
  border-top: 1px solid #ffecef;
  background: #fff9fa;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.preview-area {
  display: flex;
  align-items: center;
  gap: 8px;
}

.preview-label {
  font-size: 13px;
  color: #666;
}

.preview-emoji {
  font-size: 28px;
}

.preview-image {
  width: 32px;
  height: 32px;
  border-radius: 4px;
  object-fit: cover;
}

.close-btn {
  padding: 6px 20px;
  background: #ff69b4;
  color: white;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.close-btn:hover {
  opacity: 0.9;
  transform: scale(1.05);
}

/* 动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>