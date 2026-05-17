<template>
  <div class="content-section sakura-bg">
    <h2>🌸 用户设置</h2>
    <button class="logout-btn" @click="logout">退出登录</button>
    <button v-if="isAdmin" type="button" class="admin-btn" @click="goToAdmin">管理员入口</button>
    <div class="settings-container">
      <div class="settings-tabs">
        <button v-for="tab in tabs" :key="tab.id" :class="{ active: activeTab === tab.id }" @click="activeTab = tab.id">
          {{ tab.name }}
        </button>
      </div>

      <div class="settings-content">
        <!-- 账户设置 -->
        <div v-if="activeTab === 'account'" class="account-settings">
          <div class="setting-item">
            <div class="setting-label">用户名</div>
            <div class="setting-value">{{ user.username }}</div>
          </div>
          <div class="setting-item">
            <div class="setting-label">手机号码</div>
            <div class="setting-value">{{ user.phone || '未绑定' }}</div>
          </div>
          <div class="setting-item">
            <div class="setting-label">邮箱地址</div>
            <div class="setting-value">{{ user.email }}</div>
          </div>
          <div class="setting-item">
            <div class="setting-label">密码</div>
            <div class="setting-value">****************</div>
            <button class="setting-action" @click="editPasswordBtn">修改密码</button>
          </div>
        </div>

        <!-- 修改密码表单 用element-plus v-form-->
        <el-form v-if="showPasswordForm" ref="passwordFormRef" class="password-form" :model="passwordForm"
          :rules="passwordRules">
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="passwordForm.phone" placeholder="请输入与账号相同的手机号"></el-input>
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input v-model="passwordForm.newPassword" type="password" placeholder="只能是数字,字母,下划线,点"></el-input>
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmNewPassword">
            <el-input v-model="passwordForm.confirmNewPassword" type="password" placeholder="请确认新密码"></el-input>
          </el-form-item>
          <el-form-item label="验证码" prop="code">
            <el-input v-model="passwordForm.code" placeholder="请输入验证码"></el-input>
          </el-form-item>
          <el-button class="getCodeBtn" type="primary" @click="getCode" :disabled="isCounting">
           {{ countdown > 0 ? `${countdown}s后重发` : '获取验证码' }}</el-button>
          <el-form-item>
            <el-button type="primary" @click="submitPasswordForm()">修改</el-button>
            <el-button type="danger" @click="resetPasswordForm()">重置</el-button>
            <el-button type="info" @click="cancelForm()">关闭</el-button>
          </el-form-item>
        </el-form>


        <!-- 隐私设置 -->
        <div v-if="activeTab === 'privacy'" class="privacy-settings">
          <div class="setting-item">
            <div class="setting-label">显示手机号</div>
            <div class="setting-value">
              <label class="switch">
                <input type="checkbox" v-model="privacySettings.showPhone">
                <span class="slider round"></span>
              </label>
            </div>
          </div>
          <div class="setting-item">
            <div class="setting-label">显示邮箱</div>
            <div class="setting-value">
              <label class="switch">
                <input type="checkbox" v-model="privacySettings.showEmail">
                <span class="slider round"></span>
              </label>
            </div>
          </div>
        </div>

        <!-- 通知设置 -->
        <div v-if="activeTab === 'notifications'" class="notification-settings">
          <div class="setting-item">
            <div class="setting-label">系统消息</div>
            <div class="setting-value">
              <label class="switch">
                <input type="checkbox" v-model="notificationSettings.systemMessages">
                <span class="slider round"></span>
              </label>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useUserInfoStore } from '@/stores/userInfo'
import { ElMessage, ElMessageBox } from 'element-plus';
import { useRouter } from 'vue-router'
import { useUpdatePasswordService, sendSMSCodeService, userLogoutService } from '@/api/user'
import { useTokenStore } from '@/stores/token'


const tokenStore = useTokenStore()
const userStore = useUserInfoStore()
const router = useRouter()
const isAdmin = localStorage.getItem('isAdmin') === 'true'
// 密码表单数据
const isCounting = ref(false)
const countdown = ref(0)
const timer = ref(null)
const showPasswordForm = ref(false);
const passwordForm = ref({
  phone: "",  // 确保初始化
  code: "",
  newPassword: "",
  confirmNewPassword: ""
});
const passwordFormRef = ref(null);// 密码表单引用
const passwordRules = ref({
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3456789]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' } // 使用 pattern 而非 regexp
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '请输入6位数字验证码', trigger: 'blur' } // 简化校验
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    {
      pattern: /^[a-zA-Z0-9_.]{5,16}$/, // 直接使用 pattern
      message: '密码需为5-16位字母、数字、下划线或小数点',
      trigger: 'blur'
    }
  ],
  confirmNewPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.value.newPassword) {
          callback(new Error('两次输入密码不一致'));
        } else {
          callback();
        }
      },
      trigger: 'blur'
    }
  ]
});

// 编辑密码按钮点击事件
const editPasswordBtn = () => {
  showPasswordForm.value = true
}
//获取验证码
const getCode = async () => {
  const accountPhone = userStore.currentUser.phone
  if (isCounting.value) return
  if (!passwordForm.value.phone) {
    ElMessage.error('请输入手机号')
    return
  }
  if (!/^1[3456789]\d{9}$/.test(passwordForm.value.phone)) {
    ElMessage.error('请输入正确的手机号')
    return
  }
  if (accountPhone !== passwordForm.value.phone) {
    ElMessage.error('请输入与注册账号相同的手机号')
    return
  }
  // 调用sendSMSCodeService接口,发送验证码请求
  try {
    const result = await sendSMSCodeService(passwordForm.value.phone, 'reset')
    if (result.code === 0) {
      ElMessage.success('验证码发送成功')
       // 开始倒计时
      isCounting.value = true
      countdown.value = 60
      timer.value = setInterval(() => {
        countdown.value--
        if (countdown.value <= 0) {
          clearInterval(timer.value)
          isCounting.value = false
        }
      }, 1000)
    } else {
      ElMessage.error(result.msg || '验证码发送失败')
    }
  }catch (error) {
    ElMessage.error(error.message || '验证码发送失败')
  }
}

//提交密码表单
const submitPasswordForm = () => {
  passwordFormRef.value.validate((valid) => {
    if (valid) {
      // 提交逻辑,调用useUpdatePasswordService
      useUpdatePasswordService(passwordForm.value).then(res => {
        if (res.code === 0) {
          ElMessage.success('密码更新成功')
          // 重置表单
          resetPasswordForm()
          // 关闭表单
          showPasswordForm.value = false
          // 移除用户信息
          userStore.removeInfo()
          // 移除token
          tokenStore.removeToken()
          // 跳转到登录页面
          router.push('/login')
          // 刷新页面
          window.location.reload()

        } else {
          ElMessage.error(res.msg || '密码更新失败')
        }
      })
    } else {
      console.log('校验失败');
      ElMessage.error('请检查表单输入')
    }
  });
};


//重置密码表单
const resetPasswordForm = () => {
  passwordForm.value = {
    phone: '',
    code: '',
    newPassword: '',
    confirmNewPassword: ''
  }
}

// 取消编辑
const cancelForm = () => {
  // 在这里处理表单重置的逻辑
  //判断是否为空
  if (Object.values(passwordForm.value).every(value => !value)) {
    // 关闭表单
    showPasswordForm.value = false
    return
  }
  ElMessageBox.confirm('取消将丢失未编辑信息，是否继续？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    // 重置表单
    resetPasswordForm()
    // 关闭表单
    showPasswordForm.value = false
  }).catch(() => {
    // 取消操作
    ElMessage({
      type: 'info',
      message: '已取消'
    })
  })
}

const activeTab = ref('account');
const tabs = ref([
  { id: 'account', name: '账户设置' },
  { id: 'privacy', name: '隐私设置' },
  { id: 'notifications', name: '通知设置' }
]);
const user = ref({
  email: userStore.currentUser.email || '未绑定',
  phone: userStore.currentUser.phone || '未绑定',
  username: userStore.currentUser.username || '未知用户'
});
const privacySettings = ref({
  showPhone: false,
  showEmail: false
});
const notificationSettings = ref({
  systemMessages: true
});

const goToAdmin = () => {
  router.push('/admin')
}
const logout = () => {
  if (!userStore.currentUser?.id || userStore.currentUser.status === 'offline' || userStore.currentUser.status === '') {
    ElMessage.error('您未登录，无法登出')
    return
  }
  ElMessageBox.confirm('您确定要退出登录吗？', '温馨提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    // 1. 先调后端登出接口（此时Token还在，不会被拦截器拒绝）
    await userLogoutService().catch(e => {
      console.error('后端登出请求失败（不影响前端登出）:', e)
    })

    // 2. 再清理前端状态
    await userStore.logout()

    ElMessage.success('您已成功登出')
    await router.replace('/login')
    // 刷新页面
    window.location.reload()
  }).catch(() => {
    ElMessage.info('已取消登出')
  })
}

</script>

<style scoped>
.logout-btn {
  background-color: #74d4e0;
  color: rgb(224, 70, 122);
  font-family: Impact, Haettenschweiler, 'Arial Narrow Bold', sans-serif;
  font-size: 20px;
  font-style: italic;
  font-weight: bold;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  transition: background-color 0.3s;
  position: absolute;
  right: 30px;
}

.logout-btn:hover {
  background-color: #d3a3dd;
}

.admin-btn {
  background-color: #74d4e0;
  color: rgb(224, 70, 122);
  font-family: Impact, Haettenschweiler, 'Arial Narrow Bold', sans-serif;
  font-size: 20px;
  font-style: italic;
  font-weight: bold;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  transition: background-color 0.3s;
  position: absolute;
  top: 90px;
  right: 20px;
}

.admin-btn:hover {
  background-color: #d3a3dd;
}

.settings-container {
  margin-left: 30px;
  width: 90%;
  display: flex;
  flex-direction: column;

}

.settings-tabs {
  display: flex;
  border-bottom: 1px solid #ffcce0;
  margin-bottom: 20px;
}

.settings-tabs button {
  padding: 10px 20px;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 16px;
  color: #666;
  position: relative;
  transition: all 0.3s;
}

.settings-tabs button.active {
  color: #e83e8c;
  font-weight: bold;
}

.settings-tabs button.active:after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  width: 100%;
  height: 3px;
  background-color: #e83e8c;
}

.setting-item {
  display: flex;
  align-items: center;
  padding: 15px 0;
  border-bottom: 1px solid #ffe6ee;
}

.setting-label {
  flex: 1;
  font-weight: 500;
  color: #555;
}

.setting-value {
  flex: 2;
  color: #777;
}

.setting-action {
  background-color: #ff85a2;
  color: white;
  border: none;
  padding: 8px 15px;
  border-radius: 20px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.setting-action:hover {
  background-color: #e83e8c;
}

/* 开关样式 */
.switch {
  position: relative;
  display: inline-block;
  width: 50px;
  height: 24px;
}

.switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #ccc;
  transition: .4s;
  border-radius: 24px;
}

.slider:before {
  position: absolute;
  content: "";
  height: 16px;
  width: 16px;
  left: 4px;
  bottom: 4px;
  background-color: white;
  transition: .4s;
  border-radius: 50%;
}

input:checked+.slider {
  background-color: #ff85a2;
}

input:checked+.slider:before {
  transform: translateX(26px);
}

/* 修改密码表单 */
.password-form {
  margin: -520px auto;
  width: 600px;
  height: 850px;
  border: 2px solid #35bdcf;
  box-shadow: 0 15px 25px rgba(96, 230, 56, 0.7);
  border-radius: 20px;
  background-image: url('../assets/images/changePasswordBG.jpg');
  background-size: cover;
  opacity: 0.9;
  z-index: 1000;
}

.el-form-item {
  position: relative;
  margin: 30px auto;
  width: 200px;
  top: 430px;
  left: 120px;
}

.password-form .el-form-item .el-input__inner {
  border-radius: 25px;
}

.userInfo-form .el-form-item .el-input__inner:focus {
  border-color: #ff69b4;
}

.getCodeBtn {
  position: relative;
  top: 410px;
  left: 470px;
  font-size: 8px;
  margin-left: -10px;
}

.getCodeBtn :hover {
  background-color: #ff69b4;
}

.password-form button {
  background-color: #ff85a2;
  color: white;
  border: none;
  padding: 8px 15px;
  border-radius: 20px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.password-form button:hover {
  background-color: #e83e8c;
}
</style>