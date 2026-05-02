<template>
  <div class="verification-code">
    <!-- 手机号输入框仅在注册模式显示 -->
    <div class="input-group" v-if="!isLogin">
      <input class="acc" type="tel" :placeholder="isRegister ? '请输入手机号' : '手机号'" v-model="phoneModel"
        @blur="validatePhone">
      <span class="iconfont icon-shouji"></span>
      <div v-if="phoneError" class="error-message">{{ phoneError }}</div>
    </div>

    <div class="code-group">
      <input class="acc" type="text" placeholder="请输入验证码" v-model="codeModel">
      <button class="send-btn" :disabled="isCounting || !isPhoneValid" @click="sendCode">
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
  phone: String,
  isRegister: {
    type: Boolean,
    default: false
  },
  isLogin: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'update:phone'])

// 响应式数据
const phoneModel = ref(props.phone || '')
const codeModel = ref(props.modelValue || '')
const phoneError = ref('')
const isCounting = ref(false)
const countdown = ref(0)
const timer = ref(null)

// 计算属性
const isPhoneValid = computed(() => {
  return /^1[3-9]\d{9}$/.test(phoneModel.value)
})

// 方法
const validatePhone = () => {
  if (!phoneModel.value) {
    phoneError.value = '请输入手机号'
  } else if (!isPhoneValid.value) {
    phoneError.value = '请输入正确的手机号'
  } else {
    phoneError.value = ''
  }

  // 触发父组件更新手机号
  if (!props.isLogin) {
    emit('update:phone', phoneModel.value)
  }
}

const sendCode = async () => {
  if (!isPhoneValid.value) {
    phoneError.value = '请输入正确的手机号'
    return
  }

  try {
    // 发送登录或注册验证码
    const result = await sendSMSCodeService(phoneModel.value, 'login')

    if (result.code === 0) {
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
      // 实际项目中调用API发送验证码
      console.log(`验证码已发送至: ${phoneModel.value}`)
      ElMessage.success('验证码发送成功')
    } else {
      ElMessage.error(result.msg || '验证码发送失败')
    }
  } catch (error) {
    ElMessage.error('验证码发送失败')
  }
}

// 监听器
watch(codeModel, (newVal) => {
  emit('update:modelValue', newVal)
})

watch(phoneModel, (newVal) => {
  if (!props.isLogin) {
    emit('update:phone', newVal)
  }
})

// 监听props变化
watch(() => props.phone, (newVal) => {
  phoneModel.value = newVal || ''
})

watch(() => props.modelValue, (newVal) => {
  codeModel.value = newVal || ''
})

// 生命周期
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