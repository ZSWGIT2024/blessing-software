<template>
  <div class="verification-code">
    <!-- 账号输入框仅在注册模式显示 -->
    <div class="input-group" v-if="!isLogin">
      <input class="acc" type="text" :placeholder="isRegister ? '请输入手机号或邮箱' : '手机号或邮箱'" v-model="accountModel"
        @blur="validateAccount">
      <span class="iconfont icon-shouji"></span>
      <div v-if="accountError" class="error-message">{{ accountError }}</div>
    </div>

    <div class="code-group">
      <input class="acc" type="text" placeholder="请输入验证码" v-model="codeModel">
      <button class="send-btn" :disabled="isCounting || !isAccountValid" @click="sendCode">
        {{ countdown > 0 ? `${countdown}s后重发` : '获取验证码' }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onBeforeUnmount } from 'vue'
import { sendSMSCodeService } from '@/api/user'
import { ElMessage } from 'element-plus'

const props = defineProps({
  modelValue: String,
  account: { type: String, default: '' },
  isRegister: { type: Boolean, default: false },
  isLogin: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue', 'update:account'])

const isEmail = (str) => /^[\w.+-]+@[\w-]+\.[\w.]+$/.test(str)

const accountModel = ref(props.account || '')
const codeModel = ref(props.modelValue || '')
const accountError = ref('')
const isCounting = ref(false)
const countdown = ref(0)
const timer = ref(null)

const isAccountValid = computed(() => {
  const val = accountModel.value
  return isEmail(val) || /^1[3-9]\d{9}$/.test(val)
})

const validateAccount = () => {
  if (!accountModel.value) {
    accountError.value = '请输入手机号或邮箱'
  } else if (!isAccountValid.value) {
    accountError.value = '请输入正确的手机号或邮箱'
  } else {
    accountError.value = ''
  }
  if (!props.isLogin) {
    emit('update:account', accountModel.value)
  }
}

const sendCode = async () => {
  if (!isAccountValid.value) {
    accountError.value = '请输入正确的手机号或邮箱'
    return
  }
  try {
    const codeType = props.isRegister ? 'register' : 'login'
    const result = await sendSMSCodeService(accountModel.value, codeType)

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
      ElMessage.success(isEmail(accountModel.value) ? '验证码已发送至邮箱' : '验证码发送成功')
    } else {
      ElMessage.error(result.msg || '验证码发送失败')
    }
  } catch (error) {
    ElMessage.error('验证码发送失败')
  }
}

watch(codeModel, (newVal) => emit('update:modelValue', newVal))
watch(accountModel, (newVal) => { if (!props.isLogin) emit('update:account', newVal) })
watch(() => props.account, (newVal) => { accountModel.value = newVal || '' })
watch(() => props.modelValue, (newVal) => { codeModel.value = newVal || '' })

onBeforeUnmount(() => {
  if (timer.value) clearInterval(timer.value)
})
</script>

<style scoped>
.verification-code {
  width: 80%;
}

.code-group {
  position: relative;
  width: 100%;
  margin-bottom: 15px;
}

.code-group .acc {
  outline: none;
  border: none;
  height: 30px;
  border-bottom: 1px solid rgb(144, 129, 241);
  color: rgb(144, 129, 241);
  background-color: rgba(92, 189, 206, 0.5);
}

.send-btn {
  position: absolute;
  right: 0;
  top: 0;
  height: 35px;
  padding: 0 15px;
  background: linear-gradient(120deg, #e0c3fc 0%, #8ec5fc 100%);
  color: white;
  border: none;
  border-radius: 0 4px 4px 0;
  cursor: pointer;
  font-size: 0.6rem;
}

.send-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.error-message {
  color: #ff4d4f;
  font-size: 0.8rem;
  margin-top: 5px;
}
</style>