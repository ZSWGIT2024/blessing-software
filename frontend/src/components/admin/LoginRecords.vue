<template>
  <div class="login-records">
    <!-- 筛选栏 -->
    <div class="filter-bar">
      <div class="search-section">
        <input type="text" placeholder="用户ID" v-model="filters.userId" style="width:100px">
        <select v-model="filters.loginType">
          <option value="">全部方式</option>
          <option value="password">密码登录</option>
          <option value="code">验证码登录</option>
        </select>
        <select v-model="filters.status">
          <option value="">全部状态</option>
          <option value="success">成功</option>
          <option value="fail">失败</option>
        </select>
        <button @click="fetchRecords" :disabled="loading">查询</button>
        <button @click="resetFilters" class="secondary">重置</button>
        <button @click="showAbnormalCheck" class="warning-btn">异常检测</button>
      </div>
    </div>

    <!-- 登录记录表格 -->
    <div class="table-container">
      <table class="record-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>用户ID</th>
            <th>用户名</th>
            <th>手机号</th>
            <th>方式</th>
            <th>IP</th>
            <th>地点</th>
            <th>浏览器</th>
            <th>系统</th>
            <th>状态</th>
            <th>失败原因</th>
            <th>登录时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="rec in recordList" :key="rec.id">
            <td>{{ rec.id }}</td>
            <td>{{ rec.userId }}</td>
            <td>{{ rec.username }}</td>
            <td>{{ rec.phone ? rec.phone.substring(0,3) + '****' + rec.phone.substring(7) : '-' }}</td>
            <td><span :class="'login-type-badge type-' + rec.loginType">{{ rec.loginType === 'password' ? '密码' : '验证码' }}</span></td>
            <td>{{ rec.loginIp }}</td>
            <td>{{ rec.loginLocation }}</td>
            <td>{{ rec.browser }}</td>
            <td>{{ rec.os }}</td>
            <td><span :class="'status-badge status-' + rec.status">{{ rec.status === 'success' ? '成功' : '失败' }}</span></td>
            <td class="fail-reason">{{ rec.failReason || '-' }}</td>
            <td>{{ formatDate(rec.loginTime) }}</td>
          </tr>
        </tbody>
      </table>

      <div v-if="loading" class="loading-overlay">
        <div class="spinner"></div>
        <p>加载中...</p>
      </div>
      <div v-if="!loading && recordList.length === 0" class="empty-state">
        <p>暂无登录记录</p>
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination" v-if="pagination.total > 0">
      <button :disabled="pagination.page === 1" @click="changePage(pagination.page - 1)">上一页</button>
      <span class="page-info">第 {{ pagination.page }} / {{ pagination.totalPages }} 页 ({{ pagination.total }} 条)</span>
      <button :disabled="pagination.page >= pagination.totalPages" @click="changePage(pagination.page + 1)">下一页</button>
    </div>

    <!-- 异常检测弹窗 -->
    <div v-if="showAbnormalModal" class="modal-overlay">
      <div class="modal-content modal-sm">
        <div class="modal-header">
          <h3>异常登录检测</h3>
          <button @click="showAbnormalModal = false" class="close-btn">×</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>用户ID</label>
            <input type="text" v-model="abnormalUserId" placeholder="请输入用户ID">
          </div>
          <div class="modal-footer">
            <button @click="showAbnormalModal = false" class="secondary">取消</button>
            <button @click="checkAbnormal" :disabled="abnormalChecking" class="primary-btn">检测</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 异常结果弹窗 -->
    <div v-if="showAbnormalResultModal" class="modal-overlay">
      <div class="modal-content">
        <div class="modal-header">
          <h3>异常登录结果</h3>
          <button @click="showAbnormalResultModal = false" class="close-btn">×</button>
        </div>
        <div class="modal-body">
          <div v-if="!abnormalResults || abnormalResults.length === 0" class="empty-state">
            <p>未发现异常登录</p>
          </div>
          <table v-else class="record-table">
            <thead>
              <tr><th>IP</th><th>地点</th><th>登录时间</th><th>风险</th></tr>
            </thead>
            <tbody>
              <tr v-for="(r, i) in abnormalResults" :key="i">
                <td>{{ r.loginIp }}</td>
                <td>{{ r.loginLocation }}</td>
                <td>{{ formatDate(r.loginTime) }}</td>
                <td><span class="risk-badge">{{ r.risk || '异常' }}</span></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { adminGetLoginRecordsService, adminGetAbnormalLoginsService } from '@/api/adminLog'

const loading = ref(false)
const recordList = ref([])
const showAbnormalModal = ref(false)
const showAbnormalResultModal = ref(false)
const abnormalUserId = ref('')
const abnormalChecking = ref(false)
const abnormalResults = ref([])

const filters = reactive({ userId: '', loginType: '', status: '' })
const pagination = reactive({ page: 1, size: 20, total: 0, totalPages: 0 })

onMounted(() => fetchRecords())

async function fetchRecords() {
  loading.value = true
  try {
    const params = { page: pagination.page, size: pagination.size, ...filters }
    Object.keys(params).forEach(k => { if (params[k] === '' || params[k] === undefined) delete params[k] })
    const res = await adminGetLoginRecordsService(params)
    if (res.code === 0 && res.data) {
      recordList.value = res.data.records || res.data.list || []
      pagination.total = res.data.total || 0
      pagination.totalPages = res.data.totalPages || Math.ceil(pagination.total / pagination.size)
    }
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.userId = ''; filters.loginType = ''; filters.status = ''
  pagination.page = 1
  fetchRecords()
}

function changePage(page) {
  if (page < 1 || page > pagination.totalPages) return
  pagination.page = page
  fetchRecords()
}

function showAbnormalCheck() {
  abnormalUserId.value = ''
  showAbnormalModal.value = true
}

async function checkAbnormal() {
  if (!abnormalUserId.value) { alert('请输入用户ID'); return }
  abnormalChecking.value = true
  try {
    const res = await adminGetAbnormalLoginsService(abnormalUserId.value)
    if (res.code === 0) {
      abnormalResults.value = res.data || []
      showAbnormalModal.value = false
      showAbnormalResultModal.value = true
    } else {
      alert(res.msg || '检测失败')
    }
  } catch { alert('检测失败') } finally { abnormalChecking.value = false }
}

function formatDate(date) {
  if (!date) return '--'
  const d = new Date(date)
  return d.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}
</script>

<style scoped>
.login-records { padding: 0; }

/* 筛选栏 */
.filter-bar { background: white; padding: 16px; border-radius: 8px; margin-bottom: 16px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.search-section { display: flex; gap: 10px; flex-wrap: wrap; align-items: center; }
.search-section input, .search-section select { padding: 6px 10px; border: 1px solid #ddd; border-radius: 4px; font-size: 13px; }
.search-section button { padding: 7px 14px; border: none; border-radius: 4px; cursor: pointer; font-size: 13px; background: #1890ff; color: white; }
.search-section button.secondary { background: #f5f5f5; color: #666; }
.search-section button.warning-btn { background: #fff7e6; color: #fa8c16; border: 1px solid #ffd591; }
.search-section button:disabled { opacity: 0.6; cursor: not-allowed; }

/* 表格 */
.table-container { background: white; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); min-height: 300px; position: relative; overflow: hidden; }
.record-table { width: 100%; border-collapse: collapse; }
.record-table th, .record-table td { padding: 10px 12px; text-align: left; border-bottom: 1px solid #f0f0f0; font-size: 13px; }
.record-table th { background: #fafafa; font-weight: 600; color: #666; }
.record-table tbody tr:hover { background: #f5f7fa; }

/* 徽章 */
.login-type-badge { padding: 2px 6px; border-radius: 3px; font-size: 11px; }
.type-password { background: #e6f7ff; color: #1890ff; }
.type-code { background: #f6ffed; color: #52c41a; }
.status-badge { padding: 2px 8px; border-radius: 10px; font-size: 11px; }
.status-success { background: #f6ffed; color: #52c41a; }
.status-fail { background: #fff1f0; color: #f5222d; }
.fail-reason { color: #f5222d; max-width: 120px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.risk-badge { padding: 2px 6px; border-radius: 3px; background: #fff1f0; color: #f5222d; font-size: 11px; }

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
.modal-sm { max-width: 420px; }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; border-bottom: 1px solid #eee; }
.modal-header h3 { margin: 0; }
.close-btn { background: none; border: none; font-size: 24px; cursor: pointer; color: #999; }
.modal-body { padding: 20px; }
.form-group { margin-bottom: 14px; }
.form-group label { display: block; margin-bottom: 6px; font-weight: 500; color: #333; }
.form-group input { width: 100%; padding: 8px 10px; border: 1px solid #ddd; border-radius: 4px; font-size: 14px; box-sizing: border-box; }
.modal-footer { display: flex; justify-content: flex-end; gap: 10px; padding-top: 16px; border-top: 1px solid #eee; }
.modal-footer button { padding: 8px 16px; border: none; border-radius: 4px; cursor: pointer; font-size: 13px; }
.modal-footer button.primary-btn { background: #1890ff; color: white; }
.modal-footer button.primary-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.modal-footer button.secondary { background: #f5f5f5; color: #666; }
</style>
