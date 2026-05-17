<template>
  <div class="forgot-password-modal">
    <div class="modal-content">
      <button class="close-btn" @click="$emit('close')">×</button>
      <h3>找回密码</h3>

      <div class="steps">
        <div class="step" :class="{ active: currentStep === 1 }">1. 验证身份</div>
        <div class="step" :class="{ active: currentStep === 2 }">2. 重置密码</div>
        <div class="step" :class="{ active: currentStep === 3 }">3. 完成</div>
      </div>

      <div v-if="currentStep === 1" class="step-content">
        <div class="input-group">
          <input type="text" placeholder="请输入与账号相同的手机号或邮箱" v-model="account" @blur="validateAccount">
          <div v-if="accountError" class="error-message">{{ accountError }}</div>
        </div>

        <div class="code-group">
          <input type="text" placeholder="请输入验证码" v-model="code">
          <button class="send-btn" :disabled="isCounting || !isAccountValid" @click="sendCode">
            {{ countdown > 0 ? `${countdown}s后重发` : '获取验证码' }}
          </button>
        </div>

        <button class="next-btn" :disabled="!isAccountValid" @click="verifyCode">
          下一步
        </button>
      </div>

      <div v-if="currentStep === 2" class="step-content">
        <div class="input-group">
          <input type="password" placeholder="密码长度必须为5-16位的数字,字母,下划线,点" v-model="newPassword" @blur="validatePassword">
          <div v-if="passwordError" class="error-message">{{ passwordError }}</div>
        </div>
        <div class="input-group">
          <input type="password" placeholder="请确认新密码" v-model="confirmPassword" @blur="validateConfirmPassword">
          <div v-if="confirmPasswordError" class="error-message">{{ confirmPasswordError }}</div>
        </div>
        <button class="next-btn" @click="submitNewPassword" :disabled="!canSubmit">
          提交
        </button>
      </div>

      <div v-if="currentStep === 3" class="step-content">
        <div class="success-message">
          <i class="iconfont icon-success"></i>
          <p>密码重置成功！</p>
          <p>请使用新密码登录</p>
        </div>
        <button class="next-btn" @click="$emit('close')">完成</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onBeforeUnmount } from 'vue'
import { useUserInfoStore } from '@/stores/userInfo'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { useUpdatePasswordService, sendSMSCodeService } from '@/api/user'
import { useTokenStore } from '@/stores/token'

const userStore = useUserInfoStore()
const router = useRouter()
const tokenStore = useTokenStore()

// 响应式数据
const currentStep = ref(1)
const account = ref('')
const code = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const isCounting = ref(false)
const countdown = ref(0)
const timer = ref(null)
const accountError = ref('')
const passwordError = ref('')
const confirmPasswordError = ref('')

const isEmail = (str) => /^[\w.+-]+@[\w-]+\.[\w.]+$/.test(str)

// 计算属性
const isAccountValid = computed(() => {
  const val = account.value
  return isEmail(val) || /^1[3-9]\d{9}$/.test(val)
})
const canSubmit = computed(() => newPassword.value &&
  confirmPassword.value &&
  !passwordError.value &&
  !confirmPasswordError.value
)

// 方法
const validateAccount = () => {
  const userPhone = userStore.currentUser.phone
  const userEmail = userStore.currentUser.email
  if (!account.value) {
    accountError.value = '请输入手机号或邮箱'
  } else if (!isAccountValid.value) {
    accountError.value = '请输入正确的手机号或邮箱'
  } else if (account.value !== userPhone && account.value !== userEmail) {
    accountError.value = '输入信息与账号不匹配'
  } else {
    accountError.value = ''
  }
}

const validatePassword = () => {
  if (!newPassword.value) {
    passwordError.value = '请输入新密码'
  } else if (newPassword.value.length < 5 || newPassword.value.length > 16) {
    passwordError.value = '密码长度必须为5-16位的数字,字母,下划线,点'
  } else if (!/^[a-zA-Z0-9_.]+$/.test(newPassword.value)) {
    passwordError.value = '密码只能包含数字,字母,下划线,点'
  } else {
    passwordError.value = ''
  }
}

const validateConfirmPassword = () => {
  if (!confirmPassword.value) {
    confirmPasswordError.value = '请确认新密码'
  } else if (newPassword.value !== confirmPassword.value) {
    confirmPasswordError.value = '两次输入的密码不一致'
  } else {
    confirmPasswordError.value = ''
  }
}

const sendCode = async () => {
  if (!isAccountValid.value) return
  if (isCounting.value) return
  const accountVal = userStore.currentUser.phone || userStore.currentUser.email
  if (accountVal !== account.value) {
    ElMessage.error('请输入与注册账号相同的手机号或邮箱')
    return
  }
  try {
    const result = await sendSMSCodeService(account.value, 'reset')
    if (result.code === 0) {
      ElMessage.success(isEmail(account.value) ? '验证码已发送至邮箱' : '验证码发送成功')
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

const verifyCode = () => {
  if (!isAccountValid.value) return
  if (!code.value) {
    ElMessage.error('请输入验证码')
    return
  }
  currentStep.value = 2
}
const validateForm = () => {
  validatePassword()
  validateConfirmPassword()
  return !passwordError.value && !confirmPasswordError.value
}

const submitNewPassword = () => {
  if (!validateForm()) return
  // 调用API重置密码
  useUpdatePasswordService({
    newPassword: newPassword.value,
    confirmNewPassword: confirmPassword.value,
    code: code.value
  }).then(res => {
    if (res.code === 0) {
      ElMessage.success('密码重置成功')
      userStore.removeInfo()
      tokenStore.removeToken()
      router.push('/login')
      // 刷新页面
      window.location.reload()
    } else {
      ElMessage.error(res.msg || '密码重置失败')
      return
    }
  })
}

// 生命周期钩子
onBeforeUnmount(() => {
  if (timer.value) clearInterval(timer.value)
})
</script>

<style scoped>
.forgot-password-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(175, 235, 207, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1001;
}

.modal-content {
  position: relative;
  background: rgba(107, 215, 223, 0.8);
  padding: 30px;
  border-radius: 8px;
  width: 400px;
  max-width: 90%;
}

.close-btn {
  position: absolute;
  top: 10px;
  right: 10px;
  background: rgba(107, 215, 223, 0.8);
  border: none;
  font-size: 40px;
  cursor: pointer;
}

.close-btn:hover {
  color: #e45169;
}

.steps {
  display: flex;
  justify-content: space-between;
  margin: 20px 0;
}

.step {
  flex: 1;
  text-align: center;
  padding: 10px;
  color: #999;
  position: relative;
}

.step.active {
  color: #1890ff;
  font-weight: bold;
}

.step:not(:last-child):after {
  content: '';
  position: absolute;
  top: 50%;
  right: 0;
  width: 20px;
  height: 1px;
  background: #ddd;
}

.step-content {
  margin-top: 20px;
}

.input-group {
  margin-bottom: 15px;
}

.input-group input {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.next-btn {
  width: 100%;
  margin-top: 10px;
  padding: 10px;
  background: #1890ff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.success-message {
  text-align: center;
  margin: 20px 0;
}

.success-message .iconfont {
  font-size: 50px;
  color: #52c41a;
}

.success-message p {
  margin: 10px 0;
}

.code-group {
  position: relative;
  margin-bottom: 15px;
}

.send-btn {
  position: absolute;
  right: 0;
  top: 0;
  height: 42px;
  padding: 0 15px;
  background: #1890ff;
  color: white;
  border: none;
  border-radius: 0 4px 4px 0;
  cursor: pointer;
  font-size: 0.9rem;
}

.send-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.error-message {
  color: #ff4d4f;
  font-size: 0.8rem;
  margin-top: 5px;
  text-align: left;
}

.next-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}
</style>