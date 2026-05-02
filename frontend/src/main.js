import App from './App.vue'
import router from './router'
import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
// import locale from 'element-plus/dist/locale/zh-cn.js'
import { createPinia } from 'pinia'
import { createPersistedState } from 'pinia-persistedstate-plugin'
// import axios from 'axios'

const app = createApp(App);
const pinia = createPinia();
const persist = createPersistedState();
pinia.use(persist)

// 正确的 click-outside 指令注册方式
app.directive('click-outside', {
  beforeMount(el, binding) {
    el.clickOutsideEvent = function(event) {
      if (!(el === event.target || el.contains(event.target))) {
        binding.value(event)
      }
    }
    document.addEventListener('click', el.clickOutsideEvent)
  },
  unmounted(el) {
    document.removeEventListener('click', el.clickOutsideEvent)
  }
})

app.use(router)
   .use(ElementPlus)
   .use(pinia)

// app.config.globalProperties.$axios = axios
app.mount('#app')