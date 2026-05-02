<template>
  <div>

    <router-view></router-view>

    <ConnectionStatus />
    <Header />
    <Sakura />
    <BrideText />
    <userFile />
    <ChatWindow />

  </div>
</template>

<script>
import ConnectionStatus from '@/components/ConnectionStatus.vue'
import ChatWindow from './components/ChatWindow.vue';
import userFile from './components/UserFile.vue'
import Header from './components/Header.vue';
import BrideText from './components/BrideText.vue';
import Sakura from './components/Sakura.vue';
import sakuraType1 from '@/assets/sakura/sakura-1.png'
import sakuraType2 from '@/assets/sakura/sakura-2.png'
import sakuraType3 from '@/assets/sakura/sakura-3.png'
import sakuraType4 from '@/assets/sakura/sakura-4.png'
import sakuraType5 from '@/assets/sakura/sakura-5.png'
import sakuraType6 from '@/assets/sakura/sakura-6.png'
import sakuraType7 from '@/assets/sakura/sakura-7.png'
import sakuraType8 from '@/assets/sakura/sakura-8.png'
// ...导入其他7种图片

export default {
  components: {
    Header,
    BrideText,
    Sakura,
    userFile,
    ChatWindow,




  },

  //网页刷新的尝试代码
  created() {
    // 恢复路由状态
    const savedRoute = localStorage.getItem('currentRoute')
    if (savedRoute && this.$route.path !== savedRoute) {
      this.$router.push(savedRoute)
    }

    // 保存路由变化
    this.$router.afterEach((to) => {
      localStorage
        .setItem('currentRoute', to.path)
    })
  },
  //网页刷新的尝试代码

  mounted() {
    // 预加载图片
    [sakuraType1, sakuraType2, sakuraType3, sakuraType4, sakuraType5, sakuraType6, sakuraType7, sakuraType8].forEach(src => {
      new Image().src = src
    })

  }
}

</script>

<style scoped>
#app {
  font-family: 'Arial', sans-serif;
  background-color: #fdeff3;
  /* 樱花粉色背景 */
  color: #6d4c41;
  margin: 0;
  padding: 0;
  position: relative;
}
</style>