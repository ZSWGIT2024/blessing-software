<!-- EmojiManagement.vue - 表情包管理组件 -->
<template>
  <div class="emoji-management">
    <div class="management-header">
      <h3><i class="iconfont icon-emoji"></i> 表情包管理</h3>
      <div class="header-actions">
        <el-button type="primary" @click="showCreatePackDialog">
          <i class="iconfont icon-add"></i> 新建表情包
        </el-button>
      </div>
    </div>

    <!-- 表情包列表 -->
    <div class="pack-list">
      <el-row :gutter="20">
        <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="pack in packList" :key="pack.id">
          <el-card class="pack-card" :body-style="{ padding: '0px' }" shadow="hover">
            <div class="pack-cover" @click="showPackDetail(pack)">
              <img :src="pack.coverUrl || defaultCover" :alt="pack.packName">
              <div class="pack-stats">
                <span><i class="iconfont icon-image"></i> {{ pack.itemCount }}</span>
              </div>
            </div>
            <div class="pack-info">
              <div class="pack-name">{{ pack.packName }}</div>
              <div class="pack-desc">{{ pack.description || '暂无描述' }}</div>
              <div class="pack-actions">
                <el-button-group>
                  <el-button size="small" @click="editPack(pack)">
                    <i class="iconfont icon-edit"></i> 编辑
                  </el-button>
                  <el-button size="small" @click="showUploadItemsDialog(pack)">
                    <i class="iconfont icon-upload"></i> 上传
                  </el-button>
                  <el-button size="small" type="danger" @click="confirmDeletePack(pack)">
                    <i class="iconfont icon-delete"></i> 删除
                  </el-button>
                </el-button-group>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 创建/编辑表情包对话框 -->
    <el-dialog :title="packDialogTitle" v-model="packDialogVisible" width="500px">
      <el-form :model="packForm" :rules="packRules" ref="packFormRef" label-width="100px">
        <el-form-item label="表情包名称" prop="packName">
          <el-input v-model="packForm.packName" placeholder="请输入表情包名称"></el-input>
        </el-form-item>
        <el-form-item label="封面图片" prop="coverUrl">
          <div class="upload-cover">
            <el-upload
              class="avatar-uploader"
              action="/upload"
              :show-file-list="false"
              :before-upload="handleCoverUpload"
              :http-request="uploadCoverFile"
              :on-success="handleCoverSuccess"
              :on-error="handleCoverError"
            >
              <img v-if="packForm.coverUrl" :src="packForm.coverUrl" class="avatar">
              <div v-else class="avatar-uploader-icon">
                <span class="icon-add">上传封面</span>
              </div>
            </el-upload>
          </div>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input type="textarea" v-model="packForm.description" rows="3" placeholder="请输入表情包描述"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="packDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPackForm" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <!-- 表情包详情对话框（管理图片） -->
    <el-dialog :title="currentPack?.packName" v-model="detailDialogVisible" width="80%" fullscreen>
      <div class="pack-detail-header">
        <div class="detail-actions">
          <el-button type="primary" @click="showUploadItemsDialog(currentPack)">
            <i class="iconfont icon-upload"></i> 批量上传图片
          </el-button>
          <el-button type="danger" :disabled="!selectedItems.length" @click="batchDeleteItems">
            <i class="iconfont icon-delete"></i> 批量删除 ({{ selectedItems.length }})
          </el-button>
        </div>
      </div>

      <!-- 图片列表 -->
      <div class="item-list" v-loading="itemsLoading">
        <el-checkbox-group v-model="selectedItems" @change="handleSelectionChange">
          <el-row :gutter="10">
            <el-col :xs="12" :sm="8" :md="6" :lg="4" v-for="item in itemList" :key="item.id">
              <el-card class="item-card" :body-style="{ padding: '8px' }" shadow="hover">
                <div class="item-checkbox">
                  <el-checkbox :label="item.id" @click.stop></el-checkbox>
                </div>
                <div class="item-image">
                  <el-image :src="item.imageUrl" :preview-src-list="[item.imageUrl]" fit="cover"></el-image>
                </div>
                <div class="item-info">
                  <el-input
                    v-model="item.description"
                    size="small"
                    placeholder="描述"
                    @blur="updateItemDescription(item)"
                  ></el-input>
                </div>
                <div class="item-actions">
                  <el-button type="danger" size="small" circle @click="deleteSingleItem(item)">
                    <i class="iconfont icon-delete" title="删除">X</i>
                  </el-button>
                </div>
              </el-card>
            </el-col>
          </el-row>
        </el-checkbox-group>
        
        <div v-if="itemList.length === 0" class="empty-tip">
          <el-empty description="暂无表情图片，请上传"></el-empty>
        </div>
      </div>
    </el-dialog>

    <!-- 批量上传图片对话框 -->
    <el-dialog :title="`上传图片 - ${currentPack?.packName}`" v-model="uploadDialogVisible">
      <el-upload
        class="upload-items"
        drag
        multiple
        :auto-upload="false"
        action="/upload"
        :before-upload="beforeItemUpload"
        :on-change="handleItemChange"
        :on-remove="handleItemRemove"
        :file-list="uploadFileList"
        list-type="picture-card"
      >
        <i class="el-icon-upload"></i>
        <div class="el-upload__text">将图片拖到此处或<em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip">
            支持jpg/png/jpeg/webp格式，单张不超过10MB
          </div>
        </template>
      </el-upload>
      
      <div class="upload-descriptions" v-if="uploadFileList.length > 0">
        <div class="desc-title">图片描述（可选）</div>
        <div v-for="(file, index) in uploadFileList" :key="file.uid" class="desc-item">
          <span class="file-name">{{ file.name }}</span>
          <el-input v-model="uploadDescriptions[index]" placeholder="输入描述" size="small"></el-input>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitUploadItems" :loading="uploading">
          开始上传 ({{ uploadFileList.length }})
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getEmojiPacks,
  getEmojiPackItems,
  createEmojiPack,
  updateEmojiPack,
  deleteEmojiPack,
  uploadPackCover,
  uploadPackItems,
  updatePackItem,
  deletePackItem,
  batchDeletePackItems
} from '@/api/emojiApi'

// 默认封面图
const defaultCover = new URL('@/assets/default-pack-cover.png', import.meta.url).href

// 表情包列表
const packList = ref([])
const loading = ref(false)

// 当前选中的表情包
const currentPack = ref(null)

// 对话框控制
const packDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const uploadDialogVisible = ref(false)
const packDialogTitle = computed(() => packForm.id ? '编辑表情包' : '新建表情包')

// 表单相关
const packFormRef = ref(null)
const packForm = reactive({
  id: null,
  packName: '',
  coverUrl: '',
  description: ''
})

const packRules = {
  packName: [
    { required: true, message: '请输入表情包名称', trigger: 'blur' },
    { min: 1, max: 50, message: '长度在 1 到 50 个字符', trigger: 'blur' }
  ]
}

const submitting = ref(false)

// 图片列表相关
const itemList = ref([])
const itemsLoading = ref(false)
const selectedItems = ref([])

// 上传相关
const uploadFileList = ref([])
const uploadDescriptions = ref([])
const uploading = ref(false)

// 加载表情包列表
const loadPackList = async () => {
  loading.value = true
  try {
    const res = await getEmojiPacks()
    packList.value = res.data || []
  } catch (error) {
    ElMessage.error('加载表情包列表失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 显示创建表情包对话框
const showCreatePackDialog = () => {
  packForm.id = null
  packForm.packName = ''
  packForm.coverUrl = ''
  packForm.description = ''
  packDialogVisible.value = true
}

// 编辑表情包
const editPack = (pack) => {
  packForm.id = pack.id
  packForm.packName = pack.packName
  packForm.coverUrl = pack.coverUrl || ''
  packForm.description = pack.description || ''
  packDialogVisible.value = true
}

// 封面图片上传处理
const handleCoverUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt10M = file.size / 1024 / 1024 < 10

  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  if (!isLt10M) {
    ElMessage.error('图片大小不能超过 10MB!')
    return false
  }
  return true
}

const uploadCoverFile = async ({ file }) => {
  try {
    const res = await uploadPackCover(file)
    return res
  } catch (error) {
    throw error
  }
}

const handleCoverSuccess = (response) => {
  if (response.code === 0) {
    packForm.coverUrl = response.data
    ElMessage.success('封面上传成功')
  } else {
    ElMessage.error(response.message || '封面上传失败')
  }
}

const handleCoverError = () => {
  ElMessage.error('封面上传失败')
}

// 提交表情包表单
const submitPackForm = async () => {
  if (!packFormRef.value) return
  
  await packFormRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        if (packForm.id) {
          // 更新
          await updateEmojiPack(packForm.id, packForm)
          ElMessage.success('更新成功')
        } else {
          // 创建
          await createEmojiPack(packForm)
          ElMessage.success('创建成功')
        }
        packDialogVisible.value = false
        loadPackList()
      } catch (error) {
        ElMessage.error(packForm.id ? '更新失败' : '创建失败')
        console.error(error)
      } finally {
        submitting.value = false
      }
    }
  })
}

// 确认删除表情包
const confirmDeletePack = (pack) => {
  ElMessageBox.confirm(
    `确定要删除表情包 "${pack.packName}" 吗？删除后所有图片将被同时删除，且无法恢复！`,
    '警告',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await deleteEmojiPack(pack.id)
      ElMessage.success('删除成功')
      loadPackList()
      // 如果当前打开的详情正是被删除的表情包，关闭详情
      if (currentPack.value?.id === pack.id) {
        detailDialogVisible.value = false
      }
    } catch (error) {
      ElMessage.error('删除失败')
      console.error(error)
    }
  }).catch(() => {})
}

// 显示表情包详情
const showPackDetail = async (pack) => {
  currentPack.value = pack
  detailDialogVisible.value = true
  await loadPackItems(pack.id)
}

// 加载表情包图片
const loadPackItems = async (packId) => {
  itemsLoading.value = true
  selectedItems.value = []
  try {
    const res = await getEmojiPackItems(packId)
    itemList.value = res.data || []
  } catch (error) {
    ElMessage.error('加载图片列表失败')
    console.error(error)
  } finally {
    itemsLoading.value = false
  }
}

// 显示上传图片对话框
const showUploadItemsDialog = (pack) => {
  currentPack.value = pack
  uploadFileList.value = []
  uploadDescriptions.value = []
  uploadDialogVisible.value = true
}

// 图片上传前的校验
const beforeItemUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt10M = file.size / 1024 / 1024 < 10

  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  if (!isLt10M) {
    ElMessage.error('图片大小不能超过 10MB!')
    return false
  }
  return true
}

// 处理文件变化
const handleItemChange = (file, fileList) => {
  // 确保每个文件都有预览URL
  fileList = fileList.map(f => {
    if (!f.url && f.raw) {
      f.url = URL.createObjectURL(f.raw);
    }
    return f;
  });
  
  uploadFileList.value = fileList;
  
  // 初始化描述数组
  while (uploadDescriptions.value.length < fileList.length) {
    uploadDescriptions.value.push('');
  }
};

// 处理文件移除
const handleItemRemove = (file, fileList) => {

  uploadFileList.value = fileList
  // 重新计算描述数组
  uploadDescriptions.value = uploadDescriptions.value.slice(0, fileList.length)
}

// 提交上传
const submitUploadItems = async () => {
  if (uploadFileList.value.length === 0) {
    ElMessage.warning('请选择要上传的图片')
    return
  }
  
  uploading.value = true
  try {
    const files = uploadFileList.value.map(item => item.raw)
    const res = await uploadPackItems(
      currentPack.value.id,
      files,
      uploadDescriptions.value
    )
    
    ElMessage.success(`成功上传 ${res.data.length} 张图片`)
    uploadDialogVisible.value = false
    
    // 刷新图片列表
    if (detailDialogVisible.value) {
      await loadPackItems(currentPack.value.id)
    }
    
    // 刷新表情包列表（更新图片数量）
    loadPackList()
  } catch (error) {
    ElMessage.error('上传失败')
    console.error(error)
  } finally {
    uploading.value = false
  }
}

// 更新图片描述
const updateItemDescription = async (item) => {
  try {
    await updatePackItem(item.id, { description: item.description })
    ElMessage.success('更新成功')
  } catch (error) {
    ElMessage.error('更新失败')
    console.error(error)
  }
}

// 删除单个图片
const deleteSingleItem = (item) => {
  ElMessageBox.confirm('确定要删除这张图片吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deletePackItem(item.id)
      ElMessage.success('删除成功')
      // 刷新列表
      await loadPackItems(currentPack.value.id)
      // 刷新表情包列表（更新图片数量）
      loadPackList()
    } catch (error) {
      ElMessage.error('删除失败')
      console.error(error)
    }
  }).catch(() => {})
}

// 处理选择变化
const handleSelectionChange = (val) => {
  selectedItems.value = val
}

// 批量删除图片
const batchDeleteItems = () => {
  if (selectedItems.value.length === 0) return
  
  ElMessageBox.confirm(`确定要删除选中的 ${selectedItems.value.length} 张图片吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await batchDeletePackItems(selectedItems.value)
      ElMessage.success('批量删除成功')
      // 刷新列表
      await loadPackItems(currentPack.value.id)
      // 刷新表情包列表（更新图片数量）
      loadPackList()
      selectedItems.value = []
    } catch (error) {
      ElMessage.error('批量删除失败')
      console.error(error)
    }
  }).catch(() => {})
}

// 初始化
onMounted(() => {
  loadPackList()
})
</script>

<style scoped>
.emoji-management {
  padding: 20px;
}

.management-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.management-header h3 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.pack-list {
  min-height: 300px;
}

.pack-card {
  margin-bottom: 20px;
  cursor: pointer;
  transition: transform 0.3s;
}

.pack-card:hover {
  transform: translateY(-5px);
}

.pack-cover {
  position: relative;
  height: 160px;
  overflow: hidden;
}

.pack-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.pack-stats {
  position: absolute;
  bottom: 10px;
  right: 10px;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
}

.pack-info {
  padding: 12px;
}

.pack-name {
  font-weight: bold;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.pack-desc {
  font-size: 12px;
  color: #999;
  margin-bottom: 12px;
  height: 32px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-box-orient: vertical;
}

.pack-actions {
  display: flex;
  justify-content: flex-end;
}

/* 封面上传样式 */
.avatar-uploader {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  width: 200px;
  height: 200px;
}

.avatar-uploader:hover {
  border-color: #409eff;
}

.avatar-uploader-icon {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}

.avatar-uploader-icon .icon-add {
  font-size: 15px;
  margin-top: 80px;
  margin-left: 60px;
  background: #fafafa;
  color: #8c939d;
}

.avatar {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

/* 图片列表样式 */
.pack-detail-header {
  margin-bottom: 20px;
}

.item-list {
  min-height: 400px;
}

.item-card {
  position: relative;
  margin-bottom: 10px;
}

.item-checkbox {
  position: absolute;
  top: 12px;
  left: 12px;
  z-index: 1;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 4px;
  padding: 2px;
}

.item-image {
  width: 100%;
  height: 120px;
  margin-bottom: 8px;
}

.item-image :deep(.el-image) {
  width: 100%;
  height: 100%;
}

.item-image :deep(.el-image__inner) {
  object-fit: cover;
}

.item-info {
  margin-bottom: 8px;
}

.item-actions {
  display: flex;
  justify-content: flex-end;
}

.empty-tip {
  padding: 40px 0;
}
/* 批量上传表单框 */
.batch-upload-dialog {
  width: 600px;
  height: 800px;
  max-width: 100%;
  margin: 0 auto;
}

.batch-upload-dialog .el-upload-dragger {
  width: 100%;
  height: 200px;
}

.batch-upload-dialog .el-upload-dragger .el-icon {
  margin-top: 80px;
}

/* 上传描述样式 */
.upload-descriptions {
  margin-top: 20px;
  max-height: 300px;
  overflow-y: auto;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 10px;
}

.desc-title {
  font-weight: bold;
  margin-bottom: 10px;
}

.desc-item {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.file-name {
  width: 150px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-right: 10px;
  font-size: 12px;
  color: #606266;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .emoji-management {
    padding: 10px;
  }
  
  .pack-cover {
    height: 120px;
  }
  
  .item-image {
    height: 100px;
  }
  
  .desc-item {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .file-name {
    width: 100%;
    margin-bottom: 5px;
  }
}
</style>