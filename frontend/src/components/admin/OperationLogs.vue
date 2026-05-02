<template>
  <div class="operation-logs">
    <!-- 筛选栏 -->
    <div class="filter-bar">
      <div class="search-section">
        <input type="text" placeholder="模块名称" v-model="filters.module" @keyup.enter="fetchLogs">
        <select v-model="filters.type">
          <option value="">全部类型</option>
          <option v-for="t in logTypes" :key="t" :value="t">{{ t }}</option>
        </select>
        <input type="text" placeholder="用户ID" v-model="filters.userId" style="width:100px">
        <select v-model="filters.status">
          <option value="">全部状态</option>
          <option value="success">成功</option>
          <option value="fail">失败</option>
        </select>
        <button @click="fetchLogs" :disabled="loading">查询</button>
        <button @click="resetFilters" class="secondary">重置</button>
        <button @click="cleanLogs" class="danger-btn">清理旧日志</button>
      </div>
    </div>

    <!-- 日志表格 -->
    <div class="table-container">
      <table class="log-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>模块</th>
            <th>操作</th>
            <th>类型</th>
            <th>操作人</th>
            <th>IP</th>
            <th>耗时</th>
            <th>状态</th>
            <th>时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="log in logList" :key="log.id">
            <td>{{ log.id }}</td>
            <td>{{ log.module }}</td>
            <td>{{ log.operation }}</td>
            <td><span :class="'type-badge type-' + (log.type || '').toLowerCase()">{{ log.type }}</span></td>
            <td>{{ log.username }}</td>
            <td>{{ log.requestIp }}</td>
            <td :class="{ 'slow-query': log.duration > 2000 }">{{ log.duration ? log.duration + 'ms' : '-' }}</td>
            <td><span :class="'status-badge status-' + log.status">{{ log.status === 'success' ? '成功' : '失败' }}</span></td>
            <td>{{ formatDate(log.createTime) }}</td>
            <td><button @click="showDetail(log.id)" class="btn-detail">详情</button></td>
          </tr>
        </tbody>
      </table>

      <div v-if="loading" class="loading-overlay">
        <div class="spinner"></div>
        <p>加载中...</p>
      </div>
      <div v-if="!loading && logList.length === 0" class="empty-state">
        <p>暂无操作日志</p>
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination" v-if="pagination.total > 0">
      <button :disabled="pagination.page === 1" @click="changePage(pagination.page - 1)">上一页</button>
      <span class="page-info">第 {{ pagination.page }} / {{ pagination.totalPages }} 页 ({{ pagination.total }} 条)</span>
      <button :disabled="pagination.page >= pagination.totalPages" @click="changePage(pagination.page + 1)">下一页</button>
    </div>

    <!-- 详情模态框 -->
    <div v-if="showDetailModal" class="modal-overlay">
      <div class="modal-content">
        <div class="modal-header">
          <h3>操作日志详情</h3>
          <button @click="showDetailModal = false" class="close-btn">×</button>
        </div>
        <div class="modal-body" v-if="logDetail">
          <div class="detail-grid">
            <div class="detail-item"><label>ID</label><span>{{ logDetail.id }}</span></div>
            <div class="detail-item"><label>模块</label><span>{{ logDetail.module }}</span></div>
            <div class="detail-item"><label>操作</label><span>{{ logDetail.operation }}</span></div>
            <div class="detail-item"><label>类型</label><span>{{ logDetail.type }}</span></div>
            <div class="detail-item"><label>操作人</label><span>{{ logDetail.username }}</span></div>
            <div class="detail-item"><label>用户ID</label><span>{{ logDetail.userId }}</span></div>
            <div class="detail-item"><label>请求IP</label><span>{{ logDetail.requestIp }}</span></div>
            <div class="detail-item"><label>请求方法</label><span>{{ logDetail.method }}</span></div>
            <div class="detail-item" style="grid-column:1/-1"><label>请求URL</label><span>{{ logDetail.requestUrl }}</span></div>
            <div class="detail-item" style="grid-column:1/-1">
              <label>请求参数</label>
              <pre class="json-block">{{ formatJson(logDetail.requestParams) }}</pre>
            </div>
            <div class="detail-item" style="grid-column:1/-1">
              <label>响应结果</label>
              <pre class="json-block">{{ formatJson(logDetail.responseResult) }}</pre>
            </div>
            <div class="detail-item"><label>耗时</label><span>{{ logDetail.duration ? logDetail.duration + 'ms' : '-' }}</span></div>
            <div class="detail-item"><label>状态</label><span :class="logDetail.status === 'success' ? 'text-success' : 'text-danger'">{{ logDetail.status }}</span></div>
            <div class="detail-item" style="grid-column:1/-1" v-if="logDetail.errorMsg">
              <label>错误信息</label><span class="text-danger">{{ logDetail.errorMsg }}</span>
            </div>
            <div class="detail-item"><label>时间</label><span>{{ formatDate(logDetail.createTime) }}</span></div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import {
  adminGetOperationLogsService,
  adminGetOperationLogDetailService,
  adminCleanOperationLogsService
} from '@/api/adminLog'

const logTypes = ['LOGIN', 'LOGOUT', 'INSERT', 'UPDATE', 'DELETE', 'SELECT', 'UPLOAD', 'DOWNLOAD', 'OTHER']

const loading = ref(false)
const logList = ref([])
const showDetailModal = ref(false)
const logDetail = ref(null)

const filters = reactive({ module: '', type: '', userId: '', status: '' })
const pagination = reactive({ page: 1, size: 20, total: 0, totalPages: 0 })

onMounted(() => fetchLogs())

async function fetchLogs() {
  loading.value = true
  try {
    const params = { page: pagination.page, size: pagination.size, ...filters }
    Object.keys(params).forEach(k => { if (params[k] === '' || params[k] === undefined) delete params[k] })
    const res = await adminGetOperationLogsService(params)
    if (res.code === 0 && res.data) {
      logList.value = res.data.logs || []
      pagination.total = res.data.total || 0
      pagination.totalPages = res.data.totalPages || Math.ceil(pagination.total / pagination.size)
    }
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.module = ''; filters.type = ''; filters.userId = ''; filters.status = ''
  pagination.page = 1
  fetchLogs()
}

function changePage(page) {
  if (page < 1 || page > pagination.totalPages) return
  pagination.page = page
  fetchLogs()
}

async function showDetail(id) {
  try {
    const res = await adminGetOperationLogDetailService(id)
    if (res.code === 0) {
      logDetail.value = res.data
      showDetailModal.value = true
    }
  } catch { /* ignore */ }
}

async function cleanLogs() {
  const date = prompt('请输入清理截止日期（如：2026-01-01）')
  if (!date) return
  try {
    const res = await adminCleanOperationLogsService(date)
    if (res.code === 0) {
      alert(`已清理 ${res.data || 0} 条日志`)
      fetchLogs()
    } else {
      alert(res.msg || '清理失败')
    }
  } catch { alert('清理失败') }
}

function formatDate(date) {
  if (!date) return '--'
  const d = new Date(date)
  return d.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

function formatJson(str) {
  if (!str) return '-'
  try { return JSON.stringify(JSON.parse(str), null, 2) } catch { return str }
}
</script>

<style scoped>
.operation-logs { padding: 0; }

/* 筛选栏 */
.filter-bar { background: white; padding: 16px; border-radius: 8px; margin-bottom: 16px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.search-section { display: flex; gap: 10px; flex-wrap: wrap; align-items: center; }
.search-section input, .search-section select { padding: 6px 10px; border: 1px solid #ddd; border-radius: 4px; font-size: 13px; }
.search-section input { width: 140px; }
.search-section button { padding: 7px 14px; border: none; border-radius: 4px; cursor: pointer; font-size: 13px; background: #1890ff; color: white; }
.search-section button.secondary { background: #f5f5f5; color: #666; }
.search-section button.danger-btn { background: #fff1f0; color: #f5222d; border: 1px solid #ffccc7; }
.search-section button:disabled { opacity: 0.6; cursor: not-allowed; }

/* 表格 */
.table-container { background: white; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); min-height: 300px; position: relative; overflow: hidden; }
.log-table { width: 100%; border-collapse: collapse; }
.log-table th, .log-table td { padding: 10px 12px; text-align: left; border-bottom: 1px solid #f0f0f0; font-size: 13px; }
.log-table th { background: #fafafa; font-weight: 600; color: #666; }
.log-table tbody tr:hover { background: #f5f7fa; }
.btn-detail { padding: 4px 8px; border: 1px solid #1890ff; border-radius: 3px; background: white; color: #1890ff; cursor: pointer; font-size: 12px; }

/* 徽章 */
.type-badge { padding: 2px 6px; border-radius: 3px; font-size: 11px; }
.type-login { background: #f6ffed; color: #52c41a; }
.type-logout { background: #e6f7ff; color: #1890ff; }
.type-delete { background: #fff1f0; color: #f5222d; }
.type-update { background: #fff7e6; color: #fa8c16; }
.type-insert { background: #f6ffed; color: #52c41a; }
.type-select { background: #f5f5f5; color: #666; }
.status-badge { padding: 2px 8px; border-radius: 10px; font-size: 11px; }
.status-success { background: #f6ffed; color: #52c41a; }
.status-fail { background: #fff1f0; color: #f5222d; }
.slow-query { color: #f5222d; font-weight: 600; }

/* 分页 */
.pagination { display: flex; align-items: center; justify-content: center; gap: 16px; margin-top: 16px; padding: 12px; background: white; border-radius: 8px; }
.pagination button { padding: 6px 14px; border: 1px solid #ddd; background: white; border-radius: 4px; cursor: pointer; }
.pagination button:disabled { opacity: 0.5; cursor: not-allowed; }
.page-info { color: #666; font-size: 13px; }

/* 加载/空状态 */
.loading-overlay { display: flex; flex-direction: column; align-items: center; padding: 40px; }
.spinner { width: 36px; height: 36px; border: 3px solid #f3f3f3; border-top: 3px solid #1890ff; border-radius: 50%; animation: spin 1s linear infinite; }
@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }
.empty-state { text-align: center; padding: 60px; color: #999; }

/* 模态框 */
.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 2000; }
.modal-content { background: white; border-radius: 8px; width: 90%; max-width: 700px; max-height: 85vh; overflow-y: auto; }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; border-bottom: 1px solid #eee; }
.modal-header h3 { margin: 0; }
.close-btn { background: none; border: none; font-size: 24px; cursor: pointer; color: #999; }
.modal-body { padding: 20px; }
.detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.detail-item { display: flex; padding: 6px 0; }
.detail-item label { width: 80px; color: #666; font-weight: 500; flex-shrink: 0; }
.detail-item span { color: #333; word-break: break-all; }
.json-block { max-height: 160px; overflow: auto; background: #f5f7fa; padding: 8px; border-radius: 4px; font-size: 12px; margin: 4px 0 0 80px; white-space: pre-wrap; word-break: break-all; }
.text-success { color: #52c41a; }
.text-danger { color: #f5222d; }
</style>
