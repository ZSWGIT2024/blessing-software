<template>
  <div class="auth-container" v-if="visible" ref="draggableContainer">
      <div class="box" @mousedown="startDrag">
        <div class="left"></div>
        <div class="right">
          <h4>{{ isRegister ? '注册' : '登录' }}</h4>

          <!-- 登录方式切换按钮（仅登录页面显示） -->
          <div class="login-method-toggle" v-if="!isRegister">
            <button class="method-btn" :class="{ active: loginMethod === 'password' }" @click="loginMethod = 'password'">
              密码登录
            </button>
            <button class="method-btn" :class="{ active: loginMethod === 'code' }" @click="loginMethod = 'code'">
              验证码登录
            </button>
          </div>

          <!-- 登录表单 -->
          <form @submit.prevent="submitForm" v-if="!isRegister">
            <div class="input-group">
              <input class="acc" type="text" placeholder="请输入手机号或邮箱" v-model="loginForm.account" @blur="validateAccount('login')">
              <span class="iconfont icon-zhanghao"></span>
              <div v-if="errors.login.account" class="error-message">{{ errors.login.account }}</div>
            </div>

            <div class="input-group" v-if="loginMethod === 'password'">
              <input class="acc" :type="showLoginPassword ? 'text' : 'password'" placeholder="请输入密码"
                v-model="loginForm.password">
              <span class="iconfont icon-jiesuo"></span>
              <span class="iconfont password-toggle" :class="showLoginPassword ? 'icon-kejian' : 'icon-bukejian'"
                @click="showLoginPassword = !showLoginPassword"></span>
              <div v-if="errors.login.password" class="error-message">{{ errors.login.password }}</div>
            </div>

            <!-- 图形验证码 -->
            <div class="input-group" v-if="showCaptcha">
              <input class="acc captcha-input" type="text" placeholder="请输入图形验证码" v-model="loginForm.captchaCode" maxlength="4">
              <img :src="captchaImage" @click="refreshCaptcha" class="captcha-img" title="点击刷新验证码" alt="验证码">
              <div v-if="errors.login.captcha" class="error-message">{{ errors.login.captcha }}</div>
            </div>

            <VerificationCode v-if="loginMethod === 'code'" v-model="loginForm.code" :account="loginForm.account"
              :is-login="true" @update:account="handleAccountUpdate" />

            <button class="Login" type="submit" value="登录" @click="loginBtn">登录</button>

            <UserAgreement v-model="agreementChecked" />
            <!-- <SocialLogin /> -->
          </form>

          <!-- 注册表单 -->
          <form @submit.prevent="submitForm" v-else>
            <div class="input-group">
              <input class="acc" type="text" placeholder="请输入手机号或邮箱" v-model="registerForm.account"
                @blur="validateAccount('register')">
              <span class="iconfont icon-zhanghao"></span>
              <div v-if="errors.register.account" class="error-message">{{ errors.register.account }}</div>
            </div>

            <div class="input-group">
              <input class="acc" :type="showRegisterPassword ? 'text' : 'password'" placeholder="请输入密码"
                v-model="registerForm.password" @blur="validatePassword">
              <span class="iconfont icon-jiesuo"></span>
              <span class="iconfont password-toggle" :class="showRegisterPassword ? 'icon-kejian' : 'icon-bukejian'"
                @click="showRegisterPassword = !showRegisterPassword"></span>
              <div v-if="errors.register.password" class="error-message">{{ errors.register.password }}</div>
            </div>

            <div class="input-group">
              <input class="acc" :type="showConfirmPassword ? 'text' : 'password'" placeholder="请确认密码"
                v-model="registerForm.confirmPassword" @blur="validateConfirmPassword">
              <span class="iconfont icon-jiesuo"></span>
              <span class="iconfont password-toggle" :class="showConfirmPassword ? 'icon-kejian' : 'icon-bukejian'"
                @click="showConfirmPassword = !showConfirmPassword"></span>
              <div v-if="errors.register.confirmPassword" class="error-message">{{ errors.register.confirmPassword }}</div>
            </div>
            <div class="code-group">
              <input class="acc" type="text" placeholder="请输入验证码" v-model="registerForm.code">
              <button class="send-btn" :disabled="isCounting || !isAccountValid" @click="sendCode">
                {{ countdown > 0 ? `${countdown}s后重发` : '获取验证码' }}
              </button>
              <div v-if="errors.register.code" class="error-message">{{ errors.register.code }}</div>
            </div>

            <UserAgreement v-model="agreementChecked" />
            <button class="Login" type="submit" value="注册" @click="registerBtn">注册</button>
          </form>

          <div class="fn">
            <a href="#" @click.prevent="toggleForm">{{ isRegister ? '已有账号？登录' : '注册账号' }}</a>
            <a v-if="!isRegister" href="#" @click.prevent="showForgotPassword">忘记密码？</a>
          </div>
        </div>

        <button class="control-btn close" @click="close">X</button>
        <ForgotPassword v-if="showForgotModal" @close="showForgotModal = false" />
      </div>
    </div>
  
</template>

<script setup>
import { ref, onBeforeUnmount, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserInfoStore } from '@/stores/userInfo'
import UserAgreement from './UserAgreement.vue'
import SocialLogin from './SocialLogin.vue'
import ForgotPassword from './ForgotPassword.vue'
import VerificationCode from './VerificationCode.vue'
import { userRegisterService, userLoginService, sendSMSCodeService, userLoginByCodeService, getCaptchaService } from '@/api/user'
import { ElMessage } from 'element-plus'
import { useTokenStore } from '@/stores/token'


const props = defineProps({
  visible: { type: Boolean, default: false },
  isRegister: { type: Boolean, default: false }
})

const emit = defineEmits(['update:visible', 'update:isRegister'])

const router = useRouter()
const userStore = useUserInfoStore()
const tokenStore = useTokenStore()

// 表单数据
const loginForm = ref({
  account: '',
  password: '',
  code: '',
  captchaCode: ''
})

const registerForm = ref({
  account: '',
  password: '',
  confirmPassword: '',
  code: ''
})

// UI状态
const showLoginPassword = ref(false)
const showRegisterPassword = ref(false)
const showConfirmPassword = ref(false)
const agreementChecked = ref(false)
const showForgotModal = ref(false)
const isCounting = ref(false)
const countdown = ref(0)
const timer = ref(null)
const isAccountValid = ref(false)
const loginMethod = ref('password')

// 图形验证码
const showCaptcha = ref(false)
const captchaUuid = ref('')
const captchaImage = ref('')

// 拖拽相关
const draggableContainer = ref(null)
const isDragging = ref(false)
const dragStartX = ref(0)
const dragStartY = ref(0)

// 错误信息
const errors = ref({
  login: { account: '', password: '', code: '', captcha: '' },
  register: { account: '', password: '', confirmPassword: '', code: '' }
})


// 方法
const startDrag = (e) => {
  if (e.target.className === 'box' || e.target.tagName === 'H4' ||
    e.target.classList.contains('right')) {
    isDragging.value = true
    const container = draggableContainer.value
    dragStartX.value = e.clientX - container.getBoundingClientRect().left
    dragStartY.value = e.clientY - container.getBoundingClientRect().top

    document.addEventListener('mousemove', handleDrag)
    document.addEventListener('mouseup', stopDrag)
    e.preventDefault()
  }
}

const handleDrag = (e) => {
  if (isDragging.value) {
    const container = draggableContainer.value
    container.style.left = `${e.clientX - dragStartX.value}px`
    container.style.top = `${e.clientY - dragStartY.value}px`
    container.style.transform = 'none'
  }
}

const stopDrag = () => {
  isDragging.value = false
  document.removeEventListener('mousemove', handleDrag)
  document.removeEventListener('mouseup', stopDrag)
}

/** 判断字符串是否为邮箱格式 */
const isEmail = (str) => /^[\w.+-]+@[\w-]+\.[\w.]+$/.test(str)

const close = () => {
  loginForm.value = { account: '', password: '', code: '', captchaCode: '' }
  registerForm.value = { account: '', password: '', confirmPassword: '', code: '' }
  showCaptcha.value = false
  emit('update:visible', false)
}

const registerBtn = async () => {
  if (props.isRegister) {
    if (!validateRegisterForm() || !agreementChecked.value || !registerForm.value.code) {
      ElMessage({ type: 'error', message: '信息填写有误，请检查！' })
      return
    }
    // 注册：传 account + password + code（不含 confirmPassword）
    const { confirmPassword, ...regData } = registerForm.value
    let result = await userRegisterService(regData)
    if (result.code === 0) {
      ElMessage.success(result.msg ? result.msg : '注册成功')
      toggleForm()
    } else {
      ElMessage.error(result.msg ? result.msg : '注册失败')
    }
  } else {
    await handleLogin()
  }
}

const loginBtn = async () => {
  await handleLogin()
}

// 刷新图形验证码
const refreshCaptcha = async () => {
  try {
    const res = await getCaptchaService()
    if (res.code === 0 && res.data) {
      captchaUuid.value = res.data.uuid
      captchaImage.value = res.data.image
      showCaptcha.value = true
    }
  } catch (e) {
    console.error('获取验证码失败', e)
  }
}

// 登录处理
const handleLogin = async () => {
  if (!validateLoginForm()) {
    ElMessage({ type: 'error', message: '信息填写有误，请检查！' })
    return
  }
  if (!agreementChecked.value) {
    ElMessage({ type: 'warning', message: '请先同意用户协议' })
    return
  }

  let result
  if (loginMethod.value === 'password') {
    const loginData = {
      account: loginForm.value.account,
      password: loginForm.value.password
    }
    if (showCaptcha.value) {
      loginData.captchaUuid = captchaUuid.value
      loginData.captchaCode = loginForm.value.captchaCode
    }
    result = await userLoginService(loginData)
  } else {
    const codeData = {
      account: loginForm.value.account,
      code: loginForm.value.code
    }
    if (showCaptcha.value) {
      codeData.captchaUuid = captchaUuid.value
      codeData.captchaCode = loginForm.value.captchaCode
    }
    result = await userLoginByCodeService(codeData)
  }

  if (result.code === 0) {
    ElMessage.success(result.msg ? result.msg : '登录成功')

    // 存储双Token
    tokenStore.setTokens({
      accessToken: result.data.accessToken,
      refreshToken: result.data.refreshToken,
      tokenType: result.data.tokenType || 'Bearer'
    })

    // 存储用户信息
    userStore.setInfo({
      id: result.data.userId,
      username: result.data.username,
      nickname: result.data.nickname,
      phone: result.data.phone,
      avatar: result.data.avatar,
      userType: result.data.userType,
      status: result.data.status,
      level: result.data.level,
      exp: result.data.exp,
      vipType: result.data.vipType,
      isOnline: result.data.isOnline
    })

    showCaptcha.value = false
    if (result.data.userType === 1) {
      userStore.login({ role: 'admin', phone: loginForm.value.account })
      const redirectPath = router.currentRoute.value.query.redirect || '/admin'
      router.push(redirectPath)
    } else {
      userStore.login({ role: 'user', phone: loginForm.value.account })
      router.push('/')
    }
    emit('update:visible', false)
  } else {
    // 如果返回了"还剩X次机会"或"锁定"类错误，自动显示验证码
    const errMsg = result.msg || result.message || '登录失败'
    if (errMsg.includes('图形验证码') || errMsg.includes('还剩') || errMsg.includes('锁定')) {
      showCaptcha.value = true
      refreshCaptcha()
    }
    ElMessage.error(errMsg)
  }
}

// 发送验证码方法（支持手机号或邮箱）
const sendCode = async () => {
  const formType = props.isRegister ? 'register' : 'login'
  validateAccount(formType)

  if (!isAccountValid.value || errors.value[formType].account) {
    ElMessage({ type: 'error', message: '请输入正确的手机号或邮箱' })
    return
  }

  const target = props.isRegister ? registerForm.value.account : loginForm.value.account

  try {
    const result = await sendSMSCodeService(target, formType)
    if (result.code === 0) {
      isCounting.value = true
      countdown.value = 60
      timer.value = setInterval(() => {
        countdown.value--
        if (countdown.value <= 0) {
          clearInterval(timer.value)
          isCounting.value = false
        }
      }, 1000)
      ElMessage.success(isEmail(target) ? '验证码已发送至邮箱' : '验证码发送成功')
    } else {
      ElMessage.error(result.msg || '验证码发送失败')
    }
  } catch (error) {
    ElMessage.error('验证码发送失败')
  }
}

const handleAccountUpdate = (account) => {
  loginForm.value.account = account
  validateAccount('login')
}

const validateAccount = (formType) => {
  const account = formType === 'login' ? loginForm.value.account : registerForm.value.account
  if (!account) {
    errors.value[formType].account = '请输入手机号或邮箱'
    isAccountValid.value = false
  } else if (isEmail(account)) {
    errors.value[formType].account = ''
    isAccountValid.value = true
  } else if (!/^1[3-9]\d{9}$/.test(account)) {
    errors.value[formType].account = '请输入正确的手机号或邮箱'
    isAccountValid.value = false
  } else {
    errors.value[formType].account = ''
    isAccountValid.value = true
  }
}

const validateLoginForm = () => {
  validateAccount('login')
  if (loginMethod.value === 'password') {
    if (!loginForm.value.password) {
      errors.value.login.password = '请输入密码'
    } else {
      errors.value.login.password = ''
    }
    if (showCaptcha.value && !loginForm.value.captchaCode) {
      errors.value.login.captcha = '请输入图形验证码'
      refreshCaptcha()
    } else {
      errors.value.login.captcha = ''
    }
  } else {
    if (!loginForm.value.code) {
      errors.value.login.code = '请输入验证码'
    }
  }
  return Object.values(errors.value.login).every(error => !error)
}

const toggleForm = () => {
  emit('update:isRegister', !props.isRegister)
  loginMethod.value = 'password'
  showCaptcha.value = false
  resetErrors()
}

const validateRegisterForm = () => {
  validateAccount('register')
  validatePassword()
  validateConfirmPassword()
  return Object.values(errors.value.register).every(error => !error)
}

const validatePassword = () => {
  const password = registerForm.value.password
  if (!password) {
    errors.value.register.password = '请输入密码!'
  } else if (password.length < 5 || password.length > 16) {
    errors.value.register.password = '密码长度必须在5-16位之间!'
  } else {
    errors.value.register.password = ''
  }
}

const validateConfirmPassword = () => {
  if (registerForm.value.password !== registerForm.value.confirmPassword) {
    errors.value.register.confirmPassword = '两次输入的密码不一致'
  } else {
    errors.value.register.confirmPassword = ''
  }
}

const showForgotPassword = () => {
  showForgotModal.value = true
}

const resetErrors = () => {
  errors.value = {
    login: { account: '', password: '', code: '', captcha: '' },
    register: { account: '', password: '', confirmPassword: '', code: '' }
  }
}
onMounted(async () => {
  refreshCaptcha()
})

onBeforeUnmount(() => {
  document.removeEventListener('mousemove', handleDrag)
  document.removeEventListener('mouseup', stopDrag)
  if (timer.value) clearInterval(timer.value)
})
</script>

<style scoped>

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
  z-index: 1000;
}


.auth-container {
  position: fixed;
  bottom: 25%;
  left: 25%;
  z-index: 1000;
  cursor: move;
}

.box {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  margin: 0;
  overflow: hidden;
  width: 680px;
  height: 520px;
  border-radius: 1.5rem;
  box-shadow: 0 0 2rem 0.4rem rgba(23, 235, 146, 0.7);
  background-color: rgba(25, 245, 245, 0.4);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  cursor: default;
  /* 恢复默认光标 */
}

.box .left {
  position: relative;
  width: 30%;
  height: 100%;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

.box .left::before {
  content: '';
  display: flex;
  background-position-y: 10%;
  width: 100%;
  height: 100%;
  background-image: url(../assets/images/13.1.png);
  background-size: cover;
  opacity: 75%;
}

.box .right {
  display: flex;
  width: 70%;
  height: 100%;
  flex-direction: column;
  align-items: center;
  padding: 30px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  overflow: hidden;
}

.box .right h4 {
  color: rgb(144, 129, 241);
  font-size: 2rem;
  margin-bottom: 0;
}

.box .right form {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.input-group {
  position: relative;
  width: 80%;
  margin-bottom: 15px;
}

.input-group .iconfont {
  position: absolute;
  left: 10px;
  top: 60%;
  font-size: 1.5rem;
  transform: translateY(-50%);
  color: rgb(144, 129, 241);
}

.box .right form .acc {
  outline: none;
  width: 100%;
  height: 35px;
  font-size: 1rem;
  padding: 1rem 0 0 2.5rem;
  border: none;
  border-bottom: 1px solid rgb(144, 129, 241);
  color: rgb(144, 129, 241);
  background-color: rgba(92, 189, 206, 0.5);
}

.right .Login {
  width: 60%;
  height: 3rem;
  color: aliceblue;
  background-image: linear-gradient(120deg, #e0c3fc 0%, #8ec5fc 100%);
  font-size: 1.4rem;
  border: none;
  border-radius: 0.5rem;
  margin: 1.5rem 0;
  cursor: pointer;
}

.right .Login:hover {
  box-shadow: 0 0 2rem -0.5rem rgba(228, 54, 54, 0.3);
}

.social-login {
  width: 100%;
  text-align: center;
  margin: 10px 0;
}

.social-title {
  color: #666;
  font-size: 0.9rem;
  margin-bottom: 10px;
}

.social-icons {
  display: flex;
  justify-content: center;
  gap: 20px;
}

.social-icon {
  width: 40px;
  height: 40px;
  background-color: #f5f5f5;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.right .fn {
  display: flex;
  justify-content: center;
  width: 100%;
  margin-top: 1rem;
}

.right .fn a {
  font-size: 1rem;
  padding: 0 1rem;
  margin-bottom: 1rem;
  color: #666;
  text-decoration: none;
  cursor: pointer;
}

.right .fn a:hover {
  color: rgb(144, 129, 241);
}

.control-btn {
  position: absolute;
  top: 10px;
  right: 10px;
  width: 30px;
  height: 30px;
  border: none;
  background: rgba(25, 245, 245, 0.4);
  cursor: pointer;
  font-size: 20px;
  line-height: 30px;
  text-align: center;
  border-radius: 4px;
  padding: 0;
  transition: all 0.3s;
  color: #ff4d4f;
}

.control-btn:hover {
  background: #ec9d9d;
}

.error-message {
  color: #ff4d4f;
  font-size: 0.8rem;
  margin-top: -20px;
  text-align: right;
  width: 100%;
}

.code-group {
  position: relative;
  width: 80%;
  margin-bottom: 15px;
}

.send-btn {
  position: absolute;
  right: 0;
  top: 0;
  height: 30px;
  padding: 0 15px;
  background: linear-gradient(120deg, #e0c3fc 0%, #8ec5fc 100%);
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

.icon-shouji {
  font-size: 1.2rem;
}

/* 添加密码切换按钮样式 */
.password-toggle {
  position: absolute;
  right: 100px;
  top: 50%;
  transform: translateY(-50%);
  cursor: pointer;
  color: rgb(144, 129, 241);
  font-size: 1.2rem;
  z-index: 2;
}

.iconfont.password-toggle.icon-kejian::before {
  position: absolute;
  left: 280px;
  top: 50%;
  transform: translateY(-50%);
  cursor: pointer;
  color: rgb(144, 129, 241);
  font-size: 1.2rem;
  z-index: 2;
}

.iconfont.password-toggle.icon-bukejian::before {
  position: absolute;
  left: 280px;
  top: 50%;
  transform: translateY(-50%);
  cursor: pointer;
  color: rgb(144, 129, 241);
  font-size: 1.2rem;
  z-index: 2;
}

/* 确保密码输入框有足够的右边距 */
.input-group .acc[type="password"],
.input-group .acc[type="text"] {
  padding-right: 2.5rem;
}

/* 登录方式切换按钮样式 */
.login-method-toggle {
  display: flex;
  justify-content: center;
  margin: 15px 0;
  width: 100%;
  gap: 20px;
}

.method-btn {
  padding: 8px 20px;
  border: 1px solid rgb(144, 129, 241);
  background: transparent;
  color: rgb(144, 129, 241);
  border-radius: 20px;
  cursor: pointer;
  font-size: 0.7rem;
  transition: all 0.3s;
}

.method-btn.active {
  background: rgb(144, 129, 241);
  color: white;
}

.method-btn:hover:not(.active) {
  background: rgba(144, 129, 241, 0.1);
}

/* 图形验证码 */
.captcha-input {
  width: 55% !important;
}
.captcha-img {
  position: absolute;
  right: 0;
  top: 0;
  height: 35px;
  border-radius: 0 4px 4px 0;
  cursor: pointer;
  border: 1px solid rgb(144, 129, 241);
}
</style>
