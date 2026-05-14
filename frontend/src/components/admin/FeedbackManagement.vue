<template>
  <div class="feedback-mgmt">
    <!-- Stats -->
    <div class="stats-row">
      <div class="stat-card" v-for="s in stats" :key="s.key"
        :class="{ active: filterStatus === s.key }" @click="filterStatus = filterStatus === s.key ? '' : s.key">
        <div class="stat-num">{{ s.count }}</div>
        <div class="stat-label">{{ s.label }}</div>
      </div>
    </div>

    <!-- List -->
    <div class="feedback-table">
      <table>
        <thead>
          <tr>
            <th style="width:60px">ID</th>
            <th style="width:90px">用户</th>
            <th style="width:80px">类型</th>
            <th>内容</th>
            <th style="width:80px">状态</th>
            <th style="width:100px">时间</th>
            <th style="width:160px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in list" :key="item.id">
            <td>{{ item.id }}</td>
            <td>{{ item.username }}</td>
            <td><span class="type-tag">{{ typeLabel(item.type) }}</span></td>
            <td>
              <div class="content-cell">{{ item.content }}</div>
              <div v-if="item.images" class="images-cell">
                <img v-for="(url, i) in parseUrls(item.images)" :key="i" :src="url" class="mini-thumb" @click="previewImage(url)" />
              </div>
            </td>
            <td><span class="status-tag" :class="item.status">{{ statusLabel(item.status) }}</span></td>
            <td>{{ formatDate(item.createTime) }}</td>
            <td>
              <div class="action-row">
                <button v-if="item.status === 'pending' || item.status === 'processing'"
                  @click="openReply(item)" class="btn primary">处理</button>
                <button @click="doDelete(item.id)" class="btn danger">删除</button>
              </div>
            </td>
          </tr>
          <tr v-if="list.length === 0"><td colspan="7" class="empty-row">暂无数据</td></tr>
        </tbody>
      </table>
    </div>

    <!-- Pagination -->
    <div class="pagination" v-if="totalPages > 1">
      <button :disabled="page <= 1" @click="page--; load()">上一页</button>
      <span>{{ page }} / {{ totalPages }}</span>
      <button :disabled="page >= totalPages" @click="page++; load()">下一页</button>
    </div>

    <!-- Reply Dialog -->
    <div v-if="replyItem" class="dialog-overlay" @click.self="replyItem = null">
      <div class="dialog-box">
        <h3>处理反馈 #{{ replyItem.id }}</h3>
        <p class="dialog-meta">{{ replyItem.username }} · {{ typeLabel(replyItem.type) }} · {{ formatDate(replyItem.createTime) }}</p>
        <div class="dialog-content">{{ replyItem.content }}</div>
        <div class="form-group">
          <label>处理结果</label>
          <select v-model="replyStatus">
            <option value="processing">标记处理中</option>
            <option value="resolved">标记已处理</option>
            <option value="closed">关闭</option>
          </select>
        </div>
        <div class="form-group">
          <label>回复内容 (选填)</label>
          <textarea v-model="replyText" rows="4" placeholder="给用户的回复..." maxlength="500"></textarea>
        </div>
        <div class="dialog-actions">
          <button @click="replyItem = null">取消</button>
          <button @click="doReply" :disabled="replyLoading">{{ replyLoading ? '提交中...' : '确认' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as feedbackApi from '@/api/feedback'

const list = ref([])
const page = ref(1)
const totalPages = ref(1)
const filterStatus = ref('')
const stats = reactive([
  { key: 'pending', label: '待处理', count: 0 },
  { key: 'processing', label: '处理中', count: 0 },
  { key: 'resolved', label: '已处理', count: 0 },
  { key: 'closed', label: '已关闭', count: 0 }
])

const replyItem = ref(null)
const replyStatus = ref('resolved')
const replyText = ref('')
const replyLoading = ref(false)

const typeLabel = (t) => ({ suggestion: '建议', bug: 'Bug', complaint: '投诉', experience: '体验', other: '其他' }[t] || t)
const statusLabel = (s) => ({ pending: '待处理', processing: '处理中', resolved: '已处理', closed: '已关闭' }[s] || s)
const formatDate = (d) => d ? new Date(d).toLocaleString('zh-CN') : ''

const load = async () => {
  try {
    const res = await feedbackApi.getFeedbackList({ page: page.value, size: 20, status: filterStatus.value || undefined })
    if (res.code === 0) {
      list.value = res.data.items || []
      totalPages.value = res.data.totalPages || 1
    }
  } catch (_) {}
}

const loadStats = async () => {
  try {
    const res = await feedbackApi.getFeedbackStats()
    if (res.code === 0) {
      for (const s of stats) s.count = res.data[s.key] || 0
    }
  } catch (_) {}
}

const openReply = (item) => {
  replyItem.value = item
  replyStatus.value = item.status === 'pending' ? 'processing' : 'resolved'
  replyText.value = ''
}

const doReply = async () => {
  if (!replyItem.value) return
  replyLoading.value = true
  try {
    const res = await feedbackApi.handleFeedback(replyItem.value.id, replyStatus.value, replyText.value)
    if (res.code === 0) { ElMessage.success('处理成功'); replyItem.value = null; load(); loadStats() }
    else ElMessage.error(res.msg)
  } catch (_) { ElMessage.error('操作失败') }
  finally { replyLoading.value = false }
}

const doDelete = async (id) => {
  if (!confirm('确定删除此反馈？')) return
  try {
    const res = await feedbackApi.adminDeleteFeedback(id)
    if (res.code === 0) { ElMessage.success('已删除'); load(); loadStats() }
    else ElMessage.error(res.msg)
  } catch (_) { ElMessage.error('删除失败') }
}

const parseUrls = (images) => { if (!images) return []; try { return JSON.parse(images) } catch { return images.split(',') } }
const previewImage = (url) => window.open(url, '_blank')

watch(filterStatus, () => { page.value = 1; load() })
onMounted(() => { load(); loadStats() })
</script>

<style scoped>
.stats-row { display: flex; gap: 12px; margin-bottom: 16px; }
.stat-card { flex: 1; background: white; border-radius: 10px; padding: 14px; text-align: center; cursor: pointer; border: 2px solid transparent; transition: all 0.3s; }
.stat-card:hover { border-color: #ff69b4; }
.stat-card.active { border-color: #ff69b4; background: #fff0f5; }
.stat-num { font-size: 28px; font-weight: bold; color: #ff69b4; }
.stat-label { font-size: 13px; color: #888; margin-top: 4px; }

.feedback-table { background: white; border-radius: 10px; overflow: hidden; }
table { width: 100%; border-collapse: collapse; }
th { background: #fafafa; padding: 10px 12px; text-align: left; font-size: 13px; color: #666; border-bottom: 2px solid #eee; }
td { padding: 10px 12px; border-bottom: 1px solid #f5f5f5; font-size: 13px; vertical-align: top; }
.empty-row { text-align: center; color: #999; padding: 40px !important; }
.content-cell { max-width: 300px; word-break: break-all; line-height: 1.4; }
.images-cell { display: flex; gap: 4px; margin-top: 4px; }
.mini-thumb { width: 40px; height: 40px; object-fit: cover; border-radius: 4px; cursor: pointer; border: 1px solid #eee; }
.type-tag { padding: 2px 8px; border-radius: 10px; font-size: 11px; background: #fff0f5; color: #ff69b4; }
.status-tag { padding: 2px 10px; border-radius: 10px; font-size: 11px; }
.status-tag.pending { background: #f0f0f0; color: #666; }
.status-tag.processing { background: #fff3e0; color: #ff9800; }
.status-tag.resolved { background: #e8f5e9; color: #2e7d32; }
.status-tag.closed { background: #e0e0e0; color: #555; }
.action-row { display: flex; gap: 6px; }
.btn { padding: 4px 12px; border: none; border-radius: 4px; cursor: pointer; font-size: 12px; color: white; }
.btn.primary { background: #ff69b4; }
.btn.danger { background: #f44336; }
.pagination { display: flex; justify-content: center; gap: 16px; align-items: center; margin-top: 16px; }
.pagination button { padding: 6px 16px; border: 1px solid #ddd; border-radius: 6px; background: white; cursor: pointer; }
.pagination button:disabled { opacity: 0.4; cursor: not-allowed; }

.dialog-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 4000; }
.dialog-box { background: white; border-radius: 12px; padding: 24px; width: 500px; max-height: 80vh; overflow-y: auto; }
.dialog-box h3 { margin: 0 0 4px; color: #f1177d; }
.dialog-meta { color: #999; font-size: 12px; margin: 0 0 12px; }
.dialog-content { background: #fafafa; padding: 12px; border-radius: 8px; margin-bottom: 14px; max-height: 150px; overflow-y: auto; font-size: 14px; }
.form-group { margin-bottom: 12px; }
.form-group label { display: block; margin-bottom: 4px; color: #666; font-size: 13px; }
.form-group select, .form-group textarea { width: 100%; padding: 8px; border: 1px solid #ddd; border-radius: 6px; box-sizing: border-box; }
.dialog-actions { display: flex; gap: 12px; justify-content: flex-end; }
.dialog-actions button { padding: 8px 20px; border: none; border-radius: 6px; cursor: pointer; }
.dialog-actions button:first-child { background: #f0f0f0; color: #666; }
.dialog-actions button:last-child { background: #ff69b4; color: white; }
.dialog-actions button:last-child:disabled { opacity: 0.5; }
</style>
