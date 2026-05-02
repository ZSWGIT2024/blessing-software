import { fileURLToPath, URL } from 'node:url'
import path from 'path';
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
      // '@': path.resolve(__dirname, './src'),
    }
  },
  server: {
    proxy: {
      '/api': {//将所有以/api开头的请求，都代理到http://localhost:8080
        target: 'http://localhost:8080',
        changeOrigin: true,//将主机头的原点更改为目标URL
         rewrite: (path) => path.replace(/^\/api/, '') // 保持/api前缀  把/api替换成空字符串''
      // 完全移除rewrite
      // configure: (proxy, options) => {
      //   proxy.on('proxyReq', (proxyReq) => {
      //     console.log('代理请求到:', proxyReq.path);
      //   });
      // }
      }
    }
  },
})

