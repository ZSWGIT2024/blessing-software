<template>
    <div class="agreement">
      <label>
        <input type="checkbox" v-model="checked">
        我已阅读并同意
        <a href="#" @click.prevent="showAgreement('user')">《用户协议》</a>
        和
        <a href="#" @click.prevent="showAgreement('privacy')">《隐私政策》</a>
      </label>
      
      <div v-if="showDialog" class="agreement-dialog">
        <div class="dialog-content">
          <h3>{{ dialogTitle }}</h3>
          <div class="dialog-body">
            {{ dialogContent }}
          </div>
          <button class="close-btn" @click="showDialog = false">关闭</button>
        </div>
      </div>
    </div>
  </template>
  
  <script>
  export default {
    data() {
      return {
        checked: false,
        showDialog: false,
        dialogTitle: '',
        dialogContent: ''
      }
    },
    methods: {
      showAgreement(type) {
        if (type === 'user') {
          this.dialogTitle = '用户协议'
          this.dialogContent = `欢迎您注册成为本网站用户...（此处为完整的用户协议内容）`
        } else {
          this.dialogTitle = '隐私政策'
          this.dialogContent = `我们非常重视您的隐私保护...（此处为完整的隐私政策内容）`
        }
        this.showDialog = true
      }
    },
    watch: {
      checked(newVal) {
        this.$emit('update:modelValue', newVal)
      }
    }
  }
  </script>
  
  <style scoped>
  .agreement {
    margin: 10px 0;
    font-size: 0.9rem;
    color: #666;
  }
  
  .agreement a {
    color: #1890ff;
    text-decoration: none;
  }
  
  .agreement a:hover {
    color: #40a9ff;
    text-decoration: underline;
  }
  
  .agreement-dialog {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 1001;
  }
  
  .dialog-content {
    background: white;
    padding: 20px;
    border-radius: 8px;
    max-width: 600px;
    max-height: 80vh;
    overflow-y: auto;
  }
  
  .dialog-body {
    margin: 15px 0;
    line-height: 1.6;
  }
  
  .close-btn {
    padding: 8px 16px;
    background: #1890ff;
    color: white;
    border: none;
    border-radius: 4px;
    cursor: pointer;
  }
  </style>